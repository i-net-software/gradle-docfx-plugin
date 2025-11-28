package de.inetsoftware.docfx

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

class DocFxPlugin implements Plugin<Project> {

    private static final String DOCFX_CONFIG_EXTENSION  = "docFxConfig"
    private static final String INFO_TASK               = "docFxInfo"
    private static final String CLEAN_TASK              = "docFxClean"
    private static final String DOCS_TASK               = "docFx"
    private static final String DOCFX_ZIP_TASK          = "docFxZip"

    @Override
    void apply(Project project) {
        // Use 'docfxConfig' as extension name to avoid conflict with task names
        final DocfxExtension extension = project.extensions.create(DOCFX_CONFIG_EXTENSION, DocfxExtension)
        // Set project reference for accessing project properties
        extension.project = project

        // Create info task
        def infoTask = project.tasks.create(INFO_TASK, Info) { task ->
            task.extension = extension
        }

        // Create docFxClean task (renamed from 'clean' to avoid conflict with Gradle's standard clean task)
        def cleanTask = project.tasks.create(CLEAN_TASK, Clean) { task ->
            task.extension = extension
            task.dependsOn(infoTask)
        }

        // Create docs task
        def docsTask = project.tasks.create(DOCS_TASK, Docs) { task ->
            task.extension = extension
            task.dependsOn(infoTask)
        }

        // Create zip task that packages the generated documentation
        def zipTask = project.tasks.create(DOCFX_ZIP_TASK, Zip) { task ->
            task.group = "documentation"
            task.description = "Packages the generated DocFX documentation into a zip file"
            task.dependsOn(docsTask)
            task.archiveClassifier = "docfx"
            task.destinationDirectory = project.file("${project.buildDir}/distributions")
        }
        
        // Configure the zip task in docFx task's doLast (after _site directory is created)
        // This is the correct place to configure the 'from' for the Zip task
        docsTask.doLast {
            // Get source from extension (after docFx has potentially updated it to point to docfx.json)
            def source = extension.source
            if ((source == null || source.trim().isEmpty()) && docsTask.hasProperty('source')) {
                source = docsTask.source
            }
            
            if (source == null || source.trim().isEmpty()) {
                project.logger.info("docFxZip: source is not set, skipping zip configuration")
                return
            }
            
            // Determine the output directory from the docfx.json source
            // After docFx runs, extension.source points to the docfx.json file
            def sourceFile = project.file(source)
            if (!sourceFile.exists()) {
                project.logger.info("docFxZip: source file does not exist: ${sourceFile.absolutePath}, skipping zip configuration")
                return
            }
            
            def sourceDir = sourceFile.parentFile
            def siteDir = new File(sourceDir, "_site")
            
            if (!siteDir.exists()) {
                project.logger.info("docFxZip: _site directory does not exist at ${siteDir.absolutePath}, skipping zip configuration")
                return
            }
            
            project.logger.quiet("docFxZip: Configuring zip task from _site directory: ${siteDir.absolutePath}")
            
            // Configure the zip task's from (this happens in docFx.doLast, which is the right time)
            zipTask.from(siteDir) {
                into "/"
            }
            
            // Set archive base name from source file name
            def baseName = sourceFile.name
            if (baseName.endsWith('.json')) {
                baseName = baseName.substring(0, baseName.length() - 5)
            }
            
            // Set archiveBaseName (works for both Gradle 8 and 9)
            try {
                if (zipTask.hasProperty('archiveBaseName')) {
                    def prop = zipTask.archiveBaseName
                    if (prop instanceof org.gradle.api.provider.Property) {
                        // Gradle 9+ - Property API
                        if (!prop.isPresent() || prop.get() == project.name) {
                            prop.set(baseName)
                        }
                    } else {
                        // Gradle 8 - direct property
                        if (prop == null || prop == project.name) {
                            zipTask.archiveBaseName = baseName
                        }
                    }
                }
            } catch (Exception e) {
                project.logger.debug("Could not set archiveBaseName: ${e.message}")
            }
            
            // Configure inputs/outputs for up-to-date checking
            zipTask.inputs.dir(siteDir).optional()
            zipTask.outputs.file(zipTask.archiveFile)
        }

        // Make docs task finalized by zip task
        docsTask.finalizedBy(zipTask)
    }
}
