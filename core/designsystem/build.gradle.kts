plugins {
    id("anime.kmp.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(projects.core.common)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.coil.compose)
        }
        androidMain.dependencies {
            implementation(libs.coil.network.ktor)
            implementation(libs.backdrop)
        }
    }
}
