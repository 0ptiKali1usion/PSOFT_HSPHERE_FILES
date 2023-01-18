package psoft.hsphere.resource.apache;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/DirectoryIndexResource.class */
public class DirectoryIndexResource extends VHResource {
    protected ArrayList indexes;

    public DirectoryIndexResource(int type, Collection values) throws Exception {
        super(type, values);
        this.indexes = new ArrayList();
        Iterator i = values.iterator();
        String st_indexes = i.hasNext() ? (String) i.next() : "";
        StringTokenizer tkz = new StringTokenizer(st_indexes);
        while (tkz.hasMoreTokens()) {
            this.indexes.add(tkz.nextToken());
        }
    }

    public DirectoryIndexResource(ResourceId rid) throws Exception {
        super(rid);
        this.indexes = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM directory_ind WHERE id = ? ORDER BY order_id");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.indexes.add(rs.getString(1));
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
        Iterator i = this.indexes.iterator();
        if (!i.hasNext()) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO directory_ind (id, name, order_id) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            int count = 0;
            while (i.hasNext()) {
                try {
                    ps.setString(2, (String) i.next());
                    ps.setInt(3, count);
                    ps.executeUpdate();
                    count++;
                } catch (SQLException e) {
                    i.remove();
                    Session.getLog().warn("Error adding duplicate value", e);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (!"indexes".equals(key)) {
            return "updateInd".equals(key) ? new IndCreator() : super.get(key);
        }
        StringBuffer buf = new StringBuffer();
        Iterator i = this.indexes.iterator();
        while (i.hasNext()) {
            buf.append((String) i.next()).append(" ");
        }
        return new TemplateString(buf.toString());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM directory_ind WHERE id = ?");
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

    protected TemplateModel updateInd(List values) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM directory_ind WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            this.indexes.clear();
            Iterator i = values.iterator();
            if (i.hasNext()) {
                ps = con.prepareStatement("INSERT INTO directory_ind (id, name, order_id) VALUES (?, ?, ?)");
                ps.setLong(1, getId().getId());
                int count = 0;
                while (i.hasNext()) {
                    try {
                        String value = (String) i.next();
                        ps.setString(2, value);
                        ps.setInt(3, count);
                        ps.executeUpdate();
                        count++;
                        this.indexes.add(value);
                    } catch (SQLException e) {
                        Session.getLog().warn("Error adding duplicate value", e);
                    }
                }
                Session.closeStatement(ps);
                con.close();
                return this;
            }
            Session.closeStatement(ps2);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/apache/DirectoryIndexResource$IndCreator.class */
    public class IndCreator implements TemplateMethodModel {
        protected List list = new ArrayList();

        IndCreator() {
            DirectoryIndexResource.this = r5;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                List l2 = HTMLEncoder.decode(l);
                if (l2.isEmpty()) {
                    return DirectoryIndexResource.this.updateInd(this.list);
                }
                this.list.add(l2.get(0));
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating ", e);
                return new TemplateErrorResult(e);
            }
        }
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        StringBuffer buf = new StringBuffer();
        Iterator i = this.indexes.iterator();
        if (i.hasNext()) {
            buf.append("DirectoryIndex ");
            while (i.hasNext()) {
                buf.append(" " + ((String) i.next()));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.directory_ind.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.directory_ind.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.directory_ind.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.directory_ind.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("directory_ind.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
