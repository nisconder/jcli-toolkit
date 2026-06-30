package com.jcli.cli;

import com.jcli.codegen.TemplateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "jcli",
        version = "JCLI Toolkit v1.0.0",
        description = "Java CLI Toolkit for Developers",
        subcommands = {
                FileCommand.class,
                GenCommand.class,
                TemplateCommand.class
        },
        mixinStandardHelpOptions = true
)
public class JcliCli implements Runnable {

    @Override
    public void run() {
        // Default: show help when no subcommand given
        CommandLine.usage(this, System.out);
    }
}