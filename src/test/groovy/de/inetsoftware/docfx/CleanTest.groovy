package de.inetsoftware.docfx

import org.junit.Assert
import org.junit.Test

import java.io.InputStream
import java.nio.file.Path

class CleanTest {

    @Test
    void testParseMetadata() throws Exception {
        StringBuilder expected = new StringBuilder()
        expected.append("dev/sdk/framework\n")
        expected.append("dev/sdk/core\n")

        InputStream in = getClass().getResourceAsStream("example.json")
        try {
            List<Path> paths = Clean.parseMetadata(in)

            StringBuilder actual = new StringBuilder()
            for (Path p : paths) {
                actual.append(p.toString()).append("\n")
            }
            Assert.assertEquals("Metadata Output paths", expected.toString(), actual.toString())
        } finally {
            in?.close()
        }
    }

    @Test
    void testParseBuild() throws Exception {
        InputStream in = getClass().getResourceAsStream("example.json")
        try {
            Path path = Clean.parseBuild(in)
            Assert.assertEquals("Build Output path", "_site", path.toString())
        } finally {
            in?.close()
        }
    }
}

