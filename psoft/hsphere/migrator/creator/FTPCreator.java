package psoft.hsphere.migrator.creator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.apache.log4j.Category;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.Utils;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.ftp.FTPVHostAnonymousResource;
import psoft.hsphere.resource.ftp.FTPVHostDirectoryResource;
import psoft.hsphere.resource.ftp.FTPVHostResource;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/FTPCreator.class */
public class FTPCreator {
    private static Category log = Category.getInstance(FTPCreator.class.getName());
    protected Account account;
    protected String domain;
    protected CommonUserCreator creator;
    private Hashtable users = new Hashtable();

    public FTPCreator(Account account, String domain, CommonUserCreator creator) {
        this.account = null;
        this.domain = null;
        this.creator = null;
        this.account = account;
        this.domain = domain;
        this.creator = creator;
    }

    public void addFTP(Element ftp) throws Exception {
        if (ftp == null) {
            return;
        }
        boolean isUnix = true;
        NodeList nodes = ftp.getElementsByTagName("ftpvhost");
        if (nodes.getLength() == 0) {
            nodes = ftp.getElementsByTagName("winanonymous");
            isUnix = false;
        }
        addFtpVhost((Element) nodes.item(0), isUnix);
    }

    private void addFtpVhost(Element ftpvhost, boolean isUnix) throws Exception {
        Domain dom = Utils.getDomain(this.account, this.domain);
        if (dom == null) {
            throw new Exception("No such ftp domain: " + this.domain);
        }
        ResourceId rid = dom.getId().findChild("ftp_vhost");
        if (rid == null) {
            if (((MixedIPResource) dom.getId().findChild("ip").get()).isDedicated()) {
                List values = new ArrayList();
                if (isUnix) {
                    NamedNodeMap attrs = ftpvhost.getAttributes();
                    String server = attrs.getNamedItem("server").getNodeValue();
                    values.add(server);
                    values.add(attrs.getNamedItem(FMACLManager.ADMIN).getNodeValue());
                    this.creator.outMessage("Adding FTP unix vhost - " + server);
                    ResourceId rid2 = dom.addChild("ftp_vhost", "", values);
                    this.creator.outOK();
                    FTPVHostResource vhost = (FTPVHostResource) rid2.get();
                    addVUsers(vhost, ftpvhost);
                    addVDirs(vhost, ftpvhost);
                    addUnixFTPAnonymous(vhost, ftpvhost);
                    return;
                }
                NamedNodeMap attrs2 = ftpvhost.getAttributes();
                String name = attrs2.getNamedItem("upload").getNodeValue();
                values.add(name);
                values.add(attrs2.getNamedItem("status").getNodeValue());
                values.add(attrs2.getNamedItem("name").getNodeValue());
                this.creator.outMessage("Adding FTP windows anonymous vhost - " + name);
                dom.addChild("ftp_vhost_anonymous", "", values);
                this.creator.outOK();
                return;
            }
            throw new Exception("IP is not dedicated");
        }
    }

    private void addVDirs(FTPVHostResource vhost, Element ftpvhost) throws Exception {
        NodeList dirs = ftpvhost.getElementsByTagName("ftpdirectory");
        for (int i = 0; i < dirs.getLength(); i++) {
            Element dir = (Element) dirs.item(i);
            addVdir(vhost, dir);
        }
    }

    private void addVUsers(FTPVHostResource vhost, Element ftpvhost) throws Exception {
        NodeList users = ftpvhost.getElementsByTagName("ftpuser");
        for (int i = 0; i < users.getLength(); i++) {
            Element user = (Element) users.item(i);
            addVuser(vhost, user);
        }
    }

    private void addVuser(FTPVHostResource vhost, Element ftpvuser) throws Exception {
        List values = new ArrayList();
        String name = null;
        try {
            NamedNodeMap attrs = ftpvuser.getAttributes();
            name = attrs.getNamedItem("login").getNodeValue();
            values.add(name);
            values.add(attrs.getNamedItem("password").getNodeValue());
            this.creator.outMessage("Adding FTP user - " + name);
            ResourceId rid = vhost.addChild("ftp_vhost_user", "", values);
            this.creator.outOK();
            this.users.put(name, rid.getAsString());
        } catch (Exception ex) {
            this.creator.outFail("Failed to create FTP user - " + name, ex);
            Session.getLog().error("Failed to create FTP user - " + name, ex);
            throw ex;
        }
    }

    private void addVdir(FTPVHostResource vhost, Element ftpvdir) throws Exception {
        List values = new ArrayList();
        String name = null;
        try {
            NamedNodeMap attrs = ftpvdir.getAttributes();
            name = attrs.getNamedItem("name").getNodeValue();
            values.add(name);
            values.add(attrs.getNamedItem("read").getNodeValue());
            values.add(attrs.getNamedItem("write").getNodeValue());
            values.add(attrs.getNamedItem("list").getNodeValue());
            values.add(attrs.getNamedItem("forall").getNodeValue());
            this.creator.outMessage("Adding FTP directory - " + name);
            ResourceId rid = vhost.addChild("ftp_vhost_directory", "", values);
            this.creator.outOK();
            addVdirUsers((FTPVHostDirectoryResource) rid.get(), ftpvdir);
        } catch (Exception ex) {
            this.creator.outFail("Failed to create FTP directory - " + name, ex);
            Session.getLog().error("Failed to create FTP directory - " + name, ex);
            throw ex;
        }
    }

    private void addVdirUsers(FTPVHostDirectoryResource vdir, Element ftpvdir) throws Exception {
        NodeList vusers = ftpvdir.getElementsByTagName("vdiruser");
        for (int i = 0; i < vusers.getLength(); i++) {
            Element vuser = (Element) vusers.item(i);
            addVdirUser(vdir, vuser);
        }
    }

    private void addVdirUser(FTPVHostDirectoryResource vdir, Element vdiruser) throws Exception {
        NamedNodeMap vuser = vdiruser.getAttributes();
        String user = null;
        try {
            user = vuser.getNamedItem("name").getNodeValue();
            this.creator.outMessage("Adding FTP virtual directory user - " + user);
            vdir.FM_addUser((String) this.users.get(user));
            this.creator.outOK();
        } catch (Exception ex) {
            this.creator.outFail("Failed to create FTP virtual directory user - " + user, ex);
            Session.getLog().error("Failed to create FTP virtual directory user - " + user, ex);
            throw ex;
        }
    }

    private void addUnixFTPAnonymous(FTPVHostResource vhost, Element ftpvhost) throws Exception {
        List values = new ArrayList();
        try {
            NodeList nodes = ftpvhost.getElementsByTagName("unixanonymous");
            Element node = (Element) nodes.item(0);
            this.creator.outMessage("Adding anonymous FTP ");
            ResourceId rid = vhost.addChild("ftp_vhost_anonymous", "", values);
            NamedNodeMap attrs = node.getAttributes();
            String upload = attrs.getNamedItem("upload").getNodeValue();
            if (upload.equalsIgnoreCase("ON")) {
                FTPVHostAnonymousResource anonymous = (FTPVHostAnonymousResource) rid.get();
                anonymous.FM_update(1);
            }
            this.creator.outOK();
        } catch (Exception ex) {
            this.creator.outFail("Failed to create anonymous FTP ", ex);
            Session.getLog().error("Failed to create anonymous FTP ", ex);
            throw ex;
        }
    }
}
