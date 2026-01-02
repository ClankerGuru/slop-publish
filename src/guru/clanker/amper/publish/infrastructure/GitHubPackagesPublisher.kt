package guru.clanker.amper.publish.infrastructure

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.maven.resolver.ChecksumGenerator
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant

class GitHubPackagesPublisher(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val rateLimiter: RateLimiter = RateLimiter(),
    private val ownsHttpClient: Boolean = true
) : PublishingService, Closeable {

    override fun close() {
        if (ownsHttpClient) {
            httpClient.close()
        }
    }

    override fun publish(publication: Publication, repository: Repository): PublishingResult {
        if (repository !is Repository.GitHubPackages) {
            return PublishingResult.Failure(
                publication,
                repository,
                PublishingError.ValidationError("GitHubPackagesPublisher requires Repository.GitHubPackages")
            )
        }

        val publishedArtifacts = mutableListOf<PublishedArtifact>()

        for (artifact in publication.artifacts) {
            rateLimiter.acquire()

            val result = uploadArtifact(artifact, publication.coordinates, repository)
            when (result) {
                is UploadResult.Success -> publishedArtifacts.add(result.artifact)
                is UploadResult.RateLimited -> {
                    Thread.sleep(result.retryAfterMs)
                    val retry = uploadArtifact(artifact, publication.coordinates, repository)
                    when (retry) {
                        is UploadResult.Success -> publishedArtifacts.add(retry.artifact)
                        else -> return createFailure(publication, repository, retry)
                    }
                }
                is UploadResult.Failure -> return createFailure(publication, repository, result)
            }
        }

        return PublishingResult.Success(publication, repository, publishedArtifacts)
    }

    override fun validate(publication: Publication, repository: Repository): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (repository is Repository.GitHubPackages) {
            if (repository.token.isBlank()) {
                errors.add(ValidationError("token", "GitHub token is required", ValidationError.Severity.ERROR))
            }
        }

        return errors
    }

    private fun uploadArtifact(
        artifact: Artifact,
        coordinates: Coordinates,
        repository: Repository.GitHubPackages
    ): UploadResult {
        val url = buildArtifactUrl(coordinates, artifact, repository)

        return try {
            runBlocking {
                val file = artifact.file.toFile()
                val response = httpClient.put(url) {
                    header("Authorization", "Bearer ${repository.token}")
                    header("Content-Type", contentTypeFor(artifact.extension))
                    header("Content-Length", Files.size(artifact.file).toString())
                    setBody(ChannelProvider(Files.size(artifact.file)) { file.inputStream().toByteReadChannel() })
                }

                when (response.status) {
                    HttpStatusCode.Created, HttpStatusCode.OK -> {
                        UploadResult.Success(
                            PublishedArtifact(
                                artifact = artifact,
                                remoteUrl = url,
                                checksum = ChecksumGenerator.generate(artifact.file)
                            )
                        )
                    }
                    HttpStatusCode.TooManyRequests -> {
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 60
                        UploadResult.RateLimited(retryAfter * 1000)
                    }
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        UploadResult.Failure(
                            PublishingError.AuthenticationFailed("GitHub authentication failed: ${response.status}")
                        )
                    }
                    else -> {
                        UploadResult.Failure(
                            PublishingError.RepositoryError("Upload failed: ${response.status}", response.status.value)
                        )
                    }
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
        val fileName = artifact.filename(coordinates)
        return "${repository.url}/$basePath/$fileName"
    }

    private fun contentTypeFor(extension: String): String = when (extension) {
        "jar" -> "application/java-archive"
        "pom" -> "application/xml"
        "asc" -> "application/pgp-signature"
        else -> "application/octet-stream"
    }

    private fun createFailure(
        publication: Publication,
        repository: Repository,
        result: UploadResult
    ): PublishingResult.Failure {
        return when (result) {
            is UploadResult.Failure -> PublishingResult.Failure(publication, repository, result.error)
            is UploadResult.RateLimited -> PublishingResult.Failure(
                publication,
                repository,
                PublishingError.RepositoryError("Rate limited", 429)
            )
            else -> PublishingResult.Failure(
                publication,
                repository,
                PublishingError.RepositoryError("Unknown error", null)
            )
        }
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000
                connectTimeoutMillis = 30_000
            }
        }
    }
}

sealed class UploadResult {
    data class Success(val artifact: PublishedArtifact) : UploadResult()
    data class RateLimited(val retryAfterMs: Long) : UploadResult()
    data class Failure(val error: PublishingError) : UploadResult()
}

class RateLimiter(
    private val requestsPerMinute: Int = 30,
    private val clock: Clock = Clock.systemUTC()
) {
    private val requestTimes = mutableListOf<Instant>()

    fun acquire() {
        val delayMs: Long
        synchronized(requestTimes) {
            val now = clock.instant()
            val oneMinuteAgo = now.minusSeconds(60)

            requestTimes.removeAll { it.isBefore(oneMinuteAgo) }

            delayMs = if (requestTimes.size >= requestsPerMinute) {
                val oldestInWindow = requestTimes.first()
                val waitUntil = oldestInWindow.plusSeconds(60)
                Duration.between(now, waitUntil).toMillis().coerceAtLeast(0)
            } else {
                0
            }

            requestTimes.add(clock.instant())
        }
        if (delayMs > 0) {
            Thread.sleep(delayMs)
        }
    }
}
