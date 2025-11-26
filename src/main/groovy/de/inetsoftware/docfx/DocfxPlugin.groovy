package de.inetsoftware.docfx

import org.gradle.api.Plugin
import org.gradle.api.Project

class DocfxPlugin implements Plugin<Project> {

    private static final String INFO_TASK = "info"
    private static final String CLEAN_TASK = "clean"
    private static final String DOCS_TASK = "docs"

    @Override
    void apply(Project project) {
        final DocfxExtension extension = project.extensions.create("docfx", DocfxExtension)

        project.tasks.create(INFO_TASK, Info) { task ->
            task.extension = extension
        }
        project.tasks.create(CLEAN_TASK, Clean) { task ->
            task.extension = extension
            task.dependsOn(INFO_TASK)
        }
        project.tasks.create(DOCS_TASK, Docs) { task ->
            task.extension = extension
            task.dependsOn(INFO_TASK)
        }
    }
}

