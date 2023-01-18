package psoft.util.freemarker.acl;

import freemarker.template.TemplateHashModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;

/* loaded from: hsphere.zip:psoft/util/freemarker/acl/FMACLManager.class */
public class FMACLManager {
    private static final Category log = Category.getInstance(FMACLManager.class.getName());
    public static final String ADMIN = "admin";
    public static final String RESELLER = "reseller";
    public static final String USER = "user";
    public static final String EVERYONE = "everyone";
    Map groups = new HashMap();

    public FMACLManager() {
        this.groups.put(ADMIN, new HashSet());
        this.groups.put(RESELLER, new HashSet());
        this.groups.put(USER, new HashSet());
        this.groups.put(EVERYONE, new HashSet());
    }

    protected String getGroup() {
        if (Session.getUser() == null) {
            return EVERYONE;
        }
        Account a = Session.getAccount();
        if (a == null) {
            return USER;
        }
        String type = a.getPlan().getValue("_CREATED_BY_");
        try {
            long resellerId = Session.getResellerId();
            if (type == null || type.equals(ADMIN)) {
                return resellerId == 1 ? ADMIN : RESELLER;
            } else if (type.equals("6")) {
                return RESELLER;
            } else {
                return USER;
            }
        } catch (UnknownResellerException e) {
            log.warn(e);
            return EVERYONE;
        }
    }

    public boolean testKey(Class c, String key) {
        Collection list = getACL(getGroup());
        if (list == null) {
            return false;
        }
        return list.contains(c.getName() + "#" + key);
    }

    public void load(Document doc, FMACLObjects objects) throws TransformerException {
        NodeList list = XPathAPI.selectNodeList(doc, "/objects/object");
        for (int i = 0; i < list.getLength(); i++) {
            NamedNodeMap attr = list.item(i).getAttributes();
            String key = attr.getNamedItem(MerchantGatewayManager.MG_KEY_PREFIX).getNodeValue();
            String className = attr.getNamedItem("class").getNodeValue();
            try {
                Class c = Class.forName(className);
                TemplateHashModel thm = (TemplateHashModel) c.newInstance();
                objects.put(key, new FMACLWrapper(thm, this));
                loadACLs(c);
            } catch (IOException e) {
                log.warn("Unable to load acl", e);
            } catch (ClassCastException e2) {
                log.warn("Unable to load object", e2);
            } catch (ClassNotFoundException e3) {
                log.error("Unable to load object", e3);
            } catch (IllegalAccessException e4) {
                log.warn("Unable to load object", e4);
            } catch (InstantiationException e5) {
                log.warn("Unable to load object", e5);
            }
        }
    }

    void loadACLs(Class c) throws IOException {
        int pos;
        String prefix = c.getName() + "#";
        String resource = c.getName().substring(c.getName().lastIndexOf(46) + 1) + ".acl";
        InputStream in = c.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("No such resource: " + resource);
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        while (true) {
            String line = r.readLine();
            if (line != null) {
                String line2 = line.trim();
                if (line2.length() != 0 && line2.charAt(0) != '#' && (pos = line2.indexOf(32)) != -1) {
                    setPermissions(prefix + line2.substring(0, pos).trim(), line2.substring(pos + 1));
                }
            } else {
                in.close();
                return;
            }
        }
    }

    private void setPermissions(String key, String access) {
        for (int i = 0; i < access.length(); i++) {
            char ch = access.charAt(i);
            switch (ch) {
                case 'a':
                    getACL(ADMIN).add(key);
                    break;
                case 'e':
                    getACL(ADMIN).add(key);
                    getACL(RESELLER).add(key);
                    getACL(USER).add(key);
                    getACL(EVERYONE).add(key);
                    break;
                case 'r':
                    getACL(RESELLER).add(key);
                    break;
                case 'u':
                    getACL(USER).add(key);
                    break;
            }
        }
    }

    protected Collection getACL(String group) {
        return (Collection) this.groups.get(group);
    }
}
