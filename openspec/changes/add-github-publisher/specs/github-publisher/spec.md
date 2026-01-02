# GitHub Packages Publisher Specification

## ADDED Requirements

### Requirement: GitHub Packages Publishing
The system SHALL publish artifacts to GitHub Packages Maven registry.

#### Scenario: Publish to GitHub Packages
- **WHEN** publishing to Repository.GitHubPackages
- **THEN** artifacts are uploaded via HTTPS to maven.pkg.github.com

#### Scenario: Artifact URL structure
- **WHEN** uploading artifact with groupId "com.example", artifactId "lib", version "1.0"
- **THEN** URL follows pattern `https://maven.pkg.github.com/owner/repo/com/example/lib/1.0/lib-1.0.jar`

#### Scenario: Minimal POM
- **WHEN** publishing to GitHub Packages
- **THEN** minimal POM is generated (full metadata not required)

### Requirement: Authentication
The system SHALL support GitHub authentication methods.

#### Scenario: Personal Access Token
- **WHEN** token is a PAT with write:packages scope
- **THEN** Bearer token authentication succeeds

#### Scenario: GITHUB_TOKEN
- **WHEN** running in GitHub Actions with GITHUB_TOKEN
- **THEN** authentication succeeds for repository packages

#### Scenario: Invalid token
- **WHEN** token is invalid or expired
- **THEN** PublishingError.AuthenticationFailed is returned

#### Scenario: Insufficient permissions
- **WHEN** token lacks write:packages scope
- **THEN** PublishingError.AuthenticationFailed with 403 is returned

### Requirement: Rate Limiting
The system SHALL handle GitHub API rate limiting.

#### Scenario: Proactive rate limiting
- **WHEN** making requests
- **THEN** rate limiter throttles to avoid hitting limits

#### Scenario: HTTP 429 response
- **WHEN** server returns 429 Too Many Requests
- **THEN** request is retried after delay

#### Scenario: Retry-After header
- **WHEN** 429 response includes Retry-After header
- **THEN** delay respects specified seconds

#### Scenario: Rate limit exhausted
- **WHEN** rate limit cannot be satisfied after retry
- **THEN** appropriate error is returned

### Requirement: URL Parsing
The system SHALL parse GitHub owner and repository from URL.

#### Scenario: Parse valid URL
- **WHEN** URL is `https://maven.pkg.github.com/owner/repo`
- **THEN** owner is "owner" and repository is "repo"

#### Scenario: Invalid URL format
- **WHEN** URL doesn't match GitHub Packages pattern
- **THEN** validation error is returned

### Requirement: No Signing Required
The system SHALL skip PGP signing for GitHub Packages.

#### Scenario: Publishing without signatures
- **WHEN** publishing to GitHub Packages
- **THEN** no .asc signature files are generated

#### Scenario: Signing configuration
- **WHEN** signing is configured with skipRepositories containing GitHub repo ID
- **THEN** signing is skipped for that repository

### Requirement: Error Handling
The system SHALL provide clear error messages for GitHub-specific issues.

#### Scenario: Repository not found
- **WHEN** package repository doesn't exist
- **THEN** error includes guidance on package settings

#### Scenario: Package visibility mismatch
- **WHEN** trying to publish public package to private repo
- **THEN** appropriate error is returned

### Requirement: Ktor Client Usage
The system SHALL use Ktor Client for HTTP operations (per dependency policy).

#### Scenario: HTTP client configuration
- **WHEN** GitHubPackagesPublisher is created
- **THEN** Ktor CIO client is configured with appropriate timeouts

#### Scenario: Large file timeout
- **WHEN** uploading large artifact
- **THEN** request timeout accommodates file size (5 minutes default)
