package psoft.hsphere.util;

import java.util.Hashtable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/* loaded from: hsphere.zip:psoft/hsphere/util/XMLHashtableTranslator.class */
public class XMLHashtableTranslator {
    private String xmlFileKey;
    private Document document = null;

    public XMLHashtableTranslator(String xmlFileKey) {
        this.xmlFileKey = null;
        this.xmlFileKey = xmlFileKey;
    }

    public Hashtable translate(String elementNameTag, String keyTag, String valueTag) throws Exception {
        Hashtable result = new Hashtable();
        if (this.document == null) {
            this.document = XMLManager.getXML(this.xmlFileKey);
        }
        Element root = this.document.getDocumentElement();
        NodeList list = root.getElementsByTagName(elementNameTag);
        for (int i = 0; i < list.getLength(); i++) {
            Element oneNode = (Element) list.item(i);
            String key = oneNode.getAttribute(keyTag);
            String value = oneNode.getAttribute(valueTag);
            result.put(key, value);
        }
        return result;
    }
}
