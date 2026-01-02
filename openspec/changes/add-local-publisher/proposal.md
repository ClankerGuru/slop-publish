# Change: Add Local Repository Publisher

## Why
Local repository publishing (`~/.m2/repository`) is essential for development and testing workflows. It allows developers to test artifacts before publishing to remote repositories and enables offline publishing without network access.

## What Changes
- Create `LocalRepositoryPublisher` implementing publishing to local Maven repository
- Follow Maven repository layout conventions for artifact placement
- Generate maven-metadata.xml for version listings
- Support both snapshot and release versions
- Pure file operations with no network required

## Impact
- Affected specs: `local-publisher` (new capability)
- Affected code: `guru.clanker.amper.publish.infrastructure`
- Dependencies: Proposal 2 (Domain Model), Proposal 4 (Maven Resolver), Proposal 5 (POM Generation)

## Technical Approach

### LocalRepositoryPublisher

```kotlin
// src/guru/clanker/amper/publish/infrastructure/ (Amper convention)

/**
 * Publishes artifacts to local Maven repository.
 */
class LocalRepositoryPublisher(
    private val localRepoPath: Path = defaultLocalRepo(),
    private val pomGenerator: PomGenerator = PomGenerator(),
    private val checksumGenerator: ChecksumGenerator = ChecksumGenerator
) : PublishingRepository {
    
    override suspend fun publish(publication: Publication): PublishingResult {
        val basePath = resolveArtifactPath(publication.coordinates)
        Files.createDirectories(basePath)
        
        return try {
            val publishedArtifacts = mutableListOf<PublishedArtifact>()
            
            // Copy artifacts
            publication.artifacts.forEach { artifact ->
                val targetPath = basePath.resolve(artifactFileName(publication.coordinates, artifact))
                Files.copy(artifact.file, targetPath, StandardCopyOption.REPLACE_EXISTING)
                
                // Generate checksums
                writeChecksums(targetPath)
                
                publishedArtifacts.add(PublishedArtifact(
                    artifact = artifact,
                    remoteUrl = targetPath.toUri().toString(),
                    checksum = checksumGenerator.generate(targetPath)
                ))
            }
            
            // Generate and write POM
            val pomPath = basePath.resolve("${publication.coordinates.artifactId}-${publication.coordinates.version}.pom")
            pomGenerator.generateToFile(publication, pomPath, full = publication.pomMetadata != null)
            writeChecksums(pomPath)
            
            // Update maven-metadata.xml
            updateMetadata(publication.coordinates)
            
            PublishingResult.Success(publication, Repository.Local("local", localRepoPath.toUri().toString()), publishedArtifacts)
        } catch (e: IOException) {
            PublishingResult.Failure(
                publication,
                Repository.Local("local", localRepoPath.toUri().toString()),
                PublishingError.RepositoryError("Failed to write to local repository: ${e.message}", null)
            )
        }
    }
    
    private fun resolveArtifactPath(coordinates: Coordinates): Path {
        return localRepoPath.resolve(coordinates.toPath())
    }
    
    private fun artifactFileName(coordinates: Coordinates, artifact: Artifact): String {
        val classifier = artifact.classifier?.let { "-$it" } ?: ""
        return "${coordinates.artifactId}-${coordinates.version}$classifier.${artifact.extension}"
    }
    
    private fun writeChecksums(file: Path) {
        val checksums = checksumGenerator.generate(file)
        Files.writeString(file.resolveSibling("${file.fileName}.md5"), checksums.md5)
        Files.writeString(file.resolveSibling("${file.fileName}.sha1"), checksums.sha1)
    }
    
    private fun updateMetadata(coordinates: Coordinates) {
        val metadataPath = localRepoPath
            .resolve(coordinates.groupId.replace('.', '/'))
            .resolve(coordinates.artifactId)
            .resolve("maven-metadata-local.xml")
        
        val metadata = if (Files.exists(metadataPath)) {
            parseMetadata(metadataPath)
        } else {
            MavenMetadata(coordinates.groupId, coordinates.artifactId)
        }
        
        metadata.addVersion(coordinates.version)
        writeMetadata(metadataPath, metadata)
    }
}

fun defaultLocalRepo(): Path = Path.of(System.getProperty("user.home"), ".m2", "repository")
```

### Maven Metadata Management

```kotlin
data class MavenMetadata(
    val groupId: String,
    val artifactId: String,
    val versions: MutableList<String> = mutableListOf(),
    var latest: String? = null,
    var release: String? = null,
    var lastUpdated: String = nowTimestamp()
) {
    fun addVersion(version: String) {
        if (version !in versions) {
            versions.add(version)
            versions.sortWith(VersionComparator)
            latest = versions.last()
            if (!version.endsWith("-SNAPSHOT")) {
                release = versions.filter { !it.endsWith("-SNAPSHOT") }.lastOrNull()
            }
            lastUpdated = nowTimestamp()
        }
    }
}
```

## Acceptance Criteria
- [ ] Publish artifacts to `~/.m2/repository`
- [ ] Follow Maven repository layout (groupId as path, artifactId, version)
- [ ] Generate POM file in correct location
- [ ] Generate MD5 and SHA-1 checksum files
- [ ] Create/update maven-metadata.xml with version listing
- [ ] Support custom local repository path
- [ ] Handle concurrent access safely

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Concurrent write conflicts | Use file locking or atomic operations |
| Corrupted metadata file | Backup before write, validate after |
| Disk space issues | Check available space before large uploads |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
