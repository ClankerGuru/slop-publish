# Domain Model Specification

## ADDED Requirements

### Requirement: Maven Coordinates
The system SHALL represent Maven coordinates (GAV) as an immutable data class with validation.

#### Scenario: Valid coordinates creation
- **WHEN** creating Coordinates with valid groupId, artifactId, and version
- **THEN** the Coordinates object is created successfully

#### Scenario: Invalid groupId rejected
- **WHEN** creating Coordinates with blank or invalid groupId
- **THEN** an IllegalArgumentException is thrown with descriptive message

#### Scenario: Snapshot detection
- **WHEN** checking a version ending with "-SNAPSHOT"
- **THEN** `isSnapshot` returns true

#### Scenario: Path generation
- **WHEN** calling `toPath()` on Coordinates("com.example", "lib", "1.0.0")
- **THEN** returns "com/example/lib/1.0.0"

### Requirement: Artifact Representation
The system SHALL represent publishable artifacts with file path, classifier, and extension.

#### Scenario: JAR artifact creation
- **WHEN** creating an Artifact with a file path
- **THEN** the default extension is "jar" and classifier is null

#### Scenario: Sources artifact creation
- **WHEN** creating an Artifact with classifier "sources"
- **THEN** the artifact represents a sources JAR

#### Scenario: Blank extension rejected
- **WHEN** creating an Artifact with blank extension
- **THEN** an IllegalArgumentException is thrown

### Requirement: Repository Types
The system SHALL support multiple repository types via sealed class hierarchy.

#### Scenario: Maven repository with credentials
- **WHEN** creating a Repository.Maven with URL and credentials
- **THEN** the repository is configured for authenticated access

#### Scenario: GitHub Packages repository
- **WHEN** creating a Repository.GitHubPackages with token, owner, and repository name
- **THEN** the repository is configured for GitHub Packages access

#### Scenario: Local repository
- **WHEN** creating a Repository.Local with file:// URL
- **THEN** the repository points to local filesystem

### Requirement: POM Metadata
The system SHALL represent POM metadata required for Maven Central compliance.

#### Scenario: Complete metadata
- **WHEN** creating PomMetadata with name, description, url, licenses, developers, and scm
- **THEN** all fields are accessible for POM generation

#### Scenario: License information
- **WHEN** creating a License with name and URL
- **THEN** the license is available for POM generation

#### Scenario: Developer information
- **WHEN** creating a Developer with id, name, and email
- **THEN** the developer info is available for POM generation

### Requirement: Publication
The system SHALL combine coordinates, artifacts, and metadata into a Publication.

#### Scenario: Valid publication
- **WHEN** creating a Publication with coordinates and at least one artifact
- **THEN** the Publication is created successfully

#### Scenario: Empty artifacts rejected
- **WHEN** creating a Publication with empty artifact list
- **THEN** an IllegalArgumentException is thrown

### Requirement: Publishing Results
The system SHALL represent publishing outcomes as Success or Failure sealed types.

#### Scenario: Successful publish
- **WHEN** a publish operation completes successfully
- **THEN** PublishingResult.Success contains the publication, repository, and published artifacts

#### Scenario: Failed publish
- **WHEN** a publish operation fails
- **THEN** PublishingResult.Failure contains the publication, repository, and error details

### Requirement: Publishing Errors
The system SHALL categorize publishing errors into specific types for actionable error handling.

#### Scenario: Authentication failure
- **WHEN** authentication fails
- **THEN** PublishingError.AuthenticationFailed provides error message

#### Scenario: Network error
- **WHEN** network communication fails
- **THEN** PublishingError.NetworkError provides message and optional cause

#### Scenario: Artifact not found
- **WHEN** an artifact file doesn't exist
- **THEN** PublishingError.ArtifactNotFound provides the missing path

### Requirement: Domain Layer Purity
The domain model SHALL have no dependencies on external libraries (Maven, Ktor, Bouncy Castle).

#### Scenario: No Maven imports
- **WHEN** analyzing domain package imports
- **THEN** no org.apache.maven imports are present

#### Scenario: No Ktor imports
- **WHEN** analyzing domain package imports
- **THEN** no io.ktor imports are present

#### Scenario: No Bouncy Castle imports
- **WHEN** analyzing domain package imports
- **THEN** no org.bouncycastle imports are present

### Requirement: Publishing Service Interface
The system SHALL define a PublishingService interface for domain service abstraction.

#### Scenario: Publish method signature
- **WHEN** implementing PublishingService
- **THEN** the `publish` method accepts Publication and Repository, returns PublishingResult

#### Scenario: Validate method signature
- **WHEN** implementing PublishingService
- **THEN** the `validate` method accepts Publication and Repository, returns List<ValidationError>
