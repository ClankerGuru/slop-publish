# Remote Maven Repository Publisher Specification

## ADDED Requirements

### Requirement: Remote Repository Publishing
The system SHALL publish artifacts to remote Maven repositories via HTTP(S).

#### Scenario: Publish to Maven repository
- **WHEN** publishing to a Repository.Maven with HTTP URL
- **THEN** artifacts are uploaded via HTTP PUT requests

#### Scenario: Include POM in deployment
- **WHEN** publishing a publication
- **THEN** generated POM is uploaded alongside artifacts

#### Scenario: Successful publish result
- **WHEN** all artifacts upload successfully
- **THEN** PublishingResult.Success with remote URLs is returned

### Requirement: Authentication Support
The system SHALL support authentication for protected repositories.

#### Scenario: Username/password authentication
- **WHEN** repository has credentials configured
- **THEN** HTTP Basic authentication header is included

#### Scenario: Missing credentials for protected repo
- **WHEN** publishing to protected repo without credentials
- **THEN** PublishingError.AuthenticationFailed is returned

### Requirement: Retry with Exponential Backoff
The system SHALL retry failed requests with exponential backoff.

#### Scenario: Retry on network error
- **WHEN** a SocketTimeoutException occurs
- **THEN** request is retried after delay

#### Scenario: Exponential delay increase
- **WHEN** consecutive retries occur
- **THEN** delay doubles between each retry (up to max)

#### Scenario: Max attempts reached
- **WHEN** all retry attempts fail
- **THEN** final error is returned

#### Scenario: Non-retryable error
- **WHEN** HTTP 401 (unauthorized) is returned
- **THEN** no retry is attempted, error returned immediately

### Requirement: Rate Limiting Handling
The system SHALL respect rate limiting responses.

#### Scenario: HTTP 429 response
- **WHEN** server returns HTTP 429 (Too Many Requests)
- **THEN** request is retried after appropriate delay

#### Scenario: Retry-After header
- **WHEN** 429 response includes Retry-After header
- **THEN** delay respects the specified wait time

### Requirement: Maven Central Validation
The system SHALL validate Maven Central requirements before upload.

#### Scenario: Detect Maven Central
- **WHEN** repository URL contains "sonatype" or "maven.apache.org"
- **THEN** repository is treated as Maven Central

#### Scenario: Require full POM
- **WHEN** publishing to Maven Central
- **THEN** PomValidator.validateForMavenCentral is called

#### Scenario: Validation failure blocks upload
- **WHEN** publication fails Maven Central validation
- **THEN** upload is not attempted and validation errors returned

### Requirement: Progress Reporting
The system SHALL report progress during large uploads.

#### Scenario: Artifact started
- **WHEN** artifact upload begins
- **THEN** onArtifactStarted callback is invoked with file size

#### Scenario: Progress updates
- **WHEN** upload progresses
- **THEN** onProgress callback is invoked periodically

#### Scenario: Artifact completed
- **WHEN** artifact upload completes
- **THEN** onArtifactCompleted callback is invoked

#### Scenario: Artifact failed
- **WHEN** artifact upload fails
- **THEN** onArtifactFailed callback is invoked with error

### Requirement: Credential Masking
The system SHALL mask sensitive credentials in logs and error messages.

#### Scenario: Password masked in logs
- **WHEN** error message contains password
- **THEN** password is replaced with "****"

#### Scenario: Token masked in errors
- **WHEN** error includes authentication token
- **THEN** token is replaced with "[MASKED]"

### Requirement: Timeout Configuration
The system SHALL support configurable timeouts.

#### Scenario: Connection timeout
- **WHEN** connection takes too long
- **THEN** connection timeout triggers retry or failure

#### Scenario: Large file timeout
- **WHEN** uploading large artifact (>100MB)
- **THEN** read timeout is extended appropriately
