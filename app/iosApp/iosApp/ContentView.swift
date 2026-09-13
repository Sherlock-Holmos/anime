import AuthenticationServices
import Security
import SwiftUI
import UIKit
import AnimeShared

struct ContentView: View {
    @State private var selectedRootIndex = 0
    @State private var nativeGlassEnabled = true
    @StateObject private var nativeModel = NativeAppModel()

    var body: some View {
        TabView(selection: $selectedRootIndex) {
            NativeDiscoverView(model: nativeModel)
                .tabItem { Label(tabs[0].title, systemImage: tabs[0].systemImage) }
                .tag(0)
            NativeLibraryView(model: nativeModel)
                .tabItem { Label(tabs[1].title, systemImage: tabs[1].systemImage) }
                .tag(1)
            NativeActivityView(model: nativeModel)
                .tabItem { Label(tabs[2].title, systemImage: tabs[2].systemImage) }
                .tag(2)
            NativeProfileView(model: nativeModel)
                .tabItem { Label(tabs[3].title, systemImage: tabs[3].systemImage) }
                .tag(3)
        }
        .tint(.accentColor)
        // Keep the tab bar surface under SwiftUI's ownership so its scroll-edge material,
        // selection animation and safe-area treatment stay native.
        .toolbarBackgroundVisibility(
            nativeGlassEnabled ? .automatic : .hidden,
            for: .tabBar
        )
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

final class IosKeychain {
    static let shared = IosKeychain()
    private let service = "site.jokersh.anime.session"
    // Session credentials must remain readable after the app is relaunched once the
    // device has been unlocked. This item is device-local by design and must never be
    // restored to another device.
    private let accessibility = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

    func read(account: String) -> String? {
        var query = baseQuery(account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess else {
            if status != errSecItemNotFound {
                NSLog("Anime Keychain read failed: %d", status)
            }
            return nil
        }
        guard let data = result as? Data else {
            NSLog("Anime Keychain read returned an unexpected value")
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    func write(account: String, value: String) {
        let data = Data(value.utf8)
        var item = baseQuery(account: account)
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = accessibility

        // Add first, then update an existing item. This avoids the old delete-then-add
        // window and also repairs items created by earlier builds with weaker defaults.
        let insertStatus = SecItemAdd(item as CFDictionary, nil)
        if insertStatus == errSecSuccess {
            return
        }

        guard insertStatus == errSecDuplicateItem else {
            // A sideloaded build may receive a different keychain access group after it
            // is re-signed. Persistence failure must never terminate the app.
            NSLog("Anime Keychain insert failed: %d", insertStatus)
            return
        }

        let updateStatus = SecItemUpdate(
            baseQuery(account: account) as CFDictionary,
            [
                kSecValueData as String: data,
                kSecAttrAccessible as String: accessibility,
            ] as CFDictionary
        )
        if updateStatus != errSecSuccess {
            NSLog("Anime Keychain update failed: %d", updateStatus)
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

final class IosAuthSessionCoordinator: NSObject, ASWebAuthenticationPresentationContextProviding {
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
