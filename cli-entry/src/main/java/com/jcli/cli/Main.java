package com.jcli.cli;

import com.jcli.codegen.*;
import com.jcli.core.*;
import com.jcli.fileops.*;

import java.util.*;

public class Main {
    private static final String VERSION = "1.0.0";
    private static final Map<String, CliCommand> commands = new HashMap<>();
    private static final Map<String, Map<String, CliCommand>> subcommands = new HashMap<>();

    static {
        registerCommand("file", null, "File operations");
        registerSubCommand("file", "find", new FileFindCommand());
        registerSubCommand("file", "stat", new FileStatCommand());
        registerSubCommand("file", "rename", new FileRenameCommand());
        registerSubCommand("file", "grep", new FileGrepCommand());
        registerSubCommand("file", "sync", new FileSyncCommand());
        registerSubCommand("file", "diff", new FileDiffCommand());

        registerCommand("gen", null, "Code generation");
        registerSubCommand("gen", "class", new GenClassCommand());
        registerSubCommand("gen", "project", new GenProjectCommand());
        registerSubCommand("gen", "snippet", new GenSnippetCommand());

        registerCommand("template", new TemplateCommand(), "Template management");

        loadPlugins();
    }

    private static void registerCommand(String name, CliCommand command, String description) {
        if (command != null) {
            commands.put(name, command);
        }
    }

    private static void registerSubCommand(String command, String subcommand, CliCommand handler) {
        subcommands.computeIfAbsent(command, k -> new HashMap<>()).put(subcommand, handler);
    }

    private static void loadPlugins() {
        try {
            ServiceLoader<CliCommand> loader = ServiceLoader.load(CliCommand.class);
            for (CliCommand command : loader) {
                commands.put(command.name(), command);
            }
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            printHelp();
            System.exit(0);
        }

        if (args[0].equals("--version") || args[0].equals("-V")) {
            printVersion();
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--verbose") || args[i].equals("-v")) {
                Logger.setVerbose(true);
            } else if (args[i].equals("--quiet") || args[i].equals("-q")) {
                Logger.setQuiet(true);
            }
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            int exitCode;
            
            if (subcommands.containsKey(command) && commandArgs.length > 0) {
                String subcommand = commandArgs[0];
                Map<String, CliCommand> subMap = subcommands.get(command);
                
                if (subMap.containsKey(subcommand)) {
                    String[] subArgs = Arrays.copyOfRange(commandArgs, 1, commandArgs.length);
                    exitCode = subMap.get(subcommand).execute(subArgs);
                } else {
                    Logger.error("Unknown subcommand: " + command + " " + subcommand);
                    printSubcommandHelp(command);
                    exitCode = 1;
                }
            } else if (commands.containsKey(command)) {
                exitCode = commands.get(command).execute(commandArgs);
            } else {
                Logger.error("Unknown command: " + command);
                printHelp();
                exitCode = 1;
            }
            
            System.exit(exitCode);
        } catch (Exception e) {
            Logger.error("Error executing command: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printHelp() {
        System.out.println("JCLI Toolkit v" + VERSION);
        System.out.println("Java CLI Toolkit for Developers");
        System.out.println();
        System.out.println("Usage: jcli <command> [subcommand] [options] [args]");
        System.out.println();
        System.out.println("Commands:");

        List<String> sortedCommands = new ArrayList<>(commands.keySet());
        sortedCommands.addAll(subcommands.keySet());
        sortedCommands = new ArrayList<>(new TreeSet<>(sortedCommands));

        for (String cmd : sortedCommands) {
            System.out.printf("  %-12s %s%n", cmd, getCommandDescription(cmd));
        }

        System.out.println();
        System.out.println("Global Options:");
        System.out.println("  -v, --verbose  Verbose output");
        System.out.println("  -q, --quiet    Quiet mode");
        System.out.println("  -h, --help     Show help");
        System.out.println("  -V, --version  Show version");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  jcli file find --ext java --dir ./src");
        System.out.println("  jcli file stat --dir ./src");
        System.out.println("  jcli gen class --name UserService --template pojo");
        System.out.println("  jcli gen project --name my-app --template maven-library");
    }

    private static void printSubcommandHelp(String command) {
        System.out.println("JCLI Toolkit v" + VERSION + " - " + command);
        System.out.println();
        System.out.println("Subcommands:");
        
        Map<String, CliCommand> subMap = subcommands.get(command);
        for (Map.Entry<String, CliCommand> entry : subMap.entrySet()) {
            System.out.printf("  %-12s %s%n", entry.getKey(), entry.getValue().description());
        }
    }

    private static String getCommandDescription(String command) {
        if (subcommands.containsKey(command)) {
            return "Subcommands available";
        }
        CliCommand cmd = commands.get(command);
        return cmd != null ? cmd.description() : "";
    }

    private static void printVersion() {
        System.out.println("JCLI Toolkit v" + VERSION);
        System.out.println("Java " + System.getProperty("java.version"));
    }
}
