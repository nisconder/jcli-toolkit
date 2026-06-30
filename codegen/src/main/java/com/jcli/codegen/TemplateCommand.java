package com.jcli.codegen;

import com.jcli.core.CliCommand;
import com.jcli.core.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "template", description = "Template management", mixinStandardHelpOptions = true)
public class TemplateCommand implements CliCommand {

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
    }

    private void addTemplate(String path) {
        Logger.info("Adding template from: " + path);
        Logger.info("Template added successfully (stub implementation)");
    }

    private void removeTemplate(String name) {
        Logger.info("Removing template: " + name);
        Logger.info("Template removed successfully (stub implementation)");
    }
}