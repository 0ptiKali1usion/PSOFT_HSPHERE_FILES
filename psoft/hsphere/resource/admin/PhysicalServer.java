package psoft.hsphere.resource.admin;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinPhysicalServer;
import psoft.hsphere.resource.admin.params.BaseParam;
import psoft.hsphere.resource.admin.params.CheckParam;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.admin.params.RadioGroup;
import psoft.hsphere.resource.admin.params.SelectParam;
import psoft.hsphere.servmon.MonitoringThread;
import psoft.hsphere.servmon.ServerInfo;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/PhysicalServer.class */
public class PhysicalServer extends SharedObject implements TemplateHashModel {
    protected String name;
    protected String ip1;
    protected String mask1;
    protected String ip2;
    protected String mask2;
    protected String login;
    protected String password;
    protected int status;
    protected int osType;
    protected int getServerInfo;
    protected psoft.hsphere.resource.PhysicalServer phServer;
    protected Hashtable config;
    protected Params paramsList;
    protected String paramsPath;
    protected boolean xmlFileNotFound;
    public static boolean RESTART_WINDOWS_SERVICE = false;

    public String getLogin() {
        return getPhysicalServer().getLogin();
    }

    public int getOsType() {
        return getPhysicalServer().getOsType();
    }

    public String getPassword() {
        return getPhysicalServer().getPassword();
    }

    public int getStatus() {
        return getPhysicalServer().getStatus();
    }

    public String getName() {
        return getPhysicalServer().getName();
    }

    public String getIp1() {
        return getPhysicalServer().getPFirstIP();
    }

    public String getMask1() {
        return getPhysicalServer().getMask1();
    }

    public String getIp2() {
        return getPhysicalServer().getPSecondIP();
    }

    public String getMask2() {
        return getPhysicalServer().getMask2();
    }

    public boolean isAuthorized() {
        return getPhysicalServer().isAuthorized();
    }

    public int getGetServerInfo() {
        return this.getServerInfo;
    }

    public PhysicalServer(long id, String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int status, int osType) throws Exception {
        this(id, name, ip1, mask1, ip2, mask2, login, password, status, osType, 1);
    }

    public PhysicalServer(long id, String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int status, int osType, int getServerInfo) throws Exception {
        super(id);
        this.xmlFileNotFound = false;
        this.phServer = psoft.hsphere.resource.PhysicalServer.getPServer(id);
        getPhysicalServer().checkIfAuthorized();
        this.getServerInfo = getServerInfo;
    }

    public static PhysicalServer create(String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int osType) throws Exception {
        return create(name, ip1, mask1, ip2, mask2, login, password, osType, 1);
    }

