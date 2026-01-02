# Maven Resolver Specification

## ADDED Requirements

### Requirement: Repository System Creation
The system SHALL create and configure Maven RepositorySystem for artifact deployment.

#### Scenario: Create repository system
- **WHEN** calling `ResolverFactory.createRepositorySystem()`
- **THEN** a configured RepositorySystem is returned with HTTP and file transports

#### Scenario: HTTP transport available
- **WHEN** deploying to an HTTP(S) URL
- **THEN** the HttpTransporterFactory handles the connection

#### Scenario: File transport available
- **WHEN** deploying to a file:// URL
- **THEN** the FileTransporterFactory handles the file operations

### Requirement: Session Management
The system SHALL create RepositorySystemSession with appropriate configuration.

#### Scenario: Create session with local repo
- **WHEN** calling `createSession()` with custom local repo path
- **THEN** the session uses the specified local repository

#### Scenario: Default local repository
- **WHEN** calling `createSession()` without specifying local repo
- **THEN** the session uses `~/.m2/repository` as default

#### Scenario: Checksum policy
- **WHEN** creating a session
- **THEN** checksum verification is enabled (fail on mismatch)

### Requirement: Remote Repository Creation
The system SHALL convert domain Repository types to Maven RemoteRepository.

#### Scenario: Maven repository with credentials
- **WHEN** converting Repository.Maven with credentials
- **THEN** RemoteRepository includes username/password authentication

#### Scenario: GitHub Packages repository
- **WHEN** converting Repository.GitHubPackages
- **THEN** RemoteRepository uses owner as username and token as password

#### Scenario: Local repository
- **WHEN** converting Repository.Local
- **THEN** RemoteRepository has file:// URL and no authentication

### Requirement: Artifact Deployment
The system SHALL deploy artifacts to Maven repositories using Maven Resolver.

#### Scenario: Deploy single artifact
- **WHEN** deploying a Publication with one JAR artifact
- **THEN** the artifact is uploaded to the repository at correct coordinates path

#### Scenario: Deploy multiple artifacts
- **WHEN** deploying a Publication with JAR, sources, and javadoc
- **THEN** all artifacts are uploaded in a single deployment request

#### Scenario: Deploy with signatures
- **WHEN** deploying with additional signature files (.asc)
- **THEN** signature files are uploaded alongside artifacts

#### Scenario: Successful deployment result
- **WHEN** deployment completes successfully
- **THEN** DeploymentResult.Success contains list of published artifacts with remote URLs

### Requirement: Checksum Generation
The system SHALL generate MD5, SHA-1, and SHA-256 checksums for artifacts.

#### Scenario: Generate MD5
- **WHEN** generating checksum for a file
- **THEN** correct MD5 hex string is produced

#### Scenario: Generate SHA-1
- **WHEN** generating checksum for a file
- **THEN** correct SHA-1 hex string is produced

#### Scenario: Generate SHA-256
- **WHEN** generating checksum for a file
- **THEN** correct SHA-256 hex string is produced

#### Scenario: Large file handling
- **WHEN** generating checksums for files larger than 100MB
- **THEN** memory usage remains bounded (streaming approach)

### Requirement: Authentication Handling
The system SHALL support multiple authentication mechanisms.

#### Scenario: Username/password auth
- **WHEN** repository has username and password credentials
- **THEN** HTTP Basic authentication is used

#### Scenario: Token auth
- **WHEN** repository uses token-based authentication
- **THEN** token is passed as password with appropriate username

#### Scenario: No auth for local
- **WHEN** deploying to local repository
- **THEN** no authentication is configured

### Requirement: Error Handling
The system SHALL map Maven Resolver errors to domain error types.

#### Scenario: Authentication failure (401)
- **WHEN** deployment fails with HTTP 401
- **THEN** PublishingError.AuthenticationFailed is returned

#### Scenario: Access denied (403)
- **WHEN** deployment fails with HTTP 403
- **THEN** PublishingError.AuthenticationFailed is returned with "Access denied" message

#### Scenario: Repository not found (404)
- **WHEN** deployment fails with HTTP 404
- **THEN** PublishingError.RepositoryError is returned with status code 404

#### Scenario: Network connection error
- **WHEN** deployment fails due to network connectivity
- **THEN** PublishingError.NetworkError is returned with original cause

#### Scenario: Unknown error
- **WHEN** deployment fails for unknown reason
- **THEN** PublishingError.RepositoryError is returned with descriptive message

### Requirement: Maven API Isolation
The maven.resolver package SHALL not expose Maven Resolver types in public API.

#### Scenario: Public API uses domain types
- **WHEN** calling MavenDeployer.deploy()
- **THEN** parameters and return types are domain classes (Publication, Repository, DeploymentResult)

#### Scenario: No Maven imports in domain
- **WHEN** analyzing domain package
- **THEN** no org.apache.maven or org.eclipse.aether imports are present
