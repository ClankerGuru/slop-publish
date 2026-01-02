# Tasks: GitHub Packages Publisher

## 1. Ktor Client Setup
- [x] 1.1 Create default HttpClient with CIO engine
- [x] 1.2 Configure timeouts (5 min request, 30s connect)
- [x] 1.3 Make client injectable for testing

## 2. GitHubPackagesPublisher Implementation
- [x] 2.1 Create `GitHubPackagesPublisher.kt` class
- [x] 2.2 Implement PublishingService interface
- [x] 2.3 Implement `publish()` for Repository.GitHubPackages
- [x] 2.4 Upload each artifact via HTTP PUT
- [x] 2.5 Return PublishingResult with published artifacts

## 3. URL Building
- [x] 3.1 Implement `buildArtifactUrl()` method
- [x] 3.2 Handle Maven coordinate path conversion via Coordinates.toPath()
- [x] 3.3 Use Artifact.filename() for correct file naming

## 4. Authentication
- [x] 4.1 Add Bearer token header from repository.token
- [x] 4.2 Support Personal Access Token (PAT) / GITHUB_TOKEN
- [x] 4.3 Handle 401/403 as AuthenticationFailed

## 5. Rate Limiting
- [x] 5.1 Create `RateLimiter` class
- [x] 5.2 Implement sliding window rate limiting (30 req/min default)
- [x] 5.3 Handle HTTP 429 response
- [x] 5.4 Parse Retry-After header
- [x] 5.5 Implement retry logic after rate limit

## 6. Upload Logic
- [x] 6.1 Create `UploadResult` sealed class
- [x] 6.2 Implement Success case with PublishedArtifact
- [x] 6.3 Implement RateLimited case with retryAfterMs
- [x] 6.4 Implement Failure case with PublishingError
- [x] 6.5 Set correct Content-Type for artifact (jar, pom, asc)

## 7. Error Handling
- [x] 7.1 Map HTTP 401 to AuthenticationFailed
- [x] 7.2 Map HTTP 403 to AuthenticationFailed
- [x] 7.3 Map HTTP 429 to RateLimited
- [x] 7.4 Map other errors to RepositoryError
- [x] 7.5 Handle network exceptions

## 8. Validation
- [x] 8.1 Implement validate() method
- [x] 8.2 Check for blank token

## 9. Documentation
- [x] 9.1 Clear implementation with Ktor client
- [x] 9.2 Document rate limiting in RateLimiter class

## 10. Verification
- [x] 10.1 Build passes with Ktor dependencies
- [x] 10.2 Konsist tests pass (infrastructure depends on domain, maven)
