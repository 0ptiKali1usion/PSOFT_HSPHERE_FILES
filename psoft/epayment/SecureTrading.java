package psoft.epayment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/SecureTrading.class */
public class SecureTrading extends GenericMerchantGateway {
    private Socket socket;
    private InputStreamReader reader;
    private BufferedReader bufferedReader;
    private OutputStreamWriter writer;
    private BufferedWriter bufferedWriter;
    private String certFile;
    private String currency;
    private String siteReference;
    MediaDomainXMLParser mediaDomainParser;
    private String email;
    private static final SimpleDateFormat dateExp = new SimpleDateFormat("MM/yy");
    private static HashMap ccTypes = new HashMap();

    static {
        ccTypes.put("MC", "MASTERCARD");
        ccTypes.put("AX", "AMEX");
        ccTypes.put("VISA", "VISA");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.certFile = getValue(v, "CERT_FILE");
        this.currency = getValue(v, "CURRCODE");
        this.siteReference = getValue(v, "SITEREFERENCE");
        this.email = getValue(v, "NOTIFICATIONEMAIL");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("CERT_FILE", this.certFile);
        map.put("CURRCODE", this.currency);
        map.put("SITEREFERENCE", this.siteReference);
        map.put("NOTIFICATIONEMAIL", this.email);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap();
        boolean success = true;
        String xmlRes = "";
        String error = "";
        HashMap request = new HashMap();
        try {
            request = getRequestData(1, cc, amount);
            String xml = prepareData(request, 1).toString();
            HashMap data = postForm(xml);
            String res = (String) data.get("Result");
            if (!"1".equals(res)) {
                success = false;
                error = (String) data.get("Message");
            }
            retval.put("amount", new Double(amount));
            retval.put("TransactionReference", data.get("TransactionReference"));
            retval.put("TransactionVerifier", data.get("TransactionVerifier"));
            xmlRes = (String) data.get("response");
        } catch (Exception e) {
            success = false;
            if ("".equals(error)) {
                error = e.toString();
            }
            Session.getLog().debug("Problem with the transaction: " + error, e);
            e.printStackTrace();
        }
        writeLog(id, request.toString(), xmlRes, error);
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap respdata, CreditCard cc) throws Exception {
        double amount = ((Double) respdata.get("amount")).doubleValue();
        HashMap request = getRequestData(4, cc, amount, respdata);
        writeLog(id, request.toString(), "This transaction should be settled manually", "");
        sendEmail(this.email, 4, description, id, "TransactionReference:" + respdata.get("TransactionReference") + "\nTransactionVerifier" + respdata.get("TransactionVerifier"));
        writeCapture(amount);
        return new HashMap();
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap();
        boolean success = true;
        String xmlRes = "";
        String error = "";
        HashMap request = new HashMap();
        try {
            request = getRequestData(0, cc, amount);
            String xml = prepareData(request, 0).toString();
            HashMap data = postForm(xml);
            String res = (String) data.get("Result");
            if (!"1".equals(res)) {
                success = false;
                error = (String) data.get("Message");
            }
            retval.put("amount", new Double(amount));
            retval.put("TransactionReference", data.get("TransactionReference"));
            retval.put("TransactionVerifier", data.get("TransactionVerifier"));
            xmlRes = (String) data.get("response");
        } catch (Exception e) {
            success = false;
            if ("".equals(error)) {
                error = e.toString();
            }
            Session.getLog().debug("Problem with the transaction: " + error, e);
            e.printStackTrace();
        }
        writeLog(id, request.toString(), xmlRes, error);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap respdata, CreditCard cc) throws Exception {
        double amount = ((Double) respdata.get("amount")).doubleValue();
        HashMap request = getRequestData(4, cc, amount, respdata);
        writeLog(id, request.toString(), "This transaction should be voided manually", "");
        sendEmail(this.email, 3, description, id, "TransactionReference:" + respdata.get("TransactionReference") + "\nTransactionVerifier" + respdata.get("TransactionVerifier"));
        writeVoid(amount);
        return new HashMap();
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "XPay client.";
    }

    private StringBuffer operation(HashMap reqdata, int trType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<Operation>\n");
        xml.append("<SiteReference>" + this.siteReference + "</SiteReference>\n");
        if (trType == 1) {
            xml.append("<Currency>" + reqdata.get("Currency") + "</Currency>\n");
            xml.append("<Amount>" + reqdata.get("Amount") + "</Amount>\n");
            xml.append("<SettlementDay>0</SettlementDay>\n");
        } else if (trType == 0) {
            xml.append("<Currency>" + reqdata.get("Currency") + "</Currency>\n");
            xml.append("<Amount>" + reqdata.get("Amount") + "</Amount>\n");
            xml.append("<SettlementDay>1</SettlementDay>\n");
        } else if (trType == 4) {
            xml.append("<TransactionReference>" + reqdata.get("TransactionReference") + "</TransactionReference>\n");
            xml.append("<SettleDate>NEXT</SettleDate>\n");
            xml.append("<SettleStatus>0</SettleStatus>\n");
            xml.append("<SettleAmount>" + reqdata.get("Amount") + "</SettleAmount>\n");
        }
        xml.append("</Operation>\n");
        return xml;
    }

