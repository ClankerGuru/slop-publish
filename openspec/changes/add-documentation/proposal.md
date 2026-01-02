# Change: Add Documentation & Examples

## Why
Comprehensive documentation enables effective use of the plugin. This includes user guides, API documentation, and examples.

## What Changes
- README with quick start guide
- User documentation in docs/ folder
- Configuration reference
- API documentation via KDoc
- Example projects

## Impact
- Affected specs: `documentation` (new capability)
- Affected code: docs/, examples/, README.md
- Dependencies: All implementation complete

## Technical Approach

### Documentation Structure

```
docs/
├── getting-started.md          # Quick start guide
├── configuration.md            # Configuration reference
├── repositories/
│   ├── maven-central.md        # Maven Central publishing
│   ├── github-packages.md      # GitHub Packages publishing
│   └── local-repository.md     # Local publishing
├── signing.md                  # PGP signing setup
├── ci-cd/
│   ├── github-actions.md       # GitHub Actions setup
│   └── environment-variables.md # Environment variable handling
├── troubleshooting.md          # Common issues and solutions


examples/
├── basic-library/              # Simple library publishing
│   └── module.yaml
├── multi-target/               # Publishing to multiple repos
│   └── module.yaml
├── github-actions/             # CI workflow example
│   ├── module.yaml
│   └── .github/workflows/publish.yml
└── maven-central/              # Full Maven Central setup
    └── module.yaml
```

### README.md

```markdown
# slop-publish

Amper plugin for publishing artifacts to Maven repositories and GitHub Packages.

## Features

- Publish to Maven Central, GitHub Packages, or any Maven repository
- PGP signing with Bouncy Castle
- Environment variable support for CI/CD
- Declarative configuration in module.yaml
- Amper task integration

## Quick Start

1. Add slop-publish to your project's `project.yaml`:

```yaml
plugins:
  - ./path/to/slop-publish  # or published coordinates when available
```

2. Enable and configure in your `module.yaml`:

```yaml
plugins:
  slop-publish:
    enabled: true
    groupId: com.example
    artifactId: my-library
    version: 1.0.0
    
    repositories:
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repo
        credentials:
          username: ${GITHUB_ACTOR}
          password: ${GITHUB_TOKEN}
```

3. Build and publish:
```bash
./amper build
./amper task :my-module:publish@slop-publish
```

## Available Tasks

| Task | Description |
|------|-------------|
| `publish` | Publish to default target repositories |
| `publishLocal` | Publish to ~/.m2/repository |
| `validate` | Validate configuration without publishing |
| `publishDryRun` | Show what would be published |

## Documentation

- [Getting Started](docs/getting-started.md)
- [Configuration Reference](docs/configuration.md)
- [Publishing to Maven Central](docs/repositories/maven-central.md)
- [GitHub Packages](docs/repositories/github-packages.md)
- [PGP Signing](docs/signing.md)
- [Troubleshooting](docs/troubleshooting.md)
```

### Configuration Reference (docs/configuration.md)

```markdown
# Configuration Reference

## Plugin Settings

Configure slop-publish in your `module.yaml` under the `plugins.slop-publish` section.

### Required Settings

| Field | Description |
|-------|-------------|
| groupId | Maven group ID |
| artifactId | Maven artifact ID |
| version | Version string |

### repositories

List of target repositories.

| Field | Required | Description |
|-------|----------|-------------|
| id | Yes | Unique identifier for the repository |
| type | No | Repository type: `maven` (default), `github`, `local` |
| url | Yes | Repository URL |
| credentials | No | Authentication credentials |

#### Credentials

| Field | Required | Description |
|-------|----------|-------------|
| username | Yes | Username or token name |
| password | Yes | Password or token |

### pom

POM metadata (required for Maven Central).

| Field | Required | Description |
|-------|----------|-------------|
| name | Yes | Project name |
| description | Yes | Project description |
| url | Yes | Project URL |
| licenses | Yes | List of licenses |
| developers | Yes | List of developers |
| scm | Yes | Source control info |

### signing

PGP signing configuration.

| Field | Required | Description |
|-------|----------|-------------|
| enabled | No | Enable/disable signing (default: false) |
| key | No | ASCII-armored private key (from env var) |
| password | No | Key passphrase |
| keyId | No | Key ID (if multiple keys) |
| skipForRepositories | No | List of repo IDs to skip signing |

### targets

Named groups of repositories.

```yaml
targets:
  default:
    - github
  release:
    - mavenCentral
  all:
    - github
    - mavenCentral

defaultTarget: default
```
```

### GitHub Actions Example

```yaml
# .github/workflows/publish.yml
name: Publish

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Build
        run: ./amper build
      
      - name: Publish to GitHub Packages
        run: ./amper task :my-lib:publish@slop-publish
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Publish to Maven Central
        run: ./amper task :my-lib:publish@slop-publish
        env:
          SLOP_PUBLISH_TARGET: release
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
          GPG_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

## Acceptance Criteria
- [ ] README with quick start
- [ ] Configuration reference documentation
- [ ] Repository-specific guides (Maven Central, GitHub)
- [ ] Signing setup documentation
- [ ] CI/CD examples (GitHub Actions)
- [ ] Troubleshooting guide
- [ ] KDoc on all public APIs

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Documentation gets stale | Link docs to code, test examples in CI |
| Examples don't work | Run example projects in CI |
| Unclear error messages | Collect user feedback, improve based on issues |

## Estimated Effort
**Size: M** (Medium) - 2-3 days
