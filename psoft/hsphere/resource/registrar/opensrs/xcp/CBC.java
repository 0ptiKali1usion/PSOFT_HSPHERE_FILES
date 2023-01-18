package psoft.hsphere.resource.registrar.opensrs.xcp;

import com.sun.crypto.provider.SunJCE;
import java.security.AlgorithmParameters;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import psoft.encryption.MD5;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/CBC.class */
public class CBC {
    SecretKeySpec skeySpec;

    /* renamed from: iv */
    private byte[] f213iv = {0, 0, 0, 0, 0, 0, 0, 0};
    private Cipher ecipher;
    private Cipher dcipher;

    public CBC(String pKey) throws Exception {
        byte[] key = new byte[56];
        int i = 0;
        int j = 0;
        while (i < key.length) {
            try {
                key[i] = (byte) Integer.parseInt(pKey.substring(j, j + 2), 16);
                i++;
                j += 2;
            } catch (IndexOutOfBoundsException ex) {
                Session.getLog().error("Error forming OSRS key:", ex);
                throw new Exception("Invalid OSRS key. Please check the key for OSRS registrar in H-Sphere registrar settings");
            }
        }
        MD5 md5 = new MD5();
        byte[] material = new byte[56];
        md5.Update(key);
        System.arraycopy(md5.Final(), 0, material, 0, 16);
        md5.Init();
        md5.Update(material, 0, 16);
        System.arraycopy(md5.Final(), 0, material, 16, 16);
        md5.Init();
        md5.Update(material, 0, 32);
        System.arraycopy(md5.Final(), 0, material, 32, 16);
        md5.Init();
        md5.Update(material, 0, 48);
        System.arraycopy(md5.Final(), 0, material, 48, 8);
        SunJCE sunJCE = new SunJCE();
        if (Security.getProviders().length > 0) {
            if (Security.getProviders()[0] != null) {
                if (!Security.getProviders()[0].getName().equals(sunJCE.getName())) {
                    Security.removeProvider(sunJCE.getName());
                    Security.insertProviderAt(sunJCE, 1);
                }
            } else {
                Security.insertProviderAt(sunJCE, 1);
            }
        } else {
            Security.addProvider(sunJCE);
        }
        this.skeySpec = new SecretKeySpec(material, "Blowfish");
        AlgorithmParameters algParams = AlgorithmParameters.getInstance("Blowfish");
        algParams.init(new IvParameterSpec(this.f213iv));
        this.ecipher = Cipher.getInstance("Blowfish/CBC/NoPadding");
        this.ecipher.init(1, this.skeySpec, algParams);
        this.dcipher = Cipher.getInstance("Blowfish/CBC/NoPadding");
        this.dcipher.init(2, this.skeySpec, algParams);
    }

    public byte[] challenge(byte[] input) throws Exception {
        MD5 md5 = new MD5();
        md5.Init();
        md5.Update(input);
        byte[] message = new byte[16];
        System.arraycopy(md5.Final(), 0, message, 0, 16);
        return message;
    }

    public byte[] encrypt(byte[] input) throws Exception {
        int np = 8 - (input.length % 8);
        byte[] buf = new byte[input.length + np];
        for (int i = input.length; i < input.length + np; i++) {
            buf[input.length] = (byte) np;
        }
        System.arraycopy(input, 0, buf, 0, input.length);
        byte[] result = this.ecipher.doFinal(buf);
        byte[] buf2 = new byte["RandomIV".length() + 8 + result.length];
        System.arraycopy("RandomIV".getBytes(), 0, buf2, 0, "RandomIV".length());
        System.arraycopy(this.f213iv, 0, buf2, "RandomIV".length(), 8);
        System.arraycopy(result, 0, buf2, "RandomIV".length() + 8, result.length);
        return buf2;
    }

    public byte[] decrypt(byte[] input) throws Exception {
        byte[] buf = this.dcipher.doFinal(input);
        int len = (buf.length - 16) - buf[buf.length - 1];
        byte[] result = new byte[len];
        System.arraycopy(buf, 16, result, 0, len);
        return result;
    }
}
