package com.jcli.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Reset static state to defaults before each test.
        Logger.setLevel(LogLevel.INFO);
        Logger.setVerbose(false);
        Logger.setQuiet(false);
        // setColorEnabled(false) is gated by isColorSupported() which depends on the
        // environment (CI / terminal). To force a deterministic color-disabled state
        // we rely on the fact that in the test JVM System.console() is typically null,
        // so setColorEnabled(true) becomes a no-op and colorEnabled stays false.
        // We explicitly disable color via the public setter; if the environment would
        // otherwise allow color, this forces it off.
        Logger.setColorEnabled(false);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testInfoToStdErr() {
        Logger.info("hello-info");
        assertFalse(outContent.toString().contains("hello-info"),
                "info() must not write to System.out");
        assertTrue(errContent.toString().contains("hello-info"),
                "info() must write to System.err");
    }

    @Test
    void testWarnToStdErr() {
        Logger.warn("hello-warn");
        assertFalse(outContent.toString().contains("hello-warn"),
                "warn() must not write to System.out");
        assertTrue(errContent.toString().contains("hello-warn"),
                "warn() must write to System.err");
    }

    @Test
    void testErrorToStdErr() {
        Logger.error("hello-error");
        assertFalse(outContent.toString().contains("hello-error"),
                "error() must not write to System.out");
        assertTrue(errContent.toString().contains("hello-error"),
                "error() must write to System.err");
    }

    @Test
    void testVerboseToStdErr() {
        Logger.setVerbose(true);
        Logger.verbose("hello-verbose");
        assertFalse(outContent.toString().contains("hello-verbose"),
                "verbose() must not write to System.out");
        assertTrue(errContent.toString().contains("hello-verbose"),
                "verbose() must write to System.err");
    }

    @Test
    void testJsonToStdOut() {
        Logger.json("{\"key\":\"value\"}");
        assertTrue(outContent.toString().contains("{\"key\":\"value\"}"),
                "json() must write to System.out");
        assertFalse(errContent.toString().contains("{\"key\":\"value\"}"),
                "json() must not write to System.err");
    }

    @Test
    void testSetLevelInfo() {
        Logger.setLevel(LogLevel.INFO);
        Logger.info("info-msg");
        // DEBUG is not represented in LogLevel enum; the lowest level is INFO.
        // With level=INFO, INFO is logged and anything above INFO threshold is skipped
        // when currentLevel > INFO. Here we verify INFO is emitted.
        assertTrue(errContent.toString().contains("info-msg"),
                "INFO level should be logged when currentLevel=INFO");
    }

    @Test
    void testSetLevelWarn() {
        Logger.setLevel(LogLevel.WARN);
        Logger.info("info-skipped");
        Logger.warn("warn-msg");
        assertFalse(errContent.toString().contains("info-skipped"),
                "INFO should be skipped when currentLevel=WARN");
        assertTrue(errContent.toString().contains("warn-msg"),
                "WARN should be logged when currentLevel=WARN");
    }

    @Test
    void testSetVerbose() {
        Logger.setVerbose(true);
        Logger.verbose("verbose-on");
        assertTrue(errContent.toString().contains("verbose-on"),
                "verbose() should emit when verbose is enabled");

        // Toggle off and ensure suppression.
        errContent.reset();
        Logger.setVerbose(false);
        Logger.verbose("verbose-off");
        assertFalse(errContent.toString().contains("verbose-off"),
                "verbose() should be suppressed when verbose is disabled");
    }

    @Test
    void testSetQuiet() {
        Logger.setQuiet(true);
        Logger.info("quiet-info");
        Logger.warn("quiet-warn");
        // error() is not gated by quiet, so it still emits.
        Logger.error("quiet-error");

        assertFalse(errContent.toString().contains("quiet-info"),
                "info() should be suppressed when quiet=true");
        assertFalse(errContent.toString().contains("quiet-warn"),
                "warn() should be suppressed when quiet=true");
        assertTrue(errContent.toString().contains("quiet-error"),
                "error() should NOT be suppressed by quiet=true");
    }

    @Test
    void testColorDisabled() {
        // With colorEnabled=false the output must not contain ANSI escape sequences.
        Logger.setColorEnabled(false);
        Logger.info("plain-msg");
        String err = errContent.toString();
        assertTrue(err.contains("plain-msg"),
                "message should be present in output");
        assertFalse(err.contains("\u001B["),
                "no ANSI escape codes should be present when colorEnabled=false");
    }

    @Test
    void testSetColorEnabledTrueEvaluatesColorSupport() {
        // Setting colorEnabled=true forces evaluation of isColorSupported(),
        // which in turn exercises isCIEnvironment() and isTerminal(). In the
        // test JVM there is no console and (typically) no CI env vars, so the
        // resulting colorEnabled flag is false — but the support-detection
        // code path is still covered.
        Logger.setColorEnabled(true);
        // Regardless of the resulting flag, info() must still emit to stderr.
        Logger.info("color-check-msg");
        assertTrue(errContent.toString().contains("color-check-msg"),
                "info() should still write to System.err after setColorEnabled(true)");
    }

    @Test
    void testPrintProgressOutsideTerminalIsNoop() {
        // Outside a real terminal (System.console()==null in the test JVM),
        // printProgress must not write anything to System.err.
        Logger.printProgress(1, 2, "progress-msg");
        assertFalse(errContent.toString().contains("progress-msg"),
                "printProgress should be a no-op when not attached to a terminal");
    }
}