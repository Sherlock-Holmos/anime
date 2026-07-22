plugins { id("anime.kmp.library") }

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        implementation(projects.core.common)
        implementation(projects.core.database)
        implementation(projects.core.network)
    }
}
