package psoft.epayment;

import com.epx.busobj.Request;
import com.epx.crypto.EPXEncrypt;
import com.epx.util.EPXException;
import com.epx.util.Xtran;
import com.epx.util.XtranProperties;
import com.sun.net.ssl.internal.ssl.Provider;
import cryptix.provider.Cryptix;
import java.io.StringReader;
import java.security.Security;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/* loaded from: hsphere.zip:psoft/epayment/InterCept.class */
public class InterCept extends GenericMerchantGateway {
    protected String server;
    protected int port;
    protected String companyKey;
    protected String securityKey;
    protected String currency;
    protected String terminalID;
    private static HashMap errmap = new HashMap();
    private static final NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "US"));
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMyy");
    private static final SimpleDateFormat expMonth;
    private static final SimpleDateFormat expYear;

    static {
        Security.addProvider(new Provider());
        Security.addProvider(new Cryptix());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        errmap.put("00", "Payment server validation approved");
        errmap.put("AR", "ACCOUNT_NUMBER bin is not setup to process (replaces 'CR')");
        errmap.put("AX", "Transaction amount value requirements exceeded, see response text for details");
        errmap.put("CD", "Commercial data already associated");
        errmap.put("CF", "Credit refused, must have a relevant sale in order to process credit");
        errmap.put("DC", "Data Conflict");
        errmap.put("DF", "Date-Frequency Mismatch");
        errmap.put("DR", "Delete Refused - data integrity enforcement");
        errmap.put("IB", "Invalid Base64 Encoding");
        errmap.put("IC", "Missing/Invalid Company Key");
        errmap.put("ID", "Missing/Invalid Transaction Data");
        errmap.put("IE", "Invalid Encryption");
        errmap.put("IK", "Invalid Key (See RESPONSE_TEXT for the invalid key)");
        errmap.put("IT", "Invalid XML Transmission Format");
        errmap.put("IX", "Invalid XML Transaction Format");
        errmap.put("IY", "Invalid Type Attribute");
        errmap.put("IZ", "Invalid Compression (future use)");
        errmap.put("LM", "Field LAST_FOUR did not match last four digits of card holder<92>s account number that is contained in TRACK_DATA");
        errmap.put("MK", "Missing Key (See RESPONSE_TEXT for the missing key)");
        errmap.put("MY", "Missing Type Attribute");
        errmap.put("NF", "Transaction not found");
        errmap.put("NM", "No data mapping, Please call InterCept Payment Solutions");
        errmap.put("NS", "Transaction not settled");
        errmap.put("NX", "No XML <91>FIELDS<92> Node present");
        errmap.put("SU", "System Unavailable, Retry");
        errmap.put("TC", "Transaction Already Captured");
        errmap.put("TD", "Transaction Already Deleted");
        errmap.put("TS", "Transaction already settled");
        errmap.put("TV", "Transaction already voided");
        errmap.put("UP", "Unable to Process at this time, Retry");
        errmap.put("XE", "Currency conversion error, please call InterCept Payment Solutions");
        expMonth = new SimpleDateFormat("MM");
        expYear = new SimpleDateFormat("yy");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.companyKey = getValue(v, "COMPANYKEY");
        this.securityKey = getValue(v, "SECURITYKEY");
        this.currency = getValue(v, "CURRCODE");
        this.terminalID = getValue(v, "TERMINALID");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("COMPANYKEY", this.companyKey);
        map.put("SECURITYKEY", this.securityKey);
        map.put("CURRCODE", this.currency);
        map.put("TERMINALID", this.terminalID);
        return map;
    }

    private void setProperties(XtranProperties properties) throws Exception {
        properties.setServerIP(this.server);
        properties.setServerPort(this.port);
        properties.setCompanyKey(this.companyKey);
        properties.setComBufferSize(2048);
        properties.setSocketTimeout(5000);
        properties.setSecurityKey(this.securityKey);
        properties.setEncryptionType(3);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        XtranProperties prop = new XtranProperties();
        new EPXEncrypt();
        Request req = new Request();
        boolean success = true;
        String error = "";
        String reqData = "";
        HashMap resp = new HashMap();
        HashMap map = new HashMap();
        try {
            setProperties(prop);
            req.setProperty("SERVICE", "CC");
            req.setProperty("SERVICE_TYPE", "DEBIT");
            req.setProperty("SERVICE_SUBTYPE", "SALE");
            req.setProperty("SERVICE_FORMAT", "1010");
            req.setProperty("TERMINAL_ID", this.terminalID);
            req.setProperty("OPERATOR", "JAVA ");
            req.setProperty("AMOUNT", money.format(amount));
            req.setProperty("EXPIRATION", cc.getExp(dateFormat));
            req.setProperty("ACCOUNT_NUMBER", cc.getNumber());
            req.setProperty("CURRENCY_CODE", this.currency);
            req.setProperty("FIRST_NAME", cc.getFirstName());
            req.setProperty("LAST_NAME", cc.getLastName());
            req.setProperty("ADDRESS", cc.getAddress());
            req.setProperty("CITY", cc.getCity());
            req.setProperty("STATE", cc.getState());
            req.setProperty("POSTAL_CODE", cc.getZip());
            req.setProperty("PHONE", cc.getPhone());
            req.setProperty("COUNTRY", cc.getCountry());
            req.setProperty("TRANSACTION_INDICATOR", "7");
            reqData = req.asXML();
            resp = postForm(req, prop);
            String respResult = (String) resp.get("ARC");
            if (!"00".equals(respResult)) {
                String ErrorMessage = processError(resp);
                success = false;
                error = "Credit card processing error: " + ErrorMessage;
            }
            map.put("id", resp.get("TRANSACTION_ID"));
            map.put("amount", new Double(amount));
        } catch (EPXException ex) {
            success = false;
            error = ex.toString();
        }
        writeLog(id, reqData, resp.toString(), error);
        if (!success) {
            if ("".equals(error)) {
                error = "Unknown error with the merchant gateway";
            }
            throw new Exception(error);
        }
        writeCharge(amount);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        XtranProperties prop = new XtranProperties();
        Request req = new Request();
        boolean success = true;
        String error = "";
        String reqData = "";
        HashMap resp = new HashMap();
        HashMap map = new HashMap();
        try {
            setProperties(prop);
            req.setProperty("SERVICE", "CC");
            req.setProperty("SERVICE_TYPE", "DEBIT");
            req.setProperty("SERVICE_SUBTYPE", "AUTH");
            req.setProperty("SERVICE_FORMAT", "1010");
            req.setProperty("TERMINAL_ID", this.terminalID);
            req.setProperty("OPERATOR", "JAVA ");
            req.setProperty("AMOUNT", money.format(amount));
            req.setProperty("EXPIRATION", cc.getExp(dateFormat));
            req.setProperty("ACCOUNT_NUMBER", cc.getNumber());
            req.setProperty("CURRENCY_CODE", this.currency);
            req.setProperty("FIRST_NAME", cc.getFirstName());
            req.setProperty("LAST_NAME", cc.getLastName());
            req.setProperty("ADDRESS", cc.getAddress());
            req.setProperty("CITY", cc.getCity());
            req.setProperty("STATE", cc.getState());
            req.setProperty("POSTAL_CODE", cc.getZip());
            req.setProperty("PHONE", cc.getPhone());
            req.setProperty("COUNTRY", cc.getCountry());
            req.setProperty("TRANSACTION_INDICATOR", "7");
            reqData = req.asXML();
            resp = postForm(req, prop);
            String respResult = (String) resp.get("ARC");
            if (!"00".equals(respResult)) {
                String ErrorMessage = processError(resp);
                success = false;
                error = "Credit card processing error: " + ErrorMessage;
            }
            map.put("id", resp.get("TRANSACTION_ID"));
            map.put("amount", new Double(amount));
        } catch (Exception ex) {
            success = false;
            error = ex.getMessage();
        }
        writeLog(id, reqData, resp.toString(), error);
        if (!success) {
            if ("".equals(error)) {
                error = "Unknown error with the merchant gateway";
            }
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        XtranProperties prop = new XtranProperties();
        Request req = new Request();
        boolean success = true;
        String error = "";
        String reqData = "";
        HashMap resp = new HashMap();
        HashMap map = new HashMap();
        double amount = ((Double) data.get("amount")).doubleValue();
        try {
            setProperties(prop);
            req.setProperty("SERVICE", "CC");
            req.setProperty("SERVICE_TYPE", "DEBIT");
            req.setProperty("SERVICE_SUBTYPE", "CAPTURE");
            req.setProperty("SERVICE_FORMAT", "1010");
            req.setProperty("TERMINAL_ID", this.terminalID);
            req.setProperty("OPERATOR", "JAVA ");
            req.setProperty("AMOUNT", money.format(amount));
            req.setProperty("EXPIRATION", cc.getExp(dateFormat));
            req.setProperty("ACCOUNT_NUMBER", cc.getNumber());
            req.setProperty("CURRENCY_CODE", this.currency);
            req.setProperty("FIRST_NAME", cc.getFirstName());
            req.setProperty("LAST_NAME", cc.getLastName());
            req.setProperty("ADDRESS", cc.getAddress());
            req.setProperty("CITY", cc.getCity());
            req.setProperty("STATE", cc.getState());
            req.setProperty("POSTAL_CODE", cc.getZip());
            req.setProperty("PHONE", cc.getPhone());
            req.setProperty("COUNTRY", cc.getCountry());
            req.setProperty("TRANSACTION_INDICATOR", "7");
            req.setProperty("TRANSACTION_ID", (String) data.get("id"));
            reqData = req.asXML();
            resp = postForm(req, prop);
            String respResult = (String) resp.get("ARC");
            if (!"00".equals(respResult)) {
                String ErrorMessage = processError(resp);
                success = false;
                error = "Credit card processing error: " + ErrorMessage;
            }
            map.put("id", resp.get("TRANSACTION_ID"));
            map.put("amount", new Double(amount));
        } catch (Exception ex) {
            success = false;
            error = ex.getMessage();
        }
        writeLog(id, reqData, resp.toString(), error);
        if (!success) {
            if ("".equals(error)) {
                error = "Unknown error with the merchant gateway";
            }
            throw new Exception(error);
        }
        writeCapture(amount);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        XtranProperties prop = new XtranProperties();
        Request req = new Request();
        boolean success = true;
        String error = "";
        String reqData = "";
        HashMap resp = new HashMap();
        HashMap map = new HashMap();
        double amount = ((Double) data.get("amount")).doubleValue();
        try {
            setProperties(prop);
            req.setProperty("SERVICE", "CC");
            req.setProperty("SERVICE_TYPE", "DEBIT");
            req.setProperty("SERVICE_SUBTYPE", "VOID");
            req.setProperty("SERVICE_FORMAT", "1010");
            req.setProperty("TERMINAL_ID", this.terminalID);
            req.setProperty("OPERATOR", "JAVA ");
            req.setProperty("AMOUNT", money.format(amount));
            req.setProperty("EXPIRATION", cc.getExp(dateFormat));
            req.setProperty("ACCOUNT_NUMBER", cc.getNumber());
            req.setProperty("CURRENCY_CODE", this.currency);
            req.setProperty("FIRST_NAME", cc.getFirstName());
            req.setProperty("LAST_NAME", cc.getLastName());
            req.setProperty("ADDRESS", cc.getAddress());
            req.setProperty("CITY", cc.getCity());
            req.setProperty("STATE", cc.getState());
            req.setProperty("POSTAL_CODE", cc.getZip());
            req.setProperty("PHONE", cc.getPhone());
            req.setProperty("COUNTRY", cc.getCountry());
            req.setProperty("TRANSACTION_INDICATOR", "7");
            req.setProperty("TRANSACTION_ID", (String) data.get("id"));
            reqData = req.asXML();
            resp = postForm(req, prop);
            String respResult = (String) resp.get("ARC");
            if (!"00".equals(respResult)) {
                String ErrorMessage = processError(resp);
                success = false;
                error = "Credit card processing error: " + ErrorMessage;
            }
            map.put("id", resp.get("TRANSACTION_ID"));
            map.put("amount", new Double(amount));
        } catch (Exception ex) {
            success = false;
            error = ex.getMessage();
        }
        writeLog(id, reqData, resp.toString(), error);
        if (!success) {
            if ("".equals(error)) {
                error = "Unknown error with the merchant gateway";
            }
            throw new Exception(error);
        }
        writeVoid(amount);
        return data;
    }

    private HashMap postForm(Request xreq, XtranProperties xprop) throws Exception {
        HashMap map = new HashMap();
        Xtran control = new Xtran();
        String response = control.submit(xreq, xprop);
        if ("".equals(response)) {
            throw new Exception("Bad response from processor center");
        }
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(response)));
        Document doc = parser.getDocument();
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("FIELD");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            String fieldName = node.getAttributes().getNamedItem("KEY").getNodeValue();
            String fieldValue = node.getFirstChild().getNodeValue();
            map.put(fieldName, fieldValue);
        }
        if (map.get("TRANSACTION_ID") == null || map.get("ARC") == null) {
            throw new Exception("Bad response from processor center");
        }
        return map;
    }

    private String processError(HashMap resp) {
        String errorMessage = "";
        String mrc = (String) resp.get("MRC");
        String arc = (String) resp.get("ARC");
        if ("ER".equals(arc)) {
            errorMessage = (String) errmap.get(mrc);
        }
        return errorMessage + " Response text: " + ((String) resp.get("RESPONSE_TEXT"));
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "InterCept (http://www.interceptpaymentsolutions.net/index.asp)";
    }
}
