package psoft.hsphere.resource.ODBC;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.resource.HostDependentResource;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ODBC/DSNRecord.class */
public class DSNRecord extends Resource implements HostDependentResource {
    protected Hashtable params;

    public DSNRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.params = new Hashtable();
        String pref = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String val = (String) i.next();
            Session.getLog().debug("DSNRecord: key:" + key + " value:" + val);
            if (key.equals("DSN") && pref != null && !pref.trim().equals("") && !val.startsWith(pref)) {
                val = pref + val;
            }
            this.params.put(key, val);
        }
    }

    public DSNRecord(ResourceId rId) throws Exception {
        super(rId);
        this.params = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, value FROM odbc_params WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.params.put(rs.getString(1), rs.getString(2) == null ? "" : rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void update(Hashtable value) throws Exception {
        List args = new ArrayList();
        args.add(new String[]{"driver-name", (String) this.params.get("driver-name")});
        args.add(new String[]{"DSN", (String) this.params.get("DSN")});
        Enumeration i = value.keys();
        while (i.hasMoreElements()) {
            String key = (String) i.nextElement();
            if (!"MSSQLUID".equals(key) && !"MSSQLPWD".equals(key)) {
                args.add(new String[]{key, (String) this.params.get(key)});
            }
        }
        ((ODBCService) getParent().get()).modifyDSN(args);
        if (getId().FM_getChild("cf_dsn_record") != null) {
            CFDSNRecord cfdsn = (CFDSNRecord) getId().FM_getChild("cf_dsn_record").get();
            cfdsn.update(value);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE odbc_params SET value = ? WHERE name = ? AND id = ?");
            Enumeration i2 = value.keys();
            while (i2.hasMoreElements()) {
                String key2 = (String) i2.nextElement();
                ps.setString(1, (String) value.get(key2));
                ps.setString(2, key2);
                ps.setLong(3, getId().getId());
                ps.executeUpdate();
                this.params.put(key2, (String) value.get(key2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO odbc_params(id, name, value) VALUES(?, ?, ?)");
            Enumeration i = this.params.keys();
            while (i.hasMoreElements()) {
                String key = (String) i.nextElement();
                ps.setLong(1, getId().getId());
                ps.setString(2, key);
                ps.setString(3, (String) this.params.get(key));
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
            physicalCreate(getHostId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM odbc_params WHERE id = ?");
            ps.setLong(1, getId().getId());
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
    public TemplateModel get(String key) throws TemplateModelException {
        if (this.params.get(key) != null) {
            return new TemplateString((String) this.params.get(key));
        }
        return "dsn_updater".equals(key) ? new DSNUpdater() : super.get(key);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ODBC/DSNRecord$DSNUpdater.class */
    public class DSNUpdater implements TemplateMethodModel {

        /* renamed from: hs */
        protected Hashtable f155hs = new Hashtable();

        DSNUpdater() {
            DSNRecord.this = r5;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                List l2 = HTMLEncoder.decode(l);
                if (l2.isEmpty()) {
                    DSNRecord.this.update(this.f155hs);
                    return DSNRecord.this.getId();
                }
                String key = (String) l2.get(0);
                if (!"DSN".equals(key) && !"driver-name".equals(key)) {
                    this.f155hs.put(key, l2.get(1) == null ? "" : l2.get(1));
                    return null;
                }
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error updating DSN", e);
                return new TemplateErrorResult(e);
            }
        }
    }

    protected String _getName() {
        try {
            return ((String) this.params.get("DSN")) + " (" + ((String) this.params.get("driver-name")) + ")";
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.dsn_record.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.dsn_record.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.dsn_record.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.dsn_record.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        List args = new ArrayList();
        Enumeration i = this.params.keys();
        while (i.hasMoreElements()) {
            String key = (String) i.nextElement();
            if (!"MSSQLUID".equals(key) && !"MSSQLPWD".equals(key)) {
                args.add(new String[]{key, (String) this.params.get(key)});
            }
        }
        ((ODBCService) getParent().get()).createDSN(args, targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ((ODBCService) getParent().get()).removeDSN((String) this.params.get("driver-name"), (String) this.params.get("DSN"), targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    public Hashtable getAllParams() {
        return this.params;
    }
}
