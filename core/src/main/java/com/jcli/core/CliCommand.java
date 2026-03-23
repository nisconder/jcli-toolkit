package com.jcli.core;

public interface CliCommand {
    String name();
    
    String description();
    
    int execute(String[] args) throws Exception;
}
