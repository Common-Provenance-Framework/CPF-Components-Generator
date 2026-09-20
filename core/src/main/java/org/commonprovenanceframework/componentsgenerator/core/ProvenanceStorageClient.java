package org.commonprovenanceframework.componentsgenerator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.interop.Formats;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;

class ProvenanceStorageClient {
    public static void storeDocument(String baseUrl, String documentJson, String bundleId, String orgId, String keyPath, boolean update) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            var base64doc = Base64.getEncoder().encodeToString(documentJson.getBytes(StandardCharsets.UTF_8));

            var jsonBody = objectMapper.createObjectNode();
            jsonBody.put("graph", base64doc);
            jsonBody.put("graphFormat", "JSON");
            jsonBody.put("signature", Certificates.createSignature(documentJson, keyPath));

            try (HttpClient client = HttpClient.newHttpClient()) {
                var url = MessageFormat.format(
                        "{0}/api/v1/organizations/{1}/documents",
                        baseUrl,
                        orgId
                );

                var request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                        .build();

                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException(response.body());
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String documentDigest(JsonNode root) {
        var digest = root.path("token").path("data").path("documentDigest").asText("");
        if (!digest.isBlank()) {
            return digest;
        }
        var jwt = root.hasNonNull("jwt") ? root.path("jwt").asText("") : root.path("token").path("jwt").asText("");
        var parts = jwt.split("\\.");
        if (parts.length < 2) {
            return "";
        }
        try {
            var payload = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(parts[1]));
            return payload.path("doc_digest").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    public static HashedDocument getDocument(String baseUrl, String orgId, String bundleId) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var url = MessageFormat.format(
                    "{0}/api/v1/organizations/{1}/documents/{2}",
                    baseUrl,
                    orgId,
                    bundleId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(response.body());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            var base64Doc = root.hasNonNull("graph") ? root.path("graph").asText() : root.path("document").asText();
            var hash = documentDigest(root);
            JsonNode documentJsonNode = mapper.readTree(Base64.getDecoder().decode(base64Doc));
            CustomSerializer.AddIdToBundle(documentJsonNode);

            var docJson = documentJsonNode.toString().replace("https://openprovenance.org/blank#", "https://openprovenance.org/blank");
            InputStream stream = new ByteArrayInputStream(docJson.getBytes(StandardCharsets.UTF_8));
            InteropFramework interop = new InteropFramework();

            var document = interop.readDocument(stream, Formats.ProvFormat.JSON);
            CustomSerializer.RenameBundle(document);

            return new HashedDocument(document, hash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
