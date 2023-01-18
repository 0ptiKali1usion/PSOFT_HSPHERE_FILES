package psoft.hsphere.tools;

import cryptix.jce.provider.CryptixCrypto;
import cryptix.message.KeyBundleMessage;
import cryptix.message.MessageFactory;
import cryptix.openpgp.provider.CryptixOpenPGP;
import cryptix.pki.KeyBundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Collection;
import psoft.hsphere.C0004CP;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PGPEncrypter.class */
public class PGPEncrypter extends C0004CP {
    public PGPEncrypter() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        String message = null;
        String messageFile = null;
        String publicKeyFile = null;
        String encryptedMessageFile = null;
        String messageToEncrypt = null;
        for (int i = 0; i < argv.length; i++) {
            if ("-m".equals(argv[i]) || "--message".equals(argv[i])) {
                message = new String(argv[i + 1]);
            }
            if ("-mf".equals(argv[i]) || "--messagefile".equals(argv[i])) {
                messageFile = new String(argv[i + 1]);
            }
            if ("-k".equals(argv[i]) || "--key".equals(argv[i])) {
                publicKeyFile = new String(argv[i + 1]);
            }
            if ("-f".equals(argv[i]) || "--file".equals(argv[i])) {
                encryptedMessageFile = new String(argv[i + 1]);
            }
        }
        if ((message != null || messageFile != null) && publicKeyFile != null) {
            try {
                if (encryptedMessageFile != null) {
                    try {
                        Security.addProvider(new CryptixCrypto());
                        Security.addProvider(new CryptixOpenPGP());
                        FileInputStream in = new FileInputStream(publicKeyFile);
                        MessageFactory mf = MessageFactory.getInstance("OpenPGP");
                        Collection msgs = mf.generateMessages(in);
                        in.close();
                        KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
                        KeyBundle publicKeyBundle = kbm.getKeyBundle();
                        if (message != null && messageFile == null) {
                            messageToEncrypt = message;
                        } else if (message != null || messageFile == null) {
                            PGPMessageSigner.printHelp();
                        } else {
                            try {
                                File f = new File(messageFile);
                                byte[] buffer = new byte[(int) f.length()];
                                new FileInputStream(f).read(buffer);
                                messageToEncrypt = new String(buffer);
                            } catch (IOException ioex) {
                                ioex.printStackTrace();
                                System.out.println("Encrypting failed ");
                                System.out.println("Can not open file of message has been encrypted ");
                                System.exit(-1);
                            }
                        }
                        String encryptedMessage = C0007PGPSecurity.encrypt(messageToEncrypt, publicKeyBundle);
                        FileOutputStream out = new FileOutputStream(encryptedMessageFile);
                        out.write(encryptedMessage.getBytes());
                        out.close();
                        System.out.println("Finished at " + TimeUtils.getDate());
                    } catch (IOException ioex2) {
                        ioex2.printStackTrace();
                        System.out.println("Encrypting failed ");
                        System.out.println("Can not open public key file ");
                        System.exit(-1);
                        System.out.println("Finished at " + TimeUtils.getDate());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Encrypting failed ");
                        System.exit(-1);
                        System.out.println("Finished at " + TimeUtils.getDate());
                    }
                    System.out.println("Message encryption finished");
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
        System.out.println("H-Sphere PGP encryptor.");
        System.out.println("Usage java psoft.hsphere.tools.PGPEncrypter");
        System.out.println("   -m|--message <Message to encrypt> or");
        System.out.println("   -mf|--messagefile </path/to/file/with/message/to/encrypt>");
        System.out.println("   -k|--key </path/to/public/key/file>");
        System.out.println("   -f|--file </path/to/file/for/encrypted/message>");
    }
}
