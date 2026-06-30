package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Command(name = "stat", description = "Show statistics of directory", mixinStandardHelpOptions = true)
public class FileStatCommand implements CliCommand {
    @Option(names = {"-d", "--dir"}, description = "Directory to analyze", defaultValue = ".")
    private String dir;

    @Option(names = {"-o", "--output"}, description = "Output format: table, json", defaultValue = "table")
    private String output;

    @Option(names = {"-r", "--recursive"}, description = "Recursively analyze subdirectories")
    private boolean recursive;

    @Override
    public String name() {
        return "stat";
    }

    @Override
    public String description() {
        return "Show statistics of directory";
    }

    @Override
    public int execute(String[] args) throws Exception {
        return new picocli.CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
        Path startDir = Paths.get(dir);
        if (!Files.exists(startDir)) {
            Logger.error("Directory does not exist: " + dir);
            return 1;
        }

        DirStats stats;
        try {
            stats = analyzeDirectory(startDir, recursive);
        } catch (IOException e) {
            Logger.error("Failed to analyze directory: " + e.getMessage());
            return 1;
        }

        if ("json".equals(output)) {
            outputJson(stats);
        } else {
            outputTable(stats);
        }

        return 0;
    }

    private DirStats analyzeDirectory(Path dir, boolean recursive) throws IOException {
        DirStats stats = new DirStats();

        if (recursive) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    stats.totalFiles++;
                    stats.totalSize += attrs.size();

                    String fileName = file.getFileName().toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        String ext = fileName.substring(dotIndex + 1).toLowerCase();
                        stats.byExtension.merge(ext, 1L, Long::sum);
                    } else {
                        stats.byExtension.merge("(no ext)", 1L, Long::sum);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Logger.warn("Failed to visit: " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (var stream = Files.list(dir)) {
                stream.forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            stats.totalFiles++;
                            stats.totalSize += attrs.size();

                            String fileName = path.getFileName().toString();
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                                stats.byExtension.merge(ext, 1L, Long::sum);
                            } else {
                                stats.byExtension.merge("(no ext)", 1L, Long::sum);
                            }
                        } catch (IOException e) {
                            Logger.warn("Failed to read attributes: " + path);
                        }
                    }
                });
            }
        }

        return stats;
    }

    private void outputTable(DirStats stats) {
        System.out.println("Total Files: " + stats.totalFiles);
        System.out.println("Total Size: " + formatSize(stats.totalSize));
        System.out.println();
        System.out.println("By Extension:");

        List<Map.Entry<String, Long>> sorted = stats.byExtension.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : sorted) {
            System.out.printf("  %-15s %6d files%n", entry.getKey(), entry.getValue());
        }
    }

    private void outputJson(DirStats stats) {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"totalFiles\": ").append(stats.totalFiles).append(",\n");
        sb.append("  \"totalSize\": ").append(stats.totalSize).append(",\n");
        sb.append("  \"byExtension\": {\n");

        List<Map.Entry<String, Long>> sorted = stats.byExtension.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            if (i < sorted.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  }\n");
        sb.append("}");
        Logger.json(sb.toString());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    static class DirStats {
        long totalFiles = 0;
        long totalSize = 0;
        Map<String, Long> byExtension = new HashMap<>();
    }
}