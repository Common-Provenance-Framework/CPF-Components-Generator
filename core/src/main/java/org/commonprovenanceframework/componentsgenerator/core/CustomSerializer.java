package org.commonprovenanceframework.componentsgenerator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.interop.Formats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;

class CustomSerializer {

    public final ProvFactory pF;
    private final InteropFramework interop;

    public CustomSerializer() {
        this.pF = new org.openprovenance.prov.vanilla.ProvFactory();
        this.interop = new InteropFramework();
    }

    public Document readDocument(String path) {
        try (InputStream inputStream = new FileInputStream(path)) {
            var document = interop.readDocument(inputStream, Formats.ProvFormat.JSON);
            RenameBundle(document);

            return document;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String createProvStorageJson(Document doc) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            interop.writeDocument(outputStream, doc, Formats.ProvFormat.JSON);

            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            ObjectMapper mapper = new ObjectMapper();

            JsonNode json = mapper.readTree(inputStream);
            removeJsonKeyRecursive((ObjectNode) json, "@id");
            moveDocumentPrefixIntoBundles((ObjectNode) json);

            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void moveDocumentPrefixIntoBundles(ObjectNode root) {
        JsonNode documentPrefix = root.remove("prefix");
        if (documentPrefix == null || !documentPrefix.isObject()) {
            return;
        }
        JsonNode bundles = root.get("bundle");
        if (bundles == null || !bundles.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> bundleFields = bundles.fields();
        while (bundleFields.hasNext()) {
            Map.Entry<String, JsonNode> bundleEntry = bundleFields.next();
            JsonNode bundle = bundleEntry.getValue();
            if (!bundle.isObject()) {
                continue;
            }
            ObjectNode bundleNode = (ObjectNode) bundle;
            ObjectNode bundlePrefix = bundleNode.has("prefix") && bundleNode.get("prefix").isObject()
                    ? (ObjectNode) bundleNode.get("prefix")
                    : bundleNode.putObject("prefix");

            Iterator<Map.Entry<String, JsonNode>> declarations = documentPrefix.fields();
            while (declarations.hasNext()) {
                Map.Entry<String, JsonNode> declaration = declarations.next();
                if (!bundlePrefix.has(declaration.getKey())) {
                    bundlePrefix.set(declaration.getKey(), declaration.getValue());
                }
            }

            bundleNode.remove("prefix");
            ObjectNode reordered = bundleNode.objectNode();
            reordered.set("prefix", bundlePrefix);
            reordered.setAll(bundleNode);
            bundleEntry.setValue(reordered);
        }
    }

    private void removeJsonKeyRecursive(ObjectNode node, String keyToRemove) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();

            if (entry.getKey().equals(keyToRemove)) {
                fields.remove();
            } else if (entry.getValue().isObject()) {
                removeJsonKeyRecursive((ObjectNode) entry.getValue(), keyToRemove);
            } else if (entry.getValue().isArray()) {
                for (JsonNode element : entry.getValue()) {
                    if (element.isObject()) {
                        removeJsonKeyRecursive((ObjectNode) element, keyToRemove);
                    }
                }
            }
        }
    }

    public static void AddIdToBundle(JsonNode document) {
        var bundle = document.get("bundle");
        var fields = bundle.fields();
        if (fields.hasNext()) {
            var bundleItem = fields.next();

            var key = bundleItem.getKey();
            var innerObj = (ObjectNode) bundle.get(key);
            innerObj.put("@id", key);
        }
    }

    public static void RenameBundle(Document document) {
        var bundle = (Bundle) document.getStatementOrBundle().getFirst();
        var prefix = bundle.getId().getPrefix();
        var namespaceUri = resolvePrefix(bundle.getNamespace(), prefix);
        if (namespaceUri == null) {
            namespaceUri = resolvePrefix(document.getNamespace(), prefix);
        }
        if (namespaceUri == null) {
            return;
        }
        var pF = new org.openprovenance.prov.vanilla.ProvFactory();
        var updatedBundleId = pF.newQualifiedName(namespaceUri, bundle.getId().getLocalPart(), prefix);
        bundle.setId(updatedBundleId);
    }

    private static String resolvePrefix(Namespace namespace, String prefix) {
        return namespace == null ? null : namespace.getPrefixes().get(prefix);
    }

    public static String ProvStorageJsonHash(String documentJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(documentJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
