# Tasks: Domain Model Implementation

## 1. Core Data Classes
- [x] 1.1 Create `Coordinates` data class with GAV validation
- [x] 1.2 Create `Artifact` data class with classifier and extension
- [x] 1.3 Create `ArtifactType` enum (JAR, SOURCES, JAVADOC, POM)
- [x] 1.4 Create `Repository` sealed class hierarchy (Maven, GitHubPackages, Local)
- [x] 1.5 Create `Credentials` data class with validation

## 2. POM Metadata Classes
- [x] 2.1 Create `PomMetadata` data class with Maven Central requirements
- [x] 2.2 Create `License` data class
- [x] 2.3 Create `Developer` data class
- [x] 2.4 Create `Scm` data class

## 3. Publication and Results
- [x] 3.1 Create `Publication` data class combining coordinates and artifacts
- [x] 3.2 Create `PublishingResult` sealed class (Success, Failure)
- [x] 3.3 Create `PublishedArtifact` data class with checksum
- [x] 3.4 Create `Checksum` data class with MD5, SHA1, SHA256
- [x] 3.5 Create `PublishingError` sealed class hierarchy

## 4. Domain Service
- [x] 4.1 Create `PublishingService` interface
- [x] 4.2 Create `ValidationError` data class with severity

## 5. Unit Tests
- [x] 5.1 Write CoordinatesTest (13 tests)
- [x] 5.2 Write RepositoryTest (9 tests)
- [x] 5.3 Write PublicationTest (12 tests)
- [x] 5.4 Write PublishingResultTest (9 tests)

## 6. Verification
- [x] 6.1 Run `./amper build` successfully
- [x] 6.2 Run `./amper test` - all 50 tests passing
- [x] 6.3 Konsist architecture tests verify domain has no external dependencies
