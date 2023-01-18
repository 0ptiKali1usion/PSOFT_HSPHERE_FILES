package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MivaLicenseManipulator.class */
public class MivaLicenseManipulator extends Resource {
    protected Hashtable licenses;

    public MivaLicenseManipulator(int type, Collection init) throws Exception {
        super(type, init);
    }

    public MivaLicenseManipulator(ResourceId rid) throws Exception {
        super(rid);
    }

    protected void reloadLicenses() throws Exception {
        this.licenses = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT lic, state FROM miva_lic");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.licenses.put(rs.getString(1), rs.getString(2));
                Session.getLog().debug("Added miva license=" + rs.getString(1) + " state=" + rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void addLicense(String lic) throws Exception {
        LineNumberReader lics = new LineNumberReader(new StringReader(lic));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO miva_lic(lic, state) VALUES(?, 0)");
            while (true) {
                String currLic = lics.readLine();
                if (currLic != null) {
                    try {
                        ps.setString(1, currLic);
                        ps.executeUpdate();
                        this.licenses.put(currLic, "0");
                        this.licenses.put("state", "0");
                        Session.getLog().debug("Added license=" + currLic);
                    } catch (SQLException e) {
                        Session.addMessage(Localizer.translateMessage("miva.addingerror_license", new Object[]{currLic}));
                    }
                } else {
                    Session.closeStatement(ps);
                    con.close();
                    return;
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void updateLicenseNumber(String lic, String newNumber) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE miva_lic SET lic = ? WHERE lic = ?");
            ps.setString(1, newNumber);
            ps.setString(2, lic);
            ps.executeUpdate();
            String tstate = (String) this.licenses.get(lic);
            this.licenses.remove(lic);
            this.licenses.put(newNumber, tstate);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void deleteLicense(String lic) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT state FROM miva_lic WHERE lic = ?");
            ps.setString(1, lic);
            ResultSet rs = ps.executeQuery();
            int state = 0;
            if (rs.next()) {
                state = rs.getInt(1);
            }
            if (state == 1 || state == 3) {
                throw new HSUserException("miva.cannot_delete");
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM miva_lic WHERE lic = ?");
            ps2.setString(1, lic);
            ps2.executeUpdate();
            this.licenses.remove(lic);
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateString FM_getAccountId(String lic) throws Exception {
        String accountId = "";
        String state = (String) this.licenses.get(lic);
        if (state.equals("1") || state.equals("3")) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT parent_child.account_id FROM miva_merch, parent_child WHERE miva_merch.lic = ? AND miva_merch.id = parent_child.child_id");
                ps.setString(1, lic);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    accountId = rs.getString(1);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return new TemplateString(accountId);
    }

    public TemplateModel FM_getDomain(String lic) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String domain = "";
        try {
            ps = con.prepareStatement("SELECT name FROM domains WHERE id=(SELECT parent_id FROM parent_child WHERE child_id=(SELECT parent_id FROM parent_child, miva_merch WHERE id=child_id AND lic=?))");
            ps.setString(1, lic);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                domain = rs.getString(1);
            }
            Session.closeStatement(ps);
            con.close();
            return new TemplateString(domain);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getLicenseState(String lic) {
        return (String) this.licenses.get(lic);
    }

    public void FM_reloadLicenses() throws Exception {
        reloadLicenses();
    }

    public TemplateModel FM_addLicense(String lic) throws Exception {
        addLicense(lic);
        return this;
    }

    public TemplateModel FM_deleteLicense(String lic) throws Exception {
        deleteLicense(lic);
        return this;
    }

    public TemplateModel FM_updateLicenseNumber(String lic, String newNumber) throws Exception {
        updateLicenseNumber(lic, newNumber);
        return this;
    }

    public TemplateModel FM_getLicenseState(String lic) throws Exception {
        return new TemplateString(getLicenseState(lic));
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("licenses")) {
            return new ListAdapter(this.licenses.keySet());
        }
        if (key.equals("total_licenses")) {
            return new TemplateString(this.licenses.size());
        }
        if (key.equals("free_licenses")) {
            return quantityFreeLicenses();
        }
        if (key.equals("occupied_licenses")) {
            return quantityOccupiedLicenses();
        }
        if (key.equals("dead_licenses")) {
            return quantityDeadLicenses();
        }
        if (key.equals("installed_licenses")) {
            return quantityInstalledLicenses();
        }
        return super.get(key);
    }

    private TemplateModel quantityOccupiedLicenses() {
        int occupied = 0;
        Enumeration elements = this.licenses.elements();
        while (elements.hasMoreElements()) {
            int state = Integer.parseInt((String) elements.nextElement());
            if (state == 1) {
                occupied++;
            }
        }
        return new TemplateString(occupied);
    }

    private TemplateModel quantityFreeLicenses() {
        int free = 0;
        Enumeration elements = this.licenses.elements();
        while (elements.hasMoreElements()) {
            int state = Integer.parseInt((String) elements.nextElement());
            if (state == 0) {
                free++;
            }
        }
        return new TemplateString(free);
    }

    private TemplateModel quantityDeadLicenses() {
        int dead = 0;
        Enumeration elements = this.licenses.elements();
        while (elements.hasMoreElements()) {
            int state = Integer.parseInt((String) elements.nextElement());
            if (state == 2) {
                dead++;
            }
        }
        return new TemplateString(dead);
    }

    private TemplateModel quantityInstalledLicenses() {
        int installed = 0;
        Enumeration elements = this.licenses.elements();
        while (elements.hasMoreElements()) {
            int state = Integer.parseInt((String) elements.nextElement());
            if (state == 3) {
                installed++;
            }
        }
        return new TemplateString(installed);
    }

    public void FM_freeLicense(String lic) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE miva_lic SET state=?, domain_name=? WHERE lic=?");
            ps.setInt(1, 0);
            ps.setNull(2, 12);
            ps.setString(3, lic);
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
