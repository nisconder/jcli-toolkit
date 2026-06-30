package com.jcli.cli;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Main {

    private static final List<CliCommand> PLUGIN_COMMANDS = new ArrayList<>();

    static {
        createJcliDirectories();
        loadPlugins();
    }

    private Main() {
    }

    private static void createJcliDirectories() {
        try {
            Path jcliHome = Paths.get(System.getProperty("user.home"), ".jcli");
            Files.createDirectories(jcliHome.resolve("plugins"));
            Files.createDirectories(jcliHome.resolve("templates"));
        } catch (IOException e) {
            Logger.warn("Failed to create ~/.jcli/ directories: " + e.getMessage());
        }
    }

    private static void loadPlugins() {
        // Load ServiceLoader plugins (from classpath)
        try {
            ServiceLoader<CliCommand> loader = ServiceLoader.load(CliCommand.class);
            for (CliCommand command : loader) {
                PLUGIN_COMMANDS.add(command);
                Logger.verbose("Loaded plugin: " + command.name());
            }
        } catch (Exception e) {
            Logger.warn("Failed to load ServiceLoader plugins: " + e.getMessage());
        }

        // Load external plugins from ~/.jcli/plugins/
        Path pluginsDir = Paths.get(System.getProperty("user.home"), ".jcli", "plugins");
        if (Files.isDirectory(pluginsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
                for (Path jarPath : stream) {
                    try {
                        URLClassLoader classLoader = new URLClassLoader(
                            new URL[]{jarPath.toUri().toURL()},
                            Main.class.getClassLoader()
                        );
                        ServiceLoader<CliCommand> pluginLoader = ServiceLoader.load(CliCommand.class, classLoader);
                        for (CliCommand command : pluginLoader) {
                            PLUGIN_COMMANDS.add(command);
                            Logger.verbose("Loaded plugin from " + jarPath.getFileName() + ": " + command.name());
                        }
                    } catch (Exception e) {
                        Logger.warn("Failed to load plugin " + jarPath.getFileName() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.warn("Failed to scan plugins directory: " + e.getMessage());
            }
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