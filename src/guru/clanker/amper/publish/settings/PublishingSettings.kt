package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface PublishingSettings {
    val groupId: String
    val artifactId: String
    val version: String
    val artifacts: List<String> get() = listOf("jar", "sources", "javadoc")
    val repositories: List<RepositorySettings> get() = emptyList()
    val pom: PomSettings? get() = null
    val signing: SigningSettings? get() = null
    val targets: Map<String, List<String>> get() = emptyMap()
    val defaultTarget: String get() = "default"
}
