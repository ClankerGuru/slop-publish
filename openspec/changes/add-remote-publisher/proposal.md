# Change: Add Remote Maven Repository Publisher

## Why
Publishing to remote Maven repositories (Maven Central, Nexus, Artifactory) requires HTTP authentication, retry logic, and proper error handling. This module provides a production-ready remote publishing implementation.

## What Changes
- Create `MavenRepositoryPublisher` for remote Maven repositories
- Implement HTTP authentication (username/password, bearer tokens)
- Add retry logic with exponential backoff
- Support staging repositories for Maven Central
- Progress reporting for large artifact uploads
- Proper credential masking in logs

## Impact
- Affected specs: `remote-publisher` (new capability)
- Affected code: `guru.clanker.amper.publish.infrastructure`
- Dependencies: Proposal 4 (Maven Resolver), Proposal 5 (POM Generation), Proposal 6 (Local Publisher)

## Technical Approach

### MavenRepositoryPublisher

```kotlin
// src/guru/clanker/amper/publish/infrastructure/ (Amper convention)

/**
 * Publishes artifacts to remote Maven repositories.
 */
class MavenRepositoryPublisher(
    private val deployer: MavenDeployer = MavenDeployer(),
    private val pomGenerator: PomGenerator = PomGenerator(),
    private val retryPolicy: RetryPolicy = RetryPolicy.default()
) : PublishingRepository {
    
    override suspend fun publish(publication: Publication, repository: Repository.Maven): PublishingResult {
        // Validate for Maven Central if applicable
        if (isMavenCentral(repository)) {
            val errors = PomValidator().validateForMavenCentral(publication)
            if (errors.isNotEmpty()) {
                return PublishingResult.Failure(
                    publication, repository,
                    PublishingError.ValidationError(errors.joinToString { it.toString() })
                )
            }
        }
        
        // Generate POM
        val pomPath = generatePom(publication, isMavenCentral(repository))
        val pomArtifact = Artifact(pomPath, null, "pom")
        val allArtifacts = publication.artifacts + pomArtifact
        
        // Deploy with retry
        return retryPolicy.execute {
            deployer.deploy(
                publication.copy(artifacts = allArtifacts),
                repository
            ).toPublishingResult(publication, repository)
        }
    }
    
    private fun isMavenCentral(repository: Repository.Maven): Boolean {
        return repository.url.contains("sonatype") || 
               repository.url.contains("maven.apache.org")
    }
}
```

### Retry Policy

```kotlin
/**
 * Configurable retry policy with exponential backoff.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val multiplier: Double = 2.0,
    val retryableErrors: Set<Class<out Throwable>> = setOf(
        java.net.SocketTimeoutException::class.java,
        java.net.ConnectException::class.java
    )
) {
    suspend fun <T> execute(block: suspend () -> T): T {
        var attempt = 0
        var lastException: Throwable? = null
        var delay = initialDelayMs
        
        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                if (!isRetryable(e)) throw e
                lastException = e
                attempt++
                if (attempt < maxAttempts) {
                    delay(delay)
                    delay = (delay * multiplier).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }
        
        throw lastException ?: IllegalStateException("Retry failed")
    }
    
    private fun isRetryable(e: Throwable): Boolean {
        return retryableErrors.any { it.isInstance(e) } ||
               (e.message?.contains("429") == true) ||  // Rate limited
               (e.message?.contains("503") == true)     // Service unavailable
    }
    
    companion object {
        fun default() = RetryPolicy()
        fun noRetry() = RetryPolicy(maxAttempts = 1)
    }
}
```

### Progress Reporting

```kotlin
interface PublishingProgressListener {
    fun onArtifactStarted(artifact: Artifact, totalBytes: Long)
    fun onProgress(artifact: Artifact, bytesTransferred: Long, totalBytes: Long)
    fun onArtifactCompleted(artifact: Artifact)
    fun onArtifactFailed(artifact: Artifact, error: PublishingError)
}

class LoggingProgressListener(private val logger: Logger) : PublishingProgressListener {
    override fun onArtifactStarted(artifact: Artifact, totalBytes: Long) {
        logger.info("Uploading ${artifact.file.fileName} (${formatBytes(totalBytes)})")
    }
    
    override fun onProgress(artifact: Artifact, bytesTransferred: Long, totalBytes: Long) {
        val percent = (bytesTransferred * 100 / totalBytes).toInt()
        logger.debug("${artifact.file.fileName}: $percent%")
    }
    
    override fun onArtifactCompleted(artifact: Artifact) {
        logger.info("Completed ${artifact.file.fileName}")
    }
    
    override fun onArtifactFailed(artifact: Artifact, error: PublishingError) {
        logger.error("Failed ${artifact.file.fileName}: $error")
    }
}
```

## Acceptance Criteria
- [ ] Publish artifacts to remote Maven repositories
- [ ] Support username/password authentication
- [ ] Retry failed uploads with exponential backoff
- [ ] Validate Maven Central requirements before upload
- [ ] Progress reporting for large uploads
- [ ] Mask credentials in log output
- [ ] Handle rate limiting (HTTP 429)

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Network instability | Retry with exponential backoff |
| Rate limiting | Respect Retry-After header, add delays |
| Large file timeout | Increase timeout for large artifacts |
| Credential exposure | Mask in logs, use secure storage |

## Estimated Effort
**Size: L** (Large) - 3-4 days
