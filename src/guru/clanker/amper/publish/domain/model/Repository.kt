package guru.clanker.amper.publish.domain.model

/**
 * Repository configuration for artifact deployment.
 * 
 * This is a sealed class hierarchy supporting different repository types:
 * - [Maven]: Standard Maven repository (Maven Central, Nexus, Artifactory)
 * - [GitHubPackages]: GitHub Packages Maven registry
 * - [Local]: Local filesystem repository (e.g., ~/.m2/repository)
 */
sealed class Repository {
    /** Unique identifier for this repository */
    abstract val id: String
    
    /** Repository URL */
    abstract val url: String

    /**
     * Standard Maven repository (Maven Central, Nexus, Artifactory, etc.).
     */
    data class Maven(
        override val id: String,
        override val url: String,
        val credentials: Credentials? = null,
        val isSnapshot: Boolean = false
    ) : Repository() {
        init {
            require(id.isNotBlank()) { "Repository id must not be blank" }
            require(url.isNotBlank()) { "Repository url must not be blank" }
        }
    }

    /**
     * GitHub Packages Maven registry.
     */
    data class GitHubPackages(
        override val id: String,
        override val url: String,
        val token: String,
        val owner: String,
        val repository: String
    ) : Repository() {
        init {
            require(id.isNotBlank()) { "Repository id must not be blank" }
            require(token.isNotBlank()) { "GitHub token must not be blank" }
            require(owner.isNotBlank()) { "GitHub owner must not be blank" }
            require(repository.isNotBlank()) { "GitHub repository must not be blank" }
        }

        /**
         * Derives the URL from owner and repository if not explicitly set.
         */
        companion object {
            fun fromOwnerRepo(id: String, owner: String, repository: String, token: String): GitHubPackages {
                return GitHubPackages(
                    id = id,
                    url = "https://maven.pkg.github.com/$owner/$repository",
                    token = token,
                    owner = owner,
                    repository = repository
                )
            }
        }
    }

    /**
     * Local filesystem repository.
     */
    data class Local(
        override val id: String,
        override val url: String
    ) : Repository() {
        init {
            require(id.isNotBlank()) { "Repository id must not be blank" }
            require(url.isNotBlank()) { "Repository url must not be blank" }
        }

        companion object {
            fun default(): Local {
                val m2 = System.getProperty("user.home") + "/.m2/repository"
                return Local(id = "local", url = "file://$m2")
            }
        }
    }

    /**
     * Maven Central Portal (new Sonatype Central publishing API).
     * Uses bundle zip upload instead of traditional Nexus staging.
     */
    data class CentralPortal(
        override val id: String,
        override val url: String = "https://central.sonatype.com/api/v1/publisher/upload",
        val token: String,
        val publishingType: PublishingType = PublishingType.AUTOMATIC
    ) : Repository() {
        init {
            require(id.isNotBlank()) { "Repository id must not be blank" }
            require(token.isNotBlank()) { "Central Portal token must not be blank" }
        }

        enum class PublishingType { AUTOMATIC, USER_MANAGED }

        companion object {
            fun default(token: String): CentralPortal {
                return CentralPortal(id = "central", token = token)
            }
        }
    }
}

/**
 * Authentication credentials for repository access.
 */
data class Credentials(
    val username: String,
    val password: String
) {
    init {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }
    }
}
