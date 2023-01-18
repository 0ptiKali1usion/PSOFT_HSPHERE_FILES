package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LServerInfo.class */
public class LServerInfo implements TemplateHashModel {
    protected long lserverId;
    protected int lserverGroup;
    private static TreeMap lserverTypes;

    static {
        try {
            lserverTypes = new TreeMap();
            Document doc = XMLManager.getXML("LSERVER_INFO");
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName("lserver_info_type");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                NamedNodeMap attr = node.getAttributes();
                LogicalServerInfo logicalServerInfo = (LogicalServerInfo) Class.forName(attr.getNamedItem("class").getNodeValue()).newInstance();
                lserverTypes.put(attr.getNamedItem("id").getNodeValue(), logicalServerInfo);
            }
        } catch (Exception e) {
        }
    }

    public LServerInfo(long lserverId, int lserverGroup) {
        this.lserverId = lserverId;
        this.lserverGroup = lserverGroup;
    }

    protected List getInfo() throws Exception {
        LogicalServerInfo info = (LogicalServerInfo) lserverTypes.get(String.valueOf(this.lserverGroup));
        if (info != null) {
            return info.getInfo(this.lserverId);
        }
        Session.getLog().error("Server info class not found for server group " + this.lserverGroup);
        return null;
    }

    protected String getUsed() throws Exception {
        LogicalServerInfo info = (LogicalServerInfo) lserverTypes.get(String.valueOf(this.lserverGroup));
        if (info != null) {
            return info.getUsed(this.lserverId);
        }
        Session.getLog().error("Server info class not found for server group " + this.lserverGroup);
        return null;
    }

    protected String getFixed() throws Exception {
        LogicalServerInfo info = (LogicalServerInfo) lserverTypes.get(String.valueOf(this.lserverGroup));
        if (info != null) {
            return info.getFixed();
        }
        Session.getLog().error("Server info class not found for server group " + this.lserverGroup);
        return null;
    }

    public List getIPTypes() throws Exception {
        LogicalServerInfo info = (LogicalServerInfo) lserverTypes.get(String.valueOf(this.lserverGroup));
        if (info != null) {
            return info.getIPTypes();
        }
        Session.getLog().error("Server info class not found for server group " + this.lserverGroup);
        return null;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("used".equals(key)) {
                return new TemplateString(getUsed());
            }
            if ("params".equals(key)) {
                return new TemplateList(getInfo());
            }
            if ("fixed".equals(key)) {
                return new TemplateString(getFixed());
            }
            if ("iptypes".equals(key)) {
                return new TemplateList(getIPTypes());
            }
            return null;
        } catch (Exception ex) {
            Session.getLog().error("Error geting Lserver information", ex);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }
}
