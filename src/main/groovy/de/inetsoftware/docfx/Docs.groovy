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
     * Get executable information for DocFX.
     * On Linux/macOS, if docfx.dll exists, use 'dotnet docfx.dll', otherwise use the executable directly.
     * Returns a map with: executable, args, and optionally workingDir.
     */
    private Map<String, Object> getExecutableInfo() {
        def executable = extension.docsExecutable
        def args = []
        def workingDir = null
        
        // On non-Windows, check if we need to use 'dotnet docfx.dll'
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            def executableFile = project.file(executable)
            def docfxDll = new File(executableFile.parentFile, "docfx.dll")
            def runtimeConfig = new File(executableFile.parentFile, "docfx.runtimeconfig.json")
            
            // If executable doesn't exist or isn't executable, try docfx.dll
            if (!executableFile.exists() || !executableFile.canExecute()) {
                if (docfxDll.exists()) {
                    // Try to fix runtimeconfig.json to make it framework-dependent
                    if (runtimeConfig.exists()) {
                        fixRuntimeConfig(runtimeConfig)
                    }
                    executable = "dotnet"
                    // Use relative path and set working directory so dotnet can find runtime dependencies
                    args.add("docfx.dll")
                    workingDir = docfxDll.parentFile
                    LOGGER.quiet("Using 'dotnet docfx.dll' for DocFX execution from ${workingDir}")
                }
            }
            // If executable exists and is executable, use it directly (don't second-guess)
        }
        
        def result = [executable: executable, args: args]
        if (workingDir != null) {
            result.workingDir = workingDir
        }
        return result
    }
    
    /**
     * Fix docfx.runtimeconfig.json to make it framework-dependent instead of self-contained.
     * This allows dotnet to use the installed .NET runtime instead of requiring all runtime files.
     */
    private void fixRuntimeConfig(File runtimeConfigFile) {
        try {
            def content = runtimeConfigFile.text
            // Check if it already has a framework specified
            if (!content.contains('"framework"') && !content.contains('"frameworks"')) {
                // Parse and modify the JSON to add framework dependency
                def json = new groovy.json.JsonSlurper().parseText(content)
                if (json.runtimeOptions == null) {
                    json.runtimeOptions = [:]
                }
                if (json.runtimeOptions.framework == null) {
                    // Add a framework dependency - try to use a common one
                    // DocFX typically targets .NET Core 2.1 or later
                    json.runtimeOptions.framework = [
                        name: "Microsoft.NETCore.App",
                        version: "2.1.0"  // Minimum version, will use latest installed
                    ]
                    // Write back the modified JSON
                    def jsonBuilder = new groovy.json.JsonBuilder(json)
                    runtimeConfigFile.text = jsonBuilder.toPrettyString()
                    LOGGER.debug("Modified ${runtimeConfigFile.name} to be framework-dependent")
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not modify ${runtimeConfigFile.name}: ${e.message}")
        }
    }
}

