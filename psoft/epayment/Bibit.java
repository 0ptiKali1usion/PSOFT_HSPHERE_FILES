package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.resource.HTTPAuth;
import psoft.util.TimeUtils;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/epayment/Bibit.class */
public class Bibit extends GenericMerchantGateway {
    protected String merchCode;
    protected String userName;
    protected String passWord;
    protected String server;
    protected String path;
    protected int port;
    protected String curr_code;
    protected int precision;
    private static HashMap ccTypes;

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        ccTypes = new HashMap();
        ccTypes.put("MC", "ECMC");
        ccTypes.put("AX", "AMEX");
        ccTypes.put("SOLO", "SOLO_GB");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.merchCode = getValue(v, "MERCH_CODE");
        this.userName = getValue(v, "USERNAME");
        this.passWord = getValue(v, "PASSWORD");
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.curr_code = getValue(v, "CURRCODE");
        this.precision = getPrecision(this.curr_code);
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("MERCH_CODE", this.merchCode);
        map.put("USERNAME", this.userName);
        map.put("PASSWORD", this.passWord);
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("CURRCODE", this.curr_code);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        String trans_id = getTransID(id);
        ArrayList xmlMessage = new ArrayList();
        String error = "";
        String data_in = "";
        boolean success = true;
        xmlMessage.add("<?xml version='1.0'?>");
        xmlMessage.add("<!DOCTYPE paymentService PUBLIC '-//Bibit/DTD Bibit PaymentService v1//EN' 'http://dtd.bibit.com/paymentService_v1.dtd'>");
        xmlMessage.add("<paymentService version='1.0' merchantCode='" + this.merchCode + "'>");
        xmlMessage.add("<submit>");
        xmlMessage.add("<order orderCode='" + trans_id + "'>");
        xmlMessage.add("<description>Order</description>");
        xmlMessage.add("<amount currencyCode='" + this.curr_code + "' value='" + formatAmount(amount) + "' exponent='" + this.precision + "'/>");
        xmlMessage.add("<orderContent>" + id + "</orderContent>");
        xmlMessage.add("<paymentDetails>");
        xmlMessage.add("<" + getType(cc.getType()) + "-SSL>");
        xmlMessage.add("<cardNumber>" + cc.getNumber() + "</cardNumber>");
        xmlMessage.add("<expiryDate><date month='" + getMonth(cc.getExp()) + "' year='" + getYear(cc.getExp()) + "'/></expiryDate>");
        xmlMessage.add("<cardHolderName>" + cc.getName() + "</cardHolderName>");
        xmlMessage.add("</" + getType(cc.getType()) + "-SSL>");
        xmlMessage.add("</paymentDetails>");
        xmlMessage.add("</order>");
        xmlMessage.add("</submit>");
        xmlMessage.add("</paymentService>");
        String data_out = prepareRequest(xmlMessage);
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(id, data_out, data_in, error);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(2);
        String trans_id = getTransID(id);
        ArrayList xmlMessage = new ArrayList();
        String error = "";
        String data_in = "";
        boolean success = true;
        xmlMessage.add("<?xml version='1.0'?>");
        xmlMessage.add("<!DOCTYPE paymentService PUBLIC '-//Bibit/DTD Bibit PaymentService v1//EN' 'http://dtd.bibit.com/paymentService_v1.dtd'>");
        xmlMessage.add("<paymentService version='1.0' merchantCode='" + this.merchCode + "'>");
        xmlMessage.add("<submit>");
        xmlMessage.add("<order orderCode='" + trans_id + "'>");
        xmlMessage.add("<description>Order</description>");
        xmlMessage.add("<amount currencyCode='" + this.curr_code + "' value='" + formatAmount(amount) + "' exponent='" + this.precision + "'/>");
        xmlMessage.add("<orderContent>" + id + "</orderContent>");
        xmlMessage.add("<paymentDetails>");
        xmlMessage.add("<" + getType(cc.getType()) + "-SSL>");
        xmlMessage.add("<cardNumber>" + cc.getNumber() + "</cardNumber>");
        xmlMessage.add("<expiryDate><date month='" + getMonth(cc.getExp()) + "' year='" + getYear(cc.getExp()) + "'/></expiryDate>");
        xmlMessage.add("<cardHolderName>" + cc.getName() + "</cardHolderName>");
        xmlMessage.add("</" + getType(cc.getType()) + "-SSL>");
        xmlMessage.add("</paymentDetails>");
        xmlMessage.add("</order>");
        xmlMessage.add("</submit>");
        xmlMessage.add("</paymentService>");
        String data_out = prepareRequest(xmlMessage);
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
            retval.put("amount", new Double(amount));
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(id, data_out, data_in, error);
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
        HashMap retval = new HashMap(1);
        ArrayList xmlMessage = new ArrayList();
        String error = "";
        String data_in = "";
        boolean success = true;
        xmlMessage.add("<?xml version='1.0'?>");
        xmlMessage.add("<!DOCTYPE paymentService PUBLIC '-//Bibit/DTD Bibit PaymentService v1//EN' 'http://dtd.bibit.com/paymentService_v1.dtd'>");
        xmlMessage.add("<paymentService version='1.0' merchantCode='" + this.merchCode + "'>");
        xmlMessage.add("<modify>");
        xmlMessage.add("<orderModification orderCode='" + data.get("id") + "'>");
        xmlMessage.add("<cancel/>");
        xmlMessage.add("</orderModification>");
        xmlMessage.add("</modify>");
        xmlMessage.add("</paymentService>");
        String data_out = prepareRequest(xmlMessage);
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
        }
        writeLog(id, data_out, data_in, error);
        if (!success) {
            throw new Exception(error);
        }
        writeVoid(((Double) data.get("amount")).doubleValue());
        return retval;
    }

    private String getYear(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        st.nextToken();
        String year = st.nextToken();
        if (year.length() == 2) {
            year = year + "20" + year;
        }
        return year;
    }

    private String getMonth(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        return st.nextToken();
    }

    protected String prepareRequest(ArrayList req) {
        if (req.isEmpty()) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        StringBuffer buf1 = new StringBuffer();
        Iterator i = req.iterator();
        while (i.hasNext()) {
            String str = (String) i.next();
            buf.append(URLEncoder.encode(str));
            buf1.append(str);
        }
        return buf.toString();
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap(2);
        Authenticator.setDefault(new HTTPAuth(this.userName, this.passWord));
        URL post = new URL("https", this.server, this.port, this.path);
        HttpURLConnection postConn = (HttpURLConnection) post.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setRequestProperty("Host", post.getHost());
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
        Document doc = parseXML(response);
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("lastEvent");
        if (list.getLength() == 0) {
            NodeList errlist = root.getElementsByTagName("error");
            StringBuffer errstr = new StringBuffer();
            errstr.append("Error ");
            for (int i = 0; i < errlist.getLength(); i++) {
                errstr.append("\n code:");
                Node errnode = errlist.item(i);
                errstr.append(errnode.getAttributes().getNamedItem("code").getNodeValue());
                errstr.append(" ");
                errstr.append(errnode.getFirstChild().getNodeValue());
            }
            throw new Exception(errstr.toString());
        }
        Node node = list.item(0);
        result.put("response", node.getFirstChild().getNodeValue());
        NodeList stlist = root.getElementsByTagName("orderStatus");
        if (stlist.getLength() != 0) {
            Node stnode = stlist.item(0);
            result.put("id", stnode.getAttributes().getNamedItem("orderCode").getNodeValue());
        }
        return result;
    }

    public String getTransID(long id) {
        return id + "_" + TimeUtils.currentTimeMillis();
    }

    private Document parseXML(String xml) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(xml)));
        return parser.getDocument();
    }

    private String getType(String type) throws Exception {
        String t = (String) ccTypes.get(type);
        return t == null ? type : t;
    }

    protected int getPrecision(String currency) {
        return ISOCodes.getPrecisionByISOShortName(currency);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public String formatAmount(double amount) {
        return Long.toString(Math.round(amount * Math.pow(10.0d, this.precision)));
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Bibit PaymentService (www.bibit.com)";
    }
}
