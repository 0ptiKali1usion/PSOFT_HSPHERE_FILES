package psoft.hsphere.admin;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import psoft.encryption.Crypt;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.util.Base64;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/admin/CCEncryption.class */
public class CCEncryption {
    public static final Object ENCRYPTION_LOCK = new Object();
    private static final int KEY_LINE_LENGTH = 60;
    private static CCEncryption singleton;
    private static String encryptionstat;
    public static final short ENCRYPTIONDISABLED = 1;
    public static final short ENCRYPTION = 2;
    public static final short ENCRYPTIONENABLED = 3;
    public static final short DECRYPTION = 4;
    private PrivateKey privateEncryptionKey;
    private PublicKey publicEncryptionKey;
    private boolean keyLoadEmailSent;
    private boolean keyLoadTicketSent;
    private String publicKeyLoadingErrorMessage;

    public static synchronized CCEncryption get() throws Exception {
        if (singleton == null) {
            singleton = new CCEncryption();
        }
        return singleton;
    }

    private CCEncryption() throws Exception {
        this.publicKeyLoadingErrorMessage = null;
        if (getEncryptionStatus() == 4 || getEncryptionStatus() == 2 || getEncryptionStatus() == 3) {
            try {
                this.publicEncryptionKey = Crypt.loadKey();
                if (getEncryptionStatus() == 2) {
                    startEncryption();
                }
            } catch (Throwable t) {
                this.publicKeyLoadingErrorMessage = "Error loading public key: " + t.getMessage();
                Session.getLog().error("Error loading public key: ", t);
            }
        }
    }

    public void setEncryptionOn() throws Exception {
        synchronized (ENCRYPTION_LOCK) {
            short stat = getEncryptionStatus();
            if (stat == 1) {
                long oldResellerId = 0;
                try {
                    oldResellerId = Session.getResellerId();
                } catch (UnknownResellerException e) {
                }
                try {
                    Session.setResellerId(1L);
                    Settings.get().setValue("cc_encryption", "ENCRYPTION");
                    Session.setResellerId(oldResellerId);
                    encryptionstat = "ENCRYPTION";
                    startEncryption();
                } catch (Exception ex) {
                    Session.getLog().error("Can't set cc_encryption value in the settings", ex);
                    throw new Exception("Can't set cc_encryption value in the settings. Error: " + ex.toString());
                }
            } else {
                String mess = "";
                if (stat == 2) {
                    mess = "encryption process is running now.";
                } else if (stat == 3) {
                    mess = "encryption has been allready enabled";
                } else if (stat == 4) {
                    mess = "decryption process is running now";
                }
                throw new HSUserException("Encryption can't be enabled because: " + mess);
            }
        }
    }

    public void setEncryptionOff() throws Exception {
        synchronized (ENCRYPTION_LOCK) {
            short stat = getEncryptionStatus();
            if (stat == 3) {
                long oldResellerId = 0;
                try {
                    oldResellerId = Session.getResellerId();
                } catch (UnknownResellerException e) {
                }
                try {
                    Session.setResellerId(1L);
                    Settings.get().setValue("cc_encryption", "DECRYPTION");
                    Session.setResellerId(oldResellerId);
                    encryptionstat = "DECRYPTION";
                    startDecryption();
                } catch (Exception ex) {
                    Session.getLog().error("Can't set cc_encryption value from settings", ex);
                    throw new Exception("Can't get cc_encryption value from the settings. Error: " + ex.toString());
                }
            } else {
                String mess = "";
                if (stat == 2) {
                    mess = "encryption process is running now.";
                } else if (stat == 1) {
                    mess = "decryption has been allready enabled";
                } else if (stat == 4) {
                    mess = "decryption process is running now";
                }
                throw new HSUserException("Encryption can't be enabled because: " + mess);
            }
        }
    }

