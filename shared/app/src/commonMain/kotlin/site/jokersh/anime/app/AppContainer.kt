package site.jokersh.anime.app

import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.data.catalog.FixtureCatalogRepository
import site.jokersh.anime.data.catalog.FixtureSearchRepository
import site.jokersh.anime.data.catalog.SearchRepository

class AppContainer internal constructor(
    val profile: BuildProfile,
    val catalogRepository: CatalogRepository,
    val searchRepository: SearchRepository,
)

fun createAppContainer(profile: BuildProfile): AppContainer =
    AppContainer(
        profile = profile,
        catalogRepository = FixtureCatalogRepository(),
        searchRepository = FixtureSearchRepository(),
    )
