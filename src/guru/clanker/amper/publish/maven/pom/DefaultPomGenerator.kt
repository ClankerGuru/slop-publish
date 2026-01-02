package guru.clanker.amper.publish.maven.pom

import guru.clanker.amper.publish.domain.model.Artifact
import guru.clanker.amper.publish.domain.model.Publication
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class DefaultPomGenerator {

    fun generateMinimal(publication: Publication): String {
        return buildPom {
            modelVersion("4.0.0")
            groupId(publication.coordinates.groupId)
            artifactId(publication.coordinates.artifactId)
            version(publication.coordinates.version)
            packaging("jar")
        }
    }

    fun generateFull(publication: Publication): String {
        val metadata = publication.pomMetadata
            ?: return generateMinimal(publication)

        return buildPom {
            modelVersion("4.0.0")
            groupId(publication.coordinates.groupId)
            artifactId(publication.coordinates.artifactId)
            version(publication.coordinates.version)
            packaging("jar")

            name(metadata.name)
            description(metadata.description)
            url(metadata.url)

            licenses {
                metadata.licenses.forEach { license ->
                    license {
                        name(license.name)
                        url(license.url)
                    }
                }
            }

            developers {
                metadata.developers.forEach { dev ->
                    developer {
                        id(dev.id)
                        name(dev.name)
                        email(dev.email)
                    }
                }
            }

            scm {
                url(metadata.scm.url)
                connection(metadata.scm.connection)
                metadata.scm.developerConnection?.let { developerConnection(it) }
            }
        }
    }

    fun generateToFile(publication: Publication, outputPath: Path): Artifact {
        val content = if (publication.pomMetadata != null) {
            generateFull(publication)
        } else {
            generateMinimal(publication)
        }

        Files.createDirectories(outputPath.parent)
        Files.writeString(outputPath, content, StandardCharsets.UTF_8)
        return Artifact(outputPath, null, "pom")
    }

    private fun buildPom(block: PomBuilder.() -> Unit): String {
        return PomBuilder().apply(block).build()
    }
}

class PomBuilder {
    private val content = StringBuilder()
    private var indent = 1

    fun build(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
$content</project>"""
    }

    fun modelVersion(version: String) = element("modelVersion", version)
    fun groupId(id: String) = element("groupId", id)
    fun artifactId(id: String) = element("artifactId", id)
    fun version(version: String) = element("version", version)
    fun packaging(type: String) = element("packaging", type)
    fun name(name: String) = element("name", name)
    fun description(desc: String) = element("description", desc)
    fun url(url: String) = element("url", url)

    fun licenses(block: LicensesBuilder.() -> Unit) {
        content.appendLine("${"  ".repeat(indent)}<licenses>")
        indent++
        LicensesBuilder(content, indent).apply(block)
        indent--
        content.appendLine("${"  ".repeat(indent)}</licenses>")
    }

    fun developers(block: DevelopersBuilder.() -> Unit) {
        content.appendLine("${"  ".repeat(indent)}<developers>")
        indent++
        DevelopersBuilder(content, indent).apply(block)
        indent--
        content.appendLine("${"  ".repeat(indent)}</developers>")
    }

    fun scm(block: ScmBuilder.() -> Unit) {
        content.appendLine("${"  ".repeat(indent)}<scm>")
        indent++
        ScmBuilder(content, indent).apply(block)
        indent--
        content.appendLine("${"  ".repeat(indent)}</scm>")
    }

    private fun element(name: String, value: String) {
        content.appendLine("${"  ".repeat(indent)}<$name>${escapeXml(value)}</$name>")
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

class LicensesBuilder(private val content: StringBuilder, private var indent: Int) {
    fun license(block: LicenseBuilder.() -> Unit) {
        content.appendLine("${"  ".repeat(indent)}<license>")
        indent++
        LicenseBuilder(content, indent).apply(block)
        indent--
        content.appendLine("${"  ".repeat(indent)}</license>")
    }
}

class LicenseBuilder(private val content: StringBuilder, private val indent: Int) {
    fun name(name: String) = element("name", name)
    fun url(url: String) = element("url", url)
    private fun element(name: String, value: String) {
        content.appendLine("${"  ".repeat(indent)}<$name>${escapeXml(value)}</$name>")
    }
}

class DevelopersBuilder(private val content: StringBuilder, private var indent: Int) {
    fun developer(block: DeveloperBuilder.() -> Unit) {
        content.appendLine("${"  ".repeat(indent)}<developer>")
        indent++
        DeveloperBuilder(content, indent).apply(block)
        indent--
        content.appendLine("${"  ".repeat(indent)}</developer>")
    }
}

class DeveloperBuilder(private val content: StringBuilder, private val indent: Int) {
    fun id(id: String) = element("id", id)
    fun name(name: String) = element("name", name)
    fun email(email: String) = element("email", email)
    private fun element(name: String, value: String) {
        content.appendLine("${"  ".repeat(indent)}<$name>${escapeXml(value)}</$name>")
    }
}

class ScmBuilder(private val content: StringBuilder, private val indent: Int) {
    fun url(url: String) = element("url", url)
    fun connection(conn: String) = element("connection", conn)
    fun developerConnection(conn: String) = element("developerConnection", conn)
    private fun element(name: String, value: String) {
        content.appendLine("${"  ".repeat(indent)}<$name>${escapeXml(value)}</$name>")
    }
}

private fun escapeXml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
