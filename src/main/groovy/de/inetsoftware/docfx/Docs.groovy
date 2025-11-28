package de.inetsoftware.docfx

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

class Docs extends DocfxDefaultTask {

    private static final Logger LOGGER = Logging.getLogger(Docs.class)

    private final ExecOperations execOps

    @Inject
    Docs(ExecOperations execOps) {
        this.execOps = execOps
    }

    @TaskAction
    void exec() {
        String source = extension.source
        if (source == null || source.trim().isEmpty()) {
            LOGGER.warn("Source not set, no documentation generated.")
            return
        }

        def sourceFile = project.file(source)
        if (!sourceFile.exists()) {
            LOGGER.error("Source file does not exist: ${sourceFile.absolutePath}")
            return
        }

        // Store original source file name for zip task naming (before we update extension.source)
        // This allows the zip task to use the original file name (e.g., inetsoftware.Reporting.dll)
        // instead of the generated docfx.json name
        extension.originalSourceFileName = sourceFile.name

        // Auto-generate docfx.json if source is not a JSON file
        File docfxJsonFile
        if (!extension.isSourceJsonFile()) {
            LOGGER.quiet("Auto-generating docfx.json from source: ${sourceFile.name}")
            def generator = new DocfxJsonGenerator(extension, project)
            docfxJsonFile = generator.generateDocfxJson(sourceFile)
            
            // Update extension.source to point to generated docfx.json
            extension.source = docfxJsonFile.absolutePath
            
            // Call additionalResources closure with the docfx.json directory
            if (extension.additionalResources != null) {
                def docfxDir = docfxJsonFile.parentFile
                LOGGER.debug("Calling additionalResources closure with root: ${docfxDir}")
                extension.additionalResources.call(docfxDir)
            }
        } else {
            docfxJsonFile = sourceFile
            // Call additionalResources closure if provided (before processing)
            if (extension.additionalResources != null) {
                def sourceDir = sourceFile.parentFile
                LOGGER.debug("Calling additionalResources closure with root: ${sourceDir}")
                extension.additionalResources.call(sourceDir)
            }
        }

        LOGGER.quiet("Processing '${extension.source}'")
        doMetadata()
        doBuild()
    }

    private void doMetadata() {
        List<String> args = []
        args.add("metadata")
        args.add(extension.source)

        // Get executable info before the closure (to avoid delegate issues)
        def executableInfo = getExecutableInfo()
        Map<String, String> envVars = extension.environmentVariables

        execOps.exec { execSpec ->
            execSpec.executable = executableInfo.executable
            execSpec.args = executableInfo.args + args
            // Set working directory if specified (needed for dotnet docfx.dll)
            if (executableInfo.workingDir != null) {
                execSpec.workingDir = executableInfo.workingDir
            }
            // Set environment variables from extension
            if (!envVars.isEmpty()) {
                execSpec.environment(envVars)
            }
        }
    }

    private void doBuild() {
        List<String> args = []
        args.add("build")
        args.add(extension.source)

        // Get executable info before the closure (to avoid delegate issues)
        def executableInfo = getExecutableInfo()
        Map<String, String> envVars = extension.environmentVariables

        execOps.exec { execSpec ->
            execSpec.executable = executableInfo.executable
            execSpec.args = executableInfo.args + args
            // Set working directory if specified (needed for dotnet docfx.dll)
            if (executableInfo.workingDir != null) {
                execSpec.workingDir = executableInfo.workingDir
            }
            // Set environment variables from extension
            if (!envVars.isEmpty()) {
                execSpec.environment(envVars)
            }
        }
    }
    
