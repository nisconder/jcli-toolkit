package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "find", description = "Find files in directory", mixinStandardHelpOptions = true)
public class FileFindCommand implements CliCommand, Callable<Integer> {
    @Option(names = {"-d", "--dir"}, description = "Directory to search", defaultValue = ".")
    private String dir;

    @Option(names = {"-e", "--ext"}, description = "File extension")
    private String ext;

    @Option(names = {"-n", "--name"}, description = "File name pattern")
    private String namePattern;

    @Option(names = {"-s", "--size-gt"}, description = "Size greater than (bytes)")
    private String sizeGtStr;

    @Option(names = {"-z", "--size-lt"}, description = "Size less than (bytes)")
    private String sizeLtStr;

    @Option(names = {"-t", "--newer"}, description = "Modified after (timestamp or days:7d)")
    private String newerStr;

    @Option(names = {"-o", "--output"}, description = "Output format: list, json, csv", defaultValue = "list")
    private String output;

    @Option(names = {"-p", "--depth"}, description = "Maximum recursion depth")
    private String depthStr;

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

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
        return new picocli.CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
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
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                currentDepth++;
                if (currentDepth > maxDepth) {
                    currentDepth--;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) {
                currentDepth--;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matches(file, attrs)) {
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

        try {
            Files.walkFileTree(startDir, visitor);
        } catch (IOException e) {
            Logger.error("Failed to walk directory: " + e.getMessage());
            return 1;
        }

        if ("json".equals(output)) {
            outputJson(results);
        } else if ("csv".equals(output)) {
            outputCsv(results);
        } else {
            outputList(results);
        }

        return 0;
    }

    private boolean matches(Path file, BasicFileAttributes attrs) {
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Logger.json(gson.toJson(results));
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