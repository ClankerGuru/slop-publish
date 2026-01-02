# Design: Project Setup & Architecture Foundation

## Context
slop-publish is a greenfield Amper plugin for Maven publishing. The architecture must enforce clean separation between domain logic and infrastructure concerns to maintain testability and allow future extension to new repository types.

## Goals
- Establish consistent project structure from day one
- Enforce architecture rules automatically via Konsist
- Enable parallel development of different layers
- Minimize friction for new contributors

## Non-Goals
- Runtime plugin loading mechanism (later phase)
- Multi-module project setup (single module sufficient initially)

## Decisions

### Decision 1: Amper Build System
**What**: Use Amper with module.yaml for declarative build configuration.
**Why**: 
- Declarative YAML-based configuration aligns with plugin's config parsing approach
- JetBrains-supported tool for Kotlin projects
- Simpler dependency declaration than Gradle DSL
**Note**: Since Amper lacks built-in Maven publishing (which is what this plugin provides), 
we use manual Maven CLI deployment for publishing the plugin itself.

### Decision 2: Layered Package Architecture
**What**: Strict package separation: api → domain ← (config, maven, infrastructure) → plugin
**Why**: 
- Domain layer contains pure business logic with no external dependencies
- Infrastructure layer can be swapped (e.g., different HTTP clients)
- Easier to test domain logic in isolation

**Package Dependency Rules**:
```
api/           → domain/
domain/        → (nothing external)
config/        → domain/
maven/         → domain/, (Maven libraries)
infrastructure/→ domain/, maven/, (Ktor, Bouncy Castle)
plugin/        → all packages
```

### Decision 3: Konsist for Architecture Enforcement
**What**: Use Konsist library to write architecture tests as unit tests.
**Why**:
- Runs as part of normal test suite
- Fails build on architecture violations
- Self-documenting rules
- Active maintenance and good Kotlin support
**Note**: Konsist requires `scopeFromDirectory()` for Amper projects since there are no 
Gradle markers. Tests must check imports at file level, not class level.

**Alternatives**: 
- ArchUnit - Java-focused, less Kotlin-idiomatic
- Manual code review - doesn't scale, easily forgotten

### Decision 4: Dependency Versioning Strategy
**What**: Use libs.versions.toml version catalog for dependency management.
**Why**: 
- Centralized version management
- Amper supports $libs.* references natively
- Clear separation of versions from usage

## Dependency Matrix

| Library | Version | Purpose | Package Scope |
|---------|---------|---------|---------------|
| maven-core | 3.9.6 | Maven runtime | maven/ |
| maven-resolver-* | 1.9.18 | Artifact deployment | maven/resolver/ |
| ktor-client-* | 2.3.7 | HTTP client | infrastructure/ |
| kotlinx-serialization | 1.6.2 | JSON/XML | config/, maven/pom/ |
| kaml | 0.55.0 | YAML parsing | config/ |
| bcpg-jdk18on | 1.77 | PGP signing | maven/signing/ |
| konsist | 0.13.0 | Architecture tests | test only |

## Risks & Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Maven transitive dependency conflicts | Build failures | Use explicit versions in libs.versions.toml |
| Konsist API changes | Test breakage | Pin version, review changelogs before updates |
| Package structure too rigid | Refactoring pain | Start minimal, split only when needed |
| Amper plugin API not yet available | Can't integrate with Amper lifecycle | Implement as CLI tool, adapt when API available |

## Migration Plan
Not applicable - greenfield project.

## Open Questions
1. Should Konsist tests be in separate source set?
   - **Decision**: No, keep in regular test source set for simplicity.
2. How to integrate with Amper when plugin API becomes available?
   - **Decision**: Design plugin as standalone CLI initially, adapt interface later.
