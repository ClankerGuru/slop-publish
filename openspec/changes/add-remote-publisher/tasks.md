# Tasks: Remote Maven Repository Publisher

## 1. MavenRepositoryPublisher Implementation
- [x] 1.1 Create `MavenRepositoryPublisher.kt` class
- [x] 1.2 Implement PublishingService interface
- [x] 1.3 Implement `publish()` method for Repository.Maven
- [x] 1.4 Use DefaultMavenDeployer for actual upload
- [x] 1.5 Convert deployment result to PublishingResult

## 2. Maven Central Detection
- [x] 2.1 Detect Maven Central by URL pattern (sonatype, maven.apache.org)
- [x] 2.2 Require full POM validation for Maven Central
- [x] 2.3 Return validation errors before attempting upload

## 3. Retry Policy
- [x] 3.1 Create `RetryPolicy` data class
- [x] 3.2 Implement exponential backoff algorithm
- [x] 3.3 Configure retryable exception types (timeout, connection, 429, 503)
- [x] 3.4 Implement max delay cap
- [x] 3.5 Create default() and noRetry() factory methods

## 4. Validation
- [x] 4.1 Implement validate() method
- [x] 4.2 Use PomValidator for Maven Central repositories
- [x] 4.3 Return validation errors with severity

## 5. Documentation
- [x] 5.1 Clear implementation with retry pattern
- [x] 5.2 Document Maven Central detection logic

## 6. Verification
- [x] 6.1 Build passes successfully
- [x] 6.2 Konsist tests pass (infrastructure depends on domain, maven)
