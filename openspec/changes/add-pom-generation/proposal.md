# Change: Add POM Generation Module

## Why
Maven Central and most Maven repositories require a valid POM (Project Object Model) file describing the artifact. This module generates POM XML files from domain model objects, with support for both minimal POMs (local repos) and full POMs (Maven Central compliance).

## What Changes
- Create `PomGenerator` to generate POM XML from Publication
- Create `PomEnricher` to add optional metadata sections
- Create `PomValidator` to verify Maven Central compliance
- Support kotlinx.serialization for XML generation
- Validate required fields for different repository types

## Impact
- Affected specs: `pom-generation` (new capability)
- Affected code: `guru.clanker.amper.publish.maven.pom`
- Dependencies: Proposal 1 (Project Setup), Proposal 2 (Domain Model)

## Technical Approach

### POM Structure

Maven Central requires:
- `modelVersion` (always "4.0.0")
- `groupId`, `artifactId`, `version`
- `name`, `description`
- `url`
- `licenses` with at least one license
- `developers` with at least one developer
- `scm` with url and connection

### PomGenerator

```kotlin
// src/guru/clanker/amper/publish/maven/pom/ (Amper convention)

/**
 * Generates Maven POM files from domain model.
 */
class PomGenerator {
    
    /**
     * Generate minimal POM with only GAV coordinates.
     * Suitable for local repositories.
     */
    fun generateMinimal(publication: Publication): String {
        return buildPom {
            modelVersion("4.0.0")
            groupId(publication.coordinates.groupId)
            artifactId(publication.coordinates.artifactId)
            version(publication.coordinates.version)
            packaging("jar")
        }
    }
    
    /**
     * Generate full POM with all metadata.
     * Required for Maven Central.
     */
    fun generateFull(publication: Publication): String {
        val metadata = publication.pomMetadata
            ?: throw IllegalArgumentException("POM metadata required for full POM generation")
        
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
                developerConnection(metadata.scm.developerConnection)
            }
        }
    }
    
    /**
     * Generate POM to file.
     */
    fun generateToFile(publication: Publication, outputPath: Path, full: Boolean = true): Path {
        val content = if (full) generateFull(publication) else generateMinimal(publication)
        Files.writeString(outputPath, content, StandardCharsets.UTF_8)
        return outputPath
    }
}
```

### DSL Builder for POM XML

```kotlin
/**
 * DSL builder for constructing POM XML.
 */
class PomBuilder {
    private val root = StringBuilder()
    private var indent = 0
    
    fun build(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
${root}
</project>"""
    }
    
    fun modelVersion(version: String) = element("modelVersion", version)
    fun groupId(id: String) = element("groupId", id)
    fun artifactId(id: String) = element("artifactId", id)
    fun version(version: String) = element("version", version)
    fun packaging(type: String) = element("packaging", type)
    fun name(name: String) = element("name", name)
    fun description(desc: String) = element("description", escapeXml(desc))
    fun url(url: String) = element("url", url)
    
    fun licenses(block: LicensesBuilder.() -> Unit) {
        element("licenses") {
            LicensesBuilder(this).apply(block)
        }
    }
    
    fun developers(block: DevelopersBuilder.() -> Unit) {
        element("developers") {
            DevelopersBuilder(this).apply(block)
        }
    }
    
    fun scm(block: ScmBuilder.() -> Unit) {
        element("scm") {
            ScmBuilder(this).apply(block)
        }
    }
    
    private fun element(name: String, value: String) {
        root.appendLine("${"  ".repeat(indent + 1)}<$name>$value</$name>")
    }
    
    private fun element(name: String, block: () -> Unit) {
        root.appendLine("${"  ".repeat(indent + 1)}<$name>")
        indent++
        block()
        indent--
        root.appendLine("${"  ".repeat(indent + 1)}</$name>")
    }
}

fun buildPom(block: PomBuilder.() -> Unit): String {
    return PomBuilder().apply(block).build()
}
```

### PomValidator

```kotlin
/**
 * Validates POM against Maven Central requirements.
 */
class PomValidator {
    
    /**
     * Validate publication has required metadata for Maven Central.
     */
    fun validateForMavenCentral(publication: Publication): List<PomValidationError> {
        val errors = mutableListOf<PomValidationError>()
        val metadata = publication.pomMetadata
        
        if (metadata == null) {
            errors.add(PomValidationError.MissingMetadata("pomMetadata is required"))
            return errors
        }
        
        if (metadata.name.isBlank()) {
            errors.add(PomValidationError.MissingField("name"))
        }
        if (metadata.description.isBlank()) {
            errors.add(PomValidationError.MissingField("description"))
        }
        if (metadata.url.isBlank()) {
            errors.add(PomValidationError.MissingField("url"))
        }
        if (metadata.licenses.isEmpty()) {
            errors.add(PomValidationError.MissingField("licenses (at least one required)"))
        }
        if (metadata.developers.isEmpty()) {
            errors.add(PomValidationError.MissingField("developers (at least one required)"))
        }
        if (metadata.scm.url.isBlank()) {
            errors.add(PomValidationError.MissingField("scm.url"))
        }
        if (metadata.scm.connection.isBlank()) {
            errors.add(PomValidationError.MissingField("scm.connection"))
        }
        
        return errors
    }
}

sealed class PomValidationError {
    data class MissingMetadata(val message: String) : PomValidationError()
    data class MissingField(val field: String) : PomValidationError()
    data class InvalidFormat(val field: String, val message: String) : PomValidationError()
}
```

### XML Escaping

```kotlin
private fun escapeXml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
```

## Acceptance Criteria
- [ ] Generate minimal POM with GAV coordinates
- [ ] Generate full POM with all Maven Central required fields
- [ ] Validate POM against Maven Central requirements
- [ ] Proper XML escaping for special characters
- [ ] Unit tests verify POM structure and validity
- [ ] Integration test: generated POM parseable by Maven

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| XML generation edge cases | Comprehensive escaping, test with special chars |
| Maven Central requirements change | Document current requirements, version the validator |
| Large description/content | Use CDATA sections for very long content |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