    private StringBuffer customerInfo(HashMap reqdata, int trType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<CustomerInfo>\n");
        xml.append("<Postal>\n");
        xml.append("<Name>\n");
        xml.append("<FirstName>" + reqdata.get("FirstName") + "</FirstName>\n");
        xml.append("<MiddleName></MiddleName>\n");
        xml.append("<LastName>" + reqdata.get("LastName") + "</LastName>\n");
        xml.append("<NameSuffix></NameSuffix>\n");
        xml.append("</Name>\n");
        xml.append("<Company></Company>\n");
        xml.append("<Street>" + reqdata.get("Street") + "</Street>\n");
        xml.append("<City>" + reqdata.get("City") + "</City>\n");
        xml.append("<StateProv>" + reqdata.get("StateProv") + "</StateProv>\n");
        xml.append("<PostalCode>" + reqdata.get("PostalCode") + "</PostalCode>\n");
        xml.append("<CountryCode>" + reqdata.get("CountryCode") + "</CountryCode>\n");
        xml.append("</Postal>\n");
        xml.append("<Telecom>\n");
        xml.append("<Phone>" + reqdata.get("Phone") + "</Phone>\n");
        xml.append("</Telecom>\n");
        xml.append("<Online>\n");
        xml.append("<Email>" + reqdata.get("Email") + "</Email>\n");
        xml.append("</Online>\n");
        xml.append("</CustomerInfo>\n");
        return xml;
    }

    private StringBuffer paymentMethod(HashMap reqdata, int trType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<PaymentMethod>\n");
        xml.append("<CreditCard>\n");
        if (trType == 1 || trType == 0) {
            xml.append("<Type>" + reqdata.get("Type") + "</Type>\n");
            xml.append("<Number>" + reqdata.get("Number") + "</Number>\n");
            xml.append("<Issue></Issue>\n");
            xml.append("<StartDate></StartDate>\n");
            xml.append("<ExpiryDate>" + reqdata.get("ExpiryDate") + "</ExpiryDate>\n");
        } else if (trType == 3) {
            xml.append("<TransactionVerifier>" + reqdata.get("TransactionVerifier") + "</TransactionVerifier>");
            xml.append("<ParentTransactionReference>" + reqdata.get("TransactionReference") + "</ParentTransactionReference>");
        }
        xml.append("</CreditCard>\n");
        xml.append("</PaymentMethod>\n");
        return xml;
    }

    private StringBuffer orderInfo(HashMap reqdata, int trType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<Order>\n");
        xml.append("<OrderReference></OrderReference>\n");
        xml.append("<OrderInformation></OrderInformation>\n");
        xml.append("</Order>\n");
        return xml;
    }

    private StringBuffer optional(HashMap reqdata, int trType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<Optional>\n");
        xml.append("</Optional>\n");
        return xml;
    }

    private HashMap getRequestData(int trType, CreditCard cc, double amount) throws Exception {
        return getRequestData(trType, cc, amount, new HashMap());
    }

    private HashMap getRequestData(int trType, CreditCard cc, double amount, HashMap data) throws Exception {
        HashMap reqdata = new HashMap();
        int precision = ISOCodes.getPrecisionByISOCode(this.currency);
        reqdata.put("Currency", ISOCodes.getShortNameByISO(this.currency));
        reqdata.put("Amount", Long.toString(Math.round(amount * Math.pow(10.0d, precision))));
        reqdata.put("FirstName", cc.getFirstName());
        reqdata.put("LastName", cc.getLastName());
        reqdata.put("Street", cc.getAddress());
        reqdata.put("City", cc.getCity());
        reqdata.put("StateProv", cc.getState());
        reqdata.put("PostalCode", cc.getZip());
        reqdata.put("CountryCode", cc.getCountry());
        reqdata.put("Phone", cc.getPhone());
        reqdata.put("Email", cc.getEmail());
        reqdata.put("Type", getType(cc.getType()));
        reqdata.put("Number", cc.getNumber());
        reqdata.put("ExpiryDate", cc.getExp(dateExp));
        reqdata.put("CertificatPath", this.certFile);
        if (trType == 4 || trType == 3) {
            reqdata.put("TransactionReference", data.get("TransactionReference"));
            reqdata.put("TransactionVerifier", data.get("TransactionVerifier"));
        }
        return reqdata;
    }

