package psoft.hsphere.resource.admin.p002ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.exception.DSTemplateNotFoundException;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.admin.ds.DSTemplate */
/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ds/DSTemplate.class */
public class DSTemplate extends SharedObject implements TemplateHashModel {
    String name;

    /* renamed from: os */
    String f175os;
    String cpu;
    String ram;
    String storage;
    long someId;

    public DSTemplate(long id, String name, String os, String cpu, String ram, String storage) {
        super(id);
        this.name = name;
        this.f175os = os;
        this.cpu = cpu;
        this.ram = ram;
        this.storage = storage;
    }

    public String getName() {
        return this.name;
    }

    public String getOS() {
        return this.f175os;
    }

    public String getCPU() {
        return this.cpu;
    }

    public String getRAM() {
        return this.ram;
    }

    public String getStorage() {
        return this.storage;
    }

    public static DSTemplate createTempalte(String name, String os, String cpu, String ram, String storage) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ds_templates(id, name, os, cpu, ram, storage) VALUES (?,?,?,?,?,?)");
            long newId = Session.getNewIdAsLong("dst_seq");
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setString(3, os);
            ps.setString(4, cpu);
            ps.setString(5, ram);
            ps.setString(6, storage);
            ps.executeUpdate();
            DSTemplate dSTemplate = new DSTemplate(newId, name, os, cpu, ram, storage);
            Session.closeStatement(ps);
            con.close();
            return dSTemplate;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static DSTemplate get(long id) throws Exception {
        DSTemplate t = (DSTemplate) get(id, DSTemplate.class);
        if (t != null) {
            return t;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name, os, cpu, ram, storage FROM ds_templates WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DSTemplate dSTemplate = new DSTemplate(id, rs.getString("name"), rs.getString("os"), rs.getString("cpu"), rs.getString("ram"), rs.getString("storage"));
                Session.closeStatement(ps);
                con.close();
                return dSTemplate;
            }
            throw new DSTemplateNotFoundException("Dedicated server template with id=" + id + " not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(getName());
        }
        if ("os".equals(key)) {
            return new TemplateString(getOS());
        }
        if ("cpu".equals(key)) {
            return new TemplateString(getCPU());
        }
        if ("ram".equals(key)) {
            return new TemplateString(getRAM());
        }
        if ("storage".equals(key)) {
            return new TemplateString(getStorage());
        }
        return super.get(key);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DSTemplate) {
            DSTemplate dsTemplate = (DSTemplate) o;
            return this.f51id == dsTemplate.f51id;
        }
        return false;
    }
}
