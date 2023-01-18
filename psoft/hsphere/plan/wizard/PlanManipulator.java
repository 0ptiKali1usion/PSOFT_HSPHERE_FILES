package psoft.hsphere.plan.wizard;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletRequest;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/plan/wizard/PlanManipulator.class */
public abstract class PlanManipulator {
    private static Category log = Category.getInstance(PlanManipulator.class.getName());
    Element root;
    HashMap specialVariables = new HashMap();
    Set includedResources = new HashSet();
    Set enabledResources = new HashSet();
    Set deletedResources = new HashSet();
    HashMap unknownResources = new HashMap();

    /* renamed from: rq */
    ServletRequest f114rq = Session.getRequest();

    protected abstract void setValues(String str, NodeList nodeList, boolean z) throws Exception;

    protected abstract void processMods(String str, NodeList nodeList, boolean z) throws Exception;

    protected abstract void processResource(Node node) throws Exception;

    protected abstract void setResources(NodeList nodeList) throws Exception;

    protected boolean ifResource(String name) {
        return this.includedResources.contains(name);
    }

    public int ifResourceAttr(String attr) {
        if (attr == null || attr.length() == 0) {
            return 1;
        }
        StringTokenizer st = new StringTokenizer(attr, "|");
        boolean unknown = false;
        while (st.hasMoreElements()) {
            String resourceName = st.nextToken();
            if (ifResource(resourceName)) {
                return 1;
            }
            if (this.unknownResources.containsKey(resourceName)) {
                unknown = true;
            }
        }
        return unknown ? 0 : -1;
    }

    protected boolean ifActive(String name) {
        if (name == null || name.length() == 0) {
            return true;
        }
        return this.enabledResources.contains(name);
    }

    public boolean ifActiveAttr(String attr) {
        if (attr == null || attr.length() == 0) {
            return true;
        }
        StringTokenizer st = new StringTokenizer(attr, "|");
        while (st.hasMoreElements()) {
            if (ifActive(st.nextToken())) {
                return true;
            }
        }
        return false;
    }

    public String getNodeValue(Node n) {
        Node tmp = n.getFirstChild();
        if (tmp != null) {
            return tmp.getNodeValue();
        }
        return null;
    }

    public String getAttribute(Node n, String attName, String defValue) {
        String str = getAttribute(n, attName);
        return str == null ? defValue : str;
    }

    public String getAttribute(Node n, String attName) {
        Node att = n.getAttributes().getNamedItem(attName);
        if (att == null) {
            return null;
        }
        return att.getNodeValue();
    }

    public String parseValue(String value) {
        if (value == null || value.length() < 1) {
            return "";
        }
        char ch = value.charAt(0);
        if (ch == '$') {
            return (String) this.specialVariables.get(value.substring(1));
        }
        if (ch == '#') {
            return this.f114rq.getParameter(value.substring(1));
        }
        return value;
    }

    public Hashtable parseValues(String mask) {
        String pVal;
        Hashtable res = new Hashtable();
        if (mask != null || mask.length() > 0) {
            Enumeration i = this.f114rq.getParameterNames();
            while (i.hasMoreElements()) {
                String pName = (String) i.nextElement();
                if (pName.indexOf(mask) >= 0 && (pVal = this.f114rq.getParameter(pName)) != null) {
                    res.put(pName, pVal);
                }
            }
        }
        return res;
    }

    public boolean testNode(Node n) {
        String leftValue;
        return "eq".equals(getAttribute(n, "type")) && (leftValue = parseValue(getAttribute(n, "left"))) != null && leftValue.equals(getAttribute(n, "right"));
    }

    public boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public void prepare() {
        String instantAlias = this.f114rq.getParameter("calias");
        String stopgapAlias = this.f114rq.getParameter("stopgapalias");
        String planType = getAttribute(this.root, "name");
        if (instantAlias != null) {
            String instantPrefix = instantAlias.substring(0, instantAlias.lastIndexOf("NNN"));
            String instantZone = instantAlias.substring(instantAlias.lastIndexOf("NNN") + 3, instantAlias.length());
            this.specialVariables.put("INSTANT_ZONE", instantZone);
            this.specialVariables.put("INSTANT_PREFIX", instantPrefix);
        }
        if (stopgapAlias != null) {
            String stopgapPrefix = stopgapAlias.substring(0, stopgapAlias.lastIndexOf("NNN"));
            String stopgapZone = stopgapAlias.substring(stopgapAlias.lastIndexOf("NNN") + 3, stopgapAlias.length());
            this.specialVariables.put("STOPGAP_ZONE", stopgapZone);
            this.specialVariables.put("STOPGAP_PREFIX", stopgapPrefix);
        }
        if ("unix".equals(planType)) {
            this.specialVariables.put("HOME_DIR", HostEntry.UnixUserDefaultHome);
        } else if ("windows".equals(planType)) {
            this.specialVariables.put("HOME_DIR", HostEntry.WinUserDefaultHome);
        }
    }

    public void setResources() throws Exception {
        NodeList list = XPathAPI.selectNodeList(this.root, "categories/category/*");
        setResources(list);
        HashMap hashMap = this.unknownResources;
        while (true) {
            HashMap unknown = hashMap;
            if (unknown.size() <= 0) {
                break;
            }
            this.unknownResources = new HashMap();
            for (Object obj : unknown.keySet()) {
                processResource((Node) unknown.get(obj));
            }
            hashMap = this.unknownResources;
        }
        for (String str : this.includedResources) {
            configureResource(str, true);
        }
        for (String str2 : this.deletedResources) {
            configureResource(str2, false);
        }
    }

    protected void configureResource(String name, boolean flag) throws Exception {
        Node r = XPathAPI.selectSingleNode(this.root, "resources/res_" + name);
        if (r != null) {
            setValues(name, XPathAPI.selectNodeList(r, "values/value"), flag);
            processMods(name, XPathAPI.selectNodeList(r, "mod"), flag);
        }
    }
}
