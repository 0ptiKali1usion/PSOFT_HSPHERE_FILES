package psoft.hsphere.resource.obs;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.PropertyResourceBundle;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/resource/obs/OBSDrive.class */
public class OBSDrive extends Resource {
    protected String size;
    protected long uid;
    protected static Database obsdb;
    protected long quota;
    protected String login;
    protected String password;

    static {
        try {
            obsdb = Toolbox.getDB(PropertyResourceBundle.getBundle("psoft_config.hsphere"), new NameModifier("OBS_"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected Database getDb() {
        return obsdb;
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return (this.quota / 1024.0d) / 1024.0d;
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        return 1.0d;
    }

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        try {
            return (USFormat.parseDouble((String) i.next()) / 1024.0d) / 1024.0d;
        } catch (Exception e) {
            Session.getLog().warn("Problem parsing double", e);
            return 0.0d;
        }
    }

    public static double getSetupMultiplier(InitToken token) {
        return 1.0d;
    }

    public OBSDrive(ResourceId id) throws Exception {
        super(id);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Session.getDb();
            ps = con.prepareStatement("SELECT userid FROM obs_drive WHERE id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.uid = rs.getLong(1);
                load();
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "quota".equals(key) ? new TemplateString(getQuota()) : "login".equals(key) ? new TemplateString(this.login) : "password".equals(key) ? new TemplateString(this.password) : super.get(key);
    }

    protected void load() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getDb().getConnection();
            ps = con.prepareStatement("SELECT usr_loginname, usr_loginpassword, usr_quota FROM \"user\" WHERE usr_id = ?");
            ps.setLong(1, this.uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.password = rs.getString(2);
                this.quota = (rs.getLong(3) / 1024) / 1024;
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        }
    }

    protected String getLogin() {
        return this.login;
    }

    protected String getPassword() {
        return this.password;
    }

    protected void setPassword(String password) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getDb().getConnection();
            ps = con.prepareStatement("UPDATE \"user\" SET usr_loginpassword = ? WHERE usr_id = ?");
            ps.setString(1, password);
            ps.setLong(2, this.uid);
            ps.executeUpdate();
            this.password = password;
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    protected long getQuota() {
        return this.quota;
    }

    protected void setQuota(long quota) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getDb().getConnection();
            ps = con.prepareStatement("UPDATE \"user\" SET usr_quota = ? WHERE usr_id = ?");
            ps.setLong(1, quota * 1024 * 1024);
            ps.setLong(2, this.uid);
            ps.executeUpdate();
            Date now = TimeUtils.getDate();
            recurrentRefund(now, Session.getAccount().getPeriodEnd());
            this.quota = quota;
            recurrentCharge(now, Session.getAccount().getPeriodEnd());
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public TemplateModel FM_addQuota(long quota) throws Exception {
        setQuota(quota + getQuota());
        return this;
    }

    public TemplateModel FM_setQuota(long quota) throws Exception {
        setQuota(quota);
        return this;
    }

    public TemplateModel FM_setPassword(String password) throws Exception {
        setPassword(password);
        return this;
    }

    /* JADX WARN: Finally extract failed */
    public OBSDrive(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Connection con = getDb().getConnection();
        Statement st = null;
        PreparedStatement ps = null;
        try {
            st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT user_seq.nextval FROM DUAL");
            rs.next();
            this.uid = rs.getLong(1);
            ps = con.prepareStatement("INSERT INTO \"user\" (usr_id, cnt_code, tusr_code, trp_code, sta_code, acl_id, usr_loginname, usr_loginpassword, usr_fname, usr_lname, usr_gender, usr_birthday, usr_address, usr_city, usr_region, usr_postalcode, usr_telephone, usr_fax, usr_email, usr_question, usr_answer, usr_quota, usr_aquota, usr_status, usr_regdate, usr_fsroot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.uid);
            Iterator i = initValues.iterator();
            ps.setString(2, i.next().toString());
            ps.setLong(3, 1L);
            ps.setLong(4, 1L);
            ps.setString(5, i.next().toString());
            ps.setLong(6, 1L);
            String obj = i.next().toString();
            this.login = obj;
            ps.setString(7, obj);
            String obj2 = i.next().toString();
            this.password = obj2;
            ps.setString(8, obj2);
            ps.setString(9, i.next().toString());
            ps.setString(10, i.next().toString());
            ps.setString(11, i.next().toString());
            String birthday = i.next().toString();
            java.sql.Date dateNow = TimeUtils.getSQLDate();
            if (birthday == null || birthday.length() == 0) {
                ps.setNull(12, 91);
            } else {
                ps.setDate(12, dateNow);
            }
            ps.setString(13, i.next().toString());
            ps.setString(14, i.next().toString());
            ps.setString(15, i.next().toString());
            ps.setString(16, i.next().toString());
            ps.setString(17, i.next().toString());
            ps.setString(18, i.next().toString());
            ps.setString(19, i.next().toString());
            ps.setString(20, i.next().toString());
            ps.setString(21, i.next().toString());
            long parseLong = Long.parseLong(i.next().toString()) * 1024 * 1024;
            this.quota = parseLong;
            ps.setLong(22, parseLong);
            this.quota = (this.quota / 1024) / 1024;
            ps.setLong(23, -1L);
            ps.setString(24, "PASSIVE");
            ps.setDate(25, dateNow);
            ps.setString(26, i.next().toString());
            boolean inserted = false;
            String newLogin = this.login;
            int j = 0;
            while (!inserted) {
                try {
                    ps.executeUpdate();
                    inserted = true;
                    this.login = newLogin;
                } catch (SQLException e) {
                    String str = newLogin.substring(0, (newLogin.length() - (j / 10)) - 1) + j;
                    newLogin = str;
                    ps.setString(7, str);
                    j++;
                }
            }
            Session.closeStatement(st);
            Session.closeStatement(ps);
            con.close();
            Connection con2 = Session.getDb();
            try {
                ps = con2.prepareStatement("INSERT INTO obs_drive (id, userid) VALUES (?, ?)");
                ps.setLong(1, getId().getId());
                ps.setLong(2, this.uid);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con2.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con2.close();
                throw th;
            }
        } catch (Throwable th2) {
            Session.closeStatement(st);
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELET FROM obs_drive WHERE id = ?");
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM \"user\" WHERE usr_id = ?");
            ps.setLong(1, this.uid);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
