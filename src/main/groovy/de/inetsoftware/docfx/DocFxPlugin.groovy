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
            
            // Use onlyIf to allow task to run even if source is not set yet
            // (source might be set in another task's doLast, e.g., msbuild.doLast)
            // Actual checks happen in doFirst after dependencies have run
            task.onlyIf {
                // Always allow the task to run - we'll check conditions in doFirst
                // This handles the case where source is set in another task's doLast
                return true
            }
            
            task.doFirst {
                project.logger.quiet("docFxZip: Starting zip task")
                
                // Check source from extension first, then from docFx task (in case it was set on the task)
                def source = extension.source
                if ((source == null || source.trim().isEmpty()) && docsTask.hasProperty('source')) {
                    source = docsTask.source
                }
                
                project.logger.quiet("docFxZip: checking conditions, extension.source=${extension.source}, docFx.source=${docsTask.hasProperty('source') ? docsTask.source : 'N/A'}, resolved source=${source}")
                
                if (source == null || source.trim().isEmpty()) {
                    throw new org.gradle.api.tasks.StopExecutionException("docFxZip SKIPPED: source is not set (null or empty) - docFx task may not have source configured. Make sure docFx.source is set before docFxZip runs.")
                }
                
                // Determine the output directory from the docfx.json source
                def sourceFile = project.file(source)
                project.logger.quiet("docFxZip: checking source file: ${sourceFile.absolutePath}, exists=${sourceFile.exists()}")
                
                if (!sourceFile.exists()) {
                    throw new org.gradle.api.tasks.StopExecutionException("docFxZip SKIPPED: source file does not exist: ${sourceFile.absolutePath}")
                }
                
                def sourceDir = sourceFile.parentFile
                def siteDir = new File(sourceDir, "_site")
                project.logger.quiet("docFxZip: checking _site directory: ${siteDir.absolutePath}, exists=${siteDir.exists()}")
                
                if (!siteDir.exists()) {
                    throw new org.gradle.api.tasks.StopExecutionException("docFxZip SKIPPED: _site directory does not exist at ${siteDir.absolutePath} (docFx task may not have run successfully)")
                }
                
                project.logger.quiet("docFxZip: ALL CONDITIONS MET - zipping _site directory from ${siteDir.absolutePath}")
                task.from(siteDir) {
                    into "/"
                }
                task.destinationDirectory = project.file("${project.buildDir}/distributions")
                task.archiveClassifier = "docfx"
                
                // Set archive base name from source file name
                def baseName = sourceFile.name
                if (baseName.endsWith('.json')) {
                    baseName = baseName.substring(0, baseName.length() - 5)
                }
                
                // Set archiveBaseName (works for both Gradle 8 and 9)
                // In Gradle 8, archiveBaseName is a property
                // In Gradle 9+, archiveBaseName is a Property<String>
                try {
                    if (task.hasProperty('archiveBaseName')) {
                        def prop = task.archiveBaseName
                        if (prop instanceof org.gradle.api.provider.Property) {
                            // Gradle 9+ - Property API
                            if (!prop.isPresent() || prop.get() == project.name) {
                                prop.set(baseName)
                            }
                        } else {
                            // Gradle 8 - direct property
                            if (prop == null || prop == project.name) {
                                task.archiveBaseName = baseName
                            }
                        }
                    }
                } catch (Exception e) {
                    project.logger.debug("Could not set archiveBaseName: ${e.message}")
                }
            }
            
            task.doLast {
                project.logger.quiet("docFxZip: Completed zip task")
            }
        }

        // Make docs task finalized by zip task
        docsTask.finalizedBy(zipTask)
        
        // After evaluation, find any custom Docs tasks and make docFxZip depend on them
        // This allows build scripts to create custom Docs tasks and have them work with docFxZip
        project.afterEvaluate {
            project.tasks.withType(Docs).each { docsTaskInstance ->
                if (docsTaskInstance != docsTask && docsTaskInstance.name != DOCS_TASK) {
                    // Found a custom Docs task - make docFxZip depend on it
                    zipTask.dependsOn(docsTaskInstance)
                    // Also make the custom task finalized by zip
                    docsTaskInstance.finalizedBy(zipTask)
                }
            }
            
            // Optionally hook into common publishing tasks
            // Check for preparePublish task (common in build scripts)
            def preparePublishTask = project.tasks.findByName('preparePublish')
            if (preparePublishTask != null) {
                preparePublishTask.dependsOn(zipTask)
            }
        }
    }
}
