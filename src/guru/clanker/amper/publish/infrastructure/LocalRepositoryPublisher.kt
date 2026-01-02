package guru.clanker.amper.publish.infrastructure

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.maven.resolver.ChecksumGenerator
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LocalRepositoryPublisher(
    private val localRepoPath: Path = defaultLocalRepo()
) : PublishingService {

    override fun publish(publication: Publication, repository: Repository): PublishingResult {
        val basePath = resolveArtifactPath(publication.coordinates)
        
        return try {
            Files.createDirectories(basePath)
            val publishedArtifacts = mutableListOf<PublishedArtifact>()

            publication.artifacts.forEach { artifact ->
                val targetPath = basePath.resolve(artifactFileName(publication.coordinates, artifact))
                Files.copy(artifact.file, targetPath, StandardCopyOption.REPLACE_EXISTING)
                writeChecksums(targetPath)

                publishedArtifacts.add(
                    PublishedArtifact(
                        artifact = artifact,
                        remoteUrl = targetPath.toUri().toString(),
                        checksum = ChecksumGenerator.generate(targetPath)
                    )
                )
            }

            updateMetadata(publication.coordinates)

            PublishingResult.Success(publication, repository, publishedArtifacts)
        } catch (e: IOException) {
            PublishingResult.Failure(
                publication,
                repository,
                PublishingError.RepositoryError("Failed to write to local repository: ${e.message}", null)
            )
        }
    }

    override fun validate(publication: Publication, repository: Repository): List<ValidationError> {
        return emptyList()
    }

    private fun resolveArtifactPath(coordinates: Coordinates): Path {
        return localRepoPath.resolve(coordinates.toPath())
    }

    private fun artifactFileName(coordinates: Coordinates, artifact: Artifact): String {
        val classifier = artifact.classifier?.let { "-$it" } ?: ""
        return "${coordinates.artifactId}-${coordinates.version}$classifier.${artifact.extension}"
    }

    private fun writeChecksums(file: Path) {
        val checksums = ChecksumGenerator.generate(file)
        Files.writeString(file.resolveSibling("${file.fileName}.md5"), checksums.md5)
        Files.writeString(file.resolveSibling("${file.fileName}.sha1"), checksums.sha1)
    }

    private fun updateMetadata(coordinates: Coordinates) {
        val metadataPath = localRepoPath
            .resolve(coordinates.groupId.replace('.', '/'))
            .resolve(coordinates.artifactId)
            .resolve("maven-metadata-local.xml")

        Files.createDirectories(metadataPath.parent)

        val existingVersions = if (Files.exists(metadataPath)) {
            parseExistingVersions(metadataPath)
        } else {
            mutableListOf()
        }

        if (coordinates.version !in existingVersions) {
            existingVersions.add(coordinates.version)
        }

        val latestVersion = existingVersions.maxOrNull() ?: coordinates.version
        val releaseVersion = existingVersions.filter { !it.endsWith("-SNAPSHOT") }.maxOrNull()

        val metadata = buildMetadataXml(coordinates, existingVersions, latestVersion, releaseVersion)
        Files.writeString(metadataPath, metadata)
    }

    private fun parseExistingVersions(metadataPath: Path): MutableList<String> {
        val content = Files.readString(metadataPath)
        val versions = mutableListOf<String>()
        val regex = Regex("<version>([^<]+)</version>")
        regex.findAll(content).forEach { match ->
            versions.add(match.groupValues[1])
        }
        return versions
    }

    private fun buildMetadataXml(
        coordinates: Coordinates,
        versions: List<String>,
        latest: String,
        release: String?
    ): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("<metadata>")
            appendLine("  <groupId>${coordinates.groupId}</groupId>")
            appendLine("  <artifactId>${coordinates.artifactId}</artifactId>")
            appendLine("  <versioning>")
            appendLine("    <latest>$latest</latest>")
            if (release != null) {
                appendLine("    <release>$release</release>")
            }
            appendLine("    <versions>")
            versions.sorted().forEach { v ->
                appendLine("      <version>$v</version>")
            }
            appendLine("    </versions>")
            appendLine("    <lastUpdated>${formatLastUpdated()}</lastUpdated>")
            appendLine("  </versioning>")
            appendLine("</metadata>")
        }
    }

    private fun formatLastUpdated(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        return Instant.now().atZone(ZoneOffset.UTC).format(formatter)
    }

    companion object {
        fun defaultLocalRepo(): Path = Path.of(System.getProperty("user.home"), ".m2", "repository")
    }
}
