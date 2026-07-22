plugins {
    id("anime.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        implementation(projects.core.common)
        api(libs.navigation3.ui)
        implementation(libs.savedstate)
        implementation(libs.kotlinx.serialization.json)
    }
}
