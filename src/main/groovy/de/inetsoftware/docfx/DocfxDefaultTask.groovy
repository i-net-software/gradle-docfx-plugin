package de.inetsoftware.docfx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal

import java.io.File
import java.util.function.Consumer

abstract class DocfxDefaultTask extends DefaultTask {

    @Internal
    DocfxExtension extension = new DocfxExtension()

    // Delegate properties to extension for seamless access
    // This allows setting task.source = '...' which will set extension.source
    
    @Internal
    String getSource() {
        return extension?.source
    }
    
    void setSource(String source) {
        if (extension != null) {
            extension.source = source
        }
    }
    
    @Internal
    String getDocsHome() {
        return extension?.docsHome
    }
    
    void setDocsHome(String docsHome) {
        if (extension != null) {
            extension.docsHome = docsHome
        }
    }
    
    @Internal
    String getLocale() {
        return extension?.locale
    }
    
    void setLocale(String locale) {
        if (extension != null) {
            extension.locale = locale
        }
    }
    
    @Internal
    String getFilter() {
        return extension?.filter
    }
    
    void setFilter(String filter) {
        if (extension != null) {
            extension.filter = filter
        }
    }
    
    @Internal
    String getTitle() {
        return extension?.title
    }
    
    void setTitle(String title) {
        if (extension != null) {
            extension.title = title
        }
    }
    
    @Internal
    String getCompanyName() {
        return extension?.companyName
    }
    
    void setCompanyName(String companyName) {
        if (extension != null) {
            extension.companyName = companyName
        }
    }
    
    @Internal
    String getCompanyUrl() {
        return extension?.companyUrl
    }
    
    void setCompanyUrl(String companyUrl) {
        if (extension != null) {
            extension.companyUrl = companyUrl
        }
    }
    
    @Internal
    Map<String, String> getEnvironment() {
        return extension?.environment
    }
    
    void setEnvironment(Map<String, String> environment) {
        if (extension != null) {
            extension.environment = environment
        }
    }
    
    @Internal
    Closure getAdditionalResources() {
        return extension?.additionalResources
    }
    
    void setAdditionalResources(Closure additionalResources) {
        if (extension != null) {
            extension.additionalResources = additionalResources
        }
    }

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

