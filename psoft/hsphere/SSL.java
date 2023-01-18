package psoft.hsphere;

import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.SSLProperties;

/* loaded from: hsphere.zip:psoft/hsphere/SSL.class */
public interface SSL extends IPDeletedResource {
    SSLProperties getProperties();

    void setProperties(SSLProperties sSLProperties);

    void installCertificatePair(SSLProperties sSLProperties) throws Exception;

    void updateCertificate(SSLProperties sSLProperties) throws Exception;

    void installCACert(SSLProperties sSLProperties) throws Exception;

    void installCertificateChain(SSLProperties sSLProperties) throws Exception;

    void installRevocationCert(SSLProperties sSLProperties) throws Exception;

    void updateCertificateProperties(SSLProperties sSLProperties) throws Exception;

    void checkInstallPossibility() throws Exception;
}
