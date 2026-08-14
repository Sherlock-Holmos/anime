# Anime iOS host

`iosApp` is a thin SwiftUI host for `AnimeShared.framework`. Product UI, navigation,
repositories and state remain in Kotlin `commonMain`.

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
