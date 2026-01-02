# Change: Add Plugin Settings Module

## Why
The slop-publish plugin needs to accept user configuration from `module.yaml`. Using Amper's `@Configurable` annotation system, we define settings interfaces that Amper automatically populates from YAML. This provides type-safe configuration with IDE support, validation, and default values.

## What Changes
- Create `@Configurable` interfaces for plugin settings (replaces manual YAML parsing)
- Define `PublishingSettings` as root settings class (referenced in `pluginInfo.settingsClass`)
- Implement nested settings for repositories, POM metadata, and signing
- Add validation logic with clear error messages
- Support environment variable references via Amper's `${VAR}` syntax

## Impact
- Affected specs: `config-parsing` → renamed to `plugin-settings`
- Affected code: `guru.clanker.amper.publish.settings`
- Dependencies: Proposal 1 (Project Setup), Proposal 2 (Domain Model)

## Technical Approach

### Amper Plugin Settings System

With Amper's plugin API, configuration is declarative:
1. Define `@Configurable` interfaces with properties
2. Amper parses YAML and populates the interface
3. Access settings via `${pluginSettings}` in `plugin.yaml`
4. Settings are passed to `@TaskAction` functions

No manual YAML parsing required - Amper handles it.

### Root Settings Interface

```kotlin
// src/guru/clanker/amper/publish/settings/PublishingSettings.kt

package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

/**
 * Root configuration for slop-publish plugin.
 * 
 * Users configure this in their module.yaml:
 * ```yaml
 * plugins:
 *   slop-publish:
 *     enabled: true
 *     groupId: com.example
 *     artifactId: my-lib
 *     version: 1.0.0
 * ```
 */
@Configurable
interface PublishingSettings {
    /** Maven group ID */
    val groupId: String
    
    /** Maven artifact ID */
    val artifactId: String
    
    /** Version string */
    val version: String
    
    /** Artifact types to publish (jar, sources, javadoc) */
    val artifacts: List<String> get() = listOf("jar", "sources", "javadoc")
    
    /** Repository configurations */
    val repositories: List<RepositorySettings> get() = emptyList()
    
    /** POM metadata (required for Maven Central) */
    val pom: PomSettings? get() = null
    
    /** Signing configuration */
    val signing: SigningSettings? get() = null
    
    /** Named targets mapping to repository IDs */
    val targets: Map<String, List<String>> get() = emptyMap()
    
    /** Default target when running publish task */
    val defaultTarget: String get() = "default"
}
```

### Repository Settings

```kotlin
// src/guru/clanker/amper/publish/settings/RepositorySettings.kt

package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

/**
 * Repository configuration.
 * 
 * Example:
 * ```yaml
 * repositories:
 *   - id: github
 *     type: github
 *     url: https://maven.pkg.github.com/owner/repo
 *     credentials:
 *       username: ${GITHUB_ACTOR}
 *       password: ${GITHUB_TOKEN}
 * ```
 */
@Configurable
interface RepositorySettings {
    /** Unique repository identifier */
    val id: String
    
    /** Repository type: maven, github, or local */
    val type: String get() = "maven"
    
    /** Repository URL */
    val url: String
    
    /** Authentication credentials */
    val credentials: CredentialsSettings? get() = null
}

@Configurable
interface CredentialsSettings {
    /** Username or token name */
    val username: String
    
    /** Password or token value */
    val password: String
}
```

### POM Metadata Settings

```kotlin
// src/guru/clanker/amper/publish/settings/PomSettings.kt

package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

/**
 * POM metadata for Maven Central compliance.
 * 
 * Example:
 * ```yaml
 * pom:
 *   name: My Library
 *   description: A sample library
 *   url: https://github.com/example/my-lib
 *   licenses:
 *     - name: MIT
 *       url: https://opensource.org/licenses/MIT
 *   developers:
 *     - id: johndoe
 *       name: John Doe
 *       email: john@example.com
 *   scm:
 *     url: https://github.com/example/my-lib
 *     connection: scm:git:git://github.com/example/my-lib.git
 * ```
 */
@Configurable
interface PomSettings {
    /** Project name */
    val name: String
    
    /** Project description */
    val description: String
    
    /** Project URL */
    val url: String
    
    /** License information */
    val licenses: List<LicenseSettings>
    
    /** Developer information */
    val developers: List<DeveloperSettings>
    
    /** Source control information */
    val scm: ScmSettings
}

@Configurable
interface LicenseSettings {
    val name: String
    val url: String
}

@Configurable
interface DeveloperSettings {
    val id: String get() = ""
    val name: String
    val email: String get() = ""
}

@Configurable
interface ScmSettings {
    val url: String
    val connection: String
    val developerConnection: String get() = ""
}
```

### Signing Settings

