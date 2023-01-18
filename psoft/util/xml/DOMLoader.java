package psoft.util.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.apache.log4j.Category;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/* loaded from: hsphere.zip:psoft/util/xml/DOMLoader.class */
public class DOMLoader {
    private static Category log = Category.getInstance(DOMLoader.class.getName());
    private static DocumentBuilder dbuilder;
    private static DOMParser validateParser;

    static {
        dbuilder = null;
        validateParser = null;
        try {
            dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            validateParser = new DOMParser();
            try {
                validateParser.setFeature("http://xml.org/sax/features/validation", true);
                validateParser.setErrorHandler(new DefaultHandler() { // from class: psoft.util.xml.DOMLoader.1
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
            } catch (Exception e) {
                log.info("Parser error : ", e);
            }
        } catch (ParserConfigurationException e2) {
            log.error("Error", e2);
        } catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError) {
            log.warn("Error", transformerFactoryConfigurationError);
        }
    }

    private static Document getXML(InputStream is) throws SAXException, IOException {
        Document parse;
        synchronized (dbuilder) {
            parse = dbuilder.parse(new InputSource(is));
        }
        return parse;
    }

    private static Document getXML(InputStream is, boolean validate) throws SAXException, IOException {
        if (validate) {
            validateParser.parse(new InputSource(is));
            return validateParser.getDocument();
        }
        return getXML(is);
    }

    public static Document getXML(String filename) throws SAXException, IOException {
        return getXML(new FileInputStream(filename));
    }

    public static Document getXML(String filename, boolean validate) throws SAXException, IOException {
        return getXML(new FileInputStream(filename), validate);
    }

    public static Document getXML(String filename1, String filename2) throws Exception {
        return getXML(new String[]{filename1, filename2});
    }

    public static Document getXML(String[] args) throws Exception {
        Document doc = null;
        for (String str : args) {
            Document tmp = getXML(str);
            if (doc == null) {
                doc = tmp;
            } else {
                DOMMerger.merge(doc, tmp);
            }
        }
        return doc;
    }

    public static Document getXML(List l) throws SAXException, IOException {
        Document doc = null;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Document tmp = getXML((String) i.next());
            tmp.normalize();
            if (doc == null) {
                doc = tmp;
            } else {
                DOMMerger.merge(doc, tmp);
            }
        }
        return doc;
    }

    public static Document getXML(List l, boolean validate) throws SAXException, IOException {
        Document doc = null;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Document tmp = getXML((String) i.next(), validate);
            tmp.normalize();
            if (doc == null) {
                doc = tmp;
            } else {
                DOMMerger.merge(doc, tmp);
            }
        }
        return doc;
    }

    public static final void main(String[] args) throws Exception {
        OutputFormat format = new OutputFormat();
        format.setLineSeparator("\n");
        format.setIndenting(true);
        format.setLineWidth(0);
        format.setPreserveSpace(true);
        XMLSerializer s = new XMLSerializer(new OutputStreamWriter(System.out), format);
        s.serialize(getXML(args));
        System.out.println();
    }
}
