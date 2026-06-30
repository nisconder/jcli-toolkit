package com.jcli.fileops;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStatCommandTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private PrintStream origOut;
    private PrintStream origErr;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        origOut = System.out;
        origErr = System.err;
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    void tearDown() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    private String output() {
        return out.toString();
    }

    private void createFile(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }

    @Test
    void testStatNonRecursive() throws Exception {
        createFile(tempDir, "a.txt", "hello");
        createFile(tempDir, "b.java", "class B {}");
        createFile(tempDir, "sub/c.txt", "nested");

        FileStatCommand cmd = new FileStatCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString()});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("Total Files: 2"), "non-recursive should count only 2 top-level files");
        assertTrue(out.contains("txt"), "should list txt extension");
        assertTrue(out.contains("java"), "should list java extension");
    }

    @Test
    void testStatRecursive() throws Exception {
        createFile(tempDir, "a.txt", "hello");
        createFile(tempDir, "sub/b.txt", "nested");

        FileStatCommand cmd = new FileStatCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--recursive"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("Total Files: 2"), "recursive should count 2 files");
    }

    @Test
    void testOutputJson() throws Exception {
        createFile(tempDir, "a.txt", "hello");

        FileStatCommand cmd = new FileStatCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--output", "json"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.trim().startsWith("{"), "json output should start with {");
        assertTrue(out.contains("\"totalFiles\""), "json should contain totalFiles");
        assertTrue(out.contains("\"totalSize\""), "json should contain totalSize");
        assertTrue(out.contains("\"byExtension\""), "json should contain byExtension");
    }

    @Test
    void testStatNonExistentDir() throws Exception {
        FileStatCommand cmd = new FileStatCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.resolve("nope").toString()});
        assertEquals(1, rc);
    }
}