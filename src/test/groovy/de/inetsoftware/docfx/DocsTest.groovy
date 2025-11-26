package de.inetsoftware.docfx

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.Assert.*

/**
 * Tests for the Docs task's getExecutableInfo() method.
 * 
 * NOTE: These tests do NOT require dotnet or DocFX to be installed.
 * They only test the logic for determining which executable to use,
 * using mock files and reflection to access the private method.
 */
class DocsTest {

    private Project project
    private DocfxExtension extension
    private ExecOperations execOps
    private File testDir
    private File docfxDir

    @Before
    void setUp() {
        project = ProjectBuilder.builder().build()
        extension = new DocfxExtension()
        extension.project = project
        // Create ExecOperations using project's services
        // We need ExecOperations for the Docs constructor, but getExecutableInfo() doesn't use it
        execOps = project.services.get(ExecOperations.class)
        testDir = Files.createTempDirectory("docfx-test").toFile()
        docfxDir = new File(testDir, "docfx")
        docfxDir.mkdirs()
    }

    @After
    void tearDown() {
        if (testDir != null && testDir.exists()) {
            testDir.deleteDir()
        }
    }

    @Test
    void testGetExecutableInfo_Windows_WithExe() {
        // Skip test if not on Windows
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        File docfxExe = new File(docfxDir, "docfx.exe")
        docfxExe.createNewFile()
        docfxExe.setExecutable(true)

        // Create a minimal Docs task instance to test
        Docs docs = project.tasks.create("testDocs", Docs.class, execOps)
        docs.extension = extension

        // Use reflection to access private method for testing
        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        assertEquals("docfx.exe", new File(result.executable).name)
        assertTrue("Args should be empty on Windows with exe", result.args.isEmpty())
    }

    @Test
    void testGetExecutableInfo_NonWindows_WithDll_NoExe() {
        // Skip test if on Windows
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        // Create docfx.dll but no docfx executable
        File docfxDll = new File(docfxDir, "docfx.dll")
        docfxDll.createNewFile()

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        assertEquals("dotnet", result.executable)
        assertEquals(1, result.args.size())
        assertEquals(docfxDll.absolutePath, result.args[0])
    }

    @Test
    void testGetExecutableInfo_NonWindows_WithExe() {
        // Skip test if on Windows
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        // Create both docfx executable and docfx.dll
        File docfxExe = new File(docfxDir, "docfx")
        docfxExe.createNewFile()
        docfxExe.setExecutable(true)
        File docfxDll = new File(docfxDir, "docfx.dll")
        docfxDll.createNewFile()

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        // Should use the executable, not dotnet
        assertTrue("Should use docfx executable", result.executable.contains("docfx"))
        assertFalse("Should not use dotnet when exe exists", result.executable == "dotnet")
        assertTrue("Args should be empty when using exe", result.args.isEmpty())
    }

    @Test
    void testGetExecutableInfo_NonWindows_NoExe_NoDll() {
        // Skip test if on Windows
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        // Don't create any files

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        // Should fall back to the original executable path (even though it doesn't exist)
        assertTrue("Should return original executable path", result.executable.contains("docfx"))
        assertTrue("Args should be empty when no dll found", result.args.isEmpty())
    }

    @Test
    void testGetExecutableInfo_NonWindows_WithDll_NonExecutableExe() {
        // Skip test if on Windows
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        // Create non-executable docfx file and docfx.dll
        File docfxExe = new File(docfxDir, "docfx")
        docfxExe.createNewFile()
        docfxExe.setExecutable(false) // Not executable
        File docfxDll = new File(docfxDir, "docfx.dll")
        docfxDll.createNewFile()

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        // Should use dotnet since exe is not executable
        assertEquals("dotnet", result.executable)
        assertEquals(1, result.args.size())
        assertEquals(docfxDll.absolutePath, result.args[0])
    }

    @Test
    void testGetExecutableInfo_NoDocsHome() {
        extension.docsHome = null

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        // Should return just "docfx" or "docfx.exe" depending on platform
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            assertEquals("docfx.exe", result.executable)
        } else {
            assertEquals("docfx", result.executable)
        }
        assertTrue("Args should be empty when no docsHome", result.args.isEmpty())
    }

    @Test
    void testGetExecutableInfo_ArgsArePassedCorrectly() {
        // Skip test if on Windows
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return
        }

        extension.docsHome = docfxDir.absolutePath
        File docfxDll = new File(docfxDir, "docfx.dll")
        docfxDll.createNewFile()

        Docs docs = project.tasks.create("testDocs${System.currentTimeMillis()}", Docs.class, execOps)
        docs.extension = extension

        def method = Docs.class.getDeclaredMethod("getExecutableInfo")
        method.setAccessible(true)
        def result = method.invoke(docs)

        // Verify the structure of the returned map
        assertNotNull("Result should not be null", result)
        assertTrue("Result should contain 'executable' key", result.containsKey("executable"))
        assertTrue("Result should contain 'args' key", result.containsKey("args"))
        assertTrue("Args should be a list", result.args instanceof List)
    }
}

