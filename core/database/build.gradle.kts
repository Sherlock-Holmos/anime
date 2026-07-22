plugins {
    id("anime.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

sqldelight {
    databases {
        create("AnimeDatabase") {
            packageName.set("site.jokersh.anime.core.database")
        }
    }
}
