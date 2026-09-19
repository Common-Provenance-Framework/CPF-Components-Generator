package cz.muni.fi.components_generator.core;

import cz.muni.fi.cpm.divided.ordered.CpmOrderedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.template.schema.HashAlgorithms;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.util.List;
import java.util.Map;

class LinkBundle {
    private static final String StoragePrefix = "storage";
    private static final String MetaPrefix = "meta";

    public static void Execute(
        String storageUrlBase,
        String storageUrlBaseInternal,
        String organizationId,
        String keyPath,
        String bundleName,
        int branching,
        String fromOrganizationId,
        String fromBundleId,
        String fromConnectorId,
        String fromKeyPath,
        String outputFolder,
        boolean createGraph
    ) {
        if (storageUrlBase == null || keyPath == null) {
            throw new RuntimeException("Storage url base and key path must be set.");
        }
        if (fromOrganizationId == null || fromBundleId == null) {
            throw new RuntimeException("Source organization id and bundle id must be set.");
        }
        if (branching <= 0) {
            throw new RuntimeException("Branching must be a positive integer.");
        }

        var pF = new ProvFactory();
        var cPF = new CpmProvFactory(pF);
        var serializer = new CustomSerializer();
        var metaUrl = storageUrlBaseInternal + "api/v1/documents/meta/";
        var fromStorageUrl = storageUrlBaseInternal + "api/v1/organizations/" + fromOrganizationId + "/documents/";

        var fromDocument = ProvenanceStorageClient.getDocument(storageUrlBase, fromOrganizationId, fromBundleId);
        var fromCpm = new CpmDocument(fromDocument.getDocument(), pF, cPF, new CpmOrderedFactory());

        INode fromConnector = fromCpm.getForwardConnectors().stream()
            .filter(fc -> fromConnectorId == null || fc.getId().getLocalPart().equals(fromConnectorId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No forward connector found for id: " + fromConnectorId));

        var backwardConnector = new ForwardConnectorMetadata(
            fromConnector.getId(),
            pF.newQualifiedName(fromStorageUrl, fromBundleId, StoragePrefix),
            pF.newQualifiedName(metaUrl, fromBundleId + "_meta", MetaPrefix),
            fromDocument.getHash(),
            HashAlgorithms.SHA256
        );

        var generator = new ComponentGenerator(storageUrlBaseInternal, organizationId);
        var newDocument = generator.createBundle(bundleName, branching, List.of(backwardConnector), List.of(), Map.of());
        var newDocumentJson = serializer.createProvStorageJson(newDocument.toDocument());

        ProvenanceStorageClient.storeDocument(
            storageUrlBase,
            newDocumentJson,
            newDocument.getBundleId().getLocalPart(),
            organizationId,
            keyPath,
            false
        );

        if (fromKeyPath != null) {
            var referencedBundle = generator.addSpecializedForwardConnector(
                fromCpm,
                fromConnector,
                newDocument.getBundleId(),
                pF.newQualifiedName(metaUrl, bundleName + "_meta", MetaPrefix),
                CustomSerializer.ProvStorageJsonHash(newDocumentJson)
            );
            var referencedBundleJson = serializer.createProvStorageJson(referencedBundle);
            ProvenanceStorageClient.storeDocument(
                storageUrlBase,
                referencedBundleJson,
                fromBundleId,
                fromOrganizationId,
                fromKeyPath,
                true
            );
        }

        if (outputFolder != null) {
            ComponentGenerator.exportDocument(newDocument.toDocument(), outputFolder + newDocument.getBundleId().getLocalPart(), createGraph);
        }

        System.out.println("Linked bundle " + newDocument.getBundleId().getLocalPart()
            + " to " + fromOrganizationId + "/" + fromBundleId);
    }
}
