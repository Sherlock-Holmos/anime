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
        implementation(projects.data.collection)
        implementation(projects.data.session)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.compose.runtime)
    }
}