    /**
     * Check if 'docfx' command is available in PATH or in ~/.dotnet/tools.
     * Returns the path to docfx if found, or null if not found.
     * This indicates it was installed via 'dotnet tool install -g docfx'.
     */
    private String findDocfxInPath() {
        // First try PATH
        try {
            def process = new ProcessBuilder("docfx", "--version")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
            def exitCode = process.waitFor()
            if (exitCode == 0) {
                return "docfx"  // Found in PATH
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Fallback: check ~/.dotnet/tools/docfx (common location for dotnet tools)
        try {
            def homeDir = System.getProperty("user.home")
            def dotnetToolsDocfx = new File(homeDir, ".dotnet/tools/docfx")
            if (dotnetToolsDocfx.exists() && dotnetToolsDocfx.canExecute()) {
                def process = new ProcessBuilder(dotnetToolsDocfx.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                def exitCode = process.waitFor()
                if (exitCode == 0) {
                    return dotnetToolsDocfx.absolutePath  // Found in ~/.dotnet/tools
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return null  // Not found
    }
    
    /**
     * Check if 'docfx' command is available in PATH or in ~/.dotnet/tools.
     * This is a convenience method that returns boolean.
     */
    private boolean isDocfxInPath() {
        return findDocfxInPath() != null
    }
    
    /**
     * Get executable information for DocFX.
     * Priority order:
     * 1. Check if 'docfx' is available in PATH (installed via 'dotnet tool install -g docfx')
     * 2. On Linux/macOS, check extracted zip directory:
     *    - Native executable (docfx)
     *    - .exe file (docfx.exe) - .NET can run .exe on Linux
     *    - .dll file (docfx.dll) with 'dotnet docfx.dll'
     * Returns a map with: executable, args, and optionally workingDir.
     */
    private Map<String, Object> getExecutableInfo() {
        def executable = extension.docsExecutable
        def args = []
        def workingDir = null
        
        // Priority 0: Check if 'docfx' is available in PATH or ~/.dotnet/tools (from 'dotnet tool install -g docfx')
        // This is the preferred method as it installs the correct platform-specific version
        // Always prefer PATH version over extracted zip, even if docsHome is set
        def docfxPath = findDocfxInPath()
        if (docfxPath != null) {
            if (docfxPath == "docfx") {
                LOGGER.quiet("Using 'docfx' from PATH (installed via 'dotnet tool install -g docfx')")
            } else {
                LOGGER.quiet("Using 'docfx' from ${docfxPath} (installed via 'dotnet tool install -g docfx')")
            }
            return [executable: docfxPath, args: args]
        }
        
        // If docsHome is null and docfx is not in PATH, try using 'docfx' anyway (might be in PATH but check failed)
        if (extension.docsHome == null) {
            LOGGER.debug("docsHome is not set and 'docfx' not found in PATH, trying 'docfx' command anyway")
            return [executable: "docfx", args: args]
        }
        
        // On non-Windows, check what executable format is available in docsHome
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            def executableFile = project.file(executable)
            def docfxDir = executableFile.parentFile
            def docfxExe = new File(docfxDir, "docfx.exe")
            def docfxDll = new File(docfxDir, "docfx.dll")
            def depsJson = new File(docfxDir, "docfx.deps.json")
            def runtimeConfig = new File(docfxDir, "docfx.runtimeconfig.json")
            
            // Priority 1: Native executable (docfx)
            if (executableFile.exists() && executableFile.canExecute()) {
                // Use it directly
                return [executable: executable, args: args]
            }
            
            // Check deps.json to see which file it expects
            String expectedFile = null
            if (depsJson.exists()) {
                try {
                    def depsContent = depsJson.text
                    if (depsContent.contains('"docfx.dll"')) {
                        expectedFile = "docfx.dll"
                    } else if (depsContent.contains('"docfx.exe"')) {
                        expectedFile = "docfx.exe"
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not parse deps.json: ${e.message}")
                }
            }
            
            // Priority 2: Use the file specified in deps.json (if both exist, prefer what deps.json says)
            if (expectedFile == "docfx.dll" && docfxDll.exists()) {
                // Try to fix runtimeconfig.json to make it framework-dependent
                if (runtimeConfig.exists()) {
                    fixRuntimeConfig(runtimeConfig)
                }
                executable = "dotnet"
                args.add("docfx.dll")
                workingDir = docfxDir
                LOGGER.quiet("Using 'dotnet docfx.dll' for DocFX execution from ${workingDir} (as specified in deps.json)")
            }
            else if (expectedFile == "docfx.exe" && docfxExe.exists() && docfxExe.canExecute()) {
                // Try to fix runtimeconfig.json to make it framework-dependent
                if (runtimeConfig.exists()) {
                    fixRuntimeConfig(runtimeConfig)
                }
                executable = "dotnet"
                args.add("docfx.exe")
                workingDir = docfxDir
                LOGGER.quiet("Using 'dotnet docfx.exe' for DocFX execution from ${workingDir} (as specified in deps.json)")
            }
            // Priority 3: Fallback to .dll if it exists
            else if (docfxDll.exists()) {
                // Try to fix runtimeconfig.json to make it framework-dependent
                if (runtimeConfig.exists()) {
                    fixRuntimeConfig(runtimeConfig)
                }
                executable = "dotnet"
                args.add("docfx.dll")
                workingDir = docfxDir
                LOGGER.quiet("Using 'dotnet docfx.dll' for DocFX execution from ${workingDir}")
            }
            // Priority 4: Fallback to .exe if it exists
            else if (docfxExe.exists() && docfxExe.canExecute()) {
                // Try to fix runtimeconfig.json to make it framework-dependent
                if (runtimeConfig.exists()) {
                    fixRuntimeConfig(runtimeConfig)
                }
                executable = "dotnet"
                args.add("docfx.exe")
                workingDir = docfxDir
                LOGGER.quiet("Using 'dotnet docfx.exe' for DocFX execution from ${workingDir}")
            }
        }
        
        def result = [executable: executable, args: args]
        if (workingDir != null) {
            result.workingDir = workingDir
        }
        return result
    }
    
    /**
     * Detect available .NET Core runtime version.
     * Returns the highest available version, or "6.0.0" as a safe minimum.
     */
    private String detectDotNetFrameworkVersion() {
        try {
            // Try to run 'dotnet --list-runtimes' to detect available versions
            def process = new ProcessBuilder("dotnet", "--list-runtimes")
                .redirectErrorStream(true)
                .start()
            
            def output = new StringBuilder()
            process.inputStream.eachLine { line ->
                output.append(line).append("\n")
            }
            process.waitFor()
            
            if (process.exitValue() == 0) {
                // Parse output to find Microsoft.NETCore.App versions
                def lines = output.toString().split("\n")
                def versions = []
                lines.each { line ->
                    if (line.contains("Microsoft.NETCore.App")) {
                        // Extract version (format: "Microsoft.NETCore.App 6.0.36 [/usr/lib/dotnet/shared/Microsoft.NETCore.App]")
                        def matcher = line =~ /Microsoft\.NETCore\.App\s+([\d.]+)/
                        if (matcher) {
                            versions.add(matcher[0][1])
                        }
                    }
                }
                
                if (!versions.isEmpty()) {
                    // Sort and return the highest version
                    versions.sort { a, b ->
                        def aParts = a.split("\\.").collect { it as Integer }
                        def bParts = b.split("\\.").collect { it as Integer }
                        for (int i = 0; i < Math.max(aParts.size(), bParts.size()); i++) {
                            def aVal = i < aParts.size() ? aParts[i] : 0
                            def bVal = i < bParts.size() ? bParts[i] : 0
                            if (aVal != bVal) {
                                return aVal <=> bVal
                            }
                        }
                        return 0
                    }
                    def highestVersion = versions.last()
                    LOGGER.debug("Detected .NET runtime version: ${highestVersion}")
                    // Return major.minor.0 format (e.g., 6.0.0, 7.0.0)
                    def parts = highestVersion.split("\\.")
                    return "${parts[0]}.${parts[1]}.0"
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not detect .NET runtime version: ${e.message}")
        }
        
        // Fallback to 6.0.0 as a safe minimum (widely available)
        return "6.0.0"
    }
    
    /**
     * Fix docfx.runtimeconfig.json to make it framework-dependent instead of self-contained.
     * This allows dotnet to use the installed .NET runtime instead of requiring all runtime files.
     */
    private void fixRuntimeConfig(File runtimeConfigFile) {
        try {
            def content = runtimeConfigFile.text
            // Parse the JSON
            def json = new groovy.json.JsonSlurper().parseText(content)
            
            // Ensure runtimeOptions exists
            if (json.runtimeOptions == null) {
                json.runtimeOptions = [:]
            }
            
            // Always remove includedFrameworks if it exists (it conflicts with framework/frameworks)
            // Build a new map without includedFrameworks
            def newRuntimeOptions = [:]
            json.runtimeOptions.each { key, value ->
                if (key != 'includedFrameworks') {
                    newRuntimeOptions[key] = value
                }
            }
            
            // Check if framework already exists (keep it if it does)
            boolean hasFramework = newRuntimeOptions.framework != null || newRuntimeOptions.frameworks != null
            
            // Add framework if it doesn't exist
            if (!hasFramework) {
                // Try to detect available .NET runtime version, or use 6.0.0 as minimum
                def frameworkVersion = detectDotNetFrameworkVersion()
                newRuntimeOptions.framework = [
                    name: "Microsoft.NETCore.App",
                    version: frameworkVersion
                ]
                LOGGER.quiet("Added framework dependency to ${runtimeConfigFile.name} (version: ${frameworkVersion})")
            }
            
            // Update the json object with the cleaned runtimeOptions
            json.runtimeOptions = newRuntimeOptions
            
            // Write back the modified JSON
            def jsonBuilder = new groovy.json.JsonBuilder(json)
            runtimeConfigFile.text = jsonBuilder.toPrettyString()
            LOGGER.quiet("Modified ${runtimeConfigFile.name} to be framework-dependent (removed includedFrameworks if present)")
        } catch (Exception e) {
            LOGGER.warn("Could not modify ${runtimeConfigFile.name}: ${e.message}")
        }
    }
}

