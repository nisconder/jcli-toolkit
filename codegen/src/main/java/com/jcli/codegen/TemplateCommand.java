package com.jcli.codegen;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "template", description = "Template management", mixinStandardHelpOptions = true)
public class TemplateCommand implements CliCommand, Callable<Integer> {

    @Parameters(paramLabel = "ACTION", description = "Action: list, add, remove", arity = "0..1")
    private String action;

    @Parameters(paramLabel = "ARG", description = "Template path (for add) or name (for remove)", arity = "0..1")
    private String arg;

    @Override
    public String name() {
        return "template";
    }

    @Override
    public String description() {
        return "Template management";
    }

    @Override
    public int execute(String[] args) throws Exception {
        return new picocli.CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
        if (action == null) {
            System.out.println("Template Management Commands:");
            System.out.println("  jcli template list        - List all templates");
            System.out.println("  jcli template add <path>   - Add custom template");
            System.out.println("  jcli template remove <name> - Remove template");
            return 0;
        }

        switch (action.toLowerCase()) {
            case "list":
                listTemplates();
                break;
            case "add":
                if (arg == null) {
                    Logger.error("Template path is required");
                    return 1;
                }
                addTemplate(arg);
                break;
            case "remove":
                if (arg == null) {
                    Logger.error("Template name is required");
                    return 1;
                }
                removeTemplate(arg);
                break;
            default:
                Logger.error("Unknown command: " + action);
                return 1;
        }

        return 0;
    }

    private void listTemplates() {
        System.out.println("Built-in Templates:");
        System.out.println("  pojo           - Plain Old Java Object");
        System.out.println("  service        - Service class");
        System.out.println("  repository     - Repository class");
        System.out.println("  controller     - Controller class");
        System.out.println("  builder        - Builder pattern");
        System.out.println("  singleton      - Singleton pattern");
        System.out.println();
        System.out.println("Project Templates:");
        System.out.println("  plain-java     - Plain Java project");
        System.out.println("  maven-library  - Maven library project");
        System.out.println("  gradle-library - Gradle library project");
        System.out.println("  cli-app        - CLI application");

        // List user-installed templates from ~/.jcli/templates/
        Path templatesDir = Paths.get(System.getProperty("user.home"), ".jcli", "templates");
        if (Files.isDirectory(templatesDir)) {
            List<String> userTemplates = new java.util.ArrayList<>();
            try (Stream<Path> stream = Files.list(templatesDir)) {
                stream.forEach(path -> userTemplates.add(path.getFileName().toString()));
            } catch (IOException e) {
                Logger.warn("Failed to list user templates: " + e.getMessage());
                return;
            }
            if (!userTemplates.isEmpty()) {
                userTemplates.sort(String::compareTo);
                System.out.println();
                System.out.println("User Templates:");
                for (String name : userTemplates) {
                    System.out.println("  " + name);
                }
            }
        }
    }

    private void addTemplate(String path) {
        try {
            Path source = Paths.get(path);
            if (!Files.exists(source)) {
                Logger.error("Template path not found: " + path);
                return;
            }

            Path templatesDir = Paths.get(System.getProperty("user.home"), ".jcli", "templates");
            Files.createDirectories(templatesDir);

            String templateName = source.getFileName().toString();
            Path target = templatesDir.resolve(templateName);

            if (Files.exists(target)) {
                Logger.error("Template already exists: " + templateName);
                return;
            }

            if (Files.isDirectory(source)) {
                // Copy directory recursively
                try (Stream<Path> walk = Files.walk(source)) {
                    java.util.Iterator<Path> it = walk.iterator();
                    while (it.hasNext()) {
                        Path src = it.next();
                        Path dest = target.resolve(source.relativize(src));
                        try {
                            Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
                        } catch (IOException ex) {
                            Logger.error("Failed to copy: " + src + " - " + ex.getMessage());
                        }
                    }
                }
            } else {
                Files.copy(source, target);
            }

            Logger.info("Template added: " + templateName);
        } catch (Exception e) {
            Logger.error("Failed to add template: " + e.getMessage());
        }
    }

    private void removeTemplate(String name) {
        try {
            Path templatesDir = Paths.get(System.getProperty("user.home"), ".jcli", "templates");
            Path target = templatesDir.resolve(name);

            if (!Files.exists(target)) {
                Logger.error("Template not found: " + name);
                return;
            }

            if (Files.isDirectory(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    java.util.Iterator<Path> it = walk.sorted(Comparator.reverseOrder()).iterator();
                    while (it.hasNext()) {
                        Path path = it.next();
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            Logger.error("Failed to delete: " + path + " - " + e.getMessage());
                        }
                    }
                }
            } else {
                Files.deleteIfExists(target);
            }

            Logger.info("Template removed: " + name);
        } catch (Exception e) {
            Logger.error("Failed to remove template: " + e.getMessage());
        }
    }
}