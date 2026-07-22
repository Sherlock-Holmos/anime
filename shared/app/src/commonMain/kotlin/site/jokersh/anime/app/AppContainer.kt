package site.jokersh.anime.app

import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.data.catalog.FixtureCatalogRepository

class AppContainer internal constructor(
    val profile: BuildProfile,
    val catalogRepository: CatalogRepository,
)

fun createAppContainer(profile: BuildProfile): AppContainer =
    AppContainer(
        profile = profile,
        catalogRepository = FixtureCatalogRepository(),
    )
