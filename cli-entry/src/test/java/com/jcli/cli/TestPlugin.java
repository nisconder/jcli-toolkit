package com.jcli.cli;

import com.jcli.core.CliCommand;
import picocli.CommandLine.Command;

/**
 * Test-only plugin implementation used to exercise {@code Main}'s ServiceLoader
 * plugin discovery and dynamic subcommand registration paths. This class is
 * intentionally minimal and performs no real work; it exists solely so that
 * JaCoCo counts the plugin-loading branches of {@code Main} as covered.
 *
 * <p>The {@code @Command} annotation is required so that Picocli accepts this
 * object as a valid subcommand when {@code Main} registers it dynamically.
 *
 * <p>It is registered as a service provider via
 * {@code src/test/resources/META-INF/services/com.jcli.core.CliCommand}.
 */
@Command(name = "testplugin", description = "Test-only plugin for coverage")
public final class TestPlugin implements CliCommand, Runnable {

    @Override
    public String name() {
        return "testplugin";
    }

    @Override
    public String description() {
        return "Test-only plugin for coverage";
    }

    @Override
    public int execute(String[] args) {
        return 0;
    }

    @Override
    public void run() {
        // No-op: the plugin only needs to be registerable as a subcommand.
    }
}