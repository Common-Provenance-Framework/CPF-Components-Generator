package org.commonprovenanceframework.componentsgenerator.core;

import java.util.List;
import java.util.Map;

public record GeneratedBundle(String bundleName, String bundleId, List<String> connectorIds, Map<String, String> sourceVersions) {
}
