package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.exception.UnableAddAdmInstantAliasException;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LogicalServer.class */
public class LogicalServer extends SharedObject implements TemplateHashModel {
    protected String name;
    protected int group;
    protected String fileServer;
    protected String path;
    protected String description;
    protected long pserver;
    protected int status;
    protected int signup;
    protected int type_id;
    protected List serverIps;

    public String getName() {
        return this.name;
    }

    public int getGroup() {
        return this.group;
    }

    public String getFileServer() {
        return this.fileServer;
    }

    public String getPath() {
        return this.path;
    }

    public String getDescription() {
        return this.description;
    }

    public long getPServerId() {
        return this.pserver;
    }

    public boolean isSignup() {
        try {
            return HostManager.getHost(getId()).availableForSignup();
        } catch (Exception e) {
            return this.signup == 1;
        }
    }

    public boolean isInstalled() {
        try {
            return HostManager.getHost(getId()).isInstalled();
        } catch (Exception e) {
            return false;
        }
    }

    public int getType() {
        return this.type_id;
    }

    public LogicalServer(long id, String name, int group, String fileServer, String description, String path, long pserver, int status, int type_id, int signup) throws Exception {
        super(id);
        this.name = name;
        this.group = group;
        this.fileServer = fileServer;
        this.description = description;
        this.path = path;
        this.pserver = pserver;
        this.status = status;
        this.signup = signup;
        this.type_id = type_id;
        loadServerIps();
    }

