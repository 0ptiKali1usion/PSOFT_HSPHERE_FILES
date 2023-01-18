package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResellerPrices;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.SignupManager;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.design.DesignProvider;
import psoft.hsphere.global.Globals;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.SignupResource;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.hsphere.resource.p003ds.ResellerDSIPRangeCounter;
import psoft.hsphere.resource.plan_wizard.PlanWizard;
import psoft.util.FakeRequest;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ResellerResource.class */
public class ResellerResource extends Resource {
    protected Reseller reseller;
    protected String cpAlias;

    public ResellerResource(int type, Collection init) throws Exception {
        super(type, init);
        this.reseller = null;
        Iterator i = init.iterator();
        this.cpAlias = (String) i.next();
    }

    public ResellerResource(ResourceId rid) throws Exception {
        super(rid);
        this.reseller = null;
        this.reseller = Reseller.getReseller(rid);
        if (this.reseller != null) {
            return;
        }
        this.reseller = Reseller.getReseller(Session.getUser().getId());
    }

    protected Plan createPlan(long resellerId) throws Exception {
        PlanWizard pw = null;
        try {
            pw = new PlanWizard();
            pw.setAdminPlanName("Administrative account", resellerId);
            pw.setValue("_PERIOD_TYPES", "1");
            pw.setValue("_PERIOD_TYPE_0", "MONTH");
            pw.setValue("_PERIOD_SIZE_0", "1");
            pw.setValue("menuId", FMACLManager.ADMIN);
            pw.setValue("_template", "admin/plans.html");
            pw.setValue("_TEMPLATES_DIR", "admin/");
            pw.startResources();
            pw.addResource("merchant_manager", "psoft.hsphere.resource.admin.MerchantManager");
            pw.addResource("account_preview", "psoft.hsphere.resource.admin.AccountPreviewResource");
            pw.addResource("daily_report", "psoft.hsphere.resource.admin.DailyReportResource");
            pw.addResource("adnsmanager", "psoft.hsphere.resource.admin.AdmDNSManager");
            pw.addResource("reseller_eeman", "psoft.hsphere.resource.admin.ResellerEnterpriseManager");
            pw.addResource("su", "psoft.hsphere.resource.admin.SUResource");
            pw.addResource("ttadmin", "psoft.hsphere.resource.tt.TroubleTicketAdmin");
            pw.addMod("ttadmin", "");
            pw.addIValue("ttadmin", "", 1, "login", "Name", 0);
            pw.addIValue("ttadmin", "", 1, "description", "Description", 1);
            pw.addIValue("ttadmin", "", 1, "email", "E-mail", 2);
            pw.addResource("signupadm", "psoft.hsphere.resource.SignupResource");
            pw.addResource("billman", "psoft.hsphere.resource.admin.BillingManager");
            pw.addResource("ds_manager", "psoft.hsphere.resource.admin.ds.DSManager");
            pw.addResource("account", "psoft.hsphere.Account");
            pw.addMod("account", "");
            pw.addIResource("account", "", "reportviewer", "", 0);
            pw.addIResource("account", "", FMACLManager.ADMIN, "", 1);
            pw.addIResource("account", "", "ttadmin", "", 2);
            pw.addIResource("account", "", "signupadm", "", 3);
            pw.addIResource("account", "", "billman", "", 4);
            pw.addIResource("account", "", "cmp_groups", "", 5);
            pw.addIResource("account", "", "reseller_eeman", "", 6);
            pw.addIResource("account", "", "adnsmanager", "", 7);
            pw.addIResource("account", "", "ds_manager", "", 8);
            pw.addResource("tt", "psoft.hsphere.resource.tt.TroubleTicket");
            pw.addResource("reportviewer", "psoft.hsphere.resource.admin.ReportManager");
            pw.addResource(FMACLManager.ADMIN, "psoft.hsphere.resource.admin.AccountManager");
            pw.addResource("cmp_groups", "psoft.hsphere.resource.admin.CmpPlanGroup");
            pw.addResource("kanoodlemanager", "psoft.hsphere.resource.admin.KanoodleManager");
            pw.addResource("sitetoolboxmanager", "psoft.hsphere.resource.admin.SiteToolboxManager");
            pw.setValue("ttadmin", "_ASSIGN_TT", "TRUE");
            pw.doneResoures();
            pw.doneIResources();
            return pw.done();
        } catch (Exception ex) {
            Session.getLog().error("Failed to create admin planplan", ex);
            if (pw != null) {
                pw.abort();
            }
            throw ex;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        long resId = Session.getUser().getId();
        this.reseller = Reseller.createReseller(resId, "r" + Long.toString(resId) + this.cpAlias, Session.getAccount().getPlan().getId(), getId().getId());
        String sAllowDSResell = Session.getAccount().getPlan().getResourceValue(3010, "_GLB_DISABLED_ALLOW_DS_SELL");
        String sAllowOwnDS = Session.getAccount().getPlan().getResourceValue(3010, "_GLB_DISABLED_ALLOW_OWN_DS");
        Session.getLog().debug("Inside ResellerResource::initDone plan=" + Session.getAccount().getPlan().getId() + " " + Session.getAccount().getParent().get("description") + " allowDsSell=" + sAllowDSResell + " allowOwnDs=" + sAllowOwnDS);
        String billingMode = Settings.get().getValue("billing_mode");
        int currentPlan = Session.getAccount().getPlan().getId();
        long oldResellerId = Session.getResellerId();
        try {
            Session.setResellerId(this.reseller.getId());
            if (billingMode != null && !"".equals(billingMode)) {
                Settings.get().setValue("billing_mode", billingMode);
            }
            Plan adminPlan = createPlan(resId);
            Session.getLog().debug("Admin Plan:" + adminPlan.getId());
            Session.getLog().debug("Current Plan:" + currentPlan);
            adminPlan.FM_accessChange(Integer.toString(adminPlan.getId()) + "," + Integer.toString(currentPlan) + ",");
            this.reseller.setPlanId(adminPlan.getId());
            Session.setResellerId(oldResellerId);
        } catch (Throwable th) {
            Session.setResellerId(oldResellerId);
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        long oldResellerId = Session.getResellerId();
        if (this.reseller != null) {
            try {
                Session.setResellerId(Session.getUser().getId());
                Plan adminPlan = Plan.getPlan(this.reseller.getPlanId());
                List<AdmDNSZone> szones = AdmDNSZone.getZones();
                for (AdmDNSZone z : szones) {
                    try {
                        z.delete();
                    } catch (Exception ex) {
                        Session.getLog().error("Unable tor delete zone", ex);
                    }
                }
                long ssl_id = -1;
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM reseller_ssl WHERE reseller_id = ?");
                ps.setLong(1, this.reseller.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ssl_id = rs.getLong(1);
                }
                Session.closeStatement(ps);
                con.close();
                if (ssl_id != -1) {
                    AdmResellerSSL tmp = AdmResellerSSL.get(ssl_id);
                    tmp.delete();
                }
                Connection con2 = Session.getDb();
                PreparedStatement ps2 = con2.prepareStatement("DELETE FROM settings WHERE reseller_id = ?");
                ps2.setLong(1, this.reseller.getId());
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con2.close();
                if (adminPlan != null) {
                    adminPlan.delete();
                }
                this.reseller.delete();
                Session.setResellerId(oldResellerId);
            } catch (Throwable th) {
                Session.setResellerId(oldResellerId);
                throw th;
            }
        }
    }

    protected void createAdmin() throws Exception {
        if (getAdmin() != 0 && getAdminAccount() != null) {
            throw new HSUserException("reseller.adminexist");
        }
        String baseDesign = null;
        try {
            baseDesign = Session.getAccount().FM_design().toString();
        } catch (Exception e) {
        }
        if (baseDesign == null || "".equals(baseDesign)) {
            baseDesign = DesignProvider.isValidDesignId("xcpl") ? "xcpl" : "common";
        }
        Session.save();
        try {
            try {
                Session.setResellerId(Session.getUser().getId());
                SignupGuard sg = SignupGuard.get();
                sg.FM_updateFlags(0, 0, 0, 0, 0, 0, 0, 0, 0.0d, 0);
                if (getAdmin() != 0) {
                    Session.addMessage(Localizer.translateMessage("reseller.signup_guard_reset"));
                }
                Session.restore();
            } catch (Throwable th) {
                Session.restore();
                throw th;
            }
        } catch (Exception ex) {
            Session.getLog().debug("Unable to reset signup guard", ex);
            Session.restore();
        }
        HttpServletRequest request = new FakeRequest(Session.getRequest());
        SignupManager.saveRequest(request);
        Session.setRequest(request);
        User user = SignupResource.createUser(Session.getRequest().getParameter("login"), Session.getRequest().getParameter("password"), "Admin account", this.reseller.getPlanId(), "", Session.getUser().getId());
        this.reseller.setAdmin(user.getId());
        boolean settingsSaved = false;
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Session.setUser(user);
            Iterator iter = user.getAccountIds().iterator();
            while (iter.hasNext()) {
                ResourceId accId = (ResourceId) iter.next();
                Account acc = user.getAccount(accId);
                Session.setAccount(acc);
                ResourceId admRId = acc.FM_getChild(FMACLManager.ADMIN);
                if (admRId != null && !settingsSaved) {
                    AccountManager adm = (AccountManager) admRId.get();
                    adm.FM_setSettingsValue(DesignProvider.getDesignPrefix(baseDesign) + "available", "1");
                    adm.FM_setSettingsValue("default_design", baseDesign);
                    settingsSaved = true;
                }
                acc.FM_change_design(baseDesign);
            }
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    public TemplateModel FM_createAdmin() throws Exception {
        Session.getLog().debug("FM_createAdmin");
        createAdmin();
        return this;
    }

    public TemplateModel FM_setURL(String URL, String protocol, String port) throws Exception {
        this.reseller.setURL(URL, protocol, port);
        return this;
    }

    public long getAdmin() {
        long adminId = this.reseller.getAdmin();
        try {
            User.getUser(adminId);
            return adminId;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("plan_id".equals(key)) {
            return new TemplateString(this.reseller.getPlanId());
        }
        if (ErrorDocumentResource.MTYPE_URL.equals(key)) {
            return new TemplateString(this.reseller.getURL());
        }
        if ("protocol".equals(key)) {
            return new TemplateString(this.reseller.getProtocol());
        }
        if ("port".equals(key)) {
            return new TemplateString(this.reseller.getPort());
        }
        if ("admin_id".equals(key)) {
            return new TemplateString(getAdmin());
        }
        if ("adm_password".equals(key)) {
            try {
                User admin = User.getUser(this.reseller.getAdmin());
                return new TemplateString(admin.getPassword());
            } catch (Exception ex) {
                Session.getLog().error("Failed to get admin password", ex);
                return new TemplateString("");
            }
        } else if (FMACLManager.ADMIN.equals(key)) {
            try {
                return User.getUser(this.reseller.getAdmin());
            } catch (Exception ex2) {
                Session.getLog().error("Failed to get admin", ex2);
                return null;
            }
        } else if ("admin_account".equals(key)) {
            try {
                return getAdminAccount();
            } catch (Exception ex3) {
                Session.getLog().error("Failed to get admin", ex3);
                return null;
            }
        } else {
            return super.get(key);
        }
    }

    public TemplateModel FM_getResellerPrices() throws Exception {
        Reseller r = Reseller.getReseller(Session.getUser().getId());
        TemplateList prices = new TemplateList();
        for (String typeId : TypeRegistry.getPricedTypes()) {
            ResellerPrices rPrices = r.getPrices(Integer.parseInt(typeId));
            if (!rPrices.isNull() && Globals.isObjectDisabled(TypeRegistry.getType(typeId), r) == 0) {
                TemplateHash th = new TemplateHash();
                th.put("type", TypeRegistry.getDescription(typeId));
                if ("7108".equals(typeId)) {
                    try {
                        ResellerDSIPRangeCounter rc = ResellerDSIPRangeCounter.getResellerCounter(r.getId());
                        th.put("amount", new TemplateString(rc.getTotalAmount()));
                    } catch (Exception ex) {
                        Session.getLog().error("Unable to get amount for dedicated server IP ranges for reseller " + r.getId(), ex);
                    }
                } else {
                    th.put("amount", new TemplateString(r.getTypeCounter().getValue(Integer.parseInt(typeId))));
                }
                if (!Double.isNaN(rPrices.getSetupPrice())) {
                    th.put("setup", new TemplateString(rPrices.getSetupPrice()));
                }
                if (!Double.isNaN(rPrices.getRecurrentPrice())) {
                    th.put("recurrent", new TemplateString(rPrices.getRecurrentPrice()));
                }
                if (!Double.isNaN(rPrices.getUsagePrice())) {
                    th.put("usage", new TemplateString(rPrices.getUsagePrice()));
                }
                if (!Double.isNaN(rPrices.getRefundPercent())) {
                    th.put("refund", new TemplateString(rPrices.getRefundPercent()));
                }
                if (!Double.isNaN(rPrices.getFreeUnits())) {
                    th.put("free", new TemplateString(rPrices.getFreeUnits()));
                }
                if (!Double.isNaN(rPrices.getMax())) {
                    th.put("max", new TemplateString(rPrices.getMax()));
                }
                prices.add((TemplateModel) th);
            }
        }
        return prices;
    }

    public boolean hasClients() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM users u, user_account ua, accounts a WHERE u.reseller_id = ? AND ua.user_id = u.id AND ua.account_id = a.id");
            ps.setLong(1, this.reseller.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1) > 0;
            }
            throw new Exception("Empty 'select count(*)' result");
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Account getAdminAccount() throws Exception {
        Account admAccount = null;
        Session.getLog().debug("Reseller Admin id = " + this.reseller.getAdmin());
        User admResUser = User.getUser(this.reseller.getAdmin());
        Session.save();
        try {
            try {
                Session.setUser(admResUser);
                Iterator i = admResUser.getAccountIds().iterator();
                while (true) {
                    if (!i.hasNext()) {
                        break;
                    }
                    Account currAcc = admResUser.getAccount((ResourceId) i.next());
                    if (currAcc.getPlan().getId() == Session.getReseller().getPlanId()) {
                        admAccount = currAcc;
                        break;
                    }
                }
                Session.restore();
                return admAccount;
            } catch (Exception e) {
                throw new Exception("Unable to get administrative account");
            }
        } catch (Throwable th) {
            Session.restore();
            throw th;
        }
    }

    public Account repairAdminAccount() throws Exception {
        Session.getLog().debug("Performing reseller admin account recovery procedure");
        Session.save();
        try {
            Account a = getAdminAccount();
            Session.setAccount(a);
            Session.setUser(a.getUser());
            if (a.isSuspended() && !this.reseller.isSuspended()) {
                a.resume();
            }
            Session.restore();
            return null;
        } catch (Throwable th) {
            Session.restore();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        Session.getLog().debug("SUSPENDING RESELLER RESOURCE " + getId() + "isSuspended=" + this.reseller.isSuspended());
        if (!this.reseller.isSuspended()) {
            this.reseller.suspend();
            super.suspend();
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        Session.getLog().debug("RESUMING RESELLER RESOURCE " + getId() + "isSuspended=" + this.reseller.isSuspended());
        if (this.reseller.isSuspended()) {
            this.reseller.resume();
            super.resume();
        }
    }

    public TemplateModel FM_repairAdminAccount() throws Exception {
        return repairAdminAccount();
    }

    public TemplateModel FM_resetURL() throws Exception {
        Session.getLog().debug("Inside FM_resetURL");
        Session.save();
        try {
            Session.setResellerId(1L);
            ResourceType rt = Plan.getPlan(this.reseller.getResellerPlanId()).getResourceType(3010);
            Session.getLog().debug("Got resource type for Reseller resource");
            String alias = rt.getInitModifier("").FM_getInitValue(0).toString();
            Session.getLog().debug("Got the alias " + alias);
            Session.restore();
            String url = "r" + this.reseller.getId() + alias;
            FM_setURL(url, Reseller.getDefaultProtocol(), Reseller.getDefaultPort());
            return this;
        } catch (Throwable th) {
            Session.restore();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public boolean isSuspended() {
        return this.reseller.isSuspended();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public Collection getResellerDedicatedServersUsageInfo() throws Exception {
        Hashtable usageInfo = new Hashtable();
        List<DedicatedServerTemplate> accessibleDSTemplates = new ArrayList();
        try {
            accessibleDSTemplates = DSHolder.getAccessibleDSTemplates();
        } catch (Exception e) {
            Session.getLog().warn("Unable to get list of accessible dstemplates for reseller " + Session.getResellerId());
        }
        Plan p = Session.getAccount().getPlan();
        ResourceType rt = p.getResourceType(3010);
        for (DedicatedServerTemplate dst : accessibleDSTemplates) {
            Hashtable item = new Hashtable();
            if ("1".equals(p.getValue("_GLB_DST_" + dst.getId()))) {
                String setup = rt.getValue("DST_" + dst.getId() + "_SETUP_PRICE_");
                String recurrent = rt.getValue("DST_" + dst.getId() + "_REC_PRICE_");
                item.put("name", dst.getName());
                item.put("is_template", "1");
                item.put("setup", setup == null ? "" : setup);
                item.put("recurrent", recurrent == null ? "" : recurrent);
                item.put("available", Integer.toString(dst.getFreeServers()));
                item.put("in_use", "0");
                usageInfo.put(Long.toString(dst.getId()), item);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT dst.id, count(dst.id) as in_use FROM parent_child pc, accounts a, dedicated_servers ds, ds_templates dst WHERE pc.child_type = ? AND pc.account_id = a.id AND a.reseller_id = ? AND pc.child_id = ds.rid AND ds.reseller_id = ? AND ds.template_id = dst.id GROUP BY dst.id;");
            ps.setLong(1, 7100L);
            ps.setLong(2, Session.getResellerId());
            ps.setLong(3, 1L);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dstId = rs.getString("id");
                String inUse = rs.getString("in_use");
                if (usageInfo.keySet().contains(dstId)) {
                    ((Hashtable) usageInfo.get(dstId)).put("in_use", inUse);
                }
            }
            Session.closeStatement(ps);
            con.close();
            return usageInfo.values();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getResellerDedicatedServersUsageInfo() throws Exception {
        return new TemplateList(getResellerDedicatedServersUsageInfo());
    }
}
