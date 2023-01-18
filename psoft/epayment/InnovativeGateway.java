package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/InnovativeGateway.class */
public class InnovativeGateway extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String username;
    protected String passwd;
    protected String testmode;
    protected String avsLevel;
    private static HashMap ccTypes = new HashMap();
    private static final SimpleDateFormat expMonth = new SimpleDateFormat("MM");
    private static final SimpleDateFormat expYear = new SimpleDateFormat("yy");
    private static ArrayList responseFields = new ArrayList();

    static {
        ccTypes.put("VISA", "visa");
        ccTypes.put("MC", "mc");
        ccTypes.put("AX", "amex");
        ccTypes.put("DINERS", "diners");
        ccTypes.put("DISC", "discover");
        ccTypes.put("JCB", "jcb");
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        defaultValues.put("SERVER", "transactions.innovativegateway.com");
        defaultValues.put("PATH", "/servlet/com.gateway.aai.Aai");
        defaultValues.put("PORT", "443");
        defaultValues.put("MODE", "FALSE");
        responseFields.add("anatransid");
        responseFields.add("direct");
        responseFields.add("avs");
        responseFields.add("trans_delta_msec");
        responseFields.add("upi_delta_msec");
        responseFields.add("clientip");
        responseFields.add("test_override_errors");
        responseFields.add("approval");
        responseFields.add("response_mode");
        responseFields.add("target_app");
        responseFields.add("upg_auth");
        responseFields.add("response_fmt");
        responseFields.add("trantype");
        responseFields.add("delimited_fmt_include_fields");
        responseFields.add("cvv2_result");
        responseFields.add("error");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("MODE", "testing".equals(this.testmode) ? "TRUE" : "FALSE");
        map.put("LOGIN", this.username);
        map.put("PASSWORD", this.passwd);
        map.put("AVS", this.avsLevel);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.testmode = "TRUE".equals(getValue(v, "MODE")) ? "testing" : "";
        this.username = getValue(v, "LOGIN");
        this.passwd = getValue(v, "PASSWORD");
        this.avsLevel = getValue(v, "AVS");
    }

    protected String prepareRequest(int transactionType, long id, String description, CreditCard cc, double amount, HashMap data) throws Exception {
        Hashtable htReq = new Hashtable();
        htReq.put("target_app", "WebCharge_v5.06");
        htReq.put("response_mode", "simple");
        htReq.put("response_fmt", "delimited");
        htReq.put("upg_auth", "zxcvlkjh");
        htReq.put("card_type", ccTypes.get(cc.getType()));
        htReq.put("delimited_fmt_field_delimiter", "=");
        htReq.put("delimited_fmt_include_fields", "true");
        htReq.put("delimited_fmt_value_delimiter", "|");
        htReq.put("pw", this.passwd);
        htReq.put("username", this.username);
        htReq.put("ccnumber", cc.getNumber());
        htReq.put("month", cc.getExp(expMonth));
        htReq.put("year", cc.getExp(expYear));
        htReq.put("ccname", cc.getName());
        htReq.put("baddress", cc.getAddress());
        htReq.put("bcity", cc.getCity());
        htReq.put("bstate", cc.getState());
        htReq.put("bzip", cc.getZip());
        htReq.put("bcountry", cc.getCountry());
        htReq.put("email", cc.getEmail());
        htReq.put("bphone", cc.getPhone());
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            htReq.put("ccidentifier1", cvv);
        }
        htReq.put("fulltotal", formatAmount(amount));
        switch (transactionType) {
            case 0:
                htReq.put("trantype", "sale");
                htReq.put("test_override_errors", this.testmode);
                break;
            case 1:
                htReq.put("trantype", "preauth");
                htReq.put("test_override_errors", this.testmode);
                break;
            case 3:
                htReq.put("trantype", "void");
                htReq.put("trans_id", data.get("id"));
                htReq.put("reference", data.get("approval"));
                break;
            case 4:
                htReq.put("trantype", "postauth");
                htReq.put("trans_id", data.get("id"));
                htReq.put("reference", data.get("approval"));
                htReq.put("authamount", formatAmount(amount));
                htReq.put("voiceauth", "no");
                break;
        }
        StringBuffer buf = new StringBuffer();
        Enumeration e = htReq.keys();
        Object key = e.nextElement();
        buf.append(key).append("=").append(URLEncoder.encode((String) htReq.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            buf.append("&").append(key2).append("=").append(URLEncoder.encode((String) htReq.get(key2)));
        }
        return buf.toString();
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap();
        String request = prepareRequest(0, id, description, cc, amount, new HashMap());
        String error = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            retval.put("amount", new Double(amount));
            String approval = result.containsKey("approval") ? (String) result.get("approval") : "";
            if (!"".equals(approval)) {
                retval.put("id", result.get("anatransid"));
                retval.put("approval", approval);
            } else {
                success = false;
                if (result.containsKey("error")) {
                    error = (String) result.get("error");
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        if ((error == null || "".equals(error)) && !success) {
            error = "Can't get the error message from the response: " + result.toString();
        }
        writeLog(trId, id, amount, 0, excludeCVV(request), result.toString(), error, success);
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
        long trId = writeLog(id, amount, 1);
        HashMap retval = new HashMap();
        String request = prepareRequest(1, id, description, cc, amount, new HashMap());
        String error = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            retval.put("amount", new Double(amount));
            String approval = result.containsKey("approval") ? (String) result.get("approval") : "";
            if (!"".equals(approval)) {
                retval.put("id", result.get("anatransid"));
                retval.put("approval", approval);
            } else {
                success = false;
                if (result.containsKey("error")) {
                    error = (String) result.get("error");
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        if ((error == null || "".equals(error)) && !success) {
            error = "Can't get the error message from the response: " + result.toString();
        }
        writeLog(trId, id, amount, 1, excludeCVV(request), result.toString(), error, success);
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
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 4);
        HashMap retval = new HashMap(2);
        String request = prepareRequest(4, id, description, cc, amount, data);
        String error = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            retval.put("amount", new Double(amount));
            String approval = result.containsKey("approval") ? (String) result.get("approval") : "";
            if (!"".equals(approval)) {
                retval.put("id", result.get("anatransid"));
                retval.put("approval", approval);
            } else {
                success = false;
                if (result.containsKey("error")) {
                    error = (String) result.get("error");
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        if ((error == null || "".equals(error)) && !success) {
            error = "Can't get the error message from the response: " + result.toString();
        }
        writeLog(trId, id, amount, 4, excludeCVV(request), result.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 3);
        HashMap retval = new HashMap();
        String request = prepareRequest(3, id, description, cc, amount, data);
        String error = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            retval.put("amount", new Double(amount));
            String approval = result.containsKey("approval") ? (String) result.get("approval") : "";
            if (!"".equals(approval)) {
                retval.put("id", result.get("anatransid"));
                retval.put("approval", approval);
            } else {
                success = false;
                if (result.containsKey("error")) {
                    error = (String) result.get("error");
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        if ((error == null || "".equals(error)) && !success) {
            error = "Can't get the error message from the response: " + result.toString();
        }
        writeLog(trId, id, amount, 3, excludeCVV(request), result.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeVoid(amount);
        return retval;
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap();
        URL post = new URL("https", this.server, this.port, this.path);
        HttpURLConnection postConn = (HttpURLConnection) post.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setDoOutput(true);
        PrintWriter out = new PrintWriter(postConn.getOutputStream());
        out.println(data);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(postConn.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while (true) {
            String inputLine = in.readLine();
            if (inputLine != null) {
                buf.append(inputLine);
            } else {
                in.close();
                String response = buf.toString();
                parseResults(result, response);
                return result;
            }
        }
    }

    private void parseResults(HashMap results, String data) throws Exception {
        boolean error = false;
        StringTokenizer st = new StringTokenizer(data, "|");
        if (!st.hasMoreTokens()) {
            throw new Exception("Incorrect response has been received: " + data);
        }
        while (!error) {
            try {
                if (!st.hasMoreTokens()) {
                    break;
                }
                String newToken = st.nextToken();
                int pos = newToken.indexOf(61);
                if (pos > 0) {
                    String key = newToken.substring(0, pos).toLowerCase();
                    String val = newToken.substring(pos + 1);
                    if (responseFields.contains(key)) {
                        results.put(key, val);
                    }
                } else {
                    error = true;
                }
            } catch (Exception e) {
                throw new Exception("Incorrect response has been received: " + data);
            }
        }
    }

    private String excludeCVV(String str) {
        return str;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "InnovativeGateway (WebCharge_v5.06) http://www.innovativegateway.com";
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
