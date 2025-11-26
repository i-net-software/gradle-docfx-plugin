package de.inetsoftware.docfx

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

        execOps.exec { execSpec ->
            def executableInfo = getExecutableInfo()
            execSpec.executable = executableInfo.executable
            execSpec.args = executableInfo.args + args
            // Set environment variables from extension
            Map<String, String> envVars = extension.environmentVariables
            if (!envVars.isEmpty()) {
                execSpec.environment(envVars)
            }
        }
    }

    private void doBuild() {
        List<String> args = []
        args.add("build")
        args.add(extension.source)

        execOps.exec { execSpec ->
            def executableInfo = getExecutableInfo()
            execSpec.executable = executableInfo.executable
            execSpec.args = executableInfo.args + args
            // Set environment variables from extension
            Map<String, String> envVars = extension.environmentVariables
            if (!envVars.isEmpty()) {
                execSpec.environment(envVars)
            }
        }
    }
    
    /**
     * Get executable information for DocFX.
     * On Linux/macOS, if docfx.dll exists, use 'dotnet docfx.dll', otherwise use the executable directly.
     */
    private Map<String, Object> getExecutableInfo() {
        def executable = extension.docsExecutable
        def args = []
        
        // On non-Windows, check if we need to use 'dotnet docfx.dll'
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            def executableFile = project.file(executable)
            if (!executableFile.exists() || !executableFile.canExecute()) {
                // Try docfx.dll instead
                def docfxDll = new File(executableFile.parentFile, "docfx.dll")
                if (docfxDll.exists()) {
                    executable = "dotnet"
                    args.add(docfxDll.absolutePath)
                    LOGGER.quiet("Using 'dotnet docfx.dll' for DocFX execution")
                }
            }
        }
        
        return [executable: executable, args: args]
    }
}

