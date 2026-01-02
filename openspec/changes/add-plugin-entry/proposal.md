# Change: Add Plugin Tasks & Entry Points

## Why
The plugin needs task actions that users can invoke via `./amper task`. Using Amper's `@TaskAction` annotation, we define functions that receive settings and paths, then orchestrate the publishing pipeline. Tasks are registered in `plugin.yaml` and wired to the settings.

## What Changes
- Create `@TaskAction` functions for publish, publishLocal, and validate tasks
- Register tasks in `plugin.yaml` with settings wiring
- Locate Amper build artifacts from `${module.buildDir}`
- Orchestrate full publishing pipeline
- Support multiple publications and repositories
- Handle publishing targets (default/release/all)
- Dry-run mode for testing

## Impact
- Affected specs: `plugin-entry` (new capability)
- Affected code: `guru.clanker.amper.publish.tasks`
- Dependencies: All previous proposals (1-9)

## Technical Approach

### Amper Task System

With Amper's plugin API:
1. Define `@TaskAction` annotated functions with `@Input`/`@Output` path parameters
2. Register tasks in `plugin.yaml` with action references
3. Wire settings via `${pluginSettings}` references
4. Users invoke: `./amper task :module:taskName@slop-publish`

### plugin.yaml Task Registration

```yaml
# plugin.yaml

tasks:
  # Main publish task
  publish:
    action: !guru.clanker.amper.publish.tasks.publish
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}
      buildDir: ${module.buildDir}
      target: ${pluginSettings.defaultTarget}
      dryRun: false

  # Publish to local ~/.m2/repository
  publishLocal:
    action: !guru.clanker.amper.publish.tasks.publishLocal
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}
      buildDir: ${module.buildDir}

  # Validate configuration without publishing
  validate:
    action: !guru.clanker.amper.publish.tasks.validate
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}

  # Publish with dry-run (no actual upload)
  publishDryRun:
    action: !guru.clanker.amper.publish.tasks.publish
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}
      buildDir: ${module.buildDir}
      target: ${pluginSettings.defaultTarget}
      dryRun: true
```

### Publish Task Action

```kotlin
// src/guru/clanker/amper/publish/tasks/PublishTask.kt

package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService
import guru.clanker.amper.publish.infrastructure.*
import guru.clanker.amper.publish.maven.pom.PomGenerator
import guru.clanker.amper.publish.maven.signing.ArtifactSigner
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Main publish task action.
 * 
 * Publishes artifacts to configured repositories based on target.
 * 
 * Usage: ./amper task :module:publish@slop-publish
 */
@TaskAction
suspend fun publish(
    settings: PublishingSettings,
    @Input moduleDir: Path,
    @Input buildDir: Path,
    target: String,
    dryRun: Boolean
) {
    // Validate settings
    val errors = SettingsValidator.validate(settings)
    if (errors.any { it.severity == ValidationError.Severity.ERROR }) {
        error("Configuration validation failed:\n${errors.joinToString("\n") { "  - ${it.message}" }}")
    }
    
    // Get target repositories
    val targetRepoIds = settings.targets[target]
        ?: if (target == "default") settings.repositories.map { it.id }
        else error("Unknown target: $target. Available: ${settings.targets.keys.joinToString()}")
    
    val repositories = targetRepoIds.map { id ->
        settings.repositories.find { it.id == id }
            ?: error("Unknown repository: $id")
    }
    
    // Build publication
    val coordinates = SettingsMapper.toCoordinates(settings)
    val artifacts = collectArtifacts(settings, buildDir, coordinates)
    val pomMetadata = settings.pom?.let { SettingsMapper.toPomMetadata(it) }
    
    val publication = Publication(
        coordinates = coordinates,
        artifacts = artifacts,
        pomMetadata = pomMetadata
    )
    
    // Generate POM
    val pomGenerator = PomGenerator()
    val pomArtifact = pomGenerator.generate(publication, moduleDir.resolve("build/pom.xml"))
    val publicationWithPom = publication.copy(
        artifacts = publication.artifacts + pomArtifact
    )
    
    // Publish to each repository
    repositories.forEach { repoSettings ->
        val repository = SettingsMapper.toRepository(repoSettings)
        
        // Sign if needed
        val finalPublication = if (shouldSign(settings, repoSettings.id)) {
            val signer = ArtifactSigner(
                privateKey = settings.signing?.key ?: "",
                passphrase = settings.signing?.password ?: ""
            )
            signer.sign(publicationWithPom)
        } else {
            publicationWithPom
        }
        
        // Get appropriate publisher
        val publisher = getPublisher(repository, dryRun)
        
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
    buildDir: Path,
    coordinates: Coordinates
): List<Artifact> {
    val moduleName = coordinates.artifactId
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

private fun getPublisher(repository: Repository, dryRun: Boolean): PublishingService {
    val publisher = when (repository) {
        is Repository.Maven -> MavenRepositoryPublisher()
        is Repository.GitHubPackages -> GitHubPackagesPublisher()
        is Repository.Local -> LocalRepositoryPublisher()
    }
    
    return if (dryRun) DryRunPublisher(publisher) else publisher
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
```

