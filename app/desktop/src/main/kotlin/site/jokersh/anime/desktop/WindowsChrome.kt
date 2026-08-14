package site.jokersh.anime.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

private const val DWMWA_BORDER_COLOR = 34
private const val DWMWA_CAPTION_COLOR = 35
private const val DWMWA_TEXT_COLOR = 36
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val INT_SIZE = 4

private interface DwmApi : Library {
    @Suppress("FunctionName")
    fun DwmSetWindowAttribute(
        windowHandle: Pointer,
        attribute: Int,
        value: IntByReference,
        valueSize: Int,
    ): Int
}

internal fun applyWindowsChrome(window: Window) {
    if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return

    runCatching {
        val dwmApi = Native.load("dwmapi", DwmApi::class.java)
        val windowHandle = Native.getWindowPointer(window)
        dwmApi.setIntAttribute(windowHandle, DWMWA_USE_IMMERSIVE_DARK_MODE, 1)
        dwmApi.setIntAttribute(windowHandle, DWMWA_CAPTION_COLOR, rgb(27, 28, 42))
        dwmApi.setIntAttribute(windowHandle, DWMWA_TEXT_COLOR, rgb(238, 237, 247))
        dwmApi.setIntAttribute(windowHandle, DWMWA_BORDER_COLOR, rgb(27, 28, 42))
    }
}

private fun DwmApi.setIntAttribute(
    windowHandle: Pointer,
    attribute: Int,
    value: Int,
) {
    DwmSetWindowAttribute(
        windowHandle = windowHandle,
        attribute = attribute,
        value = IntByReference(value),
        valueSize = INT_SIZE,
    )
}

private fun rgb(
    red: Int,
    green: Int,
    blue: Int,
): Int = red or (green shl 8) or (blue shl 16)
