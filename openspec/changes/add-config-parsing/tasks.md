# Tasks: Plugin Settings Module

## 1. @Configurable Settings Interfaces
- [x] 1.1 Create `PublishingSettings.kt` root @Configurable interface
- [x] 1.2 Create `RepositorySettings.kt` with id, type, url, credentials
- [x] 1.3 Create `CredentialsSettings.kt` @Configurable interface
- [x] 1.4 Create `PomSettings.kt` with Maven Central required fields
- [x] 1.5 Create `LicenseSettings.kt`, `DeveloperSettings.kt`, `ScmSettings.kt`
- [x] 1.6 Create `SigningSettings.kt` with key settings and skip list
- [x] 1.7 Add default values for optional properties

## 2. Settings to Domain Mapping
- [x] 2.1 Create `SettingsMapper.kt` object
- [x] 2.2 Implement `toCoordinates(settings)` method
- [x] 2.3 Implement `toRepository(settings)` method with type dispatching
- [x] 2.4 Implement `toPomMetadata(settings)` method
- [x] 2.5 Handle GitHub URL parsing for owner/repo extraction

## 3. Settings Validation
- [x] 3.1 Create `SettingsValidator.kt` object
- [x] 3.2 Validate required fields (groupId, artifactId, version)
- [x] 3.3 Validate Maven Central POM requirements
- [x] 3.4 Validate unique repository IDs
- [x] 3.5 Validate target references point to valid repository IDs

## 4. Module Configuration
- [x] 4.1 Update `module.yaml` with `pluginInfo.settingsClass`
- [x] 4.2 Reference `guru.clanker.amper.publish.settings.PublishingSettings`

## 5. Documentation
- [x] 5.1 Add KDoc to all @Configurable interfaces
- [x] 5.2 Document example YAML in KDoc comments
- [x] 5.3 Update README with configuration examples

## 6. Verification
- [x] 6.1 Build passes with new settings classes
- [x] 6.2 Konsist tests pass (settings depends only on domain)
- [x] 6.3 All 50 tests pass
