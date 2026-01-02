package guru.clanker.amper.publish.infrastructure

import guru.clanker.amper.publish.domain.model.*
import guru.clanker.amper.publish.domain.service.PublishingService
import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.maven.resolver.DefaultMavenDeployer
import guru.clanker.amper.publish.maven.pom.PomValidator

class MavenRepositoryPublisher(
    private val deployer: DefaultMavenDeployer = DefaultMavenDeployer(),
    private val retryPolicy: RetryPolicy = RetryPolicy.default()
) : PublishingService {

    override fun publish(publication: Publication, repository: Repository): PublishingResult {
        if (repository !is Repository.Maven) {
            return PublishingResult.Failure(
                publication,
                repository,
                PublishingError.ValidationError("MavenRepositoryPublisher requires Repository.Maven")
            )
        }

        if (isMavenCentral(repository)) {
            val pomErrors = PomValidator.validateForMavenCentral(publication)
            if (pomErrors.isNotEmpty()) {
                return PublishingResult.Failure(
                    publication,
                    repository,
                    PublishingError.ValidationError(pomErrors.joinToString { it.toString() })
                )
            }
        }

        return retryPolicy.execute {
            deployer.deploy(publication, repository)
        }
    }

    override fun validate(publication: Publication, repository: Repository): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (repository is Repository.Maven && isMavenCentral(repository)) {
            val pomErrors = PomValidator.validateForMavenCentral(publication)
            pomErrors.forEach { pomError ->
                errors.add(
                    ValidationError(
                        pomError.field,
                        pomError.message,
                        ValidationError.Severity.ERROR
                    )
                )
            }
        }

        return errors
    }

    private fun isMavenCentral(repository: Repository.Maven): Boolean {
        return repository.url.contains("sonatype") ||
                repository.url.contains("maven.apache.org")
    }
}

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val multiplier: Double = 2.0
) {
    fun <T> execute(block: () -> T): T {
        var attempt = 0
        var lastException: Throwable? = null
        var delay = initialDelayMs

        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                if (!isRetryable(e)) throw e
                lastException = e
                attempt++
                if (attempt < maxAttempts) {
                    Thread.sleep(delay)
                    delay = (delay * multiplier).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed")
    }

    private fun isRetryable(e: Throwable): Boolean {
        return e is java.net.SocketTimeoutException ||
                e is java.net.ConnectException ||
                (e.message?.contains("429") == true) ||
                (e.message?.contains("503") == true)
    }

    companion object {
        fun default() = RetryPolicy()
        fun noRetry() = RetryPolicy(maxAttempts = 1)
    }
}
