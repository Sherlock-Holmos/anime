import java.security.MessageDigest

plugins {
    id("anime.kmp.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val verifyUpstreamSources =
    tasks.register("verifyUpstreamSources") {
        group = "verification"
        description = "Verifies vendored Kyant LiquidBottomTabs sources against upstream commit bebb11a9."

        val expected =
            mapOf(
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTab.kt" to
                    "40178a5ab429022e4e5523a39b9feb40c8c04370671361b24ad4ee992eaaeeb0",
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt" to
                    "4dfbafaf008b058bf71ceebbc568c900f7a74acaf94f2fe774c6155367ff0155",
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/DampedDragAnimation.kt" to
                    "8310075ee00a9b5021935f6da1f016cb38eac61a527d27f5318c34250402c44e",
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/DragGestureInspector.kt" to
                    "01548ab5604d89bd9a8ee66a69d716036e94559c842461fbee4afc97915770c1",
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/InteractiveHighlight.kt" to
                    "d0af46b531eba19bd0fed886892f02193f05d5a61db0925da016ff6059984f83",
                "src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/Coroutines.kt" to
                    "cd992dff147b221cfa3bcadce0116f8c58a3bbebc54b2c277cdb184c24eb8bf9",
                "src/androidMain/kotlin/com/kyant/backdrop/catalog/utils/Coroutines.kt" to
                    "2ac3e455c3ac82aa06efc3a71697db541278002089703e95091f35597a2b43ba",
            )

        inputs.files(expected.keys.map(::file))
        doLast {
            expected.forEach { (path, expectedHash) ->
                val normalized = file(path).readText().replace("\r\n", "\n").trimEnd() + "\n"
                val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
                val actualHash = digest.joinToString("") { "%02x".format(it) }
                check(actualHash == expectedHash) {
                    "Vendored Kyant source differs from upstream bebb11a9: $path"
                }
            }
        }
    }

tasks.named("check") {
    dependsOn(verifyUpstreamSources)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.backdrop)
            implementation(libs.kyant.shapes)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
