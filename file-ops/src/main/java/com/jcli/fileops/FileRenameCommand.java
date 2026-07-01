package com.jcli.fileops;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "rename", description = "Batch rename files", mixinStandardHelpOptions = true)
public class FileRenameCommand implements CliCommand, Callable<Integer> {
    @Option(names = {"-d", "--dir"}, description = "Directory to process", defaultValue = ".")
    private String dir;

    @Option(names = {"-p", "--pattern"}, description = "Pattern to match (regex)", required = true)
    private String pattern;

    @Option(names = {"-r", "--replace"}, description = "Replacement string", defaultValue = "")
    private String replace;

    @Option(names = {"-x", "--prefix"}, description = "Add prefix")
    private String prefix;

    @Option(names = {"-s", "--suffix"}, description = "Add suffix")
    private String suffix;

    @Option(names = {"-q", "--seq"}, description = "Add sequence number")
    private boolean addSeq;

    @Option(names = {"-n", "--seq-start"}, description = "Sequence start number", defaultValue = "1")
    private String seqStartStr;

    @Option(names = {"-f", "--format"}, description = "Number format: %d, %02d, %03d", defaultValue = "%d")
    private String numberFormat;

    @Option(names = {"-u", "--uppercase"}, description = "Convert to uppercase")
    private boolean toUpper;

    @Option(names = {"-l", "--lowercase"}, description = "Convert to lowercase")
    private boolean toLower;

    @Option(names = {"-y", "--dry-run"}, description = "Preview changes without executing")
    private boolean dryRun;

    @Option(names = {"-c", "--confirm"}, description = "Confirm before renaming")
    private boolean confirm;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

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

                        if (newName.contains("/") || newName.contains("\\") || newName.equals(".") || newName.equals("..")) {
                            Logger.error("Unsafe target filename generated for " + oldName + ": " + newName);
                            hasUnsafeName.set(true);
                            return;
                        }

                        if (!newName.equals(oldName)) {
                            operations.add(new RenameOp(path, startDir.resolve(newName)));
                        }
                    });
        } catch (IOException e) {
            Logger.error("Failed to list directory: " + e.getMessage());
            return 1;
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

        Path absStartDir = startDir.toAbsolutePath().normalize();
        for (RenameOp op : operations) {
            Path normalizedTarget = op.newPath().normalize();

            Path parent = normalizedTarget.getParent();
            // null parent means a single-component filename (e.g. "build.groovy") — implicitly in current dir
            if (parent != null && !parent.equals(absStartDir)) {
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
                try (Scanner scanner = new Scanner(System.in)) {
                    response = scanner.nextLine();
                }
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

    record RenameOp(Path oldPath, Path newPath) {
    }
}