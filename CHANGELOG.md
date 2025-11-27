# gradle-docfx-plugin changelog

## 0.0.9-SNAPSHOT

### Fixed
* Fixed DocFX detection to check `~/.dotnet/tools/docfx` as fallback when `docfx` is not in PATH
* Plugin now correctly finds DocFX installed via `dotnet tool install -g docfx` even when `~/.dotnet/tools` is not in PATH
* Updated `isDocfxNativelySupported()` and `isDocfxInPath()` to check `~/.dotnet/tools/docfx` location

### Changed
* `isDocfxInPath()` now returns the actual path to docfx when found (supports both PATH and `~/.dotnet/tools`)
* Plugin uses full path to `~/.dotnet/tools/docfx` when found there, ensuring correct execution

## 0.0.8

### Added
* **Native DocFX support**: Plugin now automatically detects and uses DocFX installed via `dotnet tool install -g docfx` when available in PATH
* Added `DocfxExtension.isDocfxNativelySupported()` static method to check if DocFX is available in PATH from Gradle scripts
* Plugin prefers native DocFX installation over extracted zip files to avoid architecture compatibility issues

### Fixed
* Fixed DocFX execution on Linux/macOS by detecting and using correct executable format (docfx.exe, docfx.dll, or native docfx)
* Fixed `docfx.runtimeconfig.json` handling to remove `includedFrameworks` and add `framework` dependency for framework-dependent execution
* Added automatic .NET runtime version detection - plugin now detects installed .NET versions and uses appropriate framework version
* Fixed filter property handling - empty or null filter strings are now properly omitted from `docfx.json` (prevents FileNotFoundException)
* Improved executable detection logic to check `docfx.deps.json` to determine which file format to use

### Changed
* Default `filter` property changed from empty string `""` to `null` - empty strings are now converted to `null` to prevent DocFX from trying to load non-existent filter files
* Plugin now checks PATH for `docfx` command before falling back to `docsHome` directory
* Updated README with documentation for native DocFX installation method

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

