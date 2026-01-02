# Tasks: Comprehensive Testing Infrastructure

## 1. Test Directory Structure
- [x] 1.1 Create test directories under test/guru/clanker/amper/publish/
- [x] 1.2 Organize by package: domain/, architecture/
- [x] 1.3 Use Amper's test/ directory convention

## 2. Domain Unit Tests
- [x] 2.1 CoordinatesTest - validation, isSnapshot, toPath, toBaseFilename
- [x] 2.2 RepositoryTest - all repository types (Maven, GitHub, Local)
- [x] 2.3 PublicationTest - validation, artifact handling, POM metadata
- [x] 2.4 PublishingResultTest - success/failure/checksum cases

## 3. Konsist Architecture Tests
- [x] 3.1 Domain layer isolation test (no infrastructure/maven/settings/tasks imports)
- [x] 3.2 No Maven imports in domain test
- [x] 3.3 No Ktor imports in domain test
- [x] 3.4 No Bouncy Castle in domain test
- [x] 3.5 Test class naming test (must end with "Test")
- [x] 3.6 Interface naming test (no "I" prefix)
- [x] 3.7 Layer dependency test (domain → settings/maven → infrastructure → tasks)

## 4. Test Configuration
- [x] 4.1 Add kotlin-test dependency in module.yaml
- [x] 4.2 Add konsist dependency for architecture tests
- [x] 4.3 Configure JUnit platform runner

## 5. Verification
- [x] 5.1 All 50 tests pass
- [x] 5.2 Tests run in < 1 second
- [x] 5.3 Architecture tests enforce layer boundaries
