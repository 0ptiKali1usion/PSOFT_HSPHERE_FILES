package psoft.hsphere.resource.adm3W;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/Employee.class */
public class Employee extends SharedObject implements TemplateHashModel {
    protected SimpleHash hash;
    protected String level;
    protected String email;

    public boolean isEmpty() {
        return false;
    }

    public String getEmail() {
        return this.email;
    }

    public String toString() {
        try {
            TemplateScalarModel t = get("fname");
            String res = t.getAsString() + " ";
            TemplateScalarModel t2 = get("lname");
            return res + t2.getAsString();
        } catch (Exception e) {
            return super.toString();
        }
    }

    public long getAccountId() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT accounts.id FROM accounts, parent_child, employee_ci WHERE accounts.id = parent_child.account_id AND parent_child.child_id = employee_ci.id AND employee_ci.eid = ?");
        ps.setLong(1, getId());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getLong(1);
        }
        throw new Exception("Account not found");
    }

    public static Employee create(long id, String fname, String lname, String address1, String address2, String city, String state, String postal_code, String country, String phone, String email, String level, String manager) throws Exception {
        Connection con = null;
        try {
            con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO employee (id, fname, lname, address1, address2, city, state, postal_code, country, phone, email, level, manager) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, id);
            ps.setString(2, fname);
            ps.setString(3, lname);
            ps.setString(4, address1);
            ps.setString(5, address2);
            ps.setString(6, city);
            ps.setString(7, state);
            ps.setString(8, postal_code);
            ps.setString(9, country);
            ps.setString(10, phone);
            ps.setString(11, email);
            ps.setInt(12, Integer.parseInt(level));
            ps.setLong(13, Long.parseLong(manager));
            ps.executeUpdate();
            Employee employee = new Employee(id, fname, lname, address1, address2, city, state, postal_code, country, phone, email, level, manager);
            if (con != null) {
                con.close();
            }
            return employee;
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    protected void put(String key, String value) {
        this.hash.put(key, value);
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        return this.hash.get(key);
    }

    public int getLevel() {
        return Integer.parseInt(this.level);
    }

    public Employee(long id, String fname, String lname, String address1, String address2, String city, String state, String postal_code, String country, String phone, String email, String level, String manager) {
        super(id);
        this.hash = new SimpleHash();
        this.email = email;
        put("id", Long.toString(id));
        put("fname", fname);
        put("lname", lname);
        put("address1", address1);
        put("address2", address2);
        put("city", city);
        put("state", state);
        put("postal_code", postal_code);
        put("country", country);
        put("phone", phone);
        put("email", email);
        put("level", level);
        put("manager", manager);
        this.level = level;
    }

    public static Employee get(long id) throws Exception {
        Employee e = (Employee) get(id, Employee.class);
        if (e == null) {
            Connection con = null;
            try {
                con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT fname, lname, address1, address2, city, state, postal_code, country, phone, email, level, manager FROM employee WHERE id = ?");
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    e = new Employee(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12));
                    if (con != null) {
                        con.close();
                    }
                } else {
                    if (con != null) {
                        con.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
        return e;
    }

    public static void remove(long id) throws Exception {
        remove(id, Employee.class);
        Connection con = null;
        try {
            con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("DELETE FROM employee WHERe id = ?");
            ps.setLong(1, id);
            ps.executeUpdate();
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public void setManager(long id) throws Exception {
        Session.getLog().info("Manager -->" + id);
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("UPDATE employee SET manager = ? WHERE id = ?");
        ps.setLong(1, id);
        ps.setLong(2, getId());
        ps.executeUpdate();
        put("manager", Long.toString(id));
    }

    public void change(String fname, String lname, String address1, String address2, String city, String state, String postal_code, String country, String phone, String email) throws Exception {
        Connection con = null;
        try {
            con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE employee SET fname = ?, lname = ?, address1 = ?, address2 = ?, city = ?, state = ?, postal_code = ?, country = ?, phone = ?, email = ? WHERE id = ?");
            this.email = email;
            ps.setString(1, fname);
            ps.setString(2, lname);
            ps.setString(3, address1);
            ps.setString(4, address2);
            ps.setString(5, city);
            ps.setString(6, state);
            ps.setString(7, postal_code);
            ps.setString(8, country);
            ps.setString(9, phone);
            ps.setString(10, email);
            ps.setLong(11, this.f51id);
            ps.executeUpdate();
            put("fname", fname);
            put("lname", lname);
            put("address1", address1);
            put("address2", address2);
            put("city", city);
            put("state", state);
            put("postal_code", postal_code);
            put("country", country);
            put("phone", phone);
            put("email", email);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }
}
