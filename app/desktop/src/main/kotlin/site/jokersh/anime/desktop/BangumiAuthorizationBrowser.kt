package site.jokersh.anime.desktop

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal class BangumiAuthorizationBrowser(
    private val apiBaseUrl: String,
) : AutoCloseable {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    private val activeSockets = CopyOnWriteArrayList<Socket>()
    private var proxy: ServerSocket? = null
    private var browser: Process? = null
    private var profileDirectory: File? = null

    fun open(authorizationUrl: String) {
        close()
        val state = oauthState(authorizationUrl)
        val server = ServerSocket(0, 16, InetAddress.getLoopbackAddress())
        proxy = server
        thread(name = "anime-bangumi-proxy", isDaemon = true) {
            while (!server.isClosed) {
                runCatching { server.accept() }
                    .onSuccess { socket ->
                        activeSockets += socket
                        thread(name = "anime-bangumi-proxy-connection", isDaemon = true) {
                            socket.use { relayConnect(it, state) }
                            activeSockets -= socket
                        }
                    }
            }
        }

        val edge = locateEdge() ?: error("未找到 Microsoft Edge，无法打开隔离的 Bangumi 授权窗口。")
        val profile = Files.createTempDirectory("anime-bangumi-auth-${UUID.randomUUID()}").toFile()
        profileDirectory = profile
        browser =
            ProcessBuilder(
                edge.absolutePath,
                "--app=$authorizationUrl",
                "--proxy-server=http://127.0.0.1:${server.localPort}",
                "--proxy-bypass-list=<-loopback>",
                "--user-data-dir=${profile.absolutePath}",
                "--no-first-run",
                "--disable-sync",
            ).start()
    }

    private fun relayConnect(
        socket: Socket,
        state: String,
    ) {
        socket.soTimeout = 15_000
        val requestLine =
            readConnectHeaders(socket.getInputStream())
                .lineSequence()
                .firstOrNull()
                ?: return
        val target =
            requestLine
                .split(' ')
                .getOrNull(1)
                .orEmpty()
                .lowercase()
        if (!allowedTarget(target)) {
            socket.getOutputStream().write("HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n".toByteArray())
            return
        }

        val endpoint =
            apiBaseUrl
                .replaceFirst("https://", "wss://")
                .replaceFirst("http://", "ws://") +
                "/api/v1/auth/bangumi/tunnel?target=" + URLEncoder.encode(target, Charsets.UTF_8)
        val opened = java.util.concurrent.CountDownLatch(1)
        val finished = java.util.concurrent.CountDownLatch(1)
        var failure: Throwable? = null
        val webSocket =
            client.newWebSocket(
                Request
                    .Builder()
                    .url(endpoint)
                    .header("x-anime-oauth-state", state)
                    .build(),
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        opened.countDown()
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        bytes: ByteString,
                    ) {
                        runCatching {
                            synchronized(socket) {
                                socket.getOutputStream().write(bytes.toByteArray())
                                socket.getOutputStream().flush()
                            }
                        }.onFailure { webSocket.cancel() }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        failure = t
                        opened.countDown()
                        finished.countDown()
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        finished.countDown()
                    }
                },
            )
        if (!opened.await(12, TimeUnit.SECONDS) || failure != null) {
            webSocket.cancel()
            return
        }
        socket.soTimeout = 0
        socket.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
        val buffer = ByteArray(32 * 1024)
        while (!socket.isClosed) {
            val count = runCatching { socket.getInputStream().read(buffer) }.getOrDefault(-1)
            if (count <= 0 || !webSocket.send(ByteString.of(*buffer.copyOf(count)))) break
        }
        webSocket.close(1000, "browser connection closed")
        finished.await(2, TimeUnit.SECONDS)
    }

    override fun close() {
        runCatching { proxy?.close() }
        proxy = null
        activeSockets.forEach { runCatching { it.close() } }
        activeSockets.clear()
        runCatching { browser?.destroy() }
        browser = null
        profileDirectory?.let { directory ->
            thread(name = "anime-bangumi-profile-cleanup", isDaemon = true) {
                runCatching { directory.deleteRecursively() }
            }
        }
        profileDirectory = null
    }

    private fun oauthState(url: String): String {
        val query = URI(url).rawQuery ?: error("Bangumi 授权地址缺少 state。")
        return query
            .split('&')
            .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
            .firstOrNull { it[0] == "state" }
            ?.get(1)
            ?.let { URLDecoder.decode(it, Charsets.UTF_8) }
            ?.takeIf(String::isNotBlank)
            ?: error("Bangumi 授权地址缺少 state。")
    }

    private fun allowedTarget(target: String): Boolean = target == "bgm.tv:443" || target == "lain.bgm.tv:443"

    /** Reads exactly through CRLF CRLF so no TLS ClientHello bytes are lost to buffering. */
    private fun readConnectHeaders(input: InputStream): String {
        val bytes = ArrayList<Byte>(512)
        while (bytes.size < MAX_CONNECT_HEADER_BYTES) {
            val next = input.read()
            if (next < 0) break
            bytes += next.toByte()
            val size = bytes.size
            if (
                size >= 4 &&
                bytes[size - 4] == '\r'.code.toByte() &&
                bytes[size - 3] == '\n'.code.toByte() &&
                bytes[size - 2] == '\r'.code.toByte() &&
                bytes[size - 1] == '\n'.code.toByte()
            ) {
                return bytes.toByteArray().toString(Charsets.ISO_8859_1)
            }
        }
        error("无效或过大的 CONNECT 请求头。")
    }

    private fun locateEdge(): File? =
        sequenceOf(
            System.getenv("PROGRAMFILES(X86)"),
            System.getenv("PROGRAMFILES"),
            System.getenv("LOCALAPPDATA"),
        ).filterNotNull()
            .map { File(it, "Microsoft/Edge/Application/msedge.exe") }
            .firstOrNull(File::isFile)

    private companion object {
        const val MAX_CONNECT_HEADER_BYTES = 16 * 1024
    }
}
