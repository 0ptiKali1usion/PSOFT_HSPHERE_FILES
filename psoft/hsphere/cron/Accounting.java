package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.billing.Service;
import psoft.hsphere.plan.ResourceType;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/Accounting.class */
public class Accounting extends BackgroundJob {
    public static final int STAGE_NOTRUNNING = 0;
    public static final int STAGE_SERVICES = 1;
    public static final int STAGE_ACCOUNTS = 2;
    public static final int STAGE_MOTHLY = 3;
    protected int stage;

    public int getStage() {
        return this.stage;
    }

    public void setStage(int stage) throws Exception {
        this.stage = stage;
        saveStatusData();
    }

    public Accounting(C0004CP cp) throws Exception {
        super(cp, "LAST_LAUNCH");
        this.stage = 0;
    }

    public Accounting(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
        this.stage = 0;
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        long startDate = TimeUtils.currentTimeMillis();
        switch (getStage()) {
            case 0:
                try {
                    Service.bill();
                    setStage(1);
                    setProgress(100, 1, 30);
                } catch (Exception ex) {
                    getLog().warn("Billing exception", ex);
                    sendBillingException(ex);
                } catch (Throwable ex2) {
                    getLog().warn("Billing error", ex2);
                }
            case 1:
                try {
                    chargeAccounts();
                } catch (Exception ex3) {
                    getLog().warn("Charging account and opening new period begin exception", ex3);
                    sendBillingException(ex3);
                } catch (Throwable ex4) {
                    getLog().warn("Charging account and opening new period begin error", ex4);
                }
            case 2:
                try {
                    chargeMonthlyBilledResources();
                    break;
                } catch (Exception ex5) {
                    getLog().warn("Charge Monthly Billed Resources exception", ex5);
                    sendBillingException(ex5);
                    break;
                } catch (Throwable ex6) {
                    getLog().warn("Charge Monthly Billed Resources error", ex6);
                    break;
                }
        }
        setStage(0);
        getLog().debug("Start date in Accounting = " + startDate);
        long timeDiff = TimeUtils.currentTimeMillis() - startDate;
        getLog().info("Accounting FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
    }

    private void chargeAccounts() throws Exception {
        List accounts = generateAccountListByQuery("SELECT a.id , 0 FROM accounts a, user_account ua, users u, billing_info bi, balance_credit b where ua.user_id = u.id AND ua.account_id = a.id AND a.deleted is null AND a.suspended IS NULL AND bi.id = a.bi_id AND b.id = a.id AND ((bi.type = 'CC' AND b.balance < 0) or (a.p_end <= ?))", TimeUtils.getSQLDate());
        if (!accounts.isEmpty()) {
            setProgress(accounts.size() * 3, 1, accounts.size());
            int i = 0;
            while (true) {
                if (i >= accounts.size()) {
                    break;
                }
                ResourceId aid = (ResourceId) accounts.get(i);
                if (isInterrupted()) {
                    getLog().debug("CRON JOB" + getFullMark() + " HAS BEEN INTERRUPTED");
                    break;
                }
                checkSuspended();
                try {
                    Account a = (Account) Account.get(aid);
                    getLog().debug("Accounting: The account is: " + a.getId().getId());
                    try {
                        User u = a.getUser();
                        getLog().debug("Accounting: The user is: " + u.getLogin());
                        if (u != null) {
                            try {
                                Session.setUser(u);
                                Session.setAccount(a);
                                try {
                                    try {
                                        a.setLocked(true);
                                        Session.setLanguage(new Language(null));
                                        getLog().info("Accounting: Using Account " + a.getId());
                                        freshStatusMessage("Working on account #" + a.getId().getId());
                                        Collection resIds = a.getChildManager().getAllResources();
                                        if (!a.isSuspended()) {
                                            a.charge(true, resIds);
                                        }
                                        if (a != null) {
                                            addProgress(1, "Billed account#" + a.getId().getId());
                                            a.setLocked(false);
                                        }
                                        Session.setAccount(null);
                                    } catch (Exception ex) {
                                        getLog().warn("Billing exception", ex);
                                        sendBillingException(ex);
                                        if (a != null) {
                                            addProgress(1, "Billed account#" + a.getId().getId());
                                            a.setLocked(false);
                                        }
                                        Session.setAccount(null);
                                    } catch (Throwable ex2) {
                                        getLog().warn("Billing error", ex2);
                                        if (a != null) {
                                            addProgress(1, "Billed account#" + a.getId().getId());
                                            a.setLocked(false);
                                        }
                                        Session.setAccount(null);
                                    }
                                } catch (Throwable th) {
                                    if (a != null) {
                                        addProgress(1, "Billed account#" + a.getId().getId());
                                        a.setLocked(false);
                                    }
                                    Session.setAccount(null);
                                    throw th;
                                }
                            } catch (Exception e) {
                                getLog().debug("Can't get user for account : #" + aid.getId() + " skipping", e);
                            }
                        } else {
                            getLog().debug("Can't get user for account : #" + aid.getId() + " skipping");
                        }
                    } catch (Exception e2) {
                        getLog().debug("Can't get user for account : #" + aid.getId() + " skipping", e2);
                    }
                } catch (Exception e3) {
                    getLog().debug("Can't get account: #" + aid.getId() + " skipping", e3);
                }
                i++;
            }
        }
        setStage(2);
    }

    private List generateAccountListByQuery(String queryToBill, Date sqlDate) throws SQLException {
        List accounts = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(queryToBill);
            ps.setDate(1, sqlDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ResourceId rid = new ResourceId(rs.getLong(1), rs.getInt(2));
                accounts.add(rid);
            }
            Session.closeStatement(ps);
            con.close();
            return accounts;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void chargeMonthlyBilledResources() throws Exception {
        Set<Integer> monthlyIds = TypeRegistry.getMonthlyBilledIds();
        String query = "";
        for (Integer type : monthlyIds) {
            if (!TypeRegistry.isDummy(type.intValue())) {
                if (!"".equals(query)) {
                    query = query + ",";
                }
                query = query + type.toString();
            }
        }
        if ("".equals(query)) {
            return;
        }
        String queryToBill = "SELECT child_id, child_type, account_id FROM parent_child WHERE child_type IN (" + query + ") AND p_begin <= ? ORDER BY account_id";
        Calendar cal = TimeUtils.getCalendar();
        cal.add(2, -1);
        Hashtable accounts = generateResourceListByQuery(queryToBill, cal);
        if (accounts.isEmpty()) {
            return;
        }
        setProgress(accounts.size() * 3, 1, accounts.size() * 2);
        Enumeration en = accounts.keys();
        while (en.hasMoreElements()) {
            Long aId = (Long) en.nextElement();
            List resIds = (List) accounts.get(aId);
            if (resIds != null && !resIds.isEmpty()) {
                ResourceId accountId = new ResourceId(aId.longValue(), 0);
                Session.save();
                Account a = null;
                try {
                    try {
                        if (isInterrupted()) {
                            getLog().debug("CRON JOB" + getFullMark() + " HAS BEEN INTERRUPTED");
                            if (0 != 0) {
                                addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                a.setLocked(false);
                            }
                            try {
                                Session.restore();
                                return;
                            } catch (UnknownResellerException uex) {
                                Session.getLog().error("Unable to restore user", uex);
                                return;
                            }
                        }
                        checkSuspended();
                        try {
                            a = (Account) Account.get(accountId);
                            a.setLocked(true);
                            getLog().debug("Accounting: The account is: " + a.getId().getId());
                            try {
                                User u = a.getUser();
                                getLog().debug("Accounting: The user is: " + u.getLogin());
                                if (u != null) {
                                    try {
                                        Session.setUser(u);
                                        Session.setAccount(a);
                                        java.util.Date now = TimeUtils.getDate();
                                        if (!a.isSuspended()) {
                                            boolean chargedMonthly = chargeMonthlyBilled(a, resIds);
                                            try {
                                                a.getBill().charge(a.getBillingInfo(), a.isAnniversaryBillingType());
                                                if (!a.sendInvoice(now) && chargedMonthly && "month".equals(Settings.get().getValue("send_no_charge"))) {
                                                    a.sendNoChargeNotification();
                                                }
                                            } catch (Throwable e) {
                                                Session.getLog().debug("Unable to charge the account. ", e);
                                            }
                                        }
                                        if (a != null) {
                                            addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                            a.setLocked(false);
                                        }
                                        try {
                                            Session.restore();
                                        } catch (UnknownResellerException uex2) {
                                            Session.getLog().error("Unable to restore user", uex2);
                                        }
                                    } catch (Exception e2) {
                                        getLog().debug("Can't get user for account : #" + a.getId() + " skipping", e2);
                                        if (a != null) {
                                            addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                            a.setLocked(false);
                                        }
                                        try {
                                            Session.restore();
                                        } catch (UnknownResellerException uex3) {
                                            Session.getLog().error("Unable to restore user", uex3);
                                        }
                                    }
                                } else {
                                    getLog().debug("Can't get user for account : #" + accountId.getId() + " skipping");
                                    if (a != null) {
                                        addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                        a.setLocked(false);
                                    }
                                    try {
                                        Session.restore();
                                    } catch (UnknownResellerException uex4) {
                                        Session.getLog().error("Unable to restore user", uex4);
                                    }
                                }
                            } catch (Exception e3) {
                                getLog().debug("Can't get user for account : #" + accountId.getId() + " skipping", e3);
                                if (a != null) {
                                    addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                    a.setLocked(false);
                                }
                                try {
                                    Session.restore();
                                } catch (UnknownResellerException uex5) {
                                    Session.getLog().error("Unable to restore user", uex5);
                                }
                            }
                        } catch (Exception e4) {
                            getLog().debug("Can't get account: #" + accountId.getId() + " skipping", e4);
                            if (a != null) {
                                addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                                a.setLocked(false);
                            }
                            try {
                                Session.restore();
                            } catch (UnknownResellerException uex6) {
                                Session.getLog().error("Unable to restore user", uex6);
                            }
                        }
                    } catch (Exception ex) {
                        Session.getLog().error("Unable to process monthly resources accountId:" + accountId, ex);
                        if (0 != 0) {
                            addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                            a.setLocked(false);
                        }
                        try {
                            Session.restore();
                        } catch (UnknownResellerException uex7) {
                            Session.getLog().error("Unable to restore user", uex7);
                        }
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        addProgress(1, "Billed monthly resources for account#" + a.getId().getId());
                        a.setLocked(false);
                    }
                    try {
                        Session.restore();
                    } catch (UnknownResellerException uex8) {
                        Session.getLog().error("Unable to restore user", uex8);
                    }
                    throw th;
                }
            }
        }
    }

    public static Hashtable generateResourceListByQuery(String queryToBill, Calendar cal) throws SQLException {
        Hashtable accounts = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(queryToBill);
            ps.setDate(1, new Date(cal.getTime().getTime()));
            ResultSet rs = ps.executeQuery();
            long accountId = -1;
            LinkedList resList = new LinkedList();
            while (rs.next()) {
                if (accountId != rs.getLong(3)) {
                    Long key = new Long(rs.getLong(3));
                    resList = (LinkedList) accounts.get(key);
                    if (resList == null) {
                        resList = new LinkedList();
                        accounts.put(key, resList);
                    }
                    accountId = rs.getLong(3);
                }
                ResourceId rid = new ResourceId(rs.getLong(1), rs.getInt(2));
                resList.add(rid);
            }
            Session.closeStatement(ps);
            con.close();
            return accounts;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean chargeMonthlyBilled(Account a, List list) throws Exception {
        getLog().debug("Monthly accounting, check " + a.getId());
        boolean chargedMonthly = false;
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ResourceId rId = (ResourceId) i.next();
            if (rId.isMonthly()) {
                ResourceType rt = a.getPlan().getResourceType(rId.getType());
                if (rId.isDummy() && !rt.hasUsage(a.getPeriodId()) && !rt.hasRecurrent(a.getPeriodId())) {
                    getLog().debug("Skip free resourceType: " + rId.getNamedType());
                } else {
                    try {
                        Resource r = rId.get();
                        if (r.monthlyCharge()) {
                            chargedMonthly = true;
                        }
                    } catch (Exception e) {
                        getLog().info("Monthly processing error ", e);
                        sendBillingException(e);
                    }
                }
            }
        }
        return chargedMonthly;
    }
}