    public void dropEncryption() throws Exception {
        Connection con = Session.getDb();
        long oldResellerId = 0;
        try {
            oldResellerId = Session.getResellerId();
        } catch (UnknownResellerException e) {
        }
        try {
            Session.setResellerId(1L);
            Settings.get().setValue("cc_encryption", "OFF");
            PreparedStatement ps = con.prepareStatement("UPDATE credit_card SET encrypted_cc_number=?");
            ps.setNull(1, 12);
            ps.executeUpdate();
            con.prepareStatement("UPDATE charge_log SET message_out_enc=?").setNull(1, 2005);
            Session.setResellerId(oldResellerId);
            encryptionstat = "OFF";
        } catch (Throwable th) {
            Session.setResellerId(oldResellerId);
            throw th;
        }
    }

    public boolean isOn() {
        short stat = getEncryptionStatus();
        return stat == 3 || stat == 2;
    }

    public short getEncryptionStatus() {
        short status;
        if (encryptionstat == null) {
            long oldResellerId = 0;
            try {
                try {
                    oldResellerId = Session.getResellerId();
                } catch (UnknownResellerException ex) {
                    Session.getLog().error("Can't set admin account", ex);
                }
            } catch (UnknownResellerException e) {
            }
            try {
                Session.setResellerId(1L);
                encryptionstat = Settings.get().getValue("cc_encryption");
                Session.setResellerId(oldResellerId);
            } catch (Exception ex2) {
                Session.getLog().error("Can't get cc_encryption value from settings", ex2);
                Session.setResellerId(oldResellerId);
            }
        }
        if (encryptionstat != null) {
            if ("OFF".equals(encryptionstat)) {
                status = 1;
            } else if ("ENCRYPTION".equals(encryptionstat)) {
                status = 2;
            } else if ("ON".equals(encryptionstat)) {
                status = 3;
            } else if ("DECRYPTION".equals(encryptionstat)) {
                status = 4;
            } else {
                status = 1;
            }
        } else {
            status = 1;
        }
        return status;
    }

    public boolean isKeyLoadEmailSent() {
        return this.keyLoadEmailSent;
    }

    public void justSentKeyLoadEmail() {
        this.keyLoadEmailSent = true;
    }

    public boolean isKeyLoadTicketSent() {
        return this.keyLoadTicketSent;
    }

    public void justSentKeyLoadTicket() {
        this.keyLoadTicketSent = true;
    }

    public PublicKey getPublicEncryptionKey() {
        return this.publicEncryptionKey;
    }

    public String getPublicKeyLoadingErrorMessage() {
        return this.publicKeyLoadingErrorMessage;
    }

    private void setPublicEncryptionKey(PublicKey key) throws Exception {
        this.publicEncryptionKey = key;
        Crypt.saveKey(key);
    }

    public PrivateKey getPrivateEncryptionKey() {
        return this.privateEncryptionKey;
    }

    private void setPrivateEncryptionKey(PrivateKey key) {
        this.privateEncryptionKey = key;
        this.keyLoadEmailSent = false;
        this.keyLoadTicketSent = false;
    }

    public void clearPrivateEncryptionKey() {
        this.privateEncryptionKey = null;
    }

    public boolean isPrivateKeyLoaded() {
        return this.privateEncryptionKey != null;
    }

    public boolean isPublicKeyLoaded() {
        return this.publicEncryptionKey != null;
    }

    public void generateKeyPair() throws Exception {
        KeyPair keyPair = Crypt.generateKeyPair();
        setPrivateEncryptionKey(keyPair.getPrivate());
        setPublicEncryptionKey(keyPair.getPublic());
    }

    public void setBase64PrivateKey(String privateKey) throws Exception {
        byte[] privKey = Base64.decode(privateKey);
        setPrivateEncryptionKey(Crypt.decodePrivateKey(privKey));
    }

