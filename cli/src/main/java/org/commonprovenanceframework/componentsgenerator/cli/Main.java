package org.commonprovenanceframework.componentsgenerator.cli;

import org.commonprovenanceframework.componentsgenerator.cli.CliCommands.GenerateChain;
import org.commonprovenanceframework.componentsgenerator.cli.CliCommands.LinkBundle;
import org.commonprovenanceframework.componentsgenerator.cli.CliCommands.PopulateBundle;
import org.commonprovenanceframework.componentsgenerator.cli.CliCommands.RegisterOrganisation;
import picocli.CommandLine;

@CommandLine.Command(
        name = "cpm-generator",
        description = "Main application",
        subcommands = {
                GenerateChain.class,
                LinkBundle.class,
                PopulateBundle.class,
                RegisterOrganisation.class,
        }
)
public class Main implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {

    }
}