package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.infrastructure.LocalRepositoryPublisher
import guru.clanker.amper.publish.maven.pom.DefaultPomGenerator
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.exists

@TaskAction
fun publishLocal(
    settings: PublishingSettings,
    @Input moduleDir: Path
) {
    val errors = SettingsValidator.validate(settings)
    if (errors.any { it.severity == ValidationError.Severity.ERROR }) {
        error("Configuration validation failed:\n${errors.joinToString("\n") { "  - ${it.message}" }}")
    }

    val coordinates = SettingsMapper.toCoordinates(settings)
    val artifacts = collectArtifactsLocal(settings, moduleDir, coordinates)
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

    val localRepo = Repository.Local.default()
    val publisher = LocalRepositoryPublisher()

    println("Publishing ${coordinates.groupId}:${coordinates.artifactId}:${coordinates.version} to local repository")

    val result = publisher.publish(publicationWithPom, localRepo)
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

private fun collectArtifactsLocal(
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
    // Find project root first (where project.yaml is), then use its build directory
    var current = moduleDir
    while (current.parent != null) {
        val projectYaml = current.resolve("project.yaml")
        if (projectYaml.exists()) {
            return current.resolve("build")
        }
        current = current.parent
    }
    // Fallback: look for existing build directory walking up
    current = moduleDir
    while (current.parent != null) {
        val buildDir = current.resolve("build")
        if (buildDir.exists()) return buildDir
        current = current.parent
    }
    return moduleDir.resolve("build")
}