    private StringBuffer prepareData(HashMap reqdata, int trType) throws Exception {
        StringBuffer xml = new StringBuffer();
        xml.append("<RequestBlock Version=\"3.7\">\n");
        if (trType == 1 || trType == 0) {
            xml.append("<Request Type=\"AUTH\">\n");
        } else if (trType == 3) {
            xml.append("<Request Type=\"AUTHREVERSAL\">\n");
        } else if (trType == 4) {
            xml.append("<Request Type=\"SETTLEMENT\">\n");
        }
        xml.append(operation(reqdata, trType));
        if (trType == 1 || trType == 0 || trType == 3) {
            xml.append(customerInfo(reqdata, trType));
            xml.append(paymentMethod(reqdata, trType));
            xml.append(orderInfo(reqdata, trType));
        }
        xml.append(optional(reqdata, trType));
        xml.append("</Request>\n");
        xml.append("<Certificate>");
        xml.append(readCertificateFile((String) reqdata.get("CertificatPath")));
        xml.append("</Certificate>\n");
        xml.append("</RequestBlock>\n");
        return xml;
    }

    private HashMap postForm(String xml) throws Exception {
        HashMap data = new HashMap();
        try {
            this.socket = new Socket("127.0.0.1", 5000);
            this.reader = new InputStreamReader(this.socket.getInputStream());
            this.bufferedReader = new BufferedReader(this.reader);
            this.writer = new OutputStreamWriter(this.socket.getOutputStream());
            this.bufferedWriter = new BufferedWriter(this.writer);
            this.bufferedWriter.write(xml);
            this.bufferedWriter.flush();
            StringBuffer buffer = new StringBuffer("<?xml version=\"1.0\"?>\n");
            this.bufferedReader.readLine();
            while (true) {
                String inputLine = this.bufferedReader.readLine();
                if (inputLine == null) {
                    break;
                }
                buffer.append(inputLine + "\n");
            }
            this.mediaDomainParser = new MediaDomainXMLParser(data);
            data.put("response", buffer.toString());
            HashMap data2 = this.mediaDomainParser.processXMLResponse(buffer.toString());
            if (this.socket != null) {
                this.socket.close();
            }
            if (this.bufferedReader != null) {
                this.bufferedReader.close();
            }
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            return data2;
        } catch (Throwable th) {
            if (this.socket != null) {
                this.socket.close();
            }
            if (this.bufferedReader != null) {
                this.bufferedReader.close();
            }
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
            throw th;
        }
    }

    private String getType(String type) throws Exception {
        String t = (String) ccTypes.get(type);
        return t == null ? type : t;
    }

    protected String readCertificateFile(String fileName) throws Exception {
        new Properties();
        StringBuffer certificateString = new StringBuffer();
        try {
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            while (input.ready()) {
                certificateString.append((char) input.read());
            }
            return certificateString.toString();
        } catch (IOException ioe) {
            throw new Exception("I/O Exception caught while trying to read the properties file: " + ioe);
        }
    }

    /* loaded from: hsphere.zip:psoft/epayment/SecureTrading$MediaDomainXMLParser.class */
    public class MediaDomainXMLParser {
        private Document document;
        private HashMap map;
        String nodeName;
        String nodeValue;

        public MediaDomainXMLParser(HashMap map) {
            SecureTrading.this = r4;
            this.map = map;
        }

        public HashMap processXMLResponse(String xmlResponse) throws Exception {
            Reader reader = new StringReader(xmlResponse);
            InputSource is = new InputSource(reader);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.document = builder.parse(is);
            Element root = this.document.getDocumentElement();
            root.getChildNodes();
            stepThrough(root);
            return this.map;
        }

        private void stepThrough(Node start) {
            if (!start.getNodeName().equals("#text")) {
                this.nodeName = start.getNodeName();
            }
            this.nodeValue = start.getNodeValue();
            if (this.nodeValue == null) {
                this.nodeValue = "";
            }
            if (!this.nodeValue.trim().equals("")) {
                this.map.put(this.nodeName, this.nodeValue);
            }
            if (start.getNodeType() == 1) {
                NamedNodeMap startAttribute = start.getAttributes();
                for (int i = 0; i < startAttribute.getLength(); i++) {
                    Node attribute = startAttribute.item(i);
                    this.nodeName = attribute.getNodeName();
                    this.nodeValue = attribute.getNodeValue();
                    if (this.nodeValue != null) {
                        this.map.put(this.nodeName, this.nodeValue);
                    }
                }
            }
            Node firstChild = start.getFirstChild();
            while (true) {
                Node child = firstChild;
                if (child != null) {
                    stepThrough(child);
                    firstChild = child.getNextSibling();
                } else {
                    return;
                }
            }
        }
    }
}
