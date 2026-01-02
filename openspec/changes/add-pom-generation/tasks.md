# Tasks: POM Generation Module

## 1. POM Builder DSL
- [x] 1.1 Create `PomBuilder` class
- [x] 1.2 Implement XML declaration and project root element
- [x] 1.3 Implement element builder methods (modelVersion, groupId, etc.)
- [x] 1.4 Implement nested element support (licenses, developers, scm)
- [x] 1.5 Create `LicensesBuilder` for license elements
- [x] 1.6 Create `DevelopersBuilder` for developer elements
- [x] 1.7 Create `ScmBuilder` for SCM elements
- [x] 1.8 Implement proper indentation and formatting

## 2. XML Utilities
- [x] 2.1 Implement escapeXml for special characters (&, <, >, ", ')
- [x] 2.2 Handle special characters in descriptions

## 3. DefaultPomGenerator Implementation
- [x] 3.1 Create `DefaultPomGenerator.kt` class
- [x] 3.2 Implement `generateMinimal()` for local repos (GAV only)
- [x] 3.3 Implement `generateFull()` for Maven Central (all metadata)
- [x] 3.4 Implement `generateToFile()` to write POM to path
- [x] 3.5 Add packaging element support (jar, pom)
- [x] 3.6 Handle optional fields gracefully

## 4. PomValidator Implementation
- [x] 4.1 Create `PomValidator.kt` object
- [x] 4.2 Create `PomValidationError` data class
- [x] 4.3 Implement `validateForMavenCentral()` method
- [x] 4.4 Validate required fields: name, description, url
- [x] 4.5 Validate at least one license present
- [x] 4.6 Validate at least one developer present
- [x] 4.7 Validate SCM url and connection present
- [x] 4.8 Return list of all validation errors (not fail-fast)

## 5. Documentation
- [x] 5.1 Clear code structure with DSL pattern
- [x] 5.2 Document Maven Central requirements in validator

## 6. Verification
- [x] 6.1 Verify no domain model changes required
- [x] 6.2 Build passes successfully
- [x] 6.3 Konsist tests pass
