package com.jcli.cli;

import com.jcli.codegen.GenClassCommand;
import com.jcli.codegen.GenProjectCommand;
import com.jcli.codegen.GenSnippetCommand;
import picocli.CommandLine.Command;

@Command(name = "gen", description = "Code generation",
         subcommands = {
             GenClassCommand.class,
             GenProjectCommand.class,
             GenSnippetCommand.class
         })
public class GenCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("jcli gen — Code generation");
        System.out.println("Use 'jcli gen --help' for available subcommands.");
    }
}