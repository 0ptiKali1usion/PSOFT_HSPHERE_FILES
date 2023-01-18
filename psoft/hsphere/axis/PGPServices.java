package psoft.hsphere.axis;

import cryptix.jce.provider.CryptixCrypto;
import cryptix.message.EncryptedMessage;
import cryptix.message.KeyBundleMessage;
import cryptix.message.LiteralMessage;
import cryptix.message.MessageFactory;
import cryptix.message.NotEncryptedToParameterException;
import cryptix.message.SignedMessage;
import cryptix.openpgp.provider.CryptixOpenPGP;
import cryptix.pki.KeyBundle;
import java.io.ByteArrayInputStream;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import org.apache.log4j.Category;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.hsphere.PGPSecurity.PGPKeyPairStorage;

/* loaded from: hsphere.zip:psoft/hsphere/axis/PGPServices.class */
public class PGPServices {
    private static Category log = Category.getInstance(PGPServices.class.getName());

    public String getDescription() {
        return "Functions to work with PGP";
    }

    public PGPKeysInfo getKeyPair(AuthToken at, String identification, String subkeyIdentification, String encryptPhrase) throws Exception {
        Utils.getAccount(at);
        PGPKeyPairStorage pgpKPS = C0007PGPSecurity.produceKeyPair(identification, subkeyIdentification, encryptPhrase);
        String privateKey = pgpKPS.privateKeyBundle.getEncodedString();
        String publicKey = pgpKPS.publicKeyBundle.getEncodedString();
        return new PGPKeysInfo(privateKey, publicKey, encryptPhrase);
    }

    private MessageFactory getOpenPGPMessageFactory() throws Exception {
        Security.addProvider(new CryptixCrypto());
        Security.addProvider(new CryptixOpenPGP());
        return MessageFactory.getInstance("OpenPGP");
    }

    private KeyBundle getKeyBundle(ByteArrayInputStream input) throws Exception {
        MessageFactory messageFactory = getOpenPGPMessageFactory();
        Collection msgs = messageFactory.generateMessages(input);
        KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
        KeyBundle keyBundle = kbm.getKeyBundle();
        input.close();
        return keyBundle;
    }

    public String encrypt(AuthToken at, String message, String publicKey) throws Exception {
        return encrypt(at, message, publicKey.getBytes());
    }

    public String encrypt(AuthToken at, String message, byte[] publicKey) throws Exception {
        Utils.getAccount(at);
        ByteArrayInputStream input = new ByteArrayInputStream(publicKey);
        KeyBundle publicKeyBundle = getKeyBundle(input);
        return C0007PGPSecurity.encrypt(message, publicKeyBundle);
    }

    public String decrypt(AuthToken at, String message, String privateKey, String encryptPhrase) throws Exception {
        return decrypt(at, message, privateKey.getBytes(), encryptPhrase);
    }

    public String decrypt(AuthToken at, String message) throws Exception {
        Utils.getAccount(at);
        return C0007PGPSecurity.decrypt(message);
    }

    public String decrypt(AuthToken at, String message, byte[] privateKey, String encryptPhrase) throws Exception {
        Utils.getAccount(at);
        KeyBundle privateKeyBundle = getKeyBundle(new ByteArrayInputStream(privateKey));
        ByteArrayInputStream input = new ByteArrayInputStream(message.getBytes());
        MessageFactory messageFactory = getOpenPGPMessageFactory();
        Collection msgs = messageFactory.generateMessages(input);
        EncryptedMessage em = (EncryptedMessage) msgs.iterator().next();
        input.close();
        try {
            LiteralMessage decrypt = em.decrypt(privateKeyBundle, encryptPhrase.toCharArray());
            if (decrypt instanceof LiteralMessage) {
                return decrypt.getTextData();
            }
            if (decrypt instanceof SignedMessage) {
                return ((SignedMessage) decrypt).getContents().getTextData();
            }
            return new String("Unknown message structure\n\n" + message);
        } catch (UnrecoverableKeyException e) {
            return new String("Invalid passphrase.\n Impossible to decrypt information.\n\n" + message);
        } catch (Exception e2) {
            return new String("Impossible to decrypt information.\n\n" + message);
        } catch (NotEncryptedToParameterException e3) {
            return new String("The message is encrypted by invalid key.\n Impossible to decrypt information.\n\n" + message);
        }
    }

    public boolean verifyMessage(AuthToken at, String message, String publicKey) throws Exception {
        Utils.getAccount(at);
        return C0007PGPSecurity.verifyMessage(message, publicKey);
    }

    public String signMessage(AuthToken at, String message) throws Exception {
        Utils.getAccount(at);
        return C0007PGPSecurity.signMessage(message);
    }

    public String signMessage(AuthToken at, String message, String privateKey, String encryptPhrase) throws Exception {
        Utils.getAccount(at);
        return C0007PGPSecurity.signMessage(message, privateKey, encryptPhrase);
    }

    public String unsignMessage(AuthToken at, String message) throws Exception {
        Utils.getAccount(at);
        return C0007PGPSecurity.unsignMessage(message);
    }
}
