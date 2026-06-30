package com.jcli.cli;

import com.jcli.fileops.FileDiffCommand;
import com.jcli.fileops.FileFindCommand;
import com.jcli.fileops.FileGrepCommand;
import com.jcli.fileops.FileRenameCommand;
import com.jcli.fileops.FileStatCommand;
import com.jcli.fileops.FileSyncCommand;
import picocli.CommandLine.Command;

@Command(name = "file", description = "File operations",
         subcommands = {
             FileFindCommand.class,
             FileGrepCommand.class,
             FileStatCommand.class,
             FileRenameCommand.class,
             FileSyncCommand.class,
             FileDiffCommand.class
         })
public class FileCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("jcli file — File operations");
        System.out.println("Use 'jcli file --help' for available subcommands.");
    }
}