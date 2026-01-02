package guru.clanker.amper.publish.maven

import guru.clanker.amper.publish.domain.model.Artifact
import guru.clanker.amper.publish.domain.model.Coordinates
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Generates sources and javadoc JARs required for Maven Central publishing.
 */
object ArtifactGenerator {

    /**
     * Creates a sources JAR from the src directory.
     */
    fun createSourcesJar(
        sourceDir: Path,
        outputDir: Path,
        coordinates: Coordinates
    ): Artifact {
        val jarName = "${coordinates.artifactId}-${coordinates.version}-sources.jar"
        val jarPath = outputDir.resolve(jarName)
        
        Files.createDirectories(outputDir)
        
        val manifest = createManifest(coordinates, "sources")
        
        JarOutputStream(Files.newOutputStream(jarPath), manifest).use { jos ->
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                Files.walk(sourceDir).use { paths ->
                    paths.filter { it.isRegularFile() }
                        .forEach { file ->
                            val entryName = sourceDir.relativize(file).toString().replace("\\", "/")
                            jos.putNextEntry(JarEntry(entryName))
                            Files.copy(file, jos)
                            jos.closeEntry()
                        }
                }
            }
        }
        
        return Artifact(
            file = jarPath,
            classifier = "sources",
            extension = "jar"
        )
    }

    /**
     * Creates a javadoc JAR. Since Kotlin doesn't have traditional javadoc,
     * we create a minimal placeholder with an index page.
     * 
     * For proper documentation, users should configure Dokka separately.
     */
    fun createJavadocJar(
        outputDir: Path,
        coordinates: Coordinates,
        description: String = ""
    ): Artifact {
        val jarName = "${coordinates.artifactId}-${coordinates.version}-javadoc.jar"
        val jarPath = outputDir.resolve(jarName)
        
        Files.createDirectories(outputDir)
        
        val manifest = createManifest(coordinates, "javadoc")
        
        JarOutputStream(Files.newOutputStream(jarPath), manifest).use { jos ->
            jos.putNextEntry(JarEntry("index.html"))
            val html = buildJavadocIndex(coordinates, description)
            jos.write(html.toByteArray())
            jos.closeEntry()
            
            jos.putNextEntry(JarEntry("package-list"))
            jos.write("".toByteArray())
            jos.closeEntry()
        }
        
        return Artifact(
            file = jarPath,
            classifier = "javadoc",
            extension = "jar"
        )
    }

    /**
     * Creates a javadoc JAR from an existing dokka output directory.
     */
    fun createJavadocJarFromDokka(
        dokkaOutputDir: Path,
        outputDir: Path,
        coordinates: Coordinates
    ): Artifact {
        val jarName = "${coordinates.artifactId}-${coordinates.version}-javadoc.jar"
        val jarPath = outputDir.resolve(jarName)
        
        Files.createDirectories(outputDir)
        
        val manifest = createManifest(coordinates, "javadoc")
        
        JarOutputStream(Files.newOutputStream(jarPath), manifest).use { jos ->
            if (dokkaOutputDir.exists() && dokkaOutputDir.isDirectory()) {
                Files.walk(dokkaOutputDir).use { paths ->
                    paths.filter { it.isRegularFile() }
                        .forEach { file ->
                            val entryName = dokkaOutputDir.relativize(file).toString().replace("\\", "/")
                            jos.putNextEntry(JarEntry(entryName))
                            Files.copy(file, jos)
                            jos.closeEntry()
                        }
                }
            }
        }
        
        return Artifact(
            file = jarPath,
            classifier = "javadoc",
            extension = "jar"
        )
    }

    private fun createManifest(coordinates: Coordinates, type: String): Manifest {
        return Manifest().apply {
            mainAttributes.putValue("Manifest-Version", "1.0")
            mainAttributes.putValue("Created-By", "slop-publish")
            mainAttributes.putValue("Implementation-Title", "${coordinates.artifactId}-$type")
            mainAttributes.putValue("Implementation-Version", coordinates.version)
            mainAttributes.putValue("Implementation-Vendor", coordinates.groupId)
        }
    }

    private fun buildJavadocIndex(coordinates: Coordinates, description: String): String {
        val desc = description.ifEmpty { "${coordinates.artifactId} ${coordinates.version}" }
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${coordinates.artifactId} ${coordinates.version} API</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                           max-width: 800px; margin: 50px auto; padding: 20px; }
                    h1 { color: #333; }
                    p { color: #666; line-height: 1.6; }
                    code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
                    a { color: #0066cc; }
                </style>
            </head>
            <body>
                <h1>${coordinates.artifactId}</h1>
                <p><strong>Version:</strong> ${coordinates.version}</p>
                <p><strong>Group:</strong> ${coordinates.groupId}</p>
                <p>$desc</p>
                <hr>
                <p>This is a placeholder javadoc JAR. For full API documentation, 
                   configure <a href="https://github.com/Kotlin/dokka">Dokka</a> in your build.</p>
                <p><code>${coordinates.groupId}:${coordinates.artifactId}:${coordinates.version}</code></p>
            </body>
            </html>
        """.trimIndent()
    }
}
