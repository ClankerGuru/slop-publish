# Plugin Entry Point Specification

## ADDED Requirements

### Requirement: CLI Configuration Loading
The system SHALL parse and validate configuration on CLI startup.

#### Scenario: Load valid configuration
- **WHEN** running `./slop-publish` with valid module.yaml
- **THEN** configuration is parsed and CLI initializes successfully

#### Scenario: Configuration validation
- **WHEN** CLI loads configuration
- **THEN** validation runs and errors prevent execution

#### Scenario: Environment variable resolution
- **WHEN** configuration contains ${VAR_NAME} placeholders
- **THEN** values are resolved from environment

#### Scenario: Missing configuration
- **WHEN** module.yaml has no publishing block
- **THEN** CLI reports error with guidance

### Requirement: CLI Commands
The system SHALL support CLI commands for publishing.

#### Scenario: Default publish command
- **WHEN** running `./slop-publish`
- **THEN** publishes to default target

#### Scenario: Local publish command
- **WHEN** running `./slop-publish --local`
- **THEN** publishes to ~/.m2/repository

#### Scenario: Target-specific command
- **WHEN** running `./slop-publish --target=release`
- **THEN** publishes to release target repositories

### Requirement: Publishing Pipeline Orchestration
The system SHALL orchestrate the full publishing pipeline.

#### Scenario: Publish flow
- **WHEN** publish command runs
- **THEN** pipeline executes: collect artifacts → generate POM → sign (if needed) → upload

#### Scenario: Multiple publications
- **WHEN** multiple publications are configured
- **THEN** each publication is processed

#### Scenario: Multiple repositories
- **WHEN** target includes multiple repositories
- **THEN** publication is sent to each repository

### Requirement: Artifact Collection from Amper Build
The system SHALL collect artifacts from Amper build output.

#### Scenario: JAR artifact location
- **WHEN** publication includes JAR type
- **THEN** JAR is located at build/tasks/_<module>_jarJvm/<module>-jvm.jar

#### Scenario: Sources JAR
- **WHEN** publication includes sources type
- **THEN** sources JAR is located in build output if present

#### Scenario: Javadoc JAR
- **WHEN** publication includes javadoc type
- **THEN** javadoc JAR is located in build output if present

#### Scenario: Missing artifact
- **WHEN** configured artifact doesn't exist
- **THEN** clear error with expected path is reported
- **AND** error suggests running `./amper build` first

### Requirement: Target Resolution
The system SHALL resolve publishing targets to repositories.

#### Scenario: Default target
- **WHEN** running with default target configured
- **THEN** artifacts go to default target repositories

#### Scenario: Named target
- **WHEN** running `--target=release`
- **THEN** artifacts go to release target repositories

#### Scenario: Unknown target
- **WHEN** unknown target is specified
- **THEN** error lists available targets

### Requirement: Dry Run Mode
The system SHALL support dry-run mode for testing.

#### Scenario: Dry run enabled
- **WHEN** --dry-run flag is set
- **THEN** actions are logged but not executed

#### Scenario: Dry run output
- **WHEN** in dry-run mode
- **THEN** output shows [DRY RUN] prefix with what would be published

#### Scenario: Dry run returns success
- **WHEN** dry-run completes
- **THEN** exit code is 0

### Requirement: Error Handling
The system SHALL provide clear, actionable error messages.

#### Scenario: Configuration error
- **WHEN** configuration is invalid
- **THEN** error lists all validation issues
- **AND** exit code is 1

#### Scenario: Publishing failure
- **WHEN** publishing fails
- **THEN** error includes repository, artifact, and cause
- **AND** exit code is 1

#### Scenario: Partial failure
- **WHEN** some repositories succeed and others fail
- **THEN** successes are logged and failures reported

### Requirement: Output and Logging
The system SHALL provide clear output of publishing progress and results.

#### Scenario: Progress output
- **WHEN** publishing is in progress
- **THEN** current operation is printed to stdout

#### Scenario: Success output
- **WHEN** publishing succeeds
- **THEN** published artifact URLs are printed

#### Scenario: Error output
- **WHEN** errors occur
- **THEN** errors are printed to stderr

#### Scenario: Credential masking
- **WHEN** errors contain credentials
- **THEN** credentials are masked in output
