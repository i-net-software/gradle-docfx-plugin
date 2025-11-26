package de.inetsoftware.docfx

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

        // Call additionalResources closure if provided (before processing)
        if (extension.additionalResources != null) {
            def sourceFile = project.file(source)
            def sourceDir = sourceFile.parentFile
            LOGGER.debug("Calling additionalResources closure with root: ${sourceDir}")
            extension.additionalResources.call(sourceDir)
        }

        LOGGER.quiet("Processing '${source}'")
        doMetadata()
        doBuild()
    }

    private void doMetadata() {
        List<String> args = []
        args.add("metadata")
        args.add(extension.source)

        execOps.exec { execSpec ->
            execSpec.executable = extension.docsExecutable
            execSpec.args = args
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
            execSpec.executable = extension.docsExecutable
            execSpec.args = args
            // Set environment variables from extension
            Map<String, String> envVars = extension.environmentVariables
            if (!envVars.isEmpty()) {
                execSpec.environment(envVars)
            }
        }
    }
}

