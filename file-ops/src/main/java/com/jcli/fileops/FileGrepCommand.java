package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileGrepCommand implements CliCommand {
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
        CommandLineParser parser = new CommandLineParser("jcli file grep")
                .description("Search for patterns in file contents")
                .addOption(CommandLineParser.Option.ofValue("p", "pattern", "Pattern to search (regex)"))
                .addOption(CommandLineParser.Option.ofValue("d", "dir", "Directory to search"))
                .addOption(CommandLineParser.Option.ofValue("e", "ext", "File extension filter"))
                .addOption(CommandLineParser.Option.of("i", "ignore-case", "Case insensitive search"))
                .addOption(CommandLineParser.Option.ofValue("c", "context", "Number of context lines"))
                .addOption(CommandLineParser.Option.ofValue("x", "exclude", "Exclude pattern"))
                .addOption(CommandLineParser.Option.of("v", "verbose", "Verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String pattern = cmdLine.getOptionValue(parser.getLongOption("pattern"));
        if (pattern == null) {
            Logger.error("Pattern is required");
            return 1;
        }

        String dir = cmdLine.getOptionValue(parser.getLongOption("dir"), ".");
        String ext = cmdLine.getOptionValue(parser.getLongOption("ext"));
        boolean ignoreCase = cmdLine.hasOption(parser.getShortOption("i"));
        String contextStr = cmdLine.getOptionValue(parser.getLongOption("context"), "0");
        String exclude = cmdLine.getOptionValue(parser.getLongOption("exclude"));
        boolean verbose = cmdLine.hasOption(parser.getShortOption("v"));

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

    record MatchResult(String file, int line, String content, List<String> contextLines) {}
}
