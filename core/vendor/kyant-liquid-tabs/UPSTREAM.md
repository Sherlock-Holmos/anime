# Kyant Liquid catalog components upstream

This module vendors the unmodified `LiquidBottomTabs`, `LiquidButton`, and `LiquidToggle` examples and their required helpers from:

- Repository: <https://github.com/Kyant0/AndroidLiquidGlass>
- Branch: `kmp`
- Commit: `bebb11a91bd97bf1dabde479f3b332ad9898731f`
- Release: `2.0.0`
- Source paths: `app/src/commonMain/.../components/LiquidBottomTab*.kt`, `LiquidButton.kt`, `LiquidToggle.kt`, and required `catalog/utils` files
- License: Apache License 2.0; see `LICENSE`

The upstream project publishes the low-level `backdrop` library but does not publish these high-level example components as a Maven artifact. This isolated module keeps the official implementation separate from Anime-owned adapters. Do not edit the vendored Kotlin files directly; update them by replacing them from a reviewed upstream commit.
