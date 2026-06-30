package com.jcli.codegen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCommandTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream capturedOut;
    private PrintStream originalOut;
    private String originalUserHome;

    @BeforeEach
    void isolateUserHomeAndStdout() {
        // TemplateCommand stores user templates under ~/.jcli/templates, i.e.
        // System.getProperty("user.home")/.jcli/templates. Redirect user.home
        // to a per-test temp dir so tests never touch the real home directory.
        originalUserHome = System.getProperty("user.home");
        originalOut = System.out;
        System.setProperty("user.home", tempDir.toString());
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    void restore() {
        System.setOut(originalOut);
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    private int run(String... args) throws Exception {
        return new TemplateCommand().execute(args);
    }

    private String output() {
        return capturedOut.toString();
    }

    private Path templatesDir() {
        return Paths.get(System.getProperty("user.home"), ".jcli", "templates");
    }

    @Test
    void testListTemplates() throws Exception {
        int rc = run("list");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("Built-in Templates:"), "should list built-in header");
        assertTrue(out.contains("pojo"), "should list pojo");
        assertTrue(out.contains("service"), "should list service");
        assertTrue(out.contains("repository"), "should list repository");
        assertTrue(out.contains("controller"), "should list controller");
        assertTrue(out.contains("builder"), "should list builder");
        assertTrue(out.contains("singleton"), "should list singleton");
        assertTrue(out.contains("Project Templates:"), "should list project header");
        assertTrue(out.contains("plain-java"), "should list plain-java");
        assertTrue(out.contains("maven-library"), "should list maven-library");
        assertTrue(out.contains("gradle-library"), "should list gradle-library");
        assertTrue(out.contains("cli-app"), "should list cli-app");
    }

    @Test
    void testListTemplatesWithUserTemplates() throws Exception {
        // Pre-populate the user templates dir, then list should include them.
        Path tdir = templatesDir();
        Files.createDirectories(tdir);
        Files.writeString(tdir.resolve("my-template.txt"), "hello");

        int rc = run("list");
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("User Templates:"), "should show User Templates section");
        assertTrue(out.contains("my-template.txt"), "should list the user template");
    }

    @Test
    void testNoActionPrintsHelp() throws Exception {
        int rc = run();
        assertEquals(0, rc);
        String out = output();
        assertTrue(out.contains("Template Management Commands:"), "no action should print help");
        assertTrue(out.contains("jcli template list"), "help should mention list");
        assertTrue(out.contains("jcli template add"), "help should mention add");
        assertTrue(out.contains("jcli template remove"), "help should mention remove");
    }

    @Test
    void testAddTemplate() throws Exception {
        // Create a source file to add as a template.
        Path source = tempDir.resolve("source").resolve("custom-template.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "template body");

        int rc = run("add", source.toString());
        assertEquals(0, rc);

        Path added = templatesDir().resolve("custom-template.txt");
        assertTrue(Files.exists(added), "template should be copied to ~/.jcli/templates/");
        assertEquals("template body", Files.readString(added),
                "copied template content should match source");
    }

    @Test
    void testAddTemplateDirectory() throws Exception {
        // Adding a directory template copies it recursively.
        Path sourceDir = tempDir.resolve("dir-template");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("a.txt"), "a");
        Files.writeString(sourceDir.resolve("b.txt"), "b");

        int rc = run("add", sourceDir.toString());
        assertEquals(0, rc);

        Path added = templatesDir().resolve("dir-template");
        assertTrue(Files.isDirectory(added), "directory template should be copied");
        assertTrue(Files.exists(added.resolve("a.txt")), "a.txt should be copied");
        assertTrue(Files.exists(added.resolve("b.txt")), "b.txt should be copied");
    }

    @Test
    void testAddTemplateAlreadyExists() throws Exception {
        Path source = tempDir.resolve("dup-source.txt");
        Files.writeString(source, "v1");

        // First add succeeds.
        int rc1 = run("add", source.toString());
        assertEquals(0, rc1);
        Path added = templatesDir().resolve("dup-source.txt");
        assertTrue(Files.exists(added));

        // Second add of a file with the same name should not overwrite (logs error, exit 0).
        Path source2 = tempDir.resolve("other").resolve("dup-source.txt");
        Files.createDirectories(source2.getParent());
        Files.writeString(source2, "v2");
        int rc2 = run("add", source2.toString());
        assertEquals(0, rc2);
        // Original content preserved (not overwritten).
        assertEquals("v1", Files.readString(added),
                "existing template should not be overwritten");
    }

    @Test
    void testAddTemplateMissingPath() throws Exception {
        // add with a nonexistent path logs an error but returns 0 (no exception).
        int rc = run("add", tempDir.resolve("nope.txt").toString());
        assertEquals(0, rc);
        assertFalse(Files.exists(templatesDir().resolve("nope.txt")),
                "nothing should be copied for a missing source");
    }

    @Test
    void testRemoveTemplate() throws Exception {
        // Add then remove a template file.
        Path source = tempDir.resolve("removable.txt");
        Files.writeString(source, "bye");
        assertEquals(0, run("add", source.toString()));
        Path added = templatesDir().resolve("removable.txt");
        assertTrue(Files.exists(added));

        int rc = run("remove", "removable.txt");
        assertEquals(0, rc);
        assertFalse(Files.exists(added), "template should be deleted after remove");
    }

    @Test
    void testRemoveTemplateDirectory() throws Exception {
        // Add a directory template then remove it (recursive delete).
        Path sourceDir = tempDir.resolve("rmdir");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("x.txt"), "x");
        assertEquals(0, run("add", sourceDir.toString()));
        Path added = templatesDir().resolve("rmdir");
        assertTrue(Files.isDirectory(added));

        int rc = run("remove", "rmdir");
        assertEquals(0, rc);
        assertFalse(Files.exists(added), "directory template should be removed recursively");
    }

    @Test
    void testRemoveNonexistent() throws Exception {
        // Removing a template that does not exist logs an error but the command
        // still returns 0 (removeTemplate swallows the not-found case).
        int rc = run("remove", "does-not-exist.txt");
        assertEquals(0, rc, "remove of nonexistent template returns 0 (logs error only)");
        assertFalse(Files.exists(templatesDir().resolve("does-not-exist.txt")));
    }

    @Test
    void testRemoveMissingArg() throws Exception {
        // remove without a name argument returns 1 (arg guard in call()).
        int rc = run("remove");
        assertEquals(1, rc, "remove without name should return 1");
    }

    @Test
    void testAddMissingArg() throws Exception {
        // add without a path argument returns 1 (arg guard in call()).
        int rc = run("add");
        assertEquals(1, rc, "add without path should return 1");
    }

    @Test
    void testUnknownAction() throws Exception {
        int rc = run("frobnicate");
        assertEquals(1, rc, "unknown action should return 1");
    }
}