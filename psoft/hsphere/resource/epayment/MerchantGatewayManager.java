package psoft.hsphere.resource.epayment;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import psoft.epayment.MerchantGateway;
import psoft.epayment.MerchantGatewayLog;
import psoft.epayment.UnknownPaymentInstrumentException;
import psoft.hsphere.Session;
import psoft.hsphere.Uploader;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/MerchantGatewayManager.class */
public class MerchantGatewayManager {
    protected Map gatewaysMap;
    protected MerchantGatewayLog log;
    public static final String MG_UPLOAD_FILE_CERT = "mg_upload_file_cert";
    public static final String MG_PRIVATE_KEY_PWD = "mg_private_key_pwd";
    public static final String MG_BASE_CERT_PATH = "/hsphere/local/home/cpanel/shiva/psoft_config/mg_certs";
    public static final String MG_CERT_PREFIX = "cert";
    public static final String MG_KEY_PREFIX = "key";

    public MerchantGatewayManager() {
        this(new MerchantTrackingLog());
    }

    public MerchantGatewayManager(MerchantGatewayLog log) {
        this.gatewaysMap = new HashMap();
        this.log = log;
    }

    public Map getGateway() throws Exception {
        return getGateway(Session.getResellerId());
    }

    protected Map getGateway(long resellerId) throws Exception {
        String reseller = Long.toString(resellerId);
        HashMap map = (Map) this.gatewaysMap.get(reseller);
        if (map == null) {
            map = new HashMap();
            this.gatewaysMap.put(reseller, map);
        }
        return map;
    }

    public synchronized MerchantGateway getMerchantGateway(int id) throws Exception {
        for (MerchantGateway mg : getGateway().values()) {
            if (mg.getId() == id) {
                return mg;
            }
        }
        return null;
    }

    public synchronized MerchantGateway getMerchantGateway(String type) throws UnknownPaymentInstrumentException, Exception {
        MerchantGateway gateway = (MerchantGateway) getGateway().get(type);
        if (gateway == null) {
            int id = select(type);
            return initGateway(id, type, load(id));
        }
        return gateway;
    }