    public static PhysicalServer create(String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int osType, int getServerInfo) throws Exception {
        String internalPassword = "";
        if (osType != 1) {
            internalPassword = password;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            long id = Session.getNewIdAsLong("physical_seq");
            ps = con.prepareStatement("INSERT INTO p_server (id, name, ip1, mask1, ip2, mask2, login, password, status, os_type, get_server_info) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)");
            ps.setLong(1, id);
            ps.setString(2, name);
            ps.setString(3, ip1);
            ps.setString(4, mask1);
            ps.setString(5, ip2);
            ps.setString(6, mask2);
            ps.setString(7, login);
            ps.setString(8, internalPassword);
            ps.setInt(9, osType);
            ps.setInt(10, getServerInfo);
            ps.executeUpdate();
            PhysicalServer ph = new PhysicalServer(id, name, ip1, mask1, ip2, mask2, login, password, 0, osType, getServerInfo);
            ph.getPhysicalServer().setPassword(password);
            Session.closeStatement(ps);
            con.close();
            return ph;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public psoft.hsphere.resource.PhysicalServer getPhysicalServer() {
        return this.phServer;
    }

    public TemplateModel FM_delete() throws Exception {
        delete();
        return new TemplateOKResult();
    }

    public void delete() throws Exception {
        if (getLServers().iterator().hasNext()) {
            throw new HSUserException("eeman.error_deleting_pserver");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM p_server_group_map WHERE ps_id = ?");
            ps2.setLong(1, getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM p_server WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            remove(this.f51id);
            getPhysicalServer().delete();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void remove(long id) throws Exception {
        remove(id, PhysicalServer.class);
    }

    public static PhysicalServer get(long id) throws Exception {
        PhysicalServer p = (PhysicalServer) get(id, PhysicalServer.class);
        if (p == null) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT name, ip1, mask1, ip2, mask2, login, password, status, os_type, get_server_info FROM p_server WHERE id = ?");
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    p = new PhysicalServer(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8), rs.getInt(9), rs.getInt(10));
                } else {
                    throw new Exception("PServer not found");
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return p;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(getId());
        }
        if (key.equals("name")) {
            return new TemplateString(getName());
        }
        if (key.equals("ip1")) {
            return new TemplateString(getIp1());
        }
        if (key.equals("mask1")) {
            return new TemplateString(getMask1());
        }
        if (key.equals("ip2")) {
            return new TemplateString(getIp2());
        }
        if (key.equals("mask2")) {
            return new TemplateString(getMask2());
        }
        if (key.equals("login")) {
            return new TemplateString(getLogin());
        }
        if (key.equals("password")) {
            return new TemplateString(getPassword());
        }
        if (key.equals("pstatus")) {
            return new TemplateString(getStatus());
        }
        if (key.equals("os_type")) {
            return new TemplateString(getOsType());
        }
        if (key.equals("authorized")) {
            if (isAuthorized()) {
                return new TemplateString("1");
            }
            return null;
        } else if (!key.equals("server_info")) {
            return key.equals("get_server_info") ? new TemplateString(getGetServerInfo()) : super.get(key);
        } else {
            ServerInfo sInfo = MonitoringThread.getServerInfo(getId());
            if (sInfo != null) {
                return MonitoringThread.getServerInfo(this.f51id).get();
            }
            return null;
        }
    }

    public TemplateModel FM_getLServers() throws Exception {
        return new TemplateList(getLServers());
    }

    public Collection getLServers() throws Exception {
        ArrayList result = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM l_server WHERE p_server_id = ? ORDER BY id");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
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

    public TemplateModel FM_update(String ip1, String mask1, String ip2, String mask2, String login, String password) throws Exception {
        Connection con = Session.getDb();
        try {
            if (getOsType() == 1) {
                if (password == null) {
                    password = "";
                }
                String oldPassword = getPhysicalServer().getPassword();
                if (oldPassword == null) {
                }
                boolean isChangedIp = false;
                if (!getPhysicalServer().getPFirstIP().equals(ip1)) {
                    getPhysicalServer().setIp1(ip1);
                    isChangedIp = true;
                }
                if (login != null && !"".equals(login) && !login.equals(getLogin())) {
                    getPhysicalServer().setLogin(login);
                    isChangedIp = true;
                }
                if ((!password.equals(getPhysicalServer().getPassword()) || isChangedIp || !getPhysicalServer().isAuthorized()) && !getPhysicalServer().checkIfAuthorized()) {
                    getPhysicalServer().setPassword(password);
                    if (!getPhysicalServer().checkIfAuthorized()) {
                        Object[] args = {getName()};
                        throw new HSUserException("msg.phserver_authorization_problem", args);
                    }
                    getPhysicalServer().setPassword(null);
                    password = null;
                }
            }
            PreparedStatement ps = con.prepareStatement("UPDATE p_server SET ip1 = ?, mask1 = ?," + ((ip2 == null || "".equals(ip2)) ? "ip2 = NULL," : "ip2 = ?,") + ((mask2 == null || "".equals(mask2)) ? "mask2 = NULL," : "mask2 = ?,") + "login= ?" + ((password == null || "".equals(password)) ? "" : ",password = ? ") + " WHERE id = ?");
            int i = 1 + 1;
            ps.setString(1, ip1 == null ? "" : ip1);
            int i2 = i + 1;
            ps.setString(i, mask1 == null ? "" : mask1);
            if (ip2 != null && !"".equals(ip2)) {
                i2++;
                ps.setString(i2, ip2);
            }
            if (mask2 != null && !"".equals(mask2)) {
                int i3 = i2;
                i2++;
                ps.setString(i3, mask2);
            }
            int i4 = i2;
            int i5 = i2 + 1;
            ps.setString(i4, login == null ? "" : login);
            if (password != null && !"".equals(password)) {
                i5++;
                ps.setString(i5, password);
            }
            int i6 = i5;
            int i7 = i5 + 1;
            ps.setLong(i6, getId());
            ps.executeUpdate();
            getPhysicalServer().setIp1(ip1);
            getPhysicalServer().setMask1(mask1);
            getPhysicalServer().setIp2(ip2);
            getPhysicalServer().setMask2(mask2);
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public Collection getGroups() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT group_id FROM p_server_group_map WHERE ps_id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            Collection list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
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

    public TemplateModel FM_getGroups() throws Exception {
        return new TemplateList(getGroups());
    }

    public TemplateModel FM_getUniqueTypes() throws Exception {
        return new TemplateList(getUniqueTypes());
    }

    public List getUniqueTypes() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT DISTINCT type_id FROM l_server_groups l, p_server_group_map p WHERE ps_id = ? AND p.group_id = l.id");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                List list = new ArrayList();
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
                Session.closeStatement(ps);
                con.close();
                return list;
            } catch (Exception e) {
                Session.getLog().info("Problem", e);
                Session.closeStatement(ps);
                con.close();
                return null;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addGroup(int group) throws Exception {
        for (String str : getGroups()) {
            if (group == Integer.parseInt(str)) {
                return this;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO p_server_group_map (ps_id, group_id) VALUES (?, ?)");
            ps.setLong(1, getId());
            ps.setInt(2, group);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delGroup(int group) throws Exception {
        for (String str : getLServers()) {
            if (LogicalServer.get(Long.parseLong(str)).getGroup() == group) {
                throw new HSUserException("eeman.error_deleting_pserver.group");
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM p_server_group_map WHERE ps_id = ? AND group_id = ?");
            ps.setLong(1, getId());
            ps.setInt(2, group);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    protected int getParamGroupID(BaseParam param) {
        String[] customParams = param.getCustomParams();
        return Integer.parseInt(customParams[0]);
    }

    protected Params getSelectedParams(Params parsList, int groupID) throws Exception {
        Params params = new Params();
        for (int i = 0; i < parsList.size(); i++) {
            BaseParam param = (BaseParam) parsList.get(i);
            if (groupID == -1) {
                if (getParamGroupID(param) == groupID) {
                    params.add(param);
                }
            } else if (getParamGroupID(param) != -1 && HostManager.getTypeByGroup(getParamGroupID(param)) == HostManager.getTypeByGroup(groupID)) {
                params.add(param);
            }
        }
        return params;
    }

    public TemplateModel FM_getGroupsIDs() throws Exception {
        Collection list = new ArrayList();
        list.add("-1");
        list.addAll(getUniqueTypes());
        return new TemplateList(list);
    }

    public TemplateModel FM_getParamsList() throws Exception {
        if (this.config == null) {
            readConfigParameters();
        }
        return this.paramsList;
    }

    public TemplateModel FM_getParamsList(String grID) throws Exception {
        try {
            int groupID = Integer.parseInt(grID);
            if (this.config == null) {
                readConfigParameters();
            }
            return getSelectedParams(this.paramsList, groupID);
        } catch (Exception e) {
            Session.getLog().error("Incorrect groupid attribute value");
            return null;
        }
    }

    public TemplateModel FM_notEmpty() throws Exception {
        if (this.config == null) {
            readConfigParameters();
        }
        if (this.paramsList == null) {
            return new SimpleScalar(false);
        }
        if (this.paramsList.size() == 0) {
            return new SimpleScalar(false);
        }
        return new SimpleScalar(true);
    }

    protected Collection readParamsFromServer(String[] path) throws Exception {
        if (!HostEntry.getEmulationMode()) {
            Collection listRes = null;
            try {
                psoft.hsphere.resource.PhysicalServer unixExecutor = psoft.hsphere.resource.PhysicalServer.getPServer(this.f51id);
                if (unixExecutor instanceof WinPhysicalServer) {
                    try {
                        Collection args = new ArrayList();
                        WinPhysicalServer winExecutor = (WinPhysicalServer) unixExecutor;
                        listRes = winExecutor.exec("config-get.asp", args);
                    } catch (Exception exc) {
                        throw exc;
                    }
                } else if (getPhysicalServer().isAuthorized()) {
                    listRes = unixExecutor.exec("get-config", path, null);
                }
                return listRes;
            } catch (Exception exc2) {
                throw exc2;
            }
        }
        return new ArrayList();
    }

    protected void readConfigParameters() throws Exception {
        Collection<String> listRes;
        this.config = new Hashtable();
        String[] path = {""};
        getParamsFromXML();
        if (this.paramsList == null || (listRes = readParamsFromServer(path)) == null) {
            return;
        }
        for (String tmpStr : listRes) {
            StringTokenizer st = new StringTokenizer(tmpStr, "=");
            if (st.hasMoreTokens()) {
                String name = st.nextToken();
                String value = "";
                if (st.hasMoreTokens()) {
                    value = st.nextToken();
                }
                for (int index = 0; index < this.paramsList.size(); index++) {
                    BaseParam param = this.paramsList.get(index);
                    if (param.getCurrParamName().equalsIgnoreCase(name)) {
                        this.paramsList.get(index).init(value);
                    }
                }
                this.config.put(name, value);
            }
        }
    }

    protected String getLastChangedParamName(Params paramsList) {
        String name = null;
        Iterator i = paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (param.isChanged()) {
                name = param.getCurrParamName();
            }
        }
        return name;
    }

    protected void writeConfigParameters() throws Exception {
        StringBuffer input = new StringBuffer();
        String[] path = {""};
        psoft.hsphere.resource.PhysicalServer unixExecutor = psoft.hsphere.resource.PhysicalServer.getPServer(this.f51id);
        String lastChangedParam = getLastChangedParamName(this.paramsList);
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (param.isChanged() && !param.getType().equalsIgnoreCase("label")) {
                if (unixExecutor instanceof WinPhysicalServer) {
                    WinPhysicalServer winExecutor = (WinPhysicalServer) unixExecutor;
                    ArrayList winInput = new ArrayList();
                    String[] str = {param.getCurrParamName().toString(), param.getValue().toString()};
                    winInput.add(str);
                    winExecutor.exec("config-set.asp", winInput);
                    if (RESTART_WINDOWS_SERVICE && lastChangedParam != null && lastChangedParam.equalsIgnoreCase(param.getCurrParamName())) {
                        try {
                            winExecutor.exec("hssvc-restart.asp", new ArrayList());
                        } catch (Exception e) {
                            Session.addMessage("ERROR RESTART SERVICE. Please try again.");
                        }
                    }
                } else if (param instanceof RadioGroup) {
                    Iterator j = ((RadioGroup) param).getParamsList().iterator();
                    while (j.hasNext()) {
                        CheckParam check_param = (CheckParam) j.next();
                        input.append(check_param.getCurrParamName() + "=" + check_param.getValue() + "\n");
                    }
                } else if (param instanceof SelectParam) {
                    int ind = param.getValue().indexOf(param.getCurrParamName());
                    if (ind > -1) {
                        input.append(param.getCurrParamName() + "=" + param.getValue().substring(param.getCurrParamName().length() + 1) + "\n");
                    } else {
                        input.append(param.getCurrParamName() + "=" + param.getValue() + "\n");
                    }
                } else {
                    input.append(param.getCurrParamName() + "=" + param.getValue() + "\n");
                }
            }
        }
        if (!input.toString().equals("")) {
            unixExecutor.exec("set-config", path, input.toString());
        }
    }

    public TemplateModel FM_update() throws Exception {
        try {
            writeConfigParameters();
            return new TemplateString("OK");
        } catch (Exception exc) {
            Session.addMessage("Update params error");
            Session.getLog().info(exc);
            return new TemplateString("ERROR");
        }
    }

    public TemplateModel FM_setunchanged() throws Exception {
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            ((BaseParam) i.next()).setChanged(false);
        }
        return null;
    }

    protected void getParamsFromXML() throws Exception {
        if (this.paramsList == null) {
            Params allParams = Session.getAllPhysicalServersParams();
            if (allParams == null) {
                this.paramsList = rereadAllPserversParams();
                return;
            } else if (Session.getPserverID() == getId()) {
                this.paramsList = allParams;
                return;
            } else {
                this.paramsList = rereadAllPserversParams();
                return;
            }
        }
        this.paramsList = null;
    }

    protected Params rereadAllPserversParams() throws Exception {
        Params params = null;
        try {
            PhysicalServerXMLReader xml = new PhysicalServerXMLReader();
            params = xml.getPhysicalServerParams(this);
            Session.setAllPhysicalServersParams(params);
            Session.setPserverID(getId());
        } catch (Exception exc) {
            this.xmlFileNotFound = true;
            Session.getLog().error("Not found physical servers config file(*.xml) or smth. else: ", exc);
        }
        return params;
    }

    public TemplateModel FM_encode(String str) {
        return new SimpleScalar(URLEncoder.encode(str));
    }

    public TemplateModel FM_htmlencode(String str) {
        return new SimpleScalar(HTMLEncoder.encode(str));
    }

    public TemplateModel FM_paramsnotempty(String groupId) throws Exception {
        if (this.paramsList == null) {
            return new SimpleScalar(false);
        }
        if (this.paramsList.size() == 0) {
            return new SimpleScalar(false);
        }
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (getParamGroupID(param) == Integer.parseInt(groupId)) {
                return new SimpleScalar(true);
            }
        }
        return new SimpleScalar(false);
    }

    public TemplateModel FM_notfoundconfig() throws Exception {
        if (this.xmlFileNotFound) {
            return new SimpleScalar(true);
        }
        return new SimpleScalar(false);
    }

    public TemplateModel FM_ischanged(String name, String value) throws Exception {
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (param.getCurrParamName().equalsIgnoreCase(name)) {
                if (param.getCurrParamValue().equalsIgnoreCase(value)) {
                    return new SimpleScalar(false);
                }
                return new SimpleScalar(true);
            }
        }
        Session.getLog().error("Physical server param with name -" + name + " not found");
        return null;
    }

    public TemplateModel FM_iswindows() throws Exception {
        psoft.hsphere.resource.PhysicalServer server = psoft.hsphere.resource.PhysicalServer.getPServer(getId());
        if (server instanceof WinPhysicalServer) {
            return new SimpleScalar(true);
        }
        return new SimpleScalar(false);
    }

    public TemplateModel FM_setrestart(String restart) throws Exception {
        if (restart != null) {
            if (restart.equalsIgnoreCase("on")) {
                RESTART_WINDOWS_SERVICE = true;
                return null;
            }
            RESTART_WINDOWS_SERVICE = false;
            return null;
        }
        RESTART_WINDOWS_SERVICE = false;
        return null;
    }

    protected boolean checkConfig() throws Exception {
        String[] path = {""};
        try {
            new PhysicalServerXMLReader();
            readParamsFromServer(path);
            return true;
        } catch (Exception exc) {
            Session.getLog().error("Not found physical servers config file(*.xml) or smth. else: ", exc);
            return false;
        }
    }

    public TemplateModel FM_setGetServerInfo(int getServerInfo) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE p_server SET get_server_info = ? WHERE id = ?");
            ps.setInt(1, getServerInfo);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.getServerInfo = getServerInfo;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean hasParams() throws Exception {
        boolean hasParams = false;
        Iterator i$ = getUniqueTypes().iterator();
        while (true) {
            if (!i$.hasNext()) {
                break;
            }
            Object curType = i$.next();
            if (EnterpriseManager.getTypesWithPhParams().contains(Long.valueOf((String) curType))) {
                hasParams = true;
                break;
            }
        }
        return hasParams;
    }

    public TemplateString FM_hasParams() throws Exception {
        return new TemplateString(hasParams());
    }
}
