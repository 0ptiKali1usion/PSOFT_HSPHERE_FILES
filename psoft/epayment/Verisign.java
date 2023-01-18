package psoft.epayment;

import com.Verisign.payment.PFProAPI;
import com.sun.net.ssl.internal.ssl.Provider;
import java.io.IOException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import sun.security.provider.Sun;

/* loaded from: hsphere.zip:psoft/epayment/Verisign.class */
public class Verisign extends GenericMerchantGateway {
    protected String server;
    protected int port;
    protected String partner;
    protected String login;
    protected String vendor;
    protected String password;
    protected String avs;
    protected String certPath;
    private static final SimpleDateFormat dateFormat;

    static {
        defaultValues.put("SERVER", "connect.signio.com");
        defaultValues.put("PORT", "443");
        defaultValues.put("AVS", "N");
        Security.addProvider(new Sun());
        Security.addProvider(new Provider());
        dateFormat = new SimpleDateFormat("MMyy");
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
        this.certPath = getValue(v, "CERT_PATH");
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
        map.put("CERT_PATH", this.certPath);
        map.put("AVS", this.password);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap result = processTransaction(0, id, amount, description, cc, new HashMap());
        if (result.containsKey("AVSWARN") || result.containsKey("CVVWARN")) {
            try {
                voidAuthorize(id, description, result, cc);
            } catch (Exception ex) {
                Session.getLog().error("The transaction " + result.get("id") + " can't be cancelled automatically ", ex);
            }
            throw new Exception(result.containsKey("AVSWARN") ? "Problem processing Credit Card: AVS mismatch" : "Problem processing Credit Card: CVV mismatch");
        }
        writeCharge(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap result = processTransaction(1, id, amount, description, cc, new HashMap());
        if (result.containsKey("AVSWARN") || result.containsKey("CVVWARN")) {
            try {
                voidAuthorize(id, description, result, cc);
            } catch (Exception ex) {
                Session.getLog().error("The transaction " + result.get("id") + " can't be cancelled automatically ", ex);
            }
            throw new Exception(result.containsKey("AVSWARN") ? "Problem processing Credit Card: AVS mismatch" : "Problem processing Credit Card: CVV mismatch");
        }
        writeAuthorize(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = processTransaction(4, id, amount, description, cc, data);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = processTransaction(3, id, amount, description, cc, data);
        writeCapture(amount);
        return retval;
    }

    protected String makePair(String key, String value) {
        StringBuffer st = new StringBuffer(key);
        if (value.indexOf(61) >= 0 || value.indexOf(38) >= 0) {
            st.append('[').append(value.length()).append(']');
        }
        st.append('=').append(value);
        return st.toString();
    }

    protected HashMap postForm(HashMap data) throws Exception {
        HashMap result = new HashMap();
        StringBuffer request = new StringBuffer();
        for (String name : data.keySet()) {
            String value = (String) data.get(name);
            if (request.length() > 0) {
                request.append("&").append(makePair(name, value));
            } else {
                request.append(makePair(name, value));
            }
        }
        PFProAPI pn = new PFProAPI();
        pn.SetCertPath(this.certPath);
        pn.CreateContext(this.server, this.port, 60, "", 0, "", "");
        String response = pn.SubmitTransaction(request.toString());
        StringTokenizer st = new StringTokenizer(response, "&=");
        while (st.hasMoreTokens()) {
            String respName = st.nextToken();
            String respValue = "";
            if (st.hasMoreTokens()) {
                respValue = st.nextToken();
            }
            result.put(respName, respValue);
        }
        if (result.size() < 3) {
            Session.getLog().error("Bad response from processor center: " + response);
            throw new Exception("Bad response from processor center " + response);
        }
        return result;
    }

    protected HashMap processTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        String error = "";
        boolean success = false;
        long logId = writeLog(id, amount, trType);
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        try {
            request.put("AMT", formatAmount(amount));
            request.put("TENDER", "C");
            request.put("COMMENT1", description);
            request.put("COMMENT2", Long.toString(id) + "#" + cc.getFirstName() + "#" + cc.getLastName());
            switch (trType) {
                case 0:
                case 1:
                    if (trType == 1) {
                        request.put("TRXTYPE", "A");
                    } else {
                        request.put("TRXTYPE", "S");
                    }
                    request.put("ACCT", cc.getNumber());
                    request.put("EXPDATE", cc.getExp(dateFormat));
                    request.put("STREET", cc.getAddress());
                    request.put("ZIP", cc.getZip());
                    String cvv = cc.getCVV();
                    if (cvv != null && !"".equals(cvv)) {
                        request.put("CVV2", cvv);
                        break;
                    }
                    break;
                case 3:
                    request.put("TRXTYPE", "V");
                    request.put("ORIGID", data.get("id"));
                    break;
                case 4:
                    request.put("TRXTYPE", "D");
                    request.put("ORIGID", data.get("id"));
                    break;
            }
            request.put("USER", this.login);
            request.put("PWD", this.password);
            if (this.partner != null) {
                request.put("PARTNER", this.partner);
            }
            if (this.vendor != null) {
                request.put("VENDOR", this.vendor);
            }
            response = postForm(request);
            if (response.containsKey("RESULT")) {
                String res = (String) response.get("RESULT");
                if ("0".equals(res)) {
                    if (trType == 0 || trType == 1) {
                        boolean avsStatus = checkAVS((String) response.get("AVSADDR"), (String) response.get("AVSZIP"));
                        boolean cvvMatch = (request.containsKey("CVV2") && "N".equals(response.get("CVV2MATCH"))) ? false : true;
                        if (!avsStatus) {
                            result.put("AVSWARN", "AVS");
                        }
                        if (!cvvMatch) {
                            result.put("CVVWARN", "CVV");
                        }
                    }
                    result.put("id", response.get("PNREF"));
                    result.put("amount", new Double(amount));
                    success = true;
                }
            }
            if (!success && response.containsKey("RESPMSG")) {
                error = (String) response.get("RESPMSG");
            }
        } catch (IOException ioex) {
            error = "Problem connecting to processor center: " + ioex.getMessage();
        } catch (Exception ex) {
            error = "Problem processing Credit Card: " + ex.getMessage();
        }
        if (request.containsKey("CVV2")) {
            cc.setCVVChecked(success);
            request.remove("CVV2");
            request.put("CVV2", "****");
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        return result;
    }

    protected boolean checkAVS(String avsAddr, String avsZip) throws Exception {
        String vAddr = avsAddr == null ? "X" : avsAddr;
        String vZip = avsZip == null ? "X" : avsZip;
        switch (this.avs.charAt(0)) {
            case 'F':
                if (!"Y".equals(vAddr) || !"Y".equals(vZip)) {
                    return false;
                }
                return true;
            case 'L':
                if ("N".equals(vAddr) && "N".equals(vZip)) {
                    return false;
                }
                return true;
            case 'M':
                if (!"Y".equals(vAddr) && !"Y".equals(vZip)) {
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        return checkCCCVV(acctid, cc);
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Verisign PayLink Pro (www.verisign.com)";
    }
}
