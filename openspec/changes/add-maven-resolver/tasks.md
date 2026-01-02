# Tasks: Maven Resolver Integration

## 1. DefaultMavenDeployer Implementation
- [x] 1.1 Create `DefaultMavenDeployer.kt` class
- [x] 1.2 Implement `createRepositorySystem()` with service locator
- [x] 1.3 Register BasicRepositoryConnectorFactory
- [x] 1.4 Register FileTransporterFactory for file:// URLs
- [x] 1.5 Register HttpTransporterFactory for HTTP(S) URLs
- [x] 1.6 Implement `deploy()` method accepting Publication and Repository
- [x] 1.7 Create session with local repository configuration
- [x] 1.8 Configure checksum policy

## 2. Authentication Support
- [x] 2.1 Implement username/password auth for Maven repositories
- [x] 2.2 Create AuthenticationBuilder integration
- [x] 2.3 Handle auth errors gracefully (401/403 mapping)

## 3. Artifact Conversion
- [x] 3.1 Convert domain Artifact to Maven DefaultArtifact
- [x] 3.2 Build DeployRequest with all artifacts
- [x] 3.3 Execute deployment via RepositorySystem.deploy()
- [x] 3.4 Handle deployment response and errors

## 4. Checksum Generation
- [x] 4.1 Create `ChecksumGenerator.kt` object
- [x] 4.2 Implement MD5 checksum generation
- [x] 4.3 Implement SHA-1 checksum generation
- [x] 4.4 Implement SHA-256 checksum generation
- [x] 4.5 Use efficient byte array operations

## 5. Error Handling
- [x] 5.1 Create error mapping from DeploymentException to PublishingError
- [x] 5.2 Handle 401 Unauthorized → AuthenticationFailed
- [x] 5.3 Handle 403 Forbidden → AuthenticationFailed
- [x] 5.4 Handle 404 Not Found → RepositoryError
- [x] 5.5 Handle network errors → NetworkError
- [x] 5.6 Preserve original exception for debugging

## 6. Result Mapping
- [x] 6.1 Map Maven Artifact results to PublishedArtifact
- [x] 6.2 Generate remote URLs for published artifacts
- [x] 6.3 Include checksums in PublishedArtifact

## 7. Documentation
- [x] 7.1 Add implementation with clear code structure
- [x] 7.2 Document Maven Resolver usage patterns in code

## 8. Verification
- [x] 8.1 Verify Maven types isolated to maven.resolver package
- [x] 8.2 Run Konsist architecture tests
- [x] 8.3 Build passes successfully
