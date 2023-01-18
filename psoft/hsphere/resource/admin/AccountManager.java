package psoft.hsphere.resource.admin;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.servlet.ServletRequest;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.admin.CCEncryption;
import psoft.hsphere.admin.ServiceInitializer;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.admin.signupmanager.TemplateSignupRecord;
import psoft.hsphere.async.AsyncManager;
import psoft.hsphere.global.GlobalValueProvider;
import psoft.hsphere.global.Globals;
import psoft.hsphere.global.ResourceDependences;
import psoft.hsphere.plan.wizard.PlanChanger;
import psoft.hsphere.plan.wizard.PlanCreator;
import psoft.hsphere.plan.wizard.PlanEditor;
import psoft.hsphere.plan.wizard.PlanWizardXML;
import psoft.hsphere.resource.plan_wizard.Reseller;
import psoft.hsphere.resource.plan_wizard.ResellerPlanEditor;
import psoft.knowledgebase.KnowledgeBaseManager;
import psoft.util.freemarker.HtmlEncodedHashListScalar;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AccountManager.class */
public class AccountManager extends Resource {
    protected EnterpriseManager simpleEntMan;

    public AccountManager(int type, Collection init) throws Exception {
        super(type, init);
        this.simpleEntMan = new EnterpriseManager();
    }

    public AccountManager(ResourceId rid) throws Exception {
        super(rid);
        this.simpleEntMan = new EnterpriseManager();
    }

    public SignupGuard FM_getSignupGuard() throws Exception {
        return SignupGuard.get();
    }

    public TemplateModel FM_updateService(String name) throws Exception {
        ServiceInitializer.update(name);
        return this;
    }

    public TemplateModel FM_setSettingsLargeValue(String name, String value) throws Exception {
        Settings.get().setLargeValue(name, value);
        return this;
    }

