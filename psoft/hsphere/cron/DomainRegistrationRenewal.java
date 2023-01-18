package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.OpenSRS;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/DomainRegistrationRenewal.class */
public class DomainRegistrationRenewal extends BackgroundJob {
    public DomainRegistrationRenewal(C0004CP cp) throws Exception {
        super(cp, "DOMAIN_REGISTRATION");
    }

    public DomainRegistrationRenewal(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        Connection con = Session.getDb();
        try {
            getLog().info("STARTED");
            long startDate = TimeUtils.currentTimeMillis();
            chargeRegistrarResources();
            long timeDiff = TimeUtils.currentTimeMillis() - startDate;
            getLog().info("DomainRegistrationRenewal FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            Session.closeStatement(null);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    protected void chargeRegistrarResources() throws SQLException, NoSuchTypeException {
        int renewDaysInAdvanceBeforeExpire = 0;
        Calendar cal = TimeUtils.getCalendar();
        cal.add(1, -1);
        try {
            for (Registrar reg : DomainRegistrar.getManager().getEntities()) {
                String renewDaysStr = reg.get("renew_days");
                int renewDays = 0;
                if (renewDaysStr != null && !"".equals(renewDaysStr)) {
                    try {
                        renewDays = Integer.parseInt(renewDaysStr);
                    } catch (NumberFormatException e) {
                    }
                }
                renewDaysInAdvanceBeforeExpire = Math.max(renewDaysInAdvanceBeforeExpire, renewDays);
            }
            cal.add(5, renewDaysInAdvanceBeforeExpire);
        } catch (Exception ex) {
            Session.getLog().warn("Failed to get renew_days ", ex);
        }
        freshStatusMessage("Getting domain registration resources to renew");
        Hashtable accounts = Accounting.generateResourceListByQuery("SELECT child_id, child_type, account_id FROM parent_child p, opensrs o WHERE last_payed <= ? AND o.id = p.child_id ORDER BY account_id", cal);
        if (!accounts.isEmpty()) {
            if (!isProgressInitialized()) {
                try {
                    setProgress(accounts.size(), 1, 0);
                } catch (Exception ex2) {
                    Session.getLog().error("Unable to initialize Progress", ex2);
                }
            }
            Enumeration en = accounts.keys();
            while (en.hasMoreElements()) {
                Long aId = (Long) en.nextElement();
                List resIds = (List) accounts.get(aId);
                if (resIds != null && !resIds.isEmpty()) {
                    ResourceId accountId = new ResourceId(aId.longValue(), 0);
                    Session.save();
                    Account a = null;
                    try {
                        a = (Account) Account.get(accountId);
                        a.setLocked(true);
                        User u = a.getUser();
                        Session.setUser(u);
                        Session.setAccount(a);
                        freshStatusMessage("Charging domain registration \tresources for account #" + a.getId().getId());
                        if (!a.isSuspended()) {
                            Date now = TimeUtils.getDate();
                            a.getBill().charge(a.getBillingInfo());
                            chargeOpenSRS(a, resIds);
                            a.getBill().charge(a.getBillingInfo());
                            a.sendInvoice(now);
                        }
                        addProgress(1);
                        saveStatusData();
                        a.setLocked(false);
                        try {
                            Session.restore();
                        } catch (UnknownResellerException uex) {
                            Session.getLog().error("Unable to restore user", uex);
                        }
                    } catch (Throwable ex3) {
                        try {
                            Session.getLog().error("Unable to process monthly resources accountId:" + accountId, ex3);
                            a.setLocked(false);
                            try {
                                Session.restore();
                            } catch (UnknownResellerException uex2) {
                                Session.getLog().error("Unable to restore user", uex2);
                            }
                        } catch (Throwable th) {
                            a.setLocked(false);
                            try {
                                Session.restore();
                            } catch (UnknownResellerException uex3) {
                                Session.getLog().error("Unable to restore user", uex3);
                            }
                            throw th;
                        }
                    }
                }
            }
        }
    }

    protected void sendWarning(Account a, String msgId) throws Exception {
        try {
            String email = a.getContactInfo().getEmail();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
            try {
                CustomEmailMessage.send(msgId, email, (TemplateModelRoot) root);
            } catch (Throwable se) {
                Session.getLog().warn("Error sending message", se);
            }
        } catch (NullPointerException npe) {
            Session.getLog().warn("NPE", npe);
        } catch (Throwable t) {
            Session.getLog().warn("Error", t);
        }
    }

    protected void chargeOpenSRS(Account a, List list) throws Exception {
        Set domainRegistrationTypeIds = TypeRegistry.getDomainRegistrationIds();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ResourceId rId = (ResourceId) i.next();
            if (domainRegistrationTypeIds.contains(new Integer(rId.getType()))) {
                try {
                    OpenSRS oRes = (OpenSRS) rId.get();
                    if (oRes != null) {
                        oRes.charge();
                    }
                } catch (Exception e) {
                    getLog().info("Error processing opensrs ", e);
                    sendBillingException(e);
                }
            }
        }
    }
}
