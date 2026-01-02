# slop-publish

[![Maven Central](https://img.shields.io/maven-central/v/guru.clanker/slop-publish?style=flat-square&logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/artifact/guru.clanker/slop-publish)
[![License](https://img.shields.io/github/license/ClankerGuru/slop-publish?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org)
[![Build](https://img.shields.io/github/actions/workflow/status/ClankerGuru/slop-publish/ci.yml?style=flat-square&logo=github&label=CI)](https://github.com/ClankerGuru/slop-publish/actions/workflows/ci.yml)
[![Amper](https://img.shields.io/badge/Amper-0.9.2-blueviolet?style=flat-square&logo=jetbrains)](https://github.com/JetBrains/amper/releases/tag/v0.9.2)

Maven publishing plugin for Amper build system.

> **Note**: This plugin may be deprecated if JetBrains adds native Maven publishing support to Amper.
> We'll update this README if that happens.

## Current Limitation

**Amper does not yet support importing plugins from Maven repositories.** You must include the plugin source in your project. See [Installation](#installation) for options.

This is an Amper limitation, not a plugin limitation. JetBrains is aware and may add external plugin support in a future release. Track progress at [JetBrains/amper](https://github.com/JetBrains/amper).

## Features

- **Maven Central Portal** - Direct publishing via bundle upload API (recommended)
- Publish to Maven repositories (Nexus, Artifactory)
- Publish to GitHub Packages
- **Auto-generate sources & javadoc JARs** - No manual JAR creation needed
- PGP signing with Bouncy Castle
- Declarative YAML configuration via Amper plugin settings

## Requirements

- JDK 17+
- Amper build system

## Installation

Since Amper doesn't support external plugin imports yet, you need to include the plugin source in your project.

### Option 1: Git Submodule (Recommended)

Git submodules let you pin a specific version and update easily.

**Initial setup:**
```bash
# Add the plugin as a submodule
git submodule add https://github.com/ClankerGuru/slop-publish.git plugins/slop-publish

# Pin to a specific version (recommended)
cd plugins/slop-publish
git checkout v1.0.5
cd ../..

# Commit the submodule reference
git add .gitmodules plugins/slop-publish
git commit -m "Add slop-publish plugin v1.0.5"
```

**project.yaml:**
```yaml
modules:
  - .
  - plugins/slop-publish   # Add plugin as a module

plugins:
  - ./plugins/slop-publish  # Make plugin available
```

**For teammates cloning your repo:**
```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/your/project.git

# Or if already cloned without submodules
git submodule update --init --recursive
```

**Updating to a new version:**
```bash
cd plugins/slop-publish
git fetch --tags
git checkout v1.0.6  # or desired version
cd ../..
git add plugins/slop-publish
git commit -m "Update slop-publish to v1.0.6"
```

### Option 2: Copy Source

Download or clone the repository and copy it into your project:

```bash
git clone https://github.com/ClankerGuru/slop-publish.git plugins/slop-publish
rm -rf plugins/slop-publish/.git  # Remove git history if not using submodule
```

Then add the same `project.yaml` configuration as above.

### Option 3: Download Script

Create `scripts/fetch-plugin.sh`:
```bash
#!/bin/bash
VERSION="${1:-1.0.5}"
mkdir -p plugins
curl -sL "https://github.com/ClankerGuru/slop-publish/archive/refs/tags/v${VERSION}.tar.gz" | \
  tar -xz -C plugins
mv plugins/slop-publish-${VERSION} plugins/slop-publish
```

Run: `./scripts/fetch-plugin.sh 1.0.5`

---

### Enable in Your Module

Once the plugin is in your project, enable it in your `module.yaml`:

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

### Maven Central Portal (Recommended)

The simplest way to publish to Maven Central:

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
    artifacts:
      - jar
      - sources   # Auto-generated from source files
      - javadoc   # Auto-generated (empty placeholder)
    
    repositories:
      - id: central
        type: central   # Uses Maven Central Portal bundle API
        url: ""         # Uses default Central Portal URL
    
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
      scm:
        url: https://github.com/owner/repo
        connection: scm:git:git://github.com/owner/repo.git
        developerConnection: scm:git:ssh://github.com/owner/repo.git
    
    signing:
      enabled: true
      credentials:
        file: ./local.properties
```

### Repository Types

| Type | Use Case | Auth |
|------|----------|------|
| `central` | Maven Central Portal (recommended) | `CENTRAL_TOKEN` env var |
| `github` | GitHub Packages | `GITHUB_TOKEN` env var |
| `maven` | Generic Maven repos (Nexus, Artifactory) | Username/password |

### Full Configuration Example

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
    artifacts:
      - jar
      - sources
      - javadoc
    
    repositories:
      # Maven Central Portal - auto publishes after validation
      - id: central
        type: central
        url: ""
        publishingType: AUTOMATIC  # or USER_MANAGED for manual release
      
      # GitHub Packages
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repo
      
      # Generic Maven repository
      - id: nexus
        type: maven
        url: https://nexus.example.com/repository/releases
    
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

### Repository Authentication

| Env Var | Description |
|---------|-------------|
| `CENTRAL_TOKEN` | Maven Central Portal token (from https://central.sonatype.com/account) |
| `GITHUB_TOKEN` | GitHub token for GitHub Packages |

### Signing Credentials

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
| `GPG_KEY_ID` | Your GPG key ID (e.g., `65C9CBAA`) |
| `GPG_SECRET_KEY` | Contents of your armored private key |
| `GPG_PASSPHRASE` | Your GPG passphrase |
| `CENTRAL_TOKEN` | Maven Central Portal token |

Use in workflow:
```yaml
- name: Publish to Maven Central
  env:
    GPG_KEY_ID: ${{ secrets.GPG_KEY_ID }}
    GPG_SECRET_KEY: ${{ secrets.GPG_SECRET_KEY }}
    GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
    CENTRAL_TOKEN: ${{ secrets.CENTRAL_TOKEN }}
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
