package de.inetsoftware.docfx

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

class DocfxPlugin implements Plugin<Project> {

    private static final String DOCFX_CONFIG_EXTENSION  = "docfxConfig"
    private static final String INFO_TASK               = "info"
    private static final String CLEAN_TASK              = "docfxClean"
    private static final String DOCS_TASK               = "docfx"
    private static final String DOCFX_ZIP_TASK          = "docfxZip"

    @Override
    void apply(Project project) {
        // Use 'docfxConfig' as extension name to avoid conflict with 'docfx' task
        final DocfxExtension extension = project.extensions.create(DOCFX_CONFIG_EXTENSION, DocfxExtension)
        // Set project reference for accessing project properties
        extension.project = project

        // Create info task
        def infoTask = project.tasks.create(INFO_TASK, Info) { task ->
            task.extension = extension
        }

        // Create docfxClean task (renamed from 'clean' to avoid conflict with Gradle's standard clean task)
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
            
            task.doFirst {
                // Determine the output directory from the docfx.json source
                if (extension.source != null && !extension.source.isEmpty()) {
                    def sourceFile = project.file(extension.source)
                    if (sourceFile.exists()) {
                        def sourceDir = sourceFile.parentFile
                        def siteDir = new File(sourceDir, "_site")
                        
                        if (siteDir.exists()) {
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
                        } else {
                            project.logger.warn("DocFX output directory '_site' not found at ${siteDir}. Zip task may be empty.")
                        }
                    }
                } else {
                    project.logger.warn("DocFX source not set. Zip task will be empty.")
                }
            }
        }

        // Make docs task finalized by zip task
        docsTask.finalizedBy(zipTask)
        
        // After evaluation, find any custom Docs tasks and make docfxZip depend on them
        // This allows build scripts to create custom Docs tasks (like 'docFx') and have them work with docfxZip
        project.afterEvaluate {
            project.tasks.withType(Docs).each { docsTaskInstance ->
                if (docsTaskInstance != docsTask && docsTaskInstance.name != DOCS_TASK) {
                    // Found a custom Docs task - make docfxZip depend on it
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

