package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface SigningSettings {
    val enabled: Boolean get() = false
    val key: String get() = ""
    val password: String get() = ""
    val keyId: String get() = ""
    val skipForRepositories: List<String> get() = emptyList()
}
