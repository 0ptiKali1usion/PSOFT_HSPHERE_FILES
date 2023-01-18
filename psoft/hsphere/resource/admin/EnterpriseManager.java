package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.NetworkGatewayManager;
import psoft.hsphere.admin.TransferProcess;
import psoft.hsphere.global.GlobalKeySet;
import psoft.hsphere.global.Globals;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.params.BaseParam;
import psoft.hsphere.resource.admin.params.CheckParam;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.admin.params.RadioGroup;
import psoft.hsphere.resource.admin.params.SelectParam;
import psoft.hsphere.resource.system.MailServices;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EnterpriseManager.class */
public class EnterpriseManager extends Resource {
    protected static TemplateList groups;
    protected static Hashtable typeGroups;
    protected static TemplateMap groupMap;
    protected static TemplateList types;
    protected static TemplateMap typeMap;
    protected static final String[] NEED_QMAIL_RESTART_PARAMS = {"tcpsessioncount", "bouncefrom", "bouncehost", "bouncemessage", "doublebouncehost", "doublebounceto", "doublebouncesubject", "bouncesubject", "doublebouncemessage", "badmailfrom-unknown", "maxsascore", "samsgsize", "satimeout", "localtime"};
    protected static final String[] NEED_SPAMD_RESTART_PARAMS = {"sanetcheck", "spamdchildren", "badurls", "urlscnt"};
    protected static List<Long> groupTypesWithPhParams = new ArrayList();

    public EnterpriseManager(int type, Collection init) throws Exception {
        super(type, init);
        loadGroups();
        loadTypes();
    }

    public EnterpriseManager(ResourceId rid) throws Exception {
        super(rid);
        loadGroups();
        loadTypes();
    }

    public EnterpriseManager() throws Exception {
        loadGroups();
        loadTypes();
    }

    protected static synchronized void loadGroups() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, type_id, name FROM l_server_groups ORDER BY id");
            ResultSet rs = ps.executeQuery();
            TemplateList groups2 = new TemplateList();
            Hashtable typeGroups2 = new Hashtable();
            TemplateMap groupMap2 = new TemplateMap();
            while (rs.next()) {
                String grId = rs.getString(1);
                String grTypeId = rs.getString(2);
                String grName = rs.getString(3);
                TemplateMap map = new TemplateMap();
                map.put("id", grId);
                if (Session.getPropertyString("MYDNS_USER").equals("")) {
                    if (rs.getInt(1) == rs.getInt(2)) {
                        map.put("internal", "1");
                    }
                } else if (rs.getInt(1) < 100) {
                    map.put("internal", "1");
                }
                map.put("type", grTypeId);
                map.put("name", grName);
                groups2.add((TemplateModel) map);
                groupMap2.put(grId, grName);
                Hashtable typeGroup = (Hashtable) typeGroups2.get(grTypeId);
                if (typeGroup == null) {
                    typeGroup = new Hashtable();
                }
                typeGroup.put(grId, map);
                typeGroups2.put(grTypeId, typeGroup);
            }
            groupMap = groupMap2;
            groups = groups2;
            typeGroups = typeGroups2;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getGroupsList() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT group_id FROM p_server_group_map GROUP BY group_id ORDER BY group_id");
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                list.add((TemplateModel) new TemplateString(rs.getString(1)));
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

    public TemplateModel FM_getPserversForGroup(long groupId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ps_id, parent_ps_id FROM p_server_group_map LEFT OUTER JOIN loadbalanced_pserver ON ( ps_id = id ) WHERE group_id = ?");
            ps.setLong(1, groupId);
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                if (rs.getString(2) == null) {
                    list.add((TemplateModel) PhysicalServer.get(rs.getLong(1)));
                } else {
                    list.add((TemplateModel) LoadBalancedPServer.getLoadBalancedPServerbyId(rs.getLong(1)));
                }
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

    protected synchronized void loadTypes() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, name FROM l_server_types ORDER BY id");
            ResultSet rs = ps.executeQuery();
            TemplateList types2 = new TemplateList();
            TemplateMap typeMap2 = new TemplateMap();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("id", rs.getString(1));
                map.put("name", rs.getString(2));
                types2.add((TemplateModel) map);
                typeMap2.put(rs.getString(1), rs.getString(2));
            }
            typeMap = typeMap2;
            types = types2;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getGroupType(int id) throws Exception {
        return new TemplateString(HostManager.getTypeByGroup(id));
    }

