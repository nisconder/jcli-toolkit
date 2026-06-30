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

class FileGrepCommandTest {

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

    private Path createFile(String name, String content) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    @Test
    void testGrepPattern() throws Exception {
        createFile("a.txt", "line one\nTODO: fix this\nline three");
        createFile("b.txt", "nothing here");

        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--pattern", "TODO"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("a.txt"), "should report a.txt");
        assertTrue(out.contains("TODO: fix this"), "should report matching line");
        assertFalse(out.contains("b.txt"), "should not report b.txt");
    }

    @Test
    void testGrepIgnoreCase() throws Exception {
        createFile("a.txt", "todo: lowercase\nTODO: uppercase\n");

        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--pattern", "todo", "--ignore-case"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("todo: lowercase"), "should match lowercase");
        assertTrue(out.contains("TODO: uppercase"), "should match uppercase");
    }

    @Test
    void testGrepWithContext() throws Exception {
        createFile("a.txt", "line1\nline2\nMATCH line3\nline4\nline5");

        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--pattern", "MATCH", "--context", "1"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("MATCH line3"), "should report matching line");
        assertTrue(out.contains("line2"), "should include context line before");
        assertTrue(out.contains("line4"), "should include context line after");
    }

    @Test
    void testGrepExclude() throws Exception {
        createFile("keep.txt", "TODO in keep");
        createFile("skip.txt", "TODO in skip");

        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "TODO",
                "--exclude", ".*skip\\.txt"
        });

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("keep.txt"), "should report keep.txt");
        assertFalse(out.contains("skip.txt"), "should exclude skip.txt");
    }

    @Test
    void testGrepNoMatch() throws Exception {
        createFile("a.txt", "nothing interesting here");

        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--pattern", "ZZZNOTFOUND"});

        assertEquals(0, rc);
        String out = output();
        assertFalse(out.contains("a.txt"), "no matches should produce no file output");
        assertTrue(err.toString().contains("No matches found") || out.isEmpty(),
                "should indicate no matches found");
    }

    @Test
    void testGrepNonExistentDir() throws Exception {
        FileGrepCommand cmd = new FileGrepCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.resolve("nope").toString(), "--pattern", "x"});
        assertEquals(1, rc);
    }
}