# Change: Add Domain Model Implementation

## Why
The domain model forms the core of the slop-publish plugin, representing publications, repositories, artifacts, and coordinates in a pure Kotlin model with no external dependencies. This enables clean architecture, easy testing, and flexibility to change infrastructure without affecting business logic.

## What Changes
- Create `Coordinates` data class for Maven GAV (groupId, artifactId, version)
- Create `Artifact` data class representing JAR, sources, javadoc files
- Create `Repository` sealed class hierarchy for different repository types
- Create `Publication` data class combining coordinates, artifacts, and metadata
- Create `PomMetadata` data class for POM descriptive information
- Create `PublishingResult` sealed class for operation results
- Create `PublishingService` interface for domain service abstraction
- Add comprehensive validation logic with clear error messages

## Impact
- Affected specs: `domain-model` (new capability)
- Affected code: `guru.clanker.amper.publish.domain.model`, `guru.clanker.amper.publish.domain.service`
- Dependencies: Proposal 1 (Project Setup)

## Technical Approach

### Core Data Classes

```kotlin
// src/guru/clanker/amper/publish/domain/model/ (Amper convention)

/**
 * Maven coordinates (GAV - GroupId, ArtifactId, Version).
 */
data class Coordinates(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    init {
        require(groupId.isNotBlank()) { "groupId must not be blank" }
        require(artifactId.isNotBlank()) { "artifactId must not be blank" }
        require(version.isNotBlank()) { "version must not be blank" }
        require(isValidGroupId(groupId)) { "Invalid groupId format: $groupId" }
        require(isValidArtifactId(artifactId)) { "Invalid artifactId format: $artifactId" }
    }
    
    val isSnapshot: Boolean get() = version.endsWith("-SNAPSHOT")
    
    fun toPath(): String = "${groupId.replace('.', '/')}/$artifactId/$version"
}

/**
 * Represents an artifact file to be published.
 */
data class Artifact(
    val file: Path,
    val classifier: String? = null,
    val extension: String = "jar"
) {
    init {
        require(extension.isNotBlank()) { "extension must not be blank" }
    }
}

enum class ArtifactType(val classifier: String?, val extension: String) {
    JAR(null, "jar"),
    SOURCES("sources", "jar"),
    JAVADOC("javadoc", "jar"),
    POM(null, "pom")
}

/**
 * Repository configuration.
 */
sealed class Repository {
    abstract val id: String
    abstract val url: String
    
    data class Maven(
        override val id: String,
        override val url: String,
        val credentials: Credentials? = null,
        val isSnapshot: Boolean = false
    ) : Repository()
    
    data class GitHubPackages(
        override val id: String,
        override val url: String,
        val token: String,
        val owner: String,
        val repository: String
    ) : Repository()
    
    data class Local(
        override val id: String,
        override val url: String  // file:// URL
    ) : Repository()
}

data class Credentials(
    val username: String,
    val password: String
)

/**
 * POM metadata for Maven Central compliance.
 */
data class PomMetadata(
    val name: String,
    val description: String,
    val url: String,
    val licenses: List<License>,
    val developers: List<Developer>,
    val scm: Scm
)

data class License(val name: String, val url: String)
data class Developer(val id: String, val name: String, val email: String)
data class Scm(val url: String, val connection: String, val developerConnection: String? = null)

/**
 * A publication to be deployed.
 */
data class Publication(
    val coordinates: Coordinates,
    val artifacts: List<Artifact>,
    val pomMetadata: PomMetadata? = null
) {
    init {
        require(artifacts.isNotEmpty()) { "Publication must have at least one artifact" }
    }
}

/**
 * Result of a publishing operation.
 */
sealed class PublishingResult {
    data class Success(
        val publication: Publication,
        val repository: Repository,
        val publishedArtifacts: List<PublishedArtifact>
    ) : PublishingResult()
    
    data class Failure(
        val publication: Publication,
        val repository: Repository,
        val error: PublishingError
    ) : PublishingResult()
}

data class PublishedArtifact(
    val artifact: Artifact,
    val remoteUrl: String,
    val checksum: Checksum
)

data class Checksum(val md5: String, val sha1: String, val sha256: String)

sealed class PublishingError {
    data class AuthenticationFailed(val message: String) : PublishingError()
    data class NetworkError(val message: String, val cause: Throwable?) : PublishingError()
    data class ArtifactNotFound(val path: Path) : PublishingError()
    data class ValidationError(val message: String) : PublishingError()
    data class RepositoryError(val message: String, val statusCode: Int?) : PublishingError()
}
```

### Domain Service Interface

```kotlin
// src/guru/clanker/amper/publish/domain/service/ (Amper convention)

/**
 * Core publishing service interface.
 * Implementations handle actual deployment to different repository types.
 */
interface PublishingService {
    /**
     * Publishes a publication to the specified repository.
     */
    suspend fun publish(publication: Publication, repository: Repository): PublishingResult
    
    /**
     * Validates a publication before publishing.
     */
    fun validate(publication: Publication, repository: Repository): List<ValidationError>
}

data class ValidationError(
    val field: String,
    val message: String,
    val severity: Severity
) {
    enum class Severity { ERROR, WARNING }
}
```

## Acceptance Criteria
- [ ] All domain classes are in `guru.clanker.amper.publish.domain.model`
- [ ] No imports from external libraries (Maven, Ktor, Bouncy Castle)
- [ ] All data classes are immutable
- [ ] Validation logic with clear error messages
- [ ] 100% unit test coverage for validation
- [ ] All public APIs have KDoc documentation
- [ ] Konsist architecture tests pass

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Model too rigid for future needs | Use sealed classes for extensibility, plan for backwards compatibility |
| Path handling cross-platform issues | Use `java.nio.file.Path`, avoid string manipulation for paths |

## Estimated Effort
**Size: S** (Small) - 1-2 days
