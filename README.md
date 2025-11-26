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

### Configuration

The `docfx` extension supports the following properties:

- **`source`** (String) - Path to the `docfx.json` configuration file (required)
- **`docsHome`** (String) - Path to DocFX installation directory (optional, defaults to `DOCFX_HOME` environment variable)

### Example

```groovy
docfx {
    source = 'docfx.json'
    docsHome = '/opt/docfx'  // Optional
}

// The docs task will:
// 1. Run 'info' to check DocFX version
// 2. Run 'metadata' to extract API documentation
// 3. Run 'build' to generate the final documentation site
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

