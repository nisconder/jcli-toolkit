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

class FileDiffCommandTest {

    @TempDir
    Path tempDir;

    private Path dir1;
    private Path dir2;

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private PrintStream origOut;
    private PrintStream origErr;

    @BeforeEach
    void setUp() throws IOException {
        dir1 = tempDir.resolve("dir1");
        dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

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

    private void createIdentical(String name, String content) throws IOException {
        Path p1 = dir1.resolve(name);
        Path p2 = dir2.resolve(name);
        Files.createDirectories(p1.getParent());
        Files.createDirectories(p2.getParent());
        Files.writeString(p1, content);
        Files.writeString(p2, content);
        // identical mtime + size → not modified
        FileTime t = FileTime.fromMillis(System.currentTimeMillis());
        Files.setLastModifiedTime(p1, t);
        Files.setLastModifiedTime(p2, t);
    }

    @Test
    void testIdenticalDirs() throws Exception {
        createIdentical("a.txt", "same content");
        createIdentical("b.txt", "same b content");

        FileDiffCommand cmd = new FileDiffCommand();
        int rc = cmd.execute(new String[]{"--dir1", dir1.toString(), "--dir2", dir2.toString()});

        assertEquals(0, rc);
        String errOut = err.toString();
        assertTrue(errOut.contains("Directories are identical"), "should report identical dirs");
        String stdout = out.toString();
        assertFalse(stdout.contains("Only in"), "no differences should be reported");
        assertFalse(stdout.contains("Modified"), "no modifications should be reported");
    }

    @Test
    void testWithDifferences() throws Exception {
        // file only in dir1
        Files.writeString(dir1.resolve("only1.txt"), "x");
        // file only in dir2
        Files.writeString(dir2.resolve("only2.txt"), "y");
        // modified file (different content/size)
        Files.writeString(dir1.resolve("mod.txt"), "version1");
        Files.writeString(dir2.resolve("mod.txt"), "version2-longer");

        FileDiffCommand cmd = new FileDiffCommand();
        int rc = cmd.execute(new String[]{"--dir1", dir1.toString(), "--dir2", dir2.toString()});

        assertEquals(0, rc);
        String stdout = out.toString();
        assertTrue(stdout.contains("only1.txt"), "should report file only in dir1");
        assertTrue(stdout.contains("only2.txt"), "should report file only in dir2");
        assertTrue(stdout.contains("mod.txt"), "should report modified file");
    }

    @Test
    void testNonExistentDir() throws Exception {
        FileDiffCommand cmd = new FileDiffCommand();
        int rc = cmd.execute(new String[]{
                "--dir1", tempDir.resolve("nope1").toString(),
                "--dir2", dir2.toString()
        });
        assertEquals(1, rc);
    }
}