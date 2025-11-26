package de.inetsoftware.docfx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal

import java.io.File
import java.util.function.Consumer

abstract class DocfxDefaultTask extends DefaultTask {

    @Internal
    DocfxExtension extension = new DocfxExtension()

    protected static void whenHasValue(String value, Consumer<String> consumer) {
        if (value != null && !value.isEmpty()) {
            consumer.accept(value)
        }
    }

    protected final String directoryName(String filePath) {
        String[] baseDir = ["."]
        whenHasValue(filePath) { s ->
            File file = new File(s)
            baseDir[0] = file.absoluteFile.parent
        }
        return baseDir[0]
    }
}

