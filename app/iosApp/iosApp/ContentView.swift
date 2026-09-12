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
                // Let the native chrome float over the edge-to-edge Compose page.
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
    private let edgeEffectScrollView = UIScrollView()

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

        edgeEffectScrollView.translatesAutoresizingMaskIntoConstraints = false
        edgeEffectScrollView.backgroundColor = .clear
        edgeEffectScrollView.isOpaque = false
        edgeEffectScrollView.contentInsetAdjustmentBehavior = .never
        edgeEffectScrollView.showsVerticalScrollIndicator = false
        edgeEffectScrollView.showsHorizontalScrollIndicator = false
        edgeEffectScrollView.alwaysBounceVertical = false
        edgeEffectScrollView.alwaysBounceHorizontal = false
        // Compose owns scrolling. Keep UIKit's scroll view stationary and use it only as the
        // native iOS 26 rendering host for Apple's progressive soft scroll-edge effect.
        edgeEffectScrollView.panGestureRecognizer.isEnabled = false
        edgeEffectScrollView.topEdgeEffect.style = .soft
        edgeEffectScrollView.bottomEdgeEffect.isHidden = true
        edgeEffectScrollView.leftEdgeEffect.isHidden = true
        edgeEffectScrollView.rightEdgeEffect.isHidden = true
        view.addSubview(edgeEffectScrollView)
        NSLayoutConstraint.activate([
            edgeEffectScrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            edgeEffectScrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            edgeEffectScrollView.topAnchor.constraint(equalTo: view.topAnchor),
            edgeEffectScrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        addChild(contentViewController)
        let contentView = contentViewController.view!
        contentView.translatesAutoresizingMaskIntoConstraints = false
        contentView.backgroundColor = .clear
        contentView.isOpaque = false
        edgeEffectScrollView.addSubview(contentView)
        NSLayoutConstraint.activate([
            contentView.leadingAnchor.constraint(equalTo: edgeEffectScrollView.contentLayoutGuide.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: edgeEffectScrollView.contentLayoutGuide.trailingAnchor),
            contentView.topAnchor.constraint(equalTo: edgeEffectScrollView.contentLayoutGuide.topAnchor),
            contentView.bottomAnchor.constraint(equalTo: edgeEffectScrollView.contentLayoutGuide.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: edgeEffectScrollView.frameLayoutGuide.widthAnchor),
            contentView.heightAnchor.constraint(equalTo: edgeEffectScrollView.frameLayoutGuide.heightAnchor),
        ])
        contentViewController.didMove(toParent: self)
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
