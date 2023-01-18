package psoft.epayment;

import com.ezic.direct.V2Client;
import com.sun.net.ssl.internal.ssl.Provider;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/NetBilling.class */
public class NetBilling extends GenericMerchantGateway {
    protected String mode;
    protected String accountId;
    protected String siteTag;
    protected String server;
    protected static V2Client client;
    protected String email;
    private static final SimpleDateFormat dateExp = new SimpleDateFormat("MMyy");

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.accountId = getValue(v, "ACCOUNT");
        this.siteTag = getValue(v, "SITETAG");
        this.mode = getValue(v, "MODE");
        this.server = getValue(v, "SERVER");
        this.email = getValue(v, "EMAILADDR");
        client = new V2Client();
        client.setDebug("DEBUG".equals(this.mode));
        client.setServer(this.server);
        client.setProtocol_HTTPS();
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "NetBiling";
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("ACCOUNT", this.accountId);
        map.put("SITETAG", this.siteTag);
        map.put("MODE", this.mode);
        map.put("SERVER", this.server);
        map.put("EMAILADDR", this.email);
        return map;
    }

    private void setRequest(V2Client.Request in, CreditCard cc, double amount, int ttType) throws Exception {
        setRequest(in, cc, amount, ttType, new HashMap());
    }

    private void setRequest(V2Client.Request in, CreditCard cc, double amount, int ttType, HashMap data) throws Exception {
        in.setAccountId(this.accountId);
        in.setSiteTag(this.siteTag);
        in.setAmount(amount);
        in.setPayment_CARD();
        switch (ttType) {
            case 0:
                in.setTrans_SALE();
                break;
            case 1:
                in.setTrans_AUTH();
                break;
            case 3:
                in.setTrans_REFUND();
                break;
            case 4:
                in.setTrans_CAPTURE();
                break;
        }
        if (ttType == 0 || ttType == 1) {
            in.setCardNumber(cc.getNumber());
            String cvv = cc.getCVV();
            if (!"".equals(cvv)) {
                in.setCardVerVal2(cvv);
            } else {
                in.setCardVerVal2_NONE();
            }
            in.setCardExpire(cc.getExp(dateExp));
            in.set("CUST_ADDR_STREET", cc.getAddress());
            in.set("CUST_ADDR_ZIP", cc.getZip());
            in.set("CUST_ADDR_COUNTRY", cc.getCountry());
        } else {
            in.setMasterId((String) data.get("id"));
        }
        String id = client.obtainNewId();
        in.setTransId(id);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        boolean success;
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap();
        V2Client.Request in = new V2Client.Request();
        V2Client.Response out = new V2Client.Response();
        String error = "";
        try {
            setRequest(in, cc, amount, 0);
            success = client.doTrans(in, out);
            if (success) {
                retval.put("id", out.getTransId());
                retval.put("amount", new Double(amount));
            } else {
                success = false;
                error = out.getAuthMsg();
                if ("".equals(error) || error == null) {
                    error = "Unknown error";
                }
            }
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(trId, id, amount, 0, in.toString(), out.toString(), error, success);
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        boolean success;
        long trId = writeLog(id, amount, 1);
        HashMap retval = new HashMap();
        V2Client.Request in = new V2Client.Request();
        V2Client.Response out = new V2Client.Response();
        String error = "";
        try {
            setRequest(in, cc, amount, 1);
            success = client.doTrans(in, out);
            if (success) {
                retval.put("id", out.getTransId());
                retval.put("amount", new Double(amount));
            } else {
                success = false;
                error = out.getAuthMsg();
                if ("".equals(error) || error == null) {
                    error = "Unknown error";
                }
            }
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(trId, id, amount, 1, in.toString(), out.toString(), error, success);
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        boolean success;
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 4);
        HashMap retval = new HashMap();
        V2Client.Request in = new V2Client.Request();
        V2Client.Response out = new V2Client.Response();
        String error = "";
        try {
            setRequest(in, cc, amount, 4, data);
            success = client.doTrans(in, out);
            if (success) {
                retval.put("id", out.getTransId());
                retval.put("amount", new Double(amount));
            } else {
                success = false;
                error = out.getAuthMsg();
                if ("".equals(error) || error == null) {
                    error = "Unknown error";
                }
            }
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(trId, id, amount, 4, in.toString(), out.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = new HashMap();
        sendEmail(this.email, 3, description, id, "Transaction ID is:" + data.get("id"));
        writeLog(0L, id, amount, 3, "", "", "", true);
        writeVoid(amount);
        return retval;
    }

    public HashMap checkCC(CreditCard cc) throws Exception {
        boolean success = false;
        String error = "";
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            try {
                HashMap data = authorize(-1L, "Checking verification number", 1.0d, cc);
                success = true;
                voidAuthorize(-1L, "Checking verification number", data, cc);
            } catch (Exception ex) {
                error = ex.getMessage();
                Session.getLog().error("Error checking cvv value: ", ex);
            }
        } else {
            cc.setCVVChecked(false);
        }
        if (!success) {
            throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. " + error);
        }
        return new HashMap();
    }
}
