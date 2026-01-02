# Change: Add Comprehensive Testing Infrastructure

## Why
Robust testing ensures reliability and catches regressions. This includes unit tests, integration tests, end-to-end tests, and Konsist architecture tests that enforce the layered architecture.

## What Changes
- Unit tests for all modules (80%+ coverage)
- Integration tests for publishing workflows
- Konsist architecture tests enforcing layer separation
- Test fixtures (sample YAML, test artifacts, test PGP keys)
- CI pipeline running all tests

## Impact
- Affected specs: `testing-infra` (new capability)
- Affected code: `test/guru/clanker/amper/publish/`
- Dependencies: All previous proposals

## Technical Approach

### Test Directory Structure

Note: Amper uses `test/` directly for tests (no `test/kotlin` subdirectory).

```
test/guru/clanker/amper/publish/
├── domain/
│   ├── model/
│   │   ├── CoordinatesTest.kt
│   │   ├── ArtifactTest.kt
│   │   ├── RepositoryTest.kt
│   │   ├── PublicationTest.kt
│   │   └── PublishingResultTest.kt
│   └── service/
│       └── PublishingServiceTest.kt
├── settings/
│   ├── PublishingSettingsTest.kt
│   ├── SettingsMapperTest.kt
│   └── SettingsValidatorTest.kt
├── tasks/
│   ├── PublishTaskTest.kt
│   ├── PublishLocalTaskTest.kt
│   └── ValidateTaskTest.kt
├── maven/
│   ├── resolver/
│   │   ├── ResolverFactoryTest.kt
│   │   ├── MavenDeployerTest.kt
│   │   └── ChecksumGeneratorTest.kt
│   ├── pom/
│   │   ├── PomGeneratorTest.kt
│   │   └── PomValidatorTest.kt
│   └── signing/
│       └── ArtifactSignerTest.kt
├── infrastructure/
│   ├── LocalRepositoryPublisherTest.kt
│   ├── MavenRepositoryPublisherTest.kt
│   ├── GitHubPackagesPublisherTest.kt
│   ├── RateLimiterTest.kt
│   └── RetryPolicyTest.kt
├── integration/
│   ├── LocalPublishIntegrationTest.kt
│   ├── MavenPublishIntegrationTest.kt
│   ├── GitHubPackagesIntegrationTest.kt
│   └── SigningIntegrationTest.kt
├── e2e/
│   └── FullPublishingWorkflowTest.kt
└── architecture/
    └── ArchitectureTest.kt

test-fixtures/
├── yaml/
│   ├── valid-minimal.yaml
│   ├── valid-full.yaml
│   ├── valid-github.yaml
│   └── with-env-vars.yaml
├── artifacts/
│   ├── sample-1.0.0.jar
│   ├── sample-1.0.0-sources.jar
│   └── sample-1.0.0-javadoc.jar
└── keys/
    ├── test-key.asc
    └── test-keyring.gpg
```

### Konsist Architecture Tests

Note: Konsist accesses imports via `containingFile.imports` on class declarations.

```kotlin
// test/guru/clanker/amper/publish/architecture/ArchitectureTest.kt

class ArchitectureTest {
    
    @Test
    fun `domain layer should not depend on infrastructure`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.contains(".infrastructure.") }
            }
    }
    
    @Test
    fun `domain layer should not depend on maven package`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.contains(".maven.") }
            }
    }
    
    @Test
    fun `domain layer should not depend on settings package`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.contains(".settings.") }
            }
    }
    
    @Test
    fun `domain layer should not import Maven libraries`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { import ->
                    import.name.startsWith("org.apache.maven") ||
                    import.name.startsWith("org.eclipse.aether")
                }
            }
    }
    
    @Test
    fun `domain layer should not import Ktor`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.startsWith("io.ktor") }
            }
    }
    
    @Test
    fun `domain layer should not import Bouncy Castle`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.startsWith("org.bouncycastle") }
            }
    }
    
    @Test
    fun `domain layer should not import Amper plugin API`() {
        Konsist
            .scopeFromPackage("guru.clanker.amper.publish.domain..")
            .classes()
            .assertFalse { clazz ->
                clazz.containingFile.imports.any { it.name.startsWith("org.jetbrains.amper.plugins") }
            }
    }
    
    @Test
    fun `test classes should end with Test suffix`() {
        Konsist
            .scopeFromTest()
            .classes()
            .assertTrue { it.name.endsWith("Test") }
    }
    
    @Test
    fun `interfaces do not have I prefix`() {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .assertFalse { it.name.startsWith("I") && it.name.getOrNull(1)?.isUpperCase() == true }
    }
    
    @Test
    fun `layer dependencies are respected`() {
        Konsist
            .scopeFromProduction()
            .assertArchitecture {
                val settings = Layer("Settings", "guru.clanker.amper.publish.settings..")
                val tasks = Layer("Tasks", "guru.clanker.amper.publish.tasks..")
                val domain = Layer("Domain", "guru.clanker.amper.publish.domain..")
                val maven = Layer("Maven", "guru.clanker.amper.publish.maven..")
                val infra = Layer("Infrastructure", "guru.clanker.amper.publish.infrastructure..")

                domain.dependsOnNothing()
                settings.dependsOn(domain)
                maven.dependsOn(domain)
                infra.dependsOn(domain, maven)
                tasks.dependsOn(settings, domain, infra, maven)
            }
    }
}
```

### Integration Test Example

```kotlin
class LocalPublishIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var localRepoPath: Path
    private lateinit var publisher: LocalRepositoryPublisher
    
    @BeforeEach
    fun setup() {
        localRepoPath = tempDir.resolve("m2repo")
        publisher = LocalRepositoryPublisher(localRepoPath)
    }
    
    @Test
    fun `should publish artifact to correct path`() = runBlocking {
        val publication = Publication(
            coordinates = Coordinates("com.example", "test-lib", "1.0.0"),
            artifacts = listOf(Artifact(createTestJar(), null, "jar"))
        )
        
        val result = publisher.publish(publication, Repository.Local.default())
        
        assertThat(result).isInstanceOf(PublishingResult.Success::class.java)
        
        val expectedPath = localRepoPath.resolve("com/example/test-lib/1.0.0/test-lib-1.0.0.jar")
        assertThat(expectedPath).exists()
    }
    
    @Test
    fun `should generate checksums`() = runBlocking {
        // ... test checksum files exist
    }
    
    @Test
    fun `should update maven-metadata`() = runBlocking {
        // ... test metadata XML updated
    }
}
```

## Acceptance Criteria
- [ ] Unit tests for all modules
- [ ] 80%+ coverage for core logic
- [ ] Integration tests for each publisher type
- [ ] Konsist tests enforce architecture
- [ ] Test fixtures for common scenarios
- [ ] CI runs all tests on PR/push
- [ ] Tests run in < 5 minutes

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Slow integration tests | Use local repos, mock HTTP where possible |
| Flaky network tests | Mock external services, use testcontainers |
| Test key security | Use dedicated test keys, never real credentials |

## Estimated Effort
**Size: L** (Large) - 4-5 days
