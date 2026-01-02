package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface SigningCredentialsSettings {
    val file: String get() = ""
    val keyIdKey: String get() = "guru.clanker.gpg.key.id"
    val keyKey: String get() = "guru.clanker.gpg.key.secret"
    val passwordKey: String get() = "guru.clanker.gpg.key.passphrase"
}
