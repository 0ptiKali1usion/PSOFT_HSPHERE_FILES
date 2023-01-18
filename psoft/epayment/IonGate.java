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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/IonGate.class */
public class IonGate extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String login;
    protected String password;
    protected String email;
    private static HashMap ccTypes = new HashMap();
    private static HashMap AVSRespCodes = new HashMap();
    private static final SimpleDateFormat expFormat = new SimpleDateFormat("MMyy");
    private static final NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "US"));

    static {
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        ccTypes.put("VISA", "VISA");
        ccTypes.put("MC", "MASTERCARD");
        ccTypes.put("DISC", "DISCOVER");
        ccTypes.put("AX", "AMEX");
        ccTypes.put("DINERS", "DINERS");
        ccTypes.put("JCB", "JCB");
        AVSRespCodes.put("A", "Address (street) matches, zip does not");
        AVSRespCodes.put("E", "AVS error");
        AVSRespCodes.put("G", "Global (locale/international) non-AVS participant");
        AVSRespCodes.put("N", "No match on address or zip");
        AVSRespCodes.put("R", "Retry, system unavailable or timed out");
        AVSRespCodes.put("S", "Service not supported by issuer");
        AVSRespCodes.put("U", "Address information is unavailable");
        AVSRespCodes.put("W", "9 digit zip matches, address (street) does not");
        AVSRespCodes.put("X", "Exact AVS match");
        AVSRespCodes.put("Y", "Address (street) and 5 digit zip match");
        AVSRespCodes.put("Z", "5 digit zip matches, address (street) does not");
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.login = getValue(v, "LOGIN");
        this.email = getValue(v, "EMAIL");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("LOGIN", this.login);
        map.put("EMAIL", this.email);
        return map;
    }

    private Hashtable prepareData(long id, String description, double amount, CreditCard cc, String trType) throws Exception {
        Hashtable request = new Hashtable();
        request.put("LOGIN", this.login);
        request.put("TYPE", trType);
        request.put("AMOUNT", money.format(amount));
        String cctype = (String) ccTypes.get(cc.getType());
        if (cctype == null) {
            throw new Exception("Incorrect Credit card type or this type of credit card is not supported by merchant gateway");
        }
        request.put("CARDTYPE", ccTypes.get(cc.getType()));
        request.put("CARDNUM", cc.getNumber());
        request.put("EXPIRES", cc.getExp(expFormat));
        request.put("CARDNAME", cc.getName());
        request.put("ADDRESS", cc.getAddress());
        request.put("CITY", cc.getCity());
        request.put("STATE", cc.getState());
        request.put("ZIP", cc.getZip());
        request.put("RECEIPTURL", "DISPLAY");
        return request;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        Hashtable request = prepareData(id, description, amount, cc, "SALE");
        String data_out = prepareRequest(request);
        String error = "";
        HashMap result = new HashMap();
        boolean success = true;
        try {
            result = postForm(data_out);
            if ("AA".equals(result.get("RESPONSECODE")) && "000".equals(result.get("REPLYCODE"))) {
                retval.put("amount", new Double(amount));
                retval.put("id", Long.toString(id));
                String avsResp = (String) result.get("AVSRESPONSE");
                if (!"X".equals(avsResp)) {
                    String avsMess = "AVS checking failed :" + AVSRespCodes.get(avsResp);
                    sendEmail("AVS", description, id, avsMess);
                }
            } else {
                error = (String) result.get("AUTHRESPONSE");
                if (error.length() == 0) {
                    error = "Unspecified Error";
                }
                success = false;
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(id, data_out, result.toString(), error);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(2);
        Hashtable request = prepareData(id, description, amount, cc, "SALE");
        String data_out = prepareRequest(request);
        String error = "";
        HashMap result = new HashMap();
        boolean success = true;
        try {
            result = postForm(data_out);
            if ("AA".equals(result.get("RESPONSECODE")) && "000".equals(result.get("REPLYCODE"))) {
                retval.put("amount", new Double(amount));
                retval.put("id", Long.toString(id));
                String avsResp = (String) result.get("AVSRESPONSE");
                if (!"X".equals(avsResp)) {
                    String avsMess = "AVS chacking failed :" + AVSRespCodes.get(avsResp);
                    sendEmail("AVS", description, id, avsMess);
                }
            } else {
                error = (String) result.get("AUTHRESPONSE");
                if (error.length() == 0) {
                    error = "Unspecified Error";
                }
                success = false;
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(id, data_out, result.toString(), error);
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        retval.put("id", data.get("id"));
        writeLog(id, "", "", "");
        writeCapture(((Double) data.get("amount")).doubleValue());
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        sendEmail("VOID", description, id, "");
        writeLog(id, "void:" + id + ";" + amount, "ok", "");
        writeVoid(amount);
        return data;
    }

    protected String prepareRequest(Hashtable req) {
        if (req.size() == 0) {
            return "";
        }
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
        HashMap results = parseResult(response);
        if (results.size() < 2) {
            throw new Exception("Bad response from processor center" + response);
        }
        return results;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "IonGate ()";
    }

    private HashMap parseResult(String response) {
        HashMap results = new HashMap();
        int pos = response.indexOf("Return Values</h2>");
        if (pos < 0) {
            return results;
        }
        String resp = response.substring(pos + 18);
        while (resp.length() > 0) {
            int pos2 = resp.indexOf("<BR>");
            if (pos2 < 0) {
                return results;
            }
            String pair = resp.substring(0, pos2);
            int index = pair.indexOf(61);
            String name = pair.substring(0, index);
            String value = pair.substring(index + 1);
            resp = resp.substring(pos2 + 4);
            results.put(name, value);
        }
        return results;
    }

    private void sendEmail(String subject, String description, long id, String mess) throws Exception {
        String message = "";
        if ("VOID".equals(subject)) {
            message = "You need to void transaction with account " + Long.toString(id);
        } else if ("AVS".equals(subject)) {
            message = "The transaction with account " + Long.toString(id) + " returned AVS warning.";
        }
        String body = "Dear administrator,\n" + message + "\ndescription: " + description + "\n" + mess;
        Session.getMailer().sendMessage(this.email, message, body, Session.getCurrentCharset());
    }
}