    public static LogicalServer create(String name, int group, String fileServer, String description, String path, int type_id, long pserver, int signup) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            long id = Session.getNewIdAsLong("logical_seq");
            ps = con.prepareStatement("INSERT INTO l_server (id, name, group_id, file_server, description, file_server_path, status, p_server_id, type_id, signup) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, id);
            ps.setString(2, name);
            ps.setInt(3, group);
            ps.setString(4, fileServer);
            ps.setString(5, description);
            ps.setString(6, path);
            ps.setInt(7, 0);
            ps.setLong(8, pserver);
            ps.setInt(9, type_id);
            ps.setInt(10, signup);
            ps.executeUpdate();
            LogicalServer logicalServer = new LogicalServer(id, name, group, fileServer, description, path, pserver, 0, type_id, signup);
            Session.closeStatement(ps);
            con.close();
            return logicalServer;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delete() throws Exception {
        delete();
        return new TemplateOKResult();
    }

    public void delete() throws Exception {
        if (FM_getAllIP().size() > 0) {
            throw new HSUserException("logicalserver.ip");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        HostEntry he = null;
        try {
            he = HostManager.getHost(this.f51id);
        } catch (Exception ex) {
            Session.getLog().error("Unable to get HostEntry #" + this.f51id, ex);
        }
        try {
            ps = con.prepareStatement("SELECT e_zone_id, e_dns_rec_id FROM l_server_aliases WHERE l_server_id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AdmDNSZone zone = AdmDNSZone.get(rs.getLong(1));
                if (zone != null) {
                    try {
                        zone.delServiceRecord(rs.getLong(2));
                    } catch (Exception ex2) {
                        Session.getLog().error("Unable to delete service records for zone:" + zone.getDomainName(), ex2);
                    }
                    if (zone.allowSSL() && he != null) {
                        he.setSharedSSL(zone.getId(), false);
                    }
                }
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM l_server WHERE id = ?");
            ps2.setLong(1, this.f51id);
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM shared_ssl_hosts WHERE l_server_id = ?");
            ps3.setLong(1, this.f51id);
            ps3.executeUpdate();
            ps = con.prepareStatement("DELETE FROM l_server_options WHERE l_server_id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            remove(this.f51id);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void remove(long id) throws Exception {
        remove(id, LogicalServer.class);
    }

    public static LogicalServer get(long id) throws Exception {
        LogicalServer l = (LogicalServer) get(id, LogicalServer.class);
        if (l != null) {
            return l;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name, group_id, file_server, description, file_server_path, p_server_id, status, type_id, signup FROM l_server WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LogicalServer logicalServer = new LogicalServer(id, rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getLong(6), rs.getInt(7), rs.getInt(8), rs.getInt(9));
                Session.closeStatement(ps);
                con.close();
                return logicalServer;
            }
            throw new Exception("LServer not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f51id);
        }
        if (key.equals("name")) {
            return new TemplateString(this.name);
        }
        if (key.equals("group")) {
            return new TemplateString(this.group);
        }
        if (key.equals("fileServer")) {
            return new TemplateString(this.fileServer);
        }
        if (key.equals("path")) {
            return new TemplateString(this.path);
        }
        if (key.equals("desc")) {
            return new TemplateString(this.description);
        }
        if (key.equals("pserver")) {
            return new TemplateString(this.pserver);
        }
        if (key.equals("lstatus")) {
            return new TemplateString(this.status);
        }
        if (key.equals("type_id")) {
            return new TemplateString(this.type_id);
        }
        if (key.equals("signup")) {
            return new TemplateString(isSignup());
        }
        if (key.equals("installed")) {
            if (isInstalled()) {
                return new TemplateString("1");
            }
            return null;
        } else if (key.equals("unix_user_default_homedir")) {
            return new TemplateString(HostEntry.UnixUserDefaultHomeDir);
        } else {
            if (key.equals("unix_user_home_prefix")) {
                return new TemplateString(HostEntry.UnixUserHomePrefix);
            }
            if (key.equals("win_user_default_home")) {
                return new TemplateString(HostEntry.WinUserDefaultHome);
            }
            try {
                if (key.equals("infos")) {
                    return new LServerInfo(this.f51id, HostManager.getTypeByGroup(this.group));
                }
                if (key.equals("type")) {
                    return new TemplateString(HostManager.getTypeByGroup(this.group));
                }
                if (key.equals("ip")) {
                    HostEntry he = HostManager.getHost(this.f51id);
                    return new TemplateString(he.getIP());
                } else if (key.equals("mask")) {
                    HostEntry he2 = HostManager.getHost(this.f51id);
                    return new TemplateString(he2.getIP().getMask());
                } else if (key.equals("grouptype")) {
                    return new TemplateString(HostEntry.getGroupNameByType(HostManager.getTypeByGroup(this.group)));
                } else {
                    try {
                        if (key.equals("ftp_type") && HostManager.getTypeByGroup(this.group) == 5) {
                            WinHostEntry whe = (WinHostEntry) HostManager.getHost(this.f51id);
                            return new TemplateString(whe.getFTPType());
                        }
                        return super.get(key);
                    } catch (Exception ex) {
                        Session.getLog().error("Error while getting FTP type for server " + this.f51id, ex);
                        return null;
                    }
                }
            } catch (Exception e) {
                Session.getLog().warn("Get info from Logical Server " + getId(), e);
                return null;
            }
        }
    }

    public TemplateModel FM_getOption(String name) throws Exception {
        HostEntry he = HostManager.getHost(getId());
        if (he == null) {
            return null;
        }
        return new TemplateString(he.getOption(name));
    }

    public TemplateModel FM_setOption(String name, String value) throws Exception {
        HostEntry he = HostManager.getHost(getId());
        he.setOption(name, value);
        return new TemplateOKResult();
    }

    public TemplateModel FM_setSignup(int signup) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server SET signup = ? WHERE id = ?");
            ps.setInt(1, signup);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.signup = signup;
            try {
                HostManager.getHost(getId()).setSignup(signup);
            } catch (Exception e) {
            }
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean isTagDuplicated(int flag) throws Exception {
        if (flag == 2 || (flag >= 10 && flag != 1000 && flag != 1001)) {
            Connection con1 = Session.getDb();
            PreparedStatement ps1 = null;
            try {
                ps1 = con1.prepareStatement("SELECT DISTINCT flag FROM l_server_ips WHERE l_server_id = ?;");
                ps1.setLong(1, getId());
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) {
                    int tmp_flag = rs1.getInt(1);
                    if (tmp_flag == flag) {
                        Session.closeStatement(ps1);
                        con1.close();
                        return true;
                    }
                }
                Session.closeStatement(ps1);
                con1.close();
                return false;
            } catch (Throwable th) {
                Session.closeStatement(ps1);
                con1.close();
                throw th;
            }
        }
        return false;
    }

    protected boolean isIPDuplicated(String ip, int flag) throws Exception {
        boolean result = false;
        if (flag == 5 || flag == 0 || flag == 7 || flag == 1000) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                if (flag == 0) {
                    ps = con.prepareStatement("SELECT count(*) FROM l_server_ips WHERE ip = ? AND flag != ?");
                    ps.setInt(2, HostEntry.TAKEN_VPS_IP);
                } else {
                    ps = con.prepareStatement("SELECT count(*) FROM l_server_ips WHERE ip = ?");
                }
                ps.setString(1, ip);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        result = true;
                    }
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return result;
    }

    public TemplateModel FM_addIPRange(String start, String end, String mask, int flag) throws Exception {
        Session.getLog().debug("INSIDE FM_addIPRange FLAG=" + flag + " TYPE=" + getType() + " START=" + start + " END=" + end);
        int i = start.lastIndexOf(46) + 1;
        String start1 = start.substring(0, i);
        int i2 = end.lastIndexOf(46) + 1;
        String end1 = end.substring(0, i2);
        if (!start1.equals(end1)) {
            throw new HSUserException("hostmanager.iprange");
        }
        if (isTagDuplicated(flag)) {
            throw new HSUserException("logicalserver.duplicate_tag");
        }
        boolean installed = false;
        try {
            installed = HostManager.getHost(getId()).isInstalled();
        } catch (Exception e) {
        }
        int to = Integer.parseInt(end.substring(i2));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO l_server_ips(l_server_id, ip, mask, flag, ip_num) VALUES (?, ?, ?, ?, ?)");
                ps.setLong(1, getId());
                ps.setString(3, mask);
                ps.setInt(4, flag);
                for (int from = Integer.parseInt(start.substring(i)); from <= to; from++) {
                    if (isIPDuplicated(start1 + from, flag)) {
                        throw new HSUserException("logicalserver.duplicate");
                    }
                    LServerIP lsip = new LServerIP(start1 + from, mask, flag, getId(), null);
                    try {
                        lsip.addIPPhysically(false);
                    } catch (UnableAddAdmInstantAliasException e2) {
                        Session.getLog().warn("Some errors has occured while adding IP(s)");
                    }
                    ps.setString(2, start1 + from);
                    ps.setLong(5, C0015IP.toLong(start1 + from));
                    ps.executeUpdate();
                    synchronized (this.serverIps) {
                        this.serverIps.add(lsip);
                    }
                }
                if (installed && flag == 5 && Session.getPropertyString("MYDNS_USER").equals("")) {
                    HostEntry he = HostManager.getHost(this.f51id);
                    he.exec("dns-restart", new ArrayList());
                }
                return this;
            } catch (SQLException e3) {
                throw new HSUserException("logicalserver.duplicate");
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public LServerIP addIP(String ip, String mask, int flag) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO l_server_ips(l_server_id, ip, mask, flag, ip_num) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, getId());
            ps.setString(2, ip);
            ps.setString(3, mask);
            ps.setInt(4, flag);
            ps.setLong(5, C0015IP.toLong(ip));
            LServerIP lsIP = new LServerIP(ip, mask, flag, getId(), null);
            try {
                lsIP.addIPPhysically(true);
            } catch (UnableAddAdmInstantAliasException ex) {
                Session.getLog().error("Unable to add aliasses", ex);
            }
            ps.executeUpdate();
            synchronized (this.serverIps) {
                this.serverIps.add(lsIP);
            }
            Session.closeStatement(ps);
            con.close();
            return lsIP;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteIP(String ip, int flag) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM l_server_ips WHERE l_server_id = ? AND ip = ?");
            ps.setLong(1, getId());
            ps.setString(2, ip);
            LServerIP lsip = getIp(ip, flag);
            lsip.deleteIPPhysically(true);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delIPRange(String start, String end, int flag) throws Exception {
        int i = start.lastIndexOf(46) + 1;
        String start1 = start.substring(0, i);
        int i2 = end.lastIndexOf(46) + 1;
        String end1 = end.substring(0, i2);
        if (!start1.equals(end1)) {
            throw new HSUserException("hostmanager.iprange");
        }
        int to = Integer.parseInt(end.substring(i2));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM l_server_ips WHERE l_server_id = ? AND ip = ?");
            ps.setLong(1, getId());
            for (int from = Integer.parseInt(start.substring(i)); from <= to; from++) {
                Session.getLog().debug("Trying to delete " + start1 + from + " flag = " + flag);
                LServerIP lsip = getIp(start1 + from, flag);
                if (lsip != null) {
                    lsip.deleteIPPhysically(false);
                    ps.setString(2, start1 + from);
                    ps.executeUpdate();
                    synchronized (this.serverIps) {
                        this.serverIps.remove(lsip);
                    }
                }
            }
            if (flag == 5 && Session.getPropertyString("MYDNS_USER").equals("")) {
                HostEntry he = HostManager.getHost(this.f51id);
                he.exec("dns-restart", new ArrayList());
            }
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_add_user_homes(String directory) throws Exception {
        TemplateHash result = new TemplateHash();
        HostEntry he = HostManager.getHost(this.f51id);
        List list = new ArrayList();
        list.add(directory);
        Collection col = he.exec("adduserhomes", list);
        if (col == null || col.isEmpty()) {
            result.put("status", "OK");
        } else {
            TemplateList msgs = new TemplateList(col);
            result.put("status", "ERROR");
            result.put("msgs", msgs);
        }
        return result;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LogicalServer$Range.class */
    public class Range implements TemplateHashModel {
        protected TemplateList list = new TemplateList();
        protected TemplateModel first;
        protected TemplateModel last;

        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        public Range() {
            LogicalServer.this = r5;
        }

        public void add(TemplateModel t) {
            if (this.first == null) {
                this.first = t;
            }
            this.last = t;
            this.list.add(t);
        }

        public TemplateModel get(String key) {
            if (key.equals("first")) {
                return this.first;
            }
            if (key.equals("last")) {
                return this.last;
            }
            if (key.equals("list")) {
                return this.list;
            }
            if (key.equals("isSingle")) {
                return new TemplateString(this.last == this.first);
            }
            return null;
        }
    }

    public TemplateModel FM_getAllIP() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip, mask, flag, r_id, ip_num FROM l_server_ips WHERE l_server_id = ? ORDER BY ip_num");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            String oldIp = "";
            int count = -1;
            String oldFlag = "";
            String oldMask = "";
            TemplateModel templateModel = null;
            while (rs.next()) {
                String newIp = rs.getString(1);
                String newMask = rs.getString(2);
                String newFlag = rs.getString(3);
                StringTokenizer st = new StringTokenizer(newIp, ".");
                int ip1 = Integer.parseInt(st.nextToken());
                int ip2 = Integer.parseInt(st.nextToken());
                int ip3 = Integer.parseInt(st.nextToken());
                int ip4 = Integer.parseInt(st.nextToken());
                if (newIp.equals(oldIp + count) && oldFlag.equals(newFlag) && newMask.equals(oldMask)) {
                    count++;
                } else {
                    if (templateModel != null) {
                        list.add(templateModel);
                    }
                    templateModel = new Range();
                    oldIp = ip1 + "." + ip2 + "." + ip3 + ".";
                    oldFlag = newFlag;
                    oldMask = newMask;
                    count = ip4 + 1;
                }
                TemplateMap map = new TemplateMap();
                map.put("ip", newIp);
                map.put("mask", newMask);
                map.put("flag", newFlag);
                if (Integer.parseInt(newFlag) == 1 || Integer.parseInt(newFlag) == 6 || Integer.parseInt(newFlag) == 8 || Integer.parseInt(newFlag) == 1001) {
                    map.put("busy", "1");
                }
                if ((this.group == 1 || this.group == 5) && Integer.parseInt(newFlag) != 1 && Integer.parseInt(newFlag) != 0 && Integer.parseInt(newFlag) != -1) {
                    PreparedStatement ps1 = con.prepareStatement("SELECT count(*) FROM parent_child p1, parent_child p2, l_server_ips ips, unix_user u, accounts a, plan_value pv WHERE p1.child_type=8 AND NOT EXISTS (SELECT l_server_id FROM l_server_ips WHERE r_id = p1.child_id) AND p2.child_type=7 AND p2.account_id = p1.account_id AND p2.child_id = u.id AND u.hostid = ips.l_server_id AND ips.ip= ? AND p1.account_id = a.id AND a.plan_id = pv.plan_id AND pv.name='SHARED_IP' AND pv.value = ips.flag;");
                    ps1.setString(1, newIp);
                    ResultSet rs1 = ps1.executeQuery();
                    long used = 0;
                    while (rs1.next()) {
                        used = rs1.getLong(1);
                    }
                    if (used > 0) {
                        map.put("shared_ip_used", new Long(used));
                        map.put("busy", "1");
                    }
                }
                if (Integer.parseInt(newFlag) == 4) {
                    PreparedStatement ps12 = con.prepareStatement("SELECT count(*) FROM l_server_aliases WHERE l_server_id = ?");
                    ps12.setLong(1, getId());
                    ResultSet rs12 = ps12.executeQuery();
                    long used2 = 0;
                    while (rs12.next()) {
                        used2 = rs12.getLong(1);
                    }
                    if (used2 > 0) {
                        map.put("service_ip_used", new Long(used2));
                    }
                }
                map.put("rid", rs.getString(4));
                templateModel.add(map);
            }
            if (templateModel != null) {
                list.add(templateModel);
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isCompatibleWith(LogicalServer ls) {
        return true;
    }

    /* JADX WARN: Finally extract failed */
    public TemplateModel FM_delUnusedService() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            try {
                switch (HostManager.getTypeByGroup(this.group)) {
                    case 4:
                        try {
                            try {
                                ps = con.prepareStatement("SELECT child_id FROM parent_child where child_id in (SELECT id FROM mysqlres WHERE mysql_host_id = ?)");
                                ps.setLong(1, getId());
                                ResultSet rs = ps.executeQuery();
                                List ids = new ArrayList();
                                while (rs.next()) {
                                    long rs_id = rs.getLong(1);
                                    ids.add(new Long(rs_id));
                                }
                                for (int i = 0; i < ids.size(); i++) {
                                    long tmp_id = ((Long) ids.get(i)).longValue();
                                    ResourceId r_id = new ResourceId(tmp_id, 6000);
                                    Resource.getCache().remove(r_id);
                                    ps1 = con.prepareStatement("DELETE FROM parent_child where child_id  = ?");
                                    ps1.setLong(1, tmp_id);
                                    ps1.executeUpdate();
                                    ps2 = con.prepareStatement("DELETE FROM mysqlres WHERE id = ?");
                                    ps2.setLong(1, tmp_id);
                                    ps2.executeUpdate();
                                }
                                Session.closeStatement(ps);
                                Session.closeStatement(ps1);
                                Session.closeStatement(ps2);
                                con.close();
                                break;
                            } catch (Throwable th) {
                                Session.closeStatement(ps);
                                Session.closeStatement(ps1);
                                Session.closeStatement(ps2);
                                con.close();
                                throw th;
                            }
                        } catch (Exception e) {
                            Session.getLog().error("error", e);
                            throw new HSUserException("logicalserver.undelmysql");
                        }
                    case 15:
                        try {
                            ps = con.prepareStatement("SELECT child_id FROM parent_child where child_id in (SELECT id FROM mssqlres WHERE mssql_host_id = ?)");
                            ps.setLong(1, getId());
                            ResultSet rs2 = ps.executeQuery();
                            List ids2 = new ArrayList();
                            while (rs2.next()) {
                                long rs_id2 = rs2.getLong(1);
                                ids2.add(new Long(rs_id2));
                            }
                            for (int i2 = 0; i2 < ids2.size(); i2++) {
                                long tmp_id2 = ((Long) ids2.get(i2)).longValue();
                                ResourceId r_id2 = new ResourceId(tmp_id2, 6800);
                                Resource.getCache().remove(r_id2);
                                ps1 = con.prepareStatement("DELETE FROM parent_child where child_id  = ?");
                                ps1.setLong(1, tmp_id2);
                                ps1.executeUpdate();
                                ps2 = con.prepareStatement("DELETE FROM mssqlres WHERE id = ?");
                                ps2.setLong(1, tmp_id2);
                                ps2.executeUpdate();
                            }
                            Session.closeStatement(ps);
                            Session.closeStatement(ps1);
                            Session.closeStatement(ps2);
                            con.close();
                            break;
                        } catch (Exception e2) {
                            Session.getLog().error("error", e2);
                            throw new HSUserException("logicalserver.undelmssql");
                        }
                    case 18:
                        try {
                            ps = con.prepareStatement("SELECT child_id FROM parent_child where child_id in (SELECT id FROM pgsqlres WHERE pgsql_host_id = ?)");
                            ps.setLong(1, getId());
                            ResultSet rs3 = ps.executeQuery();
                            List ids3 = new ArrayList();
                            while (rs3.next()) {
                                long rs_id3 = rs3.getLong(1);
                                ids3.add(new Long(rs_id3));
                            }
                            for (int i3 = 0; i3 < ids3.size(); i3++) {
                                long tmp_id3 = ((Long) ids3.get(i3)).longValue();
                                ResourceId r_id3 = new ResourceId(tmp_id3, 6900);
                                Resource.getCache().remove(r_id3);
                                ps1 = con.prepareStatement("DELETE FROM parent_child where child_id  = ?");
                                ps1.setLong(1, tmp_id3);
                                ps1.executeUpdate();
                                ps2 = con.prepareStatement("DELETE FROM pgsqlres WHERE id = ?");
                                ps2.setLong(1, tmp_id3);
                                ps2.executeUpdate();
                            }
                            Session.closeStatement(ps);
                            Session.closeStatement(ps1);
                            Session.closeStatement(ps2);
                            con.close();
                            break;
                        } catch (Exception e3) {
                            Session.getLog().error("error", e3);
                            throw new HSUserException("logicalserver.undelpgsql");
                        }
                }
                return this;
            } catch (Throwable th2) {
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                con.close();
                throw th2;
            }
        } catch (Throwable th3) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            throw th3;
        }
    }

    private void loadServerIps() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        List ips = new ArrayList();
        try {
            ps = con.prepareStatement("SELECT ip, mask, flag, r_id, r_type FROM l_server_ips WHERE l_server_id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LServerIP ip = new LServerIP(rs.getString("ip"), rs.getString("mask"), rs.getInt("flag"), getId(), rs.getLong("r_id") == 0 ? null : new ResourceId(rs.getLong("r_id"), rs.getInt("r_type")));
                ips.add(ip);
            }
            this.serverIps = ips;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public LServerIP getIp(String ip, int flag) {
        synchronized (this.serverIps) {
            for (LServerIP lsip : this.serverIps) {
                if (lsip.getIp().toString().equals(ip) && lsip.getFlag() == flag) {
                    return lsip;
                }
            }
            return null;
        }
    }

    public List getIps() {
        ArrayList arrayList;
        synchronized (this.serverIps) {
            arrayList = new ArrayList(this.serverIps);
        }
        return arrayList;
    }

    public TemplateModel FM_getSubnets() throws Exception {
        HostEntry he = HostManager.getHost(getId());
        if (he == null) {
            return null;
        }
        TemplateList subnets = new TemplateList();
        Collection<String> col = he.exec("vps-subnets-xml-get.pl", new String[0]);
        StringBuffer response = new StringBuffer();
        for (String s : col) {
            response.append(s);
        }
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new ByteArrayInputStream(response.toString().getBytes())));
        Document doc = parser.getDocument();
        NodeList nodeList = doc.getElementsByTagName("subnet");
        int count = nodeList.getLength();
        ArrayList check = new ArrayList();
        for (int i = 0; i < count; i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String attrAddr = attributes.getNamedItem("addr").getNodeValue();
            Node nd = attributes.getNamedItem("device");
            String attrDevice = nd != null ? nd.getNodeValue() : "";
            TemplateHash hash = new TemplateHash();
            hash.put("addr", attrAddr);
            hash.put("device", attrDevice);
            subnets.add((TemplateModel) hash);
            check.add(attrAddr);
        }
        return subnets;
    }

    public TemplateModel FM_getDevices() throws Exception {
        HostEntry he = HostManager.getHost(getId());
        if (he == null) {
            return null;
        }
        Collection<Object> col = he.exec("net-iface-list.pl", new String[0]);
        TemplateList list = new TemplateList();
        for (Object obj : col) {
            list.add(obj);
        }
        return list;
    }

    public static boolean existsLServer(long id) throws Exception {
        boolean lserverExists = false;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM l_server WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                lserverExists = true;
            }
            Session.closeStatement(ps);
            con.close();
            return lserverExists;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setFileServer(String fileServer) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server SET file_server = ? WHERE id = ?");
            ps.setString(1, fileServer);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.fileServer = fileServer;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setFileServer(String fileServer) throws Exception {
        setFileServer(fileServer);
        return this;
    }

    public void setFilePath(String filePath) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server SET file_server_path = ? WHERE id = ?");
            ps.setString(1, filePath);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.path = filePath;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setFilePath(String filePath) throws Exception {
        setFilePath(filePath);
        return this;
    }

    public void setMySQLHost(int mod) throws Exception {
        HostEntry he = HostManager.getHost(this.f51id);
        ArrayList l = new ArrayList();
        if (mod == 1) {
            l.add(he.getIP().toString());
        }
        he.exec("mysql-set-host.sh", l);
    }

    public TemplateModel FM_setMySQLHost(int mod) throws Exception {
        setMySQLHost(mod);
        return this;
    }

    /* JADX WARN: Finally extract failed */
    public void addSysCustomRecords() throws Exception {
        String servName = getName();
        String ip = "";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip FROM l_server_ips WHERE (flag IN (? ,?) OR (flag BETWEEN 10 AND 100)) AND l_server_id = ? ORDER BY flag");
            ps.setInt(1, 2);
            ps.setInt(2, 4);
            ps.setLong(3, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ip = rs.getString("ip");
            }
            Session.closeStatement(ps);
            con.close();
            if ("".equals(ip) || "".equals(servName)) {
                throw new HSUserException("IP or server name of logical server id " + getId() + " is not set");
            }
            List<AdmDNSZone> eDnsZones = AdmDNSZone.getZones();
            for (AdmDNSZone curAdmZone : eDnsZones) {
                int pos = servName.indexOf("." + curAdmZone.getName());
                if (pos > 0) {
                    List existRecords = curAdmZone.getCustomRecords();
                    AdmCustomDNSRecord rec = null;
                    Iterator j = existRecords.iterator();
                    while (true) {
                        if (!j.hasNext()) {
                            break;
                        }
                        AdmCustomDNSRecord recTmp = (AdmCustomDNSRecord) j.next();
                        if (recTmp.getName().startsWith(servName)) {
                            rec = recTmp;
                            break;
                        }
                    }
                    Session.getLog().debug("Custom DNSRecord with name: " + servName + " with data: " + ip);
                    String updated = "[CREATED]";
                    if (rec != null) {
                        try {
                            if (ip.equals(rec.getData())) {
                                Session.getLog().debug(" [SKIPPED]");
                                return;
                            }
                        } catch (Exception ex) {
                            Session.getLog().debug(" [FAILED]");
                            throw ex;
                        }
                    }
                    if (rec != null) {
                        rec.delete();
                        updated = " [RECREATED]";
                    }
                    curAdmZone.addCustRecord(ip, servName.substring(0, pos), "A", "86400", "");
                    Session.getLog().debug(updated);
                    return;
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addSysCustomRecords() {
        try {
            addSysCustomRecords();
            return new TemplateOKResult();
        } catch (Exception e) {
            Session.getLog().error("Failed to set system dns records ", e);
            return new TemplateErrorResult(e.toString());
        }
    }
}
