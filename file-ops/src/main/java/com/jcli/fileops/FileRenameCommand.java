package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileRenameCommand implements CliCommand {
    // Reuse one scanner in non-console environments to avoid repeatedly allocating scanners.
    private static final Scanner INPUT_SCANNER = new Scanner(System.in);

    @Override
    public String name() {
        return "rename";
    }

    @Override
    public String description() {
        return "Batch rename files";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli file rename")
                .description("Batch rename files")
                .addOption(CommandLineParser.Option.ofValue("d", "dir", "Directory to process"))
                .addOption(CommandLineParser.Option.ofValue("p", "pattern", "Pattern to match (regex)"))
                .addOption(CommandLineParser.Option.ofValue("r", "replace", "Replacement string"))
                .addOption(CommandLineParser.Option.ofValue("x", "prefix", "Add prefix"))
                .addOption(CommandLineParser.Option.ofValue("s", "suffix", "Add suffix"))
                .addOption(CommandLineParser.Option.of("q", "seq", "Add sequence number"))
                .addOption(CommandLineParser.Option.ofValue("n", "seq-start", "Sequence start number"))
                .addOption(CommandLineParser.Option.ofValue("f", "format", "Number format: %d, %02d, %03d"))
                .addOption(CommandLineParser.Option.of("u", "uppercase", "Convert to uppercase"))
                .addOption(CommandLineParser.Option.of("l", "lowercase", "Convert to lowercase"))
                .addOption(CommandLineParser.Option.of("y", "dry-run", "Preview changes without executing"))
                .addOption(CommandLineParser.Option.of("c", "confirm", "Confirm before renaming"))
                .addOption(CommandLineParser.Option.of("v", "verbose", "Verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String dir = cmdLine.getOptionValue(parser.getLongOption("dir"), ".");
        String pattern = cmdLine.getOptionValue(parser.getLongOption("pattern"));
        String replace = cmdLine.getOptionValue(parser.getLongOption("replace"), "");
        String prefix = cmdLine.getOptionValue(parser.getLongOption("prefix"));
        String suffix = cmdLine.getOptionValue(parser.getLongOption("suffix"));
        boolean addSeq = cmdLine.hasOption(parser.getShortOption("q"));
        String seqStartStr = cmdLine.getOptionValue(parser.getLongOption("seq-start"), "1");
        String numberFormat = cmdLine.getOptionValue(parser.getLongOption("format"), "%d");
        boolean toUpper = cmdLine.hasOption(parser.getShortOption("u"));
        boolean toLower = cmdLine.hasOption(parser.getShortOption("l"));
        boolean dryRun = cmdLine.hasOption(parser.getShortOption("y"));
        boolean confirm = cmdLine.hasOption(parser.getShortOption("c"));
        boolean verbose = cmdLine.hasOption(parser.getShortOption("v"));

        if (verbose) {
            Logger.setVerbose(true);
        }

        Path startDir = Paths.get(dir);
        if (!Files.exists(startDir)) {
            Logger.error("Directory does not exist: " + dir);
            return 1;
        }

        if (pattern == null) {
            Logger.error("Pattern is required");
            return 1;
        }

        Pattern renamePattern;
        try {
            renamePattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            Logger.error("Invalid regex pattern: " + e.getMessage());
            return 1;
        }

        int seqStart;
        try {
            seqStart = Integer.parseInt(seqStartStr);
        } catch (NumberFormatException e) {
            Logger.error("Invalid seq-start value: " + seqStartStr);
            return 1;
        }

        // Validate format eagerly to avoid failing in the middle of operation planning.
        try {
            String.format(numberFormat, seqStart);
        } catch (RuntimeException e) {
            Logger.error("Invalid format value: " + numberFormat);
            return 1;
        }

        List<RenameOp> operations = new ArrayList<>();
        AtomicBoolean hasUnsafeName = new AtomicBoolean(false);
        AtomicInteger seqNumber = new AtomicInteger(seqStart);

        try (var stream = Files.list(startDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String oldName = path.getFileName().toString();
                        String newName = oldName;

                        Matcher m = renamePattern.matcher(oldName);
                        newName = m.replaceAll(replace);

                        if (prefix != null) {
                            newName = prefix + newName;
                        }

                        if (suffix != null) {
                            int dotIndex = newName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                newName = newName.substring(0, dotIndex) + suffix + newName.substring(dotIndex);
                            } else {
                                newName = newName + suffix;
                            }
                        }

                        if (addSeq) {
                            int dotIndex = newName.lastIndexOf('.');
                            String seq = String.format(numberFormat, seqNumber.getAndIncrement());
                            if (dotIndex > 0) {
                                newName = newName.substring(0, dotIndex) + seq + newName.substring(dotIndex);
                            } else {
                                newName = newName + seq;
                            }
                        }

                        if (toUpper) {
                            newName = newName.toUpperCase();
                        } else if (toLower) {
                            newName = newName.toLowerCase();
                        }

                        // Block path traversal and path separator injection in target file name.
                        if (newName.contains("/") || newName.contains("\\") || newName.equals(".") || newName.equals("..")) {
                            Logger.error("Unsafe target filename generated for " + oldName + ": " + newName);
                            hasUnsafeName.set(true);
                            return;
                        }

                        if (!newName.equals(oldName)) {
                            operations.add(new RenameOp(path, startDir.resolve(newName)));
                        }
                    });
        }

        if (hasUnsafeName.get()) {
            Logger.error("Aborted due to unsafe target filename");
            return 1;
        }

        if (operations.isEmpty()) {
            Logger.info("No files to rename");
            return 0;
        }

        Logger.info("Found " + operations.size() + " files to rename:");

        Set<Path> seenTargets = new HashSet<>();
        Set<Path> sourcePaths = new HashSet<>();
        for (RenameOp op : operations) {
            sourcePaths.add(op.oldPath());
        }

        for (RenameOp op : operations) {
            Path normalizedTarget = op.newPath().normalize();

            if (!normalizedTarget.getParent().equals(startDir.normalize())) {
                Logger.error("Unsafe target path outside working directory: " + normalizedTarget);
                return 1;
            }

            if (!seenTargets.add(normalizedTarget)) {
                Logger.error("Target filename collision detected: " + normalizedTarget.getFileName());
                return 1;
            }

            if (Files.exists(normalizedTarget) && !sourcePaths.contains(normalizedTarget)) {
                Logger.error("Target already exists, refusing to overwrite: " + normalizedTarget.getFileName());
                return 1;
            }

            System.out.println("  " + op.oldPath().getFileName() + " -> " + op.newPath().getFileName());
        }

        if (dryRun) {
            Logger.info("Dry run mode - no changes made");
            return 0;
        }

        if (confirm) {
            System.out.print("Proceed with renaming? (y/n): ");
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
        for (RenameOp op : operations) {
            try {
                Files.move(op.oldPath(), op.newPath());
                if (verbose) {
                    Logger.info("Renamed: " + op.oldPath().getFileName() + " -> " + op.newPath().getFileName());
                }
                successCount++;
            } catch (IOException e) {
                Logger.error("Failed to rename: " + op.oldPath() + " - " + e.getMessage());
                failCount++;
            }
        }

        Logger.info("Renamed " + successCount + " files successfully");
        if (failCount > 0) {
            Logger.warn("Failed to rename " + failCount + " files");
            return 1;
        }

        return 0;
    }

    record RenameOp(Path oldPath, Path newPath) {}
}
