package guru.clanker.amper.publish.maven.pom

import guru.clanker.amper.publish.domain.model.Artifact
import guru.clanker.amper.publish.domain.model.Publication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class DefaultPomGenerator {

    private val xml = XML {
        indent = 2
        xmlDeclMode = nl.adaptivity.xmlutil.XmlDeclMode.None
    }

    fun generateMinimal(publication: Publication): String {
        val pom = PomProject(
            modelVersion = "4.0.0",
            groupId = publication.coordinates.groupId,
            artifactId = publication.coordinates.artifactId,
            version = publication.coordinates.version,
            packaging = "jar"
        )
        return formatPom(xml.encodeToString(PomProject.serializer(), pom))
    }

    fun generateFull(publication: Publication): String {
        val metadata = publication.pomMetadata
            ?: return generateMinimal(publication)

        val pom = PomProject(
            modelVersion = "4.0.0",
            groupId = publication.coordinates.groupId,
            artifactId = publication.coordinates.artifactId,
            version = publication.coordinates.version,
            packaging = "jar",
            name = metadata.name,
            description = metadata.description,
            url = metadata.url,
            licenses = PomLicenses(
                licenses = metadata.licenses.map { license ->
                    PomLicense(name = license.name, url = license.url)
                }
            ),
            developers = PomDevelopers(
                developers = metadata.developers.map { dev ->
                    PomDeveloper(id = dev.id, name = dev.name, email = dev.email)
                }
            ),
            scm = PomScm(
                url = metadata.scm.url,
                connection = metadata.scm.connection,
                developerConnection = metadata.scm.developerConnection
            )
        )
        return formatPom(xml.encodeToString(PomProject.serializer(), pom))
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

    private fun formatPom(xmlContent: String): String {
        val header = """<?xml version="1.0" encoding="UTF-8"?>"""
        val content = xmlContent
            .replace(Regex("<project[^>]*>"), """<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">""")
        return "$header\n$content"
    }
}

@Serializable
@XmlSerialName("project")
data class PomProject(
    @XmlElement(true) val modelVersion: String,
    @XmlElement(true) val groupId: String,
    @XmlElement(true) val artifactId: String,
    @XmlElement(true) val version: String,
    @XmlElement(true) val packaging: String,
    @XmlElement(true) val name: String? = null,
    @XmlElement(true) val description: String? = null,
    @XmlElement(true) val url: String? = null,
    val licenses: PomLicenses? = null,
    val developers: PomDevelopers? = null,
    val scm: PomScm? = null
)

@Serializable
@XmlSerialName("licenses")
data class PomLicenses(
    @XmlElement(true)
    @SerialName("license")
    val licenses: List<PomLicense>
)

@Serializable
@XmlSerialName("license")
data class PomLicense(
    @XmlElement(true) val name: String,
    @XmlElement(true) val url: String
)

@Serializable
@XmlSerialName("developers")
data class PomDevelopers(
    @XmlElement(true)
    @SerialName("developer")
    val developers: List<PomDeveloper>
)

@Serializable
@XmlSerialName("developer")
data class PomDeveloper(
    @XmlElement(true) val id: String,
    @XmlElement(true) val name: String,
    @XmlElement(true) val email: String
)

@Serializable
@XmlSerialName("scm")
data class PomScm(
    @XmlElement(true) val url: String,
    @XmlElement(true) val connection: String,
    @XmlElement(true) val developerConnection: String? = null
)
