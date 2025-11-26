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
        }
    }

    private void doBuild() {
        List<String> args = []
        args.add("build")
        args.add(extension.source)

        execOps.exec { execSpec ->
            execSpec.executable = extension.docsExecutable
            execSpec.args = args
        }
    }
}

