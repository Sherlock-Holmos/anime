import SwiftUI
import AnimeShared

@main
struct AnimeIosApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = IosBridge.shared.handleOpenUrl(rawUrl: url.absoluteString)
                }
        }
    }
}