    public synchronized TemplateModel FM_deleteServerGroup(int id) throws Exception {
        List hosts = HostManager.getHosts(id);
        if (hosts.size() > 0) {
            StringBuffer servers = new StringBuffer("");
            Iterator i = hosts.iterator();
            while (i.hasNext()) {
                HostEntry hs = (HostEntry) i.next();
                servers.append(hs.getName());
                if (i.hasNext()) {
                    servers.append(", ");
                }
            }
            throw new HSUserException("eeman.error_deleting_group.logicalservers", new Object[]{getGroupNameById(id), servers.toString()});
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT DISTINCT plan_id FROM plan_ivalue where type = 6 and value = ?");
            ps.setString(1, String.valueOf(id));
            ResultSet rs = ps.executeQuery();
            boolean arePlans = false;
            StringBuffer plans = new StringBuffer("");
            while (rs.next()) {
                if (arePlans) {
                    plans.append(", ");
                }
                try {
                    plans.append(Plan.getPlan(rs.getString(1)).getDescription());
                    arePlans = true;
                } catch (Exception e) {
                }
            }
            if (arePlans) {
                throw new HSUserException("eeman.error_deleting_group.plans", new Object[]{getGroupNameById(id), plans.toString()});
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM p_server_group_map WHERE group_id = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM l_server_groups WHERE id = ?");
            ps3.setInt(1, id);
            ps3.executeUpdate();
            Session.closeStatement(ps3);
            con.close();
            loadGroups();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public synchronized TemplateModel FM_addServerGroup(String name, int typeId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            int id = 0;
            PreparedStatement ps2 = con.prepareStatement("SELECT MAX(id) FROM l_server_groups");
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1) + 1;
            }
            ps2.close();
            int id2 = id < 100 ? 100 : id;
            ps = con.prepareStatement("INSERT INTO l_server_groups (id, type_id, name) VALUES (?, ?, ?)");
            ps.setInt(1, id2);
            ps.setInt(2, typeId);
            ps.setString(3, name);
            ps.executeUpdate();
            TemplateModel templateMap = new TemplateMap();
            String grId = String.valueOf(id2);
            String grTypeId = String.valueOf(typeId);
            templateMap.put("id", new TemplateString(grId));
            templateMap.put("type", new TemplateString(grTypeId));
            templateMap.put("name", new TemplateString(name));
            groups.add(templateMap);
            groupMap.put(Integer.toString(id2), name);
            Hashtable typeGroup = (Hashtable) typeGroups.get(grTypeId);
            if (typeGroup == null) {
                typeGroup = new Hashtable();
            }
            typeGroup.put(grId, templateMap);
            typeGroups.put(grTypeId, typeGroup);
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("groups")) {
            return new TemplateList(groups);
        }
        if (key.equals("groupMap")) {
            return new TemplateMap(groupMap);
        }
        if (key.equals("types")) {
            return new TemplateList(types);
        }
        if (key.equals("typeMap")) {
            return new TemplateMap(typeMap);
        }
        if (key.equals("isSSLEnabled")) {
            try {
                return isSSLEnabled();
            } catch (Exception e) {
                return null;
            }
        } else if ("mail_manager".equals(key)) {
            try {
                return new MailManager();
            } catch (Exception e2) {
                return null;
            }
        } else {
            return super.get(key);
        }
    }

    public static synchronized TemplateList getLServerGroups() {
        return new TemplateList(groups);
    }

    public static synchronized Hashtable getLServerTypeGroups(String type) {
        if (typeGroups == null) {
            try {
                loadGroups();
            } catch (Exception ex) {
                Session.getLog().error("Unable to initialize tyepGroups:", ex);
                return null;
            }
        }
        return (Hashtable) typeGroups.get(type);
    }

    public static TemplateList getEnabledLServerGroups(String groupType) throws Exception {
        TemplateList result = new TemplateList();
        GlobalKeySet gs = Globals.getAccessor().getSet("server_groups");
        Hashtable groups2 = getLServerTypeGroups(HostEntry.getGroupTypeToId().get(groupType).toString());
        if (groups2 != null) {
            Iterator i = new TreeSet(groups2.keySet()).iterator();
            while (i.hasNext()) {
                String groupId = i.next().toString();
                if (gs.isKeyDisabled(groupId) == 0) {
                    result.add((TemplateModel) ((TemplateMap) groups2.get(groupId)));
                }
            }
        }
        return result;
    }

