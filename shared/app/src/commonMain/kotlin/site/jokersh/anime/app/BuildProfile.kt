package site.jokersh.anime.app

enum class Environment {
    Demo,
    Dev,
    Prod,
}

enum class DataModePolicy {
    FixtureOnly,
    RemoteWithFixtureSwitch,
    RemoteOnly,
}

data class BuildProfile(
    val environment: Environment,
    val dataModePolicy: DataModePolicy,
    val apiBaseUrl: String?,
    val diagnosticsEnabled: Boolean,
    val searchPageSize: Int,
) {
    init {
        require(searchPageSize in 1..50) { "searchPageSize must be in 1..50" }
        require(environment != Environment.Prod || dataModePolicy == DataModePolicy.RemoteOnly)
        require(environment != Environment.Demo || dataModePolicy == DataModePolicy.FixtureOnly)
    }
}
