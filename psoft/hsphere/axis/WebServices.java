package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.IIS.CFEntry;
import psoft.hsphere.resource.IIS.PHPEntry;
import psoft.hsphere.resource.IIS.SSIEntry;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.VHostSettings;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.apache.CGIDirResource;
import psoft.hsphere.resource.apache.CGIResource;
import psoft.hsphere.resource.apache.DirectoryIndexResource;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.hsphere.resource.apache.FrontPageResource;
import psoft.hsphere.resource.apache.ISMapResource;
import psoft.hsphere.resource.apache.MimeTypeResource;
import psoft.hsphere.resource.apache.ModloganResource;
import psoft.hsphere.resource.apache.PHP3Resource;
import psoft.hsphere.resource.apache.RedirectResource;
import psoft.hsphere.resource.apache.SSIResource;
import psoft.hsphere.resource.apache.SSLResource;
import psoft.hsphere.resource.apache.ServerAliasResource;
import psoft.hsphere.resource.apache.ThrottleResource;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.dns.ThirdLevelDNSZone;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.hsphere.resource.mysql.MySQLUser;

/* loaded from: hsphere.zip:psoft/hsphere/axis/WebServices.class */
public class WebServices {
    private static Category log = Category.getInstance(WebServices.class.getName());
    protected final int UNIX_PLATFORM = 1;
    protected final int WIN_PLATFORM = 2;

    public String getDescription() {
        return "Functions to work with web";
    }

    private int getPlatformType(Account account) throws Exception {
        return account.getId().findChild("unixuser").get() instanceof WinUserResource ? 2 : 1;
    }

    protected Resource getHostingResource(Account a, String domain) throws Exception {
        return Utils.getDomain(a, domain).getId().findChild("hosting").get();
    }

    protected String getDomainType(Domain domain) throws Exception {
        return domain.getId().getNamedType();
    }

    public String dedicatedIpTypeName(String domainType) throws Exception {
        return "nodomain".equals(domainType) ? "dedic_no_a" : "dedicated";
    }

    public String sharedIpTypeName(String domainType) throws Exception {
        return "nodomain".equals(domainType) ? "shard_no_a" : "shared";
    }

    private void deleteIPsList(Domain domain, ResourceId ip) throws Exception {
        Collection<ResourceId> ips = domain.getId().findChildren("ip");
        for (ResourceId rid : ips) {
            if (!rid.equals(ip)) {
                rid.get().delete(true);
            }
        }
    }

    private void reconfigure(ResourceId oldIP, ResourceId newIP) throws Exception {
        if (newIP != null) {
            try {
                oldIP.get().FM_cdelete(0);
                ((MixedIPResource) newIP.get()).FM_reconfig();
                return;
            } catch (Exception e) {
                newIP.get().FM_cdelete(0);
                throw new Exception("Unable change ip type");
            }
        }
        throw new Exception("Unable change ip type");
    }

    public void changeIpToShared(AuthToken at, String domainName) throws Exception {
        Domain domain = Utils.getDomain(Utils.getAccount(at), domainName);
        Resource oldIP = domain.getId().findChild("ip").get();
        if (((MixedIPResource) oldIP).isDedicated()) {
            deleteIPsList(domain, oldIP.getId());
            ((MixedIPResource) oldIP).FM_ipdelete();
            List values = new ArrayList();
            ResourceId newIP = domain.addChild("ip", sharedIpTypeName(getDomainType(domain)), values);
            reconfigure(oldIP.getId(), newIP);
            return;
        }
        throw new HSUserException("Unable change to shared IP.");
    }

    public void changeIpToDedicated(AuthToken at, String domainName) throws Exception {
        Domain domain = Utils.getDomain(Utils.getAccount(at), domainName);
        Resource oldIP = domain.getId().findChild("ip").get();
        if (!((MixedIPResource) oldIP).isDedicated()) {
            deleteIPsList(domain, oldIP.getId());
            ((MixedIPResource) oldIP).FM_ipdelete();
            List values = new ArrayList();
            ResourceId newIP = domain.addChild("ip", dedicatedIpTypeName(getDomainType(domain)), values);
            reconfigure(oldIP.getId(), newIP);
            return;
        }
        throw new HSUserException("Unable change to dedicated IP.");
    }

    public String getIpType(AuthToken at, String domainName) throws Exception {
        Domain domain = Utils.getDomain(Utils.getAccount(at), domainName);
        return ((MixedIPResource) domain.getId().findChild("ip").get()).isDedicated() ? "dedicated" : "shared";
    }

