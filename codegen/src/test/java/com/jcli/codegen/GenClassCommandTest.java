package com.jcli.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GenClassCommandTest {

    @TempDir
    Path tempDir;

    private int run(String... args) throws Exception {
        return new GenClassCommand().execute(args);
    }

    private Path classFile(String pkg, String cls) {
        return tempDir.resolve(pkg.replace('.', '/')).resolve(cls + ".java");
    }

    private String read(Path p) throws Exception {
        return Files.readString(p);
    }

    /** Verify a generated .java file is syntactically valid by compiling it. */
    private void assertCompiles(Path javaFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler not available - run tests with a JDK, not a JRE");
        int rc = compiler.run(null, null, null, javaFile.toString());
        assertEquals(0, rc, "Generated file should compile: " + javaFile);
    }

    @Test
    void testGeneratePojo() throws Exception {
        int rc = run("--name", "User", "--package", "com.demo",
                "--template", "pojo", "--fields", "id:Long,name:String",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.demo", "User");
        assertTrue(Files.exists(file), "POJO file should be created");
        String content = read(file);
        assertTrue(content.contains("package com.demo;"), "should contain package decl");
        assertTrue(content.contains("public class User"), "should contain class decl");
        assertTrue(content.contains("private Long id;"), "should contain id field");
        assertTrue(content.contains("private String name;"), "should contain name field");
        assertTrue(content.contains("getId"), "should contain getter");
        assertTrue(content.contains("setId"), "should contain setter");
        assertCompiles(file);
    }

    @Test
    void testGenerateService() throws Exception {
        int rc = run("--name", "UserService", "--template", "service",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.example", "UserService");
        assertTrue(Files.exists(file), "service file should be created");
        String content = read(file);
        assertTrue(content.contains("public class UserService"), "should contain class decl");
        assertTrue(content.contains("public UserService()"), "should contain constructor");
        assertTrue(content.contains("Add service methods here"), "should contain service comment");
        assertCompiles(file);
    }

    @Test
    void testGenerateRepository() throws Exception {
        int rc = run("--name", "UserRepository", "--template", "repository",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.example", "UserRepository");
        assertTrue(Files.exists(file), "repository file should be created");
        String content = read(file);
        assertTrue(content.contains("public class UserRepository"), "should contain class decl");
        assertTrue(content.contains("Add repository methods here"), "should contain repository comment");
        assertCompiles(file);
    }

    @Test
    void testGenerateController() throws Exception {
        int rc = run("--name", "UserController", "--template", "controller",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.example", "UserController");
        assertTrue(Files.exists(file), "controller file should be created");
        String content = read(file);
        assertTrue(content.contains("public class UserController"), "should contain class decl");
        assertTrue(content.contains("Add controller methods here"), "should contain controller comment");
        assertCompiles(file);
    }

    @Test
    void testGenerateBuilder() throws Exception {
        int rc = run("--name", "Account", "--template", "builder",
                "--fields", "balance:Double,owner:String",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.example", "Account");
        assertTrue(Files.exists(file), "builder file should be created");
        String content = read(file);
        assertTrue(content.contains("public class Account"), "should contain class decl");
        assertTrue(content.contains("public static Builder builder()"), "should contain builder() factory");
        assertTrue(content.contains("public static class Builder"), "should contain Builder inner class");
        assertTrue(content.contains("public Account balance(Double balance)"), "should contain fluent setter");
        assertTrue(content.contains("public Account build()"), "should contain build() on outer");
        assertCompiles(file);
    }

    @Test
    void testGenerateSingleton() throws Exception {
        int rc = run("--name", "Config", "--template", "singleton",
                "--out", tempDir.toString());
        assertEquals(0, rc);

        Path file = classFile("com.example", "Config");
        assertTrue(Files.exists(file), "singleton file should be created");
        String content = read(file);
        assertTrue(content.contains("public class Config"), "should contain class decl");
        assertTrue(content.contains("private static Config instance;"), "should contain instance field");
        assertTrue(content.contains("private Config()"), "should contain private constructor");
        assertTrue(content.contains("public static Config getInstance()"), "should contain getInstance()");
        assertCompiles(file);
    }

    @Test
    void testMissingName() throws Exception {
        // picocli returns a non-zero exit code (2 = USAGE) when a required option is missing.
        int rc = run("--template", "pojo", "--out", tempDir.toString());
        assertNotEquals(0, rc, "missing required --name should fail");
    }

    @Test
    void testDefaultTemplateIsPojo() throws Exception {
        int rc = run("--name", "Default", "--out", tempDir.toString());
        assertEquals(0, rc);
        Path file = classFile("com.example", "Default");
        assertTrue(Files.exists(file), "default template (pojo) file should be created");
        assertCompiles(file);
    }

    @Test
    void testUnknownTemplateFallsBackToPojo() throws Exception {
        int rc = run("--name", "Weird", "--template", "nonexistent-template",
                "--out", tempDir.toString());
        assertEquals(0, rc);
        Path file = classFile("com.example", "Weird");
        assertTrue(Files.exists(file), "unknown template falls back to pojo");
        assertCompiles(file);
    }

    @Test
    void testAuthorInJavadoc() throws Exception {
        int rc = run("--name", "Authored", "--author", "TestAuthor",
                "--out", tempDir.toString());
        assertEquals(0, rc);
        String content = read(classFile("com.example", "Authored"));
        assertTrue(content.contains("@author TestAuthor"), "should embed author in javadoc");
    }
}