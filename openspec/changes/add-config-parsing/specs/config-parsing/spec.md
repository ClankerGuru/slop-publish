# Configuration Parsing Specification

## ADDED Requirements

### Requirement: YAML Configuration Parsing
The system SHALL parse the `publishing` block from Amper `module.yaml` files.

#### Scenario: Parse minimal configuration
- **WHEN** parsing YAML with repositories and publications
- **THEN** a `PublishingConfiguration` object is returned with parsed values

#### Scenario: Parse complete configuration
- **WHEN** parsing YAML with repositories, publications, signing, and targets
- **THEN** all configuration sections are correctly parsed

#### Scenario: Invalid YAML syntax
- **WHEN** parsing malformed YAML
- **THEN** a `ConfigurationException` is thrown with line number and description

### Requirement: Environment Variable Substitution
The system SHALL substitute environment variables in format `${VAR_NAME}` with their runtime values.

#### Scenario: Successful substitution
- **WHEN** parsing `${MAVEN_USERNAME}` with env containing MAVEN_USERNAME=admin
- **THEN** the value is replaced with "admin"

#### Scenario: Multiple variables in value
- **WHEN** parsing `https://${USER}:${PASS}@repo.com`
- **THEN** both variables are substituted

#### Scenario: Unresolved variable error
- **WHEN** parsing `${UNDEFINED_VAR}` with no matching env variable
- **THEN** an `UnresolvedVariablesException` is thrown listing the missing variable

#### Scenario: Unresolved variable warning mode
- **WHEN** parsing with warning mode enabled and unresolved variables exist
- **THEN** warnings are returned but parsing continues with literal value

### Requirement: Repository Configuration
The system SHALL parse repository configurations with type, URL, and optional credentials.

#### Scenario: Maven repository with credentials
- **WHEN** parsing a Maven repository with username and password
- **THEN** `RepositoryConfig` contains type=MAVEN, url, and credentials

#### Scenario: GitHub Packages repository
- **WHEN** parsing a GitHub repository with token
- **THEN** `RepositoryConfig` contains type=GITHUB with token credential

#### Scenario: Local repository
- **WHEN** parsing a local repository with file:// URL
- **THEN** `RepositoryConfig` contains type=LOCAL

#### Scenario: Multiple repositories
- **WHEN** parsing configuration with multiple repositories
- **THEN** all repositories are parsed in order

### Requirement: Publication Configuration
The system SHALL parse publication configurations with Maven coordinates and artifact types.

#### Scenario: Publication with coordinates
- **WHEN** parsing a publication with groupId, artifactId, version
- **THEN** `PublicationConfig` contains all coordinate fields

#### Scenario: Publication with artifact types
- **WHEN** parsing a publication with artifacts: [jar, sources, javadoc]
- **THEN** the artifact types are correctly parsed

#### Scenario: Publication with POM metadata
- **WHEN** parsing a publication with pom section
- **THEN** `PomConfig` contains name, description, url, licenses, developers, scm

### Requirement: Signing Configuration
The system SHALL parse optional signing configuration for artifact signing.

#### Scenario: Signing enabled
- **WHEN** parsing signing with enabled=true
- **THEN** `SigningConfig` is present with enabled=true

#### Scenario: Signing with key configuration
- **WHEN** parsing signing with keyId, password, secretKeyRing
- **THEN** all key configuration fields are parsed

#### Scenario: Signing skip list
- **WHEN** parsing signing with skipForRepositories: [github]
- **THEN** the skip list contains "github"

### Requirement: Target Configuration
The system SHALL parse publishing target mappings from target names to repository IDs.

#### Scenario: Default target
- **WHEN** parsing targets with default: [github]
- **THEN** the "default" target maps to repository ID "github"

#### Scenario: Multiple targets
- **WHEN** parsing targets with default, release, and all
- **THEN** each target maps to its specified repository IDs

### Requirement: Configuration Validation
The system SHALL validate parsed configuration and return actionable error messages.

#### Scenario: Missing required field
- **WHEN** a repository is missing required 'id' field
- **THEN** validation returns error with field path and expected value

#### Scenario: Invalid URL format
- **WHEN** a repository URL is not a valid URL
- **THEN** validation returns error with the invalid value

#### Scenario: Duplicate repository ID
- **WHEN** two repositories have the same ID
- **THEN** validation returns error identifying the duplicate

#### Scenario: Invalid target reference
- **WHEN** a target references non-existent repository ID
- **THEN** validation returns error with valid options listed

#### Scenario: Maven Central POM validation
- **WHEN** publishing to Maven Central without required POM fields
- **THEN** validation returns warnings for missing: name, description, url, licenses, developers, scm

### Requirement: Domain Model Mapping
The system SHALL map configuration objects to domain model objects.

#### Scenario: Map to Repository
- **WHEN** mapping RepositoryConfig with type=MAVEN
- **THEN** Repository.Maven domain object is created

#### Scenario: Map to Publication
- **WHEN** mapping PublicationConfig
- **THEN** Publication domain object is created with Coordinates and artifacts

#### Scenario: Map to PomMetadata
- **WHEN** mapping PomConfig
- **THEN** PomMetadata domain object is created with all fields

### Requirement: Configuration Isolation
The configuration module SHALL not expose YAML parsing library types to consumers.

#### Scenario: Clean API boundary
- **WHEN** using AmperConfigParser public API
- **THEN** only domain types and configuration data classes are exposed
- **AND** no YAML library types (kaml, snakeyaml) appear in public signatures
