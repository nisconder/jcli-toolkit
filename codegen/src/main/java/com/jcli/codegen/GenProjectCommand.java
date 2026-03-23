package com.jcli.codegen;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenProjectCommand implements CliCommand {
    @Override
    public String name() {
        return "project";
    }

    @Override
    public String description() {
        return "Generate project structure";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli gen project")
                .description("Generate project from template")
                .addOption(CommandLineParser.Option.ofValue("n", "name", "Project name"))
                .addOption(CommandLineParser.Option.ofValue("t", "template", "Template: plain-java, maven-library, gradle-library, cli-app"))
                .addOption(CommandLineParser.Option.ofValue("g", "group", "Group ID"))
                .addOption(CommandLineParser.Option.ofValue("v", "version", "Version"))
                .addOption(CommandLineParser.Option.ofValue("o", "out", "Output directory"))
                .addOption(CommandLineParser.Option.of("a", "author", "Author name in README"))
                .addOption(CommandLineParser.Option.ofValue("description", "desc", "Project description"))
                .addOption(CommandLineParser.Option.of("verbose", "verbose", "Verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String projectName = cmdLine.getOptionValue(parser.getLongOption("name"));
        if (projectName == null) {
            Logger.error("Project name is required");
            return 1;
        }

        String template = cmdLine.getOptionValue(parser.getLongOption("template"), "plain-java");
        String groupId = cmdLine.getOptionValue(parser.getLongOption("group"), "com.example");
        String version = cmdLine.getOptionValue(parser.getLongOption("version"), "1.0.0");
        String outDir = cmdLine.getOptionValue(parser.getLongOption("out"), ".");
        String description = cmdLine.getOptionValue(parser.getLongOption("desc"), projectName + " Project");
        boolean verbose = cmdLine.hasOption(parser.getLongOption("verbose"));

        if (verbose) {
            Logger.setVerbose(true);
        }

        Path outputDir = Paths.get(outDir);
        if (!outputDir.isAbsolute()) {
            outputDir = Paths.get(System.getProperty("user.dir")).resolve(outputDir);
        }

        Path projectDir = outputDir.resolve(projectName);

        if (Files.exists(projectDir)) {
            Logger.error("Directory already exists: " + projectDir);
            return 1;
        }

        switch (template.toLowerCase()) {
            case "plain-java":
                generatePlainJava(projectDir, projectName, description, verbose);
                break;
            case "maven-library":
                generateMavenLibrary(projectDir, projectName, groupId, version, description, verbose);
                break;
            case "gradle-library":
                generateGradleLibrary(projectDir, projectName, groupId, version, description, verbose);
                break;
            case "cli-app":
                generateCliApp(projectDir, projectName, groupId, version, description, verbose);
                break;
            default:
                Logger.error("Unknown template: " + template);
                return 1;
        }

        Logger.info("Project generated successfully: " + projectDir.toAbsolutePath());
        return 0;
    }

    private void generatePlainJava(Path projectDir, String projectName, String description, boolean verbose) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java"));

        createReadme(projectDir, projectName, description);
        createGitignore(projectDir);

        createMainClass(projectDir, projectName);
    }

    private void generateMavenLibrary(Path projectDir, String projectName, String groupId, String version, 
                                     String description, boolean verbose) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.createDirectories(projectDir.resolve("src/test/java"));

        createReadme(projectDir, projectName, description);
        createGitignore(projectDir);
        createMavenPom(projectDir, projectName, groupId, version, description);
        createMainClass(projectDir, projectName);
    }

    private void generateGradleLibrary(Path projectDir, String projectName, String groupId, String version,
                                      String description, boolean verbose) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.createDirectories(projectDir.resolve("src/test/java"));

        createReadme(projectDir, projectName, description);
        createGitignore(projectDir);
        createGradleBuild(projectDir, projectName, groupId, version, description);
        createMainClass(projectDir, projectName);
    }

    private void generateCliApp(Path projectDir, String projectName, String groupId, String version,
                                String description, boolean verbose) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.createDirectories(projectDir.resolve("src/test/java"));

        createReadme(projectDir, projectName, description);
        createGitignore(projectDir);
        createMavenPom(projectDir, projectName, groupId, version, description);
        createMainClass(projectDir, projectName);
    }

    private void createReadme(Path projectDir, String projectName, String description) throws IOException {
        String content = "# " + projectName + "\n\n" +
                description + "\n\n" +
                "## Getting Started\n\n" +
                "```bash\n" +
                "# Build the project\n" +
                "./gradlew build\n\n" +
                "# Run the application\n" +
                "./gradlew run\n" +
                "```\n";
        Files.writeString(projectDir.resolve("README.md"), content);
    }

    private void createGitignore(Path projectDir) throws IOException {
        String content = "# Compiled class files\n" +
                "*.class\n\n" +
                "# Log files\n" +
                "*.log\n\n" +
                "# Package Files\n" +
                "*.jar\n" +
                "*.war\n" +
                "*.nar\n" +
                "*.ear\n" +
                "*.zip\n" +
                "*.tar.gz\n" +
                "*.rar\n\n" +
                "# IDE\n" +
                ".idea/\n" +
                "*.iml\n" +
                ".vscode/\n\n" +
                "# Gradle\n" +
                ".gradle/\n" +
                "build/\n" +
                "!gradle/wrapper/gradle-wrapper.jar\n\n" +
                "# Maven\n" +
                "target/\n" +
                "!**/src/main/**/target/\n" +
                "!**/src/test/**/target/\n";
        Files.writeString(projectDir.resolve(".gitignore"), content);
    }

    private void createMavenPom(Path projectDir, String projectName, String groupId, String version, String description) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n\n" +
                "    <groupId>" + groupId + "</groupId>\n" +
                "    <artifactId>" + projectName + "</artifactId>\n" +
                "    <version>" + version + "</version>\n" +
                "    <packaging>jar</packaging>\n\n" +
                "    <name>" + projectName + "</name>\n" +
                "    <description>" + description + "</description>\n\n" +
                "    <properties>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "        <maven.compiler.source>17</maven.compiler.source>\n" +
                "        <maven.compiler.target>17</maven.compiler.target>\n" +
                "    </properties>\n\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.junit.jupiter</groupId>\n" +
                "            <artifactId>junit-jupiter-api</artifactId>\n" +
                "            <version>5.10.0</version>\n" +
                "            <scope>test</scope>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";
        Files.writeString(projectDir.resolve("pom.xml"), content);
    }

    private void createGradleBuild(Path projectDir, String projectName, String groupId, String version, String description) throws IOException {
        String content = "plugins {\n" +
                "    id 'java'\n" +
                "}\n\n" +
                "group = '" + groupId + "'\n" +
                "version = '" + version + "'\n\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n\n" +
                "dependencies {\n" +
                "    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.0'\n" +
                "    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.0'\n" +
                "}\n\n" +
                "test {\n" +
                "    useJUnitPlatform()\n" +
                "}\n";
        Files.writeString(projectDir.resolve("build.gradle"), content);
    }

    private void createMainClass(Path projectDir, String projectName) throws IOException {
        String className = capitalize(projectName);
        String content = "import java.util.Scanner;\n\n" +
                "public class " + className + " {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello from " + className + "!\");\n" +
                "    }\n" +
                "}\n";
        Files.writeString(projectDir.resolve("src/main/java/" + className + ".java"), content);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
