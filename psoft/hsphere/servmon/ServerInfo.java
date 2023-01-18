package psoft.hsphere.servmon;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import psoft.util.freemarker.TemplateXML;

/* loaded from: hsphere.zip:psoft/hsphere/servmon/ServerInfo.class */
public class ServerInfo {
    private static Category log = Category.getInstance(ServerInfo.class.getName());
    protected static DocumentBuilder dbuilder;
    protected Document doc;
    protected long serverId;

    static {
        try {
            dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            log.error("Unable to create DocumentBuilder", e);
        }
    }

    public void update(String body) {
        update(new StringReader(body));
    }

    public void update(InputStream in) {
        update(new InputStreamReader(in));
    }

    public void update(Reader body) {
        try {
            synchronized (dbuilder) {
                this.doc = dbuilder.parse(new InputSource(body));
            }
        } catch (Exception e) {
            log.error("Unable to update server status", e);
        }
    }

    public TemplateModel get() throws TemplateModelException {
        if (this.doc == null) {
            return null;
        }
        return new TemplateXML(this.doc).get("ServerInfo");
    }

    public static InputStream getStaticInfo(String name) {
        return ServerInfo.class.getClassLoader().getResourceAsStream(name);
    }
}
