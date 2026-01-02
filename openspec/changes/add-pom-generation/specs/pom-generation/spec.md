# POM Generation Specification

## ADDED Requirements

### Requirement: Minimal POM Generation
The system SHALL generate minimal valid POM files containing only Maven coordinates.

#### Scenario: Generate minimal POM
- **WHEN** calling `generateMinimal()` with a Publication
- **THEN** POM XML is returned with modelVersion, groupId, artifactId, version, and packaging

#### Scenario: Minimal POM structure
- **WHEN** examining a minimal POM
- **THEN** it contains proper XML declaration and maven-4.0.0 namespace

#### Scenario: Generate minimal POM to file
- **WHEN** calling `generateToFile()` with full=false
- **THEN** minimal POM is written to the specified path

### Requirement: Full POM Generation
The system SHALL generate complete POM files with all Maven Central required metadata.

#### Scenario: Generate full POM
- **WHEN** calling `generateFull()` with a Publication containing PomMetadata
- **THEN** POM XML includes name, description, url, licenses, developers, and scm

#### Scenario: Multiple licenses
- **WHEN** generating POM with multiple licenses
- **THEN** all licenses appear in the licenses section

#### Scenario: Multiple developers
- **WHEN** generating POM with multiple developers
- **THEN** all developers appear in the developers section

#### Scenario: SCM information
- **WHEN** generating POM with SCM metadata
- **THEN** scm section includes url, connection, and optional developerConnection

#### Scenario: Missing PomMetadata error
- **WHEN** calling `generateFull()` without PomMetadata
- **THEN** IllegalArgumentException is thrown with descriptive message

### Requirement: XML Formatting
The system SHALL generate properly formatted and escaped XML.

#### Scenario: XML declaration
- **WHEN** generating any POM
- **THEN** output begins with `<?xml version="1.0" encoding="UTF-8"?>`

#### Scenario: Proper indentation
- **WHEN** generating POM
- **THEN** nested elements are indented for readability

#### Scenario: Special character escaping
- **WHEN** description contains `<`, `>`, `&`, `"`, or `'`
- **THEN** these characters are properly XML escaped

#### Scenario: Unicode support
- **WHEN** metadata contains Unicode characters
- **THEN** characters are preserved correctly in UTF-8 output

### Requirement: Maven Central Validation
The system SHALL validate publications against Maven Central requirements.

#### Scenario: Valid publication
- **WHEN** validating a publication with all required metadata
- **THEN** validation returns empty error list

#### Scenario: Missing name
- **WHEN** validating publication with blank name
- **THEN** validation returns error for "name" field

#### Scenario: Missing description
- **WHEN** validating publication with blank description
- **THEN** validation returns error for "description" field

#### Scenario: Missing URL
- **WHEN** validating publication with blank url
- **THEN** validation returns error for "url" field

#### Scenario: No licenses
- **WHEN** validating publication with empty licenses list
- **THEN** validation returns error indicating at least one license required

#### Scenario: No developers
- **WHEN** validating publication with empty developers list
- **THEN** validation returns error indicating at least one developer required

#### Scenario: Missing SCM url
- **WHEN** validating publication with blank scm.url
- **THEN** validation returns error for "scm.url" field

#### Scenario: Missing SCM connection
- **WHEN** validating publication with blank scm.connection
- **THEN** validation returns error for "scm.connection" field

#### Scenario: Multiple validation errors
- **WHEN** validating publication with multiple missing fields
- **THEN** all errors are collected and returned (not fail-fast)

### Requirement: POM DSL Builder
The system SHALL provide a Kotlin DSL for building POM XML.

#### Scenario: Fluent builder API
- **WHEN** using buildPom { } DSL
- **THEN** POM elements can be added with fluent method calls

#### Scenario: Nested elements
- **WHEN** adding licenses or developers
- **THEN** nested builder scopes are available for child elements

### Requirement: File Output
The system SHALL write generated POMs to files.

#### Scenario: Write POM file
- **WHEN** calling generateToFile() with output path
- **THEN** POM file is created at specified path with UTF-8 encoding

#### Scenario: Overwrite existing
- **WHEN** output path already exists
- **THEN** existing file is overwritten
