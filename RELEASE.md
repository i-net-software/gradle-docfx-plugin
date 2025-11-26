# Release Process

This document describes how to create a new release of the gradle-docfx-plugin.

## Prerequisites

1. Ensure all changes are committed and pushed to the repository
2. Update `CHANGELOG.md` with the new version and changes
3. Update `gradle.properties` with the new version (remove `-SNAPSHOT` if present)

## Creating a Release

### Option 1: Using Git Tags (Recommended)

1. **Update version in `gradle.properties`**:
   ```properties
   version = 0.0.7
   ```

2. **Commit the version change**:
   ```bash
   git add gradle.properties CHANGELOG.md
   git commit -m "Release version 0.0.7"
   git push
   ```

3. **Create and push a tag**:
   ```bash
   # Tag format: v0.0.7 (recommended) or 0.0.7
   git tag -a v0.0.7 -m "Release version 0.0.7"
   git push origin v0.0.7
   ```

4. **GitHub Actions will automatically**:
   - Extract the version from the tag
   - Build the plugin
   - Publish to Maven Central (Sonatype)
   - Publish to Gradle Plugin Portal
   - Create a GitHub Release with changelog

### Option 2: Manual Release

If you prefer to create the release manually:

1. **Update version and commit** (same as Option 1, steps 1-2)

2. **Build and publish locally**:
   ```bash
   ./gradlew clean build publishToSonatype closeStagingRepositories publishPlugins
   ```

3. **Create GitHub Release manually**:
   - Go to https://github.com/i-net-software/gradle-docfx-plugin/releases/new
   - Create a new tag (e.g., `v0.0.7`)
   - Copy the relevant section from `CHANGELOG.md` as the release notes
   - Publish the release

## Required GitHub Secrets

The release workflow requires the following secrets to be configured in GitHub:

- `SONATYPE_USERNAME` - Your Sonatype username
- `SONATYPE_PASSWORD` - Your Sonatype password
- `SIGNING_KEY` - Your PGP private key (for signing artifacts)
- `SIGNING_PASSWORD` - Your PGP key passphrase
- `GRADLE_PUBLISH_KEY` - Gradle Plugin Portal API key
- `GRADLE_PUBLISH_SECRET` - Gradle Plugin Portal API secret

To configure secrets:
1. Go to your repository on GitHub
2. Settings → Secrets and variables → Actions
3. Add each secret

## Version Numbering

- Use semantic versioning: `MAJOR.MINOR.PATCH`
- Examples: `0.0.7`, `0.0.8`, `0.1.0`
- Tag format: `v0.0.7` (with `v` prefix) or `0.0.7` (without prefix) - both are supported

## After Release

1. **Update version to next SNAPSHOT** (if continuing development):
   ```properties
   version = 0.0.8-SNAPSHOT
   ```
   ```bash
   git add gradle.properties
   git commit -m "Bump version to 0.0.8-SNAPSHOT"
   git push
   ```

2. **Verify the release**:
   - Check Maven Central: https://central.sonatype.com/artifact/de.inetsoftware.gradle/gradle-docfx-plugin
   - Check Gradle Plugin Portal: https://plugins.gradle.org/plugin/de.inetsoftware.docfx
   - Check GitHub Releases: https://github.com/i-net-software/gradle-docfx-plugin/releases

