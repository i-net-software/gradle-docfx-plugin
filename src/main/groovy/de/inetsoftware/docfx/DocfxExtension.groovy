package de.inetsoftware.docfx

import org.apache.tools.ant.taskdefs.condition.Os

import java.nio.file.Path
import java.nio.file.Paths

class DocfxExtension {
    String docsHome
    String source
    String locale
    String filter = ""
    String title = "API Documentation"
    Map<String, String> environment = [:]
    Closure additionalResources = null

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

    /**
     * Get environment variables to pass to DocFX process.
     * Automatically sets locale-related environment variables if locale is specified.
     */
    Map<String, String> getEnvironmentVariables() {
        Map<String, String> env = new HashMap<>(environment)
        
        // If locale is set and not already in environment, set locale-related env vars
        if (locale != null && !locale.isEmpty()) {
            if (!env.containsKey('LC_ALL') && !System.getenv('LC_ALL')) {
                env['LC_ALL'] = "${locale}.UTF-8"
            }
            if (!env.containsKey('LANG') && !System.getenv('LANG')) {
                env['LANG'] = "${locale}.UTF-8"
            }
            if (!env.containsKey('LC_CTYPE') && !System.getenv('LC_CTYPE')) {
                env['LC_CTYPE'] = "${locale}.UTF-8"
            }
        }
        
        return env
    }
}

