package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/cron/SuspendCron.class */
public class SuspendCron extends BackgroundJob {
    private static transient HashMap sdtByRes;
    private static Date currentDate;

    public SuspendCron(C0004CP cp) throws Exception {
        super(cp, "SUSPEND_CRON");
    }

    public SuspendCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            getLog().info("CRON STARTED");
            currentDate = TimeUtils.getDate();
            long startDate = TimeUtils.currentTimeMillis();
            ps = con.prepareStatement("SELECT username FROM users WHERE id > ? ORDER BY id");
            ps.setLong(1, this.lastUser);
            ResultSet rs = ps.executeQuery();
            if ((this.lastUser == 0 || !isProgressInitialized()) && rs.last()) {
                setProgress(rs.getRow(), 1, 0);
            }
            if (rs.first()) {
                int countUser = 0;
                while (true) {
                    if (isInterrupted()) {
                        getLog().debug("CRON JOB" + getFullMark() + " HAS BEEN INTERRUPTED");
                        break;
                    }
                    checkSuspended();
                    try {
                        User u = User.getUser(rs.getString(1));
                        getLog().debug("Working on user '" + u.getLogin() + "'");
                        try {
                            Session.setUser(u);
                            countUser++;
                            Iterator i = u.getAccountIds().iterator();
                            while (i.hasNext()) {
                                Account a = u.getAccount((ResourceId) i.next());
                                Session.setAccount(a);
                                a.setLocked(true);
                                Session.setLanguage(new Language(null));
                                getLog().info("Processing account #" + a.getId());
                                freshStatusMessage("Working on account #" + a.getId().getId());
                                synchronized (a) {
                                    checkAccountDebt(a, u);
                                    if (a.getResellerId() != 1) {
                                        getLog().debug("Processing reseller # " + a.getResellerId());
                                        Reseller res = Reseller.getReseller(a.getResellerId());
                                        res.getAccount();
                                        if (res != null && res.isSuspended() && !a.isSuspended()) {
                                            getLog().debug("Trying to suspend user '" + u.getLogin() + "' of reseller #" + a.getResellerId());
                                            a.suspend("Your account will be suspended due to problems with your upstream provider. Contact your web hosting company to resolve this problem.");
                                        }
                                    }
                                    if (a.isSuspended()) {
                                        if (a.isPartlySuspended()) {
                                            getLog().debug("Force suspend for :" + a.getId().getId());
                                            a.suspend("");
                                        }
                                    } else if (a.isPartlySuspended()) {
                                        getLog().debug("Force resume for :" + a.getId().getId());
                                        a.resume();
                                    }
                                }
                                getLog().info("Account #" + a.getId() + " has been processed.");
                                Session.setAccount(null);
                                if (a != null) {
                                    a.setLocked(false);
                                }
                            }
                            addProgress(1, "Finished processing user '" + u.getLogin() + "'");
                            if (countUser > 10) {
                                saveLastUser(u.getId());
                                countUser = 0;
                            }
                        } catch (UnknownResellerException e) {
                            getLog().warn("Unable to detect the reseller for user '" + u.getLogin() + "'.", e);
                        }
                    } catch (Exception e2) {
                        getLog().debug("Cannot get user '" + rs.getString(1) + "'. Skipping...", e2);
                    }
                    if (!rs.next()) {
                        break;
                    }
                }
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate;
            getLog().info("FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            sdtByRes = null;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            sdtByRes = null;
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void checkAccountDebt(Account account, User user) throws Exception {
        Date negativeDate;
        Bill bill = account.getBill();
        if (!account.isSuspended()) {
            try {
                bill.charge(account.getBillingInfo());
                account.sendInvoice(currentDate);
            } catch (Throwable e) {
                if (e instanceof Exception) {
                    sendBillingException((Exception) e);
                }
                Session.getLog().debug("Unable to charge the account. ", e);
            }
        }
        if (!bill.isInDebt()) {
            return;
        }
        Long resId = new Long(Session.getResellerId());
        if (!getSdtByRes().containsKey(resId)) {
            getSdtByRes().put(resId, new SuspendDebtorTerms(resId));
        }
        SuspendDebtorTerms sdt = (SuspendDebtorTerms) getSdtByRes().get(resId);
        if (!sdt.isManagmentDebtorsSet()) {
            return;
        }
        String email = account.getBillingInfo().getEmail();
        Date negativeDate2 = bill.getNegativeDate();
        Date date = account.getBillingInfo().getNegativeDate();
        if (date != null) {
            Date date2 = TimeUtils.dropMinutes(date);
            if (date2.after(negativeDate2)) {
                negativeDate2 = date2;
            }
        }
        if (negativeDate2 != null) {
            negativeDate = TimeUtils.dropMinutes(negativeDate2);
        } else {
            negativeDate = TimeUtils.dropMinutes(currentDate);
        }
        Date not_term_Date = sdt.getNotTerm(negativeDate);
        Date warn_susp_term1_Date = sdt.getWarnSuspTerm1(negativeDate);
        Date warn_susp_term2_Date = sdt.getWarnSuspTerm2(negativeDate);
        Date susp_term_Date = sdt.getSuspTerm(negativeDate);
        SimpleHash root = CustomEmailMessage.getDefaultRoot(account);
        DateFormat dateFormat = DateFormat.getDateInstance();
        Date current_date = TimeUtils.dropMinutes(currentDate);
        root.put("bill", bill);
        root.put("negative_date", new TemplateString(dateFormat.format(negativeDate)));
        root.put("current_date", new TemplateString(dateFormat.format(current_date)));
        if (susp_term_Date.before(current_date)) {
            susp_term_Date = current_date;
        }
        root.put("suspend_date", new TemplateString(dateFormat.format(susp_term_Date)));
        if (!account.isSuspended()) {
            if (current_date.equals(not_term_Date)) {
                if (!sdt.isWarningSet()) {
                    return;
                }
                CustomEmailMessage.send("DEBT_WARN_NOTIFICATION", email, (TemplateModelRoot) root);
                return;
            } else if (current_date.equals(warn_susp_term1_Date) || current_date.equals(warn_susp_term2_Date)) {
                if (!sdt.isSuspNotSet()) {
                    return;
                }
                CustomEmailMessage.send("DEBT_SUSP_NOT", email, (TemplateModelRoot) root);
                return;
            } else if ((!current_date.equals(susp_term_Date) && !current_date.after(susp_term_Date)) || !sdt.isSuspReasonSet()) {
                return;
            } else {
                account.suspend(Localizer.translateMessage("suspendcron.reason"));
                return;
            }
        }
        Date suspendDate = TimeUtils.dropMinutes(account.getSuspendDate());
        Date warn_del_term1_Date = sdt.getWarnDelTerm1(suspendDate);
        Date warn_del_term2_Date = sdt.getWarnDelTerm2(suspendDate);
        Date del_Date = sdt.getDelTerm(suspendDate);
        if (del_Date.before(current_date)) {
            del_Date = current_date;
        }
        root.put("delete_date", new TemplateString(dateFormat.format(del_Date)));
        if (current_date.equals(warn_del_term1_Date) || current_date.equals(warn_del_term2_Date)) {
            if (!sdt.isDelNotSet()) {
                return;
            }
            CustomEmailMessage.send("DEBT_DEL_NOT", email, (TemplateModelRoot) root);
        } else if (!current_date.equals(del_Date) || !sdt.isDelReasonSet() || account.getAccountType().isReseller()) {
        } else {
            CustomEmailMessage.send("DEBT_DEL_REASON", email, (TemplateModelRoot) root);
            try {
                if (user.getResellerId() == 1) {
                    Session.setUser(User.getUser(FMACLManager.ADMIN));
                    Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
                    ((AccountManager) Session.getAccount().FM_findChild(FMACLManager.ADMIN).get()).FM_deleteUserAccount(user.getLogin(), account.getId().getId());
                    getLog().debug("Account " + account.getId().getId() + " has been deleted");
                } else {
                    Reseller reseller = Reseller.getReseller(user.getResellerId());
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
                            manager.FM_deleteUserAccount(user.getLogin(), account.getId().getId());
                            getLog().debug("Account " + account.getId().getId() + " has been deleted");
                            break;
                        }
                    }
                }
            } finally {
                Session.setAccount(account);
                Session.setUser(user);
            }
        }
    }