    public String getIP(AuthToken at, String domain) throws Exception {
        Domain d = Utils.getDomain(Utils.getAccount(at), domain);
        return ((MixedIPResource) d.getId().findChild("ip").get()).toString();
    }

    public void addCgiExt(AuthToken at, String extention, String handler, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(extention);
        values.add(handler);
        getHostingResource(a, domain).addChild("cgi", "", values);
    }

    public void deleteCgiExt(AuthToken at, String extention, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> col = getHostingResource(a, domain).getId().findAllChildren("cgi");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (r instanceof CGIResource) {
                if (((CGIResource) r).getExt().equalsIgnoreCase(extention)) {
                    r.FM_cdelete(0);
                    return;
                }
            } else if ((r instanceof psoft.hsphere.resource.IIS.CGIResource) && ((psoft.hsphere.resource.IIS.CGIResource) r).getExt().equalsIgnoreCase(extention)) {
                r.FM_cdelete(0);
                return;
            }
        }
    }

    public void addCgiDir(AuthToken at, String path, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(path);
        getHostingResource(a, domain).addChild("cgidir", "", values);
    }

    public void deleteCgiDir(AuthToken at, String path, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> col = getHostingResource(a, domain).getId().findAllChildren("cgidir");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (r instanceof CGIDirResource) {
                if (((CGIDirResource) r).getDir().equalsIgnoreCase(path)) {
                    r.FM_cdelete(0);
                    return;
                }
            } else if (r instanceof psoft.hsphere.resource.IIS.CGIDirResource) {
                r.FM_cdelete(0);
                return;
            }
        }
    }

    public void addPHP(AuthToken at, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domain).addChild("php3", "", values);
    }

    public void deletePHP(AuthToken at, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        getHostingResource(a, domain).getId().findChild("php3").get().FM_cdelete(0);
    }

    public boolean isEnablePHP(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "php3");
    }

    public void addPHPExt(AuthToken at, String extention, String mime, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(extention);
        values.add(mime);
        getHostingResource(a, domain).getId().findChild("php3").get().addChild("php3entry", "", values);
    }

    public void deletePHPExt(AuthToken at, String extention, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> col = getHostingResource(a, domain).getId().findChild("php3").findChildren("php3entry");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (getPlatformType(a) == 1) {
                if (((PHP3Resource) r).getExt().equalsIgnoreCase(extention)) {
                    r.FM_cdelete(0);
                    return;
                }
            } else if (((PHPEntry) r).getExt().equalsIgnoreCase(extention)) {
                r.FM_cdelete(0);
                return;
            }
        }
    }

    public void addMIMEType(AuthToken at, String extention, String mime, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(extention);
        values.add(mime);
        getHostingResource(a, domain).addChild("mimetype", "", values);
    }

    public void deleteMIMEType(AuthToken at, String extention, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> col = getHostingResource(a, domain).getId().findChildren("mimetype");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (getPlatformType(a) == 1) {
                if (((MimeTypeResource) r).getExt().equalsIgnoreCase(extention)) {
                    r.FM_cdelete(0);
                    return;
                }
            } else if (((psoft.hsphere.resource.IIS.MimeTypeResource) r).getExt().equalsIgnoreCase(extention)) {
                r.FM_cdelete(0);
                return;
            }
        }
    }

    public void addFrontpage(AuthToken at, String login, String password, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(login);
        values.add(password);
        getHostingResource(a, domain).addChild("frontpage", "", values);
    }

    public void updateFrontpage(AuthToken at, String login, String password, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(login);
        values.add(password);
        Resource r = getHostingResource(a, domain).getId().findChild("frontpage").get();
        if (getPlatformType(a) == 1) {
            ((FrontPageResource) r).FM_update(login, password);
        }
    }

    public boolean isEnableFrontpage(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "frontpage");
    }

    public void tuneFrontpage(AuthToken at, String mailSender, String mailReplyTo, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(mailSender);
        values.add(mailReplyTo);
        Resource r = getHostingResource(a, domain).getId().findChild("frontpage").get();
        if (getPlatformType(a) == 1) {
            ((FrontPageResource) r).setProperties(mailSender, mailReplyTo);
        }
    }

    public NamedParameter[] getWebSettings(AuthToken at, String domain) throws Exception {
        Account a = Utils.getAccount(at);
        return NamedParameter.getParameters(((VHostSettings) getHostingResource(a, domain)).getSettings());
    }

    public void setWebSettings(AuthToken at, String domain, NamedParameter[] settings) throws Exception {
        Account a = Utils.getAccount(at);
        ((VHostSettings) getHostingResource(a, domain)).setSettings(NamedParameter.getParametersAsHashMap(settings));
    }

    public void addUnixSSL(AuthToken at, String domainName, String privateKey, String cert, String siteName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(privateKey);
        values.add(cert);
        values.add(siteName);
        getHostingResource(a, domainName).addChild("ssl", "", values);
    }

    public void addWinSSL(AuthToken at, String domainName, int forced, int need128, String privateKey, String cert, String siteName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(Integer.toString(forced));
        values.add(Integer.toString(need128));
        values.add(privateKey);
        values.add(cert);
        values.add(siteName);
        getHostingResource(a, domainName).addChild("ssl", "", values);
    }

    public boolean isResourceEnabled(AuthToken at, String domainName, String type) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild(type);
        if (rid == null) {
            return false;
        }
        return true;
    }

    public boolean isEnableSSL(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "ssl");
    }

    public void delSSL(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("ssl");
        if (rid == null) {
            throw new Exception("Not found ssl");
        }
        rid.get().FM_cdelete(0);
    }

    private Resource findSSL(Collection ssls, String siteName) throws Exception {
        Iterator i = ssls.iterator();
        while (i.hasNext()) {
            ResourceId rid = (ResourceId) i.next();
            Resource res = rid.get();
            if (res instanceof SSLResource) {
                SSLResource ssl = (SSLResource) res;
                if (ssl.get("site_name").toString().equals(siteName)) {
                    return res;
                }
            } else if (res instanceof psoft.hsphere.resource.IIS.SSLResource) {
                psoft.hsphere.resource.IIS.SSLResource ssl2 = (psoft.hsphere.resource.IIS.SSLResource) res;
                if (ssl2.get("site_name").toString().equals(siteName)) {
                    return res;
                }
            } else {
                continue;
            }
        }
        throw new Exception("Not found ssl for site - " + siteName);
    }

    public void updateSSL(AuthToken at, String domainName, String updateType, String siteName, String key, String file) throws Exception {
        Account a = Utils.getAccount(at);
        Collection ssls = getHostingResource(a, domainName).getId().findChildren("ssl");
        Resource res = findSSL(ssls, siteName);
        if (res instanceof SSLResource) {
            SSLResource ssl = (SSLResource) res;
            if (updateType.equals("cert_only")) {
                ssl.FM_installCertificateOnly(file);
            } else if (updateType.equals(MerchantGatewayManager.MG_KEY_PREFIX)) {
                ssl.FM_installCertificate(file, key);
            } else if (updateType.equals("rev")) {
                ssl.FM_installRevFile(file);
            } else if (updateType.equals(MerchantGatewayManager.MG_CERT_PREFIX)) {
                ssl.FM_installCertFile(file);
            } else if (updateType.equals("chain")) {
                ssl.FM_installChainFile(file);
            } else if (updateType.equals("remove_rev")) {
                ssl.FM_removeRevFile();
            } else if (updateType.equals("remove_cert")) {
                ssl.FM_removeCertFile();
            } else if (updateType.equals("remove_chain")) {
                ssl.FM_removeChainFile();
            } else {
                throw new Exception("Unknown action - " + updateType);
            }
        } else if (res instanceof psoft.hsphere.resource.IIS.SSLResource) {
            psoft.hsphere.resource.IIS.SSLResource ssl2 = (psoft.hsphere.resource.IIS.SSLResource) res;
            if (updateType.equals(MerchantGatewayManager.MG_CERT_PREFIX)) {
                ssl2.FM_installCertFile(file);
            } else if (updateType.equals("update_cert")) {
                ssl2.FM_updateCert(file);
            } else if (updateType.equals(MerchantGatewayManager.MG_KEY_PREFIX)) {
                ssl2.FM_updatePair(key, file);
            } else {
                throw new Exception("Unknown action - " + updateType);
            }
        }
    }

    public void updateWinSSLOptions(AuthToken at, String domainName, int forced, int need128) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("ssl");
        if (rid == null) {
            throw new Exception("Not found ssl");
        }
        Resource res = rid.get();
        psoft.hsphere.resource.IIS.SSLResource ssl = (psoft.hsphere.resource.IIS.SSLResource) res;
        ssl.FM_updateSettings(forced, need128);
    }

    public void addSharedSSL(AuthToken at, String domainName, String name) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(name);
        getHostingResource(a, domainName).addChild("sharedssl", "", values);
    }

    public void delSharedSSL(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("sharedssl");
        if (rid == null) {
            throw new Exception("Not found shared ssl");
        }
        Resource res = rid.get();
        res.FM_cdelete(0);
    }

    public boolean isEnableSharedSSL(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "sharedssl");
    }

    public void addSSI(AuthToken at, String domainName, String fileExt) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        int platform = getPlatformType(a);
        if (platform == 1) {
            values.add(fileExt);
            getHostingResource(a, domainName).addChild("ssi", "", values);
        } else if (platform == 2) {
            ResourceId rid = getHostingResource(a, domainName).getId().findChild("ssi");
            if (rid == null) {
                rid = getHostingResource(a, domainName).addChild("ssi", "", values);
            }
            values.add(fileExt);
            rid.get().addChild("ssi_entry", "", values);
        } else {
            throw new Exception("Unsupported platform");
        }
    }

    public void delSSI(AuthToken at, String domainName, String fileExt) throws Exception {
        Collection<ResourceId> ssis;
        Account a = Utils.getAccount(at);
        int platform = getPlatformType(a);
        if (platform == 1) {
            ssis = getHostingResource(a, domainName).getId().findChildren("ssi");
        } else if (platform == 2) {
            ssis = getHostingResource(a, domainName).getId().findChildren("ssi_entry");
        } else {
            throw new Exception("Unsupported platform");
        }
        for (ResourceId rid : ssis) {
            if (rid == null) {
                throw new Exception("Not found ssi");
            }
            Resource res = rid.get();
            if (res instanceof SSIResource) {
                SSIResource ssi = (SSIResource) rid.get();
                if (ssi.getExt().equals(fileExt)) {
                    ssi.FM_cdelete(0);
                    return;
                }
            } else if (res instanceof SSIEntry) {
                SSIEntry entry = (SSIEntry) rid.get();
                if (entry.getExt().equals(fileExt)) {
                    entry.FM_cdelete(0);
                    return;
                }
            } else {
                continue;
            }
        }
        throw new Exception("Not found ssi extension - " + fileExt);
    }

    public boolean isEnableSSI(AuthToken at, String domainName) throws Exception {
        boolean res = isResourceEnabled(at, domainName, "ssi");
        if (!res) {
            res = isResourceEnabled(at, domainName, "ssi_entry");
        }
        return res;
    }

    public void addUnixRedirect(AuthToken at, String domainName, String status, String urlPath, String url, String protocol) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(status);
        values.add(urlPath);
        values.add(url);
        values.add(protocol);
        getHostingResource(a, domainName).addChild("redirect_url", "", values);
    }

    public void updateUnixRedirect(AuthToken at, String domainName, String status, String urlPath, String url, String protocol) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> redirects = getHostingResource(a, domainName).getId().findChildren("redirect_url");
        for (ResourceId rid : redirects) {
            if (rid == null) {
                throw new Exception("Not found redirect");
            }
            rid.get();
            RedirectResource redirect = (RedirectResource) rid.get();
            if (redirect.get("url_path").toString().equals(urlPath)) {
                redirect.FM_update(status, urlPath, url, protocol);
                return;
            }
        }
        throw new Exception("Not found redirect for - " + urlPath);
    }

    public void addWinRedirect(AuthToken at, String domainName, String urlPath, String url, String exact, String below, String perm) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(exact);
        values.add(below);
        values.add(perm);
        values.add(urlPath);
        values.add(url);
        getHostingResource(a, domainName).addChild("redirect_url", "", values);
    }

    public void updateWinRedirect(AuthToken at, String domainName, String urlPath, String url, String exact, String below, String perm) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> redirects = getHostingResource(a, domainName).getId().findChildren("redirect_url");
        for (ResourceId rid : redirects) {
            if (rid == null) {
                throw new Exception("Not found redirect");
            }
            rid.get();
            psoft.hsphere.resource.IIS.RedirectResource redirect = (psoft.hsphere.resource.IIS.RedirectResource) rid.get();
            if (redirect.get("url_path").toString().equals(urlPath)) {
                redirect.FM_update(exact, below, perm, urlPath, url);
                return;
            }
        }
        throw new Exception("Not found redirect for - " + urlPath);
    }

    public void delRedirect(AuthToken at, String domainName, String url) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> redirects = getHostingResource(a, domainName).getId().findChildren("redirect_url");
        for (ResourceId rid : redirects) {
            if (rid == null) {
                throw new Exception("Not found redirect");
            }
            Resource res = rid.get();
            if (res instanceof RedirectResource) {
                RedirectResource redirect = (RedirectResource) rid.get();
                if (redirect.get("url_path").toString().equals(url)) {
                    redirect.FM_cdelete(0);
                    return;
                }
            } else if (res instanceof psoft.hsphere.resource.IIS.RedirectResource) {
                psoft.hsphere.resource.IIS.RedirectResource redirect2 = (psoft.hsphere.resource.IIS.RedirectResource) rid.get();
                if (redirect2.get("url_path").toString().equals(url)) {
                    redirect2.FM_cdelete(0);
                    return;
                }
            } else {
                continue;
            }
        }
        throw new Exception("Not found redirect for - " + url);
    }

    public void addDirectoryIndexes(AuthToken at, String domainName, String indexes) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("directory_ind");
        if (rid != null) {
            delDirectoryIndexes(at, domainName);
        }
        List values = new ArrayList();
        values.add(indexes);
        getHostingResource(a, domainName).addChild("directory_ind", "", values);
    }

    public void updateDirectoryIndexes(AuthToken at, String domainName, String indexes) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("directory_ind");
        if (rid == null) {
            throw new Exception("Not found directory indexes");
        }
        addDirectoryIndexes(at, domainName, indexes);
    }

    public void delDirectoryIndexes(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("directory_ind");
        if (rid == null) {
            throw new Exception("Not found directory indexes");
        }
        Resource res = rid.get();
        if (res instanceof DirectoryIndexResource) {
            DirectoryIndexResource ind = (DirectoryIndexResource) rid.get();
            ind.FM_cdelete(0);
        } else if (res instanceof psoft.hsphere.resource.IIS.DirectoryIndexResource) {
            psoft.hsphere.resource.IIS.DirectoryIndexResource ind2 = (psoft.hsphere.resource.IIS.DirectoryIndexResource) rid.get();
            ind2.FM_cdelete(0);
        }
    }

    public boolean isEnableDirectoryIndexes(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "directory_ind");
    }

    public void addErrorLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("errorlog", "", values);
    }

    public void delErrorLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("errorlog");
        if (rid == null) {
            throw new Exception("Not found errorlog");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableErrorLog(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "errorlog");
    }

    public void addTransferLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("transferlog", "", values);
    }

    public void delTransferLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            throw new Exception("Not found transferlog");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableTransferLog(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "transferlog");
    }

    public void addWebalizer(AuthToken at, String domainName) throws Exception {
        List values = new ArrayList();
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId == null) {
            transId = getHostingResource(a, domainName).addChild("transferlog", "", values);
        }
        transId.get().addChild("webalizer", "", values);
    }

    public void delWebalizer(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId == null) {
            throw new Exception("Not found transferlog for webalizer");
        }
        ResourceId rid = transId.findChild("webalizer");
        if (rid == null) {
            throw new Exception("Not found transferlog for webalizer");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableWebalizer(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId != null) {
            ResourceId rid = transId.findChild("webalizer");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void addReferrerLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("referrerlog", "", values);
    }

    public void delReferrerLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("referrerlog");
        if (rid == null) {
            throw new Exception("Not found referrerlog");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableReferrerLog(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "referrerlog");
    }

    public void addErrorDoc(AuthToken at, String domainName, String errorCode, String errorMsg, int type) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(errorCode);
        values.add(errorMsg);
        values.add(Integer.toString(type));
        getHostingResource(a, domainName).addChild("errordoc", "", values);
    }

    public void updErrorDoc(AuthToken at, String domainName, String errorCode, String errorMsg, String type) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> errorDocs = getHostingResource(a, domainName).getId().findChildren("errordoc");
        for (ResourceId rid : errorDocs) {
            if (rid == null) {
                throw new Exception("Not found error document");
            }
            Resource res = rid.get();
            if (res instanceof ErrorDocumentResource) {
                ErrorDocumentResource doc = (ErrorDocumentResource) rid.get();
                if (doc.get("code").toString().equals(errorCode)) {
                    doc.FM_update(errorMsg, type);
                    return;
                }
            } else if (res instanceof psoft.hsphere.resource.IIS.ErrorDocumentResource) {
                psoft.hsphere.resource.IIS.ErrorDocumentResource doc2 = (psoft.hsphere.resource.IIS.ErrorDocumentResource) rid.get();
                if (doc2.get("code").toString().equals(errorCode)) {
                    doc2.FM_update(errorMsg, type);
                    return;
                }
            } else {
                continue;
            }
        }
        throw new Exception("Not found error document for - " + errorCode);
    }

    public void delErrorDoc(AuthToken at, String domainName, String errorCode) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> errorDocs = getHostingResource(a, domainName).getId().findChildren("errordoc");
        for (ResourceId rid : errorDocs) {
            if (rid == null) {
                throw new Exception("Not found error document");
            }
            Resource res = rid.get();
            if (res instanceof ErrorDocumentResource) {
                ErrorDocumentResource doc = (ErrorDocumentResource) rid.get();
                if (doc.get("code").toString().equals(errorCode)) {
                    doc.FM_cdelete(0);
                    return;
                }
            }
            if (res instanceof psoft.hsphere.resource.IIS.ErrorDocumentResource) {
                psoft.hsphere.resource.IIS.ErrorDocumentResource doc2 = (psoft.hsphere.resource.IIS.ErrorDocumentResource) rid.get();
                if (doc2.get("code").toString().equals(errorCode)) {
                    doc2.FM_cdelete(0);
                    return;
                }
            }
        }
        throw new Exception("Not found error document for - " + errorCode);
    }

    public void addUnixServerSideImagemap(AuthToken at, String domainName, String fileExt) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(fileExt);
        getHostingResource(a, domainName).addChild("ismap", "", values);
    }

    public void delUnixServerSideImagemap(AuthToken at, String domainName, String fileExt) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> errorDocs = getHostingResource(a, domainName).getId().findChildren("ismap");
        for (ResourceId rid : errorDocs) {
            if (rid == null) {
                throw new Exception("Not found error document");
            }
            rid.get();
            ISMapResource ismap = (ISMapResource) rid.get();
            if (ismap.getExt().equals(fileExt)) {
                ismap.FM_cdelete(0);
                return;
            }
        }
        throw new Exception("Not found ismap for - " + fileExt);
    }

    public void addUnixModLogAn(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            rid = getHostingResource(a, domainName).addChild("transferlog", "", values);
        }
        rid.get().addChild("modlogan", "", values);
    }

    public void delUnixModLogAn(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            throw new Exception("Not found transfer log");
        }
        ResourceId modloganId = rid.findChild("modlogan");
        if (modloganId == null) {
            throw new Exception("Not found ModLogAn");
        }
        ModloganResource modlogan = (ModloganResource) modloganId.get();
        modlogan.FM_cdelete(0);
    }

    public boolean isEnableModLogAn(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId != null) {
            ResourceId rid = transId.findChild("modlogan");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void addAgentLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("agentlog", "", values);
    }

    public void delAgentLog(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("agentlog");
        if (rid == null) {
            throw new Exception("Not found agent log");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableAgentLog(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "agentlog");
    }

    public void addMivaEmpresaEngine(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("cgidir");
        if (rid == null) {
            getHostingResource(a, domainName).addChild("cgidir", "", values);
        }
        getHostingResource(a, domainName).addChild("empresa", "", values);
    }

    public void delMivaEmpresaEngine(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("empresa");
        if (rid == null) {
            throw new Exception("Not found Miva Empresa Engine");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableMivaEmpresaEngine(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "empresa");
    }

    public void addMivaShoppingCart(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("empresa");
        if (rid == null) {
            addMivaEmpresaEngine(at, domainName);
        }
        getHostingResource(a, domainName).addChild("miva", "", values);
    }

    public void delMivaShoppingCart(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("miva");
        if (rid == null) {
            throw new Exception("Not found Miva Shopping Cart");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableMivaShoppingCart(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "miva");
    }

    public void addOsCommerce(AuthToken at, String domainName, String database, String user) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(getDatabase(a, database).getId().getAsString());
        values.add(getUser(a, user).getId().getAsString());
        getHostingResource(a, domainName).addChild("oscommerce", "", values);
    }

    public void delOsCommerce(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("oscommerce");
        if (rid == null) {
            throw new Exception("Not found osCommerce");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableOsCommerce(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "oscommerce");
    }

    public void addASP(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("asp", "", values);
        getHostingResource(a, domainName).addChild("asp_secured", "", values);
    }

    public void delASP(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId aspId = getHostingResource(a, domainName).getId().findChild("asp");
        if (aspId == null) {
            throw new Exception("Not found asp");
        }
        aspId.get().FM_cdelete(0);
        ResourceId aspSecuredId = getHostingResource(a, domainName).getId().findChild("asp_secured");
        if (aspSecuredId == null) {
            throw new Exception("Not found asp secured");
        }
        aspSecuredId.get().FM_cdelete(0);
    }

    public boolean isEnableASP(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "asp");
    }

    public void addASPSecuredLicense(AuthToken at, String domainName, String orderID) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(orderID);
        getHostingResource(a, domainName).addChild("asp_secured_license", "", values);
    }

    public void delASPSecuredLicense(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("asp_secured_license");
        if (rid == null) {
            throw new Exception("Not found asp secured license");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableASPSecuredLicense(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "asp_secured_license");
    }

    public void addASPNet(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("asp_net", "", values);
    }

    public void delASPNet(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("asp_net");
        if (rid == null) {
            throw new Exception("Not found ASP NET");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableASPNet(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "asp_net");
    }

    public void addColdFusion(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("cf", "", values);
    }

    public void delColdFusion(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("cf");
        if (rid == null) {
            throw new Exception("Not found ColdFusion");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableColdFusion(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "cf");
    }

    public void addColdFusionEntry(AuthToken at, String domainName, String fileExt) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("cf");
        if (rid == null) {
            throw new Exception("Not found ColdFusion");
        }
        List values = new ArrayList();
        values.add(fileExt);
        rid.get().addChild("cfentry", "", values);
    }

    public void delColdFusionEntry(AuthToken at, String domainName, String fileExt) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("cf");
        if (rid == null) {
            throw new Exception("Not found ColdFusion");
        }
        Collection<ResourceId> licenses = rid.findChildren("cfentry");
        for (ResourceId entryId : licenses) {
            CFEntry entry = (CFEntry) entryId.get();
            if (entry.get("ext").toString().equals(fileExt)) {
                entryId.get().FM_cdelete(0);
                return;
            }
        }
        throw new Exception("Not found ColdFusion extension");
    }

    private MySQLResource getMySQL(Account a) throws Exception {
        ResourceId rid = a.getId().findChild("MySQL");
        if (rid == null) {
            rid = a.addChild("MySQL", "", new ArrayList());
        }
        return (MySQLResource) rid.get();
    }

    private MySQLUser getUser(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMySQL(a).getId().findChildren("MySQLUser")) {
            MySQLUser user = (MySQLUser) resourceId.get();
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        throw new Exception("No such user: " + name);
    }

    private MySQLDatabase getDatabase(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMySQL(a).getId().findChildren("MySQLDatabase")) {
            MySQLDatabase db = (MySQLDatabase) resourceId.get();
            if (db.getName().equalsIgnoreCase(name)) {
                return db;
            }
        }
        throw new Exception("No such db: " + name);
    }

    public void addPhpBB(AuthToken at, String domainName, String database, String user) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(getDatabase(a, database).getId().getAsString());
        values.add(getUser(a, user).getId().getAsString());
        getHostingResource(a, domainName).addChild("phpbb", "", values);
    }

    public void delPhpBB(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("phpbb");
        if (rid == null) {
            throw new Exception("Not found phpBB");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnablePhpBB(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "phpbb");
    }

    public void addUnixMnoGoSearch(AuthToken at, String domainName, String database, String user) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(getDatabase(a, database).getId().getAsString());
        values.add(getUser(a, user).getId().getAsString());
        getHostingResource(a, domainName).addChild("mnogosearch", "", values);
    }

    public void delUnixMnoGoSearch(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("mnogosearch");
        if (rid == null) {
            throw new Exception("Not found mnoGoSearch");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableMnoGoSearch(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "mnogosearch");
    }

    public void addUnixThrottlePolicyForCDRIR(AuthToken at, String domainName, String type, String limit, String interv, String intervUn) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(type);
        values.add(limit);
        values.add("");
        values.add(interv);
        values.add(intervUn);
        getHostingResource(a, domainName).addChild("throttle", "", values);
    }

    public void updUnixThrottlePolicy(AuthToken at, String domainName, String type, String limit, String limitUn, String interv, String intervUn) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("throttle");
        if (rid == null) {
            throw new Exception("Not found Throttle Policy");
        }
        ThrottleResource throttle = (ThrottleResource) rid.get();
        throttle.FM_update(type, limit, limitUn, interv, intervUn);
    }

    public void addUnixThrottlePolicyForOSV(AuthToken at, String domainName, String type, String limit, String limitUn, String interv, String intervUn) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(type);
        values.add(limit);
        values.add(limitUn);
        values.add(interv);
        values.add(intervUn);
        getHostingResource(a, domainName).addChild("throttle", "", values);
    }

    public void addUnixThrottlePolicyForNone(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add("None");
        getHostingResource(a, domainName).addChild("throttle", "", values);
    }

    public void delUnixThrottlePolicy(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("throttle");
        if (rid == null) {
            throw new Exception("Not found Throttle Policy");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableThrottlePolicy(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "throttle");
    }

    public void addUrchin3Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            rid = getHostingResource(a, domainName).addChild("transferlog", "", values);
        }
        rid.get().addChild("urchin", "", values);
    }

    public void delUrchin3Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            throw new Exception("Not found trasfer log");
        }
        ResourceId urchin3 = rid.findChild("urchin");
        if (urchin3 == null) {
            throw new Exception("Not found Urchin3");
        }
        urchin3.get().FM_cdelete(0);
    }

    public boolean isEnableUrchin3Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId != null) {
            ResourceId rid = transId.findChild("urchin");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void addUrchin4Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            rid = getHostingResource(a, domainName).addChild("transferlog", "", values);
        }
        rid.get().addChild("urchin4", "", values);
    }

    public void delUrchin4Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        new ArrayList();
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (rid == null) {
            throw new Exception("Not found trasfer log");
        }
        ResourceId urchin4 = rid.findChild("urchin4");
        if (urchin4 == null) {
            throw new Exception("Not found Urchin4");
        }
        urchin4.get().FM_cdelete(0);
    }

    public boolean isEnableUrchin4Statistics(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId transId = getHostingResource(a, domainName).getId().findChild("transferlog");
        if (transId != null) {
            ResourceId rid = transId.findChild("urchin4");
            if (rid == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void addVhostAlias(AuthToken at, String domainName, String alias) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(alias);
        getHostingResource(a, domainName).addChild("vhost_alias", "", values);
    }

    public void delVhostAlias(AuthToken at, String domainName, String alias) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> aliases = getHostingResource(a, domainName).getId().findChildren("vhost_alias");
        for (ResourceId rid : aliases) {
            if (rid == null) {
                throw new Exception("Not found domain alias - " + alias);
            }
            Resource res = rid.get();
            if (res instanceof ServerAliasResource) {
                ServerAliasResource al = (ServerAliasResource) res;
                if (al.get("alias").toString().equals(alias)) {
                    rid.get().FM_cdelete(0);
                    return;
                }
            } else if (res instanceof psoft.hsphere.resource.IIS.ServerAliasResource) {
                psoft.hsphere.resource.IIS.ServerAliasResource al2 = (psoft.hsphere.resource.IIS.ServerAliasResource) res;
                if (al2.get("alias").toString().equals(alias)) {
                    rid.get().FM_cdelete(0);
                    return;
                }
            } else {
                continue;
            }
        }
        throw new Exception("Not found domain alias - " + alias);
    }

    public void addInstantAccessDomainAlias(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        getHostingResource(a, domainName).addChild("idomain_alias", "", values);
    }

    public void delInstantAccessDomainAlias(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = getHostingResource(a, domainName).getId().findChild("idomain_alias");
        if (rid == null) {
            throw new Exception("Not found instant access domain alias");
        }
        rid.get().FM_cdelete(0);
    }

    public boolean isEnableInstantAccessDomainAlias(AuthToken at, String domainName) throws Exception {
        return isResourceEnabled(at, domainName, "idomain_alias");
    }

    public String getInstantAccessDomainAlias(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId alias_rId = getHostingResource(a, domainName).getId().findChild("idomain_alias");
        psoft.hsphere.resource.IIS.ServerAliasResource alias = (psoft.hsphere.resource.IIS.ServerAliasResource) alias_rId.get();
        return alias.getDescription();
    }

    public NamedParameter[] getNameServers(AuthToken at, String domain) throws Exception {
        ArrayList nservers = new ArrayList();
        NamedParameter[] np = new NamedParameter[0];
        Domain d = null;
        Account a = Utils.getAccount(at);
        try {
            d = Utils.getDomain(a, domain);
        } catch (Exception e) {
        }
        if (d != null) {
            String dtype = getDomainType(d);
            if (dtype.equalsIgnoreCase("3ldomain")) {
                nservers = ((ThirdLevelDNSZone) Utils.getDomain(a, domain).getId().findChild("3l_dns_zone").get()).getNameServers();
            } else if (dtype.equalsIgnoreCase("domain")) {
                nservers = ((DNSZone) Utils.getDomain(a, domain).getId().findChild("dns_zone").get()).getNameServers();
            } else if (dtype.equalsIgnoreCase("service_domain")) {
                nservers = ((DNSZone) Utils.getDomain(a, domain).getId().findChild("service_dns_zone").get()).getNameServers();
            }
        } else {
            ResourceId rid = a.getId().findChild("3l_domain_alias");
            if (rid != null) {
                nservers = ((ThirdLevelDNSZone) rid.findChild("3l_dns_zone").get()).getNameServers();
            } else {
                ResourceId rid2 = a.getId().findChild("domain_alias");
                if (rid2 != null) {
                    nservers = ((DNSZone) rid2.findChild("dns_zone").get()).getNameServers();
                }
            }
        }
        for (int i = 0; i < nservers.size(); i++) {
            HostEntry nserver = (HostEntry) nservers.get(i);
            np = NamedParameter.addParameter(nserver.getName(), nserver.getIP().toString(), np);
        }
        return np;
    }
}
