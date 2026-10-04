package org.commonprovenanceframework.componentsgenerator.core;

import cz.muni.fi.cpm.template.schema.HashAlgorithms;
import org.openprovenance.prov.model.QualifiedName;

class ForwardConnectorMetadata {
    private final QualifiedName connectorId;
    private final QualifiedName referenceBundleId;
    private final QualifiedName referenceMetaBundleId;
    private final String referenceBundleHash;
    private final HashAlgorithms referenceBundleHashAlgorithm;
    private final String organizationId;

    public ForwardConnectorMetadata(QualifiedName connectorId, QualifiedName referenceBundleId, QualifiedName referenceMetaBundleId, String hash, HashAlgorithms referenceBundleHashAlgorithm, String organizationId) {
        this.connectorId = connectorId;
        this.referenceBundleId = referenceBundleId;
        this.referenceMetaBundleId = referenceMetaBundleId;
        this.referenceBundleHash = hash;
        this.referenceBundleHashAlgorithm = referenceBundleHashAlgorithm;
        this.organizationId = organizationId;
    }

    public QualifiedName getConnectorId() {
        return connectorId;
    }

    public QualifiedName getReferenceBundleId() {
        return referenceBundleId;
    }

    public QualifiedName getReferenceMetaBundleId() {
        return referenceMetaBundleId;
    }

    public String getReferenceBundleHash() {
        return referenceBundleHash;
    }

    public HashAlgorithms getReferenceBundleHashAlgorithm() {
        return referenceBundleHashAlgorithm;
    }

    public String getOrganizationId() {
        return organizationId;
    }
}
