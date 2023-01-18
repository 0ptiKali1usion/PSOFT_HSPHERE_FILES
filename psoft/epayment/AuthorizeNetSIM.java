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
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import psoft.encryption.MD5;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/AuthorizeNetSIM.class */
public class AuthorizeNetSIM extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String login;
    protected String transKey;
    protected String referer;
    protected String mode;
    protected static Random rand = new Random();
    protected static final int blockSize = 64;
    protected static final byte MASK1 = 54;
    protected static final byte MASK2 = 92;

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
        this.referer = getValue(v, "REFERER");
        this.transKey = getValue(v, "KEY");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("MODE", this.mode);
        map.put("LOGIN", this.login);
        map.put("REFERER", this.referer);
        map.put("KEY", this.transKey);
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
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            request.put("x_Card_Code", cvv);
        }
        request.put("x_Exp_Date", cc.getExp());
        request.put("x_First_Name", cc.getFirstName());
        request.put("x_Last_Name", cc.getLastName());
        request.put("x_Address", cc.getAddress());
        request.put("x_City", cc.getCity());
        request.put("x_State", cc.getState());
        request.put("x_Zip", cc.getZip());
        request.put("x_Country", cc.getCountry());
        request.put("x_Phone", cc.getPhone());
        request.put("x_Email", cc.getEmail());
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
        writeLog(trId, id, amount, 0, excludeCVV(data_out), data_in, error, success);
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
        HashMap retval = new HashMap(2);
        Hashtable request = new Hashtable();
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "AUTH_ONLY");
        request.put("x_Card_Num", cc.getNumber());
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            request.put("x_Card_Code", cvv);
        }
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
        request.put("x_Company", cc.getCompany());
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
        writeLog(trId, id, amount, 1, excludeCVV(data_out), data_in, error, success);
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
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "PRIOR_AUTH_CAPTURE");
        request.put("x_Card_Num", cc.getNumber());
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            request.put("x_Card_Code", cvv);
        }
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
        writeLog(trId, id, amount, 4, excludeCVV(data_out), data_in, error, success);
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
        request.put("x_Amount", formatAmount(amount));
        request.put("x_Method", "CC");
        request.put("x_Type", "VOID");
        request.put("x_Card_Num", cc.getNumber());
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            request.put("x_Card_Code", cvv);
        }
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
        writeLog(trId, id, amount, 3, excludeCVV(data_out), data_in, error, success);
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
        HashMap fingerprint = GenerateFP((String) req.get("x_Amount"));
        req.put("x_ADC_Delim_Data", "TRUE");
        req.put("x_ADC_Delim_Character", ",");
        req.put("x_ADC_Encapsulate_Character", "");
        req.put("x_ADC_Relay_Response", "TRUE");
        req.put("x_ADC_URL", "FALSE");
        req.put("x_Version", "3.1");
        req.put("x_Test_Request", this.mode);
        req.put("x_Login", this.login);
        req.put("x_fp_sequence", fingerprint.get("SEQ"));
        req.put("x_fp_timestamp", fingerprint.get("TIMESTAMP"));
        req.put("x_FP_Hash", fingerprint.get("VAL"));
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
        StringTokenizer st = new StringTokenizer(response, "\",");
        ArrayList resList = new ArrayList();
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
        return "AuthorizeNetSIM (www.authorizenet.com)";
    }

    public HashMap GenerateFP(String amount) {
        Date now = TimeUtils.getDate();
        HashMap map = new HashMap();
        String sequence = Integer.toString(rand.nextInt(HostEntry.VPS_IP));
        String timeStamp = Long.toString(now.getTime() / 1000);
        new StringBuffer();
        MD5 md5 = new MD5();
        byte[] key1 = this.transKey.getBytes();
        int length = 64;
        if (64 < key1.length) {
            length = key1.length;
        }
        byte[] key2 = new byte[length];
        for (int i = 0; i < length; i++) {
            if (i < key1.length) {
                key2[i] = key1[i];
                int i2 = i;
                key2[i2] = (byte) (key2[i2] ^ MASK1);
            } else {
                key2[i] = MASK1;
            }
        }
        byte[] key3 = append(append(key2, this.login.getBytes()), "^".getBytes());
        byte[] key4 = append(append(key3, sequence.getBytes()), "^".getBytes());
        byte[] key5 = append(append(key4, timeStamp.getBytes()), "^".getBytes());
        byte[] key6 = append(append(key5, amount.getBytes()), "^".getBytes());
        byte[] key7 = new byte[length];
        for (int i3 = 0; i3 < length; i3++) {
            if (i3 < key1.length) {
                key7[i3] = key1[i3];
                int i4 = i3;
                key7[i4] = (byte) (key7[i4] ^ MASK2);
            } else {
                key7[i3] = MASK2;
            }
        }
        md5.Update(key6);
        byte[] key8 = append(key7, md5.Final());
        md5.Init();
        md5.Update(key8);
        byte[] result = md5.Final();
        map.put("SEQ", sequence);
        map.put("TIMESTAMP", timeStamp);
        map.put("VAL", asHex(result));
        return map;
    }

    private byte[] append(byte[] op1, byte[] op2) {
        int length = op1.length + op2.length;
        byte[] result = new byte[op1.length + op2.length];
        for (int i = 0; i < length; i++) {
            if (i < op1.length) {
                result[i] = op1[i];
            } else {
                result[i] = op2[i - op1.length];
            }
        }
        return result;
    }

    public static String asHex(byte[] hash) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            if ((hash[i] & 255) < 16) {
                buf.append("0");
            }
            buf.append(Long.toString(hash[i] & 255, 16));
        }
        return buf.toString();
    }

    private String excludeCVV(String str) {
        String res = "";
        try {
            int start = str.indexOf("x_Card_Code");
            if (start >= 0) {
                String res2 = str.substring(0, start + 11);
                res = res2 + "=****";
                int end = str.indexOf(38, start);
                if (end > 0) {
                    res = res + str.substring(end);
                }
            } else {
                res = str;
            }
        } catch (Throwable tr) {
            Session.getLog().error("Error with removing cvv: ", tr);
        }
        return res;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        return checkCCCVV(acctid, cc);
    }
}
