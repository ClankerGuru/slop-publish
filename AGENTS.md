# slop-publish

**Generated:** 2026-01-02
**Commit:** c4d3c52
**Branch:** main

## OVERVIEW

Amper plugin for Maven publishing. Publishes JVM artifacts to Maven Central, GitHub Packages, and local repositories with PGP signing.

## STRUCTURE

```
slop-publish/
├── src/guru/clanker/amper/publish/
│   ├── domain/          # Pure domain models (no external deps)
│   │   ├── model/       # Coordinates, Artifact, Publication, Repository
│   │   └── service/     # PublishingService interface
│   ├── settings/        # @Configurable plugin settings (7 files)
│   ├── tasks/           # @TaskAction entry points (publish, publishLocal, validate)
│   ├── maven/           # Maven Resolver integration
│   │   ├── pom/         # POM generation & validation
│   │   ├── resolver/    # Checksum & deployment
│   │   └── signing/     # Bouncy Castle PGP
│   └── infrastructure/  # Repository publishers (Central, GitHub, Local, Maven)
├── test/                # Tests mirror src/ structure
├── openspec/            # Spec-driven development (see openspec/AGENTS.md)
├── module.yaml          # Plugin config + self-publishing settings
├── plugin.yaml          # Task registrations
├── project.yaml         # Multi-module workspace
└── libs.versions.toml   # Dependency versions
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add/modify plugin settings | `settings/PublishingSettings.kt` | Root `@Configurable` class |
| Add new task | `tasks/` + `plugin.yaml` | `@TaskAction` + YAML registration |
| Add repository type | `infrastructure/` | Implement publisher, wire in tasks |
| Modify POM generation | `maven/pom/DefaultPomGenerator.kt` | |
| Modify signing | `maven/signing/BouncyCastleSigner.kt` | |
| Domain models | `domain/model/` | Pure Kotlin, no external deps |
| Architecture rules | `test/.../architecture/ArchitectureTest.kt` | Konsist layer enforcement |
| Detailed project context | `openspec/project.md` | 470 lines of conventions |
| Spec workflow | `openspec/AGENTS.md` | Change proposal process |

## LAYER DEPENDENCIES (Enforced by Konsist)

```
domain      → nothing (pure)
settings    → domain
maven       → domain
infra       → domain, maven
tasks       → settings, domain, infra, maven
```

Violating this fails `./amper test` via ArchitectureTest.kt.

## CONVENTIONS

### Amper Plugin Patterns
- `product: jvm/amper-plugin` in module.yaml
- Settings: `@Configurable` interfaces, registered via `pluginInfo.settingsClass`
- Tasks: `@TaskAction` annotated functions, registered in `plugin.yaml`
- Invoke: `./amper task :module:taskName@slop-publish`

### Commit Prefixes
`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `arch:`

### Naming
- Interfaces: NO `I` prefix (enforced by Konsist)
- Test classes: MUST end with `Test` suffix (enforced by Konsist)
- Settings classes: in `settings/` package
- Task actions: in `tasks/` package

## ANTI-PATTERNS

| Pattern | Why |
|---------|-----|
| Domain importing infrastructure/maven/settings/tasks | Breaks layer isolation |
| Domain importing `org.apache.maven.*`, `io.ktor.*`, `org.bouncycastle.*` | Breaks purity |
| Interface starting with `I` + uppercase | Fails Konsist |
| Test class not ending with `Test` | Fails Konsist |
| Credentials in code or logs | Security violation |

## COMMANDS

```bash
./amper build                                    # Build plugin
./amper test                                     # Run tests (includes architecture)
./amper task :slop-publish:publish@slop-publish  # Publish to Maven Central
./amper task :slop-publish:publishLocal@slop-publish  # Publish to ~/.m2
./amper task :slop-publish:validate@slop-publish      # Validate config
```

## DEPENDENCIES (libs.versions.toml)

| Library | Purpose |
|---------|---------|
| Maven Resolver | Artifact deployment |
| Ktor Client | HTTP for GitHub Packages |
| kotlinx.serialization | JSON/XML |
| Bouncy Castle | PGP signing |
| Konsist | Architecture tests |
| Testcontainers | Integration tests |

**Policy**: JetBrains/kotlinx/Apache Maven only. No third-party HTTP or serialization libs.

## CI

- **ci.yml**: Build + test on push/PR to main/develop
- **publish.yml**: Publish to Maven Central on release tag or manual trigger
- Secrets: `GPG_KEY_ID`, `GPG_SECRET_KEY`, `GPG_PASSPHRASE`, `CENTRAL_TOKEN`

## NOTES

- JDK 17+ required
- Kotlin 2.0, kotlinx.serialization enabled
- Self-publishing: plugin uses itself to publish (see module.yaml `plugins:` section)
- OpenSpec: For new features/architecture changes, create proposal first (see openspec/AGENTS.md)

<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->
