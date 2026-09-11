plugins {
    id("anime.kmp.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.common)
        implementation(projects.core.model)
        implementation(projects.core.designsystem)
        implementation(projects.core.navigation)
        implementation(projects.data.settings)
        implementation(projects.data.session)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
    }
}
