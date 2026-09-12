import SwiftUI
import AnimeShared

@main
struct AnimeIosApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // The SwiftUI host must also be edge-to-edge. Applying this only to the
                // individual Compose tab leaves the hosting controller's top safe-area strip
                // opaque, which is exactly the white/dark rectangle visible above the page.
                .ignoresSafeArea(.container, edges: [.top, .bottom])
                .onOpenURL { url in
                    _ = IosBridge.shared.handleOpenUrl(rawUrl: url.absoluteString)
                }
        }
    }
}
