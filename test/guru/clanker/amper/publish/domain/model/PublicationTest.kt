package guru.clanker.amper.publish.domain.model

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PublicationTest {

    private val validCoordinates = Coordinates("com.example", "my-lib", "1.0.0")
    private val validArtifact = Artifact(file = Paths.get("build/my-lib.jar"))

    @Test
    fun `publication is created with valid data`() {
        val pub = Publication(
            coordinates = validCoordinates,
            artifacts = listOf(validArtifact)
        )
        assertEquals(validCoordinates, pub.coordinates)
        assertEquals(1, pub.artifacts.size)
    }

    @Test
    fun `publication with empty artifacts throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Publication(
                coordinates = validCoordinates,
                artifacts = emptyList()
            )
        }
    }

    @Test
    fun `artifact filename is generated correctly`() {
        val artifact = Artifact(
            file = Paths.get("build/lib.jar"),
            classifier = null,
            extension = "jar"
        )
        assertEquals("my-lib-1.0.0.jar", artifact.filename(validCoordinates))
    }

    @Test
    fun `artifact with classifier filename is generated correctly`() {
        val artifact = Artifact(
            file = Paths.get("build/lib-sources.jar"),
            classifier = "sources",
            extension = "jar"
        )
        assertEquals("my-lib-1.0.0-sources.jar", artifact.filename(validCoordinates))
    }

    @Test
    fun `artifact with blank extension throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Artifact(file = Paths.get("build/file"), extension = "")
        }
    }

    @Test
    fun `ArtifactType toArtifact creates correct artifact`() {
        val path = Paths.get("build/sources.jar")
        val artifact = ArtifactType.SOURCES.toArtifact(path)
        
        assertEquals("sources", artifact.classifier)
        assertEquals("jar", artifact.extension)
        assertEquals(path, artifact.file)
    }

    @Test
    fun `PomMetadata with blank name throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PomMetadata(
                name = "",
                description = "desc",
                url = "https://example.com",
                licenses = listOf(License("MIT", "https://mit.edu")),
                developers = listOf(Developer("dev1", "Dev One", "dev@example.com")),
                scm = Scm("https://github.com/x", "scm:git:https://github.com/x")
            )
        }
    }

    @Test
    fun `PomMetadata with empty licenses throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PomMetadata(
                name = "My Lib",
                description = "desc",
                url = "https://example.com",
                licenses = emptyList(),
                developers = listOf(Developer("dev1", "Dev One", "dev@example.com")),
                scm = Scm("https://github.com/x", "scm:git:https://github.com/x")
            )
        }
    }

    @Test
    fun `PomMetadata with empty developers throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PomMetadata(
                name = "My Lib",
                description = "desc",
                url = "https://example.com",
                licenses = listOf(License("MIT", "https://mit.edu")),
                developers = emptyList(),
                scm = Scm("https://github.com/x", "scm:git:https://github.com/x")
            )
        }
    }

    @Test
    fun `License with blank name throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            License(name = "", url = "https://mit.edu")
        }
    }

    @Test
    fun `Developer with blank id throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Developer(id = "", name = "Dev", email = "dev@example.com")
        }
    }

    @Test
    fun `Scm with blank url throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Scm(url = "", connection = "scm:git:...")
        }
    }
}
