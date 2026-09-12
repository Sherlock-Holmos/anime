import AuthenticationServices
import Security
import SwiftUI
import UIKit
import AnimeShared

struct ContentView: View {
    @State private var selectedRootIndex = 0
    @State private var nativeGlassEnabled = true

    var body: some View {
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
                // Keep the Compose scene full-bleed so the native bars can float over page
                // content. NativeChromeViewController protects only the status-bar region.
                .ignoresSafeArea(.container, edges: [.top, .bottom])
                .tabItem {
                    Label(tab.title, systemImage: tab.systemImage)
                }
                .tag(index)
            }
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

private final class NativeChromeViewController: UIViewController {
    private let contentViewController: UIViewController
    private let statusBarMaterialView =
        UIVisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterial))
    private var statusBarChromeHeightConstraint: NSLayoutConstraint?
    private let statusBarFadeMask = CAGradientLayer()

    init(contentViewController: UIViewController) {
        self.contentViewController = contentViewController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        view.isOpaque = false

        addChild(contentViewController)
        let contentView = contentViewController.view!
        contentView.translatesAutoresizingMaskIntoConstraints = false
        contentView.backgroundColor = .clear
        contentView.isOpaque = false
        view.addSubview(contentView)
        NSLayoutConstraint.activate([
            contentView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentView.topAnchor.constraint(equalTo: view.topAnchor),
            contentView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        contentViewController.didMove(toParent: self)

        statusBarMaterialView.translatesAutoresizingMaskIntoConstraints = false
        statusBarMaterialView.isUserInteractionEnabled = false
        statusBarMaterialView.clipsToBounds = true
        // Books uses a low-contrast system material that lets the page remain the visual source.
        // A stronger alpha reads as a separate dark/white rectangle on a flat Compose background.
        statusBarMaterialView.alpha = 0.42
        statusBarFadeMask.startPoint = CGPoint(x: 0.5, y: 0)
        statusBarFadeMask.endPoint = CGPoint(x: 0.5, y: 1)
        statusBarFadeMask.colors = [
            UIColor.white.cgColor,
            UIColor.white.cgColor,
            UIColor.clear.cgColor,
        ]
        statusBarFadeMask.locations = [0.0, 0.68, 1.0]
        statusBarMaterialView.layer.mask = statusBarFadeMask
        view.addSubview(statusBarMaterialView)
        statusBarChromeHeightConstraint = statusBarMaterialView.heightAnchor.constraint(equalToConstant: 0)
        NSLayoutConstraint.activate([
            statusBarMaterialView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            statusBarMaterialView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            statusBarMaterialView.topAnchor.constraint(equalTo: view.topAnchor),
            statusBarChromeHeightConstraint!,
        ])
        updateStatusBarMaterialHeight()
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateStatusBarMaterialHeight()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        statusBarFadeMask.frame = statusBarMaterialView.bounds
        updateStatusBarMaterialHeight()
    }

    private func updateStatusBarMaterialHeight() {
        let localTopInset = view.safeAreaInsets.top
        let windowTopInset = view.window?.safeAreaInsets.top ?? 0
        let statusBarFrameHeight =
            view.window?.windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let topInset = max(localTopInset, windowTopInset, statusBarFrameHeight)
        // Extend slightly below the system status area and fade out instead of ending with a
        // hard horizontal line. This is the same visual transition used by native reading apps.
        statusBarChromeHeightConstraint?.constant = topInset + 24
    }
}

private struct ComposeTabView: UIViewControllerRepresentable {
    let rootIndex: Int
    let handlesAuthCallback: Bool
    let onNativeGlassStateChanged: (KotlinBoolean) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let composeViewController = IosBridge.shared.rootViewController(
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
        return NativeChromeViewController(contentViewController: composeViewController)
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
