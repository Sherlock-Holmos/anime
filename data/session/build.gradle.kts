plugins { id("anime.kmp.library") }

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        api(libs.kotlinx.coroutines.core)
        implementation(projects.core.common)
        implementation(projects.core.network)
    }
}
