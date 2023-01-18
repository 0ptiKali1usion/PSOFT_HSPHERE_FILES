package psoft.hsphere.migrator.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Category;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.SSL;
import psoft.hsphere.axis.NamedParameter;
import psoft.hsphere.axis.Utils;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.IIS.VirtualHostingResource;
import psoft.hsphere.resource.SSLProperties;
import psoft.hsphere.resource.VHostSettings;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.apache.CGIDirResource;
import psoft.hsphere.resource.apache.CGIResource;
import psoft.hsphere.resource.apache.ServerAliasResource;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/WebServiceCreator.class */
public class WebServiceCreator {
    private static Category log = Category.getInstance(WebServiceCreator.class.getName());
    protected Account account;
    protected String domain;
    protected CommonUserCreator creator;
    protected final int UNIX_PLATFORM = 1;
    protected final int WIN_PLATFORM = 2;

    private int getPlatformType(Account account) throws Exception {
        return account.getId().findChild("unixuser").get() instanceof WinUserResource ? 2 : 1;
    }

    public WebServiceCreator(Account account, String domain, CommonUserCreator creator) {
        this.account = null;
        this.domain = null;
        this.creator = null;
        this.account = account;
        this.domain = domain;
        this.creator = creator;
    }

    public void addWebService(Element webservice) {
        if (webservice == null) {
            return;
        }
        NodeList nodes = webservice.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (!child.getNodeName().equals("#text")) {
                createObject(webservice, child.getNodeName());
            }
        }
    }

    private void createObject(Element webservice, String name) {
        NodeList list = webservice.getElementsByTagName(name);
        if (isEmpty(list)) {
            return;
        }
        Element current = (Element) list.item(0);
        try {
            if ("settings".equals(name)) {
                addSettings(current);
            } else if ("transferlog".equals(name)) {
                addTransferLog();
            } else if ("errorlog".equals(name)) {
                addErrorLog();
            } else if ("webalizer".equals(name)) {
                addWebalizer();
            } else if ("modlogan".equals(name)) {
                addModlogan();
            } else if ("referrerlog".equals(name)) {
                addReferrerLog();
            } else if ("agentlog".equals(name)) {
                addAgentLog();
            } else if ("asp".equals(name)) {
                addASP();
            } else if ("asp_net".equals(name)) {
                addASPNet();
            } else if ("asp_secured".equals(name)) {
                addASPSecured();
            } else if ("asp_secured_license".equals(name)) {
                addASPSecuredLicense(current);
            } else if ("empresa".equals(name)) {
                addMivaEmpresa();
            } else if ("miva".equals(name)) {
                addMivaCard();
            } else if ("urchin3".equals(name)) {
                addUrchin3();
            } else if ("urchin4".equals(name)) {
                addUrchin4();
            } else if ("mssqlmanager".equals(name)) {
                addMSSQLManager();
            } else if ("cgi".equals(name)) {
                createResourceFromListItems(current, "cgi", "cgi", "cgilistitem");
            } else if ("cgidir".equals(name)) {
                createResourceFromListItems(current, "cgidir", "cgi directory", "listitem");
            } else if ("ismap".equals(name)) {
                createResourceFromListItems(current, "ismap", "server side imagemap", "listitem");
            } else if ("mimetype".equals(name)) {
                createResourceFromListItems(current, "mimetype", "mime type", "mimelistitem");
            } else if ("errordoc".equals(name)) {
                createResourceFromListItems(current, "errordoc", "error document", "errordocitem");
            } else if ("vhost_alias".equals(name)) {
                createResourceFromListItems(current, "vhost_alias", "server alias", "listitem");
            } else if ("redirect_url".equals(name)) {
                createResourceFromListItems(current, "redirect_url", "redirect", "redirectitem");
            } else if ("php3".equals(name)) {
                createResourceFromListItems(current, "php3", "php", "listitem");
            } else if ("ssi".equals(name)) {
                createResourceFromListItems(current, "ssi", "server side include", "listitem");
            } else if ("cf".equals(name)) {
                createResourceFromListItems(current, "cf", "cold fusion", "listitem");
            } else if ("frontpage".equals(name)) {
                addFrontpage(current);
            } else if ("ssl".equals(name)) {
                addSSL(current);
            } else if ("directory_ind".equals(name)) {
                addDirectoryIndexes(current);
            } else if ("throttle".equals(name)) {
                addThrottlePolicy(current);
            } else {
                this.creator.outMessage("Unknown element - " + name + " [SKIPPED]\n");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private boolean isEmpty(NodeList list) {
        if (list.getLength() == 0) {
            return true;
        }
        return false;
    }

    private void addResource(String name, String descr) throws Exception {
        List values = new ArrayList();
        addResource(name, descr, values);
    }

    private boolean isResourceExist(String name) throws Exception {
        try {
            ResourceId rid = getHostingResource(this.account, this.domain).getId().findChild(name);
            if (rid != null) {
                this.creator.outMessage("Resource - " + name + " exist. Skipped resource creation.\n");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void addResource(String name, String descr, List values) throws Exception {
        try {
            if (!isResourceExist(name)) {
                this.creator.outMessage("Creating webservice : " + descr);
                getHostingResource(this.account, this.domain).addChild(name, "", values);
                this.creator.outOK();
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + descr, exc);
        }
    }

    private void addResourceItem(String name, String descr, List values) throws Exception {
        try {
            this.creator.outMessage("Creating webservice : " + descr);
            getHostingResource(this.account, this.domain).addChild(name, "", values);
            this.creator.outOK();
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + descr, exc);
        }
    }

    private Resource getHostingResource(Account a, String domain) throws Exception {
        return Utils.getDomain(a, domain).getId().findChild("hosting").get();
    }

    private Resource getTransferLogResource(Account a, String domain) throws Exception {
        return Utils.getDomain(a, domain).getId().findChild("transferlog").get();
    }

    private String getAttributeValue(NamedNodeMap attrs, String attrName) {
        if (attrs.getNamedItem(attrName) != null) {
            return decode(attrs.getNamedItem(attrName).getNodeValue());
        }
        return null;
    }

    private String getSymbol(String str, int index) {
        StringBuffer buffer = new StringBuffer();
        while (str.charAt(index) != ';') {
            buffer.append(str.charAt(index));
            index++;
        }
        int code = Integer.parseInt(buffer.toString());
        return String.valueOf((char) code);
    }

    private int getEndIndex(String str, int index) {
        while (str.charAt(index) != ';') {
            index++;
        }
        int i = index;
        int i2 = index + 1;
        return i;
    }

    private String decode(String value) {
        if (value == null) {
            return null;
        }
        int index = value.indexOf("&#");
        if (index < 0) {
            return value;
        }
        StringBuffer buffer = new StringBuffer(value);
        while (index >= 0) {
            buffer.replace(index, getEndIndex(buffer.toString(), index + 2) + 1, getSymbol(buffer.toString(), index + 2));
            index = buffer.toString().indexOf("&#");
        }
        return buffer.toString();
    }

    private void addSSL(Element sslNode) throws Exception {
        this.creator.outMessage("Setting up SSL");
        String sitename = sslNode.getAttribute("sitename");
        String key = null;
        String cert = null;
        String chain = null;
        try {
            NodeList nodes = sslNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                String nodeName = n.getNodeName();
                if ("ssl_key".equals(nodeName)) {
                    key = n.getFirstChild().getNodeValue();
                } else if ("ssl_cert".equals(nodeName)) {
                    cert = n.getFirstChild().getNodeValue();
                } else if ("ssl_chain".equals(nodeName)) {
                    chain = n.getFirstChild().getNodeValue();
                }
            }
            Collection _params = new ArrayList();
            Resource _parent = getHostingResource(this.account, this.domain);
            if (_parent instanceof VirtualHostingResource) {
                _params.add("0");
                _params.add("0");
            }
            _params.add(key);
            _params.add(cert);
            if (sitename != null && sitename.length() > 0) {
                _params.add(sitename);
            }
            ResourceId rid = _parent.addChild("ssl", "", _params);
            if (chain != null) {
                SSL ssl = (SSL) rid.get();
                SSLProperties sslp = new SSLProperties(ssl.getProperties());
                sslp.setCertificateChain(chain);
                ssl.setProperties(sslp);
                ssl.installCertificateChain(sslp);
            }
            this.creator.outOK();
        } catch (Exception e) {
            this.creator.outFail();
            this.creator.outFail("Failed to add ssl", e);
        }
    }

    private void addSettings(Element settings) throws Exception {
        NamedParameter[] params;
        this.creator.outMessage("Creating webservice : settings");
        try {
            NamedNodeMap attrs = settings.getAttributes();
            String index = getAttributeValue(attrs, "index");
            NamedParameter[] params2 = NamedParameter.addParameter("index", index, new NamedParameter[0]);
            if (getAttributeValue(attrs, "iis_status") != null) {
                String iisstatus = getAttributeValue(attrs, "iis_status");
                params = NamedParameter.addParameter("iis_status", iisstatus, params2);
            } else {
                String multiviews = getAttributeValue(attrs, "multiview");
                NamedParameter[] params3 = NamedParameter.addParameter("multiview", multiviews, params2);
                String ssiexec = getAttributeValue(attrs, "ssi");
                NamedParameter[] params4 = NamedParameter.addParameter("ssi", ssiexec, params3);
                String symlinks = getAttributeValue(attrs, "symlink");
                params = NamedParameter.addParameter("symlink", symlinks, params4);
            }
            ((VHostSettings) getHostingResource(this.account, this.domain)).setSettings(NamedParameter.getParametersAsHashMap(params));
            this.creator.outOK();
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : settings", exc);
        }
    }

    private void addTransferLog() throws Exception {
        addResource("transferlog", "transfer log");
    }

    private void addErrorLog() throws Exception {
        addResource("errorlog", "error log");
    }

    private void addWebalizer() throws Exception {
        try {
            if (!isResourceExist("webalizer")) {
                List values = new ArrayList();
                this.creator.outMessage("Creating webservice : webalizer");
                getTransferLogResource(this.account, this.domain).addChild("webalizer", "", values);
                this.creator.outOK();
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : webalizer", exc);
        }
    }

    private void addModlogan() throws Exception {
        addResource("modlogan", "modlogan");
    }

    private void addReferrerLog() throws Exception {
        addResource("referrerlog", "referrer log");
    }

    private void addAgentLog() throws Exception {
        addResource("agentlog", "agent log");
    }

    private void addASP() throws Exception {
        addResource("asp", "asp");
    }

    private void addASPNet() throws Exception {
        addResource("asp_net", "asp net");
    }

    private void addASPSecured() throws Exception {
        addResource("asp_secured", "asp secured");
    }

    private void addMivaEmpresa() throws Exception {
        addResource("empresa", "miva empresa");
    }

    private void addMivaCard() throws Exception {
        addResource("miva", "miva card");
    }

    private void addUrchin3() throws Exception {
        addResource("urchin", "urchin3");
    }

    private void addUrchin4() throws Exception {
        addResource("urchin4", "urchin4");
    }

    private void addMSSQLManager() throws Exception {
        addResource("mssqlmanager", "mssql manager");
    }

    private void createResourceFromListItems(Element resource, String resourceName, String resourceDescr, String itemName) throws Exception {
        try {
            NodeList nodes = resource.getElementsByTagName(itemName);
            if (nodes.getLength() == 0) {
                return;
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Element listitem = (Element) nodes.item(i);
                if ("ismap".equals(resourceName)) {
                    List values = new ArrayList();
                    values.add(listitem.getFirstChild().getNodeValue());
                    addResourceItem(resourceName, resourceDescr, values);
                } else if ("mimetype".equals(resourceName)) {
                    addMimeItem(listitem, resourceName, resourceDescr);
                } else if ("ssi".equals(resourceName)) {
                    addSSIItem(listitem, resourceName, resourceDescr);
                } else if ("errordoc".equals(resourceName)) {
                    addErrorDocItem(listitem, resourceName, resourceDescr);
                } else if ("redirect_url".equals(resourceName)) {
                    addRedirectItem(listitem, resourceName, resourceDescr);
                } else if ("php3".equals(resourceName)) {
                    addPHPItem(listitem, resourceName, resourceDescr);
                } else if ("cf".equals(resourceName)) {
                    addColdFusionItem(listitem, resourceName, resourceDescr);
                } else if ("vhost_alias".equals(resourceName)) {
                    addServerAliasItem(listitem, resourceName, resourceDescr);
                } else if ("cgi".equals(resourceName)) {
                    addCGIItem(listitem, resourceName, resourceDescr);
                } else if ("cgidir".equals(resourceName)) {
                    addCGIDirItem(listitem, resourceName, resourceDescr);
                } else {
                    this.creator.outMessage("Unknown element - " + resourceName + " [SKIPPED]\n");
                }
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addMimeItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        NamedNodeMap attrs = listitem.getAttributes();
        values.add(getAttributeValue(attrs, "ext"));
        values.add(getAttributeValue(attrs, "mime"));
        addResourceItem(resourceName, resourceDescr, values);
    }

    private void addErrorDocItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        NamedNodeMap attrs = listitem.getAttributes();
        values.add(getAttributeValue(attrs, "code"));
        values.add(getAttributeValue(attrs, "message"));
        values.add(getAttributeValue(attrs, "doctype"));
        addResourceItem(resourceName, resourceDescr, values);
    }

    private void addRedirectItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        NamedNodeMap attrs = listitem.getAttributes();
        if (getAttributeValue(attrs, "below") != null) {
            values.add(getAttributeValue(attrs, "exact"));
            values.add(getAttributeValue(attrs, "below"));
            values.add(getAttributeValue(attrs, "perm"));
            String urlPath = getAttributeValue(attrs, "urlpath");
            if (urlPath == null) {
                urlPath = "";
            }
            values.add(urlPath);
            values.add(getAttributeValue(attrs, "url"));
            values.add(getAttributeValue(attrs, "protocol"));
        } else {
            values.add(getAttributeValue(attrs, "status"));
            values.add(getAttributeValue(attrs, "urlpath"));
            values.add(getAttributeValue(attrs, "url"));
            values.add(getAttributeValue(attrs, "protocol"));
        }
        addResourceItem(resourceName, resourceDescr, values);
    }

    private void addPHPItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        try {
            String extension = listitem.getFirstChild().getNodeValue();
            ResourceId rid = getHostingResource(this.account, this.domain).getId().findChild(resourceName);
            if (rid == null) {
                rid = getHostingResource(this.account, this.domain).addChild(resourceName, "", values);
            }
            values.add(extension);
            this.creator.outMessage("Creating webservice : " + resourceDescr);
            try {
                rid.get().addChild("php3entry", "", values);
                this.creator.outOK();
            } catch (Exception e) {
                this.creator.outFail();
                this.creator.outMessage("PHP entry - " + extension + " exist. Skipped resource creation.\n");
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addSSIItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        String extension = listitem.getFirstChild().getNodeValue();
        if (getPlatformType(this.account) != 2) {
            if (getPlatformType(this.account) == 1) {
                values.add(extension);
                this.creator.outMessage("Creating webservice : " + resourceDescr);
                try {
                    getHostingResource(this.account, this.domain).addChild(resourceName, "", values);
                    this.creator.outOK();
                    return;
                } catch (Exception e) {
                    this.creator.outFail();
                    this.creator.outMessage("SSI entry - " + extension + " exist. Skipped resource creation.\n");
                    return;
                }
            }
            return;
        }
        try {
            ResourceId rid = getHostingResource(this.account, this.domain).getId().findChild(resourceName);
            if (rid == null) {
                rid = getHostingResource(this.account, this.domain).addChild(resourceName, "", values);
            }
            values.add(extension);
            this.creator.outMessage("Creating webservice : " + resourceDescr);
            try {
                rid.get().addChild("ssi_entry", "", values);
                this.creator.outOK();
            } catch (Exception e2) {
                this.creator.outFail();
                this.creator.outMessage("SSI entry - " + extension + " exist. Skipped resource creation.\n");
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addColdFusionItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        try {
            String extension = listitem.getFirstChild().getNodeValue();
            ResourceId rid = getHostingResource(this.account, this.domain).getId().findChild(resourceName);
            if (rid == null) {
                rid = getHostingResource(this.account, this.domain).addChild(resourceName, "", values);
            }
            values.add(extension);
            this.creator.outMessage("Creating webservice : " + resourceDescr);
            try {
                rid.get().addChild("cfentry", "", values);
                this.creator.outOK();
            } catch (Exception e) {
                this.creator.outFail();
                this.creator.outMessage("ColdFusion entry - " + extension + " exist. Skipped resource creation.\n");
            }
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addServerAliasItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        try {
            String alias = listitem.getFirstChild().getNodeValue();
            Collection<ResourceId> aliases = getHostingResource(this.account, this.domain).getId().findChildren(resourceName);
            for (ResourceId resourceId : aliases) {
                Resource res = resourceId.get();
                if (res instanceof ServerAliasResource) {
                    ServerAliasResource al = (ServerAliasResource) res;
                    if (al.get("alias").toString().equalsIgnoreCase(alias)) {
                        this.creator.outMessage("Server alias - " + alias + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else if (res instanceof psoft.hsphere.resource.IIS.ServerAliasResource) {
                    psoft.hsphere.resource.IIS.ServerAliasResource al2 = (psoft.hsphere.resource.IIS.ServerAliasResource) res;
                    if (al2.get("alias").toString().equalsIgnoreCase(alias)) {
                        this.creator.outMessage("Server alias - " + alias + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else {
                    continue;
                }
            }
            values.add(alias);
            addResourceItem(resourceName, resourceDescr, values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addCGIItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        try {
            String handler = null;
            NamedNodeMap attrs = listitem.getAttributes();
            String extension = attrs.getNamedItem("ext").getNodeValue();
            if (attrs.getNamedItem("handler") != null) {
                handler = attrs.getNamedItem("handler").getNodeValue();
            }
            Collection<ResourceId> col = getHostingResource(this.account, this.domain).getId().findChildren(resourceName);
            for (ResourceId resourceId : col) {
                Resource res = resourceId.get();
                if (res instanceof CGIResource) {
                    CGIResource cgi = (CGIResource) res;
                    if (cgi.getExt().equalsIgnoreCase(extension)) {
                        this.creator.outMessage("Cgi extension - " + extension + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else if (res instanceof psoft.hsphere.resource.IIS.CGIResource) {
                    psoft.hsphere.resource.IIS.CGIResource cgi2 = (psoft.hsphere.resource.IIS.CGIResource) res;
                    if (cgi2.getExt().toString().equalsIgnoreCase(extension)) {
                        this.creator.outMessage("Cgi extension - " + extension + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else {
                    continue;
                }
            }
            values.add(extension);
            if (handler != null) {
                values.add(handler);
            }
            addResourceItem(resourceName, resourceDescr, values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addCGIDirItem(Element listitem, String resourceName, String resourceDescr) throws Exception {
        List values = new ArrayList();
        try {
            String cgidir = listitem.getFirstChild().getNodeValue();
            Collection<ResourceId> col = getHostingResource(this.account, this.domain).getId().findChildren(resourceName);
            for (ResourceId resourceId : col) {
                Resource res = resourceId.get();
                if (res instanceof CGIDirResource) {
                    CGIDirResource cgi = (CGIDirResource) res;
                    if (cgi.getDir().equalsIgnoreCase(cgidir)) {
                        this.creator.outMessage("Cgi directory - " + cgidir + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else if (res instanceof psoft.hsphere.resource.IIS.CGIDirResource) {
                    psoft.hsphere.resource.IIS.CGIDirResource cgi2 = (psoft.hsphere.resource.IIS.CGIDirResource) res;
                    if (cgi2.get("dir").toString().equalsIgnoreCase(cgidir)) {
                        this.creator.outMessage("Cgi directory - " + cgidir + " exist. Skipped resource creation.\n");
                        return;
                    }
                } else {
                    continue;
                }
            }
            values.add(cgidir);
            addResourceItem(resourceName, resourceDescr, values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : " + resourceDescr, exc);
        }
    }

    private void addFrontpage(Element frontpage) throws Exception {
        List values = new ArrayList();
        try {
            Element user = (Element) frontpage.getElementsByTagName("login").item(0);
            Element password = (Element) frontpage.getElementsByTagName("password").item(0);
            if (user != null) {
                values.add(user.getFirstChild().getNodeValue());
                values.add(password.getFirstChild().getNodeValue());
            }
            addResource("frontpage", "front page", values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : frontpage", exc);
        }
    }

    private void addASPSecuredLicense(Element license) throws Exception {
        List values = new ArrayList();
        try {
            String orderID = license.getAttributes().getNamedItem("orderid").getNodeValue();
            values.add(orderID);
            addResource("asp_secured_license", "asp secured license", values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : asp secured license", exc);
        }
    }

    private void addDirectoryIndexes(Element dirind) throws Exception {
        List values = new ArrayList();
        try {
            values.add(dirind.getFirstChild().getNodeValue());
            addResource("directory_ind", "directory index", values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : directory index", exc);
        }
    }

    private void addThrottlePolicy(Element throttle) throws Exception {
        List values = new ArrayList();
        try {
            NamedNodeMap attrs = throttle.getAttributes();
            values.add(getAttributeValue(attrs, "type"));
            values.add(getAttributeValue(attrs, "limit"));
            String limitUn = getAttributeValue(attrs, "limitUn");
            values.add(limitUn != null ? limitUn : "");
            values.add(getAttributeValue(attrs, "interval"));
            values.add(getAttributeValue(attrs, "intervalUn"));
            addResource("throttle", "throttle policy", values);
        } catch (Exception exc) {
            this.creator.outFail();
            this.creator.outFail("Failed to add webservice : throttle policy", exc);
        }
    }
}
