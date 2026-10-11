package org.commonprovenanceframework.componentsgenerator.core;

/**
 * A provenance storage: the url requests are sent to, and the url its document identifiers must
 * start with. They differ only when the storage is reached under another name than its own,
 * for example through a port published on localhost.
 */
public record StorageEndpoint(String baseUrl, String namespaceUrl) {

    public StorageEndpoint {
        baseUrl = withoutTrailingSlash(baseUrl);
        namespaceUrl = withoutTrailingSlash(namespaceUrl);
        if (namespaceUrl == null) {
            namespaceUrl = baseUrl;
        }
        if (namespaceUrl == null) {
            throw new IllegalArgumentException("Storage base url or namespace url must be set.");
        }
    }

    public static StorageEndpoint of(String baseUrl) {
        return new StorageEndpoint(baseUrl, null);
    }

    String organizationsNamespace() {
        return namespaceUrl + "/api/v1/organizations/";
    }

    String documentsNamespace(String organizationId) {
        return organizationsNamespace() + organizationId + "/documents/";
    }

    String metaNamespace() {
        return namespaceUrl + "/api/v1/documents/meta/";
    }

    private static String withoutTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
