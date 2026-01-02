# Project Context

## Plugin Metadata
- **Name**: slop-publish
- **Group ID**: guru.clanker
- **Root Package**: guru.clanker.amper.publish
- **License**: MIT
- **Minimum JVM**: 17

## Purpose
Develop an Amper plugin that enables publishing artifacts to Maven repositories and GitHub Packages. The plugin integrates with Amper's build lifecycle using the official plugin API (`jvm/amper-plugin`), providing declarative configuration in `module.yaml` and tasks that can be invoked via `./amper task`.

## Amper Plugin Architecture

This plugin follows the [Amper Plugin Tutorial](https://github.com/JetBrains/amper-plugins-tutorial) and [Plugin Quick Start](https://amper.org/latest/user-guide/plugins/quick-start/).

### Plugin Structure
```
slop-publish/
├── src/guru/clanker/amper/publish/     # Plugin sources
│   ├── settings/                        # @Configurable interfaces
│   │   └── PublishingSettings.kt        # Root settings class
│   ├── tasks/                           # @TaskAction functions
│   │   ├── PublishTask.kt               # Main publish task
│   │   ├── PublishLocalTask.kt          # Local repo publish
│   │   └── ValidateTask.kt              # Validation task
│   ├── domain/                          # Domain models (pure Kotlin)
│   ├── maven/                           # Maven integration
│   └── infrastructure/                  # Repository implementations
├── test/                                # Tests
├── module.yaml                          # Plugin module config
└── plugin.yaml                          # Task registrations
```

### Key Plugin Files

#### module.yaml
```yaml
product: jvm/amper-plugin

dependencies:
  - org.apache.maven.resolver:maven-resolver-api:2.0.0
  - io.ktor:ktor-client-core:3.0.0
  # ... other dependencies

pluginInfo:
  settingsClass: guru.clanker.amper.publish.settings.PublishingSettings
```

#### plugin.yaml
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

### Plugin Usage

Users enable the plugin in their `module.yaml`:

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
    
    repositories:
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repo
        credentials:
          username: ${GITHUB_ACTOR}
          password: ${GITHUB_TOKEN}
    
    pom:
      name: My Library
      description: A sample library
      url: https://github.com/example/my-library
      licenses:
        - name: MIT
          url: https://opensource.org/licenses/MIT
```

Users invoke tasks via CLI:
```bash
./amper task :my-module:publish@slop-publish
./amper task :my-module:publishLocal@slop-publish
./amper task :my-module:validate@slop-publish
```

## Tech Stack
- **Kotlin** (for plugin implementation)
- **Amper** (project configuration and build system)
- **Amper Plugin API** (`jvm/amper-plugin` product type)
- **Maven Resolver/Aether** (for artifact resolution and deployment)
- **Ktor Client** (io.ktor:ktor-client-*) - HTTP client for GitHub Packages API
- **kotlinx.serialization** - JSON/XML serialization (with Ktor integration)
- **Bouncy Castle** (org.bouncycastle:bcpg-jdk18on) - PGP signing for Maven Central
- **Konsist** (for architecture and code structure testing)

> **Dependency Policy**: Only use JetBrains, kotlinx, and Apache Maven libraries. No third-party HTTP clients or serialization libs.

## Project Conventions

### Code Style
- Follow Kotlin official coding conventions
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Prefer explicit types for public API, type inference for internal code
- Use meaningful variable names (e.g., `mavenRepository` not `repo`)
- Document all public APIs with KDoc
- Naming: `camelCase` for functions/variables, `PascalCase` for classes

### Architecture Patterns
- **Amper Plugin API**: Use `@TaskAction`, `@Configurable`, `@Input`/`@Output` annotations
- **Facade Pattern**: Provide clean configuration over Maven libraries
- **Builder Pattern**: Use for repository and publication configuration
- **Separation of Concerns**: 
  - Settings parsing (`@Configurable` interfaces → internal model)
  - Publication preparation (POM generation, artifact collection)
  - Repository interaction (using Maven Resolver)
- **Extensibility**: Allow custom artifact configurations and repository types
- **Layered Architecture**:
  - Settings layer (`@Configurable` interfaces)
  - Tasks layer (`@TaskAction` functions)
  - Domain layer (publication model, repository abstraction)
  - Maven integration layer (translate to Maven concepts)
  - Infrastructure layer (Maven Resolver, GitHub Packages)

### Testing Strategy
- **Unit Tests**: Test individual components (configuration parsing, POM generation, model translation)
- **Integration Tests**: Test against local Maven repository and test repositories
- **Architecture Tests with Konsist**: 
  - Enforce package structure and dependencies
  - Verify naming conventions
  - Ensure layer separation (domain doesn't depend on infrastructure)
  - Validate that repository implementations follow interfaces
  - Check that all public APIs have documentation
  - Ensure Maven-specific classes don't leak into domain layer
- **Test Coverage**: Minimum 80% for core publishing logic

### Konsist Architecture Rules
- Domain layer must not depend on infrastructure layer
- Domain layer must not depend on Maven-specific classes
- All public API classes must have KDoc
- Repository implementations must implement `PublishingRepository` interface
- Test classes must end with `Test` suffix
- Configuration classes must be in `settings` package
- Maven integration classes must be in `maven` package
- Task actions must be in `tasks` package
- All classes in `domain` package must not use external HTTP/network libraries

### Git Workflow
- **Branching**: 
  - `main` - stable releases
  - `develop` - integration branch
  - `feature/*` - new features
  - `bugfix/*` - bug fixes
- **Commit Convention**: 
  - `feat:` - new features
  - `fix:` - bug fixes
  - `docs:` - documentation
  - `test:` - testing
  - `refactor:` - code refactoring
  - `arch:` - architecture changes
- **PR Requirements**: All changes via pull request, require at least one review
- **CI Checks**: Must pass all tests including Konsist architecture tests
- **Version Tagging**: Use semantic versioning (v1.0.0, v1.1.0, etc.)

## Domain Context

### Maven Publishing Concepts
- **Maven Coordinates**: groupId, artifactId, version (GAV)
- **POM Files**: Project Object Model XML describing artifact metadata
- **Artifact Types**: JAR, sources JAR, javadoc JAR, POM
- **Repository Types**: 
  - Local (`~/.m2/repository`)
  - Remote (Maven Central, company Nexus/Artifactory)
  - Snapshot vs. Release repositories
- **Checksums**: MD5 and SHA-1/SHA-256 for artifact verification
- **Signatures**: GPG/PGP signatures required for Maven Central

### GitHub Packages Concepts
- **Authentication**: Personal Access Token (PAT) or GitHub Actions token
- **Repository Format**: Maven-compatible API
- **URL Pattern**: `https://maven.pkg.github.com/{owner}/{repository}`
- **Visibility**: Public packages in public repos, private packages require authentication

### Amper Plugin Concepts
- **Product Type**: `jvm/amper-plugin` declares a plugin module
- **Plugin ID**: Defaults to module name, used to reference plugin
- **@Configurable**: Interface defining plugin settings schema
- **@TaskAction**: Annotated function that can be registered as a task
- **@Input/@Output**: Path annotations for task execution avoidance
- **plugin.yaml**: Declares tasks and wires settings to task parameters
- **pluginSettings**: Reference to configured settings in plugin.yaml
- **References**: `${module.rootDir}`, `${taskOutputDir}`, `${pluginSettings.foo}`

## Important Constraints

### Technical Constraints
- Must use Amper's official plugin API (`jvm/amper-plugin`)
- Compatible with JVM 17+ (project minimum requirement)
- Plugin configuration via `@Configurable` interfaces
- Task registration via `plugin.yaml`
- Tasks invoked via `./amper task :module:taskName@plugin-id`
- Authentication credentials should never be logged or exposed
- Support both Maven and GitHub Packages with unified configuration model

### Maven Central Requirements
- All artifacts must have sources and javadoc JARs
- All artifacts must be GPG signed
- POM must include: name, description, url, licenses, developers, scm
- No SNAPSHOT versions on release repository

### GitHub Packages Requirements
- Authentication via Personal Access Token (PAT) or GITHUB_TOKEN
- Repository URL must match GitHub owner/repo structure
- Support for both public and private packages
- Support GitHub Actions environment variables

### Security Constraints
- Credentials via environment variables (e.g., `${MAVEN_USERNAME}`)
- No credentials in version control or logs
- Validate artifact checksums before upload

### Architecture Constraints (Enforced by Konsist)
- Clear separation between domain and infrastructure
- Repository abstraction must allow easy addition of new repository types
- Settings parsing isolated from business logic
- No circular dependencies between packages
- Maven integration layer must isolate Maven APIs from domain
- Maven classes must not leak into public API

## External Dependencies

### Dependency Policy
**Prefer in this order**: JetBrains libraries → kotlinx libraries → Apache Maven libraries → nothing else.
Avoid third-party HTTP clients (OkHttp, Apache HttpClient) and serialization libraries (Jackson, Gson).

**Exception**: Bouncy Castle for PGP signing - no JetBrains/kotlinx alternative exists for cryptographic signing.

### Core Libraries
- **Amper Plugin API** (org.jetbrains.amper:amper-plugin-api) - plugin annotations and types
- **Maven Resolver** (org.apache.maven.resolver:maven-resolver-*) - artifact deployment
- **Ktor Client** (io.ktor:ktor-client-*) - HTTP client for GitHub Packages
- **kotlinx.serialization** (org.jetbrains.kotlinx:kotlinx-serialization-*) - serialization
- **kaml** (com.charleskorn.kaml:kaml) - YAML parsing with kotlinx.serialization
- **Konsist** (com.lemonappdev:konsist) - architecture testing
- **Bouncy Castle** (org.bouncycastle:bcpg-jdk18on) - PGP signing for Maven Central

### External Services
- **Maven Central** (oss.sonatype.org) - primary target repository
- **GitHub Packages** (maven.pkg.github.com) - GitHub-hosted Maven repository
- **Sonatype Nexus** - common enterprise repository
- **JFrog Artifactory** - alternative enterprise repository
- **Local Maven Repository** - `~/.m2/repository`

## Expected Configuration Format

Example user `module.yaml` configuration:
```yaml
product: jvm/lib

plugins:
  slop-publish:
    enabled: true
    
    # Maven coordinates
    groupId: guru.clanker
    artifactId: my-library
    version: 1.0.0
    
    # Artifact types to publish
    artifacts:
      - jar
      - sources
      - javadoc
    
    # Repository definitions
    repositories:
      # Maven Central
      - id: mavenCentral
        type: maven
        url: https://oss.sonatype.org/service/local/staging/deploy/maven2/
        credentials:
          username: ${MAVEN_USERNAME}
          password: ${MAVEN_PASSWORD}
      
      # GitHub Packages
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repository
        credentials:
          username: ${GITHUB_ACTOR}
          password: ${GITHUB_TOKEN}
      
      # Local repository
      - id: local
        type: local
        url: file://${user.home}/.m2/repository
    
    # POM metadata (required for Maven Central)
    pom:
      name: My Library
      description: A sample library
      url: https://github.com/example/my-library
      licenses:
        - name: Apache-2.0
          url: https://www.apache.org/licenses/LICENSE-2.0.txt
      developers:
        - name: John Doe
          email: john@example.com
      scm:
        url: https://github.com/example/my-library
        connection: scm:git:git://github.com/example/my-library.git
        developerConnection: scm:git:ssh://github.com/example/my-library.git
    
    # PGP signing configuration
    signing:
      enabled: true
      key: ${GPG_KEY}
      password: ${GPG_PASSWORD}
      skipForRepositories:
        - github
        - local
    
    # Publishing targets (named groups of repositories)
    targets:
      default:
        - github
      release:
        - mavenCentral
      all:
        - github
        - mavenCentral
    
    # Default target when running publish task
    defaultTarget: default
```

## Package Structure

Note: Amper uses `src/` for main sources and `test/` for tests (no `main/kotlin` or `test/kotlin` subdirectories).

```
slop-publish/
├── src/guru/clanker/amper/publish/     # Main sources (Amper convention)
│   ├── settings/                        # @Configurable interfaces
│   │   ├── PublishingSettings.kt        # Root settings (pluginInfo.settingsClass)
│   │   ├── RepositorySettings.kt        # Repository config
│   │   ├── PomSettings.kt               # POM metadata config
│   │   └── SigningSettings.kt           # Signing config
│   ├── tasks/                           # @TaskAction functions
│   │   ├── PublishTask.kt               # publish task action
│   │   ├── PublishLocalTask.kt          # publishLocal task action
│   │   └── ValidateTask.kt              # validate task action
│   ├── domain/                          # Domain models (pure, no Maven deps)
│   │   ├── model/
│   │   │   ├── Coordinates.kt
│   │   │   ├── Artifact.kt
│   │   │   ├── Repository.kt
│   │   │   └── Publication.kt
│   │   └── service/
│   │       └── PublishingService.kt     # Interface
│   ├── maven/                           # Maven integration layer
│   │   ├── resolver/
│   │   │   ├── MavenDeployer.kt
│   │   │   └── ResolverFactory.kt
│   │   ├── pom/
│   │   │   └── PomGenerator.kt
│   │   └── signing/
│   │       └── ArtifactSigner.kt
│   └── infrastructure/                  # Repository implementations
│       ├── MavenRepositoryPublisher.kt
│       ├── GitHubPackagesPublisher.kt
│       └── LocalRepositoryPublisher.kt
├── test/guru/clanker/amper/publish/     # Tests (Amper convention)
│   ├── domain/                          # Domain model tests
│   ├── settings/                        # Settings parsing tests
│   ├── tasks/                           # Task tests
│   └── architecture/                    # Konsist architecture tests
│       └── ArchitectureTest.kt
├── module.yaml                          # Plugin module configuration
└── plugin.yaml                          # Task registrations
```

## Implementation Priorities

### Phase 1: Plugin Foundation
- Setup project structure with `jvm/amper-plugin`
- Create `@Configurable` settings interfaces
- Create `plugin.yaml` with task registrations
- Implement basic `@TaskAction` functions
- Konsist architecture tests setup

### Phase 2: Core Maven Publishing
- Implement Maven Resolver integration
- POM generation from settings
- Local repository publishing (`publishLocal` task)
- Checksum generation and validation

### Phase 3: Remote Maven Repositories
- Maven Central publishing
- Authentication handling
- Remote repository publishing
- Error handling and retries

### Phase 4: GitHub Packages
- GitHub Packages adapter
- GitHub authentication (PAT, GITHUB_TOKEN)
- GitHub-specific configuration

### Phase 5: Artifact Signing (Bouncy Castle)
- PGP signing with Bouncy Castle
- Key management
- Selective signing (skip for GitHub, required for Maven Central)

### Phase 6: Polish & Documentation
- User documentation
- Example projects
- Troubleshooting guide

## Key Technical Decisions

### Why Amper Plugin API?
- **Native Integration**: First-class citizen in Amper build lifecycle
- **Declarative Config**: Users configure in familiar `module.yaml` format
- **Task System**: Execution avoidance, dependency tracking built-in
- **IDE Support**: Settings visible with tooltips in YAML editor

### Why Maven Resolver over Maven CLI?
- **Programmatic control**: Direct API access vs. subprocess execution
- **Better error handling**: Catch exceptions vs. parsing stdout/stderr
- **Performance**: No JVM startup overhead
- **Integration**: Works seamlessly in JVM plugin

### Why Bouncy Castle over GPG CLI?
- **Cross-platform**: Consistent behavior on Linux/macOS/Windows
- **CI/CD friendly**: Pass keys as environment variables
- **Testable**: Mock signing in unit tests without gpg installed

## Reference Documentation
- [Amper Plugin Quick Start](https://amper.org/latest/user-guide/plugins/quick-start/)
- [Amper Plugin Structure](https://amper.org/latest/user-guide/plugins/topics/structure/)
- [Amper Plugin Configuration](https://amper.org/latest/user-guide/plugins/topics/configuration/)
- [Amper Plugin Tasks](https://amper.org/latest/user-guide/plugins/topics/tasks/)
- [Maven Resolver](https://maven.apache.org/resolver/)
- [GitHub Packages Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
- [Konsist Documentation](https://docs.konsist.lemonappdev.com/)
- [Bouncy Castle](https://www.bouncycastle.org/java.html)
