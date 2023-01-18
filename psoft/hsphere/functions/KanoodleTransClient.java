package psoft.hsphere.functions;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/functions/KanoodleTransClient.class */
public class KanoodleTransClient {
    protected String server;
    protected String path;
    protected int port;
    protected String partnerId;

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    public KanoodleTransClient(String server, int port, String path, String partnerId) {
        this.port = port;
        this.server = server;
        this.path = path;
        this.partnerId = partnerId;
    }

    private String createXML(HashMap data, boolean isPartner) {
        ArrayList xml = new ArrayList();
        xml.add("<?xml version='1.0'?>");
        xml.add("<!DOCTYPE SaveAdvertiser [ <!ELEMENT Id (#PCDATA)>  <!ELEMENT Password(#PCDATA)> <!ELEMENT FirstName (#PCDATA)> <!ELEMENT LastName (#PCDATA)> <!ELEMENT CompanyName (#PCDATA)> <!ELEMENT Addr1 (#PCDATA)> <!ELEMENT Addr2 (#PCDATA)> <!ELEMENT City (#PCDATA)> <!ELEMENT Province (#PCDATA)> <!ELEMENT PZCode (#PCDATA)> <!ELEMENT Country (#PCDATA)> <!ELEMENT Email (#PCDATA)> <!ELEMENT PhoneNo (#PCDATA)> <!ELEMENT Spam (#PCDATA)> <!ELEMENT BillFirstName (#PCDATA)> <!ELEMENT BillLastName (#PCDATA)> <!ELEMENT BillCompanyName (#PCDATA)> <!ELEMENT BillAddr1 (#PCDATA)> <!ELEMENT BillAddr2 (#PCDATA)> <!ELEMENT BillCity (#PCDATA)> <!ELEMENT BillProvince (#PCDATA)> <!ELEMENT BillPZCode (#PCDATA)> <!ELEMENT BillCountry (#PCDATA)> <!ELEMENT BillPhoneNo (#PCDATA)> <!ELEMENT Url (#PCDATA)> <!ELEMENT Package (#PCDATA)> <!ELEMENT Existing (#PCDATA)> <!ELEMENT FullText (#PCDATA)> <!ELEMENT CreateTransformPartner (#PCDATA)> ]>");
        xml.add("<SaveAdvertiser>");
        xml.add("<Id>" + data.get("Id") + "</Id>");
        xml.add("<Password>" + data.get("Password") + "</Password>");
        xml.add("<FirstName>" + data.get("FirstName") + "</FirstName>");
        xml.add("<LastName>" + data.get("LastName") + "</LastName>");
        xml.add("<CompanyName>" + data.get("CompanyName") + "</CompanyName>");
        xml.add("<Addr1>" + data.get("Addr1") + "</Addr1>");
        xml.add("<Addr2>" + data.get("Addr2") + "</Addr2>");
        xml.add("<City>" + data.get("City") + "</City>");
        xml.add("<Province>" + data.get("Province") + "</Province>");
        xml.add("<PZCode>" + data.get("PZCode") + "</PZCode>");
        xml.add("<Country>" + data.get("Country") + "</Country>");
        xml.add("<Email>" + data.get("Email") + "</Email>");
        xml.add("<PhoneNo>" + data.get("PhoneNo") + "</PhoneNo>");
        xml.add("<Spam>" + data.get("Spam") + "</Spam>");
        xml.add("<BillFirstName>" + data.get("BillFirstName") + "</BillFirstName>");
        xml.add("<BillLastName>" + data.get("BillLastName") + "</BillLastName>");
        xml.add("<BillCompanyName>" + data.get("BillCompanyName") + "</BillCompanyName>");
        xml.add("<BillAddr1>" + data.get("BillAddr1") + "</BillAddr1>");
        xml.add("<BillAddr2>" + data.get("BillAddr2") + "</BillAddr2>");
        xml.add("<BillCity>" + data.get("BillCity") + "</BillCity>");
        xml.add("<BillProvince>" + data.get("BillProvince") + "</BillProvince>");
        xml.add("<BillPZCode>" + data.get("BillPZCode") + "</BillPZCode>");
        xml.add("<BillCountry>" + data.get("BillCountry") + "</BillCountry>");
        xml.add("<BillPhoneNo>" + data.get("BillPhoneNo") + "</BillPhoneNo>");
        xml.add("<Url>" + data.get("Url") + "</Url>");
        xml.add("<Package>" + data.get("Package") + "</Package>");
        xml.add("<Existing>" + data.get("Existing") + "</Existing>");
        xml.add("<FullText>" + data.get("FullText") + "</FullText>");
        if (isPartner) {
            xml.add("<CreateTransformPartner>1</CreateTransformPartner>");
        } else {
            xml.add("<CreateTransformPartner>0</CreateTransformPartner>");
        }
        xml.add("</SaveAdvertiser>");
        StringBuffer buf = new StringBuffer();
        StringBuffer buf1 = new StringBuffer();
        Iterator i = xml.iterator();
        while (i.hasNext()) {
            String str = (String) i.next();
            buf.append(URLEncoder.encode(str));
            buf1.append(str);
        }
        Session.getLog().debug("kanoodle xml request: " + buf1.toString());
        return buf.toString();
    }

    public HashMap createNewPartner(HashMap data) throws Exception {
        String xml = createXML(data, true);
        HashMap results = postForm(xml);
        return results;
    }

    public HashMap createNewAdvertiser(HashMap data) throws Exception {
        String xml = createXML(data, false);
        HashMap results = postForm(xml);
        return results;
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap();
        StringBuffer buf = new StringBuffer();
        URL post = new URL("https", this.server, this.port, this.path);
        HttpURLConnection postConn = (HttpURLConnection) post.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setDoOutput(true);
        Session.getLog().debug("url: " + post.toString());
        try {
            PrintWriter out = new PrintWriter(postConn.getOutputStream());
            String query = ("partnerid=" + this.partnerId) + "&xml=" + data;
            Session.getLog().debug("Query: " + query);
            out.println(query);
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(postConn.getInputStream()));
            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                buf.append(inputLine);
            }
            in.close();
            String response = buf.toString();
            Session.getLog().debug("kanoodle xml response: " + response);
            try {
                DOMParser parser = new DOMParser();
                parser.parse(new InputSource(new StringReader(response)));
                Document doc = parser.getDocument();
                Node element = doc.getDocumentElement();
                NodeList list = element.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    Element item = (Element) list.item(i);
                    String itemVal = "";
                    if (item.getChildNodes().getLength() > 0) {
                        itemVal = item.getFirstChild().getNodeValue();
                    }
                    result.put(item.getNodeName(), itemVal);
                }
                if (result.size() == 0) {
                    Session.getLog().debug("Incorrect response has been received from kanoodle: " + response);
                    throw new HSUserException("Incorrect response has been received from kanoodle");
                }
                return result;
            } catch (SAXException ex) {
                Session.getLog().debug("Bad response received: ", ex);
                throw new HSUserException("Bad response received: " + ex.getMessage() + "" + response);
            }
        } catch (IOException ex2) {
            Session.getLog().debug("Connection error: ", ex2);
            throw new HSUserException("Connection error: " + ex2.getMessage());
        }
    }
}
