package psoft.hsphere.resource.admin;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.encryption.Crypt;
import psoft.epayment.GenericMerchantGateway;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.CCEncryption;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.payment.WebPayment;
import psoft.hsphere.resource.epayment.MerchantSetting;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MerchantManager.class */
public class MerchantManager extends Resource {
    public MerchantManager(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public MerchantManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_setActive(int id, String type) throws Exception {
        Session.getMerchantGatewayManager().setActiveGateway(id, type);
        return this;
    }

    public TemplateModel FM_resetActive(String type) throws Exception {
        Session.getMerchantGatewayManager().resetActiveGateway(type);
        return this;
    }

    public TemplateModel FM_listActive() throws Exception {
        return new TemplateList(Session.getMerchantGatewayManager().listActive());
    }

    public TemplateModel FM_getCCbrands() throws Exception {
        TemplateList l = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id,name_sh,name FROM cc_brands WHERE id < 100 OR reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("id", rs.getString(1));
                map.put("name_sh", rs.getString(2).trim());
                map.put("name", rs.getString(3).trim());
                l.add((TemplateModel) map);
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

    public TemplateModel FM_addCCbrand(String name, String name_sh) throws Exception {
        Connection con = Session.getDb();
        try {
            int id = 0;
            PreparedStatement ps = con.prepareStatement("SELECT MAX(id) FROM cc_brands");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1) + 1;
            }
            int id2 = id < 100 ? 100 : id;
            if (isExistBrand(name_sh, 0)) {
                throw new HSUserException("merchantmanager.ccexist");
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO cc_brands(id, name, name_sh, reseller_id) VALUES (?, ?, ?, ?)");
            ps2.setInt(1, id2);
            ps2.setString(2, name);
            ps2.setString(3, name_sh);
            ps2.setLong(4, Session.getResellerId());
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_editCCbrand(int ccbrand_id, String name, String name_sh) throws Exception {
        Connection con = Session.getDb();
        try {
            if (isExistBrand(name_sh, ccbrand_id)) {
                throw new HSUserException("merchantmanager.ccexist");
            }
            PreparedStatement ps = con.prepareStatement("UPDATE cc_brands SET name = ?, name_sh =? WHERE id = ?");
            ps.setString(1, name);
            ps.setString(2, name_sh);
            ps.setInt(3, ccbrand_id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deleteCCbrand(int ccbrand_id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM cc_brands WHERE id = ?");
            ps.setInt(1, ccbrand_id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isExistBrand(String name_sh, int id) throws Exception {
        Connection con = Session.getDb();
        boolean ps = null;
        try {
            boolean ps2 = con.prepareStatement("SELECT id from cc_brands WHERE ((name_sh = ? AND id<100) OR (name_sh = ? AND reseller_id = ?)) AND id<>?");
            ps2.setString(1, name_sh);
            ps2.setString(2, name_sh);
            ps2.setLong(3, Session.getResellerId());
            ps2.setLong(4, id);
            ResultSet rs = ps2.executeQuery();
            return rs.next();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MerchantManager$MerchantCreator.class */
    class MerchantCreator implements TemplateMethodModel {
        MerchantCreator() {
            MerchantManager.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                List l2 = HTMLEncoder.decode(l);
                HashMap map = new HashMap();
                Iterator i = l2.iterator();
                while (i.hasNext()) {
                    map.put(i.next(), i.next());
                }
                int id = Session.getMerchantGatewayManager().createMerchantGateway(map);
                MerchantSetting.create(id, (String) map.get("TYPE"));
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating merchant", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MerchantManager$MerchantUpdator.class */
    class MerchantUpdator implements TemplateMethodModel {
        MerchantUpdator() {
            MerchantManager.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                Iterator i = HTMLEncoder.decode(l).iterator();
                HashMap map = new HashMap();
                int id = Integer.parseInt((String) i.next());
                while (i.hasNext()) {
                    map.put(i.next(), i.next());
                }
                Session.getMerchantGatewayManager().updateMerchantGateway(id, map);
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating merchant", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    public TemplateModel FM_settings(int id) throws Exception {
        return MerchantSetting.get(id);
    }

    public TemplateModel FM_get(int id) throws Exception {
        return new TemplateMap(Session.getMerchantGatewayManager().get(id));
    }

    public TemplateModel FM_list() throws Exception {
        return new TemplateList(Session.getMerchantGatewayManager().list());
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "new".equals(key) ? new MerchantCreator() : "update".equals(key) ? new MerchantUpdator() : "webprocessorparams".equals(key) ? new WebPaymentUpdator() : super.get(key);
    }

    public TemplateModel FM_getWebPaymentTransDetails(long transId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        TemplateMap map = null;
        try {
            ps = con.prepareStatement("SELECT account_id, request_id, created, gateway_name, request_type, requested_amount, result_amount, error_message, request_info, result FROM extern_pm_log WHERE id=?");
            ps.setLong(1, transId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                map = new TemplateMap();
                long accountId = rs.getLong(1);
                long requestId = rs.getLong(2);
                if (accountId > 0) {
                    map.put("account_id", new Long(accountId));
                } else {
                    map.put("request_id", new Long(requestId));
                }
                map.put("created", rs.getTimestamp(3));
                map.put("gateway_name", rs.getString(4));
                map.put("request_type", WebPayment.getRequestTypeDescription(rs.getInt(5)));
                map.put("requested_amount", new Double(rs.getDouble(6)));
                map.put("result_amount", new Double(rs.getDouble(7)));
                map.put("error_message", rs.getString(8));
                map.put("request_info", rs.getString(9));
                map.put("result", rs.getInt(10) == 0 ? "OK" : "ERROR");
            }
            Session.closeStatement(ps);
            con.close();
            return map;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getTransDetails(long transId) throws Exception {
        boolean result;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        TemplateModel templateModel = null;
        try {
            ps = con.prepareStatement("SELECT account_id, created, error_message, amount, trtype, result, message_out, message_in, message_out_enc FROM charge_log WHERE id=?");
            ps.setLong(1, transId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                templateModel = new TemplateMap();
                templateModel.put("account_id", Session.getClobValue(rs, 1));
                templateModel.put("created", rs.getTimestamp(2));
                String errMess = Session.getClobValue(rs, 3);
                templateModel.put("error_message", errMess);
                int trtype = rs.getInt(5);
                if (rs.wasNull()) {
                    trtype = 100;
                }
                String amount = "";
                if (trtype == 100) {
                    result = errMess.length() == 0;
                } else {
                    amount = Session.getClobValue(rs, 4);
                    result = rs.getBoolean(6);
                }
                templateModel.put("amount", amount);
                templateModel.put("trtype", GenericMerchantGateway.getTrDescription(trtype));
                String strResult = result ? "Successful" : "Failed";
                templateModel.put("result", strResult);
                String messOut = Session.getClobValue(rs, 7);
                if (messOut == null || "".equals(messOut)) {
                    String messOutEnc = Session.getClobValue(rs, 9);
                    synchronized (CCEncryption.ENCRYPTION_LOCK) {
                        if (messOutEnc != null && !"".equals(messOutEnc) && CCEncryption.get().isPrivateKeyLoaded()) {
                            messOut = Crypt.ldecrypt(CCEncryption.get().getPrivateEncryptionKey(), messOutEnc);
                        }
                    }
                }
                templateModel.put("message_out", messOut);
                templateModel.put("message_in", Session.getClobValue(rs, 8));
            }
            Session.closeStatement(ps);
            con.close();
            return templateModel;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MerchantManager$WebPaymentUpdator.class */
    class WebPaymentUpdator implements TemplateMethodModel {
        WebPaymentUpdator() {
            MerchantManager.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                Iterator i = HTMLEncoder.decode(l).iterator();
                String processorName = (String) i.next();
                Session.getLog().debug("Update parameters for webProcessor: " + processorName);
                while (i.hasNext()) {
                    Settings.get().setValue(processorName + "_" + i.next(), (String) i.next());
                }
                return new TemplateOKResult();
            } catch (Exception e) {
                Session.getLog().error("Error creating merchant", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    public TemplateModel FM_getAllowedIPsList(String gatewayName) throws Exception {
        Session.getLog().debug("FM_getAllowedIPsList gatewayName: " + gatewayName);
        String ips = Settings.get().getValue(gatewayName + "_IPs");
        Session.getLog().debug("FM_getAllowedIPsList ips: " + ips);
        TemplateList list = new TemplateList();
        if (ips != null && !"".equals(ips)) {
            StringTokenizer st = new StringTokenizer(ips, ";");
            while (st.hasMoreTokens()) {
                String curIP = st.nextToken();
                Session.getLog().debug("FM_getAllowedIPsList IP: " + curIP);
                list.add((TemplateModel) new TemplateString(curIP));
            }
        }
        return list;
    }

    public TemplateModel FM_addNewIP(String gatewayName, String ip) throws Exception {
        Session.getLog().debug("FM_addNewIP gatewayName: " + gatewayName + " IP: " + ip);
        String ips = Settings.get().getValue(gatewayName + "_IPs");
        if (ips == null || "".equals(ips)) {
            ips = ip + ";";
        } else if (ips.indexOf(ip) < 0) {
            ips = ips + ip + ";";
        }
        Settings.get().setLargeValue(gatewayName + "_IPs", ips);
        return this;
    }

    public TemplateModel FM_removeIP(String gatewayName, String ip) throws Exception {
        String ips = Settings.get().getValue(gatewayName + "_IPs");
        String newIPsList = "";
        if (ip != null && !"".equals(ip)) {
            StringTokenizer st = new StringTokenizer(ips, ";");
            while (st.hasMoreTokens()) {
                String curIP = st.nextToken();
                if (!ip.equals(curIP)) {
                    newIPsList = newIPsList + curIP + ";";
                }
            }
        }
        Settings.get().setLargeValue(gatewayName + "_IPs", newIPsList);
        return this;
    }

    public static HashMap getProcessorSettings(String gatewayName) throws Exception {
        HashMap values = new HashMap();
        try {
            Session.getLog().debug("Get settings  for reseller: " + Session.getResellerId());
            Document doc = XMLManager.getXML("MERCHANT_GATEWAYS_CONF");
            Element processors = (Element) doc.getElementsByTagName("processors").item(0);
            NodeList proclist = processors.getElementsByTagName("processor");
            for (int i = 0; i < proclist.getLength(); i++) {
                Element processor = (Element) proclist.item(i);
                Session.getLog().debug("Initializing a web processor: " + processor.getAttribute("name"));
                if (gatewayName.equals(processor.getAttribute("name"))) {
                    String servletname = processor.getAttribute("servlet");
                    values.put("servlet", servletname);
                    NodeList pvalues = processor.getElementsByTagName("value");
                    for (int v = 0; v < pvalues.getLength(); v++) {
                        Element val = (Element) pvalues.item(v);
                        String key = val.getAttribute("name");
                        String vvv = Settings.get().getValue(gatewayName + "_" + key);
                        values.put(key, vvv);
                        Session.getLog().debug("---" + key + " " + vvv);
                    }
                    return values;
                }
            }
            return values;
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Problem getting reseller ID:", ex);
            throw new UnknownResellerException("Problem getting reseller ID");
        } catch (Exception ex2) {
            Session.getLog().error("Problem getting payment processors info from xml: ", ex2);
            throw new Exception("Problem getting processor values from xml");
        }
    }
}
