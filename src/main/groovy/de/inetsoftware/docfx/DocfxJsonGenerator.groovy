package de.inetsoftware.docfx

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.io.File

/**
 * Helper class to generate docfx.json configuration file and supporting files.
 */
class DocfxJsonGenerator {
    
    private static final Logger LOGGER = Logging.getLogger(DocfxJsonGenerator.class)
    
    private final DocfxExtension extension
    private final Project project
    
    DocfxJsonGenerator(DocfxExtension extension, Project project) {
        this.extension = extension
        this.project = project
    }
    
    /**
     * Generate docfx.json file from source file (e.g., .dll, .csproj).
     * Returns the path to the generated docfx.json file.
     */
    File generateDocfxJson(File sourceFile) {
        def srcDir = sourceFile.parentFile.toString().replaceAll("\\\\", "/")
        def docfxDir = project.file("${project.buildDir}/tmp/docFX")
        docfxDir.mkdirs()
        
        def docfxJson = new File(docfxDir, "docfx.json")
        
        // Generate docfx.json content
        def locale = extension.getLocaleForMetadata()
        def appFooter = extension.getAppFooterForMetadata()
        
        def jsonContent = """\
{
  "metadata": [
    {
      "src": [
        {
          "files": [ "${sourceFile.name}" ],
          "src": "${srcDir}"
        }
      ],
      "dest": "${extension.metadataDest}",
      "filter": "${extension.filter ?: ""}"
    }
  ],
  "build": {
    "content": [
      {
        "files": [ "**/*.yml" ],
        "src": "${extension.metadataDest}",
        "dest": "${extension.contentDest}"
      },
      {
        "files": [ "articles/**/*.md", "*.md", "toc.yml" ]
      }
    ],
    "resource": [
      {
        "files": [ "articles/images/**", "logo.svg" ]
      }
    ],
    "overwrite": "specs/*.md",
    "globalMetadata": {
      "_appTitle": "${extension.title}",
      "_enableSearch": true${appFooter ? ",\n      \"_appFooter\": \"${appFooter}\"" : ""},
      "_locale": "${locale}"
    },
    "template": [ "${extension.template}" ],
    "markdownEngineName": "${extension.markdownEngine}",
    "dest": "${extension.outputDir}",
    "xrefService": [ "${extension.xrefService}" ]
  }
}
"""
        
        docfxJson.text = jsonContent
        LOGGER.debug("Generated docfx.json at: ${docfxJson.absolutePath}")
        
        // Generate toc.yml
        generateTocYml(docfxDir)
        
        // Generate index.md
        generateIndexMd(docfxDir)
        
        return docfxJson
    }
    
    private void generateTocYml(File docfxDir) {
        def tocYml = new File(docfxDir, "toc.yml")
        def productName = "API Documentation"
        def homepage = ""
        
        if (project.hasProperty('productVersion')) {
            def pv = project.productVersion
            if (pv.hasProperty('ProductName')) {
                productName = pv.ProductName
            }
            if (pv.hasProperty('Homepage')) {
                homepage = pv.Homepage
            }
        }
        
        // Get company information - use extension properties first, then project properties
        def companyName = extension.companyName
        def companyUrl = extension.companyUrl
        
        if (!companyName && project.hasProperty('productVersion')) {
            def pv = project.productVersion
            if (pv.hasProperty('CompanyName')) {
                companyName = pv.CompanyName
            }
        }
        
        if (!companyUrl && project.hasProperty('productVersion')) {
            def pv = project.productVersion
            if (pv.hasProperty('CompanyUrl')) {
                companyUrl = pv.CompanyUrl
            }
        }
        
        def tocContent = new StringBuilder()
        tocContent.append("- name: API Documentation\n")
        tocContent.append("  href: ${extension.metadataDest}/\n")
        tocContent.append("\n")
        if (productName && !productName.isEmpty() && productName != "API Documentation") {
            tocContent.append("- name: ${productName}\n")
            if (homepage && !homepage.isEmpty()) {
                tocContent.append("  href: ${homepage}\n")
            }
            tocContent.append("\n")
        }
        // Only add company entry if company name is provided
        if (companyName && !companyName.isEmpty()) {
            tocContent.append("- name: ${companyName}\n")
            if (companyUrl && !companyUrl.isEmpty()) {
                tocContent.append("  href: ${companyUrl}\n")
            }
        }
        
        tocYml.text = tocContent.toString()
        LOGGER.debug("Generated toc.yml at: ${tocYml.absolutePath}")
    }
    
    private void generateIndexMd(File docfxDir) {
        def indexMd = new File(docfxDir, "index.md")
        indexMd.text = ""
        LOGGER.debug("Generated index.md at: ${indexMd.absolutePath}")
    }
}

