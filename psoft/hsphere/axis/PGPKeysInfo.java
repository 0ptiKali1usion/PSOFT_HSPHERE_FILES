package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/PGPKeysInfo.class */
public class PGPKeysInfo implements Serializable {
    private String privateKey;
    private String publicKey;
    private String encryptPhrase;

    public PGPKeysInfo() {
    }

    public PGPKeysInfo(String privateKey, String publicKey, String encryptPhrase) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.encryptPhrase = encryptPhrase;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncryptPhrase() {
        return this.encryptPhrase;
    }

    public void setEncryptPhrase(String encryptPhrase) {
        this.encryptPhrase = encryptPhrase;
    }
}
