package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;

public class FileSyncCommand implements CliCommand {
    private static final Scanner INPUT_SCANNER = new Scanner(System.in);

    @Override
    public String name() {
        return "sync";
    }

    @Override
    public String description() {
        return "Sync directories";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli file sync")
                .description("Sync source directory to target directory")
                .addOption(CommandLineParser.Option.ofValue("s", "source", "Source directory"))
                .addOption(CommandLineParser.Option.ofValue("t", "target", "Target directory"))
                .addOption(CommandLineParser.Option.ofValue("x", "exclude", "Exclude pattern"))
                .addOption(CommandLineParser.Option.of("d", "delete", "Delete extra files in target"))
                .addOption(CommandLineParser.Option.of("y", "dry-run", "Preview changes without executing"))
                .addOption(CommandLineParser.Option.of("c", "confirm", "Confirm before syncing"))
                .addOption(CommandLineParser.Option.of("v", "verbose", "Verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String sourceDir = cmdLine.getOptionValue(parser.getLongOption("source"));
        String targetDir = cmdLine.getOptionValue(parser.getLongOption("target"));
        String exclude = cmdLine.getOptionValue(parser.getLongOption("exclude"));
        boolean deleteExtra = cmdLine.hasOption(parser.getShortOption("d"));
        boolean dryRun = cmdLine.hasOption(parser.getShortOption("y"));
        boolean confirm = cmdLine.hasOption(parser.getShortOption("c"));
        boolean verbose = cmdLine.hasOption(parser.getShortOption("v"));

        if (sourceDir == null || targetDir == null) {
            Logger.error("Source and target directories are required");
            return 1;
        }

        if (verbose) {
            Logger.setVerbose(true);
        }

        Path source = Paths.get(sourceDir);
        Path target = Paths.get(targetDir);

        if (!Files.exists(source)) {
            Logger.error("Source directory does not exist: " + sourceDir);
            return 1;
        }

        List<SyncOperation> operations = calculateOperations(source, target, exclude, deleteExtra);

        if (operations.isEmpty()) {
            Logger.info("No changes needed");
            return 0;
        }

        Logger.info("Operations to perform:");
        for (SyncOperation op : operations) {
            System.out.println("  " + op.type() + ": " + op.source() + " -> " + op.target());
        }

        if (dryRun) {
            Logger.info("Dry run mode - no changes made");
            return 0;
        }

        if (confirm) {
            System.out.print("Proceed with sync? (y/n): ");
            String response;
            if (System.console() != null) {
                response = System.console().readLine();
            } else {
                response = INPUT_SCANNER.nextLine();
            }
            if (!response.equalsIgnoreCase("y")) {
                Logger.info("Cancelled");
                return 0;
            }
        }

        int successCount = 0;
        int failCount = 0;
        for (SyncOperation op : operations) {
            try {
                switch (op.type()) {
                    case COPY:
                        if (Files.isDirectory(op.source())) {
                            Files.createDirectories(op.target());
                        } else {
                            Path parent = op.target().getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(op.source(), op.target(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        break;
                    case DELETE:
                        Files.delete(op.target());
                        break;
                }
                if (verbose) {
                    Logger.info("Completed: " + op.type() + " " + op.target());
                }
                successCount++;
            } catch (IOException e) {
                Logger.error("Failed: " + op.type() + " " + op.target() + " - " + e.getMessage());
                failCount++;
            }
        }

        Logger.info("Completed " + successCount + " operations");
        if (failCount > 0) {
            Logger.warn("Failed " + failCount + " operations");
            return 1;
        }

        return 0;
    }

    private List<SyncOperation> calculateOperations(Path source, Path target, String exclude, boolean deleteExtra) 
            throws IOException {
        List<SyncOperation> operations = new ArrayList<>();
        Set<Path> targetFiles = new HashSet<>();

        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path relative = source.relativize(dir);
                Path targetDir = target.resolve(relative);
                try {
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                } catch (IOException e) {
                    Logger.warn("Failed to create directory: " + targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (exclude != null && file.toString().matches(exclude)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relative = source.relativize(file);
                Path targetFile = target.resolve(relative);
                targetFiles.add(relative);

                boolean needsCopy = false;
                if (!Files.exists(targetFile)) {
                    needsCopy = true;
                } else {
                    try {
                        BasicFileAttributes targetAttrs = Files.readAttributes(targetFile, BasicFileAttributes.class);
                        if (attrs.lastModifiedTime().compareTo(targetAttrs.lastModifiedTime()) > 0) {
                            needsCopy = true;
                        }
                    } catch (IOException e) {
                        needsCopy = true;
                    }
                }

                if (needsCopy) {
                    operations.add(new SyncOperation(file, targetFile, OperationType.COPY));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Logger.warn("Failed to visit: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        if (deleteExtra) {
            Files.walkFileTree(target, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relative = target.relativize(file);
                    if (!targetFiles.contains(relative)) {
                        operations.add(new SyncOperation(null, file, OperationType.DELETE));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    Path relative = target.relativize(dir);
                    if (relative.getNameCount() > 0 && !targetFiles.contains(relative)) {
                        operations.add(new SyncOperation(null, dir, OperationType.DELETE));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return operations;
    }

    record SyncOperation(Path source, Path target, OperationType type) {}
    enum OperationType { COPY, DELETE }
}
