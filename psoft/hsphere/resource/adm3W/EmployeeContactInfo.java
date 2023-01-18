package psoft.hsphere.resource.adm3W;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/EmployeeContactInfo.class */
public class EmployeeContactInfo extends Resource {
    protected long eid;

    public long getEmployeeId() {
        return this.eid;
    }

    public EmployeeContactInfo(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        String tmp = null;
        try {
            tmp = (String) i.next();
            this.eid = Long.parseLong(tmp);
        } catch (NumberFormatException e) {
            Employee e2 = Employee.create(Resource.getNewId(), tmp, (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next(), (String) i.next());
            this.eid = e2.getId();
        }
        getLog().info("Eid-->" + this.eid);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("INSERT INTO employee_ci (id, eid) VALUES (?, ?)");
        ps.setLong(1, getId().getId());
        ps.setLong(2, this.eid);
        ps.executeUpdate();
        con.close();
    }

    public EmployeeContactInfo(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT eid FROM employee_ci WHERE id = ?");
        ps.setLong(1, rid.getId());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            this.eid = rs.getLong(1);
        } else {
            notFound();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Employee.remove(this.eid);
    }

    public TemplateModel FM_view(long id) throws Exception {
        return Employee.get(id);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("ci")) {
                getLog().info("geting employee" + this.eid + "|" + Employee.get(this.eid));
                return Employee.get(this.eid);
            }
            return super.get(key);
        } catch (Exception e) {
            getLog().error("Getting employee", e);
            return null;
        }
    }
}
