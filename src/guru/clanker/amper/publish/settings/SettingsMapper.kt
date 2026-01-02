package guru.clanker.amper.publish.settings

import guru.clanker.amper.publish.domain.model.*

object SettingsMapper {

    fun toCoordinates(settings: PublishingSettings): Coordinates {
        return Coordinates(
            groupId = settings.groupId,
            artifactId = settings.artifactId,
            version = settings.version
        )
    }

    fun toRepository(settings: RepositorySettings): Repository {
        return when (settings.type.lowercase()) {
            "central" -> Repository.CentralPortal(
                id = settings.id,
                url = settings.url.ifEmpty { "https://central.sonatype.com/api/v1/publisher/upload" },
                token = settings.credentials?.password ?: System.getenv("CENTRAL_TOKEN") ?: "",
                publishingType = Repository.CentralPortal.PublishingType.AUTOMATIC
            )
            "github" -> Repository.GitHubPackages(
                id = settings.id,
                url = settings.url,
                token = settings.credentials?.password ?: "",
                owner = extractGitHubOwner(settings.url),
                repository = extractGitHubRepo(settings.url)
            )
            "local" -> Repository.Local(
                id = settings.id,
                url = settings.url
            )
            else -> Repository.Maven(
                id = settings.id,
                url = settings.url,
                credentials = settings.credentials?.let {
                    Credentials(it.username, it.password)
                },
                isSnapshot = settings.url.contains("snapshot", ignoreCase = true)
            )
        }
    }

    fun toPomMetadata(settings: PomSettings): PomMetadata {
        return PomMetadata(
            name = settings.name,
            description = settings.description,
            url = settings.url,
            licenses = settings.licenses.map { License(it.name, it.url) },
            developers = settings.developers.map {
                Developer(it.id.ifEmpty { it.name.lowercase().replace(" ", "") }, it.name, it.email)
            },
            scm = Scm(
                url = settings.scm.url,
                connection = settings.scm.connection,
                developerConnection = settings.scm.developerConnection.ifEmpty { null }
            )
        )
    }

    private fun extractGitHubOwner(url: String): String {
        return url.removePrefix("https://maven.pkg.github.com/")
            .split("/").getOrNull(0) ?: ""
    }

    private fun extractGitHubRepo(url: String): String {
        return url.removePrefix("https://maven.pkg.github.com/")
            .split("/").getOrNull(1) ?: ""
    }
}
