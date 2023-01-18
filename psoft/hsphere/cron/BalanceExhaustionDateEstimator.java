package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/BalanceExhaustionDateEstimator.class */
public class BalanceExhaustionDateEstimator extends BackgroundJob {
    public BalanceExhaustionDateEstimator(C0004CP cp) throws Exception {
        super(cp, "BALANCE_EXHAUSTION");
    }

    public BalanceExhaustionDateEstimator(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        PreparedStatement ps;
        User u;
        checkSuspended();
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            getLog().info("Account STARTED");
            Connection conExhaust = Session.getTransConnection();
            if (this.lastUser == 0) {
                ps = conExhaust.prepareStatement("DELETE FROM balance_runout_date");
            } else {
                ps = conExhaust.prepareStatement("DELETE FROM balance_runout_date WHERE EXISTS (SELECT account_id FROM user_account WHERE account_id = id AND user_id > ?)");
                ps.setLong(1, this.lastUser);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            Session.commitTransConnection(conExhaust);
            long startDate = TimeUtils.currentTimeMillis();
            ps2 = con.prepareStatement("SELECT u.username, u.id, a.id FROM users u, user_account u_a, accounts a, billing_info b_i WHERE u.id > ? AND u.id = u_a.user_id AND a.id = u_a.account_id AND a.bi_id = b_i.id AND b_i.type = ? ORDER BY u.id");
            ps2.setLong(1, this.lastUser);
            ps2.setString(2, "Check");
            ResultSet rs = ps2.executeQuery();
            if ((this.lastUser == 0 || !isProgressInitialized()) && rs.last()) {
                setProgress(rs.getRow(), 1, 0);
            }
            if (rs.first()) {
                int countUser = 0;
                while (true) {
                    String userName = rs.getString(1);
                    long userId = rs.getLong(2);
                    long accountId = rs.getLong(3);
                    if (isInterrupted()) {
                        getLog().debug("CRON JOB" + getFullMark() + " HAS BEEN INTERRUPTED");
                        break;
                    }
                    checkSuspended();
                    User sessionUser = Session.getUser();
                    if (sessionUser != null && sessionUser.getId() == userId) {
                        u = sessionUser;
                    } else {
                        try {
                            u = User.getUser(userId);
                            getLog().debug("Working on user '" + u.getLogin() + "'");
                            try {
                                Session.setUser(u);
                                countUser++;
                            } catch (UnknownResellerException e) {
                                getLog().warn("Unable to detect the reseller for user '" + u.getLogin() + "'.", e);
                            }
                        } catch (Exception e2) {
                            getLog().debug("Cannot get user '" + userName + "'. Skipping...", e2);
                        }
                    }
                    Account a = u.getAccount(Account.getAccount(accountId).getId());
                    Session.setAccount(a);
                    a.setLocked(true);
                    Session.setLanguage(new Language(null));
                    getLog().info("Processing account #" + a.getId());
                    freshStatusMessage("Working on account #" + a.getId().getId());
                    synchronized (a) {
                        if (!a.isSuspended() && a.getBillingInfo().getBillingType() == 2) {
                            a.estimateBalanceExhaustionDate();
                        }
                    }
                    Session.setAccount(null);
                    if (a != null) {
                        a.setLocked(false);
                    }
                    addProgress(1, "Finished processing user '" + u.getLogin() + "'");
                    saveLastUser(u.getId());
                    if (!rs.next()) {
                        break;
                    }
                }
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate;
            getLog().info("FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }
}
