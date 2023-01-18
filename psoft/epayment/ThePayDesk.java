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

/* loaded from: hsphere.zip:psoft/epayment/ThePayDesk.class */
public class ThePayDesk extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String customerID;
    protected String adminEmail;
    private static final SimpleDateFormat expDate = new SimpleDateFormat("MMyy");

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.customerID = getValue(v, "ID");
        this.adminEmail = getValue(v, "NOTIFICATIONEMAIL");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("ID", this.customerID);
        map.put("NOTIFICATIONEMAIL", this.adminEmail);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap();
        String request = prepareRequest(id, description, cc, amount);
        String error = "";
        String response = "";
        boolean success = true;
        new HashMap();
        try {
            HashMap result = postForm(request);
            response = (String) result.get("response");
            retval.put("amount", new Double(amount));
            if (!"00".equals(result.get("code"))) {
                success = false;
                error = (String) result.get("error");
            } else {
                retval.put("id", result.get("trid"));
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 0, request, response, error, success);
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
        String request = prepareRequest(id, description, cc, amount);
        String error = "";
        String response = "";
        boolean success = true;
        new HashMap();
        try {
            HashMap result = postForm(request);
            response = (String) result.get("response");
            retval.put("amount", new Double(amount));
            if (!"00".equals(result.get("code"))) {
                success = false;
                error = (String) result.get("error");
            } else {
                retval.put("id", result.get("trid"));
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(trId, id, amount, 1, request, response, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = new HashMap();
        retval.put("id", data.get("id"));
        long trId = writeLog(id, amount, 4);
        writeLog(trId, id, amount, 4, "This transaction should be settled manually: Transaction ID:" + data.get("id"), "", "", true);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 3);
        writeLog(trId, id, amount, 3, "This transaction should be voided manually: Transaction ID:" + data.get("id"), "", "", true);
        sendEmail(this.adminEmail, 3, description, id, "TransactionID:" + data.get("id"));
        writeVoid(amount);
        return new HashMap();
    }

    protected String prepareRequest(long id, String description, CreditCard cc, double amount) throws Exception {
        Hashtable htReq = new Hashtable();
        htReq.put("u", this.customerID);
        htReq.put("a", formatAmount(amount));
        htReq.put("d", description);
        htReq.put("cchn", cc.getName());
        htReq.put("ccnum", cc.getNumber());
        htReq.put("cce", cc.getExp(expDate));
        htReq.put("pn", cc.getName());
        htReq.put("pe", cc.getEmail());
        htReq.put("pa", cc.getAddress());
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

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap(2);
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
            if (inputLine == null) {
                break;
            }
            buf.append(inputLine);
        }
        in.close();
        String response = buf.toString();
        result.put("response", response);
        StringTokenizer st = new StringTokenizer(response, ":");
        ArrayList resList = new ArrayList();
        while (st.hasMoreTokens()) {
            resList.add(st.nextToken());
        }
        String code = "";
        String mess = "";
        if (resList.size() < 2) {
            mess = "Bad response from processor center" + response;
        }
        if (resList.size() >= 2) {
            code = resList.get(0).toString();
            mess = resList.get(1).toString();
        }
        if ("00".equals(code)) {
            result.put("code", code);
            result.put("trid", mess);
            result.put("error", "");
        } else {
            result.put("code", code);
            result.put("trid", "");
            result.put("error", mess);
        }
        return result;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "ThepayDesc http://thepaydesk.com";
    }
}