    private synchronized HashMap getSdtByRes() throws Exception {
        if (sdtByRes == null) {
            sdtByRes = new HashMap();
        }
        return sdtByRes;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/cron/SuspendCron$SuspendDebtorTerms.class */
    public class SuspendDebtorTerms {
        private Long resId;
        static final int NOT_TERM = 0;
        static final int WARN_SUSP_TERM1 = 1;
        static final int WARN_SUSP_TERM2 = 2;
        static final int SUSP_TERM = 3;
        static final int WARN_DEL_TERM1 = 4;
        static final int WARN_DEL_TERM2 = 5;
        static final int DEL_TERM = 6;
        private int[] terms = new int[7];
        private HashMap warns;

        SuspendDebtorTerms(Long resId) {
            SuspendCron.this = r5;
            this.warns = null;
            this.resId = resId;
            this.warns = new HashMap();
            try {
                initTerms();
                initWarnings();
            } catch (Exception e) {
                Session.getLog().debug("Unable to initialize SuspendDebtorTerms object", e);
            }
        }

        private void initTerms() throws Exception {
            int not_term = 0;
            int warn_susp_term1 = 0;
            int warn_susp_term2 = 0;
            int susp_term = 0;
            int warn_del_term1 = 0;
            int warn_del_term2 = 0;
            int del_term = 0;
            Session.save();
            try {
                Session.setResellerId(this.resId);
                try {
                    not_term = Integer.parseInt(Settings.get().getValue("not_term"));
                } catch (Exception e) {
                }
                this.terms[0] = not_term;
                try {
                    warn_susp_term1 = Integer.parseInt(Settings.get().getValue("warn_susp_term1"));
                } catch (Exception e2) {
                }
                this.terms[1] = warn_susp_term1;
                try {
                    warn_susp_term2 = Integer.parseInt(Settings.get().getValue("warn_susp_term2"));
                } catch (Exception e3) {
                }
                this.terms[2] = warn_susp_term2;
                try {
                    susp_term = Integer.parseInt(Settings.get().getValue("susp_term"));
                } catch (Exception e4) {
                }
                this.terms[3] = susp_term;
                try {
                    warn_del_term1 = Integer.parseInt(Settings.get().getValue("warn_del_term1"));
                } catch (Exception e5) {
                }
                this.terms[4] = warn_del_term1;
                try {
                    warn_del_term2 = Integer.parseInt(Settings.get().getValue("warn_del_term2"));
                } catch (Exception e6) {
                }
                this.terms[5] = warn_del_term2;
                try {
                    del_term = Integer.parseInt(Settings.get().getValue("del_term"));
                } catch (Exception e7) {
                }
                this.terms[6] = del_term;
            } finally {
                Session.restore();
            }
        }

        private void initWarnings() throws Exception {
            Session.save();
            try {
                Session.setResellerId(this.resId);
                try {
                    if (Boolean.valueOf(Settings.get().getValue("is_warning")).booleanValue()) {
                        this.warns.put("is_warning", Boolean.TRUE);
                    }
                } catch (Exception e) {
                }
                try {
                    if (Boolean.valueOf(Settings.get().getValue("is_susp_not")).booleanValue()) {
                        this.warns.put("is_susp_not", Boolean.TRUE);
                    }
                } catch (Exception e2) {
                }
                try {
                    if (Boolean.valueOf(Settings.get().getValue("is_susp_reason")).booleanValue()) {
                        this.warns.put("is_susp_reason", Boolean.TRUE);
                    }
                } catch (Exception e3) {
                }
                try {
                    if (Boolean.valueOf(Settings.get().getValue("is_del_not")).booleanValue()) {
                        this.warns.put("is_del_not", Boolean.TRUE);
                    }
                } catch (Exception e4) {
                }
                try {
                    if (Boolean.valueOf(Settings.get().getValue("is_del_reason")).booleanValue()) {
                        this.warns.put("is_del_reason", Boolean.TRUE);
                    }
                } catch (Exception e5) {
                }
            } finally {
                Session.restore();
            }
        }

        public boolean isManagmentDebtorsSet() {
            boolean result = false;
            if (isWarningSet() || isSuspNotSet() || isSuspReasonSet() || isDelNotSet() || isDelReasonSet()) {
                result = true;
            }
            return result;
        }

        public boolean isWarningSet() {
            return this.warns.get("is_warning") != null;
        }

        public boolean isSuspNotSet() {
            return this.warns.get("is_susp_not") != null;
        }

        public boolean isSuspReasonSet() {
            return this.warns.get("is_susp_reason") != null;
        }

        public boolean isDelNotSet() {
            return this.warns.get("is_del_not") != null;
        }

        public boolean isDelReasonSet() {
            return this.warns.get("is_del_reason") != null;
        }

        public Date getNotTerm(Date negDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(negDate);
            calendar.add(5, this.terms[0]);
            return calendar.getTime();
        }

        public Date getWarnSuspTerm1(Date negDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(negDate);
            calendar.add(5, this.terms[0] + this.terms[1]);
            return calendar.getTime();
        }

        public Date getWarnSuspTerm2(Date negDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(negDate);
            calendar.add(5, this.terms[0] + this.terms[1] + this.terms[2]);
            return calendar.getTime();
        }

        public Date getSuspTerm(Date negDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(negDate);
            calendar.add(5, this.terms[0] + this.terms[1] + this.terms[2] + this.terms[3]);
            return calendar.getTime();
        }

        public Date getWarnDelTerm1(Date suspDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(suspDate);
            calendar.add(5, this.terms[4]);
            return calendar.getTime();
        }

        public Date getWarnDelTerm2(Date suspDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(suspDate);
            calendar.add(5, this.terms[4] + this.terms[5]);
            return calendar.getTime();
        }

        public Date getDelTerm(Date suspDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(suspDate);
            calendar.add(5, this.terms[4] + this.terms[5] + this.terms[6]);
            return calendar.getTime();
        }
    }
}
