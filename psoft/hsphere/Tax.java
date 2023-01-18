package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/Tax.class */
public class Tax implements TemplateHashModel {
    private static final int MATCH_NONE = 0;
    private static final int MATCH_COUNTRY = 1;
    private static final int MATCH_STATE = 2;
    private static final int MATCH_OUTSIDE_COUNTRY = 3;
    private static final int MATCH_OUTSIDE_STATE = 4;

    /* renamed from: id */
    protected long f53id;
    protected double percent;
    protected String description;
    protected long resellerId;
    protected boolean deleted;
    protected String country;
    protected String state;
    protected int flag;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("id")) {
            return new TemplateString(this.f53id);
        }
        if (key.equals("deleted")) {
            return new TemplateString(this.deleted);
        }
        if (key.equals("percent")) {
            return new TemplateString(this.percent);
        }
        if (key.equals("description")) {
            return new TemplateString(this.description);
        }
        if (key.equals("state")) {
            return new TemplateString(this.state);
        }
        if (key.equals("country")) {
            return new TemplateString(this.country);
        }
        if (key.equals("flag")) {
            return new TemplateString(this.flag);
        }
        if (key.equals("outside_state")) {
            return new TemplateString(this.flag == 4);
        } else if (key.equals("outside_country")) {
            return new TemplateString(this.flag == 3);
        } else {
            return null;
        }
    }

    public Tax(long id, double percent, String description, int flag, String country, String state) {
        this(id, percent, description, false, flag, country, state);
    }

    public Tax(long id, double percent, String description, boolean deleted, int flag, String country, String state) {
        this.f53id = id;
        this.percent = percent;
        this.description = description;
        this.deleted = deleted;
        this.flag = flag;
        this.country = country;
        this.state = state;
    }

    public long getId() {
        return this.f53id;
    }

    public int getFlag() {
        return this.flag;
    }

    public String getCountry() {
        return this.country;
    }

    public String getState() {
        return this.state;
    }

    public boolean isApplicable(String country, String state) throws Exception {
        if (country == null) {
            country = "";
        }
        if (state == null) {
            state = "";
        }
        switch (getFlag()) {
            case 1:
                return country.equals(getCountry());
            case 2:
                return country.equals(getCountry()) && state.equals(getState());
            case 3:
                return !country.equals(getCountry());
            case 4:
                return country.equals(getCountry()) && !state.equals(getState());
            default:
                return true;
        }
    }

    public double getPercent() {
        return this.percent;
    }

    public double getFactor() {
        return this.percent / 100.0d;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public String getDescription() {
        return this.description;
    }

    public static Tax create(double percent, String description, String country, String state, int outsideCountry, int outsideState) throws Exception {
        int flag;
        long id = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (country != null) {
            try {
                if (!country.equals("") && !country.equals("ANY")) {
                    if (state == null || state.equals("") || state.equals("ANY") || outsideCountry == 1) {
                        if (outsideCountry == 1) {
                            flag = 3;
                        } else {
                            flag = 1;
                        }
                        state = "";
                    } else if (outsideState == 1) {
                        flag = 4;
                    } else {
                        flag = 2;
                    }
                    Tax tax = new Tax(id, percent, description, flag, country, state);
                    ps = con.prepareStatement("INSERT INTO taxes(id, tax_percent,description, factor, reseller_id, flag, country, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                    ps.setLong(1, tax.getId());
                    ps.setDouble(2, tax.getPercent());
                    ps.setString(3, tax.getDescription());
                    ps.setDouble(4, tax.getFactor());
                    ps.setLong(5, Session.getResellerId());
                    ps.setInt(6, flag);
                    if (!"".equals(country) || country == null) {
                        ps.setNull(7, 12);
                    } else {
                        ps.setString(7, country);
                    }
                    if (!"".equals(state) || state == null) {
                        ps.setNull(8, 12);
                    } else {
                        ps.setString(8, state);
                    }
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                    con.close();
                    return tax;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        flag = 0;
        country = "";
        state = "";
        Tax tax2 = new Tax(id, percent, description, flag, country, state);
        ps = con.prepareStatement("INSERT INTO taxes(id, tax_percent,description, factor, reseller_id, flag, country, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setLong(1, tax2.getId());
        ps.setDouble(2, tax2.getPercent());
        ps.setString(3, tax2.getDescription());
        ps.setDouble(4, tax2.getFactor());
        ps.setLong(5, Session.getResellerId());
        ps.setInt(6, flag);
        if (!"".equals(country)) {
        }
        ps.setNull(7, 12);
        if (!"".equals(state)) {
        }
        ps.setNull(8, 12);
        ps.executeUpdate();
        Session.closeStatement(ps);
        con.close();
        return tax2;
    }

    public Tax delete() throws Exception {
        boolean willDelete = true;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM tax_bill_entry WHERE type = ?");
            ps.setLong(1, this.f53id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getLong(1) > 0) {
                willDelete = false;
            }
            ps.close();
            if (willDelete) {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM taxes WHERE id = ?");
                ps2.setLong(1, this.f53id);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                return null;
            }
            PreparedStatement ps3 = con.prepareStatement("UPDATE taxes SET deleted = ? WHERE id = ?");
            ps3.setInt(1, 1);
            ps3.setLong(2, this.f53id);
            ps3.executeUpdate();
            this.deleted = true;
            Session.closeStatement(ps3);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
