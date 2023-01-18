package psoft.hsphere.resource.admin.p002ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.exception.DServerLockException;
import psoft.hsphere.exception.DServerNotFoundException;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.admin.ds.DServer */
/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ds/DServer.class */
public class DServer extends SharedObject implements TemplateHashModel {
    public static final int STATE_AVAILABLE = 1;
    public static final int STATE_IN_USE = 2;
    public static final int STATE_CLEANUP = 3;

    /* renamed from: df */
    protected static final SimpleDateFormat f176df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    String name;
    DSTemplate template;

    /* renamed from: ip */
    String f177ip;
    URL mrtgURL;
    URL rebootURL;
    String internalId;
    String superUser;
    String suPasswd;
    double setup;
    double recurrent;
    int state;
    long rid;

    /* renamed from: os */
    String f178os;
    String cpu;
    String ram;
    String storage;
    Timestamp created;
    Timestamp taken;

    public DServer(long id, String name, DSTemplate template, String ip, URL mrtgURL, URL rebootURL, String internalId, String superUser, String suPasswd, int state, long ownedBy, Timestamp created, Timestamp taken) {
        super(id);
        this.template = null;
        this.f178os = null;
        this.cpu = null;
        this.ram = null;
        this.storage = null;
        this.created = null;
        this.taken = null;
        this.name = name;
        this.template = template;
        this.f177ip = ip;
        this.mrtgURL = mrtgURL;
        this.rebootURL = rebootURL;
        this.internalId = internalId;
        this.superUser = superUser;
        this.suPasswd = suPasswd;
        this.state = state;
        this.rid = ownedBy;
        this.created = created;
        this.taken = taken;
    }

    public DServer(long id, String name, String ip, URL mrtgURL, URL rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, int state, long ownedBy, String os, String cpu, String ram, String storage, Timestamp created, Timestamp taken) {
        super(id);
        this.template = null;
        this.f178os = null;
        this.cpu = null;
        this.ram = null;
        this.storage = null;
        this.created = null;
        this.taken = null;
        this.name = name;
        this.f177ip = ip;
        this.mrtgURL = mrtgURL;
        this.rebootURL = rebootURL;
        this.internalId = internalId;
        this.superUser = superUser;
        this.suPasswd = suPasswd;
        this.setup = setup;
        this.recurrent = recurrent;
        this.state = state;
        this.rid = ownedBy;
        this.f178os = os;
        this.cpu = cpu;
        this.ram = ram;
        this.storage = storage;
        this.created = created;
        this.taken = taken;
    }

