# Tasks: Plugin Tasks & Entry Points

## 1. @TaskAction Functions
- [x] 1.1 Create `PublishTask.kt` with @TaskAction publish()
- [x] 1.2 Create `PublishLocalTask.kt` with @TaskAction publishLocal()
- [x] 1.3 Create `ValidateTask.kt` with @TaskAction validate()
- [x] 1.4 Implement @Input annotations for moduleDir, buildDir
- [x] 1.5 Wire settings parameter from ${pluginSettings}

## 2. plugin.yaml Task Registration
- [x] 2.1 Create `plugin.yaml` at project root
- [x] 2.2 Register `publish` task with action reference
- [x] 2.3 Register `publishLocal` task with action reference
- [x] 2.4 Register `validate` task with action reference
- [x] 2.5 Register `publishDryRun` task with dryRun=true

## 3. Artifact Collection
- [x] 3.1 Implement artifact location from ${module.buildDir}
- [x] 3.2 Locate JAR at build/tasks/_<module>_jarJvm/<module>-jvm.jar
- [x] 3.3 Locate sources JAR if present
- [x] 3.4 Locate javadoc JAR if present
- [x] 3.5 Handle missing artifacts gracefully

## 4. Publishing Pipeline
- [x] 4.1 Validate settings before publish
- [x] 4.2 Resolve target to repository IDs
- [x] 4.3 Create Publication from settings
- [x] 4.4 Generate POM artifact
- [x] 4.5 Sign publication if signing enabled
- [x] 4.6 Select appropriate publisher for repository type
- [x] 4.7 Handle results (success/failure)

## 5. Dry Run Support
- [x] 5.1 Create `DryRunPublisher.kt` decorator
- [x] 5.2 Log actions with [DRY RUN] prefix
- [x] 5.3 Return simulated success results

## 6. Documentation
- [x] 6.1 Add KDoc to @TaskAction functions
- [x] 6.2 Document usage in README
- [x] 6.3 Add usage examples in proposal

## 7. Verification
- [x] 7.1 Build passes with task actions
- [x] 7.2 Konsist tests pass (tasks depends on settings, domain, infra, maven)
- [x] 7.3 All 50 tests pass
