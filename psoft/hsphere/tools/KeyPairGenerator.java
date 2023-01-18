package psoft.hsphere.tools;

import java.io.FileOutputStream;
import psoft.hsphere.C0004CP;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.hsphere.PGPSecurity.PGPKeyPairStorage;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/KeyPairGenerator.class */
public class KeyPairGenerator extends C0004CP {
    public KeyPairGenerator() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        String identification = null;
        String subkeyIdentification = null;
        String encryptPhrase = null;
        String privateKeyFileName = null;
        String publicKeyFileName = null;
        for (int i = 0; i < argv.length; i++) {
            if ("-i".equals(argv[i]) || "--identification".equals(argv[i])) {
                identification = new String(argv[i + 1]);
            }
            if ("-s".equals(argv[i]) || "--subkeyidentification".equals(argv[i])) {
                subkeyIdentification = new String(argv[i + 1]);
            }
            if ("-e".equals(argv[i]) || "--encryptphrase".equals(argv[i])) {
                encryptPhrase = new String(argv[i + 1]);
            }
            if ("-prf".equals(argv[i]) || "--privatekeyfile".equals(argv[i])) {
                privateKeyFileName = new String(argv[i + 1]);
            }
            if ("-pcf".equals(argv[i]) || "--publickeyfile".equals(argv[i])) {
                publicKeyFileName = new String(argv[i + 1]);
            }
        }
        if (identification != null && subkeyIdentification != null && encryptPhrase != null && privateKeyFileName != null) {
            try {
                if (publicKeyFileName != null) {
                    try {
                        PGPKeyPairStorage pgpKPS = C0007PGPSecurity.produceKeyPair(identification, subkeyIdentification, encryptPhrase);
                        FileOutputStream out = new FileOutputStream(privateKeyFileName);
                        out.write(pgpKPS.privateKeyBundle.getEncoded());
                        out.close();
                        FileOutputStream out2 = new FileOutputStream(publicKeyFileName);
                        out2.write(pgpKPS.publicKeyBundle.getEncoded());
                        out2.close();
                        System.out.println("Finished at " + TimeUtils.getDate());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Generation failed ");
                        System.exit(-1);
                        System.out.println("Finished at " + TimeUtils.getDate());
                    }
                    System.out.println("PGP key pair generation finished");
                    System.exit(0);
                    return;
                }
            } catch (Throwable th) {
                System.out.println("Finished at " + TimeUtils.getDate());
                throw th;
            }
        }
        System.out.println("Missconfiguration ");
        printHelp();
    }

    public static void printHelp() {
        System.out.println("H-Sphere PGP key pair generator.");
        System.out.println("Usage java psoft.hsphere.tools.KeyPairGenerator -i|--identification <your identification string>");
        System.out.println("   -s|--subkeyidentification <your session key identification>");
        System.out.println("   -e|--encryptphrase <phrase for encryption/decryption private key>");
        System.out.println("   -prf|--privatekeyfile <file where private key will be saved>");
        System.out.println("   -pcf|--publickeyfile <file where public key will be saved>");
    }
}
