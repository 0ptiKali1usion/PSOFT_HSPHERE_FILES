package psoft.hsphere.resource.adm3W;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/EmployeeManager.class */
public class EmployeeManager extends Resource {
    public EmployeeManager(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public EmployeeManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_createEmployee(String fname, String lname, String address1, String address2, String city, String state, String postal_code, String country, String phone, String email, String level, String manager) throws Exception {
        return Employee.create(Resource.getNewId(), fname, lname, address1, address2, city, state, postal_code, country, phone, email, level, manager);
    }

    public TemplateModel FM_removeEmployee(long id) throws Exception {
        Employee.remove(id);
        return null;
    }

    public TemplateModel FM_changeEmployee(long id, String fname, String lname, String address1, String address2, String city, String state, String postal_code, String country, String phone, String email) throws Exception {
        Employee e = Employee.get(id);
        if (e != null) {
            e.change(fname, lname, address1, address2, city, state, postal_code, country, phone, email);
        }
        return e;
    }

    public TemplateModel FM_setManager(long eid, long manager) throws Exception {
        Employee e = Employee.get(eid);
        e.setManager(manager);
        return e;
    }

    public TemplateModel FM_view(long id) throws Exception {
        return Employee.get(id);
    }

    public synchronized TemplateModel FM_list() throws Exception {
        TemplateList list = new TemplateList();
        Connection con = null;
        try {
            con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, fname, lname, address1, address2, city, state, postal_code, country, phone, email, level, manager FROM employee");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateModel templateModel = Employee.get(rs.getLong(1));
                if (templateModel == null) {
                    templateModel = new Employee(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13));
                }
                list.add(templateModel);
            }
            if (con != null) {
                con.close();
            }
            return list;
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }
}
