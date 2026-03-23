package com.jcli.core;

import java.util.*;

public class CommandLine {
    private final Map<CommandLineParser.Option, String> optionValues = new HashMap<>();
    private final Set<CommandLineParser.Option> presentOptions = new HashSet<>();
    private final List<String> positionalArgs = new ArrayList<>();
    private boolean showHelp = false;
    private boolean showVersion = false;

    public void addOptionValue(CommandLineParser.Option option, String value) {
        optionValues.put(option, value);
    }

    public void setOptionPresent(CommandLineParser.Option option) {
        presentOptions.add(option);
    }

    public void addPositionalArg(String arg) {
        positionalArgs.add(arg);
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean hasOption(CommandLineParser.Option option) {
        return presentOptions.contains(option) || optionValues.containsKey(option);
    }

    public String getOptionValue(CommandLineParser.Option option) {
        return optionValues.get(option);
    }

    public String getOptionValue(CommandLineParser.Option option, String defaultValue) {
        return optionValues.getOrDefault(option, defaultValue);
    }

    public List<String> getPositionalArgs() {
        return new ArrayList<>(positionalArgs);
    }

    public String getPositionalArg(int index) {
        if (index >= positionalArgs.size()) {
            throw new IndexOutOfBoundsException("Positional argument index out of range: " + index);
        }
        return positionalArgs.get(index);
    }

    public int getPositionalArgCount() {
        return positionalArgs.size();
    }

    public boolean shouldShowHelp() {
        return showHelp;
    }

    public boolean shouldShowVersion() {
        return showVersion;
    }
}
