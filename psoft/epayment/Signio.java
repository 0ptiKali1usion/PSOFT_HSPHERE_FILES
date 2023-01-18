package psoft.epayment;

import com.Signio.PFProAPI;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/epayment/Signio.class */
public class Signio extends GenericMerchantGateway {
    protected String server;
    protected int port;
    protected String partner;
    protected String login;
    protected String vendor;
    protected String password;
    protected String avs;
    private static final SimpleDateFormat expDate = new SimpleDateFormat("MMyy");

    static {
        defaultValues.put("SERVER", "connect.signio.com");
        defaultValues.put("PORT", "443");
        defaultValues.put("AVS", "N");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.partner = getValue(v, "PARTNER");
        this.login = getValue(v, "LOGIN");
        this.vendor = getValue(v, "VENDOR");
        this.password = getValue(v, "PASSWORD");
        this.avs = getValue(v, "AVS");
        if (this.avs == null) {
            this.avs = "N";
        }
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("PARTNER", this.partner);
        map.put("LOGIN", this.login);
        map.put("VENDOR", this.vendor);
        map.put("PASSWORD", this.password);
        map.put("AVS", this.password);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = doTransaction(0, id, amount, description, cc, null);
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = doTransaction(1, id, amount, description, cc, null);
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = doTransaction(4, id, amount, description, cc, data);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = doTransaction(3, id, amount, description, cc, data);
        writeVoid(amount);
        return retval;
    }

    protected String prepareRequest(Hashtable req) {
        if (req.size() == 0) {
            return "";
        }
        req.put("USER", this.login);
        req.put("PWD", this.password);
        if (this.partner != null) {
            req.put("PARTNER", this.partner);
        }
        if (this.vendor != null) {
            req.put("VENDOR", this.vendor);
        }
        Enumeration e = req.keys();
        Object key = e.nextElement();
        StringBuffer buf = new StringBuffer();
        buf.append(makePair((String) key, (String) req.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            buf.append("&").append(makePair((String) key2, (String) req.get(key2)));
        }
        return buf.toString();
    }

    protected String makePair(String key, String value) {
        StringBuffer st = new StringBuffer(key);
        if (value.indexOf(61) >= 0 || value.indexOf(38) >= 0) {
            st.append('[').append(value.length()).append(']');
        }
        st.append('=').append(value);
        return st.toString();
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        HashMap result = new HashMap();
        boolean IsError = true;
        String error = "";
        long trId = writeLog(id, amount, trType);
        Hashtable request = new Hashtable();
        String response = "";
        switch (trType) {
            case 0:
            case 1:
                request.put("AMT", formatAmount(amount));
                request.put("TENDER", "C");
                if (0 == trType) {
                    request.put("TRXTYPE", "S");
                } else {
                    request.put("TRXTYPE", "A");
                }
                request.put("ACCT", cc.getNumber());
                request.put("EXPDATE", cc.getExp(expDate));
                request.put("STREET", cc.getAddress());
                request.put("ZIP", cc.getZip());
                break;
            case 4:
                request.put("AMT", formatAmount(amount));
            case 3:
                request.put("TENDER", "C");
                if (4 == trType) {
                    request.put("TRXTYPE", "D");
                } else {
                    request.put("TRXTYPE", "V");
                }
                request.put("ORIGID", data.get("id"));
                break;
        }
        request.put("COMMENT1", description);
        request.put("COMMENT2", Long.toString(id) + "#" + cc.getFirstName() + "#" + cc.getLastName());
        String data_out = prepareRequest(request);
        try {
            PFProAPI pn = new PFProAPI();
            pn.ProcessTransaction(this.server, Integer.toString(this.port), "", "", "", "", data_out, "60");
            response = pn.TransactionResp;
            Hashtable res = new Hashtable();
            while (pn.GetNextNameValueResponsePair()) {
                res.put(pn.RespName, pn.RespValue);
            }
            if (res.size() >= 3) {
                if (res.get("RESULT").toString().equals("0")) {
                    result.put("id", res.get("PNREF"));
                    result.put("amount", new Double(amount));
                    result.put("AVSADDR", res.get("AVSADDR"));
                    result.put("AVSZIP", res.get("AVSZIP"));
                    IsError = false;
                } else {
                    error = error + "\n" + res.get("RESPMSG").toString();
                }
            } else {
                error = "\nBad response from processor center " + response;
            }
        } catch (Exception e) {
            error = error + "\n" + e.getMessage();
        }
        if (!IsError && ((0 == trType || 1 == trType) && !checkAVS(result))) {
            try {
                voidAuthorize(id, "Voided due to AVS warning", result, cc);
            } catch (Exception e2) {
                error = error + "D ";
                System.err.println("Void error, id=" + result.get("id").toString());
                e2.printStackTrace();
            }
            error = error + "\nepayment.avsfail\n" + error;
            IsError = true;
        }
        writeLog(trId, id, amount, trType, data_out, response, error, !IsError);
        if (IsError) {
            throw new Exception(error);
        }
        return result;
    }

    protected boolean checkAVS(HashMap response) throws Exception {
        String avsAddr = response.get("AVSADDR") == null ? "X" : response.get("AVSADDR").toString();
        String avsZip = response.get("AVSZIP") == null ? "X" : response.get("AVSZIP").toString();
        switch (this.avs.charAt(0)) {
            case 'F':
                if (!"Y".equals(avsAddr) || !"Y".equals(avsZip)) {
                    return false;
                }
                return true;
            case 'L':
                if ("N".equals(avsAddr) && "N".equals(avsZip)) {
                    return false;
                }
                return true;
            case 'M':
                if (!"Y".equals(avsAddr) && !"Y".equals(avsZip)) {
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Verisign PayFlow Pro (www.verisign.com)";
    }
}
