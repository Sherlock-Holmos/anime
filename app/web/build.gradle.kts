@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val systemNodeCommand = providers.environmentVariable("NODE_BINARY").orElse("node").get()

rootProject.plugins.withType<WasmNodeJsPlugin> {
    rootProject.extensions.configure<WasmNodeJsEnvSpec> {
        download.set(false)
        command.set(systemNodeCommand)
    }
}

rootProject.plugins.withType<WasmNodeJsRootPlugin> {
    rootProject.extensions.configure<WasmNodeJsRootExtension> {
        // KGP 2.4 still reads these legacy root values in Webpack tasks, while
        // direct access is deprecated and warnings are errors in this project.
        javaClass.getMethod("setDownload", Boolean::class.javaPrimitiveType).invoke(this, false)
        javaClass.getMethod("setCommand", String::class.java).invoke(this, systemNodeCommand)
    }
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "anime.js"
            }
        }
        binaries.executable()

        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_4)
            apiVersion.set(KotlinVersion.KOTLIN_2_4)
            allWarningsAsErrors.set(true)
        }
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.shared.app)
            implementation(projects.core.model)
            implementation(projects.data.catalog)
            implementation(projects.data.comment)
            implementation(projects.data.session)
            implementation(projects.data.settings)
            implementation(projects.data.collection)
            implementation(libs.compose.ui)
            implementation(libs.ktor.client.js)
        }
    }
}

tasks.named("wasmJsAggregateResources") {
    // Gradle 9.5 cannot read POSIX file modes for empty dependency resource
    // directories extracted on Windows. Give those directories a temporary
    // regular file for the copy, then remove it from the aggregate output.
    doFirst {
        layout.buildDirectory
            .dir("kotlin-multiplatform-resources/resources-from-dependencies/wasmJs")
            .get()
            .asFile
            .walkBottomUp()
            .filter { it.isDirectory && it.listFiles()?.isEmpty() == true }
            .forEach { directory -> directory.resolve(".empty").createNewFile() }
    }
    doLast {
        layout.buildDirectory
            .dir("kotlin-multiplatform-resources/aggregated-resources/wasmJs")
            .get()
            .asFile
            .walkTopDown()
            .filter { it.isFile && it.name == ".empty" }
            .forEach(File::delete)
    }
}
