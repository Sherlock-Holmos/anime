plugins {
    id("anime.kmp.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.designsystem)
        implementation(projects.core.common)
        implementation(projects.core.model)
        api(projects.core.navigation)

        implementation(projects.data.catalog)
        implementation(projects.data.collection)
        implementation(projects.data.comment)
        implementation(projects.data.session)
        implementation(projects.data.settings)

        implementation(projects.feature.discover)
        implementation(projects.feature.activity)
        implementation(projects.feature.community)
        implementation(projects.feature.search)
        implementation(projects.feature.subject)
        implementation(projects.feature.collection)
        implementation(projects.feature.comment)
        implementation(projects.feature.profile)
        implementation(projects.feature.settings)
        implementation(projects.feature.diagnostics)

        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.resources)
        implementation(libs.compose.ui)
        implementation(libs.lifecycle.viewmodel.navigation3)
    }
}

compose.resources {
    packageOfResClass = "site.jokersh.anime.app.generated.resources"
}
