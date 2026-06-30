package com.jcli.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLevelTest {

    @Test
    void testLevelOrder() {
        assertEquals(0, LogLevel.INFO.getLevel(), "INFO level should be 0");
        assertEquals(1, LogLevel.WARN.getLevel(), "WARN level should be 1");
        assertEquals(2, LogLevel.ERROR.getLevel(), "ERROR level should be 2");

        assertTrue(LogLevel.INFO.getLevel() < LogLevel.WARN.getLevel(),
                "INFO < WARN");
        assertTrue(LogLevel.WARN.getLevel() < LogLevel.ERROR.getLevel(),
                "WARN < ERROR");
    }
}