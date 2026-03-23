package com.jcli.core;

public class Logger {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    
    private static LogLevel currentLevel = LogLevel.INFO;
    private static boolean colorEnabled = true;
    private static boolean quiet = false;
    private static boolean verbose = false;

    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }

    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled && isColorSupported();
    }

    public static void setQuiet(boolean quiet) {
        Logger.quiet = quiet;
    }

    public static void setVerbose(boolean verbose) {
        Logger.verbose = verbose;
    }

    private static boolean isColorSupported() {
        return !isCIEnvironment() && isTerminal();
    }

    private static boolean isCIEnvironment() {
        return System.getenv("CI") != null || 
               System.getenv("CONTINUOUS_INTEGRATION") != null ||
               System.getenv("JENKINS_URL") != null;
    }

    private static boolean isTerminal() {
        return System.console() != null;
    }

    public static void info(String message) {
        if (currentLevel.getLevel() <= LogLevel.INFO.getLevel() && !quiet) {
            print(LogLevel.INFO, message, GREEN);
        }
    }

    public static void warn(String message) {
        if (currentLevel.getLevel() <= LogLevel.WARN.getLevel() && !quiet) {
            print(LogLevel.WARN, message, YELLOW);
        }
    }

    public static void error(String message) {
        if (currentLevel.getLevel() <= LogLevel.ERROR.getLevel()) {
            print(LogLevel.ERROR, message, RED);
        }
    }

    public static void verbose(String message) {
        if (verbose && !quiet) {
            print(LogLevel.INFO, message, BLUE);
        }
    }

    public static void json(String json) {
        System.out.println(json);
    }

    private static void print(LogLevel level, String message, String color) {
        String output;
        if (colorEnabled) {
            output = color + message + RESET;
        } else {
            output = message;
        }
        System.out.println(output);
    }

    public static void printProgress(int current, int total, String message) {
        if (!quiet && isTerminal()) {
            double percent = (double) current / total * 100;
            int filled = (int) (percent / 2);
            int empty = 50 - filled;
            StringBuilder bar = new StringBuilder("\r[");
            for (int i = 0; i < filled; i++) bar.append("=");
            for (int i = 0; i < empty; i++) bar.append(" ");
            bar.append(String.format("] %.1f%% %s", percent, message));
            
            if (colorEnabled) {
                System.err.print(BLUE + bar + RESET);
            } else {
                System.err.print(bar);
            }
            
            if (current >= total) {
                System.err.println();
            }
        }
    }
}
