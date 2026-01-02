package guru.clanker.amper.publish.tasks

import guru.clanker.amper.publish.domain.service.ValidationError
import guru.clanker.amper.publish.settings.*
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

@TaskAction
fun validate(
    settings: PublishingSettings,
    @Input moduleDir: Path
) {
    println("Validating publishing configuration...")

    val errors = SettingsValidator.validate(settings)

    val errorCount = errors.count { it.severity == ValidationError.Severity.ERROR }
    val warningCount = errors.count { it.severity == ValidationError.Severity.WARNING }

    errors.forEach { validationError ->
        val prefix = if (validationError.severity == ValidationError.Severity.ERROR) "ERROR" else "WARN"
        println("  [$prefix] ${validationError.field}: ${validationError.message}")
    }

    println("\nValidation complete: $errorCount error(s), $warningCount warning(s)")

    if (errorCount > 0) {
        error("Validation failed with $errorCount error(s)")
    }

    println("Configuration is valid for publishing.")
}
