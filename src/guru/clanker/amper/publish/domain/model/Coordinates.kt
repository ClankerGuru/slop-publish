package guru.clanker.amper.publish.domain.model

/**
 * Maven coordinates (GAV - GroupId, ArtifactId, Version).
 * 
 * Represents the unique identifier for a Maven artifact following
 * Maven naming conventions.
 */
data class Coordinates(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    init {
        require(groupId.isNotBlank()) { "groupId must not be blank" }
        require(artifactId.isNotBlank()) { "artifactId must not be blank" }
        require(version.isNotBlank()) { "version must not be blank" }
        require(isValidGroupId(groupId)) { "Invalid groupId format: $groupId" }
        require(isValidArtifactId(artifactId)) { "Invalid artifactId format: $artifactId" }
    }

    /**
     * Returns true if this is a snapshot version.
     */
    val isSnapshot: Boolean get() = version.endsWith("-SNAPSHOT")

    /**
     * Converts coordinates to Maven repository path format.
     * Example: "com.example/my-lib/1.0.0"
     */
    fun toPath(): String = "${groupId.replace('.', '/')}/$artifactId/$version"

    /**
     * Returns the base filename for artifacts (without extension).
     * Example: "my-lib-1.0.0"
     */
    fun toBaseFilename(): String = "$artifactId-$version"

    companion object {
        private val GROUP_ID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$")
        private val ARTIFACT_ID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

        /**
         * Validates Maven groupId format.
         * Must be dot-separated identifiers starting with a letter.
         */
        fun isValidGroupId(groupId: String): Boolean = GROUP_ID_PATTERN.matches(groupId)

        /**
         * Validates Maven artifactId format.
         * Must start with a letter, can contain letters, numbers, underscores, and hyphens.
         */
        fun isValidArtifactId(artifactId: String): Boolean = ARTIFACT_ID_PATTERN.matches(artifactId)
    }
}
