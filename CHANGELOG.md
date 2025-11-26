# gradle-docfx-plugin changelog

## 0.0.7

### Changed
* Converted plugin from Java to Groovy for consistency with other i-net software plugins
* Changed package structure from `net.karlmartens.docfx` to `de.inetsoftware.docfx`
* Updated group ID from `net.karlmartens.docfx` to `de.inetsoftware.gradle`
* Updated plugin ID from `net.karlmartens.docfx` to `de.inetsoftware.docfx`

### Fixed
* Fixed Gradle 9 compatibility by replacing `project.exec()` with `ExecOperations` injection
* Updated Jackson dependencies from 2.9.4 to 2.15.2
* Modernized build configuration to use `java-gradle-plugin` and `com.gradle.plugin-publish` 1.2.1

### Added
* Added Gradle wrapper for consistent builds
* Added GitHub Actions CI workflow for multi-version testing (Gradle 8.3, 8.14.3, 9.2.1)
* Added GitHub Actions release workflow for automated publishing
* Added Maven Central publishing configuration (Sonatype)
* Added artifact signing configuration
* Added comprehensive README with migration guide
* Added CHANGELOG.md and RELEASE.md documentation

## 0.0.6 (Original)

Initial release by Karl Martens.

