package guru.clanker.amper.publish.domain.service

import guru.clanker.amper.publish.domain.model.Publication
import guru.clanker.amper.publish.domain.model.PublishingResult
import guru.clanker.amper.publish.domain.model.Repository

/**
 * Core publishing service interface.
 * Implementations handle actual deployment to different repository types.
 */
interface PublishingService {
    /**
     * Publishes a publication to the specified repository.
     */
    fun publish(publication: Publication, repository: Repository): PublishingResult

    /**
     * Validates a publication before publishing.
     * Returns empty list if validation passes.
     */
    fun validate(publication: Publication, repository: Repository): List<ValidationError>
}

data class ValidationError(
    val field: String,
    val message: String,
    val severity: Severity
) {
    enum class Severity { ERROR, WARNING }
}
