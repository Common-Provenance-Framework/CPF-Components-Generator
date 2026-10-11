package org.commonprovenanceframework.componentsgenerator.core;

import cz.muni.fi.cpm.divided.ordered.CpmOrderedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.template.schema.HashAlgorithms;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LinkBundle {
    private static final String StoragePrefix = "storage";
    private static final String MetaPrefix = "meta";
    private static final String OrgPrefix = "org";

    public static GeneratedBundle Execute(
        StorageEndpoint storage,
        String organizationId,
        String keyPath,
        String bundleName,
        int branching,
        List<LinkSource> sources,
        String outputFolder,
        boolean createGraph
    ) {
        if (storage == null || storage.baseUrl() == null || keyPath == null) {
            throw new IllegalArgumentException("Storage url base and key path must be set.");
        }
        var storageUrlBase = storage.baseUrl();
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one link source must be set.");
        }
        if (branching <= 0) {
            throw new IllegalArgumentException("Branching must be a positive integer.");
        }

        var pF = new ProvFactory();
        var cPF = new CpmProvFactory(pF);
        var serializer = new CustomSerializer();
        var metaUrl = storage.metaNamespace();

        var backwardConnectors = new ArrayList<ForwardConnectorMetadata>();
        var resolved = new ArrayList<ResolvedSource>();

        for (LinkSource source : sources) {
            if (source.organizationId() == null || source.bundleId() == null) {
                throw new IllegalArgumentException("Source organization id and bundle id must be set.");
            }
            var fromStorage = source.storage() == null ? storage : source.storage();
            if (fromStorage.baseUrl() == null) {
                throw new IllegalArgumentException("Source storage base url must be set.");
            }
            var fromStorageUrl = fromStorage.documentsNamespace(source.organizationId());

            var fromDocument = ProvenanceStorageClient.getDocument(
                fromStorage.baseUrl(), source.organizationId(), source.bundleId());
            var fromCpm = new CpmDocument(fromDocument.getDocument(), pF, cPF, new CpmOrderedFactory());

            INode fromConnector = fromCpm.getForwardConnectors().stream()
                .filter(fc -> source.connectorId() == null
                    || fc.getId().getLocalPart().equals(source.connectorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "No forward connector " + source.connectorId()
                        + " in " + source.organizationId() + "/" + source.bundleId()));

            var sameStorage = fromStorage.namespaceUrl().equals(storage.namespaceUrl());
            backwardConnectors.add(new ForwardConnectorMetadata(
                fromConnector.getId(),
                pF.newQualifiedName(fromStorageUrl, source.bundleId(),
                    StoragePrefix + "_" + source.organizationId()),
                pF.newQualifiedName(fromStorage.metaNamespace(), source.bundleId() + "_meta",
                    prefix(MetaPrefix, source.organizationId(), sameStorage)),
                fromDocument.getHash(),
                HashAlgorithms.SHA256,
                pF.newQualifiedName(fromStorage.organizationsNamespace(), source.organizationId(),
                    prefix(OrgPrefix, source.organizationId(), sameStorage))
            ));
            var versionCpm = source.versionBundleId().equals(source.bundleId())
                ? fromCpm
                : new CpmDocument(ProvenanceStorageClient.getDocument(
                    fromStorage.baseUrl(), source.organizationId(), source.versionBundleId()).getDocument(),
                    pF, cPF, new CpmOrderedFactory());
            resolved.add(new ResolvedSource(source, fromStorage, versionCpm, fromConnector));
        }

        var generator = new ComponentGenerator(storage, organizationId);
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

        var sourceVersions = new LinkedHashMap<String, String>();
        for (ResolvedSource source : resolved) {
            if (source.source().keyPath() == null) {
                continue;
            }
            var receiverBundleId = newDocument.getBundleId();
            var sameStorage = source.storage().namespaceUrl().equals(storage.namespaceUrl());
            var receiver = pF.newQualifiedName(
                receiverBundleId.getNamespaceURI(),
                receiverBundleId.getLocalPart(),
                StoragePrefix + "_" + organizationId);
            var receiverMeta = pF.newQualifiedName(metaUrl, bundleName + "_meta",
                prefix(MetaPrefix, organizationId, sameStorage));
            var receiverAgent = pF.newQualifiedName(storage.organizationsNamespace(), organizationId,
                prefix(OrgPrefix, organizationId, sameStorage));

            var referencedBundle = generator.addSpecializedForwardConnector(
                source.document(),
                source.connector(),
                receiver,
                receiverMeta,
                CustomSerializer.ProvStorageJsonHash(newDocumentJson),
                receiverAgent
            );

            var bundle = (Bundle) referencedBundle.getStatementOrBundle().getFirst();
            if (bundle.getNamespace() != null) {
                bundle.getNamespace().register(receiver.getPrefix(), receiver.getNamespaceURI());
                if (!sameStorage) {
                    bundle.getNamespace().register(receiverMeta.getPrefix(), receiverMeta.getNamespaceURI());
                    bundle.getNamespace().register(receiverAgent.getPrefix(), receiverAgent.getNamespaceURI());
                }
            }
            var currentId = bundle.getId();
            bundle.setId(pF.newQualifiedName(
                currentId.getNamespaceURI(),
                currentId.getLocalPart().split("-v")[0] + "-v" + System.currentTimeMillis(),
                currentId.getPrefix()));

            ProvenanceStorageClient.storeDocument(
                source.storage().baseUrl(),
                serializer.createProvStorageJson(referencedBundle),
                bundle.getId().getLocalPart(),
                source.source().organizationId(),
                source.source().keyPath(),
                false
            );
            sourceVersions.put(source.source().bundleId(), bundle.getId().getLocalPart());
            System.out.println("Stored version " + bundle.getId().getLocalPart()
                + " of bundle " + source.source().bundleId() + " with a forward link");
        }

        if (outputFolder != null) {
            ComponentGenerator.exportDocument(newDocument.toDocument(), outputFolder + newDocument.getBundleId().getLocalPart(), createGraph);
        }

        System.out.println("Linked bundle " + newDocument.getBundleId().getLocalPart()
            + " to " + sources.size() + " source(s)");

        return new GeneratedBundle(
            bundleName,
            newDocument.getBundleId().getLocalPart(),
            newDocument.getForwardConnectors().stream().map(fc -> fc.getId().getLocalPart()).toList(),
            sourceVersions);
    }

    // A document's own prefixes already name its storage, so another storage needs prefixes of its own.
    private static String prefix(String prefix, String organizationId, boolean sameStorage) {
        return sameStorage ? prefix : prefix + "_" + organizationId;
    }

    private record ResolvedSource(LinkSource source, StorageEndpoint storage, CpmDocument document, INode connector) {
    }
}
