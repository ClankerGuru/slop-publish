# Tasks: Local Repository Publisher

## 1. LocalRepositoryPublisher Implementation
- [x] 1.1 Create `LocalRepositoryPublisher.kt` class
- [x] 1.2 Implement PublishingService interface
- [x] 1.3 Implement `publish()` method accepting Publication
- [x] 1.4 Resolve artifact path using Maven layout conventions
- [x] 1.5 Create directory structure if not exists
- [x] 1.6 Copy artifact files to target location
- [x] 1.7 Return PublishingResult with published artifacts

## 2. Checksum Generation
- [x] 2.1 Generate MD5 checksum file (.md5)
- [x] 2.2 Generate SHA-1 checksum file (.sha1)
- [x] 2.3 Write checksum files alongside artifacts
- [x] 2.4 Use ChecksumGenerator from maven.resolver package

## 3. Maven Metadata Management
- [x] 3.1 Implement metadata XML generation
- [x] 3.2 Parse existing versions from metadata file
- [x] 3.3 Add version to versions list
- [x] 3.4 Update latest and release versions
- [x] 3.5 Update lastUpdated timestamp
- [x] 3.6 Sort versions for display

## 4. File Naming
- [x] 4.1 Implement artifact file naming (artifactId-version[-classifier].extension)
- [x] 4.2 Handle classifiers (sources, javadoc)
- [x] 4.3 Handle different extensions (jar, pom)

## 5. Path Resolution
- [x] 5.1 Convert groupId to path (dots to slashes)
- [x] 5.2 Resolve full artifact path via Coordinates.toPath()
- [x] 5.3 Support custom local repository path
- [x] 5.4 Default to ~/.m2/repository

## 6. Error Handling
- [x] 6.1 Handle IOException during file operations
- [x] 6.2 Return appropriate PublishingError types

## 7. Documentation
- [x] 7.1 Clear implementation with Maven layout conventions
- [x] 7.2 Document default path in companion object

## 8. Verification
- [x] 8.1 Build passes successfully
- [x] 8.2 Konsist tests pass (infrastructure depends on domain, maven)
