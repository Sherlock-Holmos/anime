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
    implementation(projects.core.model)
    implementation(projects.data.catalog)
    implementation(projects.data.comment)
    implementation(projects.data.session)
    implementation(projects.data.settings)
    implementation(projects.data.collection)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jna)
    implementation(libs.ktor.client.okhttp)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "site.jokersh.anime.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Anime"
            packageVersion = "0.1.6"
            description = "Anime discovery and collection client"
            vendor = "Jokersh"

            windows {
                iconFile.set(project.file("src/main/resources/app-icon.ico"))
                menuGroup = "Anime"
                upgradeUuid = "761c29cf-f1fd-4cb6-a184-6464d8f9e0e6"
            }
        }
    }
}

// jpackage places jvm.dll under runtime/bin/server while its bundled VC/UCRT
// dependencies live in runtime/bin. Some Windows installations do not include
// that parent directory in the launcher DLL search path and fail with Win32 126.
// Put the already bundled runtime DLLs beside jvm.dll before packaging.
tasks.matching { it.name == "createRuntimeImage" }.configureEach {
    doLast {
        copy {
            from(layout.buildDirectory.dir("compose/tmp/main/runtime/bin")) {
                include("*.dll")
            }
            into(layout.buildDirectory.dir("compose/tmp/main/runtime/bin/server"))
        }
    }
}
