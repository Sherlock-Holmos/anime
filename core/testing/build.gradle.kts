plugins {
    id("anime.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.common)
        api(projects.core.model)
        api(libs.kotlinx.coroutines.test)
        implementation(libs.kotlinx.serialization.json)
    }
}
