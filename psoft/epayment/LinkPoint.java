package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.IOException;
import java.io.StringReader;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import lp.txn.JLinkPointTransaction;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/LinkPoint.class */
public class LinkPoint extends GenericMerchantGateway {
    protected String accountType;
    protected String transitRouting;
    protected String micr;
    protected String checkNumber;
    protected String configfile;
    protected String certpath;
    protected String passwd;
    protected String host;
    protected int port;
    protected String testmode;
    protected String avsLevel;
    private static final SimpleDateFormat expMonth = new SimpleDateFormat("MM");
    private static final SimpleDateFormat expYear = new SimpleDateFormat("yy");

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.configfile = getValue(v, "CONFIG_FILE");
        this.certpath = getValue(v, "CERT_PATH");
        this.host = getValue(v, "SERVER");
        String sport = getValue(v, "PORT");
        this.port = Integer.parseInt(sport);
        this.testmode = getValue(v, "MODE");
        this.avsLevel = getValue(v, "AVS");
        this.passwd = getValue(v, "PASSWD");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("CONFIG_FILE", this.configfile);
        map.put("CERT_PATH", this.certpath);
        map.put("SERVER", this.host);
        map.put("PORT", Integer.toString(this.port));
        map.put("AVS", this.avsLevel);
        map.put("MODE", this.testmode);
        map.put("PASSWD", this.passwd);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 0);
        String request = prepareRequest(0, id, description, cc, amount, new HashMap());
        HashMap retval = new HashMap();
        String error = "";
        String response = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            response = (String) result.get("response");
            retval.put("id", result.get("r_ordernum"));
            retval.put("amount", new Double(amount));
            if (!"APPROVED".equals(result.get("r_approved"))) {
                success = false;
                error = (String) result.get("r_error");
                if (error == null || "".equals(error)) {
                    error = "An incorrect response has been received: " + response;
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
            Session.getLog().debug("Error processing credit card: ", e);
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().debug("Error processing credit card: ", e2);
        }
        writeLog(trId, id, amount, 0, excludeCVV(request), response, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        if (!checkAVS(result)) {
            try {
                voidAuthorize(id, "Voided due to AVS warning", retval, cc);
            } catch (Exception e3) {
                Session.getLog().error("Void error, Transaction ID: " + retval.get("id").toString(), e3);
            }
            throw new Exception("epayment.avsfail");
        }
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 1);
        String request = prepareRequest(1, id, description, cc, amount, new HashMap());
        HashMap retval = new HashMap();
        String error = "";
        String response = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(request);
            response = (String) result.get("response");
            retval.put("id", result.get("r_ordernum"));
            retval.put("amount", new Double(amount));
            if (!"APPROVED".equals(result.get("r_approved"))) {
                success = false;
                error = (String) result.get("r_error");
                if (error == null || "".equals(error)) {
                    error = "An incorrect response has been received: " + response;
                }
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
            Session.getLog().debug("Error processing credit card: ", e);
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().debug("Error processing credit card: ", e2);
        }
        writeLog(trId, id, amount, 1, excludeCVV(request), response, error, success);
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        if (!checkAVS(result)) {
            try {
                voidAuthorize(id, "Voided due to AVS warning", retval, cc);
            } catch (Exception e3) {
                Session.getLog().error("Void error, Transaction ID: " + retval.get("id").toString(), e3);
            }
            throw new Exception("epayment.avsfail");
        }
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 4);
        String request = prepareRequest(4, id, description, cc, amount, data);
        HashMap retval = new HashMap();
        String error = "";
        String response = "";
        boolean success = true;
        try {
            HashMap result = postForm(request);
            response = (String) result.get("response");
            retval.put("id", result.get("r_ordernum"));
            if (!"APPROVED".equals(result.get("r_approved"))) {
                success = false;
                error = (String) result.get("r_error");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
            Session.getLog().debug("Error processing credit card: ", e);
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().debug("Error processing credit card: ", e2);
        }
        writeLog(trId, id, amount, 4, excludeCVV(request), response, error, success);
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
        String request = prepareRequest(3, id, description, cc, amount, data);
        HashMap retval = new HashMap();
        String error = "";
        String response = "";
        boolean success = true;
        try {
            HashMap result = postForm(request);
            response = (String) result.get("response");
            retval.put("id", result.get("r_ordernum"));
            if (!"APPROVED".equals(result.get("r_approved"))) {
                success = false;
                error = (String) result.get("r_error");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
            Session.getLog().debug("Error processing credit card: ", e);
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().debug("Error processing credit card: ", e2);
        }
        writeLog(trId, id, amount, 3, excludeCVV(request), response, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeVoid(amount);
        return retval;
    }

    protected String prepareRequest(int transactionType, long id, String description, CreditCard cc, double amount, HashMap data) throws Exception {
        String str;
        StringBuffer xml = new StringBuffer();
        xml.append("<order>");
        xml.append("<merchantinfo>");
        xml.append("<configfile>" + this.configfile + "</configfile>");
        xml.append("</merchantinfo>");
        xml.append("<orderoptions>");
        switch (transactionType) {
            case 0:
                xml.append("<ordertype>SALE</ordertype>");
                break;
            case 1:
                xml.append("<ordertype>PREAUTH</ordertype>");
                break;
            case 3:
                xml.append("<ordertype>VOID</ordertype>");
                break;
            case 4:
                xml.append("<ordertype>POSTAUTH</ordertype>");
                break;
        }
        xml.append("<result>" + ("TRUE".equals(this.testmode) ? "GOOD" : "LIVE") + "</result>");
        xml.append("</orderoptions>");
        xml.append("<payment>");
        xml.append("<chargetotal>" + formatAmount(amount) + "</chargetotal>");
        xml.append("</payment>");
        xml.append("<creditcard>");
        String number = cc.getNumber();
        String newnumber = "";
        for (int i = 0; i < number.length(); i += 4) {
            if (i != 0) {
                newnumber = newnumber + "-";
            }
            if (number.length() >= i + 4) {
                str = newnumber + number.substring(i, i + 4);
            } else {
                str = newnumber + number.substring(i);
            }
            newnumber = str;
        }
        xml.append("<cardnumber>" + newnumber + "</cardnumber>");
        xml.append("<cardexpmonth>" + cc.getExp(expMonth) + "</cardexpmonth>");
        xml.append("<cardexpyear>" + cc.getExp(expYear) + "</cardexpyear>");
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            xml.append("<cvmvalue>" + cvv + "</cvmvalue>");
        }
        xml.append("</creditcard>");
        if (transactionType == 4 || transactionType == 3) {
            xml.append("<transactiondetails>");
            xml.append("<oid>" + data.get("id") + "</oid>");
            xml.append("</transactiondetails>");
        }
        xml.append("<billing>");
        xml.append("<userid>" + Long.toString(id) + "</userid>");
        xml.append("<zip>" + cc.getZip() + "</zip>");
        xml.append("<name>" + cc.getName() + "</name>");
        xml.append("<address1>" + cc.getAddress() + "</address1>");
        xml.append("<city>" + cc.getCity() + "</city>");
        xml.append("<state>" + cc.getState() + "</state>");
        xml.append("<country>" + cc.getCountry() + "</country>");
        xml.append("<phone>" + cc.getPhone() + "</phone>");
        xml.append("<email>" + cc.getEmail() + "</email>");
        xml.append("</billing>");
        xml.append("</order>");
        return xml.toString();
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap res = new HashMap();
        JLinkPointTransaction txn = new JLinkPointTransaction();
        txn.setClientCertificatePath(this.certpath);
        txn.setPassword(this.passwd);
        txn.setHost(this.host);
        txn.setPort(this.port);
        String sResponse = "<response>" + txn.send(data) + "</response>";
        Session.getLog().debug("xml response: " + sResponse);
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new StringReader(sResponse)));
            Document doc = parser.getDocument();
            Node element = doc.getDocumentElement();
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node item = list.item(i);
                String itemVal = "";
                if (item.getChildNodes().getLength() > 0) {
                    itemVal = item.getFirstChild().getNodeValue();
                }
                res.put(item.getNodeName(), itemVal);
            }
        } catch (SAXException ex) {
            Session.getLog().debug("Bad response received: ", ex);
            res.put("r_error", ex.getMessage());
        }
        res.put("response", sResponse);
        return res;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "LinkPoint API V 3.01 (www.linkpoint.com) ";
    }

    private String excludeCVV(String str) {
        String res = "";
        try {
            int start = str.indexOf("<cvmvalue>");
            int end = str.indexOf("</cvmvalue>");
            if (start >= 0) {
                String res2 = str.substring(0, start + 10);
                res = (res2 + "=****") + str.substring(end);
            } else {
                res = str;
            }
        } catch (Throwable tr) {
            Session.getLog().error("Error with removing cvv: ", tr);
        }
        return res;
    }

    protected boolean checkAVS(HashMap resp) throws Exception {
        String avsRes = "";
        if (resp.containsKey("r_avs")) {
            avsRes = resp.get("r_avs").toString();
        }
        if (avsRes == null || "".equals(avsRes)) {
            avsRes = "XX";
        } else if (avsRes.length() > 2) {
            avsRes = avsRes.substring(0, 2);
        }
        Session.getLog().debug("AVS resp result: " + avsRes);
        switch (this.avsLevel.charAt(0)) {
            case 'F':
                return avsRes.indexOf("YY") >= 0;
            case 'L':
                return avsRes.indexOf("NN") < 0;
            case 'M':
                return avsRes.indexOf(89) >= 0;
            default:
                return true;
        }
    }
}
