package de.inetsoftware.docfx

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths

class Clean extends DocfxDefaultTask {

    private static final Logger LOGGER = Logging.getLogger(Clean.class)

    @TaskAction
    void exec() {
        String source = extension.source
        if (source == null || source.trim().length() == 0) {
            return
        }

        File configFile = project.file(source)
        cleanMetadata(configFile)
        cleanBuild(configFile)
    }

    private static List<Path> parseMetadataFromFile(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            return parseMetadata(in)
        } catch (IOException ex) {
            throw new RuntimeException(ex)
        }
    }

    static List<Path> parseMetadata(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
        ObjectReader reader = mapper.reader()
        JsonNode result = reader.readTree(in)

        List<Path> paths = []
        if (result.has("metadata")) {
            JsonNode metadatas = result.get("metadata")
            metadatas.forEach { metadata ->
                Path path = Paths.get("api")
                if (metadata.has("dest")) {
                    path = Paths.get(metadata.get("dest").asText())
                }
                paths.add(path)
            }
        }
        return paths
    }

    private void cleanMetadata(File configFile) {
        List<Path> apis = parseMetadataFromFile(configFile)

        List<String> includes = apis
                .stream()
                .map { p -> p.toString() + File.separator + "*.yml" }
                .collect(java.util.stream.Collectors.toList())

        for (String include : includes) {
            LOGGER.quiet("Deleting '${include}'.")
        }

        String baseDir = directoryName(configFile.path)
        project.fileTree(baseDir) { t ->
            t.include(includes)
        }.each { file ->
            file.delete()
        }
    }

    private static Path parseBuildFromFile(File configFile) {
        try (FileInputStream in = new FileInputStream(configFile)) {
            return parseBuild(in)
        } catch (IOException ex) {
            throw new RuntimeException(ex)
        }
    }

    static Path parseBuild(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
        ObjectReader reader = mapper.reader()
        JsonNode result = reader.readTree(in)
        if (result.has("build")) {
            JsonNode build = result.get("build")
            if (build.has("dest")) {
                return Paths.get(build.get("dest").asText())
            }
        }
        return Paths.get("_site")
    }

    private void cleanBuild(File configFile) {
        Path path = parseBuildFromFile(configFile)
        LOGGER.quiet("Deleting '${path.toString()}'.")
        Path baseDir = Paths.get(directoryName(configFile.path))
        project.delete(baseDir.resolve(path).toString())
    }
}