    public static boolean areEnabledLServerGroups(String groupType) throws Exception {
        GlobalKeySet gs = Globals.getAccessor().getSet("server_groups");
        Hashtable groups2 = getLServerTypeGroups(HostEntry.getGroupTypeToId().get(groupType).toString());
        if (groups2 != null) {
            Enumeration e = groups2.keys();
            while (e.hasMoreElements()) {
                String groupId = e.nextElement().toString();
                if (gs.isKeyDisabled(groupId) == 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public static LogicalServer FM_cp_lserver() {
        HostEntry cpHost = HostManager.getCPHost();
        if (cpHost != null) {
            try {
                return LogicalServer.get(cpHost.getId());
            } catch (Exception e) {
                Session.getLog().debug("Getting CP Lserver: ", e);
                return null;
            }
        }
        return null;
    }

    public static HostEntry FM_cp_host() {
        return HostManager.getCPHost();
    }

    public TemplateModel FM_getPserverList() throws Exception {
        TemplateList list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, parent_ps_id FROM p_server NATURAL LEFT OUTER JOIN loadbalanced_pserver ORDER BY parent_ps_id DESC, is_parent");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString(2) == null) {
                    list.add((TemplateModel) PhysicalServer.get(rs.getLong(1)));
                } else {
                    list.add((TemplateModel) LoadBalancedPServer.getLoadBalancedPServerbyId(rs.getLong(1)));
                }
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

    public TemplateModel FM_getLserverList() throws Exception {
        return new TemplateList(getLserverList());
    }

    public static List getLserverList() throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM l_server ORDER BY id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LogicalServer l = LogicalServer.get(rs.getLong(1));
                list.add(l);
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

    public synchronized TemplateModel FM_addLserver(String name, int group, String fileServer, String description, String path, int type_id, int p_server_id, int signup) throws Exception {
        return LogicalServer.create(name, group, fileServer, description, path, type_id, p_server_id, signup);
    }

    public synchronized TemplateModel FM_addPserver(String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int osType) throws Exception {
        return PhysicalServer.create(name, ip1, mask1, ip2, mask2, login, password, osType);
    }

    public synchronized TemplateModel FM_getLserver(long id) throws Exception {
        return LogicalServer.get(id);
    }

    public synchronized TemplateModel FM_getPserver(long id) throws Exception {
        return PhysicalServer.get(id);
    }

    public TemplateModel FM_reloadHosts() throws Exception {
        HostManager.loadHosts(true);
        return this;
    }

    public static String getGroupNameById(int id) {
        return String.valueOf(groupMap.get(String.valueOf(id)));
    }

    public TemplateModel FM_getTypeByGroup(int groupId) throws Exception {
        return new TemplateString(HostManager.getTypeByGroup(groupId));
    }

    public String getTypeNameById(int id) {
        return String.valueOf(typeMap.get(String.valueOf(id)));
    }

    public TemplateModel FM_getMailRelays() throws Exception {
        Collection<String> colres = new ArrayList();
        boolean getRelayError = false;
        TemplateList mrelays_list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement psLogSrv = con.prepareStatement("SELECT l.id, l.name FROM l_server l, l_server_groups lg WHERE lg.type_id = 3 AND lg.id <> 23 AND l.group_id = lg.id AND l.type_id = 1");
        PreparedStatement psRelays = con.prepareStatement("SELECT action, ip, note FROM admin_mail_relay WHERE mserver_id = ?");
        PreparedStatement psIns = con.prepareStatement("INSERT INTO admin_mail_relay(mserver_id, action, ip, note) VALUES(?, ?, ?, ?)");
        PreparedStatement psDel = con.prepareStatement("DELETE FROM admin_mail_relay WHERE mserver_id = ?");
        try {
            ResultSet rsLogSrv = psLogSrv.executeQuery();
            while (rsLogSrv.next()) {
                boolean needResetDBValues = false;
                ArrayList mailData = new ArrayList();
                HostEntry he = HostManager.getHost(rsLogSrv.getLong(1));
                try {
                    colres = he.exec("mailrelay-get.pl", new ArrayList());
                } catch (Exception e) {
                    getRelayError = true;
                    Session.getLog().error("Can't get mail relay: " + e);
                }
                ArrayList dbData = new ArrayList();
                psRelays.setLong(1, rsLogSrv.getLong(1));
                ResultSet rsRelays = psRelays.executeQuery();
                while (rsRelays.next()) {
                    HashMap data = new HashMap();
                    data.put("action", rsRelays.getString(1));
                    data.put("ip", rsRelays.getString(2));
                    data.put("note", rsRelays.getString(3));
                    dbData.add(data);
                }
                if (!getRelayError) {
                    for (String line : colres) {
                        String ip = line.substring(0, line.indexOf("|"));
                        String action = line.substring(line.indexOf("|") + 1, line.length());
                        String note = "";
                        boolean valueExistsInDB = false;
                        Iterator j = dbData.iterator();
                        while (true) {
                            if (!j.hasNext()) {
                                break;
                            }
                            HashMap data2 = (HashMap) j.next();
                            if (ip.equalsIgnoreCase((String) data2.get("ip")) && action.equalsIgnoreCase((String) data2.get("action"))) {
                                note = (String) data2.get("note");
                                valueExistsInDB = true;
                                break;
                            }
                        }
                        if (!valueExistsInDB || dbData.size() != colres.size()) {
                            needResetDBValues = true;
                        }
                        TemplateModel templateMap = new TemplateMap();
                        templateMap.put("id", rsLogSrv.getString(1));
                        templateMap.put("name", rsLogSrv.getString(2));
                        templateMap.put("action", action);
                        templateMap.put("ip", ip);
                        templateMap.put("note", note);
                        mrelays_list.add(templateMap);
                        HashMap data3 = new HashMap();
                        data3.put("id", rsLogSrv.getString(1));
                        data3.put("name", rsLogSrv.getString(2));
                        data3.put("action", action);
                        data3.put("ip", ip);
                        data3.put("note", note);
                        mailData.add(data3);
                    }
                    if (needResetDBValues) {
                        psDel.setLong(1, rsLogSrv.getLong(1));
                        psDel.executeUpdate();
                        Iterator j2 = mailData.iterator();
                        while (j2.hasNext()) {
                            HashMap data4 = (HashMap) j2.next();
                            psIns.setLong(1, rsLogSrv.getLong(1));
                            psIns.setString(2, (String) data4.get("action"));
                            psIns.setString(3, (String) data4.get("ip"));
                            psIns.setString(4, (String) data4.get("note"));
                            try {
                                psIns.executeUpdate();
                            } catch (SQLException ex) {
                                Session.getLog().error("Can't insert record into admin_mail_relay: mserver_id=" + rsLogSrv.getLong(1) + ",action=" + ((String) data4.get("action")) + ",ip=" + ((String) data4.get("ip")) + ",note=" + ((String) data4.get("note")) + ":" + ex);
                            }
                        }
                    }
                } else {
                    Iterator j3 = dbData.iterator();
                    while (j3.hasNext()) {
                        HashMap data5 = (HashMap) j3.next();
                        TemplateModel templateMap2 = new TemplateMap();
                        templateMap2.put("id", rsLogSrv.getString(1));
                        templateMap2.put("name", rsLogSrv.getString(2));
                        templateMap2.put("action", data5.get("action"));
                        templateMap2.put("ip", data5.get("ip"));
                        templateMap2.put("note", Localizer.translateMessage("eeman.relay.get_error", null));
                        mrelays_list.add(templateMap2);
                    }
                }
            }
            return mrelays_list;
        } finally {
            Session.closeStatement(psLogSrv);
            Session.closeStatement(psRelays);
            Session.closeStatement(psIns);
            Session.closeStatement(psDel);
            con.close();
        }
    }

    public TemplateModel FM_getIMailRelays() throws Exception {
        TemplateList mrelays_list = new TemplateList();
        Connection con = MailServices.getIrisConnection("IrisConfig");
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery("SELECT wildcard, notes FROM RelayAccess;");
            while (rs.next()) {
                TemplateMap mrelay = new TemplateMap();
                mrelay.put("ip", rs.getString(1));
                mrelay.put("notes", rs.getString(2));
                mrelays_list.add((TemplateModel) mrelay);
            }
            return mrelays_list;
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    public TemplateModel FM_getMailServersList() throws Exception {
        TemplateList mservers_list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.id, l.name FROM l_server l, l_server_groups lg WHERE lg.type_id = 3 AND lg.id <> 23 AND l.group_id = lg.id AND l.type_id = 1");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap mserver = new TemplateMap();
                mserver.put("id", rs.getString(1));
                mserver.put("name", rs.getString(2));
                mservers_list.add((TemplateModel) mserver);
            }
            Session.closeStatement(ps);
            con.close();
            return mservers_list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_addMailRelay(long mserver_id, String action, String relay, String note) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO admin_mail_relay(mserver_id, action, ip, note) VALUES(?, ?, ?, ?)");
                ps.setLong(1, mserver_id);
                ps.setString(2, action);
                ps.setString(3, relay);
                ps.setString(4, note);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException e) {
                ps.close();
                ps = con.prepareStatement("UPDATE admin_mail_relay SET note = ? WHERE mserver_id = ? AND ip = ? AND action = ?");
                ps.setString(1, note);
                ps.setLong(2, mserver_id);
                ps.setString(3, relay);
                ps.setString(4, action);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            }
            HostEntry he = HostManager.getHost(mserver_id);
            List list = new ArrayList();
            list.add(action);
            list.add(relay);
            he.exec("mailrelay-add", list);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_addIMailRelay(String ip, String notes) throws Exception {
        Connection con = MailServices.getIrisConnection("IrisConfig");
        try {
            String line = "INSERT INTO RelayAccess(relay_access_id, wildcard, notes) VALUES(0,'" + ip + "','" + notes + "')";
            Statement stm = con.createStatement();
            stm.executeUpdate(line);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public void FM_delMailRelay(long mserver_id, String action, String relay) throws Exception {
        HostEntry he = HostManager.getHost(mserver_id);
        List list = new ArrayList();
        list.add(action);
        list.add(relay);
        he.exec("mailrelay-del", list);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM admin_mail_relay WHERE mserver_id = ? AND ip = ? AND action = ?");
            ps.setLong(1, mserver_id);
            ps.setString(2, relay);
            ps.setString(3, action);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_delIMailRelay(String ip) throws Exception {
        Connection con = MailServices.getIrisConnection("IrisConfig");
        try {
            String line = "DELETE FROM RelayAccess WHERE wildcard='" + ip + "'";
            Statement stm = con.createStatement();
            stm.executeUpdate(line);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public TemplateModel isSSLEnabled() throws Exception {
        String result = "0";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM reseller_ssl WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long tmp = rs.getLong(1);
                result = String.valueOf(tmp);
            }
            Session.closeStatement(ps);
            con.close();
            return new TemplateString(result);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_initResellerSSL() throws Exception {
        AdmResellerSSL result = AdmResellerSSL.create();
        return result;
    }

    public TemplateModel FM_deleteResellerSSL(long id) throws Exception {
        AdmResellerSSL tmp = AdmResellerSSL.get(id);
        tmp.delete();
        tmp.deleteKeys();
        return this;
    }

    public TemplateModel FM_getResellerSSL(long id) throws Exception {
        return AdmResellerSSL.get(id);
    }

    public List getMoveableLServersList() throws Exception {
        List moveableList = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.id, l.name FROM l_server l, l_server_groups lg WHERE l.group_id = lg.id AND (lg.type_id = ? OR lg.type_id = ?)");
            ps.setLong(1, 1L);
            ps.setLong(2, 5L);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Hashtable server = new Hashtable();
                HostEntry he = HostManager.getHost(rs.getLong(1));
                server.put("id", rs.getString(1));
                server.put("name", rs.getString(2));
                server.put("allocation_descr", he == null ? "" : he.getAllocationDescription());
                moveableList.add(server);
            }
            Session.closeStatement(ps);
            con.close();
            return moveableList;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getMoveableLServersList() throws Exception {
        return new TemplateList(getMoveableLServersList());
    }

    public TemplateModel FM_getCompatMap() throws Exception {
        TemplateHash map = new TemplateHash();
        List servers = HostManager.getHosts();
        for (int i = 0; i < servers.size(); i++) {
            HostEntry he = (HostEntry) servers.get(i);
            TemplateList l = new TemplateList(HostManager.getHostsByGroupType(he.getGroupType()));
            Session.getLog().debug("Inside HostManager::getCompatMap server=" + he.getName() + " group type=" + he.getGroupType() + " got " + l.size() + " servers for this group type");
            map.put("" + he.getId(), l);
        }
        return map;
    }

    public TemplateModel FM_getTransferReport() {
        AdvReport rep = AdvReport.newInstance("tp_report");
        Session.getLog().debug("Got report report=" + rep);
        return rep;
    }

    public TemplateModel FM_getTransferProcess(long id) throws Exception {
        return TransferProcess.get(id);
    }

    public TemplateModel FM_commenceTransferProcess(long accId, long srcServerId, long targetServerId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id FROM transfer_process WHERE account_id=" + accId);
        if (ps.executeQuery().next()) {
            throw new HSUserException("content.move_in_process");
        }
        Account.getAccount(accId).isMoved(true);
        Session.getLog().debug("Commencing transfer process for account " + accId + " from logical server " + srcServerId + " to " + targetServerId);
        TransferProcess.createTransferProcess(srcServerId, targetServerId, accId);
        return this;
    }

    public TemplateModel FM_deleteTransferProcess(long id) throws Exception {
        TransferProcess tp = TransferProcess.get(id);
        if (tp != null) {
            tp.delete();
        }
        return new TemplateOKResult();
    }

    private long getPserverIDForMServer(String mserverId) throws Exception {
        HostEntry he = HostManager.getHost(mserverId);
        psoft.hsphere.resource.PhysicalServer ps = he.getPServer();
        return ps.getId();
    }

    public TemplateModel FM_notEmptyMailConfig(String mserverId) throws Exception {
        try {
            long pserverId = getPserverIDForMServer(mserverId);
            Params params = Session.getMailServerParams(pserverId);
            if (params == null) {
                params = readConfigParameters(pserverId);
            }
            if (params == null) {
                return new TemplateString(false);
            }
            if (params.size() == 0) {
                return new TemplateString(false);
            }
            return new TemplateString(true);
        } catch (Exception e) {
            Session.getLog().info(e);
            return new TemplateString(false);
        }
    }

    protected Params initializeParamsFromConfig(long pserverId, Params parameters) throws Exception {
        Collection<String> listRes = readParamsFromMailServer(pserverId);
        String name = "";
        String value = "";
        for (String tmpStr : listRes) {
            if (tmpStr.indexOf("#") == 0) {
                StringTokenizer st = new StringTokenizer(tmpStr.substring(1), "=");
                if (st.hasMoreTokens()) {
                    name = st.nextToken();
                    value = "";
                    if (st.hasMoreTokens()) {
                        value = st.nextToken().trim();
                    }
                    for (int index = 0; index < parameters.size(); index++) {
                        BaseParam param = parameters.get(index);
                        if (param.getCurrParamName().equalsIgnoreCase(name)) {
                            BaseParam bp = parameters.get(index);
                            bp.init(value);
                        }
                    }
                }
            } else {
                value = value + "\n" + tmpStr;
                for (int index2 = 0; index2 < parameters.size(); index2++) {
                    BaseParam param2 = parameters.get(index2);
                    if (param2.getCurrParamName().equalsIgnoreCase(name)) {
                        parameters.get(index2).init(value);
                    }
                }
            }
        }
        if (listRes.size() == 0) {
            parameters = getMailServerParamsfromDB(pserverId, parameters);
        }
        return parameters;
    }

    public Params readConfigParameters(long pserverId) throws Exception {
        Params params = Session.getMailServerParams(pserverId);
        if (params != null) {
            return params;
        }
        Params params2 = Session.getBaseMailServerParams();
        if (params2 == null) {
            params2 = readMailServerParamsFromXML();
            if (params2 == null) {
                return null;
            }
            Session.setBaseMailServerParams(params2);
        }
        Params params3 = initializeParamsFromConfig(pserverId, params2);
        Session.setMailServerParams(pserverId, params3);
        return params3;
    }

    public TemplateModel FM_getMailServerParamsList(String mserverId) throws Exception {
        long pserverId = getPserverIDForMServer(mserverId);
        Params params = Session.getMailServerParams(pserverId);
        if (params == null) {
            params = readConfigParameters(pserverId);
        }
        return params;
    }

    public TemplateModel FM_getMailServerParam(String mserverId, String paramName) throws Exception {
        long pserverId = getPserverIDForMServer(mserverId);
        Params params = Session.getMailServerParams(pserverId);
        if (params != null) {
            return params.getParam(paramName);
        }
        return params;
    }

    protected Collection readParamsFromMailServer(long pserverId) throws Exception {
        String[] path = {""};
        try {
            psoft.hsphere.resource.PhysicalServer unixExecutor = psoft.hsphere.resource.PhysicalServer.getPServer(pserverId);
            if (unixExecutor instanceof psoft.hsphere.resource.PhysicalServer) {
                try {
                    Collection listRes = unixExecutor.exec("qmail-get-config", path, null);
                    return listRes;
                } catch (Exception exc) {
                    throw exc;
                }
            }
            throw new Exception("QMail supported only for linux");
        } catch (Exception exc2) {
            throw exc2;
        }
    }

    protected Params readMailServerParamsFromXML() throws Exception {
        Params params = null;
        try {
            MailServerXMLReader xml = new MailServerXMLReader();
            params = xml.getMailServerParams();
        } catch (Exception exc) {
            Session.getLog().info("Not found mail servers config file(*.xml) " + exc.getMessage());
        }
        return params;
    }

    protected void writeConfigParameters(String mserverID, long pserverId, Params params) throws Exception {
        String[] path = {""};
        boolean needQmailRestart = false;
        boolean needSpamdRestart = false;
        StringBuffer input = new StringBuffer();
        Iterator i = params.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (!param.getType().equalsIgnoreCase("label")) {
                if (param instanceof RadioGroup) {
                    Iterator j = ((RadioGroup) param).getParamsList().iterator();
                    while (j.hasNext()) {
                        CheckParam check_param = (CheckParam) j.next();
                        input.append("#" + check_param.getCurrParamName() + "=" + check_param.getValue() + "\n");
                    }
                } else if (param instanceof SelectParam) {
                    int ind = param.getValue().indexOf(param.getCurrParamName());
                    if (ind > -1) {
                        input.append("#" + param.getCurrParamName() + "=" + param.getValue().substring(param.getCurrParamName().length() + 1) + "\n");
                    } else {
                        input.append("#" + param.getCurrParamName() + "=" + param.getValue() + "\n");
                    }
                } else {
                    String val = param.getValue();
                    if (!needQmailRestart && param.isChanged()) {
                        int j2 = 0;
                        while (true) {
                            if (j2 >= NEED_QMAIL_RESTART_PARAMS.length) {
                                break;
                            } else if (!NEED_QMAIL_RESTART_PARAMS[j2].equals(param.getCurrParamName())) {
                                j2++;
                            } else {
                                needQmailRestart = true;
                                break;
                            }
                        }
                    }
                    if (!needSpamdRestart && param.isChanged()) {
                        int j3 = 0;
                        while (true) {
                            if (j3 >= NEED_SPAMD_RESTART_PARAMS.length) {
                                break;
                            } else if (!NEED_SPAMD_RESTART_PARAMS[j3].equals(param.getCurrParamName())) {
                                j3++;
                            } else {
                                needSpamdRestart = true;
                                break;
                            }
                        }
                    }
                    input.append("#" + param.getCurrParamName() + "=" + val + "\n");
                }
            }
        }
        saveMailServerParamsToDB(pserverId, params);
        HostEntry he = HostManager.getHost(mserverID);
        he.exec("qmail-set-config", path, input.toString());
        if (needQmailRestart) {
            he.exec("restart-mail-server", path, new String());
        }
        if (needSpamdRestart) {
            he.exec("restart-spamd", path, new String());
        }
    }

    public TemplateModel FM_updateMailServerParams(String mserverId) throws Exception {
        long pserverId = getPserverIDForMServer(mserverId);
        Params params = Session.getMailServerParams(pserverId);
        try {
            writeConfigParameters(mserverId, pserverId, params);
            return new TemplateString("OK");
        } catch (Exception exc) {
            Session.addMessage("Update params error");
            Session.getLog().info(exc);
            return new TemplateString("ERROR");
        }
    }

    public TemplateModel FM_setunchanged(String mserverId) throws Exception {
        long pserverId = getPserverIDForMServer(mserverId);
        Params params = Session.getMailServerParams(pserverId);
        Iterator i = params.iterator();
        while (i.hasNext()) {
            ((BaseParam) i.next()).setChanged(false);
        }
        return null;
    }

    public TemplateModel FM_foundConfig() throws Exception {
        Params params = readMailServerParamsFromXML();
        if (params == null) {
            return new TemplateString(false);
        }
        return new TemplateString(true);
    }

    private static void saveMailServerParamsToDB(long mserverId, Params params) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("DELETE FROM qmail_params WHERE mserver_id = ?");
        ps.setLong(1, mserverId);
        ps.executeUpdate();
        ps.close();
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO qmail_params(mserver_id, param_name, param_value) VALUES(?, ?, ?)");
        try {
            try {
                Iterator i = params.iterator();
                while (i.hasNext()) {
                    BaseParam param = (BaseParam) i.next();
                    if (param.getValue() != null && !param.getValue().equalsIgnoreCase("")) {
                        ps2.setLong(1, mserverId);
                        ps2.setString(2, param.getCurrParamName());
                        Session.setClobValue(ps2, 3, param.getValue());
                        ps2.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new HSUserException("eeman.relay.add.sql_error");
            }
        } finally {
            Session.closeStatement(ps2);
            con.close();
        }
    }

    private static Params getMailServerParamsfromDB(long mserverId, Params params) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT param_name, param_value FROM qmail_params WHERE mserver_id = ?");
                ps.setLong(1, mserverId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String param_name = rs.getString("param_name");
                    String param_value = rs.getString("param_value");
                    for (int index = 0; index < params.size(); index++) {
                        BaseParam param = params.get(index);
                        if (param.getCurrParamName().equalsIgnoreCase(param_name)) {
                            params.get(index).init(param_value);
                        }
                    }
                }
                Session.closeStatement(ps);
                con.close();
                return params;
            } catch (SQLException e) {
                throw new HSUserException("Cant load qmail parameters from DB");
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    static void physicalCreateConfigParameters(long pserverId) throws Exception {
        Params params = Session.getMailServerParams(pserverId);
        Params params2 = getMailServerParamsfromDB(pserverId, params);
        String[] path = {""};
        boolean needQmailRestart = false;
        boolean needSpamdRestart = false;
        StringBuffer input = new StringBuffer();
        Iterator i = params2.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (!param.getType().equalsIgnoreCase("label")) {
                if (param instanceof RadioGroup) {
                    Iterator j = ((RadioGroup) param).getParamsList().iterator();
                    while (j.hasNext()) {
                        CheckParam check_param = (CheckParam) j.next();
                        input.append(check_param.getCurrParamName() + "=" + check_param.getValue() + "\n");
                    }
                } else {
                    String val = param.getValue();
                    if (!needQmailRestart) {
                        int j2 = 0;
                        while (true) {
                            if (j2 >= NEED_QMAIL_RESTART_PARAMS.length) {
                                break;
                            } else if (!NEED_QMAIL_RESTART_PARAMS[j2].equals(param.getCurrParamName())) {
                                j2++;
                            } else {
                                needQmailRestart = true;
                                break;
                            }
                        }
                    }
                    if (!needSpamdRestart) {
                        int j3 = 0;
                        while (true) {
                            if (j3 >= NEED_SPAMD_RESTART_PARAMS.length) {
                                break;
                            } else if (!NEED_SPAMD_RESTART_PARAMS[j3].equals(param.getCurrParamName())) {
                                j3++;
                            } else {
                                needSpamdRestart = true;
                                break;
                            }
                        }
                    }
                    input.append(param.getCurrParamName() + "=" + val + "\n");
                }
            }
        }
        HostEntry he = HostManager.getHost(pserverId);
        he.exec("qmail-set-config", path, input.toString());
        if (needQmailRestart) {
            he.exec("restart-mail-server", path, new String());
        }
        if (needSpamdRestart) {
            he.exec("restart-spamd", path, new String());
        }
    }

    public TemplateModel FM_getNetwrokGateway(String addr) throws Exception {
        return new TemplateHash(NetworkGatewayManager.getManager().getNetworkGateway(addr));
    }

    public TemplateModel FM_addNetworkGateway(String addr, String gateway, String mask) throws Exception {
        NetworkGatewayManager.getManager().addNetworkGateway(addr, gateway, mask);
        return this;
    }

    public TemplateModel FM_deleteNetworkGateway(String addr) throws Exception {
        NetworkGatewayManager.getManager().deleteNetworkGateway(addr);
        return this;
    }

    public TemplateModel FM_getNetworkGateways() throws Exception {
        return NetworkGatewayManager.getManager().getNetworkGateways();
    }

    public TemplateModel FM_assignDevice(long serverId, String addr, String device) throws Exception {
        NetworkGatewayManager.getManager().assignDevice(serverId, addr, device);
        return this;
    }

    public TemplateString FM_getSubnetMaskByIP(String ip) throws Exception {
        return new TemplateString(NetworkGatewayManager.getManager().getSubnetMaskByIP(ip));
    }

    public TemplateList FM_getParentLoadBalancedPServers(int ps_id) throws Exception {
        return new TemplateList(LoadBalancedPServer.getParentLoadBalancedPServers(ps_id));
    }

    public TemplateModel FM_insertLoadBalancedPServer(long id, long parentId) throws Exception {
        TemplateHash result = new TemplateHash();
        switch (LoadBalancedPServer.insertLoadBalancedPServer(id, parentId)) {
            case 0:
                return this;
            case 1:
                result.put("status", "containsLServers");
                break;
            case 2:
                result.put("status", "webAlreadyContainsSlave");
                break;
            case 3:
                result.put("status", "noWebOrMail");
                break;
        }
        return result;
    }

    public synchronized TemplateModel FM_getLoadBalServer(long id) throws Exception {
        return LoadBalancedPServer.getLoadBalancedPServerbyId(id);
    }

    public TemplateString FM_existsLServer(long id) throws Exception {
        if (LogicalServer.existsLServer(id)) {
            return new TemplateString("true");
        }
        return null;
    }

    public int isLBServer(long ps_id) throws Exception {
        int result;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT is_parent FROM p_server p INNER JOIN loadbalanced_pserver lb USING (id) WHERE p.id = ?");
            ps.setLong(1, ps_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getString(1) == null) {
                    result = 0;
                } else {
                    result = rs.getInt(1);
                }
            } else {
                result = -1;
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateString FM_isLBServer(long ps_id) throws Exception {
        return new TemplateString(isLBServer(ps_id));
    }

    public static void setTypesWithPhParams() throws Exception {
        Document doc = XMLManager.getXML("PHYSICAL_SERVERS_PARAMS_FILE");
        Element root = doc.getDocumentElement();
        NodeList groupTypes = XPathAPI.selectNodeList(root, "//group[@name!='']");
        for (int i = 0; i < groupTypes.getLength(); i++) {
            Element curType = (Element) groupTypes.item(i);
            String groupType = curType.getAttribute("name");
            long groupTypeId = Long.valueOf((String) HostEntry.getGroupTypeToIdHash().get(groupType)).longValue();
            groupTypesWithPhParams.add(Long.valueOf(groupTypeId));
        }
    }

    public static List<Long> getTypesWithPhParams() throws Exception {
        if (groupTypesWithPhParams.size() < 1) {
            setTypesWithPhParams();
        }
        return new ArrayList(groupTypesWithPhParams);
    }
}
