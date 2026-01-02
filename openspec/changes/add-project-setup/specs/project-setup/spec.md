# Project Setup Specification

## ADDED Requirements

### Requirement: Amper Build Configuration
The project SHALL use Amper with module.yaml for build configuration, targeting JVM 17+.

#### Scenario: Successful build
- **WHEN** running `./amper build`
- **THEN** the project compiles without errors
- **AND** all dependencies are resolved

#### Scenario: JVM target verification
- **WHEN** inspecting compiled bytecode
- **THEN** the bytecode version corresponds to JVM 17

#### Scenario: Successful test run
- **WHEN** running `./amper test`
- **THEN** all tests execute
- **AND** architecture tests are included

### Requirement: Layered Package Structure
The project SHALL organize code into distinct packages following layered architecture principles.

#### Scenario: Package structure exists
- **WHEN** inspecting the source tree under `src/guru/clanker/amper/publish/`
- **THEN** packages exist for: api, domain/model, domain/service, config, maven/resolver, maven/pom, maven/signing, infrastructure, plugin

#### Scenario: Domain layer isolation
- **WHEN** analyzing domain package imports
- **THEN** no imports from infrastructure, maven, or config packages are present

### Requirement: Architecture Test Enforcement
The project SHALL include Konsist tests that enforce architecture rules and fail the build on violations.

#### Scenario: Domain layer purity test
- **WHEN** running architecture tests via `./amper test`
- **THEN** the test verifies domain classes do not import Maven, Ktor, or Bouncy Castle classes
- **AND** the build fails if violations are detected

#### Scenario: KDoc requirement test
- **WHEN** running architecture tests
- **THEN** the test verifies all public classes have KDoc documentation
- **AND** the build fails if undocumented public classes exist

#### Scenario: Test naming convention
- **WHEN** running architecture tests
- **THEN** the test verifies all test classes end with "Test" suffix

### Requirement: Dependency Management
The project SHALL declare dependencies following the dependency policy: JetBrains/kotlinx libraries, Apache Maven libraries, and Bouncy Castle (exception for signing).

#### Scenario: No forbidden dependencies
- **WHEN** inspecting declared dependencies in module.yaml
- **THEN** no OkHttp, Apache HttpClient, Jackson, or Gson dependencies are present

#### Scenario: Required dependencies present
- **WHEN** inspecting declared dependencies in module.yaml
- **THEN** Maven Resolver, Ktor Client, kotlinx.serialization, kaml, and Bouncy Castle dependencies are declared

### Requirement: CI/CD Pipeline
The project SHALL include GitHub Actions workflow for continuous integration.

#### Scenario: CI runs on push
- **WHEN** code is pushed to any branch
- **THEN** the CI workflow triggers
- **AND** runs `./amper build` and `./amper test`

#### Scenario: CI runs on pull request
- **WHEN** a pull request is opened
- **THEN** the CI workflow triggers
- **AND** reports status to the PR

### Requirement: Development Documentation
The project SHALL include README with development setup instructions.

#### Scenario: README contains setup instructions
- **WHEN** a developer reads README.md
- **THEN** they find instructions for: cloning, building with `./amper build`, running tests with `./amper test`, and project structure overview