    public static DServer create(String name, long templateId, String ip, String mrtgURL, String rebootURL, String internalId, String superUser, String suPasswd) throws Exception {
        DSTemplate dst = DSTemplate.get(templateId);
        URL mrtg = new URL(mrtgURL);
        URL reboot = new URL(rebootURL);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Timestamp created = new Timestamp(TimeUtils.currentTimeMillis());
            long newId = Session.getNewIdAsLong("ds_seq");
            ps = con.prepareStatement("INSERT INTO dedicated_servers (id, name,template_id, ip, mrtg_url, reboot_url, internal_id, su, su_passwd, state, created) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setLong(3, dst.getId());
            ps.setString(4, ip);
            ps.setString(5, mrtg.toString());
            ps.setString(6, reboot.toString());
            ps.setString(7, internalId);
            ps.setString(8, superUser);
            ps.setString(9, suPasswd);
            ps.setLong(10, 1L);
            ps.setTimestamp(11, created);
            ps.executeUpdate();
            DServer dServer = new DServer(newId, name, dst, ip, mrtg, reboot, internalId, superUser, suPasswd, 1, 0L, created, null);
            Session.closeStatement(ps);
            con.close();
            return dServer;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static DServer create(String name, String ip, String mrtgURL, String rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage) throws Exception {
        URL mrtg = new URL(mrtgURL);
        URL reboot = new URL(rebootURL);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Timestamp created = new Timestamp(TimeUtils.currentTimeMillis());
            long newId = Session.getNewIdAsLong("ds_seq");
            ps = con.prepareStatement("INSERT INTO dedicated_servers (id,name,ip,mrtg_url, reboot_url,internal_id,su,su_passwd,setup, recurrent,state,os,cpu,ram,storage,created) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setString(3, ip);
            ps.setString(4, mrtg.toString());
            ps.setString(5, reboot.toString());
            ps.setString(6, internalId);
            ps.setString(7, superUser);
            ps.setString(8, suPasswd);
            ps.setDouble(9, setup);
            ps.setDouble(10, recurrent);
            ps.setDouble(11, 1.0d);
            ps.setString(12, os);
            ps.setString(13, cpu);
            ps.setString(14, ram);
            ps.setString(15, storage);
            ps.setTimestamp(16, created);
            ps.executeUpdate();
            DServer dServer = new DServer(newId, name, ip, mrtg, reboot, internalId, superUser, suPasswd, setup, recurrent, 1, 0L, os, cpu, ram, storage, created, null);
            Session.closeStatement(ps);
            con.close();
            return dServer;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static DServer get(long id) throws Exception {
        DServer ds = (DServer) get(id, DServer.class);
        if (ds != null) {
            return ds;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT name, template_id, ip, mrtg_url, reboot_url, internal_id, su, su_passwd, setup, recurrent, state, os, cpu, ram, storage,rid, created, taken FROM dedicated_servers WHERE id = ?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            if (rs.getLong("template_id") > 0) {
                return new DServer(id, rs.getString("name"), DSTemplate.get(rs.getLong("template_id")), rs.getString("ip"), new URL(rs.getString("mrtg_url")), new URL(rs.getString("reboot_url")), rs.getString("internal_id"), rs.getString("su"), rs.getString("su_passwd"), rs.getInt("state"), rs.getLong("rid"), rs.getTimestamp("created"), rs.getTimestamp("taken"));
            }
            return new DServer(id, rs.getString("name"), rs.getString("ip"), new URL(rs.getString("mrtg_url")), new URL(rs.getString("reboot_url")), rs.getString("internal_id"), rs.getString("su"), rs.getString("su_passwd"), rs.getDouble("setup"), rs.getDouble("recurrent"), rs.getInt("state"), rs.getLong("rid"), rs.getString("os"), rs.getString("cpu"), rs.getString("ram"), rs.getString("storage"), rs.getTimestamp("created"), rs.getTimestamp("taken"));
        }
        throw new DServerNotFoundException("Dedicated server with id=" + id + " not found");
    }

    public boolean isTemplatedServer() {
        return this.template != null;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(getName());
        }
        if ("ip".equals(key)) {
            return new TemplateString(getIp());
        }
        if ("mrtgURL".equals(key)) {
            return new TemplateString(getMrtgURL());
        }
        if ("rebootURL".equals(key)) {
            return new TemplateString(getRebootURL());
        }
        if ("internalId".equals(key)) {
            return new TemplateString(getInternalId());
        }
        if ("su".equals(key)) {
            return new TemplateString(getSuperUser());
        }
        if ("su_passwd".equals(key)) {
            return new TemplateString(getSuPasswd());
        }
        if ("setup".equals(key)) {
            return new TemplateString(getSetup());
        }
        if ("recurrent".equals(key)) {
            return new TemplateString(getRecurrent());
        }
        if ("state".equals(key)) {
            return new TemplateString(getStateDescription());
        }
        if ("rid".equals(key)) {
            return new TemplateString(getRid());
        }
        if ("os".equals(key)) {
            return new TemplateString(getOs());
        }
        if ("cpu".equals(key)) {
            return new TemplateString(getCpu());
        }
        if ("ram".equals(key)) {
            return new TemplateString(getRam());
        }
        if ("storage".equals(key)) {
            return new TemplateString(getStorage());
        }
        if ("can_be_deleted".equals(key)) {
            return new TemplateString(canBeDeleted());
        }
        if ("template".equals(key)) {
            return getTemplate();
        }
        if ("state_id".equals(key)) {
            return new TemplateString(getState());
        }
        if ("created".equals(key)) {
            return new TemplateString(HsphereToolbox.getShortDateTimeStr(this.created));
        }
        if ("taken".equals(key)) {
            return new TemplateString(HsphereToolbox.getShortDateTimeStr(this.taken));
        }
        return super.get(key);
    }

    public TemplateModel FM_save(String name, String ip, String mrtgURL, String rebootURL, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, int state) throws Exception {
        save(name, ip, mrtgURL, rebootURL, superUser, suPasswd, setup, recurrent, os, cpu, ram, storage, state);
        return this;
    }

    public void save(String name, String ip, String mrtgURL, String rebootURL, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, int state) throws Exception {
        PreparedStatement ps = null;
        StringBuffer sb = new StringBuffer("UPDATE dedicated_servers SET ip = ?, mrtg_url = ?, reboot_url = ?, su = ?, su_passwd = ?, state = ? ");
        Connection con = Session.getDb();
        URL mURL = new URL(mrtgURL);
        URL rURL = new URL(rebootURL);
        try {
            if (getState() == 1) {
                sb.append(", name= ?");
            }
            if (getTemplate() == null) {
                sb.append(", setup = ?");
                sb.append(", recurrent = ?");
            }
            if (getState() == 1 && getTemplate() == null) {
                sb.append(", os = ?");
                sb.append(", cpu = ?");
                sb.append(", ram = ?");
                sb.append(", storage = ?");
            }
            sb.append(" WHERE id = ?");
            ps = con.prepareStatement(sb.toString());
            int i = 1 + 1;
            ps.setString(1, ip);
            int i2 = i + 1;
            ps.setString(i, mURL.toString());
            int i3 = i2 + 1;
            ps.setString(i2, rURL.toString());
            int i4 = i3 + 1;
            ps.setString(i3, superUser);
            int i5 = i4 + 1;
            ps.setString(i4, suPasswd);
            int i6 = i5 + 1;
            ps.setInt(i5, state);
            if (getState() == 1) {
                i6++;
                ps.setString(i6, name);
            }
            if (getTemplate() == null) {
                int i7 = i6;
                int i8 = i6 + 1;
                ps.setDouble(i7, setup);
                i6 = i8 + 1;
                ps.setDouble(i8, recurrent);
            }
            if (getState() == 1 && getTemplate() == null) {
                int i9 = i6;
                int i10 = i6 + 1;
                ps.setString(i9, os);
                int i11 = i10 + 1;
                ps.setString(i10, cpu);
                int i12 = i11 + 1;
                ps.setString(i11, ram);
                i6 = i12 + 1;
                ps.setString(i12, storage);
            }
            int i13 = i6;
            int i14 = i6 + 1;
            ps.setLong(i13, getId());
            ps.executeUpdate();
            this.f177ip = ip;
            this.mrtgURL = mURL;
            this.rebootURL = rURL;
            this.superUser = superUser;
            this.suPasswd = suPasswd;
            if (getState() == 1) {
                this.name = name;
            }
            if (getTemplate() == null) {
                this.setup = setup;
                this.recurrent = recurrent;
            }
            if (getState() == 1 && getTemplate() == null) {
                this.f178os = os;
                this.cpu = cpu;
                this.ram = ram;
                this.storage = storage;
            }
            this.state = state;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getName() {
        return this.name;
    }

    public String getIp() {
        return this.f177ip;
    }

    public URL getMrtgURL() {
        return this.mrtgURL;
    }

    public URL getRebootURL() {
        return this.rebootURL;
    }

    public String getInternalId() {
        return this.internalId;
    }

    public String getSuperUser() {
        return this.superUser;
    }

    public String getSuPasswd() {
        return this.suPasswd;
    }

    public double getSetup() {
        if (!isTemplatedServer()) {
            return this.setup;
        }
        return Double.NaN;
    }

    public double getRecurrent() {
        if (!isTemplatedServer()) {
            return this.recurrent;
        }
        return Double.NaN;
    }

    public int getState() {
        return this.state;
    }

    public long getRid() {
        return this.rid;
    }

    public String getOs() {
        if (!isTemplatedServer()) {
            return this.f178os;
        }
        return this.template.getOS();
    }

    public String getCpu() {
        if (!isTemplatedServer()) {
            return this.cpu;
        }
        return this.template.getCPU();
    }

    public String getRam() {
        if (!isTemplatedServer()) {
            return this.ram;
        }
        return this.template.getRAM();
    }

    public String getStorage() {
        if (!isTemplatedServer()) {
            return this.storage;
        }
        return this.template.getStorage();
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DServer) {
            DServer dServer = (DServer) o;
            return this.f51id == dServer.f51id;
        }
        return false;
    }

    public String getStateDescription() {
        if (getState() == 1) {
            return Localizer.translateLabel("admin.ds.status.AVAILABLE");
        }
        if (getState() == 2) {
            return Localizer.translateLabel("admin.ds.status.IN_USE");
        }
        if (getState() == 3) {
            return Localizer.translateLabel("admin.ds.status.CLEAN_UP");
        }
        return null;
    }

    public boolean canBeDeleted() {
        return getState() == 1;
    }

    public DSTemplate getTemplate() {
        return this.template;
    }

    public synchronized void lock(ResourceId rid) throws Exception {
        PreparedStatement ps = null;
        if (getState() != 1) {
            throw new DServerLockException(Localizer.translateMessage("ds.ds_lock_error", new String[]{Long.toString(getId())}));
        }
        Timestamp now = new Timestamp(TimeUtils.currentTimeMillis());
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET rid = ?, state = ?, taken = ? WHERE id = ?");
            ps.setLong(1, rid.getId());
            ps.setInt(2, 2);
            ps.setTimestamp(3, now);
            ps.setLong(4, getId());
            ps.executeUpdate();
            this.rid = rid.getId();
            this.state = 2;
            this.taken = now;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static DServer getByRid(ResourceId rid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM dedicated_servers WHERE rid = ?");
            ps.setLong(1, rid.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DServer dServer = get(rs.getLong("id"));
                Session.closeStatement(ps);
                con.close();
                return dServer;
            }
            throw new DServerNotFoundException(Localizer.translateMessage("ds.ds_not_found", new String[]{rid.toString()}));
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
