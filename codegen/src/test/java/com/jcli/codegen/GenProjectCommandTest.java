package com.jcli.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GenProjectCommandTest {

    @TempDir
    Path tempDir;

    private int run(String... args) throws Exception {
        return new GenProjectCommand().execute(args);
    }

    private Path projectDir(String name) {
        return tempDir.resolve(name);
    }

    @Test
    void testGeneratePlainJava() throws Exception {
        int rc = run("--name", "plainproj", "--template", "plain-java",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path dir = projectDir("plainproj");
        assertTrue(Files.isDirectory(dir), "project dir should exist");
        assertTrue(Files.isDirectory(dir.resolve("src/main/java")), "src/main/java should exist");
        assertTrue(Files.exists(dir.resolve("README.md")), "README.md should exist");
        assertTrue(Files.exists(dir.resolve(".gitignore")), ".gitignore should exist");
        // main class is capitalized project name
        assertTrue(Files.exists(dir.resolve("src/main/java/Plainproj.java")),
                "main class file should exist");
        String main = Files.readString(dir.resolve("src/main/java/Plainproj.java"));
        assertTrue(main.contains("public class Plainproj"), "main class should be declared");
        assertTrue(main.contains("public static void main"), "should have main method");
    }

    @Test
    void testGenerateMavenLibrary() throws Exception {
        int rc = run("--name", "mvnlib", "--template", "maven-library",
                "--group", "com.test", "--version", "2.0.0",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path dir = projectDir("mvnlib");
        assertTrue(Files.isDirectory(dir.resolve("src/main/java")), "src/main/java should exist");
        assertTrue(Files.isDirectory(dir.resolve("src/test/java")), "src/test/java should exist");
        Path pom = dir.resolve("pom.xml");
        assertTrue(Files.exists(pom), "pom.xml should exist");
        String pomContent = Files.readString(pom);
        assertTrue(pomContent.contains("<groupId>com.test</groupId>"), "pom should contain groupId");
        assertTrue(pomContent.contains("<artifactId>mvnlib</artifactId>"), "pom should contain artifactId");
        assertTrue(pomContent.contains("<version>2.0.0</version>"), "pom should contain version");
        assertTrue(pomContent.contains("<packaging>jar</packaging>"), "pom should contain packaging");
        assertTrue(Files.exists(dir.resolve("src/main/java/Mvnlib.java")), "main class should exist");
    }

    @Test
    void testGenerateGradleLibrary() throws Exception {
        int rc = run("--name", "gradlelib", "--template", "gradle-library",
                "--group", "org.demo", "--version", "0.9.0",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path dir = projectDir("gradlelib");
        assertTrue(Files.isDirectory(dir.resolve("src/main/java")), "src/main/java should exist");
        assertTrue(Files.isDirectory(dir.resolve("src/test/java")), "src/test/java should exist");
        Path buildGradle = dir.resolve("build.gradle");
        assertTrue(Files.exists(buildGradle), "build.gradle should exist");
        String content = Files.readString(buildGradle);
        assertTrue(content.contains("group = 'org.demo'"), "build.gradle should contain group");
        assertTrue(content.contains("version = '0.9.0'"), "build.gradle should contain version");
        assertTrue(content.contains("id 'java'"), "build.gradle should apply java plugin");
        assertTrue(content.contains("useJUnitPlatform()"), "build.gradle should use JUnit platform");
        assertTrue(Files.exists(dir.resolve("src/main/java/Gradlelib.java")), "main class should exist");
    }

    @Test
    void testGenerateCliApp() throws Exception {
        int rc = run("--name", "mycli", "--template", "cli-app",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path dir = projectDir("mycli");
        assertTrue(Files.isDirectory(dir), "cli-app project dir should exist");
        assertTrue(Files.exists(dir.resolve("pom.xml")), "cli-app should have pom.xml");
        assertTrue(Files.exists(dir.resolve("README.md")), "cli-app should have README");
        assertTrue(Files.exists(dir.resolve("src/main/java/Mycli.java")), "main class should exist");
        // description defaults to "<name> Project"
        String readme = Files.readString(dir.resolve("README.md"));
        assertTrue(readme.contains("mycli Project"), "README should contain default description");
    }

    @Test
    void testMissingName() throws Exception {
        // picocli returns non-zero (USAGE = 2) when a required option is missing.
        int rc = run("--template", "plain-java", "--out", tempDir.toString());
        assertNotEquals(0, rc, "missing required --name should fail");
    }

    @Test
    void testExistingDir() throws Exception {
        // Pre-create the project directory (non-empty) so generation must refuse.
        Path existing = tempDir.resolve("alreadyhere");
        Files.createDirectories(existing);
        Files.writeString(existing.resolve("blocker.txt"), "do not overwrite");

        int rc = run("--name", "alreadyhere", "--template", "plain-java",
                "--out", tempDir.toString());
        assertEquals(1, rc, "generating into an existing dir should return 1");
        // original content preserved
        assertTrue(Files.exists(existing.resolve("blocker.txt")),
                "existing files should be untouched");
    }

    @Test
    void testUnknownTemplate() throws Exception {
        int rc = run("--name", "unknownproj", "--template", "no-such-template",
                "--out", tempDir.toString());
        assertEquals(1, rc, "unknown template should return 1");
        assertFalse(Files.exists(projectDir("unknownproj")),
                "no project dir should be created for unknown template");
    }
}