    public void validatePrivateKey() throws Exception {
        if (!isPrivateKeyLoaded()) {
            throw new PrivateKeyNotLoadedException("private key is not loaded, i cant validate it");
        }
        byte[] encrypted = Crypt.encrypt(getPublicEncryptionKey(), "This is a test string.".getBytes());
        byte[] decrypted = Crypt.decrypt(getPrivateEncryptionKey(), encrypted);
        String compareString = new String(decrypted);
        if (!"This is a test string.".equals(compareString)) {
            throw new Exception("Private key validation failed");
        }
        if (getEncryptionStatus() == 4) {
            startDecryption();
        }
    }

    public String getBase64PrivateKey() throws Exception {
        byte[] privKey = Crypt.encodePrivateKey(getPrivateEncryptionKey());
        StringBuffer encoded = new StringBuffer(Base64.encode(privKey));
        for (int breakAt = KEY_LINE_LENGTH; breakAt <= encoded.length(); breakAt += 61) {
            encoded.insert(breakAt, '\n');
        }
        return encoded.toString();
    }

    public static String encrypt(String data) throws Exception {
        String ret;
        synchronized (ENCRYPTION_LOCK) {
            byte[] encrypted = Crypt.encrypt(get().getPublicEncryptionKey(), data.getBytes());
            ret = Base64.encode(encrypted);
        }
        return ret;
    }

    public static String decrypt(String data) throws Exception {
        String ret;
        synchronized (ENCRYPTION_LOCK) {
            byte[] encrypted = Base64.decode(data);
            byte[] decrypted = Crypt.decrypt(get().getPrivateEncryptionKey(), encrypted);
            ret = new String(decrypted);
        }
        return ret;
    }

    private void startDecryption() {
        EncryptionTh th = new EncryptionTh((short) 4);
        th.setPriority(5);
        th.start();
    }

