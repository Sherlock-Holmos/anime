package site.jokersh.anime.app

class AppContainer internal constructor(
    val profile: BuildProfile,
)

fun createAppContainer(profile: BuildProfile): AppContainer = AppContainer(profile)
