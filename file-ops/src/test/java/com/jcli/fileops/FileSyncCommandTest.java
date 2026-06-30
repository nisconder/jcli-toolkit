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

class FileSyncCommandTest {

    @TempDir
    Path tempDir;

    private Path source;
    private Path target;

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private PrintStream origOut;
    private PrintStream origErr;

    @BeforeEach
    void setUp() throws IOException {
        source = tempDir.resolve("source");
        target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.createDirectories(target);

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

    @Test
    void testCopyNewer() throws Exception {
        Files.writeString(source.resolve("new.txt"), "new content");
        Files.writeString(target.resolve("old.txt"), "old");
        // make target's old.txt older
        Files.setLastModifiedTime(target.resolve("old.txt"), FileTime.fromMillis(System.currentTimeMillis() - 100000));

        FileSyncCommand cmd = new FileSyncCommand();
        int rc = cmd.execute(new String[]{"--source", source.toString(), "--target", target.toString()});

        assertEquals(0, rc);
        assertTrue(Files.exists(target.resolve("new.txt")), "newer file should be copied to target");
    }

    @Test
    void testDryRun() throws Exception {
        Files.writeString(source.resolve("file.txt"), "content");

        FileSyncCommand cmd = new FileSyncCommand();
        int rc = cmd.execute(new String[]{"--source", source.toString(), "--target", target.toString(), "--dry-run"});

        assertEquals(0, rc);
        assertFalse(Files.exists(target.resolve("file.txt")), "dry-run should not copy files");
    }

    @Test
    void testDeleteExtra() throws Exception {
        // source has a file that also exists in target (so it is preserved)
        Files.writeString(source.resolve("keep.txt"), "keep");
        Files.writeString(target.resolve("keep.txt"), "keep");
        // target has an extra file not present in source
        Files.writeString(target.resolve("extra.txt"), "extra");
        // match mtimes so keep.txt is not re-copied
        java.nio.file.attribute.FileTime t = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis());
        Files.setLastModifiedTime(source.resolve("keep.txt"), t);
        Files.setLastModifiedTime(target.resolve("keep.txt"), t);

        FileSyncCommand cmd = new FileSyncCommand();
        cmd.execute(new String[]{
                "--source", source.toString(),
                "--target", target.toString(),
                "--delete"
        });

        assertFalse(Files.exists(target.resolve("extra.txt")), "extra file should be deleted from target");
        assertTrue(Files.exists(target.resolve("keep.txt")), "keep.txt should remain");
    }

    @Test
    void testNonExistentSource() throws Exception {
        FileSyncCommand cmd = new FileSyncCommand();
        int rc = cmd.execute(new String[]{
                "--source", tempDir.resolve("nope").toString(),
                "--target", target.toString()
        });
        assertEquals(1, rc);
    }

    @Test
    void testNoChangesNeeded() throws Exception {
        // both source and target empty
        FileSyncCommand cmd = new FileSyncCommand();
        int rc = cmd.execute(new String[]{"--source", source.toString(), "--target", target.toString()});
        assertEquals(0, rc);
        assertTrue(err.toString().contains("No changes needed"));
    }
}