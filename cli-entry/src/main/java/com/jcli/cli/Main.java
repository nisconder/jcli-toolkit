package com.jcli.cli;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Main {

    private static final List<CliCommand> PLUGIN_COMMANDS = new ArrayList<>();

    static {
        loadPlugins();
    }

    private Main() {
    }

    private static void loadPlugins() {
        try {
            ServiceLoader<CliCommand> loader = ServiceLoader.load(CliCommand.class);
            for (CliCommand command : loader) {
                PLUGIN_COMMANDS.add(command);
                Logger.verbose("Loaded plugin: " + command.name());
            }
        } catch (Exception e) {
            Logger.warn("Failed to load plugins: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Parse global options first
        for (String arg : args) {
            if (arg.equals("--verbose") || arg.equals("-v")) {
                Logger.setVerbose(true);
            } else if (arg.equals("--quiet") || arg.equals("-q")) {
                Logger.setQuiet(true);
            }
        }

        CommandLine cmd = new CommandLine(new JcliCli());

        // Register plugin entries dynamically (if they implement Runnable)
        for (CliCommand plugin : PLUGIN_COMMANDS) {
            if (plugin instanceof Runnable) {
                cmd.addSubcommand(plugin.name(), (Runnable) plugin);
            }
        }

        cmd.execute(args);
    }
}