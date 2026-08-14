plugins {
    id("anime.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(projects.core.common)
        implementation(projects.core.network)
    }
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
}
