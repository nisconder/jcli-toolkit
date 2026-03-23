package com.jcli.core;

import java.util.*;
import java.util.stream.Collectors;

public class CommandLineParser {
    private final String programName;
    private final Map<String, Option> shortOptions = new HashMap<>();
    private final Map<String, Option> longOptions = new HashMap<>();
    private final List<String> positionalArgs = new ArrayList<>();
    private String version;
    private String description;

    public CommandLineParser(String programName) {
        this.programName = programName;
    }

    public CommandLineParser description(String description) {
        this.description = description;
        return this;
    }

    public CommandLineParser version(String version) {
        this.version = version;
        return this;
    }

    public CommandLineParser addOption(Option option) {
        if (option.shortName() != null) {
            shortOptions.put(option.shortName(), option);
        }
        if (option.longName() != null) {
            longOptions.put(option.longName(), option);
        }
        return this;
    }

    public CommandLineParser addPositionalArg(String name) {
        positionalArgs.add(name);
        return this;
    }

    public Option getLongOption(String longName) {
        return longOptions.get(longName);
    }

    public Option getShortOption(String shortName) {
        return shortOptions.get(shortName);
    }

    public CommandLine parse(String[] args) {
        CommandLine commandLine = new CommandLine();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("--help") || arg.equals("-h")) {
                commandLine.setShowHelp(true);
                return commandLine;
            }
            
            if (arg.equals("--version") || arg.equals("-V")) {
                commandLine.setShowVersion(true);
                return commandLine;
            }
            
            if (arg.startsWith("--")) {
                String longName = arg.substring(2);
                Option option = longOptions.get(longName);
                if (option != null) {
                    processOption(commandLine, option, args, i);
                    if (option.hasValue()) i++;
                } else {
                    throw new IllegalArgumentException("Unknown option: --" + longName);
                }
            } else if (arg.startsWith("-")) {
                String shortName = arg.substring(1);
                Option option = shortOptions.get(shortName);
                if (option != null) {
                    processOption(commandLine, option, args, i);
                    if (option.hasValue()) i++;
                } else {
                    throw new IllegalArgumentException("Unknown option: -" + shortName);
                }
            } else {
                commandLine.addPositionalArg(arg);
            }
        }
        
        return commandLine;
    }

    private void processOption(CommandLine commandLine, Option option, String[] args, int index) {
        if (option.hasValue()) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Option " + option.name() + " requires a value");
            }
            commandLine.addOptionValue(option, args[index + 1]);
        } else {
            commandLine.setOptionPresent(option);
        }
    }

    public void printHelp() {
        System.out.println(programName);
        if (description != null) {
            System.out.println(description);
        }
        System.out.println();
        System.out.println("Usage: " + programName + " [options]");
        
        if (!positionalArgs.isEmpty()) {
            System.out.print("Arguments:");
            for (String arg : positionalArgs) {
                System.out.print(" <" + arg + ">");
            }
            System.out.println();
        }
        
        System.out.println();
        System.out.println("Options:");
        
        List<Option> options = new ArrayList<>(new HashSet<>(shortOptions.values()));
        options.addAll(longOptions.values());
        options = options.stream().distinct().collect(Collectors.toList());
        
        int maxLen = options.stream()
                .mapToInt(opt -> {
                    int len = 0;
                    if (opt.shortName() != null) len += 4;
                    if (opt.longName() != null) len += opt.longName().length() + 4;
                    return len;
                })
                .max()
                .orElse(20);
        
        for (Option option : options) {
            StringBuilder sb = new StringBuilder("  ");
            
            if (option.shortName() != null) {
                sb.append("-").append(option.shortName());
                if (option.longName() != null) sb.append(", ");
            }
            
            if (option.longName() != null) {
                sb.append("--").append(option.longName());
                if (option.hasValue()) sb.append(" <value>");
            }
            
            while (sb.length() < maxLen + 4) {
                sb.append(" ");
            }
            
            if (option.description() != null) {
                sb.append(option.description());
            }
            
            System.out.println(sb);
        }
    }

    public void printVersion() {
        System.out.println(programName + " " + (version != null ? version : "unknown"));
    }

    public record Option(String name, String shortName, String longName, String description, boolean hasValue) {
        public static Option of(String shortName, String longName, String description) {
            return new Option(longName != null ? longName : shortName, shortName, longName, description, false);
        }
        
        public static Option ofValue(String shortName, String longName, String description) {
            return new Option(longName != null ? longName : shortName, shortName, longName, description, true);
        }
    }
}