    public List listActive() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, type FROM active_merch_gateway WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List l = new ArrayList();
            while (rs.next()) {
                HashMap hashMap = new HashMap();
                hashMap.put("id", rs.getString(1));
                hashMap.put("type", rs.getString(2));
                l.add(hashMap);
            }
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setActiveGateway(int id, String type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM active_merch_gateway WHERE type = ? AND reseller_id = ?");
            ps2.setString(1, type);
            ps2.setLong(2, Session.getResellerId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO active_merch_gateway (id, type, reseller_id) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            ps.setString(2, type);
            ps.setLong(3, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            initGateway(id, type, load(id));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void resetActiveGateway(String type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM active_merch_gateway WHERE type = ? AND reseller_id = ?");
            ps.setString(1, type);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            stopGateway(type);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void save(int id, Map map) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO merch_gateway (id, mkey, value, reseller_id) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (String key : map.keySet()) {
                ps.setString(2, key);
                ps.setString(3, (String) map.get(key));
                ps.setLong(4, Session.getResellerId());
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String createFile(int mid, String prefix, String file) throws Exception {
        try {
            String fName = "/hsphere/local/home/cpanel/shiva/psoft_config/mg_certs/" + prefix + mid;
            File f = new File(fName);
            if (f.isFile()) {
                f.delete();
            } else {
                IOUtils.mkdir(MG_BASE_CERT_PATH);
            }
            byte[] fileData = Uploader.getData(file);
            boolean isCreate = f.createNewFile();
            if (isCreate) {
                FileOutputStream fileoutputstream = new FileOutputStream(f);
                fileoutputstream.write(fileData, 0, fileData.length);
                fileoutputstream.close();
                String rez = prefix + mid;
                return rez;
            }
            throw new Exception(fName + " - was not created");
        } catch (Exception e) {
            throw new Exception("merchantgateway.upload_failure : " + e.getMessage());
        }
    }

    public int createMerchantGateway(Map map) throws Exception {
        int id = Session.getNewId("merch_gateway_id");
        String fileName = (String) map.get(MG_UPLOAD_FILE_CERT);
        if (!"".equals(fileName) && null != fileName && fileName.indexOf("|") > 0) {
            map.put(MG_UPLOAD_FILE_CERT, createFile(id, MG_CERT_PREFIX, fileName));
        }
        String fileName2 = (String) map.get(MG_PRIVATE_KEY_PWD);
        if (!"".equals(fileName2) && null != fileName2 && fileName2.indexOf("|") > 0) {
            map.put(MG_PRIVATE_KEY_PWD, createFile(id, MG_KEY_PREFIX, fileName2));
        }
        save(id, map);
        return id;
    }

    public void updateMerchantGateway(int id, String key, String value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            MerchantGateway mg = getMerchantGateway(id);
            synchronized (mg) {
                if (mg.getValues().get(key) != null) {
                    ps = con.prepareStatement("UPDATE merch_gateway set value = ? WHERE mkey = ? AND id = ? AND reseller_id = ?");
                    ps.setString(1, value);
                    ps.setString(2, key);
                    ps.setInt(3, id);
                    ps.setLong(4, Session.getResellerId());
                } else {
                    ps = con.prepareStatement("INSERT INTO merch_gateway (id, mkey, value, reseller_id) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, id);
                    ps.setString(2, key);
                    ps.setString(3, value);
                    ps.setLong(4, Session.getResellerId());
                }
                mg.getValues().put(key, value);
                ps.executeUpdate();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void updateMerchantGateway(int id, Map map) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            String fileName = (String) map.get(MG_UPLOAD_FILE_CERT);
            if (!"".equals(fileName) && null != fileName && fileName.indexOf("|") > 0) {
                map.put(MG_UPLOAD_FILE_CERT, createFile(id, MG_CERT_PREFIX, fileName));
            }
            String fileName2 = (String) map.get(MG_PRIVATE_KEY_PWD);
            if (!"".equals(fileName2) && null != fileName2 && fileName2.indexOf("|") > 0) {
                map.put(MG_PRIVATE_KEY_PWD, createFile(id, MG_KEY_PREFIX, fileName2));
            }
            ps = con.prepareStatement("DELETE FROM merch_gateway WHERE id = ? AND reseller_id = ?");
            ps.setInt(1, id);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            save(id, map);
            for (HashMap gw : listActive()) {
                initGateway(Integer.parseInt((String) gw.get("id")), (String) gw.get("type"), load(Integer.parseInt((String) gw.get("id"))));
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected MerchantGateway initGateway(int id, String type, HashMap hash) throws Exception {
        Class c = Class.forName((String) hash.get("CLASS_NAME"));
        MerchantGateway gateway = (MerchantGateway) c.newInstance();
        gateway.init(id, hash);
        gateway.setLog(this.log);
        getGateway().put(type, gateway);
        return gateway;
    }

    protected void stopGateway(String type) throws Exception {
        getGateway().remove(type);
    }

    protected HashMap initMap(ResultSet rs) throws Exception {
        if (rs.next()) {
            HashMap hash = new HashMap();
            hash.put(rs.getString(1), rs.getString(2));
            while (rs.next()) {
                hash.put(rs.getString(1), rs.getString(2));
            }
            return hash;
        }
        throw new UnknownPaymentInstrumentException();
    }

    public List list() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, value FROM merch_gateway WHERE mkey = 'TITLE' AND reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List l = new ArrayList();
            while (rs.next()) {
                HashMap hashMap = new HashMap();
                hashMap.put("id", rs.getString(1));
                hashMap.put("title", rs.getString(2));
                l.add(hashMap);
            }
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public HashMap get(int id) throws UnknownPaymentInstrumentException, Exception {
        return load(id);
    }

    protected HashMap load(int id) throws UnknownPaymentInstrumentException, Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT mkey, value FROM merch_gateway WHERE id = ? AND reseller_id = ?");
                ps.setInt(1, id);
                ps.setLong(2, Session.getResellerId());
                HashMap initMap = initMap(ps.executeQuery());
                Session.closeStatement(ps);
                con.close();
                return initMap;
            } catch (UnknownPaymentInstrumentException e) {
                throw new Exception("No merchant gateway #" + id);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected int select(String type) throws UnknownPaymentInstrumentException, Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM active_merch_gateway WHERE type = ? AND reseller_id = ?");
            ps.setString(1, type);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new UnknownPaymentInstrumentException("Merchant gateway settings for " + type + " hasn't been set up. Please refer to tech support");
            }
            int i = rs.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return i;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
