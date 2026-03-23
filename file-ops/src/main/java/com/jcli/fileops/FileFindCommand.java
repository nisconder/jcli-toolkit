package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileFindCommand implements CliCommand {
    @Override
    public String name() {
        return "find";
    }

    @Override
    public String description() {
        return "Find files in directory";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli file find")
                .description("Find files in directory recursively")
                .addOption(CommandLineParser.Option.ofValue("d", "dir", "Directory to search"))
                .addOption(CommandLineParser.Option.ofValue("e", "ext", "File extension"))
                .addOption(CommandLineParser.Option.ofValue("n", "name", "File name pattern"))
                .addOption(CommandLineParser.Option.ofValue("s", "size-gt", "Size greater than (bytes)"))
                .addOption(CommandLineParser.Option.ofValue("z", "size-lt", "Size less than (bytes)"))
                .addOption(CommandLineParser.Option.ofValue("t", "newer", "Modified after (timestamp or days:7d)"))
                .addOption(CommandLineParser.Option.ofValue("o", "output", "Output format: list, json, csv"))
                .addOption(CommandLineParser.Option.ofValue("p", "depth", "Maximum recursion depth"))
                .addOption(CommandLineParser.Option.of("v", "verbose", "Show verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String dir = cmdLine.getOptionValue(parser.getLongOption("dir"), ".");
        String ext = cmdLine.getOptionValue(parser.getLongOption("ext"));
        String namePattern = cmdLine.getOptionValue(parser.getLongOption("name"));
        String sizeGtStr = cmdLine.getOptionValue(parser.getLongOption("size-gt"));
        String sizeLtStr = cmdLine.getOptionValue(parser.getLongOption("size-lt"));
        String newerStr = cmdLine.getOptionValue(parser.getLongOption("newer"));
        String output = cmdLine.getOptionValue(parser.getLongOption("output"), "list");
        String depthStr = cmdLine.getOptionValue(parser.getLongOption("depth"));
        boolean verbose = cmdLine.hasOption(parser.getShortOption("v"));

        if (verbose) {
            Logger.setVerbose(true);
        }

        Path startDir = Paths.get(dir);
        if (!Files.exists(startDir)) {
            Logger.error("Directory does not exist: " + dir);
            return 1;
        }

        int maxDepth = depthStr != null ? Integer.parseInt(depthStr) : Integer.MAX_VALUE;

        List<FileInfo> results = new ArrayList<>();
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            private int currentDepth = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                currentDepth++;
                if (currentDepth > maxDepth) {
                    currentDepth--;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                currentDepth--;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matches(file, attrs, ext, namePattern, sizeGtStr, sizeLtStr, newerStr)) {
                    results.add(new FileInfo(file, attrs));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Logger.warn("Failed to visit: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(startDir, visitor);

        if (output.equals("json")) {
            outputJson(results);
        } else if (output.equals("csv")) {
            outputCsv(results);
        } else {
            outputList(results);
        }

        return 0;
    }

    private boolean matches(Path file, BasicFileAttributes attrs, String ext, String namePattern,
                           String sizeGtStr, String sizeLtStr, String newerStr) {
        String fileName = file.getFileName().toString();

        if (ext != null) {
            if (!fileName.toLowerCase().endsWith("." + ext.toLowerCase())) {
                return false;
            }
        }

        if (namePattern != null) {
            if (!fileName.matches(namePattern.replace("*", ".*").replace("?", "."))) {
                return false;
            }
        }

        long size = attrs.size();
        if (sizeGtStr != null) {
            long sizeGt = parseSize(sizeGtStr);
            if (size <= sizeGt) return false;
        }
        if (sizeLtStr != null) {
            long sizeLt = parseSize(sizeLtStr);
            if (size >= sizeLt) return false;
        }

        if (newerStr != null) {
            long modifiedTime = parseNewer(newerStr);
            if (attrs.lastModifiedTime().toMillis() < modifiedTime) {
                return false;
            }
        }

        return true;
    }

    private long parseSize(String sizeStr) {
        sizeStr = sizeStr.toLowerCase();
        long multiplier = 1;
        if (sizeStr.endsWith("k")) {
            multiplier = 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        } else if (sizeStr.endsWith("m")) {
            multiplier = 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        } else if (sizeStr.endsWith("g")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        }
        return Long.parseLong(sizeStr) * multiplier;
    }

    private long parseNewer(String newerStr) {
        newerStr = newerStr.toLowerCase();
        if (newerStr.endsWith("d")) {
            int days = Integer.parseInt(newerStr.substring(0, newerStr.length() - 1));
            return System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
        }
        try {
            return Long.parseLong(newerStr);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private void outputList(List<FileInfo> results) {
        for (FileInfo info : results) {
            System.out.println(info.path());
        }
    }

    private void outputJson(List<FileInfo> results) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < results.size(); i++) {
            FileInfo info = results.get(i);
            sb.append("  {\n");
            sb.append("    \"path\": \"").append(info.path()).append("\",\n");
            sb.append("    \"size\": ").append(info.size()).append(",\n");
            sb.append("    \"modified\": ").append(info.lastModified()).append("\n");
            sb.append("  }");
            if (i < results.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        Logger.json(sb.toString());
    }

    private void outputCsv(List<FileInfo> results) {
        System.out.println("path,size,modified");
        for (FileInfo info : results) {
            System.out.println(info.path() + "," + info.size() + "," + info.lastModified());
        }
    }

    record FileInfo(String path, long size, long lastModified) {
        FileInfo(Path path, BasicFileAttributes attrs) {
            this(path.toAbsolutePath().toString(), attrs.size(), attrs.lastModifiedTime().toMillis());
        }
    }
}
