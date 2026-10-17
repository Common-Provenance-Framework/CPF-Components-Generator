package org.commonprovenanceframework.componentsgenerator.core;

import java.util.List;

public class Commands {
    public static List<GeneratedBundle> GenerateChain(
            int provenanceChainLength,
            int branching,
            String bundleNameBase,
            String organizationId,
            StorageEndpoint storage,
            String keyPath,
            String outputFolder,
            boolean createGraph
    ) {
        return GenerateChain.Execute(
                provenanceChainLength,
                branching,
                bundleNameBase,
                organizationId,
                storage,
                keyPath,
                outputFolder,
                createGraph
        );
    }


    public static String PopulateBundle(
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
            String externalId,
            String outputFolder,
            boolean createGraph
    ) {
        return PopulateBundle.Execute(
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
                externalId,
                outputFolder,
                createGraph
        );
    }

    public static GeneratedBundle LinkBundle(
            StorageEndpoint storage,
            String organizationId,
            String keyPath,
            String bundleName,
            int branching,
            List<LinkSource> sources,
            String outputFolder,
            boolean createGraph
    ) {
        return LinkBundle.Execute(
                storage,
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
