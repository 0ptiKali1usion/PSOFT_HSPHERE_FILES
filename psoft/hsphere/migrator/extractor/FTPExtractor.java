package psoft.hsphere.migrator.extractor;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.axis.Utils;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.ftp.FTPVHostAnonymousResource;
import psoft.hsphere.resource.ftp.FTPVHostDirectoryResource;
import psoft.hsphere.resource.ftp.FTPVHostResource;
import psoft.hsphere.resource.ftp.FTPVHostUserResource;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/FTPExtractor.class */
public class FTPExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(FTPExtractor.class.getName());
    private boolean force;

    public FTPExtractor(Document doc, boolean force) {
        super(doc);
        this.force = force;
    }

    public Element getAllFTPInfoAsXML(Domain domain) throws Exception {
        return getFTP(domain);
    }

    private Element getFTP(Domain domain) throws Exception {
        Account account = domain.getAccount();
        String domainName = domain.getName();
        Element node = createNode("ftp");
        Element unixVhost = getFtpVhost(account, domainName);
        if (unixVhost == null) {
            appendChildNode(node, getWinFtpAnonymousVhost(account, domainName));
        } else {
            appendChildNode(node, unixVhost);
        }
        return checkChildren(node);
    }

    private Element getFtpVhost(Account account, String domain) throws Exception {
        Domain dom;
        ResourceId vhostId;
        Element node = createNode("ftpvhost");
        try {
            dom = Utils.getDomain(account, domain);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (dom == null || (vhostId = dom.getId().findChild("ftp_vhost")) == null) {
            return null;
        }
        FTPVHostResource vhost = (FTPVHostResource) vhostId.get();
        node.setAttributeNode(createAttribute("server", vhost.get("serverName").toString()));
        node.setAttributeNode(createAttribute(FMACLManager.ADMIN, vhost.get("serverAdmin").toString()));
        appendChildNode(node, getUnixFtpAnonymousVhost(account, domain));
        getVFTPhostDirectories(node, vhost);
        getVFTPhostUsers(node, vhost);
        return checkChildren(node);
    }

    private void getVFTPhostDirectories(Element parent, FTPVHostResource vhost) throws Exception {
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_directory")) {
            FTPVHostDirectoryResource dir = (FTPVHostDirectoryResource) resourceId.get();
            if (dir != null) {
                appendChildNode(parent, getFTPVhostDirectory(dir));
            }
        }
    }

    private Element getFTPVhostDirectory(FTPVHostDirectoryResource dir) throws Exception {
        Element node = createNode("ftpdirectory");
        try {
            node.setAttributeNode(createAttribute("name", dir.get("name").toString()));
            node.setAttributeNode(createAttribute("read", dir.get("r").toString()));
            node.setAttributeNode(createAttribute("write", dir.get("w").toString()));
            node.setAttributeNode(createAttribute("list", dir.get("l").toString()));
            node.setAttributeNode(createAttribute("forall", dir.get("forAll").toString()));
            getFTPVdirUsers(node, dir);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private void getFTPVdirUsers(Element parent, FTPVHostDirectoryResource dir) throws Exception {
        TemplateList users = dir.FM_userList();
        while (users.hasNext()) {
            ResourceId rid = users.next();
            FTPVHostUserResource user = (FTPVHostUserResource) rid.get();
            getFTPVdirUser(parent, user);
        }
    }

    private void getFTPVdirUser(Element parent, FTPVHostUserResource user) throws Exception {
        Element node = createNode("vdiruser");
        try {
            node.setAttribute("name", user.get("vlogin").toString());
            appendChildNode(parent, node);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
    }

    private void getVFTPhostUsers(Element parent, FTPVHostResource vhost) throws Exception {
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_user")) {
            FTPVHostUserResource user = (FTPVHostUserResource) resourceId.get();
            if (user != null) {
                appendChildNode(parent, getVFTPhostUser(user));
            }
        }
    }

    private Element getVFTPhostUser(FTPVHostUserResource user) throws Exception {
        Element node = createNode("ftpuser");
        try {
            node.setAttributeNode(createAttribute("login", user.get("vlogin").toString()));
            node.setAttributeNode(createAttribute("password", user.get("vpassword").toString()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getUnixFtpAnonymousVhost(Account account, String domain) throws Exception {
        Domain dom;
        ResourceId vhostId;
        ResourceId anonId;
        FTPVHostAnonymousResource vhost;
        Element node = createNode("unixanonymous");
        try {
            dom = Utils.getDomain(account, domain);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (dom == null || (vhostId = dom.getId().findChild("ftp_vhost")) == null || (anonId = vhostId.get().FM_findChild("ftp_vhost_anonymous")) == null || (vhost = (FTPVHostAnonymousResource) anonId.get()) == null) {
            return null;
        }
        node.setAttributeNode(createAttribute("value", "ON"));
        if (vhost.get("allowIncoming").toString().equals("1")) {
            node.setAttributeNode(createAttribute("upload", "ON"));
        } else {
            node.setAttributeNode(createAttribute("upload", "OFF"));
        }
        return checkChildren(node);
    }

    private Element getWinFtpAnonymousVhost(Account account, String domain) throws Exception {
        Domain dom;
        ResourceId vhostId;
        psoft.hsphere.resource.IIS.FTPVHostAnonymousResource vhost;
        Element node = createNode("winanonymous");
        try {
            dom = Utils.getDomain(account, domain);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (dom == null || (vhostId = dom.getId().findChild("ftp_vhost_anonymous")) == null || (vhost = (psoft.hsphere.resource.IIS.FTPVHostAnonymousResource) vhostId.get()) == null) {
            return null;
        }
        node.setAttributeNode(createAttribute("status", vhost.get("ftp_status").toString()));
        node.setAttributeNode(createAttribute("upload", vhost.get("ftp_upload").toString()));
        node.setAttributeNode(createAttribute("name", vhost.get("ftp_name").toString()));
        return checkChildren(node);
    }
}
