package guru.clanker.amper.publish.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepositoryTest {

    @Test
    fun `Maven repository is created with valid data`() {
        val repo = Repository.Maven(
            id = "central",
            url = "https://repo1.maven.org/maven2",
            credentials = Credentials("user", "pass"),
            isSnapshot = false
        )
        assertEquals("central", repo.id)
        assertEquals("https://repo1.maven.org/maven2", repo.url)
    }

    @Test
    fun `Maven repository with blank id throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Repository.Maven(id = "", url = "https://example.com")
        }
    }

    @Test
    fun `Maven repository with blank url throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Repository.Maven(id = "test", url = "")
        }
    }

    @Test
    fun `GitHubPackages repository is created with valid data`() {
        val repo = Repository.GitHubPackages(
            id = "github",
            url = "https://maven.pkg.github.com/owner/repo",
            token = "ghp_token123",
            owner = "owner",
            repository = "repo"
        )
        assertEquals("github", repo.id)
        assertEquals("owner", repo.owner)
        assertEquals("repo", repo.repository)
    }

    @Test
    fun `GitHubPackages fromOwnerRepo creates correct URL`() {
        val repo = Repository.GitHubPackages.fromOwnerRepo(
            id = "github",
            owner = "myorg",
            repository = "myrepo",
            token = "token123"
        )
        assertEquals("https://maven.pkg.github.com/myorg/myrepo", repo.url)
    }

    @Test
    fun `GitHubPackages with blank token throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Repository.GitHubPackages(
                id = "github",
                url = "https://maven.pkg.github.com/owner/repo",
                token = "",
                owner = "owner",
                repository = "repo"
            )
        }
    }

    @Test
    fun `Local repository default creates m2 path`() {
        val repo = Repository.Local.default()
        assertEquals("local", repo.id)
        assertTrue(repo.url.contains(".m2/repository"))
    }

    @Test
    fun `Credentials with blank username throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Credentials(username = "", password = "pass")
        }
    }

    @Test
    fun `Credentials with blank password throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Credentials(username = "user", password = "")
        }
    }

    private fun assertTrue(condition: Boolean) {
        kotlin.test.assertTrue(condition)
    }
}
