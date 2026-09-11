pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/central") {
            name = "AliyunCentralMirror"
            content {
                includeGroupByRegex("org\\.jetbrains\\.kotlin.*")
                includeGroupByRegex("org\\.jetbrains\\.androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Kotlin/Wasm registers the version-pinned Binaryen Ivy repository at project
    // level. PREFER_SETTINGS intentionally ignores project repositories, so keep
    // the official distribution repository here as well. The content filter keeps
    // it isolated from normal Maven dependency resolution.
    // The settings-owned repositories also prevent host-level Gradle init scripts
    // from shadowing Maven Central with incomplete mirrors (notably iOS variants).
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/central") {
            name = "AliyunCentralMirror"
            content {
                includeGroupByRegex("org\\.jetbrains\\.kotlin.*")
                includeGroupByRegex("org\\.jetbrains\\.androidx.*")
            }
        }
        mavenCentral()
        ivy {
            name = "KotlinWasmBinaryen"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout {
                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.github.webassembly", "binaryen")
            }
        }
    }
}

rootProject.name = "anime"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app:android")
include(":app:desktop")
include(":app:web")
include(":shared:app")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:navigation")
include(":core:database")
include(":core:network")
include(":core:testing")
include(":core:vendor:kyantLiquidTabs")
project(":core:vendor:kyantLiquidTabs").projectDir = file("core/vendor/kyant-liquid-tabs")

include(":data:catalog")
include(":data:collection")
include(":data:comment")
include(":data:session")
include(":data:settings")

include(":feature:discover")
include(":feature:activity")
include(":feature:community")
include(":feature:search")
include(":feature:subject")
include(":feature:collection")
include(":feature:comment")
include(":feature:profile")
include(":feature:settings")
include(":feature:diagnostics")

include(":benchmark")
