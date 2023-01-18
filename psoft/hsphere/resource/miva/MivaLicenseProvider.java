package psoft.hsphere.resource.miva;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/MivaLicenseProvider.class */
public class MivaLicenseProvider {
    public static final int LIC_FREE = 0;
    public static final int LIC_USED = 1;
    public static final int LIC_DEAD = 2;
    public static final int LIC_INSTALLED = 3;
    private static MivaLicenseProvider ourInstance = new MivaLicenseProvider();

    public static MivaLicenseProvider getInstance() {
        return ourInstance;
    }

    private MivaLicenseProvider() {
    }

    public String getLicense(String domainName) throws Exception {
        Connection con = Session.getDb();
        synchronized (this) {
            PreparedStatement ps = con.prepareStatement("SELECT lic,state FROM miva_lic WHERE domain_name = ?");
            ps.setString(1, domainName);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                Session.closeStatement(ps);
                PreparedStatement ps2 = con.prepareStatement("SELECT lic FROM miva_lic WHERE state = ? and domain_name IS NULL");
                ps2.setInt(1, 0);
                ResultSet rs1 = ps2.executeQuery();
                if (rs1.next()) {
                    String lic = rs1.getString("lic");
                    PreparedStatement ps1 = con.prepareStatement("UPDATE miva_lic SET state = ?, domain_name = ?  WHERE lic = ? AND domain_name IS NULL");
                    ps1.setInt(1, 1);
                    ps1.setString(2, domainName);
                    ps1.setString(3, lic);
                    ps1.executeUpdate();
                    Session.closeStatement(ps2);
                    Session.closeStatement(ps1);
                    con.close();
                    return lic;
                }
                throw new HSUserException("mivaresource.license");
            }
            int state = rs.getInt("state");
            String lic2 = rs.getString("lic");
            switch (state) {
                case 0:
                    Session.closeStatement(ps);
                    Session.closeStatement(null);
                    con.close();
                    return lic2;
                case 1:
                    throw new HSUserException("miva.license.allready_occupied");
                case 2:
                    PreparedStatement ps12 = con.prepareStatement("UPDATE miva_lic SET state = ? WHERE lic = ?");
                    ps12.setInt(1, 1);
                    ps12.setString(2, lic2);
                    ps12.executeUpdate();
                    Session.closeStatement(ps);
                    Session.closeStatement(ps12);
                    con.close();
                    return lic2;
                case 3:
                    Session.closeStatement(ps);
                    Session.closeStatement(null);
                    con.close();
                    return lic2;
                default:
                    Session.closeStatement(ps);
                    Session.closeStatement(null);
                    con.close();
                    throw new HSUserException("mivaresource.license");
            }
        }
    }

    public void setLicenseInstalled(String domainName, String lic) throws Exception {
        Connection con = Session.getDb();
        synchronized (this) {
            PreparedStatement ps = con.prepareStatement("UPDATE miva_lic SET domain_name = ?, state = ? WHERE lic = ?");
            ps.setString(1, domainName);
            ps.setInt(2, 3);
            ps.setString(3, lic);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void releaseLic(String lic) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        synchronized (this) {
            PreparedStatement ps = con.prepareStatement("SELECT state, domain_name FROM miva_lic WHERE lic = ?");
            ps.setString(1, lic);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int currState = rs.getInt("state");
                ps1 = con.prepareStatement("UPDATE miva_lic SET state = ?, domain_name = ? WHERE lic = ?");
                if (currState == 1 || currState == 3) {
                    ps1.setString(3, lic);
                    if (rs.getInt("state") == 1) {
                        if (rs.getString("domain_name") != null) {
                            ps1.setInt(1, 2);
                            ps1.setString(2, rs.getString("domain_name"));
                        } else {
                            ps1.setInt(1, 0);
                            ps1.setNull(2, 12);
                        }
                    } else if (rs.getInt("state") == 3) {
                        ps1.setInt(1, 2);
                        ps1.setString(2, rs.getString("domain_name"));
                    }
                    ps1.executeUpdate();
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        }
    }
}
