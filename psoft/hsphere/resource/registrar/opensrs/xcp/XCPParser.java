package psoft.hsphere.resource.registrar.opensrs.xcp;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/XCPParser.class */
public class XCPParser {
    protected static DocumentBuilder dbuilder;
    protected Document doc;
    protected String str;

    static {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbuilder = dbf.newDocumentBuilder();
            dbuilder.setEntityResolver(new XCPResolver());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return this.str;
    }

    public XCPParser(String str) throws Exception {
        this.str = str;
        this.doc = dbuilder.parse(new XCPInputSource(str));
        XPathAPI.selectSingleNode(this.doc, "/OPS_envelope/header/version/text()");
    }

    public static XCPParser getInstance(String str) throws Exception {
        return new XCPParser(str);
    }

    public String getResponseText() throws TransformerException {
        return get("/item[@key=\"response_text\"]");
    }

    public boolean isSuccess() throws TransformerException {
        return "1".equals(get("/item[@key=\"is_success\"]"));
    }

    public int getResponseCode() throws TransformerException, NumberFormatException {
        return Integer.parseInt(get("/item[@key=\"response_code\"]"));
    }

    public String getAttribute(String key) throws TransformerException {
        return get("/item[@key=\"attributes\"]/dt_assoc/item[@key=\"" + key + "\"]");
    }

    public String get(String path) throws TransformerException {
        Node n = XPathAPI.selectSingleNode(this.doc, "/OPS_envelope/body/data_block/dt_assoc" + path + "/text()");
        return n.getNodeValue();
    }
}
