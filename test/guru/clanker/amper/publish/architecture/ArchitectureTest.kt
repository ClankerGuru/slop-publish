package guru.clanker.amper.publish.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class ArchitectureTest {

    @Test
    fun `domain layer has no infrastructure dependencies`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { import ->
                    import.name.contains(".infrastructure.") ||
                    import.name.contains(".maven.") ||
                    import.name.contains(".settings.") ||
                    import.name.contains(".tasks.")
                }
            }
    }

    @Test
    fun `domain layer has no Maven class imports`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { import ->
                    import.name.startsWith("org.apache.maven")
                }
            }
    }

    @Test
    fun `domain layer has no Ktor class imports`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { import ->
                    import.name.startsWith("io.ktor")
                }
            }
    }

    @Test
    fun `domain layer has no Bouncy Castle imports`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { import ->
                    import.name.startsWith("org.bouncycastle")
                }
            }
    }

    @Test
    fun `test classes end with Test suffix`() {
        Konsist
            .scopeFromTest()
            .classes()
            .assertTrue { it.name.endsWith("Test") }
    }

    @Test
    fun `layer dependencies are respected`() {
        Konsist
            .scopeFromProduction()
            .assertArchitecture {
                val domain = Layer("Domain", "guru.clanker.amper.publish.domain..")
                val settings = Layer("Settings", "guru.clanker.amper.publish.settings..")
                val maven = Layer("Maven", "guru.clanker.amper.publish.maven..")
                val infra = Layer("Infrastructure", "guru.clanker.amper.publish.infrastructure..")
                val tasks = Layer("Tasks", "guru.clanker.amper.publish.tasks..")

                domain.dependsOnNothing()
                settings.dependsOn(domain)
                maven.dependsOn(domain)
                infra.dependsOn(domain, maven)
                tasks.dependsOn(settings, domain, infra, maven)
            }
    }

    @Test
    fun `interfaces do not have I prefix`() {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .assertFalse { iface ->
                iface.name.startsWith("I") && iface.name.getOrNull(1)?.isUpperCase() == true
            }
    }
}
