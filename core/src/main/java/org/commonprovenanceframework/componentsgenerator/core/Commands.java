package org.commonprovenanceframework.componentsgenerator.core;

import java.util.List;

public class Commands {
    public static List<GeneratedBundle> GenerateChain(
            int provenanceChainLength,
            int branching,
            String bundleNameBase,
            String organizationId,
            String storageUrlBase,
            String keyPath,
            String outputFolder,
            boolean createGraph,
            String storageUrlBaseInternal

    ) {
        return GenerateChain.Execute(
                provenanceChainLength,
                branching,
                bundleNameBase,
                organizationId,
                storageUrlBase,
                keyPath,
                outputFolder,
                createGraph,
                storageUrlBaseInternal
        );
    }


    public static void PopulateBundle(
            String bundlePath,
            String storageUrlBase,
            String orgId,
            String keyPath,
            String bundleId,
            String connectorId,
            int forwardDistance,
            int backwardDistance,
            int entityCount,
            String type,
            String outputFolder,
            boolean createGraph
    ) {
        PopulateBundle.Execute(
                bundlePath,
                storageUrlBase,
                orgId,
                keyPath,
                bundleId,
                connectorId,
                forwardDistance,
                backwardDistance,
                entityCount,
                type,
                outputFolder,
                createGraph
        );
    }

    public static GeneratedBundle LinkBundle(
            String storageUrlBase,
            String storageUrlBaseInternal,
            String organizationId,
            String keyPath,
            String bundleName,
            int branching,
            List<LinkSource> sources,
            String outputFolder,
            boolean createGraph
    ) {
        return LinkBundle.Execute(
                storageUrlBase,
                storageUrlBaseInternal,
                organizationId,
                keyPath,
                bundleName,
                branching,
                sources,
                outputFolder,
                createGraph
        );
    }

    public static void RegisterOrganisation(
            String storageUrlBase,
            String[] intermediateCertificates,
            String clientCertificate,
            String lastIntermediateKey,
            String outputFolder,
            String organizationId
    ) {
        RegisterOrganisation.Execute(
                storageUrlBase,
                intermediateCertificates,
                clientCertificate,
                lastIntermediateKey,
                outputFolder,
                organizationId
        );
    }
}
