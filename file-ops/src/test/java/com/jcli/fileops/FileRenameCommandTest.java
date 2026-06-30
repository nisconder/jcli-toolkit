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

class FileRenameCommandTest {

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

    private Path createFile(String name, String content) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    @Test
    void testDryRun() throws Exception {
        Path a = createFile("a.txt", "content-a");
        Path b = createFile("b.txt", "content-b");

        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "\\.txt$",
                "--replace", ".bak",
                "--dry-run"
        });

        assertEquals(0, rc);
        assertTrue(Files.exists(a), "dry-run should not rename a.txt");
        assertTrue(Files.exists(b), "dry-run should not rename b.txt");
        assertFalse(Files.exists(tempDir.resolve("a.bak")), "no rename should occur in dry-run");
    }

    @Test
    void testPrefixRename() throws Exception {
        createFile("file1.txt", "x");
        createFile("file2.txt", "y");

        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "file",
                "--replace", "file",
                "--prefix", "pre_"
        });

        assertEquals(0, rc);
        assertTrue(Files.exists(tempDir.resolve("pre_file1.txt")), "prefixed file1 should exist");
        assertTrue(Files.exists(tempDir.resolve("pre_file2.txt")), "prefixed file2 should exist");
        assertFalse(Files.exists(tempDir.resolve("file1.txt")), "original file1 should be gone");
    }

    @Test
    void testSuffixRename() throws Exception {
        createFile("data.txt", "x");

        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "data",
                "--replace", "data",
                "--suffix", "_s"
        });

        assertEquals(0, rc);
        assertTrue(Files.exists(tempDir.resolve("data_s.txt")), "suffixed file should exist (suffix before extension)");
        assertFalse(Files.exists(tempDir.resolve("data.txt")), "original should be gone");
    }

    @Test
    void testSeqRename() throws Exception {
        createFile("a.txt", "1");
        createFile("b.txt", "2");
        createFile("c.txt", "3");

        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "[a-c]",
                "--replace", "file",
                "--seq",
                "--format", "%02d",
                "--seq-start", "1"
        });

        assertEquals(0, rc);
        assertTrue(Files.exists(tempDir.resolve("file01.txt")), "file01.txt should exist");
        assertTrue(Files.exists(tempDir.resolve("file02.txt")), "file02.txt should exist");
        assertTrue(Files.exists(tempDir.resolve("file03.txt")), "file03.txt should exist");
    }

    @Test
    void testNoFilesToRename() throws Exception {
        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.toString(),
                "--pattern", "ZZZ",
                "--replace", "x"
        });
        assertEquals(0, rc);
        assertTrue(err.toString().contains("No files to rename"));
    }

    @Test
    void testNonExistentDir() throws Exception {
        FileRenameCommand cmd = new FileRenameCommand();
        int rc = cmd.execute(new String[]{
                "--dir", tempDir.resolve("nope").toString(),
                "--pattern", "x"
        });
        assertEquals(1, rc);
    }
}