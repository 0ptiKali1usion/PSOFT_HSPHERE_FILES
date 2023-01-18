package psoft.hsphere.resource.epayment;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;
import psoft.validators.ServletRequestAccessor;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/ContactInfoObject.class */
public class ContactInfoObject implements TemplateHashModel {

    /* renamed from: id */
    protected long f193id;
    protected String name;
    protected String lastName;
    protected String company;
    protected String address1;
    protected String address2;
    protected String city;
    protected String state;
    protected String state2;
    protected String postalCode;
    protected String country;
    protected String phone;
    protected String email;

    public String getEmail() {
        return this.email;
    }

    public long getId() {
        return this.f193id;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f193id);
        }
        if (!key.equals("name") && !key.equals("first_name")) {
            if (key.equals("last_name")) {
                return new TemplateString(this.lastName);
            }
            if (!key.equals("company") && !key.equals("org_name")) {
                if (key.equals("address1")) {
                    return new TemplateString(this.address1);
                }
                if (key.equals("address2")) {
                    return new TemplateString(this.address2);
                }
                if (key.equals("city")) {
                    return new TemplateString(this.city);
                }
                if (key.equals("state")) {
                    return new TemplateString(this.state);
                }
                if (key.equals("state2")) {
                    return new TemplateString(this.state2);
                }
                if (key.equals("postal_code")) {
                    return new TemplateString(this.postalCode);
                }
                if (key.equals("country")) {
                    return this.country == null ? new TemplateString("US") : new TemplateString(this.country);
                } else if (key.equals("phone")) {
                    return new TemplateString(this.phone);
                } else {
                    if (key.equals("email")) {
                        return new TemplateString(this.email);
                    }
                    return null;
                }
            }
            return new TemplateString(this.company);
        }
        return new TemplateString(this.name);
    }

    public ContactInfoObject(String name, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        this(name, "", "", address1, address2, city, state, "", postalCode, country, phone, email);
    }

    public ContactInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        this(name, lastName, company, address1, address2, city, state, "", postalCode, country, phone, email);
    }

    public ContactInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) throws Exception {
        this.f193id = Resource.getNewId();
        this.name = name;
        this.lastName = lastName;
        this.company = company;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.state2 = state2;
        this.postalCode = postalCode;
        this.country = country;
        this.phone = phone;
        this.email = email;
        save();
    }

    public ContactInfoObject(Collection values) throws Exception {
        Iterator i = values.iterator();
        this.f193id = Resource.getNewId();
        this.name = (String) i.next();
        this.lastName = (String) i.next();
        this.company = (String) i.next();
        this.address1 = (String) i.next();
        this.address2 = (String) i.next();
        this.city = (String) i.next();
        this.state = (String) i.next();
        this.state2 = (String) i.next();
        this.postalCode = (String) i.next();
        this.country = (String) i.next();
        this.phone = (String) i.next();
        this.email = (String) i.next();
        save();
    }

    public ContactInfoObject(NameModifier nm) throws Exception {
        this.f193id = Resource.getNewId();
        Accessor a = new ServletRequestAccessor(Session.getRequest());
        this.name = a.get(nm.getName("name"));
        if (this.name == null || "".equals(this.name)) {
            this.name = a.get(nm.getName("first_name"));
        }
        this.lastName = a.get(nm.getName("last_name"));
        this.company = a.get(nm.getName("company"));
        this.address1 = a.get(nm.getName("address1"));
        this.address2 = a.get(nm.getName("address2"));
        this.city = a.get(nm.getName("city"));
        this.state = a.get(nm.getName("state"));
        this.state2 = a.get(nm.getName("state2"));
        this.postalCode = a.get(nm.getName("postal_code"));
        this.country = a.get(nm.getName("country"));
        this.phone = a.get(nm.getName("phone"));
        this.email = a.get(nm.getName("email"));
        save();
    }

    public boolean equals(String name, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) {
        return iseq(name, this.name) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email);
    }

    public boolean equals(String name, String lastName, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) {
        return iseq(name, this.name) && iseq(lastName, this.lastName) && iseq(company, this.company) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email);
    }

    public boolean equals(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) {
        return iseq(name, this.name) && iseq(lastName, this.lastName) && iseq(company, this.company) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(state2, this.state2) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email);
    }

    protected boolean iseq(String a, String b) {
        return (a != null && a.equals(b)) || b == a;
    }

    protected void save() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO contact_info (id, address1, address2, city, state, state2, postal_code, country, phone, name, last_name, company, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.f193id);
            ps.setString(2, this.address1);
            ps.setString(3, this.address2);
            ps.setString(4, this.city);
            ps.setString(5, this.state);
            ps.setString(6, this.state2);
            ps.setString(7, this.postalCode);
            ps.setString(8, this.country);
            ps.setString(9, this.phone);
            ps.setString(10, this.name);
            ps.setString(11, this.lastName);
            ps.setString(12, this.company);
            ps.setString(13, this.email);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateCI(String name1, String lastName1, String company1, String address11, String address21, String city1, String state1, String postalCode1, String country1, String phone1, String email1) throws Exception {
        updateCI(name1, lastName1, company1, address11, address21, city1, state1, "", postalCode1, country1, phone1, email1);
    }

    public void updateCI(String name1, String lastName1, String company1, String address11, String address21, String city1, String state1, String state21, String postalCode1, String country1, String phone1, String email1) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE contact_info SET address1=?, address2=?, city=?, state=?, state2=?, postal_code=?, country=?, phone=?, name=?, last_name=?, company=?, email=? WHERE id = ?");
            ps.setString(1, address11);
            ps.setString(2, address21);
            ps.setString(3, city1);
            ps.setString(4, state1);
            ps.setString(5, state21);
            ps.setString(6, postalCode1);
            ps.setString(7, country1);
            ps.setString(8, phone1);
            ps.setString(9, name1);
            ps.setString(10, lastName1);
            ps.setString(11, company1);
            ps.setString(12, email1);
            ps.setLong(13, this.f193id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ContactInfoObject(long id) throws Exception {
        this.f193id = id;
        if (id > 0) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT name, last_name, company, address1, address2, city, state, state2, postal_code, country, phone, email FROM contact_info WHERE id = ?");
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.name = rs.getString(1);
                    this.lastName = rs.getString(2);
                    this.company = rs.getString(3);
                    this.address1 = rs.getString(4);
                    this.address2 = rs.getString(5);
                    this.city = rs.getString(6);
                    this.state = rs.getString(7);
                    this.state2 = rs.getString(8);
                    this.postalCode = rs.getString(9);
                    this.country = rs.getString(10);
                    this.phone = rs.getString(11);
                    this.email = rs.getString(12);
                    return;
                }
                throw new Exception("Not Found Contact Info #" + id);
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    public String getState() {
        return this.state;
    }

    public String getState2() {
        return this.state2;
    }

    public String getCountry() {
        return this.country;
    }
}
