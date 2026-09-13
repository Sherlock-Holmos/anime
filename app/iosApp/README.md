# Anime iOS host

`iosApp` is a SwiftUI host for `AnimeShared.framework`. The iOS App Shell, four root tabs,
subject detail, ratings, collection actions, community comments, account flows and service
diagnostics are native SwiftUI. Repositories, authentication, caching and domain state remain
in KMP and are exposed through the Swift-friendly `IosNativeAppFacade`.

Android, Desktop and Web continue to use the Compose Multiplatform UI. iOS no longer embeds a
Compose root for its main navigation; this keeps navigation, safe-area behavior, scroll-edge
materials, sheets and system controls under SwiftUI while preserving the shared business layer.

Requirements:

- macOS with the current stable Xcode and an Apple Silicon simulator;
- JDK 17 available to Gradle;
- a development team selected for `site.jokersh.anime` when running on device.

Open `iosApp.xcodeproj`. The first build invokes
`:shared:app:embedAndSignAppleFrameworkForXcode`, which builds and embeds the matching
simulator or device framework. Bangumi authorization uses the system
`ASWebAuthenticationSession` sheet and returns through `anime://bangumi-auth`; access
and refresh tokens are stored in Keychain, while non-sensitive caches use
`NSUserDefaults`.

Command-line verification on macOS:

```sh
./gradlew :shared:app:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build
```
