import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Bundling

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}

group = "site.jokersh.anime"
version = "0.1.0-SNAPSHOT"

val ktlint by configurations.creating {
    attributes.attribute(
        Bundling.BUNDLING_ATTRIBUTE,
        objects.named(Bundling.SHADOWED),
    )
}

dependencies {
    ktlint(libs.ktlint.cli)
}

tasks.register<JavaExec>("animeKtlintCheck") {
    group = "verification"
    description = "Checks Kotlin and Kotlin DSL formatting."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**")
}

tasks.register<JavaExec>("animeFormat") {
    group = "formatting"
    description = "Formats Kotlin and Kotlin DSL sources."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", "**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**")
}

tasks.register("animeCheck") {
    group = "verification"
    description = "Runs the stable Anime verification entry point."
    dependsOn("animeKtlintCheck")
    dependsOn("checkModuleGraph")
    dependsOn("fixtureCheck")
}

val checkModuleGraph =
    tasks.register("checkModuleGraph") {
        group = "verification"
        description = "Checks direct project dependencies against the Wiki module boundaries."
    }

tasks.register("fixtureCheck") {
    group = "verification"
    description = "Stable Fixture verification entry point; schema checks are added during F2."
}

gradle.projectsEvaluated {
    val projectEdges =
        subprojects
            .flatMap { source ->
                source.configurations.flatMap { configuration ->
                    configuration.dependencies
                        .withType(ProjectDependency::class.java)
                        .map { dependency: ProjectDependency -> Pair(source.path, dependency.path) }
                        .filter { (sourcePath, targetPath) -> sourcePath != targetPath }
                }
            }.distinct()
            .sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })

    val violations =
        projectEdges.filterNot { (source, target) ->
            when {
                source == ":app:android" -> {
                    target == ":shared:app"
                }

                source == ":shared:app" -> {
                    target.startsWith(":core:") ||
                        target.startsWith(":data:") ||
                        target.startsWith(":feature:")
                }

                source.startsWith(":feature:") -> {
                    target.startsWith(":core:") || target.startsWith(":data:")
                }

                source.startsWith(":data:") -> {
                    target.startsWith(":core:")
                }

                source.startsWith(":core:") -> {
                    target.startsWith(":core:")
                }

                else -> {
                    true
                }
            }
        }

    checkModuleGraph.configure {
        inputs.property("projectEdges", projectEdges.map { "${it.first}->${it.second}" })
        doLast {
            check(violations.isEmpty()) {
                "Forbidden project dependencies:\n${violations.joinToString("\n") { "${it.first} -> ${it.second}" }}"
            }
        }
    }

    tasks.named("animeCheck") {
        dependsOn(subprojects.mapNotNull { it.tasks.findByName("check") })
    }
}

tasks.register("animeScreenshot") {
    group = "verification"
    description = "Stable screenshot-test entry point; test suites are added during F1."
}

tasks.register("animeUiTest") {
    group = "verification"
    description = "Stable Android UI-test entry point; requires a connected device."
}

tasks.register("animeBenchmark") {
    group = "verification"
    description = "Stable Macrobenchmark entry point; benchmark cases are added during F10."
}
