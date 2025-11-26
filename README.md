# Gradle DocFX Plugin

> **Note:** This is a fork of the original [gradle-docfx-plugin](https://github.com/karlmartens/docfx-plugin) by Karl Martens, updated for **Gradle 8/9 compatibility** and published under the `de.inetsoftware` group ID.

> **Disclaimer:** Most of the changes in this fork, including Gradle 8/9 compatibility fixes, Java to Groovy conversion, and CI/CD pipeline improvements, were created with the assistance of Cursor AI. While the code has been tested and verified, please review changes carefully before using in production environments.

## Fork Information

This fork maintains **forward compatibility** with the original plugin while providing:
- **Gradle 8/9 compatibility** - Fixed deprecated API usage and compatibility issues
- **Groovy implementation** - Converted from Java to Groovy for consistency with other i-net software plugins
- **Updated group ID** - Published as `de.inetsoftware.gradle:gradle-docfx-plugin`
- **Updated plugin IDs** - Use `de.inetsoftware.docfx` instead of `net.karlmartens.docfx`
- **Modern CI/CD pipeline** - GitHub Actions workflows for automated testing and releases
- **All original functionality preserved** - Drop-in replacement for the original plugin

### Migration from Original Plugin

If you're using the original `net.karlmartens.docfx:gradle-docfx-plugin`, you can migrate to this fork by:

1. **Update your buildscript dependency:**
   ```groovy
   buildscript {
       dependencies {
           classpath 'de.inetsoftware.gradle:gradle-docfx-plugin:0.0.7'
       }
   }
   ```

2. **Update plugin application:**
   ```groovy
   apply plugin: 'de.inetsoftware.docfx'
   // or
   plugins {
       id 'de.inetsoftware.docfx' version '0.0.7'
   }
   ```

All task names and configuration remain the same - only the plugin ID and group ID have changed.

---

## Overview

This plugin allows you to generate documentation using [DocFX](https://dotnet.github.io/docfx/) from a Gradle build.

## Usage

### Basic Setup

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'de.inetsoftware.gradle:gradle-docfx-plugin:0.0.7'
    }
}

apply plugin: 'de.inetsoftware.docfx'

docfx {
    source = 'docfx.json'
    docsHome = '/path/to/docfx'  // Optional: defaults to DOCFX_HOME environment variable
}
```

### Using the Plugin DSL

```groovy
plugins {
    id 'de.inetsoftware.docfx' version '0.0.7'
}

docfx {
    source = 'docfx.json'
}
```

### Tasks

The plugin provides the following tasks:

- **`info`** - Displays DocFX version information
- **`clean`** - Cleans generated documentation files (metadata and build output)
- **`docs`** - Generates documentation (runs `info` first, then `metadata` and `build`)
- **`docfxZip`** - Packages the generated documentation into a zip file (automatically runs after `docs`)

### Configuration

The `docfx` extension supports the following properties:

- **`source`** (String) - Path to the `docfx.json` configuration file or source file (e.g., `.dll`, `.csproj`). If not a `.json` file, the plugin will auto-generate `docfx.json` (required)
- **`docsHome`** (String) - Path to DocFX installation directory (optional, defaults to `DOCFX_HOME` environment variable)
- **`locale`** (String) - Locale for DocFX execution (e.g., "de-DE"). Automatically sets `LC_ALL`, `LANG`, and `LC_CTYPE` environment variables
- **`filter`** (String) - Filter string for DocFX metadata generation (optional, default: empty string)
- **`title`** (String) - Title for documentation (optional, default: "API Documentation")
- **`environment`** (Map<String, String>) - Additional environment variables to pass to DocFX process
- **`additionalResources`** (Closure) - Closure called before DocFX execution to add additional resources. Receives the docfx.json parent directory as parameter
- **`template`** (String) - DocFX template to use (optional, default: "statictoc")
- **`markdownEngine`** (String) - Markdown engine name (optional, default: "markdig")
- **`xrefService`** (String) - Cross-reference service URL (optional, default: Microsoft xref service)
- **`appFooter`** (String) - Custom footer HTML for documentation (optional, auto-generated from project properties if not set)
- **`outputDir`** (String) - Output directory for generated documentation (optional, default: "_site")
- **`metadataDest`** (String) - Destination directory for metadata files (optional, default: "obj/api")
- **`contentDest`** (String) - Destination directory for content files (optional, default: "api")
- **`companyName`** (String) - Company name for toc.yml and footer (optional, falls back to `productVersion.CompanyName` if available)
- **`companyUrl`** (String) - Company URL for toc.yml (optional, falls back to `productVersion.CompanyUrl` if available)

### Examples

#### Basic Usage

```groovy
docfx {
    source = 'docfx.json'  // Use existing docfx.json
    docsHome = '/opt/docfx'  // Optional
}

// The docs task will:
// 1. Run 'info' to check DocFX version
// 2. Run 'metadata' to extract API documentation
// 3. Run 'build' to generate the final documentation site
// The docfxZip task will automatically package the output
```

#### Auto-Generate docfx.json from Source File

```groovy
docfx {
    source = 'path/to/MyAssembly.dll'  // Plugin will auto-generate docfx.json
    title = 'My API Documentation'
    locale = 'en-US'
    companyName = 'My Company'
    companyUrl = 'https://www.example.com'
}

// The plugin automatically:
// 1. Detects that source is not a .json file
// 2. Generates docfx.json with sensible defaults
// 3. Generates toc.yml and index.md
// 4. Then proceeds with normal DocFX execution
```

#### With Locale and Environment Variables

```groovy
docfx {
    source = 'docfx.json'
    locale = 'de-DE'  // Automatically sets LC_ALL, LANG, LC_CTYPE
    environment = [
        'DOCFX_CUSTOM_VAR': 'value'
    ]
}
```

#### With Additional Resources

```groovy
docfx {
    source = 'docfx.json'
    additionalResources = { root ->
        // Copy additional files into the docfx working directory
        copy {
            into root
            from 'readme.md'
            rename 'readme.md', 'index.md'
        }
        copy {
            into root
            from 'logo.svg'
        }
    }
}
```

## Requirements

- **DocFX** - Must be installed and available in PATH, or set `DOCFX_HOME` environment variable
- **Gradle 8.3+** or **Gradle 9.x**
- **Java 17+**

## See also

- [DocFX Documentation](https://dotnet.github.io/docfx/)
- [Gradle MSBuild plugin](https://github.com/i-net-software/gradle-msbuild-plugin) - Build .NET projects before generating documentation
- [Gradle NuGet plugin](https://github.com/i-net-software/gradle-nuget-plugin) - Restore NuGet packages

## Original Plugin

This is a fork of the original [gradle-docfx-plugin](https://github.com/karlmartens/docfx-plugin) by Karl Martens.

## License

This plugin is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) with no warranty (expressed or implied) for any purpose.

