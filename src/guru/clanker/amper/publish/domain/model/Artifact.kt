package guru.clanker.amper.publish.domain.model

import java.nio.file.Path

/**
 * Represents an artifact file to be published.
 * 
 * An artifact is a file (typically a JAR) that will be deployed to a Maven repository.
 * Each artifact has a path to the file, optional classifier, and extension.
 */
data class Artifact(
    val file: Path,
    val classifier: String? = null,
    val extension: String = "jar"
) {
    init {
        require(extension.isNotBlank()) { "extension must not be blank" }
    }

    /**
     * Returns the filename for this artifact given coordinates.
     * Example: "my-lib-1.0.0-sources.jar"
     */
    fun filename(coordinates: Coordinates): String {
        val base = coordinates.toBaseFilename()
        val classifierPart = classifier?.let { "-$it" } ?: ""
        return "$base$classifierPart.$extension"
    }
}

/**
 * Standard artifact types for Maven publications.
 */
enum class ArtifactType(val classifier: String?, val extension: String) {
    /** Main JAR artifact */
    JAR(null, "jar"),
    
    /** Sources JAR */
    SOURCES("sources", "jar"),
    
    /** Javadoc JAR */
    JAVADOC("javadoc", "jar"),
    
    /** POM file */
    POM(null, "pom");

    /**
     * Creates an Artifact from this type with the given file path.
     */
    fun toArtifact(file: Path): Artifact = Artifact(
        file = file,
        classifier = classifier,
        extension = extension
    )
}