    public TemplateModel FM_setSettingsValue(String name, String value) throws Exception {
        Settings.get().setValue(name, value);
        return this;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AccountManager$ResourceAdder.class */
    class ResourceAdder implements TemplateMethodModel {
        ResourceAdder() {
            AccountManager.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            List l2 = HTMLEncoder.decode(l);
            Account oldA = Session.getAccount();
            try {
                long accountId = Long.parseLong((String) l2.get(0));
                Account a = (Account) Account.get(new ResourceId(accountId, 0));
                Session.setAccount(a);
                String rid = (String) l2.get(1);
                Resource r = Resource.get(new ResourceId(rid));
                ResourceId child = r.addChild((String) l2.get(3), (String) l2.get(4), l2.subList(4, l2.size()));
                Session.setAccount(oldA);
                return child;
            } catch (Exception e) {
                AccountManager.getLog().warn("Error adding new child: " + l2, e);
                TemplateHash result = new TemplateHash();
                result.put("STATUS", "ERROR");
                result.put("msg", "error");
                try {
                    Session.setAccount(oldA);
                } catch (Exception e2) {
                    Session.getLog().error("Failed to set account " + oldA.getId().getId(), e);
                }
                return result;
            }
        }
    }

    public Plan FM_saveResellerPlanChanges() throws Exception {
        return ResellerPlanEditor.savePlanChanges();
    }

    public Plan FM_resellerPlanWizard() throws Exception {
        return Reseller.createPlan();
    }

    public TemplateModel FM_listRecipients() throws Exception {
        return new MapAdapter(MailMan.getMailMan().getRecipients());
    }

    public TemplateModel FM_deleteAllRecipients(int type) throws Exception {
        MailMan.getMailMan().deleteAllRecipients(type);
        return this;
    }

    public TemplateModel FM_deleteRecipient(int type, String email) throws Exception {
        MailMan.getMailMan().deleteRecipient(type, email);
        return this;
    }

    public TemplateModel FM_addRecipient(int type, String email, String name) throws Exception {
        MailMan.getMailMan().addRecipient(type, email, name);
        return this;
    }

    public TemplateModel FM_deleteUserAccount(String username, long aid) throws Exception {
        return FM_deleteUserAccount(username, aid, 2);
    }

    public TemplateModel FM_deleteUserAccount(String username, long aid, int billingAction) throws Exception {
        User u = User.getUser(username);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM user_account WHERE account_id = ?");
            ps2.setLong(1, aid);
            ResultSet rs = ps2.executeQuery();
            int count = rs.next() ? rs.getInt(1) : 0;
            if (count != 0) {
                Account oldAccount = Session.getAccount();
                User oldUser = Session.getUser();
                Account a = u.getAccount(new ResourceId(aid, 0));
                Session.setUser(u);
                Session.setAccount(a);
                if (a.isBlocked()) {
                    throw new HSUserException("content.move_lock_resource");
                }
                if (!a.getPlan().isAccessible(oldAccount.getPlan().getId())) {
                    throw new HSUserException("accountmanager.permissions");
                }
                if (a.getResellerId() != 1) {
                    psoft.hsphere.Reseller r = psoft.hsphere.Reseller.getReseller(a.getResellerId());
                    if (r.getAdmin() == a.getUser().getId()) {
                        ps2 = con.prepareStatement("SELECT count(*) FROM users WHERE reseller_id = ? AND username <> ? ");
                        ps2.setLong(1, a.getResellerId());
                        ps2.setString(2, a.getUser().getLogin());
                        ResultSet rs2 = ps2.executeQuery();
                        int rcount = 0;
                        if (rs2.next()) {
                            rcount = rs2.getInt(1);
                        }
                        if (rcount > 0 && a.getPlan().getId() == r.getPlanId()) {
                            ps2 = con.prepareStatement("SELECT count(*) FROM accounts WHERE plan_id = ? AND id IN (SELECT account_id FROM user_account WHERE user_id = ?)");
                            ps2.setLong(1, r.getPlanId());
                            ps2.setLong(2, a.getUser().getId());
                            ResultSet rs3 = ps2.executeQuery();
                            if (rs3.next()) {
                                rcount = rs3.getInt(1);
                            }
                            if (rcount < 2) {
                                throw new HSUserException("accountmanager.admin_acc_delete");
                            }
                        }
                    }
                }
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
                Session.setAccount(a);
                Session.setUser(u);
                if (a.getPlan().isResourceAvailable(FMACLManager.RESELLER) != null && ((ResellerResource) a.getId().findChild(FMACLManager.RESELLER).get()).hasClients()) {
                    throw new HSUserException("accountmanager.reseller");
                }
                if (a.getId().getId() == 1) {
                    throw new HSUserException("accountmanager.admin");
                }
                synchronized (a) {
                    a.delete(false, billingAction);
                }
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            }
            ps2.close();
            Session.getLog().info("----------->Deleting from user_account");
            ps = con.prepareStatement("DELETE FROM user_account WHERE account_id = ?");
            ps.setLong(1, aid);
            ps.executeUpdate();
            HashSet set = u.getAccountIds(true);
            if (set == null || set.size() == 0) {
                u.delete();
                Session.getLog().debug("User " + u.getLogin() + " has been deleted");
            }
            return this;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("plans".equals(key)) {
            return new TemplateList(Plan.getPlanList(true));
        }
        if ("groups".equals(key)) {
            return Plan.getGroupedPlanTree(new ArrayList(Plan.getPlanList()));
        }
        if ("addResource".equals(key)) {
            return new ResourceAdder();
        }
        if ("system_info".equals(key)) {
            return new SystemInfoByReseller();
        }
        if ("kb_manager".equals(key)) {
            return new KnowledgeBaseManager();
        }
        if ("email_manager".equals(key)) {
            return new EmailManager();
        }
        if ("global_objects".equals(key)) {
            try {
                return new TemplateList(Globals.getAccessor().getAllGlobalObjectNames());
            } catch (Exception ex) {
                Session.getLog().error("Unable to get global resource list.", ex);
                return new TemplateList();
            }
        } else if ("global_sections".equals(key)) {
            try {
                List sections = Globals.getAccessor().getAllSections();
                if (sections != null) {
                    return new TemplateList(sections);
                }
            } catch (Exception ex2) {
                Session.getLog().error("Unable to get the list of visual sections.", ex2);
            }
            return new TemplateList();
        } else if ("async_manager".equals(key)) {
            return AsyncManager.getManager();
        } else {
            return super.get(key);
        }
    }

    public TemplateModel FM_getPlan(int id) throws Exception {
        return Plan.getPlan(id);
    }

    public TemplateModel FM_getGlobalSection(String id) throws Exception {
        return Globals.getAccessor().getSection(id);
    }

    public TemplateModel FM_getGlobalDependences(String key) throws Exception {
        return ResourceDependences.getAccessor().getGlobalsAsTemplateList(key);
    }

    public TemplateModel FM_getGlobalSet(String setName) throws Exception {
        return Globals.getAccessor().getSet(setName);
    }

    public TemplateModel FM_isResourceDisabled(String key) throws Exception {
        int res = Globals.isObjectDisabled(key);
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    public TemplateModel FM_isResourceDisabledPlan(String key, int reselPlanId) throws Exception {
        int res = Globals.isObjectDisabled(key, reselPlanId);
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    public TemplateModel FM_isResourceDisabledByReq(String key) throws Exception {
        int res = Globals.isObjectDisabled(key, (ServletRequest) Session.getRequest());
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    public TemplateModel FM_isSetKeyDisabled(String setName, String key) throws Exception {
        int res = Globals.isSetKeyDisabled(setName, key);
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    public TemplateModel FM_isSetKeyDisabledPlan(String setName, String key, int reselPlanId) throws Exception {
        int res = Globals.isSetKeyDisabled(setName, key, reselPlanId);
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    public TemplateModel FM_getAvailableServerGroups(String strGroupType) throws Exception {
        return new TemplateList(EnterpriseManager.getEnabledLServerGroups(strGroupType));
    }

    public TemplateModel FM_thereAreAvailableGroups(String strGroupType) throws Exception {
        return new TemplateString(EnterpriseManager.getEnabledLServerGroups(strGroupType).isEmpty() ? "" : "1");
    }

    public TemplateModel FM_updateGlobalSettings() throws Exception {
        GlobalValueProvider.updateGlobalSettings(Session.getRequest());
        return this;
    }

    public TemplateModel FM_updateGlobalSettings(String section) throws Exception {
        GlobalValueProvider.updateGlobalSettings(Session.getRequest(), section);
        return this;
    }

    public TemplateModel FM_isCPSSLEnabled(String sslType) throws Exception {
        if (AdmResellerSSL.sslBaseTypes.contains(sslType) && Globals.isObjectDisabled(Plan.CP_SSL_PREFIX + sslType) == 0) {
            return new TemplateString("1");
        }
        return new TemplateString("");
    }

    public TemplateModel FM_isCPSSLEnabled(String sslType, int reselPlanId) throws Exception {
        if (AdmResellerSSL.sslBaseTypes.contains(sslType) && Globals.isObjectDisabled(Plan.CP_SSL_PREFIX + sslType, reselPlanId) == 0) {
            return new TemplateString("1");
        }
        return new TemplateString("");
    }

    public TemplateModel FM_suspendAccount(String username, long aid, String reason) throws Exception {
        if (reason.length() > 1024) {
            throw new HSUserException("accountmanager.suspendreason");
        }
        Session.getLog().debug("AccountManager: Going to suspend account id=" + aid + " for user=" + username);
        User u = User.getUser(username);
        Plan oldPlan = Session.getAccount().getPlan();
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Account a = u.getAccount(new ResourceId(aid, 0));
            Session.setUser(u);
            Session.setAccount(a);
            if (!a.getPlan().isAccessible(oldPlan.getId())) {
                throw new HSUserException("accountmanager.permissions");
            }
            synchronized (a) {
                a.suspend(reason);
            }
            return this;
        } finally {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
        }
    }

    public boolean canBeSuspended(long aid) throws Exception {
        User u = User.getUser(User.getUsername(aid));
        return ("TRUE".equalsIgnoreCase(Session.getPropertyString("EMULATION_MODE")) && (Session.getAccount().getId().getId() == aid || u.getId() == 1)) ? false : true;
    }

    public TemplateModel FM_canBeSuspended(long aid) throws Exception {
        return new TemplateString(canBeSuspended(aid));
    }

    public TemplateModel FM_resumeAccount(String username, long aid) throws Exception {
        User u = User.getUser(username);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        Plan oldPlan = Session.getAccount().getPlan();
        Account a = u.getAccount(new ResourceId(aid, 0));
        if (!a.getPlan().isAccessible(oldPlan.getId())) {
            throw new HSUserException("accountmanager.permissions");
        }
        Session.setAccount(a);
        Session.setUser(u);
        try {
            synchronized (a) {
                a.resume();
            }
            return this;
        } finally {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
        }
    }

    public TemplateModel FM_deletePlan(String planId) throws Exception {
        Plan plan = Plan.getPlan(planId);
        Session.getLog().debug("Inside AccountManager::FM_deletePlan, got plan " + plan.getDescription());
        plan.delete();
        return this;
    }

    public TemplateModel FM_getSignupRecord(long uid) throws Exception {
        Session.getLog().info("Inside signup record --> " + uid);
        return new HtmlEncodedHashListScalar(new TemplateSignupRecord(SignupManager.getRecordByUserId(uid)));
    }

    public TemplateModel FM_resetCredit() throws Exception {
        resetCredit(false);
        return this;
    }

    public static void resetCredit(boolean forAll) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String query = "UPDATE balance_credit SET credit = 0";
        if (!forAll) {
            try {
                query = query + " WHERE id IN (SELECT id FROM accounts WHERE reseller_id = ?)";
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        ps = con.prepareStatement(query);
        if (!forAll) {
            ps.setLong(1, Session.getResellerId());
        }
        ps.executeUpdate();
        Session.closeStatement(ps);
        con.close();
        Session.getLog().debug("Reseting cache after credit limit changing");
        Resource.resetCache();
    }

    public TemplateModel FM_resetSendInvoice(int state) throws Exception {
        resetSendInvoice(state == 1);
        return this;
    }

    public static void resetSendInvoice(boolean state) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET receive_invoice = ? WHERE reseller_id = ?");
            ps.setInt(1, state ? 1 : 0);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            Session.getLog().debug("Reseting cache after send invoice state changing");
            Resource.resetCache();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getAdminPlanId() throws Exception {
        psoft.hsphere.Reseller res = Session.getReseller();
        return new TemplateString(res.getPlanId());
    }

    public TemplateModel FM_generateEncryptionKeys() throws Exception {
        CCEncryption.get().generateKeyPair();
        return this;
    }

    public TemplateModel FM_clearPrivateKey() throws Exception {
        CCEncryption.get().clearPrivateEncryptionKey();
        return this;
    }

    public TemplateModel FM_encryptAllCreditCards() throws Exception {
        CCEncryption.get().setEncryptionOn();
        return this;
    }

    public TemplateModel FM_decryptAllCreditCards() throws Exception {
        CCEncryption.get().setEncryptionOff();
        return this;
    }

    public TemplateModel FM_dropEncryptionCreditCards() throws Exception {
        CCEncryption.get().dropEncryption();
        return this;
    }

    public TemplateModel FM_postPrivateKey(String privateKey) throws Exception {
        CCEncryption.get().setBase64PrivateKey(privateKey);
        CCEncryption.get().validatePrivateKey();
        return this;
    }

    public TemplateModel FM_getPrivateKey() throws Exception {
        return new TemplateString(CCEncryption.get().getBase64PrivateKey());
    }

    public TemplateModel FM_isPrivateKeyLoaded() throws Exception {
        return new TemplateString(CCEncryption.get().isPrivateKeyLoaded());
    }

    public TemplateModel FM_encryptionStatus() throws Exception {
        CCEncryption enc = CCEncryption.get();
        return new TemplateString(Short.toString(enc.getEncryptionStatus()));
    }

    public TemplateModel FM_isPublicKeyLoaded() throws Exception {
        return new TemplateString(CCEncryption.get().isPublicKeyLoaded());
    }

    public TemplateModel FM_getPublicKeyLoadingErrorMessage() throws Exception {
        return new TemplateString(CCEncryption.get().getPublicKeyLoadingErrorMessage());
    }

    public TemplateModel FM_getWizards() throws Exception {
        TemplateModel t = PlanWizardXML.getWizards();
        return t;
    }

    public TemplateModel FM_getPlanChanger(int planId) throws Exception {
        return new PlanChanger(Plan.getPlan(planId));
    }

    public TemplateModel FM_getWizard(String name) throws Exception {
        return PlanWizardXML.getWizard(name);
    }

    public TemplateModel FM_createPlan() throws Exception {
        return PlanCreator.process();
    }

    public TemplateModel FM_savePlan(int planId) throws Exception {
        return PlanEditor.process();
    }

    public TemplateModel FM_saveChangedPlan(ServletRequest rq) throws Exception {
        return PlanEditor.processRequest(rq);
    }

    public TemplateModel FM_isReseller() throws Exception {
        if (Session.getResellerId() == 1) {
            return null;
        }
        return TemplateString.TRUE;
    }

    public TemplateModel FM_getResellerName(long resellerId) throws Exception {
        if (Session.getResellerId() == 1) {
            return new TemplateString(psoft.hsphere.Reseller.getReseller(resellerId).getUser());
        }
        return null;
    }
}
