import AuthenticationServices
import Security
import SwiftUI
import UIKit
import AnimeShared

struct ContentView: View {
    @State private var selectedRootIndex = 0
    @State private var nativeGlassEnabled = true
    @State private var nativeRootNavigationVisible = true

    var body: some View {
        ComposeRootView(onRootSelectionChanged: { index in
            selectedRootIndex = index.intValue
        }, onNativeGlassStateChanged: { enabled in
            nativeGlassEnabled = enabled.boolValue
        }, onNativeRootNavigationVisibilityChanged: { visible in
            nativeRootNavigationVisible = visible.boolValue
        })
            .ignoresSafeArea()
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if nativeRootNavigationVisible {
                    NativeLiquidGlassTabBar(
                        selectedIndex: $selectedRootIndex,
                        glassEnabled: nativeGlassEnabled,
                        onSelect: { index in
                            IosBridge.shared.requestRootSelection(index: Int32(index))
                        },
                    )
                    .padding(.horizontal, 12)
                    .padding(.top, 6)
                    .padding(.bottom, 6)
                }
            }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    let onRootSelectionChanged: (KotlinInt) -> Void
    let onNativeGlassStateChanged: (KotlinBoolean) -> Void
    let onNativeRootNavigationVisibilityChanged: (KotlinBoolean) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        IosBridge.shared.mainViewController(
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
            onRootSelectionChanged: onRootSelectionChanged,
            onNativeGlassStateChanged: onNativeGlassStateChanged,
            onNativeRootNavigationVisibilityChanged: onNativeRootNavigationVisibilityChanged,
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct NativeLiquidGlassTabBar: View {
    @Binding var selectedIndex: Int
    let glassEnabled: Bool
    let onSelect: (Int) -> Void

    private let tabs: [(title: String, systemImage: String)] = [
        ("发现", "sparkles.magnifyingglass"),
        ("资料库", "books.vertical"),
        ("动态", "bubble.left.and.bubble.right"),
        ("我的", "person.crop.circle"),
    ]

    var body: some View {
        Group {
            if glassEnabled {
                glassTabBar
            } else {
                fallbackTabBar
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("根导航")
    }

    private var glassTabBar: some View {
        GlassEffectContainer(spacing: 8) {
            HStack(spacing: 8) {
                ForEach(Array(tabs.enumerated()), id: \.offset) { index, tab in
                    tabButton(index: index, tab: tab, glass: true)
                }
            }
            .padding(8)
        }
    }

    private var fallbackTabBar: some View {
        HStack(spacing: 8) {
            ForEach(Array(tabs.enumerated()), id: \.offset) { index, tab in
                tabButton(index: index, tab: tab, glass: false)
            }
        }
        .padding(12)
        .background(.bar, in: Capsule())
    }

    @ViewBuilder
    private func tabButton(
        index: Int,
        tab: (title: String, systemImage: String),
        glass: Bool,
    ) -> some View {
        let button = Button {
            onSelect(index)
        } label: {
            Label(tab.title, systemImage: tab.systemImage)
                .font(.caption2.weight(index == selectedIndex ? .semibold : .medium))
                .labelStyle(.titleAndIcon)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
        }
        if glass {
            button
                .buttonStyle(.glass(.regular))
                .tint(index == selectedIndex ? .accentColor : .secondary)
        } else {
            button
                .buttonStyle(.bordered)
                .tint(index == selectedIndex ? .accentColor : .secondary)
        }
    }
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
