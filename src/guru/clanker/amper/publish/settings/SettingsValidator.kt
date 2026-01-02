package guru.clanker.amper.publish.settings

import guru.clanker.amper.publish.domain.service.ValidationError

object SettingsValidator {

    fun validate(settings: PublishingSettings): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (settings.groupId.isBlank()) {
            errors += ValidationError("groupId", "groupId is required", ValidationError.Severity.ERROR)
        }
        if (settings.artifactId.isBlank()) {
            errors += ValidationError("artifactId", "artifactId is required", ValidationError.Severity.ERROR)
        }
        if (settings.version.isBlank()) {
            errors += ValidationError("version", "version is required", ValidationError.Severity.ERROR)
        }

        val mavenCentralRepos = settings.repositories.filter {
            it.url.contains("oss.sonatype.org") || it.url.contains("central.sonatype")
        }
        if (mavenCentralRepos.isNotEmpty() && settings.pom == null) {
            errors += ValidationError(
                "pom",
                "POM metadata required for Maven Central publishing",
                ValidationError.Severity.ERROR
            )
        }

        val repoIds = settings.repositories.map { it.id }
        val duplicates = repoIds.groupBy { it }.filter { it.value.size > 1 }.keys
        duplicates.forEach { dup ->
            errors += ValidationError("repositories", "Duplicate repository ID: $dup", ValidationError.Severity.ERROR)
        }

        settings.targets.forEach { (target, repoRefs) ->
            repoRefs.forEach { ref ->
                if (ref !in repoIds) {
                    errors += ValidationError(
                        "targets.$target",
                        "Unknown repository: $ref. Valid: ${repoIds.joinToString()}",
                        ValidationError.Severity.ERROR
                    )
                }
            }
        }

        return errors
    }
}
