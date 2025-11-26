package de.inetsoftware.docfx

import org.apache.tools.ant.taskdefs.condition.Os

import java.nio.file.Path
import java.nio.file.Paths

class DocfxExtension {
    String docsHome
    String source

    DocfxExtension() {
        docsHome = System.getenv("DOCFX_HOME")
    }

    String getDocsExecutable() {
        String executable = "docfx"
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            executable += ".exe"
        }

        if (docsHome == null) {
            return executable
        }

        Path path = Paths.get(docsHome, executable)
        return path.toString()
    }
}

