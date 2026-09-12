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

/// Hosts a Compose root inside the UIScrollView that actually owns vertical movement.
/// This is the key difference from the previous stationary wrapper: iOS now receives real
/// content offsets and can render the same progressive `.soft` edge used by Apple Books.
private final class NativeRootScrollViewController: UIViewController {
    private let contentViewController: UIViewController
    private let scrollView = UIScrollView()
    private var contentHeightConstraint: NSLayoutConstraint?
    // Give Compose room for the first non-lazy root layout. The measured final height replaces
    // this probe as soon as discovery data reaches a terminal state.
    private var reportedContentHeight: CGFloat = 3_000
    private var rootPageVisible = true
    private var savedRootOffsetY: CGFloat?
    private var didScheduleCIScrollPreview = false

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

        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.backgroundColor = .clear
        scrollView.isOpaque = false
        scrollView.contentInsetAdjustmentBehavior = .never
        scrollView.showsVerticalScrollIndicator = true
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.alwaysBounceVertical = true
        scrollView.alwaysBounceHorizontal = false
        scrollView.isDirectionalLockEnabled = true
        scrollView.delaysContentTouches = false
        scrollView.canCancelContentTouches = true
        scrollView.topEdgeEffect.isHidden = false
        scrollView.topEdgeEffect.style = .soft
        scrollView.bottomEdgeEffect.isHidden = true
        scrollView.leftEdgeEffect.isHidden = true
        scrollView.rightEdgeEffect.isHidden = true
        view.addSubview(scrollView)
        NSLayoutConstraint.activate([
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.topAnchor.constraint(equalTo: view.topAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        addChild(contentViewController)
        let contentView = contentViewController.view!
        contentView.translatesAutoresizingMaskIntoConstraints = false
        contentView.backgroundColor = .clear
        contentView.isOpaque = false
        scrollView.addSubview(contentView)
        let heightConstraint = contentView.heightAnchor.constraint(equalToConstant: 1)
        contentHeightConstraint = heightConstraint
        NSLayoutConstraint.activate([
            contentView.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            contentView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),
            heightConstraint,
        ])
        contentViewController.didMove(toParent: self)
        updateNativeScrollGeometry(preserveOffset: false)
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateNativeScrollGeometry(preserveOffset: true)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateNativeScrollGeometry(preserveOffset: true)
    }

    func setRootPageVisible(_ visible: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self, self.rootPageVisible != visible else { return }
            if !visible {
                self.savedRootOffsetY = self.scrollView.contentOffset.y
            }
            self.rootPageVisible = visible
            self.updateNativeScrollGeometry(preserveOffset: false)
        }
    }

    func setReportedContentHeight(_ height: CGFloat) {
        guard height.isFinite, height > 0 else { return }
        DispatchQueue.main.async { [weak self] in
            guard let self, abs(self.reportedContentHeight - height) > 0.5 else { return }
            self.reportedContentHeight = height
            if self.rootPageVisible {
                self.updateNativeScrollGeometry(preserveOffset: true)
                self.scheduleCIScrollPreviewIfNeeded()
            }
        }
    }

    private func scheduleCIScrollPreviewIfNeeded() {
        guard ProcessInfo.processInfo.arguments.contains("--ci-scroll-edge-preview"),
              !didScheduleCIScrollPreview,
              reportedContentHeight > view.bounds.height + 160 else { return }
        didScheduleCIScrollPreview = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            guard let self, self.rootPageVisible else { return }
            let topInset = self.scrollView.contentInset.top
            let maximumOffset = max(-topInset, self.reportedContentHeight - self.view.bounds.height)
            self.scrollView.setContentOffset(
                CGPoint(x: 0, y: min(140, maximumOffset)),
                animated: false
            )
        }
    }

    private func updateNativeScrollGeometry(preserveOffset: Bool) {
        guard isViewLoaded else { return }
        let localTopInset = view.safeAreaInsets.top
        let windowTopInset = view.window?.safeAreaInsets.top ?? 0
        let statusBarFrameHeight =
            view.window?.windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let topInset = max(localTopInset, windowTopInset, statusBarFrameHeight)
        let viewportHeight = max(view.bounds.height, 1)

        scrollView.isScrollEnabled = rootPageVisible
        scrollView.alwaysBounceVertical = rootPageVisible
        scrollView.topEdgeEffect.isHidden = !rootPageVisible
        contentHeightConstraint?.constant =
            rootPageVisible ? max(reportedContentHeight, viewportHeight - topInset) : viewportHeight

        if rootPageVisible {
            let oldOffset = scrollView.contentOffset.y
            scrollView.contentInset = UIEdgeInsets(top: topInset, left: 0, bottom: 0, right: 0)
            scrollView.verticalScrollIndicatorInsets = UIEdgeInsets(top: topInset, left: 0, bottom: 0, right: 0)
            let desiredOffset = preserveOffset ? oldOffset : (savedRootOffsetY ?? -topInset)
            let maximumOffset = max(-topInset, (contentHeightConstraint?.constant ?? 0) - viewportHeight)
            scrollView.setContentOffset(
                CGPoint(x: 0, y: min(max(desiredOffset, -topInset), maximumOffset)),
                animated: false
            )
        } else {
            scrollView.contentInset = .zero
            scrollView.verticalScrollIndicatorInsets = .zero
            scrollView.setContentOffset(.zero, animated: false)
        }
    }
}

private final class NativeRootScrollCoordinator {
    weak var host: NativeRootScrollViewController?

    func rootVisibilityChanged(_ visible: KotlinBoolean) {
        host?.setRootPageVisible(visible.boolValue)
    }

    func contentHeightChanged(_ height: KotlinDouble) {
        host?.setReportedContentHeight(CGFloat(height.doubleValue))
    }
}

private struct ComposeTabView: UIViewControllerRepresentable {
    let rootIndex: Int
    let handlesAuthCallback: Bool
    let onNativeGlassStateChanged: (KotlinBoolean) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let nativeScrollCoordinator = NativeRootScrollCoordinator()
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
            onNativeRootNavigationVisibilityChanged: { visible in
                nativeScrollCoordinator.rootVisibilityChanged(visible)
            },
            onNativeContentHeightChanged: { height in
                nativeScrollCoordinator.contentHeightChanged(height)
            },
            handlesAuthCallback: handlesAuthCallback,
        )
        guard rootIndex == 0 else { return composeViewController }
        let host = NativeRootScrollViewController(contentViewController: composeViewController)
        nativeScrollCoordinator.host = host
        return host
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
