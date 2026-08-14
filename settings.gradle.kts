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
    // Kotlin/Wasm registers version-pinned Ivy repositories for browser
    // toolchains such as Binaryen. They are not Maven application dependencies.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
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
