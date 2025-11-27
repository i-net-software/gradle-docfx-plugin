package de.inetsoftware.docfx

import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

class Info extends DocfxDefaultTask {

    private final ExecOperations execOps

    @Inject
    Info(ExecOperations execOps) {
        this.execOps = execOps
    }

    @TaskAction
    void exec() {
        List<String> args = []
        args.add("--version")

        try {
            execOps.exec { execSpec ->
                execSpec.executable = extension.docsExecutable
                execSpec.args = args
                // Set environment variables from extension
                Map<String, String> envVars = extension.environmentVariables
                if (!envVars.isEmpty()) {
                    execSpec.environment(envVars)
                }
                // Ignore errors - this is just an info task
                execSpec.ignoreExitValue = true
            }
        } catch (Exception e) {
            // If docfx is not available, just log a warning and continue
            // The build script will handle downloading/installing docfx if needed
            project.logger.warn("docFxInfo: Could not execute docfx --version: ${e.message}")
            project.logger.info("docFxInfo: This is normal if docfx is not yet installed. The build script will handle installation if needed.")
        }
    }
}

