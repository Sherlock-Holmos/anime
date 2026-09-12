import AuthenticationServices
import Security
import SwiftUI
import UIKit
import AnimeShared

struct ContentView: View {
    @State private var selectedRootIndex = 0
    @State private var nativeGlassEnabled = true

    var body: some View {
        ZStack(alignment: .top) {
            TabView(selection: $selectedRootIndex) {
                ForEach(Array(tabs.enumerated()), id: \.offset) { index, tab in
                    ComposeTabView(
                        rootIndex: index,
                        handlesAuthCallback: index == 0,
                        onNativeGlassStateChanged: { enabled in
                            if index == 0 {
                                nativeGlassEnabled = enabled.boolValue
                            }
                        },
                    )
                    // Let the system tab bar and status-bar material float over the Compose page.
                    .ignoresSafeArea(.container, edges: [.top, .bottom])
                    .tabItem {
                        Label(tab.title, systemImage: tab.systemImage)
                    }
                    .tag(index)
                }
            }

            // Use Apple's semantic bar material instead of a hand-tuned UIKit blur. This is the
            // same material family used by native reading/navigation surfaces and automatically
            // adapts to light/dark mode, contrast settings, and future iOS releases.
            NativeStatusBarMaterial()
        }
        .tint(.accentColor)
        .toolbarBackground(nativeGlassEnabled ? .visible : .hidden, for: .tabBar)
        .background(Color.clear)
    }

    private let tabs: [(title: String, systemImage: String)] = [
        // `sparkles.magnifyingglass` is not rendered consistently by every SF Symbols
        // catalog shipped with Xcode. Use the stable discovery symbol so the first tab
        // always has a visible native icon.
        ("发现", "sparkles"),
        ("资料库", "books.vertical"),
        ("动态", "bubble.left.and.bubble.right"),
        ("我的", "person.crop.circle"),
    ]
}

private struct NativeStatusBarMaterial: View {
    private let fadeDistance: CGFloat = 42

    var body: some View {
        GeometryReader { proxy in
            let topInset = max(proxy.safeAreaInsets.top, currentStatusBarHeight())
            let materialHeight = topInset + fadeDistance
            Rectangle()
                .fill(Material.bar)
                .frame(width: proxy.size.width, height: materialHeight)
                .mask(
                    LinearGradient(
                        stops: [
                            .init(color: .white, location: 0),
                            .init(
                                color: .white,
                                location: topInset / max(materialHeight, 1),
                            ),
                            .init(color: .clear, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom,
                    )
                )
                .frame(maxHeight: .infinity, alignment: .top)
        }
        .ignoresSafeArea(.container, edges: .top)
        .allowsHitTesting(false)
    }
}

private func currentStatusBarHeight() -> CGFloat {
    UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .filter { $0.activationState == .foregroundActive }
        .compactMap { $0.statusBarManager?.statusBarFrame.height }
        .max() ?? 0
}

private struct ComposeTabView: UIViewControllerRepresentable {
    let rootIndex: Int
    let handlesAuthCallback: Bool
    let onNativeGlassStateChanged: (KotlinBoolean) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        IosBridge.shared.rootViewController(
            rootIndex: Int32(rootIndex),
            openExternalUrl: { rawUrl in
                IosAuthSessionCoordinator.shared.start(rawUrl: rawUrl)
            },
            readSecret: { account in
                IosKeychain.shared.read(account: account)
            },
            writeSecret: { account, value in
                IosKeychain.shared.write(account: account, value: value)
            },
            removeSecret: { account in
                IosKeychain.shared.remove(account: account)
            },
            onNativeGlassStateChanged: onNativeGlassStateChanged,
            handlesAuthCallback: handlesAuthCallback,
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class IosKeychain {
    static let shared = IosKeychain()
    private let service = "site.jokersh.anime.session"

    func read(account: String) -> String? {
        var query = baseQuery(account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func write(account: String, value: String) {
        let data = Data(value.utf8)
        let query = baseQuery(account: account)
        let updateStatus = SecItemUpdate(
            query as CFDictionary,
            [kSecValueData as String: data] as CFDictionary
        )
        guard updateStatus == errSecItemNotFound else {
            if updateStatus != errSecSuccess {
                NSLog("Anime Keychain update failed: %d", updateStatus)
            }
            return
        }

        var insert = query
        insert[kSecValueData as String] = data
        let insertStatus = SecItemAdd(insert as CFDictionary, nil)
        if insertStatus != errSecSuccess {
            // A sideloaded build may receive a different keychain access group
            // after it is re-signed. Persistence failure must never terminate the app.
            NSLog("Anime Keychain insert failed: %d", insertStatus)
        }
    }

    func remove(account: String) {
        SecItemDelete(baseQuery(account: account) as CFDictionary)
    }

    private func baseQuery(account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }
}

private final class IosAuthSessionCoordinator: NSObject, ASWebAuthenticationPresentationContextProviding {
    static let shared = IosAuthSessionCoordinator()

    private var session: ASWebAuthenticationSession?

    func start(rawUrl: String) {
        guard let url = URL(string: rawUrl) else { return }
        let nextSession = ASWebAuthenticationSession(url: url, callbackURLScheme: "anime") { callback, _ in
            if let callback {
                _ = IosBridge.shared.handleOpenUrl(rawUrl: callback.absoluteString)
            }
        }
        nextSession.presentationContextProvider = self
        nextSession.prefersEphemeralWebBrowserSession = false
        session = nextSession
        nextSession.start()
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
}
