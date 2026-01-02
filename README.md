# slop-publish

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

Add the plugin to your project's `project.yaml`:

```yaml
settings:
  plugins:
    - guru.clanker.amper:slop-publish:1.0.0
```

## Usage

Run plugin tasks via Amper:

```bash
# Publish to configured repositories
./amper task :my-module:publish@slop-publish

# Publish to local Maven repository (~/.m2/repository)
./amper task :my-module:publishLocal@slop-publish

# Validate configuration without publishing
./amper task :my-module:validate@slop-publish

# Dry run - validate and show what would be published
./amper task :my-module:publishDryRun@slop-publish
```

## Configuration

Configure publishing in your `module.yaml`:

```yaml
settings:
  slop-publish:
    repositories:
      - id: github
        type: github
        url: https://maven.pkg.github.com/owner/repo
        credentials:
          username: ${GITHUB_ACTOR}
          password: ${GITHUB_TOKEN}
      
      - id: maven-central
        type: maven
        url: https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/
        credentials:
          username: ${OSSRH_USERNAME}
          password: ${OSSRH_PASSWORD}
    
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
      keyId: ${GPG_KEY_ID}
      password: ${GPG_PASSPHRASE}
      secretKey: ${GPG_SECRET_KEY}
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
