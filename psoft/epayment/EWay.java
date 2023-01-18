package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/EWay.class */
public class EWay extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String customerID;
    protected String adminEmail;
    protected static int precision;
    private static final SimpleDateFormat expMonth;
    private static final SimpleDateFormat expYear;

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        precision = ISOCodes.getPrecisionByISOShortName("USD");
        expMonth = new SimpleDateFormat("MM");
        expYear = new SimpleDateFormat("yy");
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
        HashMap retval = new HashMap(1);
        String error = "";
        String data_in = "";
        boolean success = true;
        String data_out = prepareRequest(description, amount, cc);
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("Response");
            if (!"True".equals(result.get("ewayTrxnStatus"))) {
                success = false;
                error = (String) result.get("ewayTrxnError");
            } else {
                retval.put("id", result.get("ewayTrxnNumber"));
                retval.put("amount", new Double(amount));
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
            Session.getLog().error("Problem with the CC charge: ", e);
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().error("Problem with the CC charge: ", e2);
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
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap(1);
        String error = "";
        String data_in = "";
        boolean success = true;
        String data_out = prepareRequest(description, amount, cc);
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("Response");
            if (!"True".equals(result.get("ewayTrxnStatus"))) {
                success = false;
                error = (String) result.get("ewayTrxnError");
            } else {
                retval.put("id", result.get("ewayTrxnNumber"));
                retval.put("amount", new Double(amount));
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
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        retval.put("id", data.get("id"));
        writeCapture(((Double) data.get("amount")).doubleValue());
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        writeLog(id, "", "This transaction should be voided manually", "");
        sendEmail(this.adminEmail, 3, description, id, "TransactionID:" + data.get("id"));
        writeVoid(amount);
        return new HashMap();
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public String formatAmount(double amount) {
        return Long.toString(Math.round(amount * Math.pow(10.0d, precision)));
    }

    protected String prepareRequest(String description, double amount, CreditCard cc) throws Exception {
        ArrayList xmlMessage = new ArrayList();
        xmlMessage.add("<ewaygateway>");
        xmlMessage.add("    <ewayCustomerID>" + this.customerID + "</ewayCustomerID>");
        xmlMessage.add("    <ewayTotalAmount>" + formatAmount(amount) + "</ewayTotalAmount>");
        xmlMessage.add("    <ewayCustomerFirstName>" + cc.getFirstName() + "</ewayCustomerFirstName>");
        xmlMessage.add("    <ewayCustomerLastName>" + cc.getLastName() + "</ewayCustomerLastName>");
        xmlMessage.add("    <ewayCustomerEmail>" + cc.getEmail() + "</ewayCustomerEmail>");
        xmlMessage.add("    <ewayCustomerAddress>" + cc.getAddress() + "</ewayCustomerAddress>");
        xmlMessage.add("    <ewayCustomerPostcode>" + cc.getZip() + "</ewayCustomerPostcode>");
        xmlMessage.add("    <ewayCustomerInvoiceDescription>" + description + "</ewayCustomerInvoiceDescription>");
        xmlMessage.add("    <ewayCustomerInvoiceRef></ewayCustomerInvoiceRef>");
        xmlMessage.add("    <ewayCardHoldersName>" + cc.getName() + "</ewayCardHoldersName>");
        xmlMessage.add("    <ewayCardNumber>" + cc.getNumber() + "</ewayCardNumber>");
        xmlMessage.add("    <ewayCardExpiryMonth>" + cc.getExp(expMonth) + "</ewayCardExpiryMonth>");
        xmlMessage.add("    <ewayCardExpiryYear>" + cc.getExp(expYear) + "</ewayCardExpiryYear>");
        xmlMessage.add("    <ewayTrxnNumber></ewayTrxnNumber>");
        xmlMessage.add("    <ewayOption1>0</ewayOption1>");
        xmlMessage.add("    <ewayOption2>0</ewayOption2>");
        xmlMessage.add("    <ewayOption3>0</ewayOption3>");
        xmlMessage.add("</ewaygateway>");
        StringBuffer buf = new StringBuffer();
        StringBuffer buf1 = new StringBuffer();
        Iterator i = xmlMessage.iterator();
        while (i.hasNext()) {
            String str = (String) i.next();
            buf.append(str);
            buf1.append(str);
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
        Session.getLog().debug("xml response: " + response);
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new StringReader(response)));
            Document doc = parser.getDocument();
            Node element = doc.getDocumentElement();
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node item = list.item(i);
                String itemVal = "";
                if (item.getChildNodes().getLength() > 0) {
                    itemVal = item.getFirstChild().getNodeValue();
                }
                result.put(item.getNodeName(), itemVal);
            }
        } catch (SAXException ex) {
            Session.getLog().debug("Bad response received: ", ex);
            result.put("ewayTrxnError", ex.getMessage());
        }
        result.put("Response", response);
        return result;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "eWay PaymentService (www.eway.com.au)";
    }
}
