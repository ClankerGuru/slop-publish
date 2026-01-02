package guru.clanker.amper.publish.maven.resolver

import guru.clanker.amper.publish.domain.model.*
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.deployment.DeployRequest
import org.eclipse.aether.deployment.DeploymentException
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import java.nio.file.Path

class DefaultMavenDeployer {

    private val repositorySystem: RepositorySystem = createRepositorySystem()

    fun deploy(publication: Publication, repository: Repository): PublishingResult {
        if (repository !is Repository.Maven) {
            return PublishingResult.Failure(
                publication,
                repository,
                PublishingError.ValidationError("DefaultMavenDeployer requires Repository.Maven")
            )
        }

        val session = MavenRepositorySystemUtils.newSession()
        val localRepoPath = Path.of(System.getProperty("user.home"), ".m2", "repository")
        session.localRepositoryManager = repositorySystem.newLocalRepositoryManager(
            session,
            LocalRepository(localRepoPath.toFile())
        )

        val remoteRepo = createRemoteRepository(repository)

        val request = DeployRequest().apply {
            this.repository = remoteRepo

            publication.artifacts.forEach { artifact ->
                addArtifact(artifact.toMavenArtifact(publication.coordinates))
            }
        }

        return try {
            val result = repositorySystem.deploy(session, request)
            val publishedArtifacts = result.artifacts.map { mavenArtifact ->
                PublishedArtifact(
                    artifact = Artifact(
                        file = mavenArtifact.file.toPath(),
                        classifier = mavenArtifact.classifier.takeIf { it.isNotEmpty() },
                        extension = mavenArtifact.extension
                    ),
                    remoteUrl = "${repository.url}/${publication.coordinates.toPath()}/${mavenArtifact.file.name}",
                    checksum = ChecksumGenerator.generate(mavenArtifact.file.toPath())
                )
            }
            PublishingResult.Success(publication, repository, publishedArtifacts)
        } catch (e: DeploymentException) {
            PublishingResult.Failure(
                publication,
                repository,
                mapDeploymentError(e)
            )
        }
    }

    private fun createRepositorySystem(): RepositorySystem {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        return locator.getService(RepositorySystem::class.java)
    }

    private fun createRemoteRepository(repository: Repository.Maven): RemoteRepository {
        val builder = RemoteRepository.Builder(
            repository.id,
            "default",
            repository.url
        )

        repository.credentials?.let { creds ->
            builder.setAuthentication(
                AuthenticationBuilder()
                    .addUsername(creds.username)
                    .addPassword(creds.password)
                    .build()
            )
        }

        return builder.build()
    }

    private fun Artifact.toMavenArtifact(coordinates: Coordinates): DefaultArtifact {
        return DefaultArtifact(
            coordinates.groupId,
            coordinates.artifactId,
            classifier ?: "",
            extension,
            coordinates.version,
            null,
            file.toFile()
        )
    }

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
}
