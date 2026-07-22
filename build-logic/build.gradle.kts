plugins {
    `kotlin-dsl`
}

group = "site.jokersh.anime.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.android.tools.build:gradle:9.3.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
