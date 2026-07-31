import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val moduleNamespace =
    project.path
        .split(':')
        .filter(String::isNotBlank)
        .joinToString(".")
        .let { suffix -> "site.jokersh.anime.$suffix" }

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_2_4)
            apiVersion.set(KotlinVersion.KOTLIN_2_4)
            allWarningsAsErrors.set(true)
        }
    }

    android {
        namespace = moduleNamespace
        compileSdk = 37
        minSdk = 26

        // AGP 9's Android-KMP library plugin keeps Android resource processing
        // disabled by default. Compose Multiplatform resources are published
        // through that pipeline, so consumers would otherwise compile against
        // generated Res accessors while the APK is missing their asset payload.
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_2_4)
            apiVersion.set(KotlinVersion.KOTLIN_2_4)
            allWarningsAsErrors.set(true)
        }

        withHostTest {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    if (
        project.path == ":core:model" ||
        project.path == ":core:designsystem" ||
        project.path.startsWith(":data:")
    ) {
        explicitApi()
    }
}
