plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "site.jokersh.anime.benchmark"
    compileSdk = 37
    targetProjectPath = ":app:android"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
}
