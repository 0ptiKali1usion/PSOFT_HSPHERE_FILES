package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import psoft.hsphere.AccessError;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.util.freemarker.TemplateString;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;
import psoft.validators.ServletRequestAccessor;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ContactInfo.class */
public class ContactInfo extends Resource {
    protected Map info;
    protected RegistrantExtraInfo extraInfo;
    static final String EXTRAINFO_PREFIX = "extra_";
    static final String[] EXTRA_FIELDS = {"tld", "app_purpose", "nexus_category", "nexus_validator", "isa_trademark", "legal_type", "lang_pref", "confirmaddress", "uk_legal_type", "uk_reg_co_no", "registered_for"};

    public Map getMap() {
        return new HashMap(this.info);
    }

    public Map getExtraInfo() {
        if (this.extraInfo != null) {
            return new HashMap(this.extraInfo.getInfo());
        }
        return null;
    }

    protected void load() throws Exception {
        String extra = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT first_name, last_name, org_name, address1, address2, address3, city, state, country, postal_code, phone, fax, email, extra FROM domain_contact_info WHERE id = ? AND type= ?");
            ps.setLong(1, this.f41id.getId());
            ps.setLong(2, this.f41id.getType());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.info = new HashMap();
                this.info.put("first_name", rs.getString(1));
                this.info.put("last_name", rs.getString(2));
                this.info.put("org_name", rs.getString(3));
                this.info.put("address1", rs.getString(4));
                this.info.put("address2", rs.getString(5));
                this.info.put("address3", rs.getString(6));
                this.info.put("city", rs.getString(7));
                this.info.put("state", rs.getString(8));
                this.info.put("country", rs.getString(9));
                this.info.put("postal_code", rs.getString(10));
                this.info.put("phone", rs.getString(11));
                this.info.put("fax", rs.getString(12));
                this.info.put("email", rs.getString(13));
                extra = rs.getString(14);
            }
            Session.closeStatement(ps);
            con.close();
            if (extra != null) {
                this.extraInfo = new RegistrantExtraInfo(extra);
                this.extraInfo.load();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ContactInfo(ResourceId id) throws Exception {
        super(id);
        this.extraInfo = null;
        load();
    }

    public ContactInfo(int type, Collection values) throws Exception {
        super(type, values);
        this.extraInfo = null;
        this.info = new HashMap();
        if (values.size() > 1) {
            Iterator i = values.iterator();
            this.info.put("first_name", (String) i.next());
            this.info.put("last_name", (String) i.next());
            this.info.put("org_name", (String) i.next());
            this.info.put("address1", (String) i.next());
            this.info.put("address2", (String) i.next());
            this.info.put("address3", (String) i.next());
            this.info.put("city", (String) i.next());
            this.info.put("state", (String) i.next());
            this.info.put("country", (String) i.next());
            this.info.put("postal_code", (String) i.next());
            this.info.put("phone", (String) i.next());
            this.info.put("fax", (String) i.next());
            this.info.put("email", (String) i.next());
            if (i.hasNext()) {
                Object value = i.next();
                if (Map.class.isInstance(value)) {
                    this.extraInfo = new RegistrantExtraInfo((Map) value);
                    return;
                }
                return;
            }
            return;
        }
        String mod = (String) values.iterator().next();
        NameModifier nm = new NameModifier(mod);
        Session.getLog().debug("ContactInfo name modifier:" + mod);
        Accessor a = new ServletRequestAccessor(Session.getRequest());
        this.info.put("first_name", a.get(nm.getName("first_name")));
        Session.getLog().debug("ContactInfo name modifier:" + a.get(nm.getName("first_name")));
        this.info.put("last_name", a.get(nm.getName("last_name")));
        this.info.put("org_name", a.get(nm.getName("org_name")));
        this.info.put("address1", a.get(nm.getName("address1")));
        this.info.put("address2", a.get(nm.getName("address2")));
        this.info.put("address3", a.get(nm.getName("address3")));
        this.info.put("city", a.get(nm.getName("city")));
        this.info.put("state", a.get(nm.getName("state")));
        this.info.put("country", a.get(nm.getName("country")));
        this.info.put("postal_code", a.get(nm.getName("postal_code")));
        this.info.put("phone", a.get(nm.getName("phone")));
        this.info.put("fax", a.get(nm.getName("fax")));
        this.info.put("email", a.get(nm.getName("email")));
        NameModifier nm2 = new NameModifier(mod + EXTRAINFO_PREFIX);
        Map extraMap = new HashMap();
        for (int ind = 0; ind < EXTRA_FIELDS.length; ind++) {
            String extraValue = a.get(nm2.getName(EXTRA_FIELDS[ind]));
            Session.getLog().debug("KATON: Extra param: " + nm2.getName(EXTRA_FIELDS[ind]) + " value: " + extraValue);
            if (extraValue != null) {
                extraMap.put(EXTRA_FIELDS[ind], extraValue);
            }
        }
        if (!extraMap.isEmpty()) {
            this.extraInfo = new RegistrantExtraInfo(extraMap);
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO domain_contact_info (id, type, first_name, last_name, org_name, address1, address2, address3, city, state, country, postal_code, phone, fax, email, extra) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, getId().getType());
            ps.setString(3, (String) this.info.get("first_name"));
            ps.setString(4, (String) this.info.get("last_name"));
            ps.setString(5, (String) this.info.get("org_name"));
            ps.setString(6, (String) this.info.get("address1"));
            ps.setString(7, (String) this.info.get("address2"));
            ps.setString(8, (String) this.info.get("address3"));
            ps.setString(9, (String) this.info.get("city"));
            ps.setString(10, (String) this.info.get("state"));
            ps.setString(11, (String) this.info.get("country"));
            ps.setString(12, (String) this.info.get("postal_code"));
            ps.setString(13, (String) this.info.get("phone"));
            ps.setString(14, (String) this.info.get("fax"));
            ps.setString(15, (String) this.info.get("email"));
            if (this.extraInfo != null) {
                ps.setString(16, this.extraInfo.getExtra());
            } else {
                ps.setString(16, null);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (this.extraInfo != null) {
                this.extraInfo.writeDb();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        String val = (String) this.info.get(key);
        if (val == null && this.extraInfo != null) {
            val = this.extraInfo.get(key);
        }
        return val == null ? super.get(key) : new TemplateString(val);
    }

    public TemplateModel FM_update(String first_name, String last_name, String org_name, String address1, String address2, String address3, String city, String state, String country, String postal_code, String phone, String fax, String email) throws AccessError {
        getLog().info("Updating contact info");
        try {
            accessCheck(0);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE domain_contact_info SET first_name = ?, last_name = ?, org_name = ?, address1 = ?, address2 = ?, address3 = ?, city = ?, state = ?, country = ?, postal_code = ?, phone = ?, fax = ?, email = ? WHERE id = ?");
            ps.setString(1, first_name);
            ps.setString(2, last_name);
            ps.setString(3, org_name);
            ps.setString(4, address1);
            ps.setString(5, address2);
            ps.setString(6, address3);
            ps.setString(7, city);
            ps.setString(8, state);
            ps.setString(9, country);
            ps.setString(10, postal_code);
            ps.setString(11, phone);
            ps.setString(12, fax);
            ps.setString(13, email);
            ps.setLong(14, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            load();
            return this;
        } catch (Exception se) {
            getLog().warn("Error updating contact info", se);
            return new TemplateErrorResult(se);
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        accessCheck(1);
        super.delete();
        if (this.extraInfo != null) {
            this.extraInfo.deleteDb();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM domain_contact_info WHERE id = ?");
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

    public boolean containsExtraInfo() {
        return this.extraInfo != null;
    }

    public void setExtraInfo(Map info) throws Exception {
        this.extraInfo = new RegistrantExtraInfo(info);
        this.extraInfo.writeDb();
    }

    public static Map getExtraMap(Map inputMap, String paramPrefix) {
        Map extraMap = new HashMap();
        new NameModifier(paramPrefix);
        for (int ind = 0; ind < EXTRA_FIELDS.length; ind++) {
            String extraValue = (String) inputMap.get(paramPrefix + EXTRA_FIELDS[ind]);
            if (extraValue != null) {
                extraMap.put(EXTRA_FIELDS[ind], extraValue);
            }
        }
        if (extraMap.isEmpty()) {
            return null;
        }
        return extraMap;
    }

    public void setExtraInfoUsTld(String app_purpose, String nexus_category, String validator) throws Exception {
        this.extraInfo = new RegistrantExtraInfo();
        this.extraInfo.usTldFields(app_purpose, nexus_category, validator);
        this.extraInfo.writeDb();
    }

    public void setExtraInfoCaTld(String isa_trademark, String legal_type, String lang_pref, String domain_description) throws Exception {
        this.extraInfo = new RegistrantExtraInfo();
        this.extraInfo.caTldFields(isa_trademark, legal_type, lang_pref, domain_description);
        this.extraInfo.writeDb();
    }

    public void setExtraInfoDeTld(String confirmaddress) throws Exception {
        this.extraInfo = new RegistrantExtraInfo();
        this.extraInfo.deTldFields(confirmaddress);
        this.extraInfo.writeDb();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/ContactInfo$RegistrantExtraInfo.class */
    public class RegistrantExtraInfo {
        protected Map info;
        protected String extra;

        public RegistrantExtraInfo() {
            ContactInfo.this = r4;
            this.extra = "1";
            this.info = null;
        }

        public RegistrantExtraInfo(String extra) {
            ContactInfo.this = r4;
            this.extra = "1";
            this.extra = extra;
        }

        public RegistrantExtraInfo(Map info) {
            ContactInfo.this = r6;
            this.extra = "1";
            this.info = new HashMap(info);
        }

        public void usTldFields(String app_purpose, String nexus_category, String validator) throws Exception {
            this.info = new HashMap();
            this.info.put("app_purpose", app_purpose);
            this.info.put("nexus_category", nexus_category);
            this.info.put("nexus_validator", validator);
            this.info.put("tld", "us");
            this.extra = "us";
        }

        public void caTldFields(String isa_trademark, String legal_type, String lang_pref, String domain_description) throws Exception {
            this.info = new HashMap();
            this.info.put("isa_trademark", isa_trademark);
            this.info.put("legal_type", legal_type);
            this.info.put("lang_pref", lang_pref);
            this.info.put("domain_description", domain_description);
            this.info.put("tld", "ca");
            this.extra = "ca";
        }

        public void deTldFields(String confirmaddress) throws Exception {
            this.info = new HashMap();
            this.info.put("confirmaddress", confirmaddress);
            this.info.put("tld", "de");
            this.extra = "de";
        }

        protected void writeDb() throws SQLException {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM domain_extra_info WHERE ci_id = ?");
                ps2.setLong(1, ContactInfo.this.f41id.getId());
                ps2.executeUpdate();
                ps = con.prepareStatement("INSERT INTO domain_extra_info (ci_id, name, value) VALUES (?, ?, ?)");
                ps.setLong(1, ContactInfo.this.f41id.getId());
                for (String name : this.info.keySet()) {
                    ps.setString(2, name);
                    ps.setString(3, (String) this.info.get(name));
                    ps.executeUpdate();
                }
                Session.commitTransConnection(con);
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }

        protected void load() throws SQLException {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT name, value FROM domain_extra_info WHERE ci_id = ?");
                ps.setLong(1, ContactInfo.this.f41id.getId());
                ResultSet rs = ps.executeQuery();
                this.info = new HashMap();
                while (rs.next()) {
                    this.info.put(rs.getString("name"), rs.getString("value"));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }

        protected void deleteDb() throws SQLException {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("DELETE FROM domain_extra_info WHERE ci_id = ?");
                ps.setLong(1, ContactInfo.this.f41id.getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }

        public String getExtra() {
            return this.extra;
        }

        public Map getInfo() {
            return this.info;
        }

        public String get(String key) {
            if (this.info == null) {
                return null;
            }
            return (String) this.info.get(key);
        }
    }
}
