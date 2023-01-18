package psoft.hsphere.PGPSecurity;

import cryptix.jce.provider.CryptixCrypto;
import cryptix.message.EncryptedMessage;
import cryptix.message.EncryptedMessageBuilder;
import cryptix.message.KeyBundleMessage;
import cryptix.message.LiteralMessage;
import cryptix.message.LiteralMessageBuilder;
import cryptix.message.Message;
import cryptix.message.MessageFactory;
import cryptix.message.NotEncryptedToParameterException;
import cryptix.message.SignedMessage;
import cryptix.message.SignedMessageBuilder;
import cryptix.openpgp.PGPArmouredMessage;
import cryptix.openpgp.PGPKeyBundle;
import cryptix.openpgp.provider.CryptixOpenPGP;
import cryptix.pki.CertificateBuilder;
import cryptix.pki.KeyBundle;
import cryptix.pki.KeyBundleFactory;
import cryptix.pki.PrincipalBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Collection;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;

/* renamed from: psoft.hsphere.PGPSecurity.PGPSecurity */
/* loaded from: hsphere.zip:psoft/hsphere/PGPSecurity/PGPSecurity.class */
public class C0007PGPSecurity {
    public static PGPKeyPairStorage produceKeyPair(String identification, String subKeyIdentification, String encryptPhrase) throws Exception {
        Security.addProvider(new CryptixCrypto());
        Security.addProvider(new CryptixOpenPGP());
        PGPKeyPairStorage result = new PGPKeyPairStorage();
        SecureRandom sr = new SecureRandom();
        KeyBundleFactory kbf = KeyBundleFactory.getInstance("OpenPGP");
        PGPKeyBundle resultPublicKey = kbf.generateEmptyKeyBundle();
        PGPKeyBundle resultPrivateKey = kbf.generateEmptyKeyBundle();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/DSA");
        kpg.initialize(1024, sr);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey pubKey = kp.getPublic();
        PrivateKey privKey = kp.getPrivate();
        PrincipalBuilder principalBuilder = PrincipalBuilder.getInstance("OpenPGP/UserID");
        Principal userID = principalBuilder.build(identification);
        CertificateBuilder certBuilder = CertificateBuilder.getInstance("OpenPGP/Self");
        Certificate cert = certBuilder.build(pubKey, userID, privKey, sr);
        resultPublicKey.addCertificate(cert);
        resultPrivateKey.addCertificate(cert);
        resultPrivateKey.addPrivateKey(privKey, pubKey, encryptPhrase.toCharArray(), sr);
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance("OpenPGP/Encryption/ElGamal");
        kpg2.initialize(1024, sr);
        KeyPair kp2 = kpg2.generateKeyPair();
        PublicKey pubsubkey = kp2.getPublic();
        PrivateKey privsubkey = kp2.getPrivate();
        resultPublicKey.addPublicSubKey(pubsubkey, privKey);
        resultPrivateKey.addPublicSubKey(pubsubkey, resultPublicKey);
        resultPrivateKey.addPrivateSubKey(privsubkey, pubsubkey, encryptPhrase.toCharArray(), sr);
        PrincipalBuilder principalBuilder2 = PrincipalBuilder.getInstance("OpenPGP/UserID");
        Principal userID2 = principalBuilder2.build(subKeyIdentification);
        CertificateBuilder certBuilder2 = CertificateBuilder.getInstance("OpenPGP/Self");
        Certificate cert2 = certBuilder2.build(pubKey, userID2, privKey, sr);
        resultPublicKey.addCertificate(cert2);
        resultPrivateKey.addCertificate(cert2);
        PGPArmouredMessage armoured = new PGPArmouredMessage(resultPublicKey);
        result.publicKeyBundle = armoured;
        PGPArmouredMessage armoured2 = new PGPArmouredMessage(resultPrivateKey);
        result.privateKeyBundle = armoured2;
        return result;
    }

