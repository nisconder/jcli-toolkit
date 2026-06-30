package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "diff", description = "Compare directories", mixinStandardHelpOptions = true)
public class FileDiffCommand implements CliCommand, Callable<Integer> {
    @Option(names = {"--dir1"}, description = "First directory", required = true)
    private String dir1Str;

    @Option(names = {"--dir2"}, description = "Second directory", required = true)
    private String dir2Str;

    @Option(names = {"-x", "--exclude"}, description = "Exclude pattern")
    private String exclude;

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
        return new picocli.CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
        Path dir1 = Paths.get(dir1Str);
        Path dir2 = Paths.get(dir2Str);

        if (!Files.exists(dir1) || !Files.exists(dir2)) {
            Logger.error("One or both directories do not exist");
            return 1;
        }

        List<String> onlyInDir1 = new ArrayList<>();
        List<String> onlyInDir2 = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        try {
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
        } catch (IOException e) {
            Logger.error("Failed to compare directories: " + e.getMessage());
            return 1;
        }

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