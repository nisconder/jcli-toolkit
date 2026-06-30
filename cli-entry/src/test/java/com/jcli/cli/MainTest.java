package com.jcli.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import picocli.CommandLine;

/**
 * Tests for the Picocli command dispatch layer of {@link JcliCli}.
 *
 * <p>All tests drive the command tree through {@link CommandLine#execute(String...)},
 * which returns the exit code directly without terminating the JVM.
 */
class MainTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedOut;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void replaceStreams() {
        originalOut = System.out;
        originalErr = System.err;
        capturedOut = new ByteArrayOutputStream();
        capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String out() {
        return capturedOut.toString();
    }

    private String err() {
        return capturedErr.toString();
    }

    @Test
    void testHelp() {
        int exitCode = new CommandLine(new JcliCli()).execute("--help");
        assertEquals(0, exitCode, "--help should exit with code 0");
        assertTrue(out().contains("Usage:"), "help output should contain usage header");
        assertTrue(out().contains("jcli"), "help output should contain command name");
    }

    @Test
    void testVersion() {
        int exitCode = new CommandLine(new JcliCli()).execute("--version");
        assertEquals(0, exitCode, "--version should exit with code 0");
        assertTrue(out().contains("JCLI Toolkit v1.0.0"),
                "version output should contain 'JCLI Toolkit v1.0.0'");
    }

    @Test
    void testNoArgs() {
        int exitCode = new CommandLine(new JcliCli()).execute();
        // No subcommand -> JcliCli.run() prints usage; Picocli returns 0.
        assertEquals(0, exitCode, "no args should not crash");
        assertTrue(out().contains("Usage:"), "no args should print usage");
    }

    @Test
    void testUnknownCommand() {
        int exitCode = new CommandLine(new JcliCli()).execute("nosuchcommand");
        // Picocli default exit code for unknown commands is 2.
        assertEquals(2, exitCode, "unknown command should return exit code 2");
    }

    @Test
    void testFileFindHelp() {
        int exitCode = new CommandLine(new JcliCli()).execute("file", "find", "--help");
        assertEquals(0, exitCode, "file find --help should exit with code 0");
        assertTrue(out().contains("Usage:"), "file find --help should print usage");
        assertTrue(out().contains("find"),
                "file find --help should mention the find command");
    }

    @Test
    void testFileGrepHelp() {
        int exitCode = new CommandLine(new JcliCli()).execute("file", "grep", "--help");
        assertEquals(0, exitCode, "file grep --help should exit with code 0");
        assertTrue(out().contains("Usage:"), "file grep --help should print usage");
        assertTrue(out().contains("grep"),
                "file grep --help should mention the grep command");
    }

    @Test
    void testFileFindCommand() {
        int exitCode = new CommandLine(new JcliCli()).execute(
                "file", "find", "--dir", ".", "--ext", "java");
        assertEquals(0, exitCode, "file find --dir . --ext java should dispatch and run");
        // The find command prints matching file paths to stdout; just ensure it ran
        // without crashing. Output may be empty if no java files match, so we only
        // assert the exit code here.
    }

    @Test
    void testGenClassHelp() {
        int exitCode = new CommandLine(new JcliCli()).execute("gen", "class", "--help");
        assertEquals(0, exitCode, "gen class --help should exit with code 0");
        assertTrue(out().contains("Usage:"), "gen class --help should print usage");
        assertTrue(out().contains("class"),
                "gen class --help should mention the class command");
    }

    @Test
    void testTemplateHelp() {
        int exitCode = new CommandLine(new JcliCli()).execute("template", "--help");
        assertEquals(0, exitCode, "template --help should exit with code 0");
        assertTrue(out().contains("Usage:"), "template --help should print usage");
        assertTrue(out().contains("template"),
                "template --help should mention the template command");
    }

    /**
     * Loading the {@link Main} class triggers its static initializer, which exercises
     * {@code Main.createJcliDirectories()} and {@code Main.loadPlugins()}.
     */
    @Test
    void testMainStaticInitializerLoads() throws Exception {
        Class<?> mainClass = Class.forName("com.jcli.cli.Main");
        Class.forName("com.jcli.cli.Main", true, mainClass.getClassLoader());
        assertEquals("com.jcli.cli.Main", mainClass.getName(),
                "Main class should load and initialize without error");
    }

    @Test
    void testFileParentCommand() {
        int exitCode = new CommandLine(new JcliCli()).execute("file");
        assertEquals(0, exitCode, "'file' with no subcommand should run FileCommand.run()");
        assertTrue(out().contains("File operations"),
                "file parent command should print its banner");
    }

    @Test
    void testGenParentCommand() {
        int exitCode = new CommandLine(new JcliCli()).execute("gen");
        assertEquals(0, exitCode, "'gen' with no subcommand should run GenCommand.run()");
        assertTrue(out().contains("Code generation"),
                "gen parent command should print its banner");
    }

    /**
     * Drives {@link Main#main(String[])} with {@code --help}. In this codebase
     * {@code Main.main} delegates to {@link CommandLine#execute(String...)} and returns
     * normally (it does not terminate the JVM), so this is safe to invoke from a test.
     */
    @Test
    void testMainHelpViaEntryPoint() {
        Main.main(new String[]{"--help"});
        assertTrue(out().contains("Usage:"),
                "Main.main(--help) should print usage output");
    }

    @Test
    void testMainNoArgsViaEntryPoint() {
        Main.main(new String[]{});
        assertTrue(out().contains("Usage:"),
                "Main.main() with no args should print usage output");
    }

    @Test
    void testMainVerboseFlagParsed() {
        Main.main(new String[]{"--verbose", "--help"});
        assertTrue(out().contains("Usage:"),
                "Main.main(--verbose --help) should still print usage output");
    }

    @Test
    void testMainQuietFlagParsed() {
        Main.main(new String[]{"--quiet", "--help"});
        assertTrue(out().contains("Usage:"),
                "Main.main(--quiet --help) should still print usage output");
    }
}