    private void startEncryption() {
        EncryptionTh th = new EncryptionTh((short) 2);
        th.setPriority(5);
        th.start();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/admin/CCEncryption$EncryptionTh.class */
    public class EncryptionTh extends Thread {
        boolean cont;
        private short method;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        EncryptionTh(short method) {
            super("Encryption");
            CCEncryption.this = r4;
            this.method = method;
            this.cont = true;
            setDaemon(true);
        }

        public void die() {
            this.cont = false;
            interrupt();
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            if (this.method == 2) {
                try {
                    if (CCEncryption.this.isPublicKeyLoaded()) {
                        encryptAllCreditCards();
                        encryptChargeLog();
                        if (this.cont) {
                            long oldResellerId = 0;
                            try {
                                oldResellerId = Session.getResellerId();
                            } catch (UnknownResellerException e) {
                            }
                            Session.setResellerId(1L);
                            Settings.get().setValue("cc_encryption", "ON");
                            Session.setResellerId(oldResellerId);
                            String unused = CCEncryption.encryptionstat = "ON";
                        } else {
                            Session.getLog().error("Encryption process was interrupted");
                        }
                    } else {
                        Session.getLog().error("Public key is not loaded");
                    }
                    return;
                } catch (Exception ex) {
                    Session.getLog().error("Encryption problem: ", ex);
                    return;
                }
            }
            try {
                if (CCEncryption.this.isPrivateKeyLoaded()) {
                    decryptAllCreditCards();
                    decryptChargeLog();
                    if (this.cont) {
                        long oldResellerId2 = 0;
                        try {
                            oldResellerId2 = Session.getResellerId();
                        } catch (UnknownResellerException e2) {
                        }
                        Session.setResellerId(1L);
                        Settings.get().setValue("cc_encryption", "OFF");
                        Session.setResellerId(oldResellerId2);
                        String unused2 = CCEncryption.encryptionstat = "OFF";
                    } else {
                        Session.getLog().error("Decryption process was interrupted");
                    }
                } else {
                    Session.getLog().error("Private key is not loaded");
                }
            } catch (Exception ex2) {
                Session.getLog().error("Decryption problem: ", ex2);
            }
        }

        private void encryptChargeLog() throws Exception {
            PreparedStatement ps = null;
            HashMap messages = new HashMap();
            boolean inProcess = true;
            long overall = 0;
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                Connection con = Session.getTransConnection();
                try {
                    long startTime = TimeUtils.currentTimeMillis();
                    Session.getLog().info("ChargeLog encryption started");
                    while (inProcess && this.cont) {
                        ps = con.prepareStatement("SELECT id, message_out FROM charge_log WHERE message_out is not null  LIMIT 700");
                        ResultSet rs = ps.executeQuery();
                        int count = 0;
                        while (rs.next()) {
                            long transId = rs.getLong(1);
                            String encrypted = "";
                            String mess = Session.getClobValue(rs, 2);
                            if (!"".equals(mess)) {
                                encrypted = Crypt.lencrypt(CCEncryption.get().getPublicEncryptionKey(), mess);
                            }
                            messages.put(new Long(transId), encrypted);
                            count++;
                        }
                        Session.closeStatement(ps);
                        if (count == 0) {
                            inProcess = false;
                        } else {
                            ps = con.prepareStatement("UPDATE charge_log SET message_out_enc=?, message_out=? WHERE id=?");
                            ps.setNull(2, 2005);
                            for (Long id : messages.keySet()) {
                                Session.setClobValue(ps, 1, (String) messages.get(id));
                                ps.setLong(3, id.longValue());
                                ps.executeUpdate();
                                overall++;
                            }
                            con.commit();
                            Session.closeStatement(ps);
                            messages.clear();
                            Session.getLog().debug("Records processed: " + overall);
                            TimeUtils.sleep(500L);
                        }
                    }
                    long timeDiff = TimeUtils.currentTimeMillis() - startTime;
                    Session.getLog().info("ChargeLog encryption finished, process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                    Session.closeStatement(ps);
                    Session.commitTransConnection(con);
                    Session.getLog().info("Records were processed: " + Long.toString(overall));
                } catch (Exception ex) {
                    Session.getLog().error("Error during Charge Log encryption ", ex);
                    throw ex;
                }
            }
        }

        private void decryptChargeLog() throws Exception {
            PreparedStatement ps = null;
            HashMap messages = new HashMap();
            boolean inProcess = true;
            long overall = 0;
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                Connection con = Session.getTransConnection();
                try {
                    long startTime = TimeUtils.currentTimeMillis();
                    Session.getLog().info("Charge Log decryption started");
                    while (inProcess && this.cont) {
                        ps = con.prepareStatement("SELECT id, message_out_enc FROM charge_log WHERE message_out_enc is not null LIMIT 700");
                        ResultSet rs = ps.executeQuery();
                        int count = 0;
                        while (rs.next()) {
                            long transId = rs.getLong(1);
                            String decrypted = "";
                            String mess = Session.getClobValue(rs, 2);
                            if (!"".equals(mess)) {
                                decrypted = Crypt.ldecrypt(CCEncryption.get().getPrivateEncryptionKey(), mess);
                            }
                            messages.put(new Long(transId), decrypted);
                            count++;
                        }
                        Session.closeStatement(ps);
                        if (count == 0) {
                            inProcess = false;
                        } else {
                            ps = con.prepareStatement("UPDATE charge_log SET message_out=?, message_out_enc=? WHERE id=?");
                            ps.setNull(2, 2005);
                            for (Long id : messages.keySet()) {
                                Session.setClobValue(ps, 1, (String) messages.get(id));
                                ps.setLong(3, id.longValue());
                                ps.executeUpdate();
                                overall++;
                            }
                            con.commit();
                            Session.closeStatement(ps);
                            messages.clear();
                            Session.getLog().debug("Records processed: " + overall);
                            TimeUtils.sleep(500L);
                        }
                    }
                    long timeDiff = TimeUtils.currentTimeMillis() - startTime;
                    Session.getLog().info("ChargeLog decryption finished, process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                    Session.closeStatement(ps);
                    Session.commitTransConnection(con);
                    Session.getLog().info("Records were processed: " + Long.toString(overall));
                } catch (Exception ex) {
                    Session.getLog().error("Error during ChargeLog encryption ", ex);
                    throw ex;
                }
            }
        }

        private void encryptAllCreditCards() throws Exception {
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                long startTime = TimeUtils.currentTimeMillis();
                Session.getLog().info("CC encryption started");
                Connection con = Session.getTransConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("SELECT id, cc_number FROM credit_card WHERE cc_number IS NOT NULL");
                    ResultSet rs = ps.executeQuery();
                    HashMap encrypted = new HashMap();
                    while (rs.next() && this.cont) {
                        String number = rs.getString("cc_number");
                        String encodedNumber = "";
                        if (!"".equals(number)) {
                            byte[] encryptedNumber = Crypt.encrypt(CCEncryption.get().getPublicEncryptionKey(), number.getBytes());
                            encodedNumber = Base64.encode(encryptedNumber);
                        }
                        encrypted.put(new Integer(rs.getInt("id")), encodedNumber);
                    }
                    Session.closeStatement(ps);
                    PreparedStatement ps2 = con.prepareStatement("UPDATE credit_card SET cc_number = ?, encrypted_cc_number = ? WHERE id = ?");
                    ps2.setNull(1, 12);
                    Iterator keys = encrypted.keySet().iterator();
                    while (keys.hasNext() && this.cont) {
                        Integer id = (Integer) keys.next();
                        ps2.setString(2, (String) encrypted.get(id));
                        ps2.setInt(3, id.intValue());
                        ps2.executeUpdate();
                    }
                    if (!this.cont) {
                        con.rollback();
                        Session.closeStatement(ps2);
                        Session.commitTransConnection(con);
                        return;
                    }
                    long timeDiff = TimeUtils.currentTimeMillis() - startTime;
                    Session.getLog().info("CC encryption finished, process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                    Session.closeStatement(ps2);
                    Session.commitTransConnection(con);
                } catch (Exception e) {
                    con.rollback();
                    Session.getLog().error("Error during CC encryption ", e);
                    throw e;
                }
            }
        }

        private void decryptAllCreditCards() throws Exception {
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                long startTime = TimeUtils.currentTimeMillis();
                Session.getLog().info("CC decryption started");
                Connection con = Session.getTransConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("SELECT id, encrypted_cc_number FROM credit_card WHERE encrypted_cc_number IS NOT NULL");
                    ResultSet rs = ps.executeQuery();
                    HashMap decrypted = new HashMap();
                    while (rs.next() && this.cont) {
                        String encodedNumber = rs.getString("encrypted_cc_number");
                        String number = "";
                        if (!"".equals(encodedNumber)) {
                            byte[] encryptedCCNumber = Base64.decode(encodedNumber);
                            byte[] decryptedNumber = Crypt.decrypt(CCEncryption.get().getPrivateEncryptionKey(), encryptedCCNumber);
                            number = new String(decryptedNumber);
                        }
                        decrypted.put(new Integer(rs.getInt("id")), number);
                    }
                    Session.closeStatement(ps);
                    PreparedStatement ps2 = con.prepareStatement("UPDATE credit_card SET cc_number = ?, encrypted_cc_number = ? WHERE id = ?");
                    ps2.setNull(2, 12);
                    Iterator keys = decrypted.keySet().iterator();
                    while (keys.hasNext() && this.cont) {
                        Integer id = (Integer) keys.next();
                        ps2.setString(1, (String) decrypted.get(id));
                        ps2.setInt(3, id.intValue());
                        ps2.executeUpdate();
                    }
                    if (!this.cont) {
                        con.rollback();
                        Session.closeStatement(ps2);
                        Session.commitTransConnection(con);
                        return;
                    }
                    long timeDiff = TimeUtils.currentTimeMillis() - startTime;
                    Session.getLog().info("CC decryption finished, process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                    Session.closeStatement(ps2);
                    Session.commitTransConnection(con);
                } catch (Exception e) {
                    con.rollback();
                    Session.getLog().error("Error during CC decryption ", e);
                    throw e;
                }
            }
        }
    }
}
