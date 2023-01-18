package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Category;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/AccountPreferences.class */
public class AccountPreferences implements TemplateHashModel, TemplateMethodModel {
    private static Category log = Category.getInstance(AccountPreferences.class.getName());
    long accountId;
    HashMap preferences = null;
    public static final String DESIGN_ID = "design_id";
    public static final String ICON_IMAGE_SET = "icon_image_set";
    public static final String SKILL_ICON_SET = "skill_icon_set";
    public static final String LANGUAGE = "lang";

    public AccountPreferences(long id) {
        this.accountId = id;
    }

    public boolean isEmpty() {
        try {
            init();
        } catch (SQLException e) {
            log.warn("Error", e);
        }
        return this.preferences.isEmpty();
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        try {
            List l2 = HTMLEncoder.decode(l);
            setValue((String) l2.get(0), (String) l2.get(1));
            return null;
        } catch (SQLException e) {
            log.warn("Error", e);
            throw new TemplateModelException(e.getMessage());
        }
    }

    public TemplateModel get(String key) {
        return new TemplateString(getValueSafe(key));
    }

    public String getValueSafe(String key) {
        try {
            String value = getValue(key);
            return value == null ? "" : value;
        } catch (SQLException se) {
            log.info("Error retreiving value for account# " + this.accountId + ", key# " + key, se);
            return "";
        }
    }

    public synchronized String getValue(String key) throws SQLException {
        init();
        return (String) this.preferences.get(key);
    }

    public synchronized void removeValue(String key) throws SQLException {
        init();
        if (this.preferences.containsKey(key)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("DELETE FROM account_pref WHERE id = ? AND attribute = ?");
                ps.setLong(1, this.accountId);
                ps.setString(2, key);
                ps.executeUpdate();
                this.preferences.remove(key);
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public synchronized void setValue(String key, String value) throws SQLException {
        init();
        boolean valueExists = this.preferences.containsKey(key);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (value == null) {
            value = "";
        }
        try {
            if (valueExists) {
                ps = con.prepareStatement("UPDATE account_pref SET value = ? WHERE attribute = ? AND id = ?");
            } else {
                ps = con.prepareStatement("INSERT INTO account_pref (value, attribute, id) VALUES (?, ?, ?)");
            }
            ps.setString(1, value);
            ps.setString(2, key);
            ps.setLong(3, this.accountId);
            ps.executeUpdate();
            this.preferences.put(key, value);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void load() throws SQLException {
        this.preferences = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT attribute, value FROM account_pref WHERE id = ?");
            ps.setLong(1, this.accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.preferences.put(rs.getString(1), rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private synchronized void init() throws SQLException {
        if (this.preferences == null) {
            load();
        }
    }

    public String getPromoCode() {
        String res = null;
        try {
            res = getValue("_ACCOUNT_PROMO");
        } catch (Exception e) {
            Session.getLog().error("Error getting account preferences: _ACCOUNT_PROMO");
        }
        return res;
    }
}
