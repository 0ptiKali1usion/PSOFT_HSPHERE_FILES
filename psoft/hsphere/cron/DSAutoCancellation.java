package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.global.Globals;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.p001ds.DedicatedServerState;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.admin.p002ds.DSManager;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/cron/DSAutoCancellation.class */
public class DSAutoCancellation extends BackgroundJob {
    static final String SETTINGS_DSCANCEL_VAR = "ds_cancel_notify";
    static final String AP_DSCANCEL_VAR_PREF = "_notified_dscancel_";

    public DSAutoCancellation(C0004CP cp) throws Exception {
        super(cp, "DS_AUTOCANCEL");
    }

    public DSAutoCancellation(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        Session.setResellerId(1L);
        if (Globals.isObjectDisabled("ds_enable") != 0) {
            Session.getLog().debug("The Dedicated Server facilities disabled for the entire system. Process is finished.");
            return;
        }
        DSManager resDSManager = null;
        Connection con = Session.getDb();
        try {
            Date startDate = TimeUtils.getDate();
            PreparedStatement ps = con.prepareStatement("SELECT ds.id as ds_id, ds.cancellation as cancellation, a.id as account_id FROM dedicated_servers ds LEFT OUTER JOIN (parent_child p_c JOIN accounts a ON (p_c.account_id = a.id AND a.deleted IS null)) ON (ds.rid = p_c.child_id) WHERE ds.cancellation IS NOT null ORDER BY ds.reseller_id, ds.id");
            ResultSet rs = ps.executeQuery();
            if (rs.last()) {
                setProgress(rs.getRow(), 1, 0);
                rs.first();
                while (true) {
                    if (isInterrupted()) {
                        getLog().debug("CRON '" + getDBMark() + "' HAS BEEN INTERRUPTED.");
                        break;
                    }
                    checkSuspended();
                    long dsId = rs.getLong("ds_id");
                    long accountId = rs.getLong("account_id");
                    Timestamp cancellation = rs.getTimestamp("cancellation");
                    if (dsId == 0) {
                        Session.getLog().debug("Detected dedicated server in the database with an incorrect id: [" + rs.getString("ds_id") + "].");
                    } else if (accountId == 0) {
                        DedicatedServer ds = resDSManager.getAccessibleDedicatedServer(dsId);
                        if (ds != null) {
                            ds.discardCancellation();
                            ds.setState(DedicatedServerState.CLEAN_UP);
                        } else {
                            Session.getLog().error("Unable to get the dedicated server object (id #" + dsId + ") to clean up the 'cancellation' field which is incorrectly set for a non-existent account.");
                        }
                    } else {
                        cancelIfSafe(dsId, accountId, cancellation, startDate);
                    }
                    addProgress(1, "Finished processing server #" + dsId);
                    if (!rs.next()) {
                        break;
                    }
                }
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
            getLog().info("Dedicated Server Cancellation Cron FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    protected boolean cancelIfSafe(long dsId, long accountId, Timestamp cancellation, Date currentDate) throws Exception {
        Session.save();
        try {
            Account a = Account.getAccount(accountId);
            User u = a.getUser();
            Session.setUser(u);
            Session.setAccount(a);
            String dcv = Settings.get().getValue(SETTINGS_DSCANCEL_VAR);
            if (dcv != null && !"".equals(dcv)) {
                int days = Integer.parseInt(dcv);
                Calendar estimated = TimeUtils.getCalendar();
                estimated.setTime(currentDate);
                estimated.add(5, days);
                if (estimated.getTime().before(cancellation)) {
                    Session.restore();
                    return false;
                }
                String apVar = AP_DSCANCEL_VAR_PREF + String.valueOf(dsId);
                AccountPreferences ap = a.getPreferences();
                if (!"1".equals(ap.getValue(apVar))) {
                    String email = a.getContactInfo().getEmail();
                    SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
                    root.put("ds", DSHolder.getAcessibleDedicatedServer(dsId));
                    root.put("cancellation", new TemplateString(cancellation));
                    CustomEmailMessage.send("DS_AUTOCANCEL", email, (TemplateModelRoot) root);
                    ap.setValue(apVar, "1");
                    Session.restore();
                    return false;
                }
            }
            if (currentDate.before(cancellation)) {
                Session.restore();
                return false;
            }
            deleteDSResource(dsId, a);
            cleanAPN(dsId, a);
            Session.restore();
            return true;
        } catch (Throwable th) {
            Session.restore();
            throw th;
        }
    }

    private void deleteDSResource(long dsId, Account a) throws Exception {
        DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(dsId);
        if (ds == null) {
            throw new Exception("Dedicated server #" + dsId + " is not accessible for account #" + a.getId().getId());
        }
        long rid = ds.getRid();
        ResourceId rId = new ResourceId(rid, ResourceId.getTypeById(rid));
        DedicatedServerResource dsRes = (DedicatedServerResource) DedicatedServerResource.get(rId);
        if (rId == null) {
            throw new Exception("Cannot get the correspondent resource for account #" + a.getId().get() + " related to the dedicated server #" + dsId);
        }
        Session.getLog().debug("Cancellation of the server #" + ds.getId());
        dsRes.delete(false);
    }

    private void cleanAPN(long dsId, Account a) {
        String apVar = AP_DSCANCEL_VAR_PREF + String.valueOf(dsId);
        try {
            AccountPreferences ap = a.getPreferences();
            if (ap.getValue(apVar) != null) {
                ap.removeValue(apVar);
            }
        } catch (Exception ex) {
            Session.getLog().error("Unable to clean up the account preferences for key '" + apVar + "', account id #" + a.getId(), ex);
        }
    }
}
