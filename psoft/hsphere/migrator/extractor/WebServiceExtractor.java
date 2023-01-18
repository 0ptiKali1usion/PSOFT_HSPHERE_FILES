package psoft.hsphere.migrator.extractor;

import java.util.Collection;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.axis.NamedParameter;
import psoft.hsphere.axis.Utils;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.IIS.CFEntry;
import psoft.hsphere.resource.IIS.OsCommerce;
import psoft.hsphere.resource.IIS.PHPEntry;
import psoft.hsphere.resource.IIS.SSIEntry;
import psoft.hsphere.resource.VHostSettings;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.apache.CGIDirResource;
import psoft.hsphere.resource.apache.CGIResource;
import psoft.hsphere.resource.apache.DirectoryIndexResource;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.hsphere.resource.apache.FrontPageResource;
import psoft.hsphere.resource.apache.ISMapResource;
import psoft.hsphere.resource.apache.MimeTypeResource;
import psoft.hsphere.resource.apache.MnogoSearch;
import psoft.hsphere.resource.apache.PHP3Resource;
import psoft.hsphere.resource.apache.PHPBBResource;
import psoft.hsphere.resource.apache.RedirectResource;
import psoft.hsphere.resource.apache.SSIResource;
import psoft.hsphere.resource.apache.ServerAliasResource;
import psoft.hsphere.resource.apache.ThrottleResource;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/WebServiceExtractor.class */
public class WebServiceExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(WebServiceExtractor.class.getName());
    private boolean force;
    private String unsupported;
    protected final int UNIX_PLATFORM = 1;
    protected final int WIN_PLATFORM = 2;

    private int getPlatformType(Account account) throws Exception {
        return account.getId().findChild("unixuser").get() instanceof WinUserResource ? 2 : 1;
    }

    public WebServiceExtractor(Document doc, boolean force) {
        super(doc);
        this.unsupported = "Unsupported platform";
        this.UNIX_PLATFORM = 1;
        this.WIN_PLATFORM = 2;
        this.force = force;
    }

    public Element getAllWebServiceInfoAsXML(Domain domain) throws Exception {
        return getWebService(domain);
    }

    private Element getWebService(Domain domain) throws Exception {
        Account account = domain.getAccount();
        String domainName = domain.getName();
        Element node = createNode("webservice");
        try {
            appendChildNode(node, getSettings(account, domainName));
            appendChildNode(node, getErrorLog(account, domainName));
            appendChildNode(node, getTransferLog(account, domainName));
            appendChildNode(node, getWebalizer(account, domainName));
            appendChildNode(node, getModLogAn(account, domainName));
            appendChildNode(node, getReferrerLog(account, domainName));
            appendChildNode(node, getAgentLog(account, domainName));
            appendChildNode(node, getUrchin3(account, domainName));
            appendChildNode(node, getUrchin4(account, domainName));
            appendChildNode(node, getResourceValues(account, domainName, "cgi"));
            appendChildNode(node, getResourceValues(account, domainName, "cgidir"));
            if (getPlatformType(account) == 1) {
                appendChildNode(node, getResourceValues(account, domainName, "ismap"));
            }
            appendChildNode(node, getResourceValues(account, domainName, "mimetype"));
            appendChildNode(node, getPHP(account, domainName));
            appendChildNode(node, getSSI(account, domainName));
            appendChildNode(node, getResourceValues(account, domainName, "errordoc"));
            appendChildNode(node, getResourceValues(account, domainName, "vhost_alias"));
            appendChildNode(node, getResourceValues(account, domainName, "redirect_url"));
            appendChildNode(node, getDirectoryIndexes(account, domainName));
            if (getPlatformType(account) == 1) {
                appendChildNode(node, getThrottlePolicy(account, domainName));
            }
            appendChildNode(node, getMnoGoSearch(account, domainName));
            appendChildNode(node, getPhpBB(account, domainName));
            appendChildNode(node, getFrontPage(account, domainName));
            if (getPlatformType(account) == 2) {
                appendChildNode(node, getASP(account, domainName));
                appendChildNode(node, getASPNet(account, domainName));
                appendChildNode(node, getColdFusion(account, domainName));
                appendChildNode(node, getMSSQLManager(account, domainName));
            }
            appendChildNode(node, getMivaEmpresaEngine(account, domainName));
            appendChildNode(node, getMivaShoppingCart(account, domainName));
            appendChildNode(node, getOsCommerce(account, domainName));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getResourceValues(Account account, String domainName, String resourceName) throws Exception {
        Element node = createNode(resourceName);
        if (getHostingResource(account, domainName) == null) {
            throw new Exception("Not found hosting resource for domain - " + domainName);
        }
        Collection<ResourceId> col = getHostingResource(account, domainName).getId().findChildren(resourceName);
        for (ResourceId rid : col) {
            if (rid == null) {
                return null;
            }
            Resource r = rid.get();
            if (resourceName.equals("cgi")) {
                appendChildNode(node, getCGIEntry(r));
            } else if (resourceName.equals("cgidir")) {
                appendChildNode(node, getCGIDirEntry(r));
            } else if (resourceName.equals("ismap")) {
                appendChildNode(node, getUnixServerSideImagemapEntry(r));
            } else if (resourceName.equals("mimetype")) {
                appendChildNode(node, getMIMETypeEntry(r));
            } else if (resourceName.equals("errordoc")) {
                appendChildNode(node, getErrorEntry(r));
            } else if (resourceName.equals("vhost_alias")) {
                appendChildNode(node, getVhostAlias(r));
            } else if (resourceName.equals("redirect_url")) {
                appendChildNode(node, getRedirect(r));
            }
        }
        return checkChildren(node);
    }

    private Element getSettings(Account account, String domainName) throws Exception {
        NamedParameter[] params;
        Element node = createNode("settings");
        try {
            params = getWebSettings(account, domainName);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (params == null) {
            return null;
        }
        node.setAttributeNode(createAttribute(params[0].getName(), params[0].getValue()));
        if (getPlatformType(account) == 1) {
            node.setAttributeNode(createAttribute(params[1].getName(), params[1].getValue()));
            node.setAttributeNode(createAttribute(params[2].getName(), params[2].getValue()));
            node.setAttributeNode(createAttribute(params[3].getName(), params[3].getValue()));
        } else if (getPlatformType(account) == 2) {
            node.setAttributeNode(createAttribute(params[1].getName(), params[1].getValue()));
        } else {
            throw new Exception(this.unsupported);
        }
        return checkChildren(node);
    }

    private Element createNodeWithValueAttr(String nodeName, boolean isON) throws Exception {
        Element node = null;
        if (isON) {
            node = createNode(nodeName);
            node.setAttributeNode(createAttribute("value", "ON"));
        }
        return checkChildren(node);
    }

    private Element getErrorLog(Account account, String domainName) throws Exception {
        boolean isON = isEnableErrorLog(account, domainName);
        return createNodeWithValueAttr("errorlog", isON);
    }

    private Element getTransferLog(Account account, String domainName) throws Exception {
        boolean isON = isEnableTransferLog(account, domainName);
        return createNodeWithValueAttr("transferlog", isON);
    }

    private Element getWebalizer(Account account, String domainName) throws Exception {
        boolean isON = isEnableWebalizer(account, domainName);
        return createNodeWithValueAttr("webalizer", isON);
    }

    private Element getModLogAn(Account account, String domainName) throws Exception {
        boolean isON = isEnableModLogAn(account, domainName);
        return createNodeWithValueAttr("modlogan", isON);
    }

    private Element getReferrerLog(Account account, String domainName) throws Exception {
        boolean isON = isEnableReferrerLog(account, domainName);
        return createNodeWithValueAttr("referrerlog", isON);
    }

    private Element getAgentLog(Account account, String domainName) throws Exception {
        boolean isON = isEnableAgentLog(account, domainName);
        return createNodeWithValueAttr("agentlog", isON);
    }

    private Element getUrchin3(Account account, String domainName) throws Exception {
        boolean isON = isEnableUrchin3Statistics(account, domainName);
        return createNodeWithValueAttr("urchin3", isON);
    }

    private Element getUrchin4(Account account, String domainName) throws Exception {
        boolean isON = isEnableUrchin4Statistics(account, domainName);
        return createNodeWithValueAttr("urchin4", isON);
    }

    private Element getCGIEntry(Resource r) throws Exception {
        Element node = createNode("cgilistitem");
        try {
            if (r instanceof CGIResource) {
                String extension = ((CGIResource) r).getExt();
                node.setAttributeNode(createAttribute("ext", extension));
            } else if (r instanceof psoft.hsphere.resource.IIS.CGIResource) {
                String extension2 = ((psoft.hsphere.resource.IIS.CGIResource) r).getExt();
                node.setAttributeNode(createAttribute("ext", extension2));
                node.setAttributeNode(createAttribute("handler", "Perl"));
            } else {
                throw new Exception(this.unsupported);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getCGIDirEntry(Resource r) throws Exception {
        String extension;
        Element node = null;
        try {
            if (r instanceof CGIDirResource) {
                extension = ((CGIDirResource) r).getDir();
            } else if (r instanceof psoft.hsphere.resource.IIS.CGIDirResource) {
                extension = ((psoft.hsphere.resource.IIS.CGIDirResource) r).get("dir").toString();
            } else {
                throw new Exception(this.unsupported);
            }
            node = createNode("listitem", extension);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getUnixServerSideImagemapEntry(Resource r) throws Exception {
        Element node = null;
        try {
            ISMapResource ismap = (ISMapResource) r;
            node = createNode("listitem", ismap.getExt());
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getMIMETypeEntry(Resource r) throws Exception {
        Element node = createNode("mimelistitem");
        try {
            if (r instanceof MimeTypeResource) {
                node.setAttributeNode(createAttribute("ext", ((MimeTypeResource) r).getExt()));
                node.setAttributeNode(createAttribute("mime", ((MimeTypeResource) r).get("mime").toString()));
            } else if (r instanceof psoft.hsphere.resource.IIS.MimeTypeResource) {
                node.setAttributeNode(createAttribute("ext", ((psoft.hsphere.resource.IIS.MimeTypeResource) r).getExt()));
                node.setAttributeNode(createAttribute("mime", ((psoft.hsphere.resource.IIS.MimeTypeResource) r).get("mime").toString()));
            } else {
                throw new Exception(this.unsupported);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getPHP(Account account, String domainName) throws Exception {
        Element node = createNode("php3");
        if (isEnablePHP(account, domainName)) {
            Collection<ResourceId> col = getHostingResource(account, domainName).getId().findChild("php3").findChildren("php3entry");
            for (ResourceId resourceId : col) {
                Resource res = resourceId.get();
                if (res != null) {
                    appendChildNode(node, getPHPEntry(res));
                }
            }
            return checkChildren(node);
        }
        return null;
    }

    private Element getPHPEntry(Resource r) throws Exception {
        String item;
        Element node = null;
        try {
            if (r instanceof PHP3Resource) {
                item = ((PHP3Resource) r).getExt();
            } else if (r instanceof PHPEntry) {
                item = ((PHPEntry) r).getExt();
            } else {
                throw new Exception(this.unsupported);
            }
            node = createNode("listitem", item);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getSSI(Account account, String domainName) throws Exception {
        Collection<ResourceId> ssis;
        Element node = createNode("ssi");
        if (getPlatformType(account) == 1) {
            ssis = getHostingResource(account, domainName).getId().findChildren("ssi");
        } else if (getPlatformType(account) == 2) {
            ssis = getHostingResource(account, domainName).getId().findChildren("ssi_entry");
        } else {
            throw new Exception(this.unsupported);
        }
        for (ResourceId rid : ssis) {
            if (rid == null) {
                throw new Exception("Not found ssi");
            }
            Resource res = rid.get();
            if (res != null) {
                appendChildNode(node, getSSIEntry(res));
            }
        }
        return checkChildren(node);
    }

    private Element getSSIEntry(Resource res) throws Exception {
        String item;
        Element node = null;
        try {
            if (res instanceof SSIResource) {
                SSIResource ssi = (SSIResource) res;
                item = ssi.getExt();
            } else if (res instanceof SSIEntry) {
                SSIEntry entry = (SSIEntry) res;
                item = entry.getExt();
            } else {
                throw new Exception(this.unsupported);
            }
            node = createNode("listitem", item);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getErrorEntry(Resource res) throws Exception {
        Element node = createNode("errordocitem");
        try {
            if (res instanceof ErrorDocumentResource) {
                ErrorDocumentResource doc = (ErrorDocumentResource) res;
                node.setAttributeNode(createAttribute("code", doc.get("code").toString()));
                node.setAttributeNode(createAttribute("message", doc.get("msg").toString()));
                node.setAttributeNode(createAttribute("doctype", doc.get("doctype").toString()));
            } else if (res instanceof psoft.hsphere.resource.IIS.ErrorDocumentResource) {
                psoft.hsphere.resource.IIS.ErrorDocumentResource doc2 = (psoft.hsphere.resource.IIS.ErrorDocumentResource) res;
                node.setAttributeNode(createAttribute("code", doc2.get("code").toString()));
                node.setAttributeNode(createAttribute("message", doc2.get("msg").toString()));
                node.setAttributeNode(createAttribute("doctype", doc2.get("doctype").toString()));
            } else {
                throw new Exception(this.unsupported);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getVhostAlias(Resource res) throws Exception {
        String item;
        Element node = null;
        try {
            if (res instanceof ServerAliasResource) {
                ServerAliasResource alias = (ServerAliasResource) res;
                item = alias.get("alias").toString();
            } else if (res instanceof psoft.hsphere.resource.IIS.ServerAliasResource) {
                psoft.hsphere.resource.IIS.ServerAliasResource alias2 = (psoft.hsphere.resource.IIS.ServerAliasResource) res;
                item = alias2.get("alias").toString();
            } else {
                throw new Exception(this.unsupported);
            }
            node = createNode("listitem", item);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getRedirect(Resource res) throws Exception {
        Element node = createNode("redirectitem");
        try {
            if (res instanceof RedirectResource) {
                RedirectResource redirect = (RedirectResource) res;
                node.setAttributeNode(createAttribute("urlpath", redirect.get("url_path").toString()));
                node.setAttributeNode(createAttribute("protocol", redirect.get("protocol").toString()));
                node.setAttributeNode(createAttribute("url", redirect.get("url").toString()));
                node.setAttributeNode(createAttribute("status", redirect.get("stat").toString()));
            } else if (res instanceof psoft.hsphere.resource.IIS.RedirectResource) {
                psoft.hsphere.resource.IIS.RedirectResource redirect2 = (psoft.hsphere.resource.IIS.RedirectResource) res;
                node.setAttributeNode(createAttribute("urlpath", redirect2.get("url_path").toString()));
                node.setAttributeNode(createAttribute("protocol", redirect2.get("protocol").toString()));
                node.setAttributeNode(createAttribute("url", redirect2.get("url").toString()));
                node.setAttributeNode(createAttribute("exact", redirect2.get("isExact").toString()));
                node.setAttributeNode(createAttribute("below", redirect2.get("isBelow").toString()));
                node.setAttributeNode(createAttribute("perm", redirect2.get("isPerm").toString()));
            } else {
                throw new Exception(this.unsupported);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getDirectoryIndexes(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = null;
        if (isEnableDirectoryIndexes(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("directory_ind");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            Resource res = rid.get();
            if (res instanceof DirectoryIndexResource) {
                DirectoryIndexResource indexes = (DirectoryIndexResource) res;
                node = createNode("directory_ind", indexes.get("indexes").toString());
            } else if (res instanceof psoft.hsphere.resource.IIS.DirectoryIndexResource) {
                psoft.hsphere.resource.IIS.DirectoryIndexResource indexes2 = (psoft.hsphere.resource.IIS.DirectoryIndexResource) res;
                node = createNode("directory_ind", indexes2.get("indexes").toString());
            } else {
                throw new Exception(this.unsupported);
            }
            return checkChildren(node);
        }
        return null;
    }

    private Element getThrottlePolicy(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = createNode("throttle");
        if (isEnableThrottlePolicy(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("throttle");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            ThrottleResource policy = (ThrottleResource) rid.get();
            String type = policy.get("policy").toString();
            node.setAttributeNode(createAttribute("type", type));
            if (type.equals("Concurrent") || type.equals("Document") || type.equals("Request") || type.equals("Idle") || type.equals("Random")) {
                node.setAttributeNode(createAttribute("limit", policy.get("limit").toString()));
                node.setAttributeNode(createAttribute("interval", policy.get("interval").toString()));
                node.setAttributeNode(createAttribute("intervalUn", policy.get("intervalUn").toString()));
            } else if (type.equals("Original") || type.equals("Speed") || type.equals("Volume")) {
                node.setAttributeNode(createAttribute("limit", policy.get("limit").toString()));
                node.setAttributeNode(createAttribute("limitUn", policy.get("limitUn").toString()));
                node.setAttributeNode(createAttribute("interval", policy.get("interval").toString()));
                node.setAttributeNode(createAttribute("intervalUn", policy.get("intervalUn").toString()));
            } else if (type.equals("None")) {
            }
            return checkChildren(node);
        }
        return null;
    }

    private Element createMySQLNode(Element parent, String dbID, String userID) throws Exception {
        ResourceId mysqlDB = new ResourceId(dbID);
        MySQLDatabase database = new MySQLDatabase(mysqlDB);
        ResourceId mysqlUser = new ResourceId(userID);
        MySQLUser user = new MySQLUser(mysqlUser);
        parent.setAttributeNode(createAttribute("db", database.get("db_name").toString()));
        parent.setAttributeNode(createAttribute(FMACLManager.USER, user.getName()));
        return checkChildren(parent);
    }

    private Element getMnoGoSearch(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = createNode("mnogosearch");
        if (isEnableMnoGoSearch(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("mnogosearch");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            MnogoSearch mnogosearch = (MnogoSearch) rid.get();
            node = createMySQLNode(node, mnogosearch.get("db").toString(), mnogosearch.get(FMACLManager.USER).toString());
            return checkChildren(node);
        }
        return null;
    }

    private Element getPhpBB(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = createNode("phpbb");
        if (isEnablePhpBB(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("phpbb");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            Resource res = rid.get();
            String db = null;
            String usr = null;
            if (res instanceof PHPBBResource) {
                PHPBBResource phpbb = (PHPBBResource) res;
                db = phpbb.get("db").toString();
                usr = phpbb.get(FMACLManager.USER).toString();
            } else if (res instanceof psoft.hsphere.resource.IIS.PHPBBResource) {
                psoft.hsphere.resource.IIS.PHPBBResource phpbb2 = (psoft.hsphere.resource.IIS.PHPBBResource) res;
                db = phpbb2.get("db").toString();
                usr = phpbb2.get(FMACLManager.USER).toString();
            }
            node = createMySQLNode(node, db, usr);
            return checkChildren(node);
        }
        return null;
    }

    private Element getOsCommerce(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = createNode("oscommerce");
        if (isEnableOsCommerce(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("oscommerce");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            Resource res = rid.get();
            String db = null;
            String usr = null;
            if (res instanceof OsCommerce) {
                OsCommerce oscommerce = (OsCommerce) res;
                db = oscommerce.get("db").toString();
                usr = oscommerce.get(FMACLManager.USER).toString();
            } else if (res instanceof psoft.hsphere.resource.apache.OsCommerce) {
                psoft.hsphere.resource.apache.OsCommerce oscommerce2 = (psoft.hsphere.resource.apache.OsCommerce) res;
                db = oscommerce2.get("db").toString();
                usr = oscommerce2.get(FMACLManager.USER).toString();
            }
            node = createMySQLNode(node, db, usr);
            return checkChildren(node);
        }
        return null;
    }

    private Element getFrontPage(Account account, String domainName) throws Exception {
        ResourceId rid;
        Element node = createNode("frontpage");
        if (isEnableFrontpage(account, domainName)) {
            try {
                rid = getHostingResource(account, domainName).getId().findChild("frontpage");
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
            if (rid == null) {
                return null;
            }
            Resource res = rid.get();
            if (res instanceof FrontPageResource) {
                FrontPageResource frontpage = (FrontPageResource) res;
                appendChildNode(node, createNode("login", frontpage.get("login").toString()));
                appendChildNode(node, createNode("password", frontpage.get("password").toString()));
                return checkChildren(node);
            } else if (res instanceof psoft.hsphere.resource.IIS.FrontPageResource) {
                boolean isON = isEnableFrontpage(account, domainName);
                return createNodeWithValueAttr("frontpage", isON);
            } else {
                throw new Exception(this.unsupported);
            }
        }
        return null;
    }

    private Element getASP(Account account, String domainName) throws Exception {
        boolean isON = isEnableASP(account, domainName);
        Element node = createNodeWithValueAttr("asp", isON);
        if (node != null) {
            Element child = createNodeWithValueAttr("asp_secured", isON);
            node.appendChild(child);
        }
        return checkChildren(node);
    }

    private Element getASPNet(Account account, String domainName) throws Exception {
        boolean isON = isEnableASPNet(account, domainName);
        return createNodeWithValueAttr("asp_net", isON);
    }

    private Element getColdFusion(Account account, String domainName) throws Exception {
        boolean isON;
        Element node = null;
        try {
            isON = isEnableColdFusion(account, domainName);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (isON) {
            node = createNode("cf");
            ResourceId rid = getHostingResource(account, domainName).getId().findChild("cf");
            Collection<ResourceId> items = rid.findChildren("cfentry");
            for (ResourceId entryId : items) {
                CFEntry entry = (CFEntry) entryId.get();
                String item = entry.get("ext").toString();
                Element child = createNode("listitem", item);
                appendChildNode(node, child);
            }
            return checkChildren(node);
        }
        return null;
    }

    private Element getMSSQLManager(Account account, String domainName) throws Exception {
        boolean isON = isEnableMSSQLManager(account, domainName);
        return createNodeWithValueAttr("mssqlmanager", isON);
    }

    private Element getMivaEmpresaEngine(Account account, String domainName) throws Exception {
        boolean isON = isEnableMivaEmpresaEngine(account, domainName);
        return createNodeWithValueAttr("empresa", isON);
    }

    private Element getMivaShoppingCart(Account account, String domainName) throws Exception {
        boolean isON = isEnableMivaShoppingCart(account, domainName);
        return createNodeWithValueAttr("miva", isON);
    }

    protected Resource getHostingResource(Account account, String domain) throws Exception {
        Resource dom = Utils.getDomain(account, domain).getId().findChild("hosting").get();
        return dom;
    }

    private boolean isResourceEnabled(Account a, String domainName, String type) throws Exception {
        try {
            ResourceId rid = getHostingResource(a, domainName).getId().findChild(type);
            if (rid == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnablePHP(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "php3");
    }

    public boolean isEnableFrontpage(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "frontpage");
    }

    public NamedParameter[] getWebSettings(Account a, String domain) throws Exception {
        VHostSettings settings = (VHostSettings) getHostingResource(a, domain);
        try {
            return NamedParameter.getParameters(settings.getSettings());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEnableSSL(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "ssl");
    }

    public boolean isEnableSharedSSL(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "sharedssl");
    }

    public boolean isEnableSSI(Account a, String domainName) throws Exception {
        boolean res = isResourceEnabled(a, domainName, "ssi");
        if (!res) {
            res = isResourceEnabled(a, domainName, "ssi_entry");
        }
        return res;
    }

    public boolean isEnableDirectoryIndexes(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "directory_ind");
    }

    public boolean isEnableErrorLog(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "errorlog");
    }

    public boolean isEnableTransferLog(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "transferlog");
    }

    public boolean isEnableWebalizer(Account a, String domainName) throws Exception {
        ResourceId transId = null;
        try {
            transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        } catch (Exception e) {
        }
        if (transId != null) {
            ResourceId rid = transId.findChild("webalizer");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isEnableReferrerLog(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "referrerlog");
    }

    public boolean isEnableModLogAn(Account a, String domainName) throws Exception {
        ResourceId transId = null;
        try {
            transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        } catch (Exception e) {
        }
        if (transId != null) {
            ResourceId rid = transId.findChild("modlogan");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isEnableAgentLog(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "agentlog");
    }

    public boolean isEnableMivaEmpresaEngine(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "empresa");
    }

    public boolean isEnableMivaShoppingCart(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "miva");
    }

    public boolean isEnableOsCommerce(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "oscommerce");
    }

    public boolean isEnableASP(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "asp");
    }

    public boolean isEnableASPSecuredLicense(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "asp_secured_license");
    }

    public boolean isEnableASPNet(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "asp_net");
    }

    public boolean isEnableColdFusion(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "cf");
    }

    public boolean isEnablePhpBB(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "phpbb");
    }

    public boolean isEnableMnoGoSearch(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "mnogosearch");
    }

    public boolean isEnableThrottlePolicy(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "throttle");
    }

    public boolean isEnableUrchin3Statistics(Account a, String domainName) throws Exception {
        ResourceId transId = null;
        try {
            transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        } catch (Exception e) {
        }
        if (transId != null) {
            ResourceId rid = transId.findChild("urchin");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isEnableUrchin4Statistics(Account a, String domainName) throws Exception {
        ResourceId transId = null;
        try {
            transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        } catch (Exception e) {
        }
        if (transId != null) {
            ResourceId rid = transId.findChild("urchin4");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isEnableInstantAccessDomainAlias(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "idomain_alias");
    }

    public boolean isEnableMSSQLManager(Account a, String domainName) throws Exception {
        return isResourceEnabled(a, domainName, "mssqlmanager");
    }
}
