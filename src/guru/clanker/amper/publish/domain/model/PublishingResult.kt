package guru.clanker.amper.publish.domain.model

import java.nio.file.Path

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

data class Checksum(
    val md5: String,
    val sha1: String,
    val sha256: String
) {
    init {
        require(md5.length == 32) { "MD5 checksum must be 32 characters" }
        require(sha1.length == 40) { "SHA1 checksum must be 40 characters" }
        require(sha256.length == 64) { "SHA256 checksum must be 64 characters" }
    }
}

/**
 * Errors that can occur during publishing.
 */
sealed class PublishingError {
    abstract val message: String

    data class AuthenticationFailed(override val message: String) : PublishingError()
    data class NetworkError(override val message: String, val cause: Throwable?) : PublishingError()
    data class ArtifactNotFound(val path: Path) : PublishingError() {
        override val message: String = "Artifact not found: $path"
    }
    data class ValidationError(override val message: String) : PublishingError()
    data class RepositoryError(override val message: String, val statusCode: Int?) : PublishingError()
}