```kotlin
// src/guru/clanker/amper/publish/settings/SigningSettings.kt

package guru.clanker.amper.publish.settings

import org.jetbrains.amper.plugins.Configurable

/**
 * PGP signing configuration.
 * 
 * Example:
 * ```yaml
 * signing:
 *   enabled: true
 *   key: ${GPG_KEY}
 *   password: ${GPG_PASSWORD}
 *   skipForRepositories:
 *     - github
 *     - local
 * ```
 */
@Configurable
interface SigningSettings {
    /** Enable/disable signing */
    val enabled: Boolean get() = false
    
    /** ASCII-armored private key (from env var) */
    val key: String get() = ""
    
    /** Key passphrase */
    val password: String get() = ""
    
    /** Key ID (optional, defaults to first key in keyring) */
    val keyId: String get() = ""
    
    /** Repositories to skip signing for */
    val skipForRepositories: List<String> get() = emptyList()
}
```

### Settings to Domain Conversion

```kotlin
// src/guru/clanker/amper/publish/settings/SettingsMapper.kt

package guru.clanker.amper.publish.settings

import guru.clanker.amper.publish.domain.model.*

/**
 * Converts @Configurable settings to domain model objects.
 */
object SettingsMapper {
    
    fun toCoordinates(settings: PublishingSettings): Coordinates {
        return Coordinates(
            groupId = settings.groupId,
            artifactId = settings.artifactId,
            version = settings.version
        )
    }
    
    fun toRepository(settings: RepositorySettings): Repository {
        return when (settings.type.lowercase()) {
            "github" -> Repository.GitHubPackages(
                id = settings.id,
                url = settings.url,
                token = settings.credentials?.password ?: "",
                owner = extractGitHubOwner(settings.url),
                repository = extractGitHubRepo(settings.url)
            )
            "local" -> Repository.Local(
                id = settings.id,
                url = settings.url
            )
            else -> Repository.Maven(
                id = settings.id,
                url = settings.url,
                credentials = settings.credentials?.let {
                    Credentials(it.username, it.password)
                },
                isSnapshot = settings.url.contains("snapshot", ignoreCase = true)
            )
        }
    }
    
    fun toPomMetadata(settings: PomSettings): PomMetadata {
        return PomMetadata(
            name = settings.name,
            description = settings.description,
            url = settings.url,
            licenses = settings.licenses.map { License(it.name, it.url) },
            developers = settings.developers.map { 
                Developer(it.id, it.name, it.email) 
            },
            scm = Scm(
                url = settings.scm.url,
                connection = settings.scm.connection,
                developerConnection = settings.scm.developerConnection.ifEmpty { null }
            )
        )
    }
    
    private fun extractGitHubOwner(url: String): String {
        // https://maven.pkg.github.com/owner/repo -> owner
        return url.removePrefix("https://maven.pkg.github.com/")
            .split("/").getOrNull(0) ?: ""
    }
    
    private fun extractGitHubRepo(url: String): String {
        // https://maven.pkg.github.com/owner/repo -> repo
        return url.removePrefix("https://maven.pkg.github.com/")
            .split("/").getOrNull(1) ?: ""
    }
}
```

### Validation

```kotlin
// src/guru/clanker/amper/publish/settings/SettingsValidator.kt

package guru.clanker.amper.publish.settings

import guru.clanker.amper.publish.domain.service.ValidationError

object SettingsValidator {
    
    fun validate(settings: PublishingSettings): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        // Required fields
        if (settings.groupId.isBlank()) {
            errors += ValidationError("groupId", "groupId is required", ValidationError.Severity.ERROR)
        }
        if (settings.artifactId.isBlank()) {
            errors += ValidationError("artifactId", "artifactId is required", ValidationError.Severity.ERROR)
        }
        if (settings.version.isBlank()) {
            errors += ValidationError("version", "version is required", ValidationError.Severity.ERROR)
        }
        
        // Check for Maven Central requirements
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
        
        // Validate repository IDs are unique
        val repoIds = settings.repositories.map { it.id }
        val duplicates = repoIds.groupBy { it }.filter { it.value.size > 1 }.keys
        duplicates.forEach { dup ->
            errors += ValidationError("repositories", "Duplicate repository ID: $dup", ValidationError.Severity.ERROR)
        }
        
        // Validate targets reference valid repositories
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
```

## Acceptance Criteria
- [ ] `@Configurable` interfaces define all plugin settings
- [ ] `PublishingSettings` registered as `pluginInfo.settingsClass`
- [ ] Settings accessible via `${pluginSettings}` in plugin.yaml
- [ ] Default values provided for optional settings
- [ ] Validation catches missing required fields
- [ ] Validation enforces Maven Central POM requirements
- [ ] SettingsMapper converts to domain objects
- [ ] KDoc on all public interfaces (visible in IDE tooltips)
- [ ] Unit tests for validation and mapping

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Amper @Configurable API changes | Follow official plugin tutorial, check release notes |
| Complex nested configuration | Keep nesting shallow, use sensible defaults |
| Environment variable not resolved | Document that Amper handles ${VAR} resolution |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
