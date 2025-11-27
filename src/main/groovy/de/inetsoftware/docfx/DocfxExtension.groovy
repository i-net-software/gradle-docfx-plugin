package de.inetsoftware.docfx

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

import java.nio.file.Path
import java.nio.file.Paths

class DocfxExtension {
    String docsHome
    String source
    String locale
    String filter = null  // Optional: path to filter config file (e.g., "filterConfig.yml"). If null or empty, no filter is applied.
    String title = "API Documentation"
    Map<String, String> environment = [:]
    Closure additionalResources = null
    
    // DocFX configuration options for auto-generated docfx.json
    String template = "statictoc"
    String markdownEngine = "markdig"
    String xrefService = "https://xref.docs.microsoft.com/query?uid={uid}"
    String appFooter = null  // If null, will be auto-generated if project has productVersion
    String outputDir = "_site"
    String metadataDest = "obj/api"
    String contentDest = "api"
    
    // Company information for toc.yml (optional, will use project properties if available)
    String companyName = null
    String companyUrl = null
    
    // Project reference for accessing project properties (set by plugin)
    Project project = null

    DocfxExtension() {
        docsHome = System.getenv("DOCFX_HOME")
    }

    /**
     * Check if DocFX is natively supported on the current platform.
     * Returns true if 'docfx' command is available in PATH or ~/.dotnet/tools (installed via 'dotnet tool install -g docfx').
     * This method can be called from Gradle scripts to conditionally download DocFX zip if needed.
     * 
     * Usage in Gradle script:
     * <pre>
     * if (!de.inetsoftware.docfx.DocfxExtension.isDocfxNativelySupported()) {
     *     // Download and extract DocFX zip
     * }
     * </pre>
     * 
     * @return true if 'docfx' is available in PATH or ~/.dotnet/tools, false otherwise
     */
    static boolean isDocfxNativelySupported() {
        // First try PATH
        try {
            def process = new ProcessBuilder("docfx", "--version")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
            def exitCode = process.waitFor()
            if (exitCode == 0) {
                return true
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
                return exitCode == 0
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return false
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
    
    /**
     * Check if source is a JSON file (docfx.json)
     */
    boolean isSourceJsonFile() {
        if (source == null || source.isEmpty()) {
            return false
        }
        return source.toLowerCase().endsWith('.json')
    }
    
    /**
     * Get the locale value for docfx.json globalMetadata.
     * Returns the locale property if set, otherwise defaults to 'de-DE' or project property.
     */
    String getLocaleForMetadata() {
        if (locale != null && !locale.isEmpty()) {
            return locale
        }
        if (project != null && project.hasProperty('docFxLocale')) {
            return project.docFxLocale
        }
        return 'de-DE'
    }
    
    /**
     * Get the app footer for docfx.json globalMetadata.
     * Returns custom footer if set, otherwise generates default from project properties.
     */
    String getAppFooterForMetadata() {
        if (appFooter != null && !appFooter.isEmpty()) {
            return appFooter
        }
        if (project != null && project.hasProperty('productVersion')) {
            def pv = project.productVersion
            def startYear = pv.hasProperty('CopyrightStart') ? pv.CopyrightStart : ''
            def currentYear = pv.hasProperty('CurrentYear') ? pv.CurrentYear : ''
            
            // Get company name dynamically - only include if available
            def footerCompanyName = companyName
            if (!footerCompanyName && pv.hasProperty('CompanyName')) {
                footerCompanyName = pv.CompanyName
            }
            
            // Only generate footer if we have company name
            if (footerCompanyName && !footerCompanyName.isEmpty()) {
                return "<span>Copyright &copy; ${startYear}-${currentYear} ${footerCompanyName}</span>"
            }
        }
        return ""
    }
}

