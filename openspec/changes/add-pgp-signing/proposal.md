# Change: Add PGP Signing with Bouncy Castle

## Why
Maven Central requires all artifacts to be signed with PGP/GPG. This module provides artifact signing using Bouncy Castle, supporting key loading from environment variables (CI/CD friendly), GPG keyring files, and direct key input.

## What Changes
- Create `ArtifactSigner` using Bouncy Castle bcpg-jdk18on
- Support ASCII-armored key loading from strings/env vars
- Support GPG keyring file loading
- Generate detached signatures (.asc files)
- Handle passphrase-protected keys
- Verify generated signatures
- Selective signing (skip for specific repositories)

## Impact
- Affected specs: `pgp-signing` (new capability)
- Affected code: `guru.clanker.amper.publish.maven.signing`
- Dependencies: Proposal 1 (Project Setup - Bouncy Castle deps)

## Technical Approach

### ArtifactSigner

```kotlin
// src/guru/clanker/amper/publish/maven/signing/ (Amper convention)

/**
 * Signs artifacts using PGP with Bouncy Castle.
 */
class ArtifactSigner(
    private val signingKey: PGPSecretKey,
    private val passphrase: CharArray
) {
    /**
     * Create signer from ASCII-armored key string.
     * Suitable for CI/CD environments with key as environment variable.
     */
    constructor(armoredKey: String, passphrase: String) : this(
        loadSecretKey(armoredKey),
        passphrase.toCharArray()
    )
    
    /**
     * Create signer from GPG keyring file.
     */
    constructor(keyringPath: Path, keyId: Long, passphrase: String) : this(
        loadSecretKeyFromKeyring(keyringPath, keyId),
        passphrase.toCharArray()
    )
    
    /**
     * Sign a file and return path to detached signature.
     */
    fun sign(file: Path): Path {
        val signaturePath = file.resolveSibling("${file.fileName}.asc")
        
        val privateKey = signingKey.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider())
                .build(passphrase)
        )
        
        val signatureGenerator = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(
                signingKey.publicKey.algorithm,
                PGPUtil.SHA256
            ).setProvider(BouncyCastleProvider())
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
    
    /**
     * Sign multiple files.
     */
    fun signAll(files: List<Path>): List<Path> {
        return files.map { sign(it) }
    }
    
    /**
     * Verify a signature against the original file.
     */
    fun verify(file: Path, signaturePath: Path): Boolean {
        val signature = loadSignature(signaturePath)
        signature.init(
            JcaPGPContentVerifierBuilderProvider().setProvider(BouncyCastleProvider()),
            signingKey.publicKey
        )
        
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var len: Int
            while (input.read(buffer).also { len = it } >= 0) {
                signature.update(buffer, 0, len)
            }
        }
        
        return signature.verify()
    }
    
    companion object {
        init {
            Security.addProvider(BouncyCastleProvider())
        }
        
        fun loadSecretKey(armoredKey: String): PGPSecretKey {
            val keyRing = PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(armoredKey.byteInputStream()),
                JcaKeyFingerprintCalculator()
            )
            return keyRing.keyRings.asSequence()
                .flatMap { it.secretKeys.asSequence() }
                .first { it.isSigningKey }
        }
        
        fun loadSecretKeyFromKeyring(path: Path, keyId: Long): PGPSecretKey {
            Files.newInputStream(path).use { input ->
                val keyRing = PGPSecretKeyRingCollection(
                    PGPUtil.getDecoderStream(input),
                    JcaKeyFingerprintCalculator()
                )
                return keyRing.getSecretKey(keyId)
                    ?: throw IllegalArgumentException("Key $keyId not found in keyring")
            }
        }
    }
}
```

### SigningConfiguration

```kotlin
/**
 * Configuration for artifact signing.
 */
data class SigningConfiguration(
    val enabled: Boolean,
    val keySource: KeySource,
    val passphrase: String,
    val skipRepositories: List<String> = emptyList()
)

sealed class KeySource {
    /** ASCII-armored key from string (e.g., environment variable) */
    data class ArmoredKey(val key: String) : KeySource()
    
    /** Key from GPG keyring file */
    data class Keyring(val path: Path, val keyId: Long) : KeySource()
    
    /** Key from file path */
    data class KeyFile(val path: Path) : KeySource()
}
```

### Integration with Publishing

```kotlin
/**
 * Signs publication artifacts before deployment.
 */
class SigningPublisher(
    private val signer: ArtifactSigner,
    private val config: SigningConfiguration,
    private val delegate: PublishingRepository
) : PublishingRepository {
    
    override suspend fun publish(
        publication: Publication,
        repository: Repository
    ): PublishingResult {
        // Skip signing for excluded repositories
        if (repository.id in config.skipRepositories) {
            return delegate.publish(publication, repository)
        }
        
        // Sign all artifacts
        val signatures = publication.artifacts.map { artifact ->
            Artifact(
                file = signer.sign(artifact.file),
                classifier = artifact.classifier,
                extension = "${artifact.extension}.asc"
            )
        }
        
        // Sign POM
        val pomSignature = // ... sign POM file
        
        // Publish with signatures
        val enrichedPublication = publication.copy(
            artifacts = publication.artifacts + signatures + pomSignature
        )
        
        return delegate.publish(enrichedPublication, repository)
    }
}
```

## Acceptance Criteria
- [ ] Sign artifacts using Bouncy Castle
- [ ] Load keys from ASCII-armored strings (env vars)
- [ ] Load keys from GPG keyring files
- [ ] Generate detached .asc signature files
- [ ] Handle passphrase-protected keys
- [ ] Verify generated signatures
- [ ] Skip signing for specified repositories
- [ ] Clear passphrase from memory after use

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Key exposure in memory | Clear passphrases, use char[] not String |
| Bouncy Castle version conflicts | Pin version, test compatibility |
| Invalid key format | Clear error messages with format examples |
| Key not found | List available keys in error message |

## Estimated Effort
**Size: L** (Large) - 3-4 days
