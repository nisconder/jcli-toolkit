package com.jcli.cli;

import com.jcli.codegen.GenClassCommand;
import com.jcli.codegen.GenProjectCommand;
import com.jcli.codegen.GenSnippetCommand;
import com.jcli.codegen.TemplateCommand;
import com.jcli.fileops.FileDiffCommand;
import com.jcli.fileops.FileFindCommand;
import com.jcli.fileops.FileGrepCommand;
import com.jcli.fileops.FileRenameCommand;
import com.jcli.fileops.FileStatCommand;
import com.jcli.fileops.FileSyncCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "jcli",
        version = "JCLI Toolkit v1.0.0",
        description = "Java CLI Toolkit for Developers",
        subcommands = {
                FileFindCommand.class,
                FileGrepCommand.class,
                FileStatCommand.class,
                FileRenameCommand.class,
                FileSyncCommand.class,
                FileDiffCommand.class,
                GenClassCommand.class,
                GenProjectCommand.class,
                GenSnippetCommand.class,
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