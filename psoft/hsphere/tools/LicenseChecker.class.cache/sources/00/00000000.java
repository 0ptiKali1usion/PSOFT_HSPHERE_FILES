package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.db.Database;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.license.License;
import psoft.util.Toolbox;

/* loaded from: LicenseChecker.class */
public class LicenseChecker {
    protected License license;
    protected long allowableAccounts = -1;
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
    protected Database db = Toolbox.getDB(this.config);

    public LicenseChecker() throws Exception {
        Session.setDb(this.db);
        Session.setResellerId(1L);
        this.license = new License(Settings.get().getValue("license"));
    }

    public void setAllowableAccounts(long accs) {
        this.allowableAccounts = accs;
    }

    private long getUsedAccounts() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        long result = 0;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) AS user_count FROM accounts WHERE deleted IS NULL");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getLong(1);
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private long getLicensedAccounts() throws Exception {
        String accounts = this.license.getValue("ACCOUNTS");
        if (accounts == null || "".equals(accounts)) {
            return -3L;
        }
        return Long.parseLong(accounts);
    }

    public int getAccountsStatus(long accs) throws Exception {
        this.allowableAccounts = accs;
        return getAccountsStatus();
    }

    public int getAccountsStatus() throws Exception {
        long usedAccounts = getUsedAccounts();
        if (this.allowableAccounts > -1 && this.allowableAccounts - usedAccounts < 0) {
            return -4;
        }
        return 0;
    }

    public boolean isLicenseExpired() throws Exception {
        String tmp_date = this.license.getValue("ISSUED");
        long tmpDate = Long.parseLong(tmp_date);
        GregorianCalendar issuedCal = new GregorianCalendar();
        issuedCal.setTime(new Date(tmpDate));
        GregorianCalendar today = new GregorianCalendar();
        today.setTime(new Date());
        issuedCal.add(5, 30);
        if (today.after(issuedCal)) {
            return true;
        }
        return false;
    }

    public int getLicenseStatus() throws Exception {
        if (!this.license.isValid()) {
            return -1;
        }
        String accounts = this.license.getValue("ACCOUNTS");
        if (accounts.equals("UNLIMITED")) {
            return 0;
        }
        String licenseType = this.license.getValue("LICENSE");
        boolean isTrial = licenseType.equalsIgnoreCase("TRIAL");
        boolean isExpired = isLicenseExpired();
        if (isTrial && isExpired) {
            return -2;
        }
        if (this.allowableAccounts > -1) {
            return getAccountsStatus();
        }
        long licensedAccounts = getLicensedAccounts();
        if (licensedAccounts < 0) {
            return -3;
        }
        long usedAccounts = getUsedAccounts();
        if (licensedAccounts - usedAccounts < 0) {
            return -3;
        }
        return 0;
    }

    public static void main(String[] args) {
        try {
            LicenseChecker lc = new LicenseChecker();
            if (args.length > 0) {
                try {
                    lc.setAllowableAccounts(Long.parseLong(args[0]));
                } catch (Exception e) {
                }
            }
            int status = lc.getLicenseStatus();
            switch (status) {
                case -4:
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere installation/update. Please contact sales@psoft.net");
                    System.exit(-4);
                    return;
                case -3:
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere license. Please contact sales@psoft.net for a license update.");
                    System.exit(-3);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere installation/update. Please contact sales@psoft.net");
                    System.exit(-4);
                    return;
                case -2:
                    System.out.println("The trial period has expired. Please contact sales@psoft.net for an H-Sphere license or uninstall H-Sphere.");
                    System.exit(-2);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere license. Please contact sales@psoft.net for a license update.");
                    System.exit(-3);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere installation/update. Please contact sales@psoft.net");
                    System.exit(-4);
                    return;
                case -1:
                    System.out.println("Your H-Sphere license is invalid.Please contact sales@psoft.net for a valid H-Sphere license.");
                    System.exit(-1);
                    System.out.println("The trial period has expired. Please contact sales@psoft.net for an H-Sphere license or uninstall H-Sphere.");
                    System.exit(-2);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere license. Please contact sales@psoft.net for a license update.");
                    System.exit(-3);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere installation/update. Please contact sales@psoft.net");
                    System.exit(-4);
                    return;
                case 0:
                    System.exit(0);
                    System.out.println("Your H-Sphere license is invalid.Please contact sales@psoft.net for a valid H-Sphere license.");
                    System.exit(-1);
                    System.out.println("The trial period has expired. Please contact sales@psoft.net for an H-Sphere license or uninstall H-Sphere.");
                    System.exit(-2);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere license. Please contact sales@psoft.net for a license update.");
                    System.exit(-3);
                    System.out.println("You have exceeded the number of accounts allowed by your H-Sphere installation/update. Please contact sales@psoft.net");
                    System.exit(-4);
                    return;
                default:
                    return;
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}