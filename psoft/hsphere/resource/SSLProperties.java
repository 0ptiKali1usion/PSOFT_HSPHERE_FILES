package psoft.hsphere.resource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SSLProperties.class */
public class SSLProperties {
    private int rev;
    private int cert;
    private int chain;
    private int vclient;
    private int vdepth;
    private String siteName = "";
    private String revocationCertificate = "";
    private String certificateChain = "";
    private int forced;
    private int need128;
    private String key;
    private String certificate;
    private String certificateAuthority;

    public SSLProperties() {
    }

    public SSLProperties(SSLProperties sslp) {
        setRev(sslp.getRev());
        setCert(sslp.getCert());
        setChain(sslp.getChain());
        setVclient(sslp.getVclient());
        setVdepth(sslp.getVdepth());
        setSiteName(sslp.getSiteName());
        setForced(sslp.getForced());
        setNeed128(sslp.getNeed128());
        setKey(sslp.getKey());
        setCertificate(sslp.getCertificate());
        setCertificateAuthority(sslp.getCertificateAuthority());
        setRevocationCertificate(sslp.getRevocationCertificate());
    }

    public int getCert() {
        return this.cert;
    }

    public void setCert(int cert) {
        this.cert = cert;
    }

    public String getCertificate() {
        return this.certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public int getChain() {
        return this.chain;
    }

    public void setChain(int chain) {
        this.chain = chain;
    }

    public int getForced() {
        return this.forced;
    }

    public void setForced(int forced) {
        this.forced = forced;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getNeed128() {
        return this.need128;
    }

    public void setNeed128(int need128) {
        this.need128 = need128;
    }

    public int getRev() {
        return this.rev;
    }

    public void setRev(int rev) {
        this.rev = rev;
    }

    public int getVclient() {
        return this.vclient;
    }

    public void setVclient(int vclient) {
        this.vclient = vclient;
    }

    public int getVdepth() {
        return this.vdepth;
    }

    public void setVdepth(int vdepth) {
        this.vdepth = vdepth;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCertificateAuthority() {
        return this.certificateAuthority;
    }

    public void setCertificateAuthority(String certificateAuthority) {
        this.certificateAuthority = certificateAuthority;
    }

    public String getRevocationCertificate() {
        return this.revocationCertificate;
    }

    public void setRevocationCertificate(String revocationCertificate) {
        this.revocationCertificate = revocationCertificate;
    }

    public String getCertificateChain() {
        return this.certificateChain;
    }

    public void setCertificateChain(String certificateChain) {
        this.certificateChain = certificateChain;
    }
}
