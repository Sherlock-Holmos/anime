plugins {
    id("anime.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.datetime)
    }
}
