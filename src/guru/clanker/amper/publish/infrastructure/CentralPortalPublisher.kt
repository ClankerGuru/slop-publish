package guru.clanker.amper.publish.infrastructure

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.maven.resolver.ChecksumGenerator
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

class CentralPortalPublisher : PublishingService {

    override fun publish(publication: Publication, repository: Repository): PublishingResult {
        if (repository !is Repository.CentralPortal) {
            return PublishingResult.Failure(
                publication,
                repository,
                PublishingError.ValidationError("CentralPortalPublisher requires Repository.CentralPortal")
            )
        }

        val tempDir = Files.createTempDirectory("central-bundle")
        try {
            publication.artifacts.forEach { artifact ->
                if (!artifact.file.toFile().exists()) {
                    return PublishingResult.Failure(
                        publication,
                        repository,
                        PublishingError.ValidationError("Artifact not found: ${artifact.file}")
                    )
                }
            }
            
            val bundleZip = try {
                createBundle(publication, tempDir)
            } catch (e: Exception) {
                return PublishingResult.Failure(
                    publication,
                    repository,
                    PublishingError.ValidationError("Bundle creation failed: ${e::class.simpleName}: ${e.message}")
                )
            }
            
            return uploadBundle(bundleZip, publication, repository)
        } catch (e: Exception) {
            val cause = e.cause?.let { " caused by ${it::class.simpleName}: ${it.message}" } ?: ""
            return PublishingResult.Failure(
                publication,
                repository,
                PublishingError.NetworkError("Upload failed: ${e::class.simpleName}: ${e.message}$cause", e)
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    override fun validate(publication: Publication, repository: Repository): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        if (repository !is Repository.CentralPortal) {
            errors.add(ValidationError("repository", "Must be CentralPortal type", ValidationError.Severity.ERROR))
        }
        
        if (publication.pomMetadata == null) {
            errors.add(ValidationError("pom", "POM metadata required for Central", ValidationError.Severity.ERROR))
        }
        
        val hasSignatures = publication.artifacts.any { it.extension.endsWith(".asc") }
        if (!hasSignatures) {
            errors.add(ValidationError("signing", "Signatures required for Central", ValidationError.Severity.ERROR))
        }
        
        return errors
    }

    private fun createBundle(publication: Publication, tempDir: Path): Path {
        val coords = publication.coordinates
        val groupPath = coords.groupId.replace('.', '/')
        val artifactDir = tempDir.resolve("$groupPath/${coords.artifactId}/${coords.version}")
        Files.createDirectories(artifactDir)

        publication.artifacts.forEach { artifact ->
            val targetName = buildArtifactName(coords, artifact)
            val targetPath = artifactDir.resolve(targetName)
            Files.copy(artifact.file, targetPath, StandardCopyOption.REPLACE_EXISTING)
            
            if (!artifact.extension.endsWith(".asc") && 
                !artifact.extension.endsWith(".md5") && 
                !artifact.extension.endsWith(".sha1")) {
                generateChecksums(targetPath)
            }
        }

        val bundleZip = tempDir.resolve("bundle.zip")
        val rootDir = tempDir.resolve(groupPath.split("/").first())
        createZip(bundleZip, rootDir)
        return bundleZip
    }

    private fun buildArtifactName(coords: Coordinates, artifact: Artifact): String {
        val classifier = artifact.classifier?.let { "-$it" } ?: ""
        return "${coords.artifactId}-${coords.version}$classifier.${artifact.extension}"
    }

    private fun generateChecksums(file: Path) {
        val checksum = ChecksumGenerator.generate(file)
        file.resolveSibling("${file.fileName}.md5").writeText(checksum.md5)
        file.resolveSibling("${file.fileName}.sha1").writeText(checksum.sha1)
    }

    private fun createZip(zipPath: Path, rootDir: Path) {
        ZipOutputStream(FileOutputStream(zipPath.toFile())).use { zos ->
            Files.walk(rootDir).use { paths ->
                paths.filter { it.isRegularFile() }
                    .forEach { file ->
                        val entryName = rootDir.parent.relativize(file).toString().replace("\\", "/")
                        zos.putNextEntry(ZipEntry(entryName))
                        Files.copy(file, zos)
                        zos.closeEntry()
                    }
            }
        }
    }

    private fun uploadBundle(
        bundleZip: Path,
        publication: Publication,
        repository: Repository.CentralPortal
    ): PublishingResult = runBlocking {
        val client = HttpClient(CIO)
        try {
            val coords = publication.coordinates
            val name = "${coords.artifactId}-${coords.version}"
            val publishingType = repository.publishingType.name
            
            val response = client.submitFormWithBinaryData(
                url = "${repository.url}?name=$name&publishingType=$publishingType",
                formData = formData {
                    append("bundle", bundleZip.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"bundle.zip\"")
                        append(HttpHeaders.ContentType, "application/zip")
                    })
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer ${repository.token}")
            }

            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val checksum = ChecksumGenerator.generate(bundleZip)
                PublishingResult.Success(
                    publication,
                    repository,
                    publication.artifacts
                        .filter { !it.extension.endsWith(".asc") }
                        .map { artifact ->
                            PublishedArtifact(
                                artifact,
                                "https://central.sonatype.com/artifact/${coords.groupId}/${coords.artifactId}/${coords.version}",
                                checksum
                            )
                        }
                )
            } else {
                PublishingResult.Failure(
                    publication,
                    repository,
                    PublishingError.NetworkError(
                        "HTTP ${response.status.value} ${response.status.description}: $body",
                        null
                    )
                )
            }
        } finally {
            client.close()
        }
    }
}
