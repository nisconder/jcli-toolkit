package com.jcli.codegen;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

public class GenSnippetCommand implements CliCommand {
    @Override
    public String name() {
        return "snippet";
    }

    @Override
    public String description() {
        return "Generate code snippets";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli gen snippet")
                .description("Generate code snippets")
                .addOption(CommandLineParser.Option.ofValue("t", "type", "Snippet type: getter-setter, equals-hashcode, builder, logger, try-with-resources"))
                .addOption(CommandLineParser.Option.ofValue("f", "fields", "Field definitions (name:type,...)"))
                .addOption(CommandLineParser.Option.of("c", "clipboard", "Copy to clipboard (if supported)"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        String type = cmdLine.getOptionValue(parser.getLongOption("type"));
        if (type == null) {
            Logger.error("Snippet type is required");
            return 1;
        }

        String fieldsStr = cmdLine.getOptionValue(parser.getLongOption("fields"));
        boolean clipboard = cmdLine.hasOption(parser.getShortOption("c"));

        String snippet = generateSnippet(type, fieldsStr);

        if (clipboard) {
            try {
                java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(snippet), null);
                Logger.info("Snippet copied to clipboard");
            } catch (Exception e) {
                Logger.warn("Could not copy to clipboard: " + e.getMessage());
                System.out.println(snippet);
            }
        } else {
            System.out.println(snippet);
        }

        return 0;
    }

    private String generateSnippet(String type, String fieldsStr) {
        switch (type.toLowerCase()) {
            case "getter-setter":
                return generateGetterSetter(fieldsStr);
            case "equals-hashcode":
                return generateEqualsHashcode(fieldsStr);
            case "builder":
                return generateBuilder(fieldsStr);
            case "logger":
                return generateLogger();
            case "try-with-resources":
                return generateTryWithResources();
            default:
                return "Unknown snippet type: " + type;
        }
    }

    private String generateGetterSetter(String fieldsStr) {
        if (fieldsStr == null) {
            return "// Usage: --fields \"name:type,...\"\n// Example: --fields \"id:Long,name:String,age:int\"";
        }

        StringBuilder sb = new StringBuilder();
        String[] fieldDefs = fieldsStr.split(",");
        
        for (String fieldDef : fieldDefs) {
            String[] parts = fieldDef.trim().split(":");
            if (parts.length == 2) {
                String name = parts[0].trim();
                String type = parts[1].trim();
                String capitalizedName = capitalize(name);
                
                sb.append("public ").append(type).append(" get").append(capitalizedName).append("() {\n");
                sb.append("    return ").append(name).append(";\n");
                sb.append("}\n\n");
                
                sb.append("public void set").append(capitalizedName).append("(").append(type).append(" ").append(name).append(") {\n");
                sb.append("    this.").append(name).append(" = ").append(name).append(";\n");
                sb.append("}\n\n");
            }
        }
        
        return sb.toString();
    }

    private String generateEqualsHashcode(String fieldsStr) {
        return "@Override\n" +
                "public boolean equals(Object o) {\n" +
                "    if (this == o) return true;\n" +
                "    if (o == null || getClass() != o.getClass()) return false;\n" +
                "    MyClass that = (MyClass) o;\n" +
                "    return Objects.equals(field, that.field);\n" +
                "}\n\n" +
                "@Override\n" +
                "public int hashCode() {\n" +
                "    return Objects.hash(field);\n" +
                "}\n";
    }

    private String generateBuilder(String fieldsStr) {
        return "public static Builder builder() {\n" +
                "    return new Builder();\n" +
                "}\n\n" +
                "public static class Builder {\n" +
                "    // Add fields here\n" +
                "    private MyClass instance = new MyClass();\n\n" +
                "    public Builder field(Type field) {\n" +
                "        instance.field = field;\n" +
                "        return this;\n" +
                "    }\n\n" +
                "    public MyClass build() {\n" +
                "        return instance;\n" +
                "    }\n" +
                "}\n";
    }

    private String generateLogger() {
        return "private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);\n" +
                "\n" +
                "// Usage:\n" +
                "LOGGER.info(\"Info message\");\n" +
                "LOGGER.debug(\"Debug message\");\n" +
                "LOGGER.error(\"Error message\", exception);\n";
    }

    private String generateTryWithResources() {
        return "try (InputStream is = new FileInputStream(\"file.txt\")) {\n" +
                "    // Read from input stream\n" +
                "    // Resources are automatically closed\n" +
                "} catch (IOException e) {\n" +
                "    // Handle exception\n" +
                "}\n";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
