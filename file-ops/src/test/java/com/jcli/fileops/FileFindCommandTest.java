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
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class FileFindCommandTest {

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

    private Path createFile(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    @Test
    void testFindByExtension() throws Exception {
        createFile(tempDir, "A.java", "class A {}");
        createFile(tempDir, "B.java", "class B {}");
        createFile(tempDir, "C.txt", "hello");

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--ext", "java"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("A.java"), "should list A.java");
        assertTrue(out.contains("B.java"), "should list B.java");
        assertFalse(out.contains("C.txt"), "should not list C.txt");
    }

    @Test
    void testFindByNamePattern() throws Exception {
        createFile(tempDir, "TestFoo.java", "x");
        createFile(tempDir, "Bar.java", "x");

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--name", "Test*.java"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("TestFoo.java"), "should match TestFoo.java");
        assertFalse(out.contains("Bar.java"), "should not match Bar.java");
    }

    @Test
    void testFindBySize() throws Exception {
        createFile(tempDir, "small.txt", "ab");
        createFile(tempDir, "big.txt", "0123456789".repeat(100));

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--size-gt", "50"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("big.txt"), "big.txt should be found");
        assertFalse(out.contains("small.txt"), "small.txt should not be found");
    }

    @Test
    void testFindWithDepth() throws Exception {
        createFile(tempDir, "top.txt", "top");
        createFile(tempDir, "sub/nested.txt", "nested");

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--ext", "txt", "--depth", "1"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("top.txt"), "top-level file should be found");
        assertFalse(out.contains("nested.txt"), "nested file should be skipped with depth 1");
    }

    @Test
    void testOutputJson() throws Exception {
        createFile(tempDir, "A.java", "class A {}");

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--ext", "java", "--output", "json"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.trim().startsWith("["), "json output should start with [");
        assertTrue(out.contains("\"path\""), "json should contain path field");
        assertTrue(out.contains("\"size\""), "json should contain size field");
        assertTrue(out.contains("\"lastModified\""), "json should contain lastModified field");
        assertTrue(out.contains("A.java"), "json should contain A.java path");
    }

    @Test
    void testOutputCsv() throws Exception {
        createFile(tempDir, "A.java", "class A {}");

        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.toString(), "--ext", "java", "--output", "csv"});

        assertEquals(0, rc);
        String out = output();
        assertTrue(out.startsWith("path,size,modified"), "csv should start with header");
        assertTrue(out.contains("A.java"), "csv should contain A.java row");
    }

    @Test
    void testNonExistentDir() throws Exception {
        FileFindCommand cmd = new FileFindCommand();
        int rc = cmd.execute(new String[]{"--dir", tempDir.resolve("nonexistent").toString()});
        assertEquals(1, rc);
    }
}