    public static String decrypt(String message) throws Exception {
        Security.addProvider(new CryptixCrypto());
        Security.addProvider(new CryptixOpenPGP());
        MessageFactory mf = MessageFactory.getInstance("OpenPGP");
        Settings settings = Settings.get();
        String privateKey = settings.getValue("TT_PGPPRIVATEKEY");
        InputStream in = new ByteArrayInputStream(privateKey.getBytes());
        Collection msgs = mf.generateMessages(in);
        KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
        KeyBundle privateKeyBundle = kbm.getKeyBundle();
        in.close();
        String privateCodePhrase = settings.getValue("TT_PGPPRIVATEPHRASE");
        InputStream in2 = new ByteArrayInputStream(message.getBytes());
        Collection msgs2 = mf.generateMessages(in2);
        EncryptedMessage em = (EncryptedMessage) msgs2.iterator().next();
        in2.close();
        try {
            LiteralMessage decrypt = em.decrypt(privateKeyBundle, privateCodePhrase.toCharArray());
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

    public static String signMessage(String inputMessage) throws Exception {
        try {
            Security.addProvider(new CryptixCrypto());
            Security.addProvider(new CryptixOpenPGP());
            MessageFactory mf = MessageFactory.getInstance("OpenPGP");
            Settings settings = Settings.get();
            String privateKey = settings.getValue("TT_PGPPRIVATEKEY");
            InputStream in = new ByteArrayInputStream(privateKey.getBytes());
            Collection msgs = mf.generateMessages(in);
            KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
            KeyBundle privateKeyBundle = kbm.getKeyBundle();
            in.close();
            String privateCodePhrase = settings.getValue("TT_PGPPRIVATEPHRASE");
            LiteralMessageBuilder lbm = LiteralMessageBuilder.getInstance("OpenPGP");
            lbm.init(inputMessage);
            Message msg = lbm.build();
            SignedMessageBuilder smb = SignedMessageBuilder.getInstance("OpenPGP");
            smb.init(msg);
            smb.addSigner(privateKeyBundle, privateCodePhrase.toCharArray());
            Message msg2 = smb.build();
            return new String(new PGPArmouredMessage(msg2).getEncoded());
        } catch (Exception e) {
            Session.getLog().error("Message was not signed (possible private key is invalid or does not exist)");
            return null;
        }
    }

    public static String signMessage(String inputMessage, String privateKey, String privateCodePhrase) throws Exception {
        try {
            Security.addProvider(new CryptixCrypto());
            Security.addProvider(new CryptixOpenPGP());
            MessageFactory mf = MessageFactory.getInstance("OpenPGP");
            InputStream in = new ByteArrayInputStream(privateKey.getBytes());
            Collection msgs = mf.generateMessages(in);
            KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
            KeyBundle privateKeyBundle = kbm.getKeyBundle();
            in.close();
            LiteralMessageBuilder lbm = LiteralMessageBuilder.getInstance("OpenPGP");
            lbm.init(inputMessage);
            Message msg = lbm.build();
            SignedMessageBuilder smb = SignedMessageBuilder.getInstance("OpenPGP");
            smb.init(msg);
            smb.addSigner(privateKeyBundle, privateCodePhrase.toCharArray());
            Message msg2 = smb.build();
            return new String(new PGPArmouredMessage(msg2).getEncoded());
        } catch (Exception e) {
            System.out.println("Message was not signed (possible private key is invalid or does not exist)");
            return null;
        }
    }

    public static String unsignMessage(String inputMessage) throws Exception {
        Security.addProvider(new CryptixCrypto());
        Security.addProvider(new CryptixOpenPGP());
        MessageFactory mf = MessageFactory.getInstance("OpenPGP");
        InputStream in = new ByteArrayInputStream(inputMessage.getBytes());
        SignedMessage sm = (Message) mf.generateMessages(in).iterator().next();
        return sm.getContents().getTextData();
    }

    public static String encrypt(String message, KeyBundle publicKeyBundle) throws Exception {
        LiteralMessageBuilder lbm = LiteralMessageBuilder.getInstance("OpenPGP");
        lbm.init(message);
        LiteralMessage litmsg = lbm.build();
        EncryptedMessageBuilder ebm = EncryptedMessageBuilder.getInstance("OpenPGP");
        ebm.init(litmsg);
        ebm.addRecipient(publicKeyBundle);
        Message msg = ebm.build();
        PGPArmouredMessage armoured = new PGPArmouredMessage(msg);
        return new String(armoured.getEncodedString());
    }

    public static boolean verifyMessage(String inputMessage, String publicKey) throws Exception {
        Security.addProvider(new CryptixCrypto());
        Security.addProvider(new CryptixOpenPGP());
        MessageFactory mf = MessageFactory.getInstance("OpenPGP");
        InputStream in = new ByteArrayInputStream(publicKey.getBytes());
        Collection msgs = mf.generateMessages(in);
        KeyBundleMessage kbm = (KeyBundleMessage) msgs.iterator().next();
        KeyBundle publicKeyBundle = kbm.getKeyBundle();
        in.close();
        SignedMessage signedMessage = (Message) mf.generateMessages(new ByteArrayInputStream(inputMessage.getBytes())).iterator().next();
        if (!(signedMessage instanceof SignedMessage)) {
            return false;
        }
        SignedMessage sm = signedMessage;
        if (sm.verify(publicKeyBundle)) {
            return true;
        }
        return false;
    }
}
