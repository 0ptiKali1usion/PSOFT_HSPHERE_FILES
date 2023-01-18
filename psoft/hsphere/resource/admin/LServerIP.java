package psoft.hsphere.resource.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.exception.UnableAddAdmInstantAliasException;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LServerIP.class */
public class LServerIP {
    private int flag;
    private long lserverId;

    /* renamed from: ip */
    private C0015IP f171ip;
    ResourceId rid;
    static Boolean isAutoUpdateDisabled = null;

    public LServerIP(String ip, String mask, int flag, long lserverId, ResourceId rid) {
        this(C0015IP.createIP(ip, mask), flag, lserverId, rid);
    }

    public LServerIP(C0015IP ip, int flag, long lserverId, ResourceId rid) {
        this.flag = flag;
        this.lserverId = lserverId;
        this.f171ip = ip;
        this.rid = rid;
    }

    public int getFlag() {
        return this.flag;
    }

    public long getLserverId() {
        return this.lserverId;
    }

    public C0015IP getIp() {
        return this.f171ip;
    }

    public ResourceId getRid() {
        return this.rid;
    }

    public boolean isIPValid() throws Exception {
        boolean z;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = Session.getDb();
        if (getRid() == null) {
            return true;
        }
        try {
            if (getFlag() < 0) {
                return true;
            }
            try {
                if (getFlag() == 1) {
                    ps = con.prepareStatement("SELECT p.account_id FROM l_server_ips lip, parent_child p, accounts a WHERE lip.l_server_id = ? AND lip.ip = ? AND lip.flag = ? AND lip.r_id = p.child_id AND p.account_id = a.id AND a.deleted IS NULL");
                    ps.setLong(1, getLserverId());
                    ps.setString(2, getIp().toString());
                    ps.setInt(3, getFlag());
                    rs = ps.executeQuery();
                } else if (getFlag() == 6) {
                    ps = con.prepareStatement("SELECT la.prefix FROM l_server_aliases la, l_server_ips lip WHERE lip.l_server_id = ? AND lip.flag = ? AND lip.ip = ? AND lip.r_id = la.e_dns_rec_id");
                    ps.setLong(1, getLserverId());
                    ps.setInt(2, 6);
                    ps.setString(3, getIp().toString());
                    rs = ps.executeQuery();
                } else if (getFlag() == 8) {
                    ps = con.prepareStatement("SELECT rssl.cp_alias FROM l_server_ips lip, reseller_ssl rssl WHERE lip.l_server_id = ? AND lip.flag = ? AND lip.ip = ? AND lip.r_id = rssl.id;");
                    ps.setLong(1, getLserverId());
                    ps.setInt(2, 8);
                    ps.setString(3, getIp().toString());
                    rs = ps.executeQuery();
                }
                if (rs != null) {
                    if (rs.next()) {
                        z = true;
                        boolean z2 = z;
                        Session.closeStatement(ps);
                        con.close();
                        return z2;
                    }
                }
                z = false;
                boolean z22 = z;
                Session.closeStatement(ps);
                con.close();
                return z22;
            } catch (SQLException sqlEx) {
                Session.getLog().debug("An SQL error occured while validating the " + this.f171ip + " IP address on the " + getLserverId() + " logical server", sqlEx);
                Session.closeStatement(ps);
                con.close();
                return false;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized boolean isAliasesAutoUpdateDisabled() {
        if (isAutoUpdateDisabled == null) {
            String val = Session.getPropertyString("ADMIN_DNS_ALIASES_AUTOUPDATE");
            isAutoUpdateDisabled = new Boolean("DISABLED".equalsIgnoreCase(val));
        }
        return isAutoUpdateDisabled.booleanValue();
    }

    public void addIPPhysically(boolean dnsRestart) throws Exception {
        addIPPhysically(dnsRestart, isAliasesAutoUpdateDisabled());
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v23, types: [java.lang.String[], java.lang.String[][]] */
    public void addIPPhysically(boolean dnsRestart, boolean setupIPOnly) throws Exception {
        LogicalServer ls = LogicalServer.get(getLserverId());
        boolean installed = false;
        try {
            installed = HostManager.getHost(getLserverId()).isInstalled();
        } catch (Exception e) {
        }
        if (!installed) {
            return;
        }
        if (isSharedIP()) {
            if (ls.getType() == 1) {
                HostEntry he = HostManager.getHost(getLserverId());
                List list = new ArrayList();
                list.add("add");
                list.add(getIp().toString());
                list.add(getIp().getMask());
                he.exec("ip-shared", list);
            } else {
                WinHostEntry he2 = (WinHostEntry) HostManager.getHost(getLserverId());
                he2.exec("ip-create.asp", (String[][]) new String[]{new String[]{"ip", getIp().toString()}, new String[]{"mask", getIp().getMask()}, new String[]{"ignoreexist", "1"}});
            }
        } else if (isResellerDNSIP()) {
            if ("".equals(Session.getPropertyString("MYDNS_USER"))) {
                HostEntry he3 = HostManager.getHost(getLserverId());
                if (!he3.getPFirstIP().equals(this.f171ip)) {
                    List list2 = new ArrayList();
                    list2.add("add");
                    list2.add(getIp().toString());
                    list2.add(getIp().getMask());
                    he3.exec("ip-dns", list2);
                    if (dnsRestart) {
                        HostEntry he4 = HostManager.getHost(getLserverId());
                        he4.exec("dns-restart", new ArrayList());
                    }
                }
            }
        } else if (setupIPOnly && isDedicatedIP()) {
            if (ls.getType() == 1) {
                HostEntry he5 = HostManager.getHost(getLserverId());
                List list3 = new ArrayList();
                list3.add("add");
                list3.add(getIp().toString());
                list3.add(getIp().getMask());
                he5.exec("ip-update", list3);
            } else {
                WinHostEntry he6 = (WinHostEntry) HostManager.getHost(getLserverId());
                he6.exec("ip-create.asp", (String[][]) new String[]{new String[]{"ip", getIp().toString()}, new String[]{"mask", getIp().getMask()}, new String[]{"ignoreexist", "1"}});
            }
        }
        if (setupIPOnly) {
            return;
        }
        if (isSharedIP() || (isServiceIP() && ls.getType() == 10)) {
            AdmInstantAlias.getByTag(getFlag());
            StringBuffer errors = new StringBuffer();
            for (AdmInstantAlias als : AdmInstantAlias.getByTag(getFlag())) {
                try {
                    Session.getLog().debug("Adding ADNSRecord for lserver " + getLserverId() + " and IP " + getIp().toString());
                    try {
                        als.delRecord(getLserverId(), getIp().toString());
                    } catch (Exception ex1) {
                        Session.getLog().warn("Unable to delete AdmDNSRecord", ex1);
                    }
                    als.addRecord(getLserverId(), getIp().toString());
                    als.getLservers();
                } catch (Exception ex) {
                    errors.append(ex.getMessage()).append('\n');
                }
            }
            if (errors.length() > 0) {
                throw new UnableAddAdmInstantAliasException(errors.toString());
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v20, types: [java.lang.String[], java.lang.String[][]] */
    public void deleteIPPhysically(boolean dnsRestart) throws Exception {
        LogicalServer ls = LogicalServer.get(getLserverId());
        boolean installed = false;
        try {
            installed = HostManager.getHost(getLserverId()).isInstalled();
        } catch (Exception e) {
        }
        if (!installed) {
            return;
        }
        try {
            if (isSharedIP()) {
                if (ls.getType() == 1) {
                    HostEntry he = HostManager.getHost(getLserverId());
                    if (!he.getPFirstIP().equals(getIp().toString())) {
                        List list = new ArrayList();
                        list.add("del");
                        list.add(getIp().toString());
                        he.exec("ip-shared", list);
                    }
                } else {
                    WinHostEntry he2 = (WinHostEntry) HostManager.getHost(getLserverId());
                    if (!he2.getPFirstIP().equals(this.f171ip)) {
                        he2.exec("ip-delete.asp", (String[][]) new String[]{new String[]{"ip", getIp().toString()}});
                    }
                }
            } else if (isResellerDNSIP() && "".equals(Session.getPropertyString("MYDNS_USER"))) {
                HostEntry he3 = HostManager.getHost(getLserverId());
                if (!he3.getPFirstIP().equals(this.f171ip)) {
                    List list2 = new ArrayList();
                    list2.add("del");
                    list2.add(getIp().toString());
                    he3.exec("ip-dns", list2);
                    if (dnsRestart) {
                        he3.exec("dns-restart", new ArrayList());
                    }
                }
            } else if ((isTakenResellerDNSIPwithNegativeSign() || isDedicatedIPwithNegativeSign()) && ls.getType() == 1) {
                HostEntry he4 = HostManager.getHost(getLserverId());
                List list3 = new ArrayList();
                list3.add("del");
                list3.add(getIp().toString());
                list3.add(getIp().getMask());
                he4.exec("ip-update", list3);
            }
            if (isSharedIP() || (isServiceIP() && ls.getType() == 10)) {
                AdmInstantAlias.getByTag(this.flag);
                for (AdmInstantAlias als : AdmInstantAlias.getByTag(this.flag)) {
                    try {
                        als.delRecord(getIp().toString());
                        als.getLservers();
                    } catch (Exception ex) {
                        Session.getLog().error("Error occured during deleting Instant alias record:", ex);
                    }
                }
            }
        } catch (Exception ex2) {
            Session.getLog().error("An error occured while physically deleting the " + this.f171ip + " IP address with the " + this.flag + " flag from the " + ls.getId() + ". " + ls.getDescription() + " logical server:", ex2);
        }
    }

    public boolean isSharedIP() {
        return getFlag() == 2 || (getFlag() >= 10 && getFlag() != 1000) || getFlag() == -2 || (getFlag() <= -10 && getFlag() != -1000);
    }

    public boolean isResellerDNSIP() {
        return getFlag() == 5;
    }

    public boolean isTakenResellerDNSIPwithNegativeSign() {
        return getFlag() == -6;
    }

    public boolean isResellerCPIP() {
        return getFlag() == 10;
    }

    public boolean isServiceIP() {
        return getFlag() == 4;
    }

    public boolean isDedicatedIP() {
        return getFlag() == 1;
    }

    public boolean isDedicatedIPwithNegativeSign() {
        return getFlag() == -1;
    }

    public int releaseIP() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        int newDbFlag = getFlag();
        switch (getFlag()) {
            case -1:
            case 1:
                newDbFlag = 0;
                break;
            case 6:
                newDbFlag = 5;
                break;
            case 8:
                newDbFlag = 7;
                break;
            case HostEntry.TAKEN_VPS_IP /* 1001 */:
                newDbFlag = 1000;
                break;
        }
        if (newDbFlag != getFlag()) {
            try {
                ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = NULL, r_type = NULL WHERE l_server_id = ? AND ip = ?");
                ps.setInt(1, newDbFlag);
                ps.setLong(2, getLserverId());
                ps.setString(3, getIp().toString());
                ps.executeUpdate();
                this.flag = newDbFlag;
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return getFlag();
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void setRid(ResourceId rid) {
        this.rid = rid;
    }
}
