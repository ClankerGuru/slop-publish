package guru.clanker.amper.publish.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CoordinatesTest {

    @Test
    fun `valid coordinates are created successfully`() {
        val coords = Coordinates("com.example", "my-lib", "1.0.0")
        assertEquals("com.example", coords.groupId)
        assertEquals("my-lib", coords.artifactId)
        assertEquals("1.0.0", coords.version)
    }

    @Test
    fun `snapshot version is detected`() {
        val snapshot = Coordinates("com.example", "my-lib", "1.0.0-SNAPSHOT")
        val release = Coordinates("com.example", "my-lib", "1.0.0")
        
        assertTrue(snapshot.isSnapshot)
        assertFalse(release.isSnapshot)
    }

    @Test
    fun `toPath generates correct Maven path`() {
        val coords = Coordinates("com.example.sub", "my-lib", "2.0.0")
        assertEquals("com/example/sub/my-lib/2.0.0", coords.toPath())
    }

    @Test
    fun `toBaseFilename generates correct filename base`() {
        val coords = Coordinates("com.example", "my-lib", "1.0.0")
        assertEquals("my-lib-1.0.0", coords.toBaseFilename())
    }

    @Test
    fun `blank groupId throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates("", "my-lib", "1.0.0")
        }
    }

    @Test
    fun `blank artifactId throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates("com.example", "", "1.0.0")
        }
    }

    @Test
    fun `blank version throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates("com.example", "my-lib", "")
        }
    }

    @Test
    fun `invalid groupId format throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates("123invalid", "my-lib", "1.0.0")
        }
    }

    @Test
    fun `invalid artifactId format throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates("com.example", "123invalid", "1.0.0")
        }
    }

    @Test
    fun `valid groupId patterns are accepted`() {
        assertTrue(Coordinates.isValidGroupId("com"))
        assertTrue(Coordinates.isValidGroupId("com.example"))
        assertTrue(Coordinates.isValidGroupId("com.example.sub"))
        assertTrue(Coordinates.isValidGroupId("org.apache_commons"))
        assertTrue(Coordinates.isValidGroupId("io.github.user1"))
    }

    @Test
    fun `invalid groupId patterns are rejected`() {
        assertFalse(Coordinates.isValidGroupId(""))
        assertFalse(Coordinates.isValidGroupId("123"))
        assertFalse(Coordinates.isValidGroupId(".com"))
        assertFalse(Coordinates.isValidGroupId("com."))
        assertFalse(Coordinates.isValidGroupId("com..example"))
    }

    @Test
    fun `valid artifactId patterns are accepted`() {
        assertTrue(Coordinates.isValidArtifactId("mylib"))
        assertTrue(Coordinates.isValidArtifactId("my-lib"))
        assertTrue(Coordinates.isValidArtifactId("my_lib"))
        assertTrue(Coordinates.isValidArtifactId("myLib123"))
    }

    @Test
    fun `invalid artifactId patterns are rejected`() {
        assertFalse(Coordinates.isValidArtifactId(""))
        assertFalse(Coordinates.isValidArtifactId("123lib"))
        assertFalse(Coordinates.isValidArtifactId("-mylib"))
        assertFalse(Coordinates.isValidArtifactId("my.lib"))
    }
}
