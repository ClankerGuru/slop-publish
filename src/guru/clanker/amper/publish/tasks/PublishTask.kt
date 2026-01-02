package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.infrastructure.LocalRepositoryPublisher
import guru.clanker.amper.publish.infrastructure.MavenRepositoryPublisher
import guru.clanker.amper.publish.infrastructure.GitHubPackagesPublisher
import guru.clanker.amper.publish.maven.pom.DefaultPomGenerator
import guru.clanker.amper.publish.maven.signing.BouncyCastleSigner
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.exists

@TaskAction
fun publish(
    settings: PublishingSettings,
    @Input moduleDir: Path,
    target: String,
    dryRun: Boolean
) {
    val errors = SettingsValidator.validate(settings)
    if (errors.any { it.severity == ValidationError.Severity.ERROR }) {
        error("Configuration validation failed:\n${errors.joinToString("\n") { "  - ${it.message}" }}")
    }

    val targetRepoIds = settings.targets[target]
        ?: if (target == "default") settings.repositories.map { it.id }
        else error("Unknown target: $target. Available: ${settings.targets.keys.joinToString()}")

    val repositories = targetRepoIds.map { id ->
        settings.repositories.find { it.id == id }
            ?: error("Unknown repository: $id")
    }

    val coordinates = SettingsMapper.toCoordinates(settings)
    val artifacts = collectArtifacts(settings, moduleDir, coordinates)
    val pomMetadata = settings.pom?.let { SettingsMapper.toPomMetadata(it) }

    val publication = Publication(
        coordinates = coordinates,
        artifacts = artifacts,
        pomMetadata = pomMetadata
    )

    val pomGenerator = DefaultPomGenerator()
    val pomPath = moduleDir.resolve("build/pom.xml")
    val pomArtifact = pomGenerator.generateToFile(publication, pomPath)
    val publicationWithPom = publication.copy(artifacts = publication.artifacts + pomArtifact)

    repositories.forEach { repoSettings ->
        val repository = SettingsMapper.toRepository(repoSettings)

        val finalPublication = if (shouldSign(settings, repoSettings.id)) {
            val signer = BouncyCastleSigner(
                armoredKey = settings.signing?.key ?: "",
                passphrase = settings.signing?.password ?: ""
            )
            signer.signPublication(publicationWithPom)
        } else {
            publicationWithPom
        }

        if (dryRun) {
            println("[DRY RUN] Would publish to ${repository.id}:")
            println("[DRY RUN]   ${coordinates.groupId}:${coordinates.artifactId}:${coordinates.version}")
            finalPublication.artifacts.forEach { artifact ->
                println("[DRY RUN]   - ${artifact.file.fileName}")
            }
            return@forEach
        }

        val publisher = when (repository) {
            is Repository.Maven -> MavenRepositoryPublisher()
            is Repository.GitHubPackages -> GitHubPackagesPublisher()
            is Repository.Local -> LocalRepositoryPublisher()
        }

        println("Publishing ${coordinates.groupId}:${coordinates.artifactId}:${coordinates.version} to ${repository.id}")

        val result = publisher.publish(finalPublication, repository)
        handleResult(result)
    }
}

private fun shouldSign(settings: PublishingSettings, repoId: String): Boolean {
    val signing = settings.signing ?: return false
    if (!signing.enabled) return false
    return repoId !in signing.skipForRepositories
}

private fun collectArtifacts(
    settings: PublishingSettings,
    moduleDir: Path,
    coordinates: Coordinates
): List<Artifact> {
    val moduleName = moduleDir.fileName.toString()
    val buildDir = findBuildDir(moduleDir)
    val jarDir = buildDir.resolve("tasks/_${moduleName}_jarJvm")

    return settings.artifacts.mapNotNull { type ->
        when (type.lowercase()) {
            "jar" -> {
                val jarPath = jarDir.resolve("$moduleName-jvm.jar")
                if (jarPath.exists()) Artifact(jarPath, null, "jar") else null
            }
            "sources" -> {
                val sourcesPath = jarDir.resolve("$moduleName-sources.jar")
                if (sourcesPath.exists()) Artifact(sourcesPath, "sources", "jar") else null
            }
            "javadoc" -> {
                val javadocPath = jarDir.resolve("$moduleName-javadoc.jar")
                if (javadocPath.exists()) Artifact(javadocPath, "javadoc", "jar") else null
            }
            else -> null
        }
    }
}

private fun findBuildDir(moduleDir: Path): Path {
    var current = moduleDir
    while (current.parent != null) {
        val buildDir = current.resolve("build")
        if (buildDir.exists()) return buildDir
        val projectYaml = current.resolve("project.yaml")
        if (projectYaml.exists()) return current.resolve("build")
        current = current.parent
    }
    return moduleDir.resolve("build")
}

private fun handleResult(result: PublishingResult) {
    when (result) {
        is PublishingResult.Success -> {
            println("  Published ${result.publishedArtifacts.size} artifact(s)")
            result.publishedArtifacts.forEach { artifact ->
                println("    - ${artifact.remoteUrl}")
            }
        }
        is PublishingResult.Failure -> {
            error("Failed: ${result.error.message}")
        }
    }
}
