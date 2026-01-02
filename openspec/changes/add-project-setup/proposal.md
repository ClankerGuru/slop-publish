# Change: Add Project Setup & Architecture Foundation

## Why
The slop-publish plugin requires a well-structured project foundation before any feature development can begin. This establishes build configuration, dependency management, package structure, and architecture enforcement via Konsist tests. The plugin uses Amper's official plugin API (`jvm/amper-plugin`).

## What Changes
- Initialize Amper build with `module.yaml` using `product: jvm/amper-plugin`
- Create `plugin.yaml` with task registrations
- Create package structure following layered architecture (settings/tasks/domain/maven/infrastructure)
- Set up Konsist for architecture testing to enforce layer separation
- Configure GitHub Actions CI/CD pipeline
- Add basic README with development setup instructions

## Impact
- Affected specs: `project-setup` (new capability)
- Affected code: Build files, package structure, CI configuration
- Dependencies: None (foundational)

## Technical Approach

### Build System
Use Amper with `module.yaml` for declarative build configuration and `plugin.yaml` for task registration.

### Dependencies (in libs.versions.toml)
```toml
[versions]
maven = "3.9.6"
maven-resolver = "2.0.0"
ktor = "3.0.0"
kotlinx-serialization = "1.7.0"
kaml = "0.55.0"
bouncy-castle = "1.78"
konsist = "0.17.3"

[libraries]
maven-core = { module = "org.apache.maven:maven-core", version.ref = "maven" }
maven-model = { module = "org.apache.maven:maven-model", version.ref = "maven" }
maven-resolver-api = { module = "org.apache.maven.resolver:maven-resolver-api", version.ref = "maven-resolver" }
maven-resolver-impl = { module = "org.apache.maven.resolver:maven-resolver-impl", version.ref = "maven-resolver" }
maven-resolver-connector-basic = { module = "org.apache.maven.resolver:maven-resolver-connector-basic", version.ref = "maven-resolver" }
maven-resolver-transport-http = { module = "org.apache.maven.resolver:maven-resolver-transport-http", version.ref = "maven-resolver" }
maven-resolver-transport-file = { module = "org.apache.maven.resolver:maven-resolver-transport-file", version.ref = "maven-resolver" }

ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-serialization-kotlinx-xml = { module = "io.ktor:ktor-serialization-kotlinx-xml", version.ref = "ktor" }

kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

kaml = { module = "com.charleskorn.kaml:kaml", version.ref = "kaml" }

bcpg = { module = "org.bouncycastle:bcpg-jdk18on", version.ref = "bouncy-castle" }
bcprov = { module = "org.bouncycastle:bcprov-jdk18on", version.ref = "bouncy-castle" }

konsist = { module = "com.lemonappdev:konsist", version.ref = "konsist" }
```

### module.yaml
```yaml
product: jvm/amper-plugin

dependencies:
  # Maven Core
  - $libs.maven.core
  - $libs.maven.model
  - $libs.maven.resolver.api
  - $libs.maven.resolver.impl
  - $libs.maven.resolver.connector.basic
  - $libs.maven.resolver.transport.http
  - $libs.maven.resolver.transport.file
  
  # Ktor Client
  - $libs.ktor.client.core
  - $libs.ktor.client.cio
  - $libs.ktor.client.content.negotiation
  - $libs.ktor.serialization.kotlinx.json
  - $libs.ktor.serialization.kotlinx.xml
  
  # kotlinx.serialization
  - $libs.kotlinx.serialization.core
  - $libs.kotlinx.serialization.json
  
  # YAML parsing
  - $libs.kaml
  
  # Bouncy Castle (signing)
  - $libs.bcpg
  - $libs.bcprov

test-dependencies:
  - $kotlin.test
  - $libs.konsist

settings:
  jvm:
    release: 17
  kotlin:
    languageVersion: 2.0
    serialization: json

pluginInfo:
  settingsClass: guru.clanker.amper.publish.settings.PublishingSettings
```

### plugin.yaml
```yaml
tasks:
  publish:
    action: !guru.clanker.amper.publish.tasks.publish
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}
      buildDir: ${module.buildDir}
      target: ${pluginSettings.defaultTarget}
      dryRun: false

  publishLocal:
    action: !guru.clanker.amper.publish.tasks.publishLocal
      settings: ${pluginSettings}
      moduleDir: ${module.rootDir}
      buildDir: ${module.buildDir}

  validate:
    action: !guru.clanker.amper.publish.tasks.validate
      settings: ${pluginSettings}
```

### Package Structure

Note: Amper uses `src/` for main sources and `test/` for tests (no `main/kotlin` subdirectories).

```
slop-publish/
├── src/guru/clanker/amper/publish/     # Main sources (Amper convention)
│   ├── settings/                        # @Configurable interfaces
│   │   ├── PublishingSettings.kt        # Root settings (pluginInfo.settingsClass)
│   │   ├── RepositorySettings.kt        # Repository configuration
│   │   ├── PomSettings.kt               # POM metadata configuration
│   │   └── SigningSettings.kt           # Signing configuration
│   ├── tasks/                           # @TaskAction functions
│   │   ├── PublishTask.kt               # Main publish task
│   │   ├── PublishLocalTask.kt          # Local repository publish
│   │   └── ValidateTask.kt              # Validation task
│   ├── domain/                          # Pure domain model (no external deps)
│   │   ├── model/                       # Data classes
│   │   └── service/                     # Domain service interfaces
│   ├── maven/                           # Maven integration layer
│   │   ├── resolver/                    # Maven Resolver integration
│   │   ├── pom/                         # POM generation
│   │   └── signing/                     # Bouncy Castle signing
│   └── infrastructure/                  # Repository implementations
├── test/guru/clanker/amper/publish/     # Tests (Amper convention)
│   ├── settings/                        # Settings parsing tests
│   ├── tasks/                           # Task tests
│   ├── domain/                          # Domain model tests
│   └── architecture/                    # Konsist architecture tests
├── module.yaml                          # Plugin module configuration
└── plugin.yaml                          # Task registrations
```

### Architecture Rules (Konsist)
- Domain layer MUST NOT import from infrastructure, maven, or settings packages
- Domain layer MUST NOT use Maven, Ktor, or Bouncy Castle classes
- Settings classes MUST use `@Configurable` annotation
- Task actions MUST use `@TaskAction` annotation
- All public classes MUST have KDoc
- Test classes MUST end with "Test" suffix

## Acceptance Criteria
- [ ] Amper build compiles successfully with `product: jvm/amper-plugin`
- [ ] `plugin.yaml` registers publish, publishLocal, and validate tasks
- [ ] `@Configurable` root settings interface created
- [ ] Package structure created with placeholder classes
- [ ] Konsist architecture tests pass
- [ ] GitHub Actions CI runs tests on push/PR
- [ ] README contains development setup instructions
- [ ] JVM 17 target configured

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Maven dependency conflicts | Use explicit version alignment in libs.versions.toml |
| Konsist version compatibility | Pin to stable version, test on CI |
| Amper plugin API changes | Follow official tutorial, check release notes |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
