package psoft.hsphere.resource.zeus;

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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.VHostSettings;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/VirtualHostingResource.class */
public class VirtualHostingResource extends Resource implements IPDependentResource, VHostSettings {
    protected int index;
    protected int ssi;
    protected int multiview;
    protected String dir;
    protected String entry;
    protected String[] indexValues;
    protected String[] ssiValues;
    protected String[] multiviewValues;
    protected String INDEX;
    protected String SSI;
    protected String MILTIVIEW;

    public VirtualHostingResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.indexValues = new String[]{"disabled", "enabled", "fancy"};
        this.ssiValues = new String[]{"disabled", "enabled"};
        this.multiviewValues = new String[]{"disabled", "enabled"};
        this.INDEX = "index";
        this.SSI = "ssi";
        this.MILTIVIEW = "multiview";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.dir = (String) i.next();
            if (i.hasNext()) {
                this.index = Integer.parseInt((String) i.next());
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
        this.indexValues = new String[]{"disabled", "enabled", "fancy"};
        this.ssiValues = new String[]{"disabled", "enabled"};
        this.multiviewValues = new String[]{"disabled", "enabled"};
        this.INDEX = "index";
        this.SSI = "ssi";
        this.MILTIVIEW = "multiview";
        Session.getLog().debug("ZEUS loaded" + this);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT entry, dir, indx, ssi, multiview FROM zeus_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.entry = Session.getClobValue(rs, 1);
                this.dir = rs.getString(2);
                this.index = rs.getInt(3);
                this.ssi = rs.getInt(4);
                this.multiview = rs.getInt(5);
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
        Session.getLog().debug("ZEUS was restarted by IP");
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
            ps = con.prepareStatement("DELETE FROM zeus_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            HostEntry he = recursiveGet("host");
            List l = new ArrayList();
            l.add(Long.toString(getId().getId()));
            he.exec("zeus-delconf", l);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getServerAdmin() throws Exception {
        return new TemplateString("webmaster@" + recursiveGet("real_name"));
    }

    public String getPath() throws Exception {
        return recursiveGet("dir") + "/" + this.dir;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("server")) {
            return new TemplateString("zeus");
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
            } else if (key.equals("handlers")) {
                return new TemplateList(getHandlerList());
            } else {
                if (key.equals("index")) {
                    return new TemplateString(this.index);
                }
                if (key.equals("ssi")) {
                    return new TemplateString(this.ssi);
                }
                if (key.equals("vserver")) {
                    return new TemplateString(getId().getId());
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
            this.ssi = Integer.parseInt(getPlanValue("SSI"));
        } catch (NullPointerException e2) {
        }
        try {
            this.multiview = Integer.parseInt(getPlanValue("MULTIVIEW"));
        } catch (NullPointerException e3) {
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        String idomain;
        super.initDone();
        Resource domain = getParent().get();
        String name = domain.get("name").toString();
        if (this.dir == null) {
            this.dir = name;
        }
        HostEntry he = recursiveGet("host");
        String login = recursiveGet("login").toString();
        String group = recursiveGet("group").toString();
        String systemUserId = Long.toString(getParent().getId());
        ResourceId ip = domain.FM_getChild("ip");
        if (ip.get("shared").toString().equals("1")) {
            idomain = FM_getChild("idomain_alias").get("alias").toString();
        } else {
            idomain = ip.get("ip").toString();
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
        list.add(Session.getProperty("PATH_SITE_STUDIO").toString());
        list.add(Session.getProperty("SITE_STUDIO_CLASS").toString());
        getLog().debug("VirtualHostingResource.initDone, exec " + list);
        he.exec("apache-vhost", list);
        getLog().debug("VirtualHostingResource.initDone, execDone " + list);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO zeus_vhost (id, host_id, entry, dir, indx, ssi, multiview) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, he.getId());
            Session.setClobValue(ps, 3, getConfigEntry());
            ps.setString(4, this.dir);
            ps.setInt(5, this.index);
            ps.setInt(6, this.ssi);
            ps.setInt(7, this.multiview);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            restart();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void restart() throws Exception {
        HostEntry he = recursiveGet("host");
        String tmp_entry = getConfigEntry();
        if (this.entry != null && this.entry.equals(tmp_entry)) {
            Session.getLog().info("Skip ZEUS restart, equals configs");
            return;
        }
        Session.getLog().info("Before synch");
        List l = new ArrayList();
        l.add(Long.toString(getId().getId()));
        he.exec("zeus-saveconf", l, tmp_entry);
        ResourceId ssl = FM_getChild("ssl");
        if (ssl == null) {
            ssl = FM_getChild("sharedssl");
        }
        if (ssl != null) {
            restartSSL(ssl);
        }
        List l2 = new ArrayList();
        l2.add(Long.toString(getId().getId()));
        he.exec("zeus-reconfig", l2);
        this.entry = tmp_entry;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE zeus_vhost SET entry = ? WHERE id = ?");
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

    public void restartSSL(ResourceId ssl) throws Exception {
        HostEntry he = recursiveGet("host");
        String ssl_entry = getConfigEntry(ssl);
        List l = new ArrayList();
        l.add(getId().getId() + "s");
        he.exec("zeus-saveconf", l, ssl_entry);
        List l2 = new ArrayList();
        l2.add(getId().getId() + "s");
        he.exec("zeus-reconfig", l2);
    }

    public void deleteSSL() throws Exception {
        HostEntry he = recursiveGet("host");
        List l = new ArrayList();
        l.add(getId().getId() + "s");
        he.exec("zeus-delconf", l);
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
        result.put(this.SSI, getSettingStringValue(this.ssiValues, this.ssi));
        result.put(this.MILTIVIEW, getSettingStringValue(this.multiviewValues, this.multiview));
        return result;
    }

    @Override // psoft.hsphere.resource.VHostSettings
    public void setSettings(HashMap settings) throws Exception {
        int index = -1;
        int ssi = -1;
        int multiview = -1;
        for (Object obj : settings.keySet()) {
            String key = obj.toString();
            String value = settings.get(key).toString();
            try {
                if (this.INDEX.equals(key)) {
                    index = getSettingIndex(value, this.indexValues);
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
        updateSettings(index, ssi, multiview);
    }

    public synchronized void updateSettings(int index, int ssi, int multiview) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        boolean optAdded = false;
        StringBuffer psStr = new StringBuffer("UPDATE zeus_vhost SET ");
        if (checkSettingIndValue(index, this.indexValues)) {
            psStr.append("indx = ").append(String.valueOf(index));
            optAdded = true;
        } else {
            index = -1;
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

    public TemplateModel FM_updateSettings(int index, int ssi, int multiview) throws Exception {
        updateSettings(index, ssi, multiview);
        return this;
    }

    public TemplateModel FM_updateConfig() throws Exception {
        restart();
        return this;
    }

    public String getConfigEntry() throws Exception {
        return getConfigEntry(null);
    }

    public String getConfigEntry(ResourceId ssl) throws Exception {
        SimpleHash root = new SimpleHash();
        root.put("hosting", this);
        root.put("account", Session.getAccount());
        root.put("toolbox", HsphereToolbox.toolbox);
        if (ssl != null) {
            root.put("ssl", ssl);
        }
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getTemplate("/zeus/vhost.config").process(root, out);
        out.close();
        Session.getLog().info("End getConfigEntry");
        return sw.toString();
    }

    public TemplateModel FM_removeContent() throws Exception {
        removeContent();
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

    public List getHandlerList() throws Exception {
        List result = new ArrayList();
        Collection<String> listRes = new ArrayList();
        listRes.add("/usr/bin/perl|Perl");
        for (String tmpStr : listRes) {
            HashMap script = new HashMap();
            StringTokenizer st = new StringTokenizer(tmpStr, "|");
            if (st.hasMoreTokens()) {
                script.put("name", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                script.put("caption", st.nextToken());
            }
            result.add(script);
        }
        return result;
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

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("hosting.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
