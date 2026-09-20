package org.commonprovenanceframework.componentsgenerator.core;

import cz.muni.fi.cpm.divided.ordered.CpmOrderedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.template.schema.HashAlgorithms;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LinkBundle {
    private static final String StoragePrefix = "storage";
    private static final String MetaPrefix = "meta";

    public static GeneratedBundle Execute(
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
        if (storageUrlBase == null || keyPath == null) {
            throw new IllegalArgumentException("Storage url base and key path must be set.");
        }
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one link source must be set.");
        }
        if (branching <= 0) {
            throw new IllegalArgumentException("Branching must be a positive integer.");
        }

        var pF = new ProvFactory();
        var cPF = new CpmProvFactory(pF);
        var serializer = new CustomSerializer();
        var metaUrl = storageUrlBaseInternal + "api/v1/documents/meta/";

        var backwardConnectors = new ArrayList<ForwardConnectorMetadata>();
        var resolved = new ArrayList<ResolvedSource>();

        for (LinkSource source : sources) {
            if (source.organizationId() == null || source.bundleId() == null) {
                throw new IllegalArgumentException("Source organization id and bundle id must be set.");
            }
            var fromStorageUrl = storageUrlBaseInternal
                + "api/v1/organizations/" + source.organizationId() + "/documents/";

            var fromDocument = ProvenanceStorageClient.getDocument(
                storageUrlBase, source.organizationId(), source.bundleId());
            var fromCpm = new CpmDocument(fromDocument.getDocument(), pF, cPF, new CpmOrderedFactory());

            INode fromConnector = fromCpm.getForwardConnectors().stream()
                .filter(fc -> source.connectorId() == null
                    || fc.getId().getLocalPart().equals(source.connectorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "No forward connector " + source.connectorId()
                        + " in " + source.organizationId() + "/" + source.bundleId()));

            backwardConnectors.add(new ForwardConnectorMetadata(
                fromConnector.getId(),
                pF.newQualifiedName(fromStorageUrl, source.bundleId(),
                    StoragePrefix + "_" + source.organizationId()),
                pF.newQualifiedName(metaUrl, source.bundleId() + "_meta", MetaPrefix),
                fromDocument.getHash(),
                HashAlgorithms.SHA256
            ));
            resolved.add(new ResolvedSource(source, fromCpm, fromConnector));
        }

        var generator = new ComponentGenerator(storageUrlBaseInternal, organizationId);
        var newDocument = generator.createBundle(bundleName, branching, backwardConnectors, List.of(), Map.of());
        var newDocumentJson = serializer.createProvStorageJson(newDocument.toDocument());

        ProvenanceStorageClient.storeDocument(
            storageUrlBase,
            newDocumentJson,
            newDocument.getBundleId().getLocalPart(),
            organizationId,
            keyPath,
            false
        );

        for (ResolvedSource source : resolved) {
            if (source.source().keyPath() == null) {
                continue;
            }
            var receiverBundleId = newDocument.getBundleId();
            var receiverPrefix = StoragePrefix + "_" + organizationId;

            var referencedBundle = generator.addSpecializedForwardConnector(
                source.document(),
                source.connector(),
                pF.newQualifiedName(
                    receiverBundleId.getNamespaceURI(),
                    receiverBundleId.getLocalPart(),
                    receiverPrefix),
                pF.newQualifiedName(metaUrl, bundleName + "_meta", MetaPrefix),
                CustomSerializer.ProvStorageJsonHash(newDocumentJson)
            );

            var bundle = (Bundle) referencedBundle.getStatementOrBundle().getFirst();
            if (bundle.getNamespace() != null) {
                bundle.getNamespace().register(receiverPrefix, receiverBundleId.getNamespaceURI());
            }
            var currentId = bundle.getId();
            bundle.setId(pF.newQualifiedName(
                currentId.getNamespaceURI(),
                currentId.getLocalPart().split("-v")[0] + "-v" + System.currentTimeMillis(),
                currentId.getPrefix()));

            ProvenanceStorageClient.storeDocument(
                storageUrlBase,
                serializer.createProvStorageJson(referencedBundle),
                bundle.getId().getLocalPart(),
                source.source().organizationId(),
                source.source().keyPath(),
                false
            );
        }

        if (outputFolder != null) {
            ComponentGenerator.exportDocument(newDocument.toDocument(), outputFolder + newDocument.getBundleId().getLocalPart(), createGraph);
        }

        System.out.println("Linked bundle " + newDocument.getBundleId().getLocalPart()
            + " to " + sources.size() + " source(s)");

        return new GeneratedBundle(
            bundleName,
            newDocument.getBundleId().getLocalPart(),
            newDocument.getForwardConnectors().stream().map(fc -> fc.getId().getLocalPart()).toList());
    }

    private record ResolvedSource(LinkSource source, CpmDocument document, INode connector) {
    }
}
