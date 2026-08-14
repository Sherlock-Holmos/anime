plugins {
    id("anime.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        api(libs.kotlinx.coroutines.core)
        implementation(projects.core.common)
        implementation(projects.core.database)
        implementation(projects.core.network)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
    }
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
}
