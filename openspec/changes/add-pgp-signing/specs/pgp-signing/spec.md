# PGP Signing Specification

## ADDED Requirements

### Requirement: ASCII-Armored Key Loading
The system SHALL load PGP keys from ASCII-armored strings for CI/CD compatibility.

#### Scenario: Load from environment variable
- **WHEN** creating ArtifactSigner with armored key string
- **THEN** key is parsed and ready for signing

#### Scenario: Invalid armored key format
- **WHEN** armored key string is malformed
- **THEN** clear exception with format guidance is thrown

#### Scenario: Key without signing capability
- **WHEN** armored key cannot sign (encryption-only)
- **THEN** exception indicates signing key required

### Requirement: Keyring File Loading
The system SHALL load PGP keys from GPG keyring files.

#### Scenario: Load from keyring with ID
- **WHEN** creating ArtifactSigner with keyring path and key ID
- **THEN** specified key is extracted from keyring

#### Scenario: Key ID not found
- **WHEN** specified key ID doesn't exist in keyring
- **THEN** exception lists available key IDs

#### Scenario: Default GPG keyring
- **WHEN** loading from ~/.gnupg/secring.gpg or pubring.kbx
- **THEN** key is loaded successfully

### Requirement: Signature Generation
The system SHALL generate detached PGP signatures in ASCII-armored format.

#### Scenario: Sign single file
- **WHEN** calling sign(file)
- **THEN** file.asc is created containing ASCII-armored signature

#### Scenario: Signature content
- **WHEN** examining generated .asc file
- **THEN** content starts with "-----BEGIN PGP SIGNATURE-----"

#### Scenario: Sign multiple files
- **WHEN** calling signAll with list of files
- **THEN** .asc file is created for each input file

#### Scenario: SHA-256 algorithm
- **WHEN** generating signature
- **THEN** SHA-256 hash algorithm is used

### Requirement: Passphrase Handling
The system SHALL handle passphrase-protected private keys securely.

#### Scenario: Correct passphrase
- **WHEN** providing correct passphrase
- **THEN** key is decrypted and signing succeeds

#### Scenario: Incorrect passphrase
- **WHEN** providing wrong passphrase
- **THEN** clear exception indicates passphrase failure

#### Scenario: Passphrase memory cleanup
- **WHEN** signing completes or fails
- **THEN** passphrase char array is cleared from memory

### Requirement: Signature Verification
The system SHALL verify generated signatures.

#### Scenario: Valid signature
- **WHEN** verifying file against its valid signature
- **THEN** verify() returns true

#### Scenario: Tampered file
- **WHEN** verifying modified file against original signature
- **THEN** verify() returns false

#### Scenario: Wrong key verification
- **WHEN** verifying with different key than signer
- **THEN** verify() returns false

### Requirement: Selective Signing
The system SHALL skip signing for specified repositories.

#### Scenario: Skip for GitHub Packages
- **WHEN** publishing to repository in skipRepositories list
- **THEN** no signatures are generated

#### Scenario: Sign for Maven Central
- **WHEN** publishing to repository not in skipRepositories
- **THEN** all artifacts are signed

#### Scenario: Configure skip list
- **WHEN** configuring SigningConfiguration
- **THEN** skipRepositories accepts list of repository IDs

### Requirement: Publishing Integration
The system SHALL integrate signing into publishing pipeline.

#### Scenario: Sign before publish
- **WHEN** using SigningPublisher
- **THEN** artifacts are signed before delegation to publisher

#### Scenario: Include signatures in publication
- **WHEN** publishing signed artifacts
- **THEN** .asc files are included as additional artifacts

#### Scenario: POM signing
- **WHEN** publishing to Maven Central
- **THEN** POM file is also signed

### Requirement: Security
The system SHALL protect key material and passphrases.

#### Scenario: No key logging
- **WHEN** errors occur during signing
- **THEN** key material never appears in logs

#### Scenario: Char array passphrases
- **WHEN** passphrase is provided
- **THEN** it uses char[] (not String) for memory safety
