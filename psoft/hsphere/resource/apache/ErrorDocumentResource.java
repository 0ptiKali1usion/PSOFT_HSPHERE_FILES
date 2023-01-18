package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ErrorDocumentResource.class */
public class ErrorDocumentResource extends VHResource {
    protected int code;
    protected String msg;
    protected int type;
    public static final int TYPE_URL = 0;
    public static final int TYPE_MESS = 1;
    public static final String MTYPE_MESS = "MESS";
    public static final String MTYPE_URL = "URL";

    public ErrorDocumentResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.code = Integer.parseInt((String) i.next());
        this.msg = (String) i.next();
        if (i.hasNext()) {
            this.type = getType((String) i.next());
        } else {
            this.type = 1;
        }
    }

    public ErrorDocumentResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT code, msg, type FROM apache_edoc WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.code = rs.getInt(1);
                this.msg = Session.getClobValue(rs, 2);
                this.type = rs.getInt(3);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                long parentId = getParent().getId();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM apache_edoc WHERE parent_id = ? AND code = ?");
                ps.setLong(1, parentId);
                ps.setInt(2, this.code);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Error document exists");
                }
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO apache_edoc (id, code, msg, type, parent_id) VALUES (?, ?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setInt(2, this.code);
                Session.setClobValue(ps2, 3, this.msg);
                ps2.setInt(4, this.type);
                ps2.setLong(5, parentId);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting edoc", e);
                throw new HSUserException("errordoc.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_edoc WHERE id = ?");
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

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        StringBuffer buf = new StringBuffer();
        buf.append("ErrorDocument ").append(this.code).append(" ");
        if (this.type == 1) {
            buf.append('\"');
        }
        buf.append(this.msg).append("\n");
        return buf.toString();
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("code") ? new TemplateString(this.code) : key.equals("msg") ? new TemplateString(this.msg) : key.equals("doctype") ? new TemplateString(this.type) : key.equals("mtype") ? new TemplateString(getMType()) : super.get(key);
    }

    public TemplateModel FM_update(String msg, String mType) throws Exception {
        int type = getType(mType);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_edoc SET msg = ?, type=? WHERE id = ?");
            Session.setClobValue(ps, 1, msg);
            ps.setInt(2, type);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.msg = msg;
            this.type = type;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.errordoc.setup", new Object[]{String.valueOf(this.code), _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.errordoc.recurrent", new Object[]{getPeriodInWords(), String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.errordoc.refund", new Object[]{String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.errordoc.refundall", new Object[]{String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("errordoc.desc", new Object[]{recursiveGet("real_name").toString(), new Integer(this.code).toString()});
    }

    protected String getMType() {
        return this.type == 0 ? MTYPE_URL : MTYPE_MESS;
    }

    protected int getType(String mType) {
        return MTYPE_URL.equals(mType) ? 0 : 1;
    }
}
