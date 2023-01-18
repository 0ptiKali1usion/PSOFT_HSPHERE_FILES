package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/cron/TrialCron.class */
public class TrialCron extends BackgroundJob {
    public TrialCron(C0004CP cp) throws Exception {
        super(cp, "TRIAL");
    }

    public TrialCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        getLog().info("Starting Trial Cron");
        Date startDate = TimeUtils.getDate();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT username, a.id  FROM user_account ua, users u, accounts a  WHERE u.id = ua.user_id AND a.id = ua.account_id  AND a.deleted IS NULL AND a.bi_id < 0");
                ResultSet rs = ps.executeQuery();
                if (rs.last()) {
                    setProgress(rs.getRow(), 1, 0);
                }
                if (rs.first()) {
                    Account a = null;
                    while (true) {
                        if (isInterrupted()) {
                            getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
                            break;
                        }
                        checkSuspended();
                        try {
                            User u = User.getUser(rs.getString(1));
                            getLog().debug("Trial: The user is " + u.getLogin());
                            Session.setUser(u);
                            a = Session.getUser().getAccount(new ResourceId(rs.getLong(2), 0));
                            Session.setAccount(a);
                            getLog().debug("Trial period for " + u.getLogin() + " is " + a.getTrialDayLeftTill());
                            if (a.getTrialDayLeftTill() <= 0) {
                                synchronized (a) {
                                    a.suspend("Your trial period has expired");
                                }
                            }
                            checkAccount(a, u);
                        } catch (Throwable ex) {
                            getLog().error("Trial cron error:", ex);
                            Ticket.create(ex, a, "Trial cron :Error during suspend : ");
                        }
                        addProgress(1);
                        if (!rs.next()) {
                            break;
                        }
                    }
                }
                long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
                getLog().info("Trial Cron FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                getLog().error("Trial Cron error: ", e);
                Session.closeStatement(null);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    protected void checkAccount(Account a, User u) throws Exception {
        boolean is_trial_warning;
        boolean is_trial_susp_not;
        boolean is_trial_del_not;
        boolean is_trial_del_reason;
        long trialDayTill = a.getTrialDayLeftTill();
        int trial_not_term1 = 0;
        int trial_not_term2 = 0;
        int trial_warn_susp_term1 = 0;
        int trial_warn_susp_term2 = 0;
        int trial_warn_del_term1 = 0;
        int trial_warn_del_term2 = 0;
        int trial_del_term = 0;
        try {
            trial_not_term1 = Integer.parseInt(Settings.get().getValue("trial_not_term1"));
        } catch (Exception e) {
        }
        try {
            trial_not_term2 = Integer.parseInt(Settings.get().getValue("trial_not_term2"));
        } catch (Exception e2) {
        }
        try {
            trial_warn_susp_term1 = Integer.parseInt(Settings.get().getValue("trial_warn_susp_term1"));
        } catch (Exception e3) {
        }
        try {
            trial_warn_susp_term2 = Integer.parseInt(Settings.get().getValue("trial_warn_susp_term2"));
        } catch (Exception e4) {
        }
        try {
            trial_warn_del_term1 = Integer.parseInt(Settings.get().getValue("trial_warn_del_term1"));
        } catch (Exception e5) {
        }
        try {
            trial_warn_del_term2 = Integer.parseInt(Settings.get().getValue("trial_warn_del_term2"));
        } catch (Exception e6) {
        }
        try {
            trial_del_term = Integer.parseInt(Settings.get().getValue("trial_del_term"));
        } catch (Exception e7) {
        }
        try {
            is_trial_warning = Boolean.valueOf(Settings.get().getValue("is_trial_warning")).booleanValue();
        } catch (Exception e8) {
            is_trial_warning = false;
        }
        try {
            is_trial_susp_not = Boolean.valueOf(Settings.get().getValue("is_trial_susp_not")).booleanValue();
        } catch (Exception e9) {
            is_trial_susp_not = false;
        }
        try {
            is_trial_del_not = Boolean.valueOf(Settings.get().getValue("is_trial_del_not")).booleanValue();
        } catch (Exception e10) {
            is_trial_del_not = false;
        }
        try {
            is_trial_del_reason = Boolean.valueOf(Settings.get().getValue("is_trial_del_reason")).booleanValue();
        } catch (Exception e11) {
            is_trial_del_reason = false;
        }
        if (!is_trial_warning && !is_trial_susp_not && !is_trial_del_not && !is_trial_del_reason) {
            return;
        }
        String email = a.getContactInfo().getEmail();
        SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
        DateFormat dateFormat = DateFormat.getDateInstance();
        Date current_date = TimeUtils.getDate();
        root.put("current_date", new TemplateString(dateFormat.format(current_date)));
        if (!a.isSuspended()) {
            Date delete_date = new Date(current_date.getTime() + ((trialDayTill + trial_warn_del_term1 + trial_warn_del_term2 + trial_del_term) * 86400000));
            if (delete_date.before(current_date)) {
                delete_date = current_date;
            }
            root.put("delete_date", new TemplateString(dateFormat.format(delete_date)));
            root.put("suspension_date", new TemplateString(dateFormat.format(new Date(current_date.getTime() + (trialDayTill * 86400000)))));
            root.put("trial_days_till", new TemplateString(String.valueOf(trialDayTill)));
            if (trialDayTill == trial_not_term1 || trialDayTill == trial_not_term2) {
                if (!is_trial_warning) {
                    return;
                }
                CustomEmailMessage.send("TRIAL_APPROACH_NOT", email, (TemplateModelRoot) root);
            }
            if ((trialDayTill == trial_warn_susp_term1 || trialDayTill == trial_warn_susp_term2) && is_trial_susp_not) {
                CustomEmailMessage.send("TRIAL_SUSP_NOT", email, (TemplateModelRoot) root);
                return;
            }
            return;
        }
        long daysInSuspend = Math.round((float) ((TimeUtils.currentTimeMillis() - a.getSuspendDate().getTime()) / 86400000));
        Date delete_date2 = new Date(a.getSuspendDate().getTime() + ((daysInSuspend + trial_warn_del_term1 + trial_warn_del_term2 + trial_del_term) * 86400000));
        if (delete_date2.before(current_date)) {
            delete_date2 = current_date;
        }
        root.put("delete_date", new TemplateString(dateFormat.format(delete_date2)));
        root.put("suspension_date", new TemplateString(dateFormat.format((Date) a.getSuspendDate())));
        if (daysInSuspend == trial_warn_del_term1 || daysInSuspend == trial_warn_del_term1 + trial_warn_del_term2) {
            if (!is_trial_del_not) {
                return;
            }
            CustomEmailMessage.send("TRIAL_DEL_NOT", email, (TemplateModelRoot) root);
        }
        if (daysInSuspend < trial_warn_del_term1 + trial_warn_del_term2 + trial_del_term || !is_trial_del_reason || a.getAccountType().isReseller()) {
            return;
        }
        CustomEmailMessage.send("TRIAL_DEL_REASON", email, (TemplateModelRoot) root);
        try {
            if (u.getResellerId() == 1) {
                Session.setUser(User.getUser(FMACLManager.ADMIN));
                Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
                ((AccountManager) Session.getAccount().FM_findChild(FMACLManager.ADMIN).get()).FM_deleteUserAccount(u.getLogin(), a.getId().getId());
                getLog().debug("Account " + a.getId().getId() + " has been deleted");
            } else {
                Reseller reseller = Reseller.getReseller(u.getResellerId());
                User admin = User.getUser(reseller.getAdmin());
                Session.setUser(admin);
                HashSet accounts = admin.getAccountIds(true);
                Iterator i = accounts.iterator();
                while (true) {
                    if (!i.hasNext()) {
                        break;
                    }
                    Account adminAccount = admin.getAccount((ResourceId) i.next());
                    ResourceId rid = adminAccount.FM_findChild(FMACLManager.ADMIN);
                    if (rid != null) {
                        Session.setAccount(adminAccount);
                        AccountManager manager = (AccountManager) rid.get();
                        manager.FM_deleteUserAccount(u.getLogin(), a.getId().getId());
                        getLog().debug("Account " + a.getId().getId() + " has been deleted");
                        break;
                    }
                }
            }
        } finally {
            Session.setAccount(a);
            Session.setUser(u);
        }
    }
}
