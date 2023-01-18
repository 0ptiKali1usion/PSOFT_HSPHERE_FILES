package psoft.hsphere.resource.apache;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.VHostSettings;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/VirtualHostingResource.class */
public class VirtualHostingResource extends Resource implements IPDependentResource, HostDependentResource, VHostSettings {
    protected int index;
    protected int symlink;
    protected int ssi;
    protected int multiview;
    protected String dir;
    protected String entry;
    protected List scriptList;
    protected String[] indexValues;
    protected String[] symlinkValues;
    protected String[] ssiValues;
    protected String[] multiviewValues;
    protected String INDEX;
    protected String SYMLINK;
    protected String SSI;
    protected String MILTIVIEW;

    public VirtualHostingResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.scriptList = new ArrayList();
        this.indexValues = new String[]{"disabled", "enabled", "fancy"};
        this.symlinkValues = new String[]{"disabled", "enabled", "owner"};
        this.ssiValues = new String[]{"disabled", "enabled"};
        this.multiviewValues = new String[]{"disabled", "enabled"};
        this.INDEX = "index";
        this.SYMLINK = "symlink";
        this.SSI = "ssi";
        this.MILTIVIEW = "multiview";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.dir = (String) i.next();
            if (i.hasNext()) {
                this.index = Integer.parseInt((String) i.next());
                if (i.hasNext()) {
                    this.symlink = Integer.parseInt((String) i.next());
                }
                if (i.hasNext()) {
                    this.ssi = Integer.parseInt((String) i.next());
                }
                if (i.hasNext()) {
                    this.multiview = Integer.parseInt((String) i.next());
                    return;
                }
                return;
            }
            initDirParam();
            return;
        }
        this.dir = null;
        initDirParam();
    }

    public VirtualHostingResource(ResourceId rId) throws Exception {
        super(rId);
        this.scriptList = new ArrayList();
        this.indexValues = new String[]{"disabled", "enabled", "fancy"};
        this.symlinkValues = new String[]{"disabled", "enabled", "owner"};
        this.ssiValues = new String[]{"disabled", "enabled"};
        this.multiviewValues = new String[]{"disabled", "enabled"};
        this.INDEX = "index";
        this.SYMLINK = "symlink";
        this.SSI = "ssi";
        this.MILTIVIEW = "multiview";
        Session.getLog().debug("Apache loaded" + this);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT entry, dir, indx, symlink, ssi, multiview FROM  apache_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.entry = Session.getClobValue(rs, 1);
                this.dir = rs.getString(2);
                this.index = rs.getInt(3);
                this.symlink = rs.getInt(4);
                this.ssi = rs.getInt(5);
                this.multiview = rs.getInt(6);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        Session.getLog().debug("Apache was restarted by IP");
        restart();
    }

    public TemplateModel FM_getIP() throws Exception {
        try {
            return getIp().get("ip");
        } catch (Exception e) {
            Session.getLog().error(e);
            return null;
        }
    }

    public Resource getIp() throws Exception {
        Resource r = getParent().get();
        ResourceId ipId = r.FM_getChild("ip");
        if (ipId != null) {
            return ipId.get();
        }
        return null;
    }

    public boolean hasDedicatedIp() throws Exception {
        return !"1".equals(getIp().get("shared").toString());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            HostEntry he = recursiveGet("host");
            synchronized (he) {
                List l = new ArrayList();
                l.add(getId().getId() + ".conf");
                he.exec("apache-delconf", l);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getServerAdmin() throws Exception {
        return new TemplateString("webmaster@" + recursiveGet("name").toString());
    }

    public String getPath() throws Exception {
        return recursiveGet("dir") + "/" + this.dir;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("server")) {
            return new TemplateString("apache");
        }
        try {
            if (key.equals("config")) {
                return new TemplateString(getConfigEntry());
            }
            if (key.equals("path")) {
                return new TemplateString(getPath());
            }
            if (key.equals("state")) {
                if (this.entry.equals(getConfigEntry())) {
                    return this;
                }
                return null;
            } else if (key.equals("script_list")) {
                return new TemplateList(getScriptList());
            } else {
                if (key.equals("index")) {
                    return new TemplateString(this.index);
                }
                if (key.equals("symlink")) {
                    return new TemplateString(this.symlink);
                }
                if (key.equals("ssi")) {
                    return new TemplateString(this.ssi);
                }
                if (key.equals("multiview")) {
                    return new TemplateString(this.multiview);
                }
                if (key.equals("local_dir")) {
                    return new TemplateString(this.dir);
                }
                if (key.equals("ip")) {
                    try {
                        return FM_getIP();
                    } catch (Exception e) {
                        return null;
                    }
                }
                return super.get(key);
            }
        } catch (Exception e2) {
            getLog().warn("vh cofig " + key, e2);
            return null;
        }
    }

    public void initDirParam() throws Exception {
        try {
            this.index = Integer.parseInt(getPlanValue("INDEX"));
        } catch (NullPointerException e) {
        }
        try {
            this.symlink = Integer.parseInt(getPlanValue("SYMLINK"));
        } catch (NullPointerException e2) {
        }
        try {
            this.ssi = Integer.parseInt(getPlanValue("SSI"));
        } catch (NullPointerException e3) {
        }
        try {
            this.multiview = Integer.parseInt(getPlanValue("MULTIVIEW"));
        } catch (NullPointerException e4) {
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        initContent();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO apache_vhost (id, host_id, entry, dir, indx, symlink, ssi, multiview) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, Integer.parseInt(recursiveGet("host_id").toString()));
            Session.setClobValue(ps, 3, getConfigEntry());
            ps.setString(4, this.dir);
            ps.setInt(5, this.index);
            ps.setInt(6, this.symlink);
            ps.setInt(7, this.ssi);
            ps.setInt(8, this.multiview);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void postInitDone() throws Exception {
        restart();
    }

    public void restart() throws Exception {
        restart(false);
    }

    public void restart(boolean force) throws Exception {
        HostEntry he = recursiveGet("host");
        String tmp_entry = getConfigEntry();
        if (this.entry != null && this.entry.equals(tmp_entry) && !force) {
            Session.getLog().info("Skip Apache restart, equals configs");
            return;
        }
        Session.getLog().info("Before synch");
        synchronized (he) {
            Session.getLog().info("After synch");
            List l = new ArrayList();
            l.add(getId().getId() + ".conf");
            he.exec("apache-saveconf", l, tmp_entry);
            List l2 = new ArrayList();
            if (force) {
                l2.add("force");
            }
            he.exec("apache-reconfig", l2);
            this.entry = tmp_entry;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_vhost SET entry = ? WHERE id = ?");
            Session.setClobValue(ps, 1, getConfigEntry());
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void initContent() throws Exception {
        String idomain;
        Resource domain = getParent().get();
        String name = domain.get("name").toString();
        if (this.dir == null) {
            this.dir = ((Domain) domain).getName();
        }
        HostEntry he = recursiveGet("host");
        String login = recursiveGet("login").toString();
        String group = recursiveGet("group").toString();
        String systemUserId = Long.toString(getParent().getId());
        ResourceId ip = domain.FM_getChild("ip");
        if (ip.get("shared").toString().equals("1")) {
            ResourceId idomainRid = FM_getChild("idomain_alias");
            if (idomainRid != null) {
                idomain = FM_getChild("idomain_alias").get("alias").toString();
            } else {
                idomain = name;
            }
        } else {
            idomain = Session.int2ext(ip.get("ip").toString());
        }
        String ssPath = Session.getProperty("PATH_SITE_STUDIO");
        String ssClass = Session.getProperty("SITE_STUDIO_CLASS");
        if (ssPath.length() == 0) {
            Session.getLog().error("PATH_SITE_STUDIO is not defined in hsphere.properties");
            ssPath = "sitestudio_path_is_not_defined";
        }
        if (ssClass.length() == 0) {
            Session.getLog().error("SITE_STUDIO_CLASS is not defined in hsphere.properties");
            ssClass = "sitestudio_class_is_not_defined";
        }
        List list = new ArrayList();
        list.add(this.dir);
        list.add(login);
        list.add(group);
        list.add("715");
        list.add(name);
        list.add(idomain);
        list.add(systemUserId);
        list.add(Session.getUser().get("CP_URL").toString());
        list.add(ssPath);
        list.add(ssClass);
        String vhost_plan = getPlanValue("VHOST_PLAN");
        if (vhost_plan != null) {
            list.add(vhost_plan);
        } else {
            list.add("");
        }
        getLog().debug("VirtualHostingResource.initDone, exec " + list);
        he.exec("apache-vhost", list);
        getLog().debug("VirtualHostingResource.initDone, execDone " + list);
    }

    public TemplateModel FM_initContent() throws Exception {
        initContent();
        return this;
    }

    public void removeContent() throws Exception {
        HostEntry he = recursiveGet("host");
        List list = new ArrayList();
        list.add(recursiveGet("login").toString());
        list.add(getPath());
        getLog().debug("VirtualHostingResource.removeContent, exec " + list);
        he.exec("apache-vhost-content-del", list);
        getLog().debug("VirtualHostingResource.removeContent, execDone " + list);
    }

    public TemplateModel FM_removeContent() throws Exception {
        removeContent();
        return this;
    }

    private String getSettingStringValue(String[] values, int index) {
        try {
            return values[index];
        } catch (IndexOutOfBoundsException e) {
            return String.valueOf(index);
        }
    }

    private boolean checkSettingIndValue(int index, String[] values) {
        return index >= 0 && index < values.length;
    }

    private int getSettingIndex(String name, String[] values) throws IndexOutOfBoundsException {
        if (name != null) {
            int index = values.length;
            do {
                index--;
                if (index < 0) {
                    try {
                        int index2 = Integer.getInteger(name).intValue();
                        if (checkSettingIndValue(index2, values)) {
                            return index2;
                        }
                    } catch (Exception e) {
                    }
                }
            } while (!name.equals(values[index]));
            return index;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override // psoft.hsphere.resource.VHostSettings
    public HashMap getSettings() {
        HashMap result = new HashMap();
        result.put(this.INDEX, getSettingStringValue(this.indexValues, this.index));
        result.put(this.SYMLINK, getSettingStringValue(this.symlinkValues, this.symlink));
        result.put(this.SSI, getSettingStringValue(this.ssiValues, this.ssi));
        result.put(this.MILTIVIEW, getSettingStringValue(this.multiviewValues, this.multiview));
        return result;
    }

    @Override // psoft.hsphere.resource.VHostSettings
    public void setSettings(HashMap settings) throws Exception {
        int index = -1;
        int symlink = -1;
        int ssi = -1;
        int multiview = -1;
        for (Object obj : settings.keySet()) {
            String key = obj.toString();
            String value = settings.get(key).toString();
            try {
                if (this.INDEX.equals(key)) {
                    index = getSettingIndex(value, this.indexValues);
                } else if (this.SYMLINK.equals(key)) {
                    symlink = getSettingIndex(value, this.symlinkValues);
                } else if (this.SSI.equals(key)) {
                    ssi = getSettingIndex(value, this.ssiValues);
                } else if (this.MILTIVIEW.equals(key)) {
                    multiview = getSettingIndex(value, this.multiviewValues);
                } else {
                    throw new HSUserException("An unknown option " + key + ".");
                }
            } catch (IndexOutOfBoundsException e) {
                throw new HSUserException("Unknown value [" + value + "] for the " + key + " option.");
            }
        }
        updateSettings(index, symlink, ssi, multiview);
    }

    public synchronized void updateSettings(int index, int symlink, int ssi, int multiview) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        boolean optAdded = false;
        StringBuffer psStr = new StringBuffer("UPDATE apache_vhost SET ");
        if (checkSettingIndValue(index, this.indexValues)) {
            psStr.append("indx = ").append(String.valueOf(index));
            optAdded = true;
        } else {
            index = -1;
        }
        if (checkSettingIndValue(symlink, this.symlinkValues)) {
            if (optAdded) {
                psStr.append(", ");
            } else {
                optAdded = true;
            }
            psStr.append("symlink = ").append(String.valueOf(symlink));
        } else {
            symlink = -1;
        }
        if (checkSettingIndValue(ssi, this.ssiValues)) {
            if (optAdded) {
                psStr.append(", ");
            } else {
                optAdded = true;
            }
            psStr.append("ssi = ").append(String.valueOf(ssi));
        } else {
            ssi = -1;
        }
        if (checkSettingIndValue(multiview, this.multiviewValues)) {
            if (optAdded) {
                psStr.append(", ");
            } else {
                optAdded = true;
            }
            psStr.append("multiview = ").append(String.valueOf(multiview));
        } else {
            multiview = -1;
        }
        if (optAdded) {
            try {
                psStr.append(" WHERE id = ?");
                ps = con.prepareStatement(psStr.toString());
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                if (index >= 0) {
                    this.index = index;
                }
                if (symlink >= 0) {
                    this.symlink = symlink;
                }
                if (ssi >= 0) {
                    this.ssi = ssi;
                }
                if (multiview >= 0) {
                    this.multiview = multiview;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public TemplateModel FM_updateSettings(int index, int symlink, int ssi, int multiview) throws Exception {
        if (Session.getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        updateSettings(index, symlink, ssi, multiview);
        return this;
    }

    public TemplateModel FM_updateConfig() throws Exception {
        if (Session.getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        restart();
        return this;
    }

    public String getConfigEntry() throws Exception {
        SimpleHash root = new SimpleHash();
        root.put("hosting", this);
        root.put("account", Session.getAccount());
        root.put("toolbox", HsphereToolbox.toolbox);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getTemplate("/domain/vhost.config").process(root, out);
        out.close();
        Session.getLog().info("End getConfigEntry");
        return sw.toString();
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        try {
            this.suspended = true;
            restart();
            this.suspended = false;
            super.suspend();
        } catch (Throwable th) {
            this.suspended = false;
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        try {
            this.suspended = false;
            restart();
            this.suspended = true;
            super.resume();
        } catch (Throwable th) {
            this.suspended = true;
            throw th;
        }
    }

    public List getScriptList() throws Exception {
        this.scriptList.clear();
        String dir = getPath();
        List list = new ArrayList();
        list.add(dir);
        HostEntry he = recursiveGet("host");
        Collection<String> listRes = he.exec("getscripts", list);
        for (String tmpStr : listRes) {
            Hashtable script = new Hashtable();
            StringTokenizer st = new StringTokenizer(tmpStr, "|");
            if (st.hasMoreTokens()) {
                script.put("name", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                script.put("caption", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                script.put("mainfile", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                new Localizer();
                script.put("description", Localizer.translateLabel(st.nextToken()));
            }
            if (st.hasMoreTokens()) {
                script.put("enabled", st.nextToken());
            }
            this.scriptList.add(script);
        }
        return this.scriptList;
    }

    public TemplateModel FM_addScript(String name) throws Exception {
        if (Session.getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        List list = new ArrayList();
        list.add(recursiveGet("login").toString());
        list.add(getPath());
        list.add(name);
        HostEntry he = recursiveGet("host");
        he.exec("addscript", list);
        return this;
    }

    public TemplateModel FM_delScript(String name) throws Exception {
        if (Session.getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        List list = new ArrayList();
        list.add(recursiveGet("login").toString());
        list.add(getPath());
        list.add(name);
        HostEntry he = recursiveGet("host");
        he.exec("delscript", list);
        return this;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        String tmp_entry = getConfigEntry();
        Session.getLog().info("Before synch");
        synchronized (he) {
            Session.getLog().info("After synch");
            List l = new ArrayList();
            l.add(getId().getId() + ".conf");
            he.exec("apache-saveconf", l, tmp_entry);
            List l2 = new ArrayList();
            l2.add("force");
            he.exec("apache-reconfig", l2);
            this.entry = tmp_entry;
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        synchronized (he) {
            List l = new ArrayList();
            l.add(getId().getId() + ".conf");
            he.exec("apache-delconf", l);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE apache_vhost SET host_id = ? WHERE id = ?");
            ps.setLong(1, newHostId);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("hosting.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