### PublishLocal Task Action

```kotlin
// src/guru/clanker/amper/publish/tasks/PublishLocalTask.kt

package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.infrastructure.LocalRepositoryPublisher
import guru.clanker.amper.publish.maven.pom.PomGenerator
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.*
import java.nio.file.Path

/**
 * Publish to local Maven repository (~/.m2/repository).
 * 
 * Usage: ./amper task :module:publishLocal@slop-publish
 */
@TaskAction
suspend fun publishLocal(
    settings: PublishingSettings,
    @Input moduleDir: Path,
    @Input buildDir: Path
) {
    val errors = SettingsValidator.validate(settings)
    if (errors.any { it.severity == ValidationError.Severity.ERROR }) {
        error("Configuration validation failed:\n${errors.joinToString("\n") { "  - ${it.message}" }}")
    }
    
    val coordinates = SettingsMapper.toCoordinates(settings)
    val artifacts = collectArtifacts(settings, buildDir, coordinates)
    val pomMetadata = settings.pom?.let { SettingsMapper.toPomMetadata(it) }
    
    val publication = Publication(
        coordinates = coordinates,
        artifacts = artifacts,
        pomMetadata = pomMetadata
    )
    
    // Generate POM
    val pomGenerator = PomGenerator()
    val pomArtifact = pomGenerator.generate(publication, moduleDir.resolve("build/pom.xml"))
    val publicationWithPom = publication.copy(
        artifacts = publication.artifacts + pomArtifact
    )
    
    val localRepo = Repository.Local.default()
    val publisher = LocalRepositoryPublisher()
    
    println("Publishing ${coordinates.groupId}:${coordinates.artifactId}:${coordinates.version} to local repository")
    
    val result = publisher.publish(publicationWithPom, localRepo)
    handleResult(result)
}
```

### Validate Task Action

```kotlin
// src/guru/clanker/amper/publish/tasks/ValidateTask.kt

package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.*
import java.nio.file.Path

/**
 * Validate publishing configuration without publishing.
 * 
 * Usage: ./amper task :module:validate@slop-publish
 */
@TaskAction
fun validate(
    settings: PublishingSettings,
    @Input moduleDir: Path
) {
    println("Validating publishing configuration...")
    
    val errors = SettingsValidator.validate(settings)
    
    val errorCount = errors.count { it.severity == ValidationError.Severity.ERROR }
    val warningCount = errors.count { it.severity == ValidationError.Severity.WARNING }
    
    errors.forEach { error ->
        val prefix = if (error.severity == ValidationError.Severity.ERROR) "ERROR" else "WARN"
        println("  [$prefix] ${error.field}: ${error.message}")
    }
    
    println("\nValidation complete: $errorCount error(s), $warningCount warning(s)")
    
    if (errorCount > 0) {
        error("Validation failed with $errorCount error(s)")
    }
    
    println("Configuration is valid for publishing.")
}
```

### Dry Run Publisher

```kotlin
// src/guru/clanker/amper/publish/tasks/DryRunPublisher.kt

package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService

/**
 * Dry-run publisher that logs actions without executing.
 */
class DryRunPublisher(
    private val delegate: PublishingService
) : PublishingService {
    
    override suspend fun publish(publication: Publication, repository: Repository): PublishingResult {
        println("[DRY RUN] Would publish to ${repository.id}:")
        println("[DRY RUN]   ${publication.coordinates.groupId}:${publication.coordinates.artifactId}:${publication.coordinates.version}")
        publication.artifacts.forEach { artifact ->
            println("[DRY RUN]   - ${artifact.file.fileName} (${artifact.classifier ?: "main"}.${artifact.extension})")
        }
        
        return PublishingResult.Success(
            publication = publication,
            repository = repository,
            publishedArtifacts = emptyList()
        )
    }
    
    override fun validate(publication: Publication, repository: Repository) = 
        delegate.validate(publication, repository)
}
```

## Usage Examples

```bash
# Validate configuration
./amper task :my-lib:validate@slop-publish

# Publish to default target
./amper task :my-lib:publish@slop-publish

# Publish to local ~/.m2/repository
./amper task :my-lib:publishLocal@slop-publish

# Dry run (show what would be published)
./amper task :my-lib:publishDryRun@slop-publish
```

## Acceptance Criteria
- [ ] `@TaskAction` functions defined for publish, publishLocal, validate
- [ ] Tasks registered in `plugin.yaml`
- [ ] Settings wired via `${pluginSettings}`
- [ ] Locates artifacts from `${module.buildDir}`
- [ ] Orchestrates full publishing pipeline
- [ ] Supports multiple publications
- [ ] Supports multiple target repositories
- [ ] Provides dry-run mode via separate task
- [ ] Clear error messages for common issues
- [ ] Integration tests for full workflow

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Amper build output structure changes | Abstract artifact location, use module.buildDir reference |
| Complex error scenarios | Structured error types, actionable messages |
| Partial publish failures | Clear error reporting, suggest retry |
| Amper plugin API changes | Follow official docs, check release notes |

## Estimated Effort
**Size: L** (Large) - 4-5 days
