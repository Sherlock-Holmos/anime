pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "anime"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app:android")
include(":shared:app")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:navigation")
include(":core:database")
include(":core:network")
include(":core:testing")

include(":data:catalog")
include(":data:collection")
include(":data:comment")
include(":data:session")
include(":data:settings")

include(":feature:discover")
include(":feature:search")
include(":feature:subject")
include(":feature:collection")
include(":feature:comment")
include(":feature:profile")
include(":feature:settings")
include(":feature:diagnostics")

include(":benchmark")
