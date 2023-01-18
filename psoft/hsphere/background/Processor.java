package psoft.hsphere.background;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Map;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/background/Processor.class */
public abstract class Processor extends Thread implements TemplateModel {
    protected Boolean running;
    protected boolean stop;

    /* renamed from: cp */
    protected C0004CP f74cp;

    /* renamed from: id */
    protected long f75id;
    protected Date created;
    protected String description;
    protected Map values;
    protected long resellerId;

    protected abstract void processing() throws Exception;

    @Override // java.lang.Thread
    public long getId() {
        return this.f75id;
    }

    public Processor(C0004CP cp, Long id, String name, Date created) throws Exception {
        this(cp, id, name, created, new Long(1L));
    }

    public Processor(C0004CP cp, Long id, String name, Date created, Long resellerId) throws Exception {
        this(cp, id, name, created, null, resellerId);
    }

    public Processor(C0004CP cp, Long id, String name, Date created, Map values, Long resellerId) throws Exception {
        super(name);
        this.running = Boolean.FALSE;
        this.stop = false;
        this.f74cp = cp;
        this.f75id = id.longValue();
        this.description = name;
        this.created = created;
        this.values = values;
        this.resellerId = resellerId.longValue();
        if (values != null) {
            saveValues();
        }
    }

    protected void saveValues() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        TimeUtils.getDate();
        try {
            ps1 = con.prepareStatement("DELETE FROM background_manager_valuesWHERE id = ?");
            ps1.setLong(1, getId());
            ps1.executeUpdate();
            ps = con.prepareStatement("INSERT INTO background_manager_values(id, name, value) VALUES (?, ?, ?)");
            ps.setLong(1, getId());
            for (String key : this.values.keySet()) {
                ps.setString(2, key);
                ps.setString(3, (String) this.values.get(key));
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    public void die() {
        if (!this.running.booleanValue()) {
            interrupt();
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        this.f74cp.setConfig();
        this.f74cp.initLog();
        try {
            Session.setResellerId(this.resellerId);
            synchronized (this.running) {
                this.running = Boolean.TRUE;
                processing();
            }
        } catch (Exception ex) {
            Session.getLog().error("Error during processing:", ex);
        }
    }

    public void delete() throws Exception {
        die();
        this.values.clear();
        saveValues();
    }
}
