package com.jcli.codegen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class GenSnippetCommandTest {

    private ByteArrayOutputStream capturedOut;
    private PrintStream originalOut;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private int run(String... args) throws Exception {
        return new GenSnippetCommand().execute(args);
    }

    private String output() {
        return capturedOut.toString();
    }

    @Test
    void testSnippetGetterSetter() throws Exception {
        int rc = run("--type", "getter-setter", "--fields", "id:Long,name:String");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("public Long getId()"), "should generate getId");
        assertTrue(out.contains("public void setId(Long id)"), "should generate setId");
        assertTrue(out.contains("public String getName()"), "should generate getName");
        assertTrue(out.contains("public void setName(String name)"), "should generate setName");
    }

    @Test
    void testSnippetEqualsHashcode() throws Exception {
        int rc = run("--type", "equals-hashcode");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("public boolean equals(Object o)"), "should generate equals");
        assertTrue(out.contains("@Override"), "should have @Override");
        assertTrue(out.contains("public int hashCode()"), "should generate hashCode");
        assertTrue(out.contains("Objects.hash"), "should use Objects.hash");
    }

    @Test
    void testSnippetBuilder() throws Exception {
        int rc = run("--type", "builder");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("public static Builder builder()"), "should generate builder factory");
        assertTrue(out.contains("public static class Builder"), "should generate Builder class");
        assertTrue(out.contains("public MyClass build()"), "should generate build()");
    }

    @Test
    void testSnippetLogger() throws Exception {
        int rc = run("--type", "logger");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("private static final Logger LOGGER"), "should declare LOGGER");
        assertTrue(out.contains("LoggerFactory.getLogger"), "should use LoggerFactory");
        assertTrue(out.contains("LOGGER.info"), "should show info usage");
    }

    @Test
    void testSnippetTryWithResources() throws Exception {
        int rc = run("--type", "try-with-resources");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("try (InputStream is = new FileInputStream"), "should generate try-with-resources");
        assertTrue(out.contains("catch (IOException e)"), "should catch IOException");
    }

    @Test
    void testSnippetGetterSetterWithoutFields() throws Exception {
        int rc = run("--type", "getter-setter");
        assertEquals(0, rc);
        String out = output();
        // Without --fields, the snippet prints a usage hint.
        assertTrue(out.contains("Usage:") || out.contains("--fields"),
                "should print usage hint when no fields provided");
    }

    @Test
    void testSnippetUnknownType() throws Exception {
        int rc = run("--type", "no-such-snippet");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("Unknown snippet type: no-such-snippet"),
                "should report unknown snippet type");
    }

    @Test
    void testSnippetMissingType() throws Exception {
        // --type is required; picocli returns non-zero (USAGE = 2) when missing.
        int rc = run("--fields", "id:Long");
        assertNotEquals(0, rc, "missing required --type should fail");
    }
}