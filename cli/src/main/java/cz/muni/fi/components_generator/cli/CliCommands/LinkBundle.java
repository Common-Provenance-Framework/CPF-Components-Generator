package cz.muni.fi.components_generator.cli.CliCommands;

import cz.muni.fi.components_generator.core.Commands;
import picocli.CommandLine.*;

@Command(name = "link-bundle", description = "Creates a bundle linked to an existing bundle of another organization")
public class LinkBundle implements Runnable {
    @Spec
    Model.CommandSpec spec;

    int branching = 1;

    @Option(names = {"-o", "--bundle-name"}, required = true, description = "Name of the new bundle")
    String bundleName;

    @Option(names = {"-b", "--branching"}, description = "number of forward connectors in the new bundle")
    public void setBranching(int value) {
        if (value <= 0) {
            throw new ParameterException(spec.commandLine(), "branching must be greater than 0");
        }
        branching = value;
    }

    @Option(names = {"-O", "--organization-id"}, required = true, description = "organization the new bundle belongs to")
    String organizationId;

    @Option(names = {"-k", "--key-path"}, required = true, description = "signing key of the new bundle's organization")
    String keyPath;

    @Option(names = {"-F", "--from-organization-id"}, required = true, description = "organization that owns the referenced bundle")
    String fromOrganizationId;

    @Option(names = {"-B", "--from-bundle-id"}, required = true, description = "id of the referenced bundle")
    String fromBundleId;

    @Option(names = {"-c", "--from-connector-id"}, description = "forward connector of the referenced bundle to link from")
    String fromConnectorId;

    @Option(names = {"-K", "--from-key-path"}, description = "signing key of the referenced bundle's organization; when set the referenced bundle is updated with a forward connector")
    String fromKeyPath;

    @Option(names = {"-s", "--storage-base-url"}, required = true, description = "base url of prov storage")
    String storageUrlBase;

    @Option(names = {"-d", "--directory"}, description = "bundles output directory")
    String outputFolder;

    @Option(names = {"-g", "--create-graph"}, description = "Creates a graph representation of the bundle. Will be ignored is directory is not set. Requires graphviz to work.")
    boolean createGraph;

    String storageUrlBaseInternal = "http://prov-storage-hospital:8000/";

    @Override
    public void run() {
        Commands.LinkBundle(
            storageUrlBase,
            storageUrlBaseInternal,
            organizationId,
            keyPath,
            bundleName,
            branching,
            fromOrganizationId,
            fromBundleId,
            fromConnectorId,
            fromKeyPath,
            outputFolder,
            createGraph
        );
    }
}
