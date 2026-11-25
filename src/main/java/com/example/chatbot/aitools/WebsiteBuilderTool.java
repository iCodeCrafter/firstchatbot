package com.example.chatbot.aitools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class WebsiteBuilderTool {

    private final Path workspace;

    public WebsiteBuilderTool() {
        Path configuredWorkspace = Path.of("generated-sites").toAbsolutePath().normalize();
        try {
            if (Files.isSymbolicLink(configuredWorkspace)) {
                throw new IllegalStateException("The website workspace must not be a symbolic link");
            }
            Files.createDirectories(configuredWorkspace);
            this.workspace = configuredWorkspace.toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create website workspace", e);
        }
    }

    @Tool(description = "Creates a new directory inside the website workspace.")
    public String createDirectory(
            @ToolParam(description = "Relative project directory path, for example brewlab") String path) {
        try {
            Path directory = safePath(path);
            Files.createDirectories(directory.getParent());
            Files.createDirectory(directory);
            return "Directory created successfully: " + path;
        } catch (IOException | IllegalArgumentException e) {
            return "Failed to create directory: " + e.getMessage();
        }
    }

    @Tool(description = """
            Creates or overwrites a text file inside the website workspace.
            Use this to create HTML, CSS, and JavaScript files.
            """)
    public String writeFile(
            @ToolParam(description = "Relative file path, for example brewlab/index.html") String path,
            @ToolParam(description = "Complete content that should be written into the file") String content) {
        if (content == null) {
            return "Failed to write file: content must not be null";
        }

        try {
            Path file = safePath(path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return "File written successfully: " + path;
        } catch (IOException | IllegalArgumentException e) {
            return "Failed to write file: " + e.getMessage();
        }
    }

    @Tool(description = "Reads a text file from the website workspace.")
    public String readFile(@ToolParam(description = "Relative file path") String path) {
        try {
            return Files.readString(safePath(path), StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException e) {
            return "Failed to read file: " + e.getMessage();
        }
    }

    @Tool(description = "Lists all files and directories inside a website project.")
    public String listFiles(
            @ToolParam(description = "Relative project directory path, for example brewlab") String path) {
        try {
            Path directory = safePath(path);
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                return "Directory does not exist: " + path;
            }
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                return "Path is not a directory: " + path;
            }

            try (var files = Files.walk(directory)) {
                String result = files
                        .filter(file -> !file.equals(directory))
                        .map(workspace::relativize)
                        .map(Path::toString)
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.joining("\n"));
                return result.isEmpty() ? "No files found in: " + path : result;
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            return "Failed to list files: " + e.getMessage();
        }
    }

    private Path safePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }

        Path relativePath = Path.of(path);
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Only relative paths inside generated-sites are allowed");
        }

        Path resolved = workspace.resolve(relativePath).normalize();
        if (!resolved.startsWith(workspace)) {
            throw new IllegalArgumentException("Access outside generated-sites is not allowed");
        }

        Path current = workspace;
        for (Path segment : workspace.relativize(resolved)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Symbolic links are not allowed in generated-sites");
            }
        }
        return resolved;
    }
}
