# slop-publish

[![Maven Central](https://img.shields.io/maven-central/v/guru.clanker/slop-publish?style=flat-square&logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/artifact/guru.clanker/slop-publish)
[![License](https://img.shields.io/github/license/ClankerGuru/slop-publish?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org)
[![Build](https://img.shields.io/github/actions/workflow/status/ClankerGuru/slop-publish/ci.yml?style=flat-square&logo=github&label=CI)](https://github.com/ClankerGuru/slop-publish/actions/workflows/ci.yml)
[![Amper](https://img.shields.io/badge/Amper-0.9.2-blueviolet?style=flat-square&logo=jetbrains)](https://github.com/JetBrains/amper/releases/tag/v0.9.2)

Maven publishing plugin for Amper build system.

## Features

- Publish to Maven repositories (Maven Central, Nexus, Artifactory)
- Publish to GitHub Packages
- PGP signing with Bouncy Castle
- Declarative YAML configuration via Amper plugin settings

## Requirements

- JDK 17+
- Amper build system

## Installation

Add the plugin to your `module.yaml`:

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
```

## Usage

```bash
# Publish to local Maven repository (~/.m2/repository)
./amper task :my-module:publishLocal@slop-publish

# Publish to configured repositories
./amper task :my-module:publish@slop-publish

# Validate configuration without publishing
./amper task :my-module:validate@slop-publish

# Dry run - show what would be published
./amper task :my-module:publishDryRun@slop-publish
```

## Configuration

Full configuration example in `module.yaml`:

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
    artifacts:
      - jar
    
    repositories:
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repo
      
      - id: central
        type: maven
        url: https://central.sonatype.com/api/v1/publisher/upload
    
    pom:
      name: My Library
      description: A useful library
      url: https://github.com/owner/repo
      licenses:
        - name: MIT
          url: https://opensource.org/licenses/MIT
      developers:
        - id: dev1
          name: Developer One
          email: dev@example.com
      scm:
        url: https://github.com/owner/repo
        connection: scm:git:git://github.com/owner/repo.git
        developerConnection: scm:git:ssh://github.com/owner/repo.git
    
    signing:
      enabled: true
      keyId: YOUR_GPG_KEY_ID
```

## Environment Variables

Signing credentials are read from environment variables (never put secrets in config files):

| Property | Env Var Fallback | Description |
|----------|------------------|-------------|
| `guru.clanker.gpg.key.id` | `GPG_KEY_ID` | Key ID for validation |
| `guru.clanker.gpg.key.secret` | `GPG_SECRET_KEY` | Armored private key |
| `guru.clanker.gpg.key.passphrase` | `GPG_PASSPHRASE` | Key passphrase |

### Getting Your GPG Key

```bash
# List your keys to find the key ID
gpg --list-secret-keys --keyid-format SHORT

# Export your private key (armored)
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# The contents of private-key.asc goes into GPG_SECRET_KEY
# Delete the file after copying to a secure location!
rm private-key.asc
```

### Local Development

**Option 1: local.properties (recommended)**

Create `local.properties` in your project root (add to `.gitignore`!):
```properties
guru.clanker.gpg.key.id=65C9CBAA
guru.clanker.gpg.key.secret=-----BEGIN PGP PRIVATE KEY BLOCK-----\n\
lQPGBF...\n\
-----END PGP PRIVATE KEY BLOCK-----
guru.clanker.gpg.key.passphrase=your-passphrase
```

Configure in `module.yaml`:
```yaml
signing:
  enabled: true
  credentials:
    file: ./local.properties
```

The `keyId` is validated against the secret key - if they don't match, signing fails with a clear error.

**Option 2: Environment variables**

```bash
export GPG_SECRET_KEY="$(cat ~/.gnupg/private-key.asc)"
export GPG_PASSPHRASE="your-passphrase"
./amper task :my-module:publish@slop-publish
```

**Option 3: direnv**

Create `.envrc` in your project root (add to `.gitignore`!):
```bash
export GPG_SECRET_KEY="$(cat ~/.gnupg/private-key.asc)"
export GPG_PASSPHRASE="your-passphrase"
```

Then run `direnv allow` once.

### Credential Resolution Order

1. `credentials.file` - Properties file (if specified)
2. `key` / `password` - Direct values in config (if specified)  
3. `GPG_SECRET_KEY` / `GPG_PASSPHRASE` - Environment variables (fallback)

### GitHub Actions

Add secrets in your repository: **Settings → Secrets and variables → Actions**

| Secret Name | Value |
|-------------|-------|
| `GPG_SECRET_KEY` | Contents of your armored private key |
| `GPG_PASSPHRASE` | Your GPG passphrase |

Use in workflow:
```yaml
- name: Publish
  env:
    GPG_SECRET_KEY: ${{ secrets.GPG_SECRET_KEY }}
    GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
  run: ./amper task :my-module:publish@slop-publish
```

## Development

```bash
./amper build    # Build the project
./amper test     # Run tests (including architecture tests)
```

## Architecture

```
src/guru/clanker/amper/publish/
├── domain/         # Pure domain model (no dependencies)
├── settings/       # @Configurable plugin settings
├── tasks/          # @TaskAction plugin entry points
├── maven/          # Maven Resolver integration
│   ├── resolver/   # Checksum & deployment
│   ├── pom/        # POM generation & validation
│   └── signing/    # PGP signing with Bouncy Castle
└── infrastructure/ # Repository implementations
```

Architecture rules enforced by Konsist tests ensure clean layer separation:
- `domain` depends on nothing
- `settings` depends on `domain`
- `maven` depends on `domain`
- `infrastructure` depends on `domain`, `maven`
- `tasks` depends on `settings`, `domain`, `infrastructure`, `maven`

## License

MIT
