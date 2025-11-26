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

