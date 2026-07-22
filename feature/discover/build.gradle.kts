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
        implementation(projects.data.catalog)
        implementation(libs.lifecycle.viewmodel)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.runtime)
        implementation(libs.compose.resources)
        implementation(libs.compose.ui)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

compose.resources {
    packageOfResClass = "site.jokersh.anime.feature.discover.generated.resources"
}
