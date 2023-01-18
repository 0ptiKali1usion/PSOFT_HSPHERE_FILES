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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/AuthorizeNet.class */
public class AuthorizeNet extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String login;
    protected String password;
    protected String referer;
    protected String mode;

    static {
        defaultValues.put("SERVER", "secure.authorize.net");
        defaultValues.put("PATH", "/gateway/transact.dll");
        defaultValues.put("PORT", "443");
        defaultValues.put("MODE", "FALSE");
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.mode = getValue(v, "MODE");
        this.login = getValue(v, "LOGIN");
        this.password = getValue(v, "PASSWORD");
        this.referer = getValue(v, "REFERER");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("MODE", this.mode);
        map.put("LOGIN", this.login);
        if (this.password != null && !"".equals(this.password)) {
            map.put("PASSWORD", this.password);
        }
        map.put("REFERER", this.referer);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "AUTH_CAPTURE");
        request.put("x_Card_Num", cc.getNumber());
        request.put("x_Exp_Date", cc.getExp());
        request.put("x_First_Name", cc.getFirstName());
        request.put("x_Last_Name", cc.getLastName());
        request.put("x_Address", cc.getAddress());
        request.put("x_City", cc.getCity());
        request.put("x_State", cc.getState());
        request.put("x_Zip", cc.getZip());
        request.put("x_Country", cc.getCountry());
        request.put("x_Phone", cc.getPhone());
        request.put("x_EMail", cc.getEmail());
        request.put("x_Description", description);
        request.put("x_Company", cc.getName());
        request.put("x_Invoice_Num", Long.toString(TimeUtils.currentTimeMillis()));
        request.put("x_Cust_ID", Long.toString(id));
        String data_out = prepareRequest(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
            if (!"1".equals(result.get("result"))) {
                success = false;
                error = (String) result.get("message");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 0, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 1);
        HashMap retval = new HashMap(2);
        Hashtable request = new Hashtable();
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "AUTH_ONLY");
        request.put("x_Card_Num", cc.getNumber());
        request.put("x_Exp_Date", cc.getExp());
        request.put("x_First_Name", cc.getFirstName());
        request.put("x_Last_Name", cc.getLastName());
        request.put("x_Address", cc.getAddress());
        request.put("x_City", cc.getCity());
        request.put("x_State", cc.getState());
        request.put("x_Zip", cc.getZip());
        request.put("x_Country", cc.getCountry());
        request.put("x_Phone", cc.getPhone());
        request.put("x_EMail", cc.getEmail());
        request.put("x_Description", description);
        request.put("x_Company", cc.getName());
        request.put("x_Invoice_Num", Long.toString(TimeUtils.currentTimeMillis()));
        request.put("x_Cust_ID", Long.toString(id));
        String data_out = prepareRequest(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("amount", new Double(amount));
            retval.put("id", result.get("id"));
            if (!"1".equals(result.get("result"))) {
                success = false;
                error = (String) result.get("message");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 1, data_out, data_in, error, success);
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
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        if (this.password != null && !"".equals(this.password)) {
            request.put("x_Password", this.password);
        }
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "PRIOR_AUTH_CAPTURE");
        request.put("x_Card_Num", cc.getNumber());
        request.put("x_Exp_Date", cc.getExp());
        request.put("x_Trans_ID", data.get("id"));
        request.put("x_Description", description);
        request.put("x_Company", cc.getName());
        request.put("x_Invoice_Num", Long.toString(TimeUtils.currentTimeMillis()));
        request.put("x_Cust_ID", Long.toString(id));
        String data_out = prepareRequest(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
            if (!"1".equals(result.get("result"))) {
                success = false;
                error = (String) result.get("message");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 4, data_out, data_in, error, success);
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
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        if (this.password != null && !"".equals(this.password)) {
            request.put("x_Password", this.password);
        }
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "VOID");
        request.put("x_Card_Num", cc.getNumber());
        request.put("x_Exp_Date", cc.getExp());
        request.put("x_Trans_ID", data.get("id"));
        request.put("x_Description", description);
        request.put("x_Company", cc.getName());
        request.put("x_Invoice_Num", Long.toString(TimeUtils.currentTimeMillis()));
        request.put("x_Cust_ID", Long.toString(id));
        String data_out = prepareRequest(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
            if (!"1".equals(result.get("result"))) {
                success = false;
                error = (String) result.get("message");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 3, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeVoid(amount);
        return retval;
    }

    protected String prepareRequest(Hashtable req) {
        if (req.size() == 0) {
            return "";
        }
        req.put("x_ADC_Delim_Data", "TRUE");
        req.put("x_ADC_Delim_Character", ",");
        req.put("x_ADC_Encapsulate_Character", "");
        req.put("x_ADC_Relay_Response", "TRUE");
        req.put("x_ADC_URL", "FALSE");
        req.put("x_Version", "3.1");
        req.put("x_Test_Request", this.mode);
        req.put("x_Login", this.login);
        Enumeration e = req.keys();
        Object key = e.nextElement();
        StringBuffer buf = new StringBuffer();
        buf.append(key).append("=").append(URLEncoder.encode((String) req.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            buf.append("&").append(key2).append("=").append(URLEncoder.encode((String) req.get(key2)));
        }
        return buf.toString();
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap(2);
        URL post = new URL("https", this.server, this.port, this.path);
        HttpURLConnection postConn = (HttpURLConnection) post.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setDoOutput(true);
        if (this.referer != null) {
            postConn.setRequestProperty("Referer", this.referer);
        }
        PrintWriter out = new PrintWriter(postConn.getOutputStream());
        out.println(data);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(postConn.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while (true) {
            String inputLine = in.readLine();
            if (inputLine == null) {
                break;
            }
            buf.append(inputLine);
        }
        in.close();
        String response = buf.toString();
        result.put("response", response);
        StringTokenizer st = new StringTokenizer(response, "\",|");
        Vector resList = new Vector();
        while (st.hasMoreTokens()) {
            resList.add(st.nextToken());
        }
        if (resList.size() < 4) {
            throw new Exception("Bad response from processor center" + response);
        }
        result.put("id", resList.get(6).toString());
        result.put("result", resList.get(0));
        result.put("message", resList.get(3));
        return result;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "AuthorizeNet (www.authorizenet.com)";
    }
}
