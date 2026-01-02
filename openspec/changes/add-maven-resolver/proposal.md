# Change: Add Maven Resolver Integration

## Why
Maven Resolver (formerly Aether) is the core library for programmatic artifact deployment to Maven repositories. This module wraps Maven Resolver APIs to provide a clean Kotlin interface for deploying artifacts, handling authentication, and managing repository sessions.

## What Changes
- Create `ResolverFactory` to instantiate and configure Maven RepositorySystem
- Create `MavenDeployer` to deploy artifacts using Maven Resolver API
- Implement `RepositorySystemSession` configuration with authentication
- Support HTTP and file:// transports
- Generate checksums (MD5, SHA-1, SHA-256) for artifacts
- Handle deployment errors with retry logic
- Isolate Maven APIs from domain layer

## Impact
- Affected specs: `maven-resolver` (new capability)
- Affected code: `guru.clanker.amper.publish.maven.resolver`
- Dependencies: Proposal 1 (Project Setup), Proposal 2 (Domain Model)

## Technical Approach

### Maven Resolver Architecture

Maven Resolver requires several components:
1. **RepositorySystem** - Core service for resolution/deployment
2. **RepositorySystemSession** - Configuration for a single session
3. **RemoteRepository** - Target repository definition
4. **DeployRequest** - Artifacts to deploy

### ResolverFactory

```kotlin
// src/guru/clanker/amper/publish/maven/resolver/ (Amper convention)

/**
 * Factory for creating configured Maven Resolver components.
 */
class ResolverFactory {
    
    /**
     * Create a new RepositorySystem with HTTP and file transports.
     */
    fun createRepositorySystem(): RepositorySystem {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        return locator.getService(RepositorySystem::class.java)
    }
    
    /**
     * Create a session configured for deployment.
     */
    fun createSession(
        system: RepositorySystem,
        localRepo: Path = defaultLocalRepo()
    ): RepositorySystemSession {
        val session = MavenRepositorySystemUtils.newSession()
        session.localRepositoryManager = system.newLocalRepositoryManager(
            session,
            LocalRepository(localRepo.toFile())
        )
        session.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        return session
    }
    
    /**
     * Create RemoteRepository from domain Repository.
     */
    fun createRemoteRepository(repository: Repository): RemoteRepository {
        val builder = RemoteRepository.Builder(
            repository.id,
            "default",
            repository.url
        )
        
        when (repository) {
            is Repository.Maven -> {
                repository.credentials?.let { creds ->
                    builder.setAuthentication(
                        AuthenticationBuilder()
                            .addUsername(creds.username)
                            .addPassword(creds.password)
                            .build()
                    )
                }
            }
            is Repository.GitHubPackages -> {
                builder.setAuthentication(
                    AuthenticationBuilder()
                        .addUsername(repository.owner)
                        .addPassword(repository.token)
                        .build()
                )
            }
            is Repository.Local -> {
                // No auth for local
            }
        }
        
        return builder.build()
    }
}
```

### MavenDeployer

```kotlin
/**
 * Deploys artifacts to Maven repositories using Maven Resolver.
 */
class MavenDeployer(
    private val resolverFactory: ResolverFactory = ResolverFactory()
) {
    private val repositorySystem = resolverFactory.createRepositorySystem()
    
    /**
     * Deploy a publication to the specified repository.
     */
    fun deploy(
        publication: Publication,
        repository: Repository,
        additionalArtifacts: List<Path> = emptyList()  // Signatures, checksums
    ): DeploymentResult {
        val session = resolverFactory.createSession(repositorySystem)
        val remoteRepo = resolverFactory.createRemoteRepository(repository)
        
        val request = DeployRequest().apply {
            this.repository = remoteRepo
            
            // Add main artifacts
            publication.artifacts.forEach { artifact ->
                addArtifact(artifact.toMavenArtifact(publication.coordinates))
            }
            
            // Add additional files (signatures)
            additionalArtifacts.forEach { file ->
                addArtifact(createArtifactFromFile(file, publication.coordinates))
            }
        }
        
        return try {
            val result = repositorySystem.deploy(session, request)
            DeploymentResult.Success(
                deployedArtifacts = result.artifacts.map { it.toPublishedArtifact() }
            )
        } catch (e: DeploymentException) {
            DeploymentResult.Failure(
                error = mapDeploymentError(e),
                cause = e
            )
        }
    }
    
    private fun Artifact.toMavenArtifact(coordinates: Coordinates): DefaultArtifact {
        return DefaultArtifact(
            coordinates.groupId,
            coordinates.artifactId,
            classifier,
            extension,
            coordinates.version,
            null,
            file.toFile()
        )
    }
}

sealed class DeploymentResult {
    data class Success(val deployedArtifacts: List<PublishedArtifact>) : DeploymentResult()
    data class Failure(val error: PublishingError, val cause: Throwable?) : DeploymentResult()
}
```

### Checksum Generation

```kotlin
/**
 * Generates checksums for artifact files.
 */
object ChecksumGenerator {
    
    fun generate(file: Path): Checksum {
        val bytes = Files.readAllBytes(file)
        return Checksum(
            md5 = bytes.md5Hex(),
            sha1 = bytes.sha1Hex(),
            sha256 = bytes.sha256Hex()
        )
    }
    
    private fun ByteArray.md5Hex(): String = 
        MessageDigest.getInstance("MD5").digest(this).toHexString()
    
    private fun ByteArray.sha1Hex(): String = 
        MessageDigest.getInstance("SHA-1").digest(this).toHexString()
    
    private fun ByteArray.sha256Hex(): String = 
        MessageDigest.getInstance("SHA-256").digest(this).toHexString()
    
    private fun ByteArray.toHexString(): String = 
        joinToString("") { "%02x".format(it) }
}
```

### Error Mapping

```kotlin
private fun mapDeploymentError(e: DeploymentException): PublishingError {
    return when {
        e.message?.contains("401") == true -> 
            PublishingError.AuthenticationFailed("Authentication failed: ${e.message}")
        e.message?.contains("403") == true ->
            PublishingError.AuthenticationFailed("Access denied: ${e.message}")
        e.message?.contains("404") == true ->
            PublishingError.RepositoryError("Repository not found", 404)
        e.cause is java.net.ConnectException ->
            PublishingError.NetworkError("Connection failed", e.cause)
        else ->
            PublishingError.RepositoryError(e.message ?: "Deployment failed", null)
    }
}
```

## Acceptance Criteria
- [ ] ResolverFactory creates properly configured RepositorySystem
- [ ] MavenDeployer deploys artifacts to local repository
- [ ] MavenDeployer deploys artifacts to remote repository with auth
- [ ] Checksums generated correctly (MD5, SHA-1, SHA-256)
- [ ] Deployment errors mapped to domain error types
- [ ] Integration tests verify deployment to local repo
- [ ] Maven Resolver types not exposed in public API

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Maven Resolver version conflicts | Pin explicit versions in libs.versions.toml, verify compatibility |
| HTTP transport configuration complexity | Start with defaults, make configurable later |
| Session lifecycle management | Create fresh session per deployment, document reuse patterns |

## Estimated Effort
**Size: L** (Large) - 3-4 days
