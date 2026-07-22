import groovy.json.JsonSlurper
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Bundling
import java.security.MessageDigest

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
    description = "Validates Fixture schema, hashes, enums, references, comment depth, and scenario coverage."

    val fixtureDirectory = layout.projectDirectory.dir("fixtures/v1")
    inputs.dir(fixtureDirectory)

    doLast {
        fun parse(name: String): Map<*, *> = JsonSlurper().parse(fixtureDirectory.file(name).asFile) as Map<*, *>

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun Map<*, *>.long(key: String): Long = (get(key) as Number).toLong()

        val expectedSchema = "anime.fixture/v1"
        val manifest = parse("manifest.json")
        check(manifest["schema"] == expectedSchema)
        check(manifest["clock"] == "2026-07-19T08:00:00Z")
        check((manifest["seed"] as Number).toLong() == 20_260_719L)
        check(manifest["defaultScenario"] == "happy")

        val declaredFiles = manifest["files"] as Map<*, *>
        check(
            declaredFiles.keys ==
                setOf(
                    "subjects.json",
                    "discovery.json",
                    "search.json",
                    "collections.json",
                    "comments.json",
                    "users.json",
                    "scenarios.json",
                ),
        )
        declaredFiles.forEach { (name, expectedHash) ->
            val file = fixtureDirectory.file(name as String).asFile
            check(file.isFile) { "Fixture file is missing: $name" }
            check(sha256(file) == expectedHash) { "Fixture digest mismatch: $name" }
            check(parse(name)["schema"] == expectedSchema) { "Fixture schema mismatch: $name" }
        }

        val subjects = parse("subjects.json")["subjects"] as List<Map<*, *>>
        val subjectIds = subjects.map { it.long("id") }
        check(subjectIds == (1001L..1012L).toList()) { "Fixture subjects must be the ordered IDs 1001..1012" }
        check(subjectIds.distinct().size == subjectIds.size)
        val subjectTypes = setOf("Tv", "Web", "Ova", "Movie", "Other")
        val airingStatuses = setOf("Announced", "Airing", "Finished", "Unknown")
        subjects.forEach { subject ->
            check(subject["type"] in subjectTypes)
            check(subject["airingStatus"] in airingStatuses)
            val votes = (subject["votes"] as Number).toInt()
            val score = subject["score"] as Number?
            check(votes >= 0 && (score == null || score.toDouble() in 0.0..10.0))
            check(votes != 0 || score == null)
        }

        val discovery = parse("discovery.json")["sections"] as List<Map<*, *>>
        val discoveryIds = discovery.flatMap { it["subjectIds"] as List<Number> }.map(Number::toLong)
        check(discoveryIds.all(subjectIds::contains)) { "Discovery references an unknown subject" }

        val search = parse("search.json")
        check((search["pageSize"] as Number).toInt() == 5)
        val searchIds =
            (search["expected"] as Map<*, *>)
                .values
                .flatMap { it as List<Number> }
                .map(Number::toLong)
        check(searchIds.all(subjectIds::contains)) { "Search references an unknown subject" }

        val collectionStatuses = setOf("Wish", "Watching", "Completed", "OnHold", "Dropped")
        val syncPhases = setOf("Synced", "Pending", "Syncing", "Failed", "Conflict")
        val collections = parse("collections.json")["collections"] as List<Map<*, *>>
        collections.forEach { collection ->
            check(collection.long("subjectId") in subjectIds)
            check(collection["status"] in collectionStatuses)
            check(collection["sync"] in syncPhases)
            val watched = (collection["watchedEpisodes"] as Number).toInt()
            val total = (collection["totalEpisodes"] as Number?)?.toInt()
            check(watched >= 0 && (total == null || watched <= total))
        }

        val comments = parse("comments.json")["comments"] as List<Map<*, *>>
        check(comments.size == 18) { "Fixture must contain exactly 18 comments" }
        val commentsById = comments.associateBy { it["id"] as String }
        comments.forEach { comment ->
            check(comment.long("subjectId") in subjectIds)
            val parentId = comment["parentId"] as String?
            if (parentId != null) {
                val parent = checkNotNull(commentsById[parentId]) { "Missing comment parent: $parentId" }
                check(parent["parentId"] == null) { "Fixture comments cannot be nested beyond one reply level" }
                check(parent.long("subjectId") == comment.long("subjectId"))
            }
        }

        val expectedScenarios =
            setOf(
                "happy",
                "cold-slow",
                "refresh-slow",
                "empty-search",
                "offline-cached",
                "offline-empty",
                "server-error",
                "rate-limited",
                "unauthorized",
                "write-retry",
                "collection-conflict",
                "corrupted-fixture",
            )
        val scenarios = parse("scenarios.json")["scenarios"] as List<Map<*, *>>
        check(scenarios.map { it["id"] }.toSet() == expectedScenarios)
        scenarios.forEach { scenario ->
            check(scenario.containsKey("readDelayMs"))
            check(scenario.containsKey("writeDelayMs"))
            check(scenario.containsKey("network"))
            check(scenario["failurePlan"] is List<*>)
        }
    }
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
