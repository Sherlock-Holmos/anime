import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val configuredApiBaseUrl = providers.gradleProperty("ANIME_API_BASE_URL").orElse("").get()
val apiBaseUrlLiteral = "\"${configuredApiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""

extensions.configure<ApplicationExtension> {
    namespace = "site.jokersh.anime"
    compileSdk = 37

    defaultConfig {
        applicationId = "site.jokersh.anime"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("String", "ANIME_ENVIRONMENT", "\"Demo\"")
            buildConfigField("String", "ANIME_DATA_MODE_POLICY", "\"FixtureOnly\"")
            buildConfigField("String", "ANIME_API_BASE_URL", "\"\"")
            buildConfigField("boolean", "ANIME_DIAGNOSTICS_ENABLED", "true")
            buildConfigField("int", "ANIME_SEARCH_PAGE_SIZE", "5")
        }
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ANIME_ENVIRONMENT", "\"Dev\"")
            buildConfigField("String", "ANIME_DATA_MODE_POLICY", "\"RemoteWithFixtureSwitch\"")
            buildConfigField("String", "ANIME_API_BASE_URL", apiBaseUrlLiteral)
            buildConfigField("boolean", "ANIME_DIAGNOSTICS_ENABLED", "true")
            buildConfigField("int", "ANIME_SEARCH_PAGE_SIZE", "20")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "ANIME_ENVIRONMENT", "\"Prod\"")
            buildConfigField("String", "ANIME_DATA_MODE_POLICY", "\"RemoteOnly\"")
            buildConfigField("String", "ANIME_API_BASE_URL", apiBaseUrlLiteral)
            buildConfigField("boolean", "ANIME_DIAGNOSTICS_ENABLED", "false")
            buildConfigField("int", "ANIME_SEARCH_PAGE_SIZE", "20")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes +=
            setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST",
            )
    }

    lint {
        warningsAsErrors = true
        // Versions are intentionally locked by FEB-2026-07-21 and move only through an ADR.
        disable +=
            setOf(
                "AndroidGradlePluginVersion",
                "NewerVersionAvailable",
                // AAPT still requires adaptive-icon XML in a v26-qualified resource directory.
                "ObsoleteSdkInt",
            )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        val environment =
            variantBuilder.productFlavors
                .firstOrNull { it.first == "environment" }
                ?.second
        variantBuilder.enable =
            when (environment) {
                "demo", "dev" -> variantBuilder.buildType == "debug"
                "prod" -> variantBuilder.buildType == "release"
                else -> false
            }
    }
}

dependencies {
    implementation(projects.shared.app)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

val verifyDemoDebugRuntimeResources by
    tasks.registering {
        group = "verification"
        description = "Verifies that required Compose Multiplatform resources are packaged in the demo APK."
        dependsOn("packageDemoDebug")

        val apkFile = layout.buildDirectory.file("outputs/apk/demo/debug/android-demo-debug.apk")
        inputs.file(apkFile)

        doLast {
            val requiredEntries =
                setOf(
                    "assets/composeResources/site.jokersh.anime.app.generated.resources/values/strings.commonMain.cvr",
                    "assets/composeResources/site.jokersh.anime.app.generated.resources/drawable/ic_explore.xml",
                )

            ZipFile(apkFile.get().asFile).use { apk ->
                val missingEntries = requiredEntries.filter { apk.getEntry(it) == null }
                check(missingEntries.isEmpty()) {
                    "Demo APK is missing required Compose resources: ${missingEntries.joinToString()}"
                }
            }
        }
    }

tasks.configureEach {
    if (name == "assembleDemoDebug") {
        dependsOn(verifyDemoDebugRuntimeResources)
    }
}
