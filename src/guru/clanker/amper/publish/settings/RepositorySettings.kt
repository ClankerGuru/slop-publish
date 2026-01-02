package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface RepositorySettings {
    val id: String
    val type: String get() = "maven"
    val url: String
    val credentials: CredentialsSettings? get() = null
}

@Configurable
interface CredentialsSettings {
    val username: String
    val password: String
}
