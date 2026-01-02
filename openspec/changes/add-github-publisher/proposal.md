# Change: Add GitHub Packages Publisher

## Why
GitHub Packages provides a Maven-compatible repository tightly integrated with GitHub workflows. This module provides specialized support for GitHub Packages including PAT/GITHUB_TOKEN authentication and rate limiting handling.

## What Changes
- Create `GitHubPackagesPublisher` using Ktor Client
- Support Personal Access Token (PAT) authentication
- Support GITHUB_TOKEN for GitHub Actions
- Handle GitHub API rate limiting (429 responses)
- Parse owner/repo from repository URL
- No signing required (unlike Maven Central)

## Impact
- Affected specs: `github-publisher` (new capability)
- Affected code: `guru.clanker.amper.publish.infrastructure`
- Dependencies: Proposal 4 (Maven Resolver), Proposal 5 (POM Generation)

## Technical Approach

### GitHubPackagesPublisher

```kotlin
// src/guru/clanker/amper/publish/infrastructure/ (Amper convention)

/**
 * Publishes artifacts to GitHub Packages Maven registry.
 */
class GitHubPackagesPublisher(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val pomGenerator: PomGenerator = PomGenerator(),
    private val rateLimiter: RateLimiter = RateLimiter()
) : PublishingRepository {
    
    override suspend fun publish(
        publication: Publication,
        repository: Repository.GitHubPackages
    ): PublishingResult {
        // Generate minimal POM (GitHub doesn't require full metadata)
        val pomPath = pomGenerator.generateToFile(
            publication,
            createTempPomPath(publication),
            full = false
        )
        val pomArtifact = Artifact(pomPath, null, "pom")
        
        val allArtifacts = publication.artifacts + pomArtifact
        val publishedArtifacts = mutableListOf<PublishedArtifact>()
        
        for (artifact in allArtifacts) {
            rateLimiter.acquire()
            
            val result = uploadArtifact(artifact, publication.coordinates, repository)
            when (result) {
                is UploadResult.Success -> publishedArtifacts.add(result.artifact)
                is UploadResult.RateLimited -> {
                    delay(result.retryAfterMs)
                    // Retry once
                    val retry = uploadArtifact(artifact, publication.coordinates, repository)
                    if (retry is UploadResult.Success) {
                        publishedArtifacts.add(retry.artifact)
                    } else {
                        return createFailure(publication, repository, retry)
                    }
                }
                is UploadResult.Failure -> {
                    return createFailure(publication, repository, result)
                }
            }
        }
        
        return PublishingResult.Success(publication, repository, publishedArtifacts)
    }
    
    private suspend fun uploadArtifact(
        artifact: Artifact,
        coordinates: Coordinates,
        repository: Repository.GitHubPackages
    ): UploadResult {
        val url = buildArtifactUrl(coordinates, artifact, repository)
        
        return try {
            val response = httpClient.put(url) {
                header("Authorization", "Bearer ${repository.token}")
                header("Content-Type", contentTypeFor(artifact.extension))
                setBody(artifact.file.toFile().readBytes())
            }
            
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    UploadResult.Success(PublishedArtifact(
                        artifact = artifact,
                        remoteUrl = url,
                        checksum = ChecksumGenerator.generate(artifact.file)
                    ))
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 60
                    UploadResult.RateLimited(retryAfter * 1000)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                    UploadResult.Failure(PublishingError.AuthenticationFailed(
                        "GitHub authentication failed: ${response.status}"
                    ))
                }
                else -> {
                    UploadResult.Failure(PublishingError.RepositoryError(
                        "Upload failed: ${response.status}",
                        response.status.value
                    ))
                }
            }
        } catch (e: Exception) {
            UploadResult.Failure(PublishingError.NetworkError(e.message ?: "Unknown error", e))
        }
    }
    
    private fun buildArtifactUrl(
        coordinates: Coordinates,
        artifact: Artifact,
        repository: Repository.GitHubPackages
    ): String {
        val basePath = coordinates.toPath()
        val fileName = "${coordinates.artifactId}-${coordinates.version}" +
            (artifact.classifier?.let { "-$it" } ?: "") +
            ".${artifact.extension}"
        return "${repository.url}/$basePath/$fileName"
    }
    
    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000  // 5 minutes for large files
                connectTimeoutMillis = 30_000
            }
        }
        
        fun parseGitHubUrl(url: String): Pair<String, String>? {
            val regex = Regex("""https://maven\.pkg\.github\.com/([^/]+)/([^/]+)""")
            return regex.find(url)?.let { match ->
                match.groupValues[1] to match.groupValues[2]
            }
        }
    }
}

sealed class UploadResult {
    data class Success(val artifact: PublishedArtifact) : UploadResult()
    data class RateLimited(val retryAfterMs: Long) : UploadResult()
    data class Failure(val error: PublishingError) : UploadResult()
}
```

### Rate Limiter

```kotlin
/**
 * Simple rate limiter to avoid hitting GitHub API limits.
 */
class RateLimiter(
    private val requestsPerMinute: Int = 30,
    private val clock: Clock = Clock.systemUTC()
) {
    private val requestTimes = mutableListOf<Instant>()
    
    suspend fun acquire() {
        synchronized(requestTimes) {
            val now = clock.instant()
            val oneMinuteAgo = now.minusSeconds(60)
            
            // Remove old requests
            requestTimes.removeAll { it.isBefore(oneMinuteAgo) }
            
            if (requestTimes.size >= requestsPerMinute) {
                val oldestInWindow = requestTimes.first()
                val waitUntil = oldestInWindow.plusSeconds(60)
                val delayMs = Duration.between(now, waitUntil).toMillis()
                if (delayMs > 0) {
                    delay(delayMs)
                }
            }
            
            requestTimes.add(clock.instant())
        }
    }
}
```

## Acceptance Criteria
- [ ] Publish to GitHub Packages Maven registry
- [ ] Support PAT authentication
- [ ] Support GITHUB_TOKEN (Actions)
- [ ] Handle HTTP 429 rate limiting with retry
- [ ] Parse owner/repo from URL
- [ ] No signing required
- [ ] Integration tests with test repository

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Rate limiting | Proactive rate limiter, respect Retry-After |
| Token expiration | Clear error message, document token scopes |
| URL format changes | Abstract URL building, version check |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
