package psoft.hsphere.migrator.extractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/InfoExtractorUtils.class */
public class InfoExtractorUtils {
    private Document document;

    public InfoExtractorUtils(Document doc) {
        this.document = doc;
    }

    private StringBuffer createBuffer(char[] symbols, int encodedIndex) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(new String(symbols, 0, encodedIndex));
        return buffer;
    }

    private String encode(String input) {
        StringBuffer buffer = null;
        char[] symbols = input.toCharArray();
        for (int i = 0; i < symbols.length; i++) {
            switch (symbols[i]) {
                case '\n':
                case '\r':
                    if (buffer == null) {
                        buffer = createBuffer(symbols, i);
                    }
                    buffer.append("&#");
                    buffer.append(Integer.toString(symbols[i]));
                    buffer.append(';');
                    break;
                default:
                    if (buffer != null) {
                        buffer.append(symbols[i]);
                        break;
                    } else {
                        break;
                    }
            }
        }
        return buffer != null ? buffer.toString() : input;
    }

    private String getEncodedValue(String value) {
        String value2;
        if (value == null) {
            value2 = "";
        } else {
            value2 = encode(value);
        }
        return value2;
    }

    public Element createNode(String name) {
        Element node = this.document.createElement(name);
        return node;
    }

    public Element createNode(String name, String value) {
        Element node = this.document.createElement(name);
        node.appendChild(this.document.createTextNode(getEncodedValue(value)));
        return node;
    }

    public Attr createAttribute(String name, String value) {
        Attr attribute = this.document.createAttribute(name);
        attribute.setValue(getEncodedValue(value));
        return attribute;
    }

    public void createAttribute(Element parent, String name, String value) {
        Attr attribute = this.document.createAttribute(name);
        attribute.setValue(getEncodedValue(value));
        parent.setAttributeNode(attribute);
    }

    public void createNotemptyAttribute(Element parent, String name, String value) {
        Attr attribute = this.document.createAttribute(name);
        attribute.setValue(getEncodedValue(value));
        if (!"".equals(value) && value != null) {
            parent.setAttributeNode(attribute);
        }
    }

    public Attr createBoolAttr(String name, boolean value) {
        Attr attribute = this.document.createAttribute(name);
        if (value) {
            attribute.setValue("1");
        } else {
            attribute.setValue("0");
        }
        return attribute;
    }

    public Attr createBoolAttr(Element parent, String name, boolean value) {
        return parent.setAttributeNode(createBoolAttr(name, value));
    }

    public boolean getBoolAttrValue(Element parent, String name) {
        Attr attr = parent.getAttributeNode(name);
        if ("1".equals(attr.getValue())) {
            return true;
        }
        return false;
    }

    public Element checkChildren(Element parent) throws Exception {
        if (parent == null) {
            return null;
        }
        if (parent.getChildNodes().getLength() == 0 && parent.getAttributes().getLength() == 0) {
            return null;
        }
        return parent;
    }

    public Node appendChildNode(Element parent, Element child) {
        if (child != null) {
            parent.appendChild(child);
        }
        return parent;
    }

    public Node setNodeValue(Element parent, String value) {
        if (value != null) {
            parent.setNodeValue(value);
        }
        return parent;
    }

    public Document getDocument() {
        return this.document;
    }

    public NodeList getNodeList(Node current, String expr) throws Exception {
        return XPathAPI.selectNodeList(current, expr);
    }

    public String getAttributeValue(Node current, String expr) throws Exception {
        Node node = XPathAPI.selectSingleNode(current, "@" + expr);
        if (node != null) {
            return node.getNodeValue();
        }
        return null;
    }

    public static Document createDocument(String root, String dtd) throws Exception {
        DocumentType theDocType = new DocumentImpl().createDocumentType(root, (String) null, dtd);
        DocumentImpl impl = new DocumentImpl(theDocType);
        return impl;
    }

    public static InputSource getInputSource(String xml) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
        return new InputSource(stream);
    }

    public static Document getDocumentFromXMLString(String xml) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
        InputSource source = new InputSource(stream);
        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setErrorHandler(new DefaultHandler() { // from class: psoft.hsphere.migrator.extractor.InfoExtractorUtils.1
            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void warning(SAXParseException e) {
            }
        });
        parser.parse(source);
        return parser.getDocument();
    }

    public static Document getDocumentFromFile(String file) throws Exception {
        InputSource source = new InputSource(new FileInputStream(file));
        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setErrorHandler(new DefaultHandler() { // from class: psoft.hsphere.migrator.extractor.InfoExtractorUtils.2
            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
            public void warning(SAXParseException e) {
            }
        });
        parser.parse(source);
        return parser.getDocument();
    }

    public static void serializeDocument(Document doc, String file) throws Exception {
        OutputFormat format = new OutputFormat(doc, "utf-8", true);
        FileOutputStream output = new FileOutputStream(file);
        XMLSerializer serial = new XMLSerializer(output, format);
        serial.serialize(doc);
    }

    public static ByteArrayOutputStream serializeDocument(Document doc) throws Exception {
        OutputFormat format = new OutputFormat(doc, "utf-8", true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLSerializer serial = new XMLSerializer(output, format);
        serial.serialize(doc);
        return output;
    }

    public static void validateDocument(String file, boolean validate) {
        try {
            InputSource input = new InputSource(new FileInputStream(file));
            DOMParser parser = new DOMParser();
            if (validate) {
                parser.setFeature("http://xml.org/sax/features/validation", true);
                parser.setErrorHandler(new DefaultHandler() { // from class: psoft.hsphere.migrator.extractor.InfoExtractorUtils.3
                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void error(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void fatalError(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void warning(SAXParseException e) {
                    }
                });
            }
            parser.parse(input);
            System.out.print("Document XML syntax is valid\n");
        } catch (Exception exc) {
            System.out.print("Invalid XML syntax\n" + exc + "\n");
            exc.printStackTrace();
        }
    }
}
