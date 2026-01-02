package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface PomSettings {
    val name: String
    val description: String
    val url: String
    val licenses: List<LicenseSettings>
    val developers: List<DeveloperSettings>
    val scm: ScmSettings
}

@Configurable
interface LicenseSettings {
    val name: String
    val url: String
}

@Configurable
interface DeveloperSettings {
    val id: String get() = ""
    val name: String
    val email: String get() = ""
}

@Configurable
interface ScmSettings {
    val url: String
    val connection: String
    val developerConnection: String get() = ""
}
