# Local Repository Publisher Specification

## ADDED Requirements

### Requirement: Local Repository Publishing
The system SHALL publish artifacts to local Maven repository following Maven layout conventions.

#### Scenario: Publish to default location
- **WHEN** publishing without custom path
- **THEN** artifacts are written to `~/.m2/repository`

#### Scenario: Publish to custom location
- **WHEN** publishing with custom localRepoPath
- **THEN** artifacts are written to specified directory

#### Scenario: Maven layout structure
- **WHEN** publishing artifact with groupId "com.example" artifactId "lib" version "1.0"
- **THEN** artifacts are placed in `com/example/lib/1.0/`

### Requirement: Artifact File Naming
The system SHALL name artifact files following Maven naming conventions.

#### Scenario: Main JAR naming
- **WHEN** publishing JAR artifact without classifier
- **THEN** file is named `{artifactId}-{version}.jar`

#### Scenario: Sources JAR naming
- **WHEN** publishing artifact with classifier "sources"
- **THEN** file is named `{artifactId}-{version}-sources.jar`

#### Scenario: Javadoc JAR naming
- **WHEN** publishing artifact with classifier "javadoc"
- **THEN** file is named `{artifactId}-{version}-javadoc.jar`

#### Scenario: POM file naming
- **WHEN** publishing POM
- **THEN** file is named `{artifactId}-{version}.pom`

### Requirement: Checksum Generation
The system SHALL generate checksum files for all published artifacts.

#### Scenario: MD5 checksum
- **WHEN** artifact is published
- **THEN** corresponding `.md5` file is created with MD5 hash

#### Scenario: SHA-1 checksum
- **WHEN** artifact is published
- **THEN** corresponding `.sha1` file is created with SHA-1 hash

### Requirement: Maven Metadata Management
The system SHALL maintain maven-metadata.xml for version listing.

#### Scenario: Create new metadata
- **WHEN** publishing to new artifact coordinates
- **THEN** maven-metadata-local.xml is created with version listing

#### Scenario: Update existing metadata
- **WHEN** publishing new version to existing artifact
- **THEN** version is added to existing metadata versions list

#### Scenario: Latest version tracking
- **WHEN** metadata is updated
- **THEN** `latest` element reflects most recent version

#### Scenario: Release version tracking
- **WHEN** publishing non-snapshot version
- **THEN** `release` element reflects most recent release version

#### Scenario: Version ordering
- **WHEN** listing versions in metadata
- **THEN** versions are sorted according to Maven version ordering

#### Scenario: Timestamp update
- **WHEN** metadata is modified
- **THEN** `lastUpdated` element contains current timestamp

### Requirement: Directory Creation
The system SHALL create necessary directory structure.

#### Scenario: Create missing directories
- **WHEN** target directory does not exist
- **THEN** full directory path is created

#### Scenario: Existing directory preserved
- **WHEN** target directory already exists
- **THEN** existing content is preserved, new files added

### Requirement: Error Handling
The system SHALL handle file operation errors gracefully.

#### Scenario: Permission denied
- **WHEN** write permission is denied
- **THEN** PublishingResult.Failure with descriptive error is returned

#### Scenario: Disk full
- **WHEN** disk space is insufficient
- **THEN** PublishingResult.Failure with descriptive error is returned

#### Scenario: IO exception
- **WHEN** unexpected IO error occurs
- **THEN** PublishingResult.Failure with original error message is returned

### Requirement: Concurrent Access Safety
The system SHALL handle concurrent publishes safely.

#### Scenario: Concurrent writes to different versions
- **WHEN** two publishes to different versions occur simultaneously
- **THEN** both complete successfully without corruption

#### Scenario: Concurrent metadata updates
- **WHEN** two publishes update metadata simultaneously
- **THEN** both versions appear in final metadata
