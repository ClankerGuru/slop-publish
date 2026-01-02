# Tasks: PGP Signing with Bouncy Castle

## 1. Bouncy Castle Setup
- [x] 1.1 Register BouncyCastleProvider on init (idempotent)
- [x] 1.2 Verify bcpg-jdk18on and bcprov-jdk18on dependencies in module.yaml

## 2. Key Loading
- [x] 2.1 Create `loadSecretKey(armoredKey: String)` for ASCII-armored keys
- [x] 2.2 Find signing-capable key from keyring
- [x] 2.3 Handle key loading with JcaKeyFingerprintCalculator

## 3. BouncyCastleSigner Implementation
- [x] 3.1 Create `BouncyCastleSigner.kt` class
- [x] 3.2 Implement constructor from armored key string and passphrase
- [x] 3.3 Extract private key with JcePBESecretKeyDecryptorBuilder
- [x] 3.4 Handle blank key gracefully (skip signing)

## 4. Signature Generation
- [x] 4.1 Initialize PGPSignatureGenerator with SHA-256
- [x] 4.2 Stream file content through signature generator
- [x] 4.3 Output ASCII-armored signature (.asc)
- [x] 4.4 Implement `sign(file: Path): Path`
- [x] 4.5 Implement `signPublication(publication): Publication`

## 5. Publication Signing
- [x] 5.1 Sign all artifacts in publication
- [x] 5.2 Create signature artifacts with .asc extension
- [x] 5.3 Return new publication with signature artifacts added

## 6. Security Considerations
- [x] 6.1 Use char[] for passphrases in BC API
- [x] 6.2 Handle blank key configuration gracefully

## 7. Documentation
- [x] 7.1 Clear implementation with BouncyCastle patterns
- [x] 7.2 Document key loading in companion object

## 8. Verification
- [x] 8.1 Build passes with Bouncy Castle dependencies
- [x] 8.2 Konsist tests pass (signing in maven package)
