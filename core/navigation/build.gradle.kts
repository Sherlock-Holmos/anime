@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    id("anime.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    wasmJs {
        browser {
            testTask {
                // Navigation tests are pure common logic and already run on Desktop/Android.
                // Navigation3 currently pulls the Skiko browser runtime into the Wasm test bundle,
                // where its generated relative skiko.mjs import cannot be resolved by Karma.
                // Keep compiling the Wasm tests, but do not treat this upstream runner issue as a
                // product test failure.
                enabled = false
            }
        }
    }

    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        implementation(projects.core.common)
        api(libs.navigation3.ui)
        implementation(libs.savedstate)
        implementation(libs.kotlinx.serialization.json)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}
