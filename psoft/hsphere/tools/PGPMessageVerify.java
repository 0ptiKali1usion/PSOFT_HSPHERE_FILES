package psoft.hsphere.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import psoft.hsphere.C0004CP;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PGPMessageVerify.class */
public class PGPMessageVerify extends C0004CP {
    public PGPMessageVerify() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        String signedMessageFile = null;
        String publicKeyFile = null;
        for (int i = 0; i < argv.length; i++) {
            if ("-f".equals(argv[i]) || "--messagefile".equals(argv[i])) {
                signedMessageFile = new String(argv[i + 1]);
            }
            if ("-k".equals(argv[i]) || "--key".equals(argv[i])) {
                publicKeyFile = new String(argv[i + 1]);
            }
        }
        if (signedMessageFile == null || publicKeyFile == null) {
            System.out.println("Missconfiguration ");
            printHelp();
            return;
        }
        try {
            try {
                try {
                    File f = new File(publicKeyFile);
                    byte[] buffer = new byte[(int) f.length()];
                    FileInputStream in = new FileInputStream(f);
                    in.read(buffer);
                    String publicKey = new String(buffer);
                    File f2 = new File(signedMessageFile);
                    byte[] buffer2 = new byte[(int) f2.length()];
                    FileInputStream in2 = new FileInputStream(f2);
                    in2.read(buffer2);
                    String signedMessage = new String(buffer2);
                    int beginSignedIndex = signedMessage.indexOf(Ticket.PGP_SIGNIFICATION_BEGIN);
                    if (beginSignedIndex != -1) {
                        int endSignedIndex = signedMessage.indexOf(Ticket.PGP_SIGNIFICATION_END);
                        if (endSignedIndex != -1) {
                            signedMessage = signedMessage.substring(beginSignedIndex, endSignedIndex + Ticket.PGP_SIGNIFICATION_END.length());
                        }
                    } else {
                        System.out.println("Message is not signed");
                    }
                    boolean authenticated = C0007PGPSecurity.verifyMessage(signedMessage, publicKey);
                    if (authenticated) {
                        System.out.println("Signature OK");
                    } else {
                        System.out.println("Signature BAD");
                    }
                    System.out.println("Finished at " + TimeUtils.getDate());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Authentication failed ");
                    System.exit(-1);
                    System.out.println("Finished at " + TimeUtils.getDate());
                }
            } catch (IOException ioex) {
                ioex.printStackTrace();
                System.out.println("File access failed ");
                System.exit(-1);
                System.out.println("Finished at " + TimeUtils.getDate());
            }
            System.out.println("Message authentication finished");
            System.exit(0);
        } catch (Throwable th) {
            System.out.println("Finished at " + TimeUtils.getDate());
            throw th;
        }
    }

    public static void printHelp() {
        System.out.println("H-Sphere PGP message verify.");
        System.out.println("Usage java psoft.hsphere.tools.PGPMessageVerify");
        System.out.println("   -f|--messagefile </path/to/file/for/signed/message>");
        System.out.println("   -k|--key </path/to/public/key/file>");
    }
}
