package guru.clanker.amper.publish.domain.model

/**
 * A publication to be deployed to a Maven repository.
 */
data class Publication(
    val coordinates: Coordinates,
    val artifacts: List<Artifact>,
    val pomMetadata: PomMetadata? = null
) {
    init {
        require(artifacts.isNotEmpty()) { "Publication must have at least one artifact" }
    }
}

/**
 * POM metadata for Maven Central compliance.
 */
data class PomMetadata(
    val name: String,
    val description: String,
    val url: String,
    val licenses: List<License>,
    val developers: List<Developer>,
    val scm: Scm
) {
    init {
        require(name.isNotBlank()) { "POM name must not be blank" }
        require(licenses.isNotEmpty()) { "At least one license is required for Maven Central" }
        require(developers.isNotEmpty()) { "At least one developer is required for Maven Central" }
    }
}

data class License(
    val name: String,
    val url: String
) {
    init {
        require(name.isNotBlank()) { "License name must not be blank" }
        require(url.isNotBlank()) { "License URL must not be blank" }
    }
}

data class Developer(
    val id: String,
    val name: String,
    val email: String
) {
    init {
        require(id.isNotBlank()) { "Developer id must not be blank" }
        require(name.isNotBlank()) { "Developer name must not be blank" }
    }
}

data class Scm(
    val url: String,
    val connection: String,
    val developerConnection: String? = null
) {
    init {
        require(url.isNotBlank()) { "SCM url must not be blank" }
        require(connection.isNotBlank()) { "SCM connection must not be blank" }
    }
}
