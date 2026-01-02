package guru.clanker.amper.publish.maven.signing

import guru.clanker.amper.publish.domain.model.Artifact
import guru.clanker.amper.publish.domain.model.Publication
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.security.Security

class BouncyCastleSigner(
    private val armoredKey: String,
    private val passphrase: String,
    private val expectedKeyId: String = ""
) {
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun sign(file: Path): Path {
        if (armoredKey.isBlank()) {
            throw IllegalStateException("Signing key is not configured")
        }

        val signaturePath = file.resolveSibling("${file.fileName}.asc")
        val signingKey = loadSecretKey(armoredKey)
        
        if (expectedKeyId.isNotBlank()) {
            val actualKeyId = extractKeyId(signingKey)
            val normalizedExpected = expectedKeyId.uppercase().takeLast(8)
            if (actualKeyId != normalizedExpected) {
                throw IllegalStateException(
                    "GPG key ID mismatch: expected $normalizedExpected but loaded key has ID $actualKeyId"
                )
            }
        }

        val privateKey = signingKey.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(passphrase.toCharArray())
        )

        val signatureGenerator = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(
                signingKey.publicKey.algorithm,
                PGPUtil.SHA256
            ).setProvider(BouncyCastleProvider.PROVIDER_NAME)
        )

        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey)

        Files.newOutputStream(signaturePath).use { out ->
            ArmoredOutputStream(out).use { armoredOut ->
                Files.newInputStream(file).use { input ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (input.read(buffer).also { len = it } >= 0) {
                        signatureGenerator.update(buffer, 0, len)
                    }
                }
                signatureGenerator.generate().encode(armoredOut)
            }
        }

        return signaturePath
    }

    fun signPublication(publication: Publication): Publication {
        if (armoredKey.isBlank()) {
            return publication
        }

        val signatures = publication.artifacts.map { artifact ->
            val signaturePath = sign(artifact.file)
            Artifact(
                file = signaturePath,
                classifier = artifact.classifier,
                extension = "${artifact.extension}.asc"
            )
        }

        return publication.copy(artifacts = publication.artifacts + signatures)
    }

    companion object {
        fun loadSecretKey(armoredKey: String): PGPSecretKey {
            val keyRing = PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(armoredKey.byteInputStream()),
                JcaKeyFingerprintCalculator()
            )
            return keyRing.keyRings.asSequence()
                .flatMap { it.secretKeys.asSequence() }
                .first { it.isSigningKey }
        }
        
        fun extractKeyId(key: PGPSecretKey): String {
            // Format lower 32 bits as exactly 8 hex digits with zero padding
            return "%08X".format(key.keyID and 0xFFFFFFFFL)
        }
    }
}
