# Tasks: Project Setup & Architecture Foundation

## 1. Build Configuration (Amper)
- [x] 1.1 Create `project.yaml` with module listing
- [x] 1.2 Create `module.yaml` with JVM 17 target and Kotlin 2.0
- [x] 1.3 Add Maven dependencies (maven-core, maven-resolver-*)
- [x] 1.4 Add Ktor Client dependencies (core, cio, content-negotiation, serialization)
- [x] 1.5 Add kotlinx.serialization dependencies
- [x] 1.6 Add kaml dependency for YAML parsing
- [x] 1.7 Add Bouncy Castle dependencies (bcpg-jdk18on, bcprov-jdk18on)
- [x] 1.8 Add Konsist test dependency
- [x] 1.9 Configure kotlin-test for unit testing
- [x] 1.10 Configure as `product: jvm/amper-plugin`
- [x] 1.11 Add `pluginInfo.settingsClass` configuration

## 2. Package Structure
- [x] 2.1 Create `src/guru/clanker/amper/publish/domain/model/` with domain classes
- [x] 2.2 Create `src/guru/clanker/amper/publish/domain/service/` with PublishingService
- [x] 2.3 Create `src/guru/clanker/amper/publish/settings/` with @Configurable interfaces
- [x] 2.4 Create `src/guru/clanker/amper/publish/tasks/` with @TaskAction functions
- [x] 2.5 Create `src/guru/clanker/amper/publish/maven/resolver/` with Maven integration
- [x] 2.6 Create `src/guru/clanker/amper/publish/maven/pom/` with POM generation
- [x] 2.7 Create `src/guru/clanker/amper/publish/maven/signing/` with PGP signing
- [x] 2.8 Create `src/guru/clanker/amper/publish/infrastructure/` with publishers

## 3. Konsist Architecture Tests
- [x] 3.1 Create `test/guru/clanker/amper/publish/architecture/` directory
- [x] 3.2 Write test: domain layer has no infrastructure dependencies
- [x] 3.3 Write test: domain layer has no Maven class imports
- [x] 3.4 Write test: domain layer has no Ktor class imports
- [x] 3.5 Write test: domain layer has no Bouncy Castle imports
- [x] 3.6 Write test: test classes end with "Test" suffix
- [x] 3.7 Write test: interfaces do not have I prefix
- [x] 3.8 Write test: layer dependencies are respected

## 4. Plugin Configuration
- [x] 4.1 Create `plugin.yaml` with task registrations
- [x] 4.2 Register publish, publishLocal, validate, publishDryRun tasks
- [x] 4.3 Wire settings via ${pluginSettings}

## 5. Documentation
- [x] 5.1 Create README.md with project overview
- [x] 5.2 Add development setup instructions
- [x] 5.3 Add build commands documentation
- [x] 5.4 Add architecture overview
- [x] 5.5 Add plugin usage examples

## 6. Verification
- [x] 6.1 Run `./amper build` successfully
- [x] 6.2 Run `./amper test` with all 50 tests passing
