# gradle-docfx-plugin changelog

## 0.0.7

### Changed
* Converted plugin from Java to Groovy for consistency with other i-net software plugins
* Changed package structure from `net.karlmartens.docfx` to `de.inetsoftware.docfx`
* Updated group ID from `net.karlmartens.docfx` to `de.inetsoftware.gradle`
* Updated plugin ID from `net.karlmartens.docfx` to `de.inetsoftware.docfx`
* **BREAKING**: Renamed `clean` task to `docfxClean` to avoid conflict with Gradle's standard `clean` task

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
* Added `docfxZip` task that automatically packages generated documentation into a zip file
* Added `locale` property to `DocfxExtension` - automatically sets locale environment variables (LC_ALL, LANG, LC_CTYPE)
* Added `environment` map property to `DocfxExtension` - allows setting custom environment variables for DocFX process
* Added `additionalResources` closure property to `DocfxExtension` - allows copying additional files before DocFX execution
* Added `filter` and `title` properties to `DocfxExtension` for future use
* Environment variables are now automatically passed to all DocFX commands (info, metadata, build)
* **Auto-generation of docfx.json**: Plugin now automatically generates `docfx.json` configuration file when `source` points to a non-JSON file (e.g., `.dll`, `.csproj`, `.sln`). This eliminates the need for manual `docfx.json` creation in most cases.
* **Enhanced configuration options**: Added properties for customizing generated `docfx.json`:
  * `template` - DocFX template to use (default: "statictoc")
  * `markdownEngine` - Markdown engine name (default: "markdig")
  * `xrefService` - Cross-reference service URL (default: Microsoft xref service)
  * `appFooter` - Custom footer HTML (auto-generated from project properties if not set)
  * `outputDir` - Output directory for generated documentation (default: "_site")
  * `metadataDest` - Destination for metadata files (default: "obj/api")
  * `contentDest` - Destination for content files (default: "api")
* **Company information support**: Added `companyName` and `companyUrl` properties for including company information in generated `toc.yml` and footer. Falls back to `productVersion.CompanyName` and `productVersion.CompanyUrl` if available.
* **Automatic file generation**: Plugin now automatically generates `toc.yml` and `index.md` files when auto-generating `docfx.json`.

## 0.0.6 (Original)

Initial release by Karl Martens.

