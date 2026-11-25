package com.example.chatbot.aitools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebsiteBuilderToolTest {

    private final WebsiteBuilderTool tool = new WebsiteBuilderTool();

    @Test
    void writesReadsAndListsProjectFiles() throws IOException {
        String project = "test-" + UUID.randomUUID();
        Path projectPath = Path.of("generated-sites").toAbsolutePath().normalize().resolve(project);
        try {
            assertEquals("File written successfully: " + project + "/index.html",
                    tool.writeFile(project + "/index.html", "<h1>Hello</h1>"));
            assertEquals("<h1>Hello</h1>", tool.readFile(project + "/index.html"));
            assertEquals(project + "/index.html", tool.listFiles(project));
        } finally {
            deleteProject(projectPath);
        }
    }

    @Test
    void rejectsPathsOutsideTheWorkspace() {
        assertTrue(tool.createDirectory("../outside").startsWith("Failed to create directory:"));
        assertTrue(tool.writeFile("/tmp/outside.html", "content")
                .startsWith("Failed to write file:"));
        assertTrue(tool.readFile("  ").startsWith("Failed to read file:"));
    }

    @Test
    void reportsMissingAndNonDirectoryPaths() throws IOException {
        String project = "test-" + UUID.randomUUID();
        Path projectPath = Path.of("generated-sites").toAbsolutePath().normalize().resolve(project);
        try {
            assertEquals("Directory created successfully: " + project, tool.createDirectory(project));
            assertTrue(tool.createDirectory(project).startsWith("Failed to create directory:"));
            tool.writeFile(project + "/index.html", "content");

            assertEquals("Directory does not exist: " + project + "/missing",
                    tool.listFiles(project + "/missing"));
            assertEquals("Path is not a directory: " + project + "/index.html",
                    tool.listFiles(project + "/index.html"));
        } finally {
            deleteProject(projectPath);
        }
    }

    private static void deleteProject(Path projectPath) throws IOException {
        if (!Files.exists(projectPath)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(projectPath)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
