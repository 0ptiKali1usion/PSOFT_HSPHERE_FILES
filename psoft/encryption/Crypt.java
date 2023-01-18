package psoft.encryption;

import com.sun.crypto.provider.SunJCE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import javax.crypto.Cipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.util.Base64;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/encryption/Crypt.class */
public class Crypt {
    private static final int DATABLOCK = 1;
    private static final int FINALDATABLOCK = 2;

    static {
        Security.addProvider(new SunJCE());
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair generateKeyPair(int length) throws CryptException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(length);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new CryptException("Error generating key", e);
        }
    }

    public static KeyPair generateKeyPair() throws CryptException {
        return generateKeyPair(1024);
    }

    public static byte[] encodePrivateKey(PrivateKey key) throws CryptException {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = (PKCS8EncodedKeySpec) kf.getKeySpec(key, PKCS8EncodedKeySpec.class);
            return keySpec.getEncoded();
        } catch (Exception e) {
            throw new CryptException("Error encoding key", e);
        }
    }

    public static byte[] encodePublicKey(PublicKey key) throws CryptException {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = (X509EncodedKeySpec) kf.getKeySpec(key, X509EncodedKeySpec.class);
            return keySpec.getEncoded();
        } catch (Exception e) {
            throw new CryptException("Error encoding key", e);
        }
    }

    public static PrivateKey decodePrivateKey(byte[] data) throws CryptException {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
            return kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new CryptException("Error encoding key", e);
        }
    }

    public static PublicKey decodePublicKey(byte[] data) throws CryptException {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(data);
            return kf.generatePublic(keySpec);
        } catch (Exception e) {
            throw new CryptException("Error encoding key", e);
        }
    }

    public static byte[] encrypt(PublicKey key, byte[] data) throws CryptException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
            cipher.init(1, key);
            return cipher.doFinal(data, 0, data.length);
        } catch (Exception e) {
            throw new CryptException("Error encrypting data", e);
        }
    }

    public static byte[] decrypt(PrivateKey key, byte[] data) throws CryptException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
            cipher.init(2, key);
            return cipher.doFinal(data, 0, data.length);
        } catch (Exception e) {
            throw new CryptException("Error decrypting data", e);
        }
    }

    public static String lencrypt(PublicKey key, String data) throws CryptException {
        int len;
        int type;
        String result = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
            cipher.init(1, key);
            int blockSize = cipher.getBlockSize() - 1;
            byte[] bdata = data.getBytes();
            int overallLen = bdata.length;
            int curPos = 0;
            while (curPos < overallLen) {
                if (curPos + blockSize < overallLen) {
                    len = blockSize;
                    type = 1;
                } else {
                    len = overallLen - curPos;
                    type = 2;
                }
                byte[] out = cipher.doFinal(bdata, curPos, len);
                String tmp = Base64.encode(out);
                String slen = Integer.toString(tmp.length());
                String result2 = result + Integer.toString(type);
                for (int i = slen.length(); i < 5; i++) {
                    result2 = result2 + "0";
                }
                result = result2 + slen + tmp;
                curPos += len;
            }
            return result;
        } catch (Exception ex) {
            throw new CryptException("Error encrypting data", ex);
        }
    }

    public static String ldecrypt(PrivateKey key, String data) throws CryptException {
        String result = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
            cipher.init(2, key);
            boolean process = true;
            int curPos = 0;
            while (process) {
                int type = Integer.parseInt(data.substring(curPos, curPos + 1));
                int len = Integer.parseInt(data.substring(curPos + 1, curPos + 6));
                String tmp = data.substring(curPos + 6, curPos + 6 + len);
                byte[] decoded = Base64.decode(tmp);
                result = result + new String(cipher.doFinal(decoded, 0, decoded.length));
                curPos += len + 6;
                if (type != 1) {
                    process = false;
                }
            }
            return result;
        } catch (Exception e) {
            throw new CryptException("Error decrypting data", e);
        }
    }

    public static synchronized void saveKey(PublicKey key) throws Exception {
        byte[] encodedKey = encodePublicKey(key);
        String base64encoded = Base64.encode(encodedKey);
        Session.getLog().debug("Base 64 encoded key: " + base64encoded);
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            try {
                Session.setUser(User.getUser(FMACLManager.ADMIN));
                Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
                Settings.get().setLargeValue("public_enc_key", base64encoded);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
            } catch (Exception ex) {
                Session.getLog().error("Problem storing public encryption key", ex);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
            }
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public static synchronized PublicKey loadKey() throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            Session.setUser(User.getUser(FMACLManager.ADMIN));
            Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
            String base64encoded = Settings.get().getValue("public_enc_key");
            byte[] decoded = Base64.decode(base64encoded);
            PublicKey decodePublicKey = decodePublicKey(decoded);
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            return decodePublicKey;
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public static void main(String[] args) throws Exception {
        PublicKey publicKey;
        PrivateKey privateKey;
        if (args.length != 2) {
            System.err.println("File & keystore");
            System.exit(1);
        }
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            System.out.println("Provider " + i);
            System.out.println(providers[i].toString());
            System.out.println(providers[i].getInfo());
            Enumeration names = providers[i].propertyNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement().toString();
                System.out.println(name + "=" + providers[i].getProperty(name));
            }
            System.out.println();
        }
        String filename = args[0];
        String keystoreName = args[1];
        File keystoreFile = new File(keystoreName);
        if (!keystoreFile.exists()) {
            System.out.println("Generating key pair...");
            KeyPair keyPair = generateKeyPair();
            System.out.println("Done.");
            System.out.println("====================");
            System.out.println("saving Public key to keystore named " + keystoreName + " ...");
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            saveKey(publicKey);
            System.out.println("Done.");
            System.out.println("====================");
            System.out.println("saving private key to file named " + keystoreName + ".priv ...");
            byte[] encodedKey = encodePrivateKey(privateKey);
            FileOutputStream fos = new FileOutputStream(keystoreName + ".priv");
            fos.write(encodedKey, 0, encodedKey.length);
            fos.close();
            System.out.println("Done.");
        } else {
            System.out.println("Loading public key from database ");
            publicKey = loadKey();
            System.out.println("Done.");
            System.out.println("====================");
            System.out.println("Loading private key from " + keystoreName + ".priv");
            File privKeyFile = new File(keystoreName + ".priv");
            FileInputStream fis = new FileInputStream(privKeyFile);
            byte[] encodedKey2 = new byte[(int) privKeyFile.length()];
            fis.read(encodedKey2);
            fis.close();
            privateKey = decodePrivateKey(encodedKey2);
        }
        System.out.println("====================");
        System.out.println("encrypting file " + filename + " with public key....");
        File file = new File(filename);
        byte[] buf = new byte[(int) file.length()];
        FileInputStream fis2 = new FileInputStream(file);
        fis2.read(buf);
        fis2.close();
        byte[] crypt = encrypt(publicKey, buf);
        System.out.println("Done.");
        System.out.println("====================");
        System.out.println("Saving encrypted data as " + filename + ".enc ...");
        FileOutputStream fos2 = new FileOutputStream(filename + ".enc");
        fos2.write(crypt, 0, crypt.length);
        fos2.close();
        System.out.println("Done.");
        System.out.println("====================");
        System.out.println("decrypting data using the private key ...");
        byte[] decrypted = decrypt(privateKey, crypt);
        System.out.println("Done.");
        System.out.println("====================");
        System.out.println("Saving decrypted data as " + filename + ".dec ...");
        FileOutputStream fos3 = new FileOutputStream(filename + ".dec");
        fos3.write(decrypted);
        fos3.close();
    }
}
