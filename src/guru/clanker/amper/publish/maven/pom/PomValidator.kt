package guru.clanker.amper.publish.maven.pom

import guru.clanker.amper.publish.domain.model.Publication

object PomValidator {

    fun validateForMavenCentral(publication: Publication): List<PomValidationError> {
        val errors = mutableListOf<PomValidationError>()
        val metadata = publication.pomMetadata

        if (metadata == null) {
            errors.add(PomValidationError("pomMetadata", "POM metadata is required for Maven Central"))
            return errors
        }

        if (metadata.name.isBlank()) {
            errors.add(PomValidationError("name", "name is required"))
        }
        if (metadata.description.isBlank()) {
            errors.add(PomValidationError("description", "description is required"))
        }
        if (metadata.url.isBlank()) {
            errors.add(PomValidationError("url", "url is required"))
        }
        if (metadata.licenses.isEmpty()) {
            errors.add(PomValidationError("licenses", "at least one license is required"))
        }
        if (metadata.developers.isEmpty()) {
            errors.add(PomValidationError("developers", "at least one developer is required"))
        }
        if (metadata.scm.url.isBlank()) {
            errors.add(PomValidationError("scm.url", "scm.url is required"))
        }
        if (metadata.scm.connection.isBlank()) {
            errors.add(PomValidationError("scm.connection", "scm.connection is required"))
        }

        return errors
    }
}

data class PomValidationError(
    val field: String,
    val message: String
)
