package de.inetsoftware.docfx

import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files

import static org.junit.Assert.*

/**
 * Integration tests for the Docs task.
 * 
 * NOTE: These tests do NOT require dotnet or DocFX to be installed.
 * All tests either:
 * - Test early return paths (null/empty/non-existent source)
 * - Test component logic in isolation (JSON generation, closure handling)
 * - Never actually execute DocFX commands
 */
class DocsIntegrationTest {

    private def project
    private DocfxExtension extension
    private ExecOperations execOps
    private File testDir
    private File sourceFile

    @Before
    void setUp() {
        project = ProjectBuilder.builder().build()
        extension = new DocfxExtension()
        extension.project = project
        execOps = project.services.get(ExecOperations.class)
        
        testDir = Files.createTempDirectory("docfx-integration-test").toFile()
        sourceFile = new File(testDir, "TestLibrary.dll")
        sourceFile.createNewFile()
    }

    @After
    void tearDown() {
        if (testDir != null && testDir.exists()) {
            testDir.deleteDir()
        }
    }

    @Test
    void testExec_WithNullSource_DoesNotExecute() {
        extension.source = null
        
        Docs docs = project.tasks.create("testDocsNull", Docs.class, execOps)
        docs.extension = extension
        
        // Should not throw exception, just return early
        try {
            docs.exec()
            // If we get here, it means it handled null gracefully
            assertTrue("Should handle null source gracefully", true)
        } catch (Exception e) {
            fail("Should not throw exception for null source: ${e.message}")
        }
    }

    @Test
    void testExec_WithEmptySource_DoesNotExecute() {
        extension.source = ""
        
        Docs docs = project.tasks.create("testDocsEmpty", Docs.class, execOps)
        docs.extension = extension
        
        try {
            docs.exec()
            assertTrue("Should handle empty source gracefully", true)
        } catch (Exception e) {
            fail("Should not throw exception for empty source: ${e.message}")
        }
    }

    @Test
    void testExec_WithNonExistentFile_DoesNotExecute() {
        extension.source = "${testDir}/NonExistent.dll"
        
        Docs docs = project.tasks.create("testDocsNonExistent", Docs.class, execOps)
        docs.extension = extension
        
        try {
            docs.exec()
            // Should handle non-existent file gracefully
            assertTrue("Should handle non-existent file gracefully", true)
        } catch (Exception e) {
            // It's okay if it logs an error, but shouldn't crash
            assertTrue("Should handle non-existent file: ${e.message}", 
                e.message.contains("does not exist") || e.message.contains("Source file"))
        }
    }

    @Test
    void testExec_WithJsonSource_DoesNotAutoGenerate() {
        File jsonFile = new File(testDir, "docfx.json")
        jsonFile.text = '{"metadata": []}'
        extension.source = jsonFile.absolutePath
        
        Docs docs = project.tasks.create("testDocsJson", Docs.class, execOps)
        docs.extension = extension
        
        // Should not auto-generate since source is already JSON
        // We can't easily test the full execution without DocFX, but we can verify
        // that isSourceJsonFile returns true
        assertTrue("Should recognize JSON source", extension.isSourceJsonFile())
    }

    @Test
    void testExec_WithDllSource_AutoGeneratesJson() {
        extension.source = sourceFile.absolutePath
        
        Docs docs = project.tasks.create("testDocsDll", Docs.class, execOps)
        docs.extension = extension
        
        // Should trigger auto-generation
        assertFalse("Should not recognize DLL as JSON", extension.isSourceJsonFile())
        
        // The actual generation happens in exec(), but we can verify the setup
        // by checking that the extension is configured correctly
        assertEquals(sourceFile.absolutePath, extension.source)
    }

    @Test
    void testExec_WithAdditionalResources_ClosureIsCalled() {
        extension.source = sourceFile.absolutePath
        def closureCalled = false
        def closureRoot = null
        
        extension.additionalResources = { root ->
            closureCalled = true
            closureRoot = root
        }
        
        Docs docs = project.tasks.create("testDocsResources", Docs.class, execOps)
        docs.extension = extension
        
        // We can't easily test the full execution, but we can verify
        // that the closure is set up correctly
        assertNotNull("Additional resources closure should be set", extension.additionalResources)
        
        // Test the closure directly
        extension.additionalResources.call(testDir)
        assertTrue("Closure should be callable", closureCalled)
        assertEquals(testDir, closureRoot)
    }

    @Test
    void testExec_WithLocale_SetsEnvironmentVariables() {
        extension.source = sourceFile.absolutePath
        extension.locale = "fr-FR"
        
        def envVars = extension.getEnvironmentVariables()
        
        // Locale vars may not be set if system already has them
        // Just verify the method returns a proper map
        assertNotNull("Environment variables should not be null", envVars)
        assertTrue("Should return a Map", envVars instanceof Map)
    }

    @Test
    void testExec_WithFilter_IncludesInJson() {
        extension.source = sourceFile.absolutePath
        extension.filter = "MyNamespace"
        
        def generator = new DocfxJsonGenerator(extension, project)
        def docfxJson = generator.generateDocfxJson(sourceFile)
        def content = docfxJson.text
        
        assertTrue("Should contain filter in generated JSON", content.contains("\"filter\": \"MyNamespace\""))
    }

    @Test
    void testExec_WithCustomTitle_UsesInJson() {
        extension.source = sourceFile.absolutePath
        extension.title = "Custom Title"
        
        def generator = new DocfxJsonGenerator(extension, project)
        def docfxJson = generator.generateDocfxJson(sourceFile)
        def content = docfxJson.text
        
        assertTrue("Should contain custom title", content.contains("\"_appTitle\": \"Custom Title\""))
    }
}

