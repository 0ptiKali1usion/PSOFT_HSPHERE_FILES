package psoft.hsphere.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import psoft.hsphere.C0004CP;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PGPMessageSigner.class */
public class PGPMessageSigner extends C0004CP {
    public PGPMessageSigner() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        String message = null;
        String messageFile = null;
        String signedMessageFile = null;
        String privateKeyFile = null;
        String privateCodePhrase = null;
        for (int i = 0; i < argv.length; i++) {
            if ("-m".equals(argv[i]) || "--message".equals(argv[i])) {
                message = new String(argv[i + 1]);
            }
            if ("-mf".equals(argv[i]) || "--messagefile".equals(argv[i])) {
                messageFile = new String(argv[i + 1]);
            }
            if ("-k".equals(argv[i]) || "--key".equals(argv[i])) {
                privateKeyFile = new String(argv[i + 1]);
            }
            if ("-p".equals(argv[i]) || "--codephrase".equals(argv[i])) {
                privateCodePhrase = new String(argv[i + 1]);
            }
            if ("-f".equals(argv[i]) || "--file".equals(argv[i])) {
                signedMessageFile = new String(argv[i + 1]);
            }
        }
        if ((message == null && messageFile == null) || signedMessageFile == null || privateCodePhrase == null || privateKeyFile == null) {
            System.out.println("Missconfiguration ");
            printHelp();
            return;
        }
        try {
            try {
                String signedMessage = null;
                File f = new File(privateKeyFile);
                byte[] buffer = new byte[(int) f.length()];
                FileInputStream in = new FileInputStream(f);
                in.read(buffer);
                String privateKey = new String(buffer);
                if (message != null && messageFile == null) {
                    signedMessage = C0007PGPSecurity.signMessage(message, privateKey, privateCodePhrase);
                } else if (message != null || messageFile == null) {
                    printHelp();
                } else {
                    File f2 = new File(messageFile);
                    byte[] buffer2 = new byte[(int) f2.length()];
                    FileInputStream in2 = new FileInputStream(f2);
                    in2.read(buffer2);
                    String message2 = new String(buffer2);
                    signedMessage = C0007PGPSecurity.signMessage(message2, privateKey, privateCodePhrase);
                }
                if (signedMessage != null) {
                    FileOutputStream out = new FileOutputStream(signedMessageFile);
                    out.write(signedMessage.getBytes());
                    out.close();
                } else {
                    System.out.println("Message signification faild");
                }
                System.out.println("Finished at " + TimeUtils.getDate());
            } catch (IOException ioex) {
                ioex.printStackTrace();
                System.out.println("File access failed ");
                System.exit(-1);
                System.out.println("Finished at " + TimeUtils.getDate());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Signification failed ");
                System.exit(-1);
                System.out.println("Finished at " + TimeUtils.getDate());
            }
            System.out.println("Message signification finished");
            System.exit(0);
        } catch (Throwable th) {
            System.out.println("Finished at " + TimeUtils.getDate());
            throw th;
        }
    }

    public static void printHelp() {
        System.out.println("H-Sphere PGP message signer.");
        System.out.println("Usage java psoft.hsphere.tools.PGPMessageSigner");
        System.out.print("   -m|--message <Message to sign>  or");
        System.out.println("   -mf|--messagefile </path/to/file/with/message/to/sign>");
        System.out.println("   -f|--file </path/to/file/for/signed/message>");
        System.out.println("   -k|--key </path/to/private/key/file>");
        System.out.println("   -p|--codephrase <private code phrase>");
    }
}
