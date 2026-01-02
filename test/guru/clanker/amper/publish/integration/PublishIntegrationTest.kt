package guru.clanker.amper.publish.integration

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.infrastructure.MavenRepositoryPublisher
import guru.clanker.amper.publish.infrastructure.RetryPolicy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for publishing artifacts to a real Maven repository.
 * 
 * Uses Reposilite - a lightweight Maven repository - in a Testcontainer.
 */
@Testcontainers
class PublishIntegrationTest {

    companion object {
        private const val REPOSILITE_PORT = 8080

        @Container
        @JvmStatic
        val reposilite: GenericContainer<*> = GenericContainer("dzikoysk/reposilite:3.5.10")
            .withExposedPorts(REPOSILITE_PORT)
            .withEnv("REPOSILITE_OPTS", "--token admin:secret")
            .waitingFor(Wait.forLogMessage(".*Reposilite has been started.*\\n", 1))

        private lateinit var repoUrl: String

        @BeforeAll
        @JvmStatic
        fun setup() {
            repoUrl = "http://${reposilite.host}:${reposilite.getMappedPort(REPOSILITE_PORT)}/releases"
        }
    }

    @Test
    fun `publish artifact to Reposilite and verify it exists`(@TempDir tempDir: Path) {
        // Given: A publication with a test JAR
        val jarFile = createTestJar(tempDir, "test-lib.jar")
        val coordinates = Coordinates(
            groupId = "com.example",
            artifactId = "test-lib",
            version = "1.0.0"
        )
        val publication = Publication(
            coordinates = coordinates,
            artifacts = listOf(Artifact(file = jarFile, extension = "jar"))
        )
        val repository = Repository.Maven(
            id = "reposilite",
            url = repoUrl,
            credentials = Credentials(username = "admin", password = "secret")
        )

        // When: Publishing to Reposilite
        val publisher = MavenRepositoryPublisher(retryPolicy = RetryPolicy.noRetry())
        val result = publisher.publish(publication, repository)

        // Then: Publish succeeds
        assertTrue(result is PublishingResult.Success, "Expected success but got: $result")
        val success = result as PublishingResult.Success
        assertTrue(success.publishedArtifacts.isNotEmpty(), "Expected at least one published artifact")

        // And: Artifact is accessible via HTTP
        val artifactUrl = "$repoUrl/${coordinates.toPath()}/test-lib-1.0.0.jar"
        val responseCode = httpGet(artifactUrl, "admin", "secret")
        assertEquals(200, responseCode, "Artifact should be accessible at $artifactUrl")
    }

    @Test
    fun `publish fails with invalid credentials`(@TempDir tempDir: Path) {
        // Given: A publication with wrong credentials
        val jarFile = createTestJar(tempDir, "another-lib.jar")
        val publication = Publication(
            coordinates = Coordinates("com.example", "another-lib", "1.0.0"),
            artifacts = listOf(Artifact(file = jarFile))
        )
        val repository = Repository.Maven(
            id = "reposilite",
            url = repoUrl,
            credentials = Credentials(username = "wrong", password = "wrong")
        )

        // When: Publishing with bad credentials
        val publisher = MavenRepositoryPublisher(retryPolicy = RetryPolicy.noRetry())
        val result = publisher.publish(publication, repository)

        // Then: Publish fails with auth error
        assertTrue(result is PublishingResult.Failure, "Expected failure but got: $result")
    }

    @Test
    fun `publish snapshot artifact`(@TempDir tempDir: Path) {
        // Given: A snapshot publication
        val jarFile = createTestJar(tempDir, "snapshot-lib.jar")
        val coordinates = Coordinates(
            groupId = "com.example",
            artifactId = "snapshot-lib",
            version = "1.0.0-SNAPSHOT"
        )
        val publication = Publication(
            coordinates = coordinates,
            artifacts = listOf(Artifact(file = jarFile))
        )
        val snapshotRepoUrl = "http://${reposilite.host}:${reposilite.getMappedPort(REPOSILITE_PORT)}/snapshots"
        val repository = Repository.Maven(
            id = "reposilite-snapshots",
            url = snapshotRepoUrl,
            credentials = Credentials(username = "admin", password = "secret"),
            isSnapshot = true
        )

        // When: Publishing snapshot
        val publisher = MavenRepositoryPublisher(retryPolicy = RetryPolicy.noRetry())
        val result = publisher.publish(publication, repository)

        // Then: Publish succeeds
        assertTrue(result is PublishingResult.Success, "Expected success but got: $result")
    }

    private fun createTestJar(dir: Path, name: String): Path {
        val jarFile = dir.resolve(name)
        val manifest = Manifest().apply {
            mainAttributes.putValue("Manifest-Version", "1.0")
            mainAttributes.putValue("Created-By", "slop-publish-test")
        }
        JarOutputStream(Files.newOutputStream(jarFile), manifest).use { jar ->
            jar.putNextEntry(java.util.jar.JarEntry("META-INF/test.txt"))
            jar.write("test content".toByteArray())
            jar.closeEntry()
        }
        return jarFile
    }

    private fun httpGet(url: String, username: String, password: String): Int {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        val auth = java.util.Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        connection.setRequestProperty("Authorization", "Basic $auth")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return try {
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
