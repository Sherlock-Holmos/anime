import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(projects.shared.app)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "site.jokersh.anime.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Anime"
            packageVersion = "0.1.0"
            description = "Anime discovery and collection client"
            vendor = "Jokersh"

            windows {
                menuGroup = "Anime"
                upgradeUuid = "761c29cf-f1fd-4cb6-a184-6464d8f9e0e6"
            }
        }
    }
}
