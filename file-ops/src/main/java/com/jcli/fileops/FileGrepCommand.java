package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Command(name = "grep", description = "Search file contents", mixinStandardHelpOptions = true)
public class FileGrepCommand implements CliCommand {
    @Option(names = {"-p", "--pattern"}, description = "Pattern to search (regex)", required = true)
    private String pattern;

    @Option(names = {"-d", "--dir"}, description = "Directory to search", defaultValue = ".")
    private String dir;

    @Option(names = {"-e", "--ext"}, description = "File extension filter")
    private String ext;

    @Option(names = {"-i", "--ignore-case"}, description = "Case insensitive search")
    private boolean ignoreCase;

    @Option(names = {"-c", "--context"}, description = "Number of context lines", defaultValue = "0")
    private String contextStr;

    @Option(names = {"-x", "--exclude"}, description = "Exclude pattern")
    private String exclude;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    @Override
    public String name() {
        return "grep";
    }

    @Override
    public String description() {
        return "Search file contents";
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

        int context = Integer.parseInt(contextStr);

        Path startDir = Paths.get(dir);
        if (!Files.exists(startDir)) {
            Logger.error("Directory does not exist: " + dir);
            return 1;
        }

        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        Pattern regex = Pattern.compile(pattern, flags);

        List<MatchResult> results = new ArrayList<>();

        try {
            Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!Files.isRegularFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (ext != null) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.toLowerCase().endsWith("." + ext.toLowerCase())) {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    if (exclude != null && file.toString().matches(exclude)) {
                        return FileVisitResult.CONTINUE;
                    }

                    searchFile(file, regex, context, results);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Logger.warn("Failed to visit: " + file + " - " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Logger.error("Failed to walk directory: " + e.getMessage());
            return 1;
        }

        if (results.isEmpty()) {
            Logger.info("No matches found");
            return 0;
        }

        for (MatchResult result : results) {
            System.out.println(result.file + ":" + result.line + ": " + result.content);
            for (String ctx : result.contextLines) {
                System.out.println("  " + ctx);
            }
        }

        Logger.info("Found " + results.size() + " matches");
        return 0;
    }

    private void searchFile(Path file, Pattern pattern, int context, List<MatchResult> results) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (pattern.matcher(line).find()) {
                    List<String> contextLines = new ArrayList<>();
                    int start = Math.max(0, i - context);
                    int end = Math.min(lines.size(), i + context + 1);

                    for (int j = start; j < end; j++) {
                        if (j != i) {
                            contextLines.add((j + 1) + ": " + lines.get(j));
                        }
                    }

                    results.add(new MatchResult(
                            file.toAbsolutePath().toString(),
                            i + 1,
                            line.trim(),
                            contextLines
                    ));
                }
            }
        } catch (IOException e) {
            Logger.warn("Failed to read file: " + file + " - " + e.getMessage());
        }
    }

    record MatchResult(String file, int line, String content, List<String> contextLines) {
    }
}