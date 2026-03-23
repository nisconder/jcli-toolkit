package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileDiffCommand implements CliCommand {
    @Override
    public String name() {
        return "diff";
    }

    @Override
    public String description() {
        return "Compare directories";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli file diff")
                .description("Compare two directories")
                .addOption(CommandLineParser.Option.ofValue("d1", "dir1", "First directory"))
                .addOption(CommandLineParser.Option.ofValue("d2", "dir2", "Second directory"))
                .addOption(CommandLineParser.Option.ofValue("x", "exclude", "Exclude pattern"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String dir1Str = cmdLine.getOptionValue(parser.getLongOption("dir1"));
        String dir2Str = cmdLine.getOptionValue(parser.getLongOption("dir2"));
        String exclude = cmdLine.getOptionValue(parser.getLongOption("exclude"));

        if (dir1Str == null || dir2Str == null) {
            Logger.error("Both directories are required");
            return 1;
        }

        Path dir1 = Paths.get(dir1Str);
        Path dir2 = Paths.get(dir2Str);

        if (!Files.exists(dir1) || !Files.exists(dir2)) {
            Logger.error("One or both directories do not exist");
            return 1;
        }

        List<String> onlyInDir1 = new ArrayList<>();
        List<String> onlyInDir2 = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        Files.walkFileTree(dir1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (exclude != null && file.toString().matches(exclude)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relative = dir1.relativize(file);
                Path target = dir2.resolve(relative);

                if (!Files.exists(target)) {
                    onlyInDir1.add(relative.toString());
                } else {
                    try {
                        BasicFileAttributes targetAttrs = Files.readAttributes(target, BasicFileAttributes.class);
                        if (!attrs.lastModifiedTime().equals(targetAttrs.lastModifiedTime())
                                || attrs.size() != targetAttrs.size()) {
                            modified.add(relative.toString());
                        }
                    } catch (IOException e) {
                        modified.add(relative.toString());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        Files.walkFileTree(dir2, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (exclude != null && file.toString().matches(exclude)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relative = dir2.relativize(file);
                Path target = dir1.resolve(relative);

                if (!Files.exists(target)) {
                    onlyInDir2.add(relative.toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        if (!onlyInDir1.isEmpty()) {
            System.out.println("Only in " + dir1 + ":");
            onlyInDir1.forEach(f -> System.out.println("  - " + f));
            System.out.println();
        }

        if (!onlyInDir2.isEmpty()) {
            System.out.println("Only in " + dir2 + ":");
            onlyInDir2.forEach(f -> System.out.println("  + " + f));
            System.out.println();
        }

        if (!modified.isEmpty()) {
            System.out.println("Modified:");
            modified.forEach(f -> System.out.println("  M " + f));
            System.out.println();
        }

        if (onlyInDir1.isEmpty() && onlyInDir2.isEmpty() && modified.isEmpty()) {
            Logger.info("Directories are identical");
        }

        return 0;
    }
}
