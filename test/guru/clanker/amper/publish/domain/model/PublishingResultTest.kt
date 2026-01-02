package guru.clanker.amper.publish.domain.model

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PublishingResultTest {

    private val publication = Publication(
        coordinates = Coordinates("com.example", "my-lib", "1.0.0"),
        artifacts = listOf(Artifact(file = Paths.get("build/lib.jar")))
    )
    private val repository = Repository.Local.default()

    @Test
    fun `Success result contains publication and repository`() {
        val result = PublishingResult.Success(
            publication = publication,
            repository = repository,
            publishedArtifacts = emptyList()
        )
        assertEquals(publication, result.publication)
        assertEquals(repository, result.repository)
    }

    @Test
    fun `Failure result contains error message`() {
        val error = PublishingError.AuthenticationFailed("Invalid credentials")
        val result = PublishingResult.Failure(
            publication = publication,
            repository = repository,
            error = error
        )
        assertEquals("Invalid credentials", result.error.message)
    }

    @Test
    fun `ArtifactNotFound error generates path message`() {
        val path = Paths.get("/missing/file.jar")
        val error = PublishingError.ArtifactNotFound(path)
        assertEquals("Artifact not found: /missing/file.jar", error.message)
    }

    @Test
    fun `NetworkError contains cause`() {
        val cause = RuntimeException("Connection refused")
        val error = PublishingError.NetworkError("Network failure", cause)
        assertEquals("Network failure", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `RepositoryError contains status code`() {
        val error = PublishingError.RepositoryError("Forbidden", 403)
        assertEquals("Forbidden", error.message)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun `Checksum with invalid MD5 length throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Checksum(md5 = "short", sha1 = "a".repeat(40), sha256 = "b".repeat(64))
        }
    }

    @Test
    fun `Checksum with invalid SHA1 length throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Checksum(md5 = "a".repeat(32), sha1 = "short", sha256 = "b".repeat(64))
        }
    }

    @Test
    fun `Checksum with invalid SHA256 length throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Checksum(md5 = "a".repeat(32), sha1 = "a".repeat(40), sha256 = "short")
        }
    }

    @Test
    fun `valid Checksum is created`() {
        val checksum = Checksum(
            md5 = "a".repeat(32),
            sha1 = "b".repeat(40),
            sha256 = "c".repeat(64)
        )
        assertEquals(32, checksum.md5.length)
        assertEquals(40, checksum.sha1.length)
        assertEquals(64, checksum.sha256.length)
    }
}
