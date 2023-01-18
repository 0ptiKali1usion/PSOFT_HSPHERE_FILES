package psoft.hsphere.calc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Account;
import psoft.hsphere.Session;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ReferralCalc.class */
public class ReferralCalc {
    public static double calc() throws Exception {
        int referal_group = Session.getUser().getReferalGroupOfReferal();
        Session.getLog().debug("ReferalCalc: referal_group=" + referal_group);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT value FROM referal_group WHERE id = ? AND disabled=0");
            ps.setInt(1, referal_group);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double val = rs.getDouble("value");
                double count = count(referal_group, val);
                Session.closeStatement(ps);
                con.close();
                return count;
            }
            Session.closeStatement(ps);
            con.close();
            return 0.0d;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static double count(int referal_group, double val) throws Exception {
        Session.getLog().debug("This is switch and referal_group is:" + referal_group);
        switch (referal_group) {
            case 1:
                return val;
            case 2:
                return USFormat.parseDouble(Session.getAccount().getPlanValue("_REFERRAL_FEE"));
            case 3:
                return countGroup3();
            case 4:
                return val;
            default:
                return 0.0d;
        }
    }

    private static double countGroup3() throws Exception {
        Account a = Session.getAccount();
        long days = (a.getPeriodEnd().getTime() - a.getPeriodBegin().getTime()) / 86400000;
        double totalUsage = a.calc(3, a.getPeriodBegin(), a.getPeriodEnd());
        double totalSetup = a.calc(1, a.getPeriodBegin(), a.getPeriodEnd());
        double totalRecurr = a.calc(2, a.getPeriodBegin(), a.getPeriodEnd());
        return ((totalUsage + totalSetup) + totalRecurr) / days;
    }
}
