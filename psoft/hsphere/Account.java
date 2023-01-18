package psoft.hsphere;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.admin.signupmanager.SignupRecord;
import psoft.hsphere.admin.signupmanager.TemplateSignupRecord;
import psoft.hsphere.billing.Service;
import psoft.hsphere.calc.Calc;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostDependentResourceIterator;
import psoft.hsphere.resource.HostDependentTypeList;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.MailHostDependentTypeList;
import psoft.hsphere.resource.UnixUserResource;
import psoft.hsphere.resource.VHostingHostDependentTypeList;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.admin.ContentMoveItem;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.customjob.CustomJobClient;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.resource.email.AntiVirus;
import psoft.hsphere.resource.email.Autoresponder;
import psoft.hsphere.resource.email.MailAlias;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.tools.ToolLogger;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.NameModifier;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/Account.class */
public class Account extends Resource {
    protected Timestamp created;
    protected String description;
    protected int planId;
    protected long resellerId;
    protected TypeCounter typeCounter;
    protected Hashtable triggers;
    protected String suspendReason;
    protected Timestamp suspendDate;
    protected boolean partlySuspended;
    protected boolean showTooltips;
    protected boolean receiveInvoice;
    protected Date lockedTime;
    protected static boolean suspendRefundAll;

    /* renamed from: bi */
    protected BillingInfoObject f21bi;
    protected long biId;

    /* renamed from: ci */
    protected ContactInfoObject f22ci;
    protected long ciId;
    protected boolean isLocked;
    protected boolean isAllowChildMod;
    protected boolean isDemo;
    protected Service service;
    protected Boolean isMoved;
    protected Timestamp deleted;
    protected long signupRecord;

    /* renamed from: cm */
    protected ChildManager f23cm;
    protected boolean beingMoved;
    protected AccountPreferences preferences;
    protected String platformType;
    protected boolean noRefund;
    protected Date balanceExhaustionDate;
    protected int periodId;
    protected Date pEnd;
    protected ActiveBill bill;
    protected Map billCache;
    protected SortedSet actualBill;
    private AccountType accountType;

    public boolean isLocked() {
        if (this.isLocked) {
            try {
                Date now = new Date();
                if (now.getTime() - this.lockedTime.getTime() > 1200000) {
                    this.isLocked = false;
                }
            } catch (Exception ex) {
                Session.getLog().debug("Error", ex);
            }
        }
        return this.isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
        if (locked) {
            this.lockedTime = new Date();
        }
    }

    public boolean isNoRefund() {
        return this.noRefund;
    }

    public TemplateModel FM_setNoRefund(String val) throws Exception {
        if ("1".equals(val)) {
            setNoRefund(true);
        } else {
            setNoRefund(false);
        }
        return this;
    }

    public void setNoRefund(boolean noRefund) {
        String withoutRefund = getPlan().getValue("WITHOUT_REFUND");
        boolean notRefund = (withoutRefund == null || "".equals(withoutRefund)) ? false : true;
        this.noRefund = notRefund && noRefund;
    }

    static {
        try {
            suspendRefundAll = "1".equals(Session.getPropertyString("SUSPEND_REFUND100"));
        } catch (Exception e) {
            suspendRefundAll = false;
        }
    }

    @Override // psoft.hsphere.Resource, psoft.util.LockableCacheEntry
    public boolean locked() {
        return super.locked() || isLocked();
    }

    public Service getService() {
        return this.service;
    }

    public List getServices() throws Exception {
        return this.service.getServices();
    }

    public void addTrigger(int type, ResourceId rId) {
        if (this.triggers == null) {
            this.triggers = new Hashtable();
        }
        this.triggers.put(new Integer(type), rId);
    }

    public void decType(ResourceId resId, double amount) throws Exception {
        ResourceId rId = getTrigger(resId.getType());
        if (rId != null) {
            Resource r = Resource.get(rId);
            Date now = TimeUtils.getDate();
            r.recurrentRefund(now, getPeriodEnd());
            getTypeCounter().dec(resId.getType(), amount);
            r.recurrentCharge(now, getPeriodEnd());
        } else {
            getTypeCounter().dec(resId.getType(), amount);
        }
        if (Session.getResellerId() != 1) {
            Session.getReseller().getTypeCounter().dec(resId, amount);
        }
    }

    public void incType(ResourceId resId, double amount) throws Exception {
        ResourceId rId = getTrigger(resId.getType());
        if (rId != null) {
            Resource r = Resource.get(rId);
            Date now = TimeUtils.getDate();
            r.recurrentRefund(now, getPeriodEnd());
            getTypeCounter().inc(resId.getType(), amount);
            r.recurrentCharge(now, getPeriodEnd());
        } else {
            getTypeCounter().inc(resId.getType(), amount);
        }
        if (Session.getResellerId() != 1) {
            Session.getReseller().getTypeCounter().inc(resId, amount);
        }
    }

    public ResourceId getTrigger(int type) {
        if (this.triggers == null) {
            return null;
        }
        return (ResourceId) this.triggers.get(new Integer(type));
    }

    public TypeCounter getTypeCounter() {
        if (this.typeCounter == null) {
            this.typeCounter = new TypeCounter();
            try {
                _loadTypeCounter();
            } catch (Exception e) {
                Session.getLog().warn("Error loading typeCounter", e);
            }
        }
        return this.typeCounter;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public Account(Plan plan, String description, BillingInfoObject bi, ContactInfoObject ci, String mod, int periodId) throws Exception {
        this.receiveInvoice = false;
        this.lockedTime = null;
        this.f21bi = null;
        this.f22ci = null;
        this.isLocked = false;
        this.isAllowChildMod = true;
        this.isMoved = null;
        this.deleted = null;
        this.signupRecord = -1L;
        this.beingMoved = false;
        this.platformType = null;
        this.noRefund = false;
        this.balanceExhaustionDate = null;
        this.bill = new ActiveBill();
        this.billCache = new Hashtable();
        this.actualBill = new TreeSet();
        this.accountType = null;
        this.f21bi = bi;
        this.f22ci = ci;
        Session.setAccount(this);
        this.f23cm = new ChildManager(this);
        cache.put(getId(), this);
        this.description = description;
        this.periodId = periodId;
        this.resellerId = plan.getResellerId();
        this.showTooltips = true;
        this.isDemo = "1".equals(plan.getValue("_EMULATION_MODE"));
        this.planId = plan.getId();
        this.receiveInvoice = "1".equals(plan.getValue("_SEND_INVOICE"));
        this.created = new Timestamp(TimeUtils.currentTimeMillis());
        this.accountId = getId();
        this.preferences = new AccountPreferences(this.accountId.getId());
        String promoCode = Session.getRequest().getParameter("_promo_code");
        if (promoCode != null && promoCode.length() > 0) {
            this.preferences.setValue("_ACCOUNT_PROMO", promoCode);
        }
        if (Session.getRequest().getParameter("signup_id") == null || Session.getRequest().getParameter("signup_id").length() == 0) {
            throw new Exception("No signup_id available in request");
        }
        if (getBillingInfo().getBillingType() == -1) {
            Invoice inv = Invoice.getInvoice(plan, mod, periodId, Session.getRequest().getParameter("_promo_code"));
            double total = USFormat.parseDouble(inv.get("total").toString());
            if (total > plan.getTrialCredit()) {
                throw new HSUserException("account.cost");
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO accounts (id, created, plan_id, description, bill_id, bi_id, ci_id, period_id, suspended, tooltips, receive_invoice, reseller_id, demo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.f41id.getId());
            ps.setTimestamp(2, this.created);
            ps.setInt(3, this.planId);
            ps.setString(4, description);
            ps.setLong(5, 0L);
            ps.setLong(6, bi.getId());
            ps.setLong(7, ci.getId());
            ps.setInt(8, periodId);
            ps.setNull(9, 93);
            ps.setInt(10, 1);
            ps.setInt(11, this.receiveInvoice ? 1 : 0);
            ps.setLong(12, this.resellerId);
            ps.setLong(13, this.isDemo ? 1L : 0L);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            process(this);
            boolean criticalDone = false;
            HashMap authRes = null;
            boolean skipCharge = getBillingInfo().getBillingType() == 2 || getBillingInfo().getBillingType() == 3;
            try {
                this.isLocked = true;
                this.service = new Service(getId().getId());
                Connection con2 = Session.getTransConnection();
                if (mod == null) {
                    init();
                } else {
                    init(mod);
                }
                PreparedStatement ps2 = con2.prepareStatement("INSERT INTO f_user_account(user_id, account_id, signup_id, request_id)VALUES(?, ?, ?, ?)");
                Session.getLog().debug("Adding F_USER_ACCOUNT entry signupId=" + Session.getRequest().getParameter("signup_id"));
                ps2.setLong(1, Session.getUser().getId());
                ps2.setLong(2, getId().getId());
                ps2.setInt(3, Integer.parseInt(Session.getRequest().getParameter("signup_id")));
                if (Session.getRequest().getParameter("request_record_id") != null) {
                    ps2.setInt(4, Integer.parseInt(Session.getRequest().getParameter("request_record_id")));
                } else {
                    ps2.setInt(4, 0);
                }
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                Session.commitTransConnection(con2);
                Session.getLog().debug("CHECKING FOR THIRD PARTY RESOURCES");
                Iterator i = new ThirdPartyIterator(getId());
                if (i.hasNext()) {
                    Session.getLog().debug("THIRD PARTY RESOURCES FOUND");
                    if (!skipCharge) {
                        Session.getLog().info("Authorizing");
                        authRes = getBill().auth(getBillingInfo());
                        while (i.hasNext()) {
                            ((ThirdPartyResource) i.next()).thirdPartyAction();
                            criticalDone = true;
                        }
                        Session.getLog().info("Post auth charging");
                        try {
                            getBill().capture(getBillingInfo(), authRes);
                        } catch (Exception e) {
                            Ticket.create(e, this, "PostAuth(CC Capture) error after successful third party resource(like OpenSRS) registration");
                            Session.getLog().error("PostAuth error " + e);
                        }
                    } else {
                        while (i.hasNext()) {
                            ((ThirdPartyResource) i.next()).thirdPartyAction();
                            criticalDone = true;
                        }
                    }
                } else if (!skipCharge) {
                    Session.getLog().info("Charging");
                    getBill().charge(getBillingInfo());
                }
                Connection con3 = Session.getDb();
                PreparedStatement ps3 = con3.prepareStatement("DELETE FROM f_user_account WHERE account_id = ?");
                ps3.setLong(1, getId().getId());
                ps3.executeUpdate();
                Session.closeStatement(ps3);
                con3.close();
                initBill(TimeUtils.getDate(), true);
                this.isLocked = false;
            } catch (Throwable e2) {
                try {
                    if (!criticalDone) {
                        getLog().warn("error create", e2);
                        Connection con4 = Session.getTransConnection();
                        try {
                            delete(true, 2);
                            Session.commitTransConnection(con4);
                        } catch (NotInitDoneException e3) {
                            Session.commitTransConnection(con4);
                        }
                        if (authRes != null) {
                            Session.getLog().info("Voiding auth");
                            getBill().void_auth(getBillingInfo(), authRes);
                        }
                        if (e2 instanceof Exception) {
                            throw ((Exception) e2);
                        }
                        getLog().warn("error create-->", e2);
                        throw new Exception("Runtime error:" + e2.getMessage());
                    }
                    Ticket.create(e2, this, "Unable to rollback resource creation process after at least one critical resource(like OpenSRS) has been created");
                    Session.getLog().error("Critical error on third party resource creation " + e2);
                    this.isLocked = false;
                } catch (Throwable th) {
                    this.isLocked = false;
                    throw th;
                }
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    public Account(ResourceId id) throws Exception {
        super(id);
        this.receiveInvoice = false;
        this.lockedTime = null;
        this.f21bi = null;
        this.f22ci = null;
        this.isLocked = false;
        this.isAllowChildMod = true;
        this.isMoved = null;
        this.deleted = null;
        this.signupRecord = -1L;
        this.beingMoved = false;
        this.platformType = null;
        this.noRefund = false;
        this.balanceExhaustionDate = null;
        this.bill = new ActiveBill();
        this.billCache = new Hashtable();
        this.actualBill = new TreeSet();
        this.accountType = null;
        Session.setAccount(this);
        this.f23cm = new ChildManager(this);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT created, plan_id, description, p_end, bill_id, bi_id, ci_id, period_id, suspended, suspend_reason, tooltips, reseller_id, receive_invoice, demo, deleted FROM accounts WHERE id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.created = rs.getTimestamp(1);
                this.planId = rs.getInt(2);
                this.description = rs.getString(3);
                this.pEnd = rs.getDate(4);
                Session.getLog().debug("SUSPENDED=" + rs.getTimestamp(9) + " res suspended:" + this.suspended);
                if (this.suspended != (rs.getTimestamp(9) != null)) {
                    this.partlySuspended = true;
                } else {
                    this.partlySuspended = false;
                }
                this.suspended = rs.getTimestamp(9) != null;
                this.suspendDate = rs.getTimestamp(9);
                this.suspendReason = rs.getString(10);
                this.showTooltips = rs.getInt(11) == 1;
                this.receiveInvoice = rs.getInt(13) == 1;
                try {
                    this.bill.setBill(new Bill(rs.getLong(5)));
                    if (this.bill.closed() != null) {
                        initBill(TimeUtils.getDate(), true);
                    }
                } catch (Exception e) {
                    Session.getLog().warn("loading bill", e);
                    initBill(TimeUtils.getDate(), true);
                }
                this.biId = rs.getLong(6);
                this.ciId = rs.getLong(7);
                this.periodId = rs.getInt(8);
                this.resellerId = rs.getLong(12);
                this.isDemo = 1 == rs.getInt("demo");
                this.deleted = rs.getTimestamp("deleted");
                if (getBillingInfo().getBillingType() == 2) {
                    ps.close();
                    ps = con.prepareStatement("SELECT runout_date FROM balance_runout_date WHERE id = ?");
                    ps.setLong(1, id.getId());
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next()) {
                        setBalanceExhaustionDate(new Date(rs2.getDate(1).getTime()));
                    }
                }
                this.preferences = new AccountPreferences(this.accountId.getId());
                loadTypeCounter();
                this.triggers = new Hashtable();
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("SELECT id, type, rtype FROM type_billing WHERE account_id = ?");
                ps2.setLong(1, id.getId());
                ResultSet rs3 = ps2.executeQuery();
                while (rs3.next()) {
                    addTrigger(rs3.getInt(3), new ResourceId(rs3.getLong(1), rs3.getInt(2)));
                }
                this.service = new Service(getId().getId());
                Session.closeStatement(ps2);
                con.close();
                return;
            }
            throw new SQLException("no such account:" + id);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        this.service.deleteServices();
        Date now = TimeUtils.getDate();
        int failed = now.getTime() - this.created.getTime() < 600000 ? 1 : 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                Timestamp t = new Timestamp(now.getTime());
                PreparedStatement ps2 = con.prepareStatement("UPDATE accounts SET deleted = ?, failed = ? WHERE id = ?");
                ps2.setTimestamp(1, t);
                ps2.setInt(2, failed);
                ps2.setLong(3, getId().getId());
                ps2.executeUpdate();
                setDeleted(t);
                ps = con.prepareStatement("DELETE FROM f_user_account WHERE account_id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().error("Error deleting an account", e);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void loadTypeCounter() throws Exception {
        this.typeCounter = null;
    }

    public void _loadTypeCounter() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.getLog().debug("fill-in TypeCounter");
            synchronized (this.typeCounter) {
                this.typeCounter.clear();
                ps = con.prepareStatement("SELECT child_type, sum(amount) FROM parent_child, resource_amount WHERE account_id = ? AND child_id = id GROUP BY child_type");
                ps.setLong(1, this.f41id.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    this.typeCounter.setValue(rs.getInt(1), rs.getDouble(2));
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setPlan(Plan p) {
        this.planId = p.getId();
    }

    public Plan getPlan() {
        try {
            return Plan.getPlan(this.planId);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Error getting Plan:", ex);
            return null;
        }
    }

    public TemplateModel FM_getPrices(String type) throws Exception {
        double amount;
        double amount2;
        double amount3;
        Map result = new HashMap();
        int typeId = Integer.parseInt(TypeRegistry.getTypeId(type));
        ResourceType rt = getPlan().getResourceType(typeId);
        if (rt == null) {
            return new MapAdapter(result);
        }
        boolean isBillable = false;
        double amount4 = Calc.getPrice(getPlan(), rt, getPeriodId(), "SETUP");
        if (Double.isNaN(amount4)) {
            amount4 = Calc.getDefaultPrice(getPlan(), rt, getPeriodId(), "SETUP");
        }
        if (amount4 > 1.0E-4d) {
            isBillable = true;
            result.put("setup", USFormat.format(amount4));
        }
        double amount5 = Calc.getPrice(getPlan(), rt, getPeriodId(), "UNIT");
        if (Double.isNaN(amount5)) {
            amount5 = Calc.getDefaultPrice(getPlan(), rt, getPeriodId(), "UNIT") * Calc.getMultiplier();
        }
        if (amount5 > 1.0E-4d) {
            isBillable = true;
            result.put("recurrent", USFormat.format(amount5));
        }
        double amount6 = Calc.getPrice(getPlan(), rt, getPeriodId(), "USAGE");
        if (Double.isNaN(amount6)) {
            amount6 = Calc.getDefaultPrice(getPlan(), rt, getPeriodId(), "USAGE") * Calc.getMultiplier();
        }
        if (amount6 > 1.0E-4d) {
            isBillable = true;
            result.put("usage", USFormat.format(amount6));
        }
        String per = rt.getValue("_REFUND_PRICE_" + getPeriodId());
        if (per != null) {
            amount = USFormat.parseDouble(per);
        } else {
            String per2 = rt.getValue("_REFUND_PRICE_");
            if (per2 == null) {
                amount = 100.0d;
            } else {
                amount = USFormat.parseDouble(per2);
            }
        }
        result.put("refund", USFormat.format(amount));
        try {
            String maxNumberS = rt.getValue("_MAX");
            amount2 = USFormat.parseDouble(maxNumberS);
        } catch (Exception e) {
            amount2 = -1.0d;
        }
        if (amount2 != -1.0d) {
            isBillable = true;
            result.put("max", USFormat.format(amount2));
        }
        String freeNumberS = rt.getValue("_FREE_UNITS_" + getPeriodId());
        if (freeNumberS == null) {
            freeNumberS = rt.getValue("_FREE_UNITS_");
        }
        if (freeNumberS == null) {
            amount3 = 0.0d;
        } else {
            try {
                amount3 = USFormat.parseDouble(freeNumberS);
            } catch (ParseException e2) {
                amount3 = 0.0d;
            }
        }
        if (amount3 > 1.0E-4d) {
            result.put("free", USFormat.format(amount3));
        }
        result.put("amount", USFormat.format(getTypeCounter().getValue(typeId)));
        result.put("billable", isBillable ? "1" : "0");
        return new MapAdapter(result);
    }

    public TemplateModel FM_getAmountByType(String type) throws Exception {
        return new TemplateString(getTypeCounter().getValue(Integer.parseInt(TypeRegistry.getTypeId(type))));
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("description".equals(key)) {
            return new TemplateString(this.description + this.f41id.getId());
        }
        if (key.equals("periodId")) {
            return new TemplateString(this.periodId);
        }
        if (key.equals("bi")) {
            return getBillingInfo();
        }
        if (key.equals("ci")) {
            return getContactInfo();
        }
        if (key.equals("changeBI")) {
            return new BIChanger();
        }
        if (key.equals("changePI")) {
            return new PIChanger();
        }
        if (key.equals("plan")) {
            return getPlan();
        }
        if (key.equals("planId")) {
            return new TemplateString(this.planId);
        }
        if (key.equals("bill")) {
            return getBill();
        }
        if (key.equals("tooltips")) {
            return new TemplateString(this.showTooltips);
        }
        if (key.equals("receive_invoice")) {
            return new TemplateString(this.receiveInvoice);
        }
        if (key.equals("suspend_reason")) {
            return new TemplateString(this.suspendReason);
        }
        if (key.equals("password")) {
            return new TemplateString(getUser().getPassword());
        }
        if (key.equals("login")) {
            return new TemplateString(getUser().getLogin());
        }
        if (key.equals("trial_time_left")) {
            return new TemplateString(getTrialDayLeftTill());
        }
        if (key.equals("p_end")) {
            return new TemplateString(DateFormat.getDateInstance(2).format(getPeriodEnd()));
        }
        if (key.equals("exhaustion_date")) {
            return new TemplateString(DateFormat.getDateInstance(2).format(getBalanceExhaustionDate()));
        }
        if (key.equals("isAllowChildMod")) {
            return new TemplateString(this.isAllowChildMod);
        }
        if (key.equals("created")) {
            return new TemplateString(new Date(getCreated().getTime()).toString());
        }
        if (key.equals("isDemo")) {
            return new TemplateString(isDemoAccount());
        }
        if (key.equals("services")) {
            try {
                return new TemplateList(getServices());
            } catch (Exception e) {
                return null;
            }
        } else if (key.equals("customjobs")) {
            return new CustomJobClient();
        } else {
            if (key.equals("preferences")) {
                return this.preferences;
            }
            if (key.equals("jobManager")) {
                try {
                    if (getAccountType().isAdmin() && Session.getResellerId() == 1) {
                        return C0004CP.jobManager;
                    }
                    throw new Exception("Account type must be an admin one (jobManager access)");
                } catch (Exception e2) {
                    Session.getLog().error("Account.get error", e2);
                }
            }
            if ("signup_record".equals(key)) {
                try {
                    SignupRecord sr = getSignupRecord();
                    if (sr != null) {
                        return new TemplateSignupRecord(sr);
                    }
                } catch (Exception e3) {
                    Session.getLog().error("Account.get error", e3);
                }
            }
            return super.get(key);
        }
    }

    public ResourceId FM_getResource(String id) {
        Session.getLog().info("Account: Get rId=" + id);
        return new ResourceId(id);
    }

    @Override // psoft.hsphere.Resource
    public void billingInit() throws Exception {
        setPeriodBegin(TimeUtils.dropMinutes(TimeUtils.getDate()));
        initBill(getPeriodBegin());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET bill_id = ? WHERE id = ?");
            ps.setLong(1, this.bill.getId());
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            setupCharge(getPeriodBegin());
            recurrentCharge(getPeriodBegin(), getAccount().getPeriodEnd());
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getPlanValue(String key) {
        return getPlan().getValue(getId().getType(), key);
    }

    @Override // psoft.hsphere.Resource
    public String getResourcePlanValue(String key) {
        return getPlan().getResourceType(getId().getType()).getValue(key);
    }

    public ResourceType getResourceType(int typeId) {
        return getPlan().getResourceType(typeId);
    }

    public static Resource get(ResourceId id) throws Exception {
        Resource r;
        try {
            synchronized (id.getLock()) {
                r = (Resource) cache.get(id);
                if (r == null) {
                    r = new Account(id);
                    cache.put(r.getId(), r);
                }
            }
            return r;
        } finally {
            id.releaseLock();
        }
    }

    public ChildManager getChildManager() {
        return this.f23cm;
    }

    public int getPeriodId() {
        return this.periodId;
    }

    public long getPeriodSize() {
        Session.getLog().info("In getPeriodSize");
        return getPeriodEnd().getTime() - getPeriodBegin().getTime();
    }

    public Bill getBill() {
        return this.bill;
    }

    public synchronized Bill getBill(long id) throws Exception {
        if (getBill().getId() == id) {
            return getBill();
        }
        Bill b = (Bill) this.billCache.get(new Long(id));
        if (b != null) {
            Session.getLog().debug("Get cached bill " + id);
            return b;
        }
        Bill b2 = new Bill(id);
        this.billCache.put(new Long(id), b2);
        return b2;
    }

    public Iterator getBills() throws Exception {
        List bills = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, opened FROM bill WHERE account_id = ? ORDER BY opened, id");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Bill b = getBill(rs.getLong(1));
                bills.add(b);
            }
            Session.closeStatement(ps);
            con.close();
            return bills.iterator();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Iterator getActualBills() throws Exception {
        synchronized (this.actualBill) {
            if (this.actualBill.contains(getBill())) {
                Session.getLog().debug("Get actual bills from cache");
                return new TreeSet(this.actualBill).iterator();
            }
            this.actualBill.clear();
            Connection con = Session.getDb();
            this.actualBill.add(getBill());
            PreparedStatement ps = con.prepareStatement("SELECT id, opened FROM bill WHERE account_id = ? AND (opened >= ? OR (closed IS NULL OR closed >= ?)) AND id <> ?");
            ps.setLong(1, getId().getId());
            ps.setTimestamp(2, new Timestamp(TimeUtils.dropMinutes(getPeriodBegin()).getTime()));
            ps.setTimestamp(3, new Timestamp(TimeUtils.dropMinutes(getPeriodBegin()).getTime()));
            ps.setLong(4, getBill().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session.getLog().info("getting bills, " + rs.getLong(1));
                Bill b = getBill(rs.getLong(1));
                this.actualBill.add(b);
            }
            Session.closeStatement(ps);
            con.close();
            return new TreeSet(this.actualBill).iterator();
        }
    }

    public Date nextPaymentDate() {
        return nextPaymentDate(getPeriodBegin());
    }

    public Date nextPaymentDate(Date now) {
        return getPlan().getNextPaymentDate(now, this.periodId);
    }

    public void closeBillingPeriod(Date now, boolean refundAll, boolean setPBegin) throws Exception {
        closeBillingPeriod(now, refundAll, false, null, setPBegin);
    }

    protected void closeBillingPeriod(Date now, boolean refundAll, Collection resIds, boolean setPEnd) throws Exception {
        closeBillingPeriod(now, refundAll, false, resIds, setPEnd);
    }

    protected void closeBillingPeriod(Date now, boolean refundAll, boolean skipBilling, boolean setPEnd) throws Exception {
        closeBillingPeriod(now, refundAll, skipBilling, null, setPEnd);
    }

    protected void closeBillingPeriod(Date now, boolean refundAll, boolean skipBilling, Collection resIds, boolean setPEnd) throws Exception {
        try {
            Iterator i = resIds == null ? getChildManager().getAllResources().iterator() : resIds.iterator();
            boolean skipBilling2 = skipBilling || !setPEnd;
            if (setPEnd) {
                while (i.hasNext()) {
                    try {
                        ResourceId rId = (ResourceId) i.next();
                        if (rId.getType() != 0) {
                            Resource r = null;
                            if (!skipBilling2) {
                                if (rId.isMonthly()) {
                                    r = rId.get();
                                    Session.getLog().debug("In Account.closeBillingPeriod(). Monthly action for montly resource performs");
                                    Session.getLog().debug("Closing billing periond: performing monthly action for " + r.getDescription() + " " + r.getId().toString());
                                    if (!rId.isDummy()) {
                                        r.monthlyUsageCharge(now);
                                    }
                                    r.closePeriodMonthlyAction(now);
                                } else {
                                    if (getResourceType(rId.getType()).hasUsage(this.periodId)) {
                                        r = rId.get();
                                        getLog().debug("Calc usage for " + r.toString());
                                        r.usageCharge(now);
                                    }
                                    if (!isNoRefund() && now.before(getPeriodEnd())) {
                                        if (r == null) {
                                            r = rId.get();
                                        }
                                        if (refundAll) {
                                            r.recurrentRefundAll(now, getPeriodEnd());
                                        } else {
                                            r.recurrentRefund(now, getPeriodEnd());
                                        }
                                    }
                                }
                            }
                            if (r != null) {
                                r.setPeriodBegin(now);
                                getTypeCounter().dec(r.getId().getType(), r.getAmount());
                            } else {
                                rId.softSetPeriodBegin(now);
                            }
                        }
                    } catch (Exception e) {
                        getLog().error("Error getting resource", e);
                    }
                }
            }
            if (!skipBilling2 && getResourceType().hasUsage(this.periodId)) {
                getLog().debug("Calc usage for Account itself");
                usageCharge(now);
            }
            if (!isNoRefund() && !skipBilling2 && now != getPeriodEnd()) {
                if (refundAll) {
                    recurrentRefundAll(now, getPeriodEnd());
                } else {
                    recurrentRefund(now, getPeriodEnd());
                }
            }
            Session.getLog().debug("DATE BEGIN INVOICE:" + getPeriodBegin());
            if (setPEnd) {
                setPeriodBegin(now);
                setPeriodEnd(now);
            }
        } finally {
            loadTypeCounter();
        }
    }

    public void setNewPeriodBegin(Date newPeriodBegin) throws Exception {
        Session.setBillingNote(Localizer.translateMessage("accountmanager.set_new_period_begin_note", new Object[]{f42df.format(newPeriodBegin)}));
        Connection con = Session.getTransConnection();
        try {
            initBill(TimeUtils.getDate(), true);
            closeBillingPeriod(getPeriodBegin(), true, true);
            openBillingPeriod(newPeriodBegin, true);
            Session.setBillingNote("");
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.setBillingNote("");
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public void openBillingPeriod(Date now, boolean setPEnd) throws Exception {
        openBillingPeriod(now, null, setPEnd);
    }

    /* JADX WARN: Removed duplicated region for block: B:122:0x0070 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:76:0x001e  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0024 A[Catch: all -> 0x0163, TryCatch #0 {all -> 0x0163, blocks: (B:70:0x0004, B:74:0x0014, B:79:0x0024, B:81:0x003f, B:85:0x004f, B:88:0x0066, B:90:0x0070, B:93:0x0087, B:95:0x008f, B:97:0x00a1, B:99:0x00aa, B:101:0x00c4, B:102:0x00ca, B:106:0x00e3, B:108:0x00f6, B:109:0x0143, B:112:0x014e, B:86:0x005e), top: B:120:0x0004, inners: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x004f A[Catch: all -> 0x0163, TryCatch #0 {all -> 0x0163, blocks: (B:70:0x0004, B:74:0x0014, B:79:0x0024, B:81:0x003f, B:85:0x004f, B:88:0x0066, B:90:0x0070, B:93:0x0087, B:95:0x008f, B:97:0x00a1, B:99:0x00aa, B:101:0x00c4, B:102:0x00ca, B:106:0x00e3, B:108:0x00f6, B:109:0x0143, B:112:0x014e, B:86:0x005e), top: B:120:0x0004, inners: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:86:0x005e A[Catch: all -> 0x0163, TryCatch #0 {all -> 0x0163, blocks: (B:70:0x0004, B:74:0x0014, B:79:0x0024, B:81:0x003f, B:85:0x004f, B:88:0x0066, B:90:0x0070, B:93:0x0087, B:95:0x008f, B:97:0x00a1, B:99:0x00aa, B:101:0x00c4, B:102:0x00ca, B:106:0x00e3, B:108:0x00f6, B:109:0x0143, B:112:0x014e, B:86:0x005e), top: B:120:0x0004, inners: #1 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void openBillingPeriod(java.util.Date r6, java.util.Collection r7, boolean r8) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 365
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.Account.openBillingPeriod(java.util.Date, java.util.Collection, boolean):void");
    }

    public synchronized boolean sendInvoice(Date fromDate) throws Exception {
        String email;
        if (!this.receiveInvoice) {
            return true;
        }
        List<BillEntry> entries = new ArrayList();
        Hashtable taxes = new Hashtable();
        double total = 0.0d;
        double taxTotal = 0.0d;
        int orderId = 0;
        for (BillEntry ent : getBill().getEntries()) {
            int lastOrderId = ent.getOrderId();
            if (lastOrderId > orderId) {
                orderId = lastOrderId;
            }
            if (ent.getOpened().getTime() >= fromDate.getTime() && ent.getAmount() != 0.0d && ent.getOrderId() == 0) {
                entries.add(ent);
                if (!ent.isCharge()) {
                    total += ent.getAmount();
                }
                for (TaxBillEntry tx : ent.getTaxes()) {
                    Double curTax = (Double) taxes.get(String.valueOf(tx.getType()));
                    if (curTax == null) {
                        taxes.put(String.valueOf(tx.getType()), new Double(tx.getAmount()));
                    } else {
                        taxes.put(String.valueOf(tx.getType()), new Double(curTax.doubleValue() + tx.getAmount()));
                    }
                    if (!ent.isCharge()) {
                        taxTotal += tx.getAmount();
                    }
                }
            }
        }
        if (entries.size() == 0) {
            return false;
        }
        int orderId2 = orderId + 1;
        try {
            Session.getLog().debug("Invoice EMAIL");
            if (getBillingInfo().getEmail() != null) {
                email = getBillingInfo().getEmail();
            } else {
                email = getContactInfo().getEmail();
            }
            Session.getLog().debug("Email:" + email + " BIEmail:" + getBillingInfo().getEmail() + " CIEmail:" + getContactInfo().getEmail());
            if (email != null) {
                SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                root.put("entries", new TemplateList(entries));
                root.put("order_id", new TemplateString(String.valueOf(getId().getId()) + "-" + String.valueOf(getBill().getId()) + "-" + String.valueOf(orderId2)));
                root.put("taxes", new TemplateHash(taxes));
                root.put("subtotal", USFormat.format(total));
                root.put("total", USFormat.format(total + taxTotal));
                Reseller tmpRes = Reseller.getReseller(Session.getResellerId());
                String resellerURL = tmpRes.getProtocol() + tmpRes.getURL() + ":" + tmpRes.getPort();
                root.put("reseller_url", new TemplateString(resellerURL));
                CustomEmailMessage.send("INVOICE", email, (TemplateModelRoot) root);
            }
            for (BillEntry billEntry : entries) {
                billEntry.setOrderId(orderId2);
            }
            return true;
        } catch (Throwable e) {
            Session.getLog().warn("Error Sending Email: ", e);
            return true;
        }
    }

    public synchronized void sendNoChargeNotification() throws Exception {
        String email;
        if (!this.receiveInvoice) {
            return;
        }
        try {
            Session.getLog().debug("No Charge Due Notification EMAIL");
            if (getBillingInfo().getEmail() != null) {
                email = getBillingInfo().getEmail();
            } else {
                email = getContactInfo().getEmail();
            }
            Session.getLog().debug("Email:" + email + " BIEmail:" + getBillingInfo().getEmail() + " CIEmail:" + getContactInfo().getEmail());
            if (email != null) {
                DateFormat dateFormat = DateFormat.getDateInstance();
                SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                root.put("current_date", new TemplateString(dateFormat.format(TimeUtils.getDate())));
                root.put("bill", this.bill);
                if (isAnniversaryBillingType()) {
                    CustomEmailMessage.send("NO_CHARGES_ANNIVERSARY", email, (TemplateModelRoot) root);
                } else {
                    CustomEmailMessage.send("NO_CHARGES_CREDIT_LIMIT", email, (TemplateModelRoot) root);
                }
            }
        } catch (Throwable e) {
            Session.getLog().warn("Error Sending Email: ", e);
        }
    }

    public TemplateModel FM_estimateOpenPeriod(int planId, int periodId) throws Exception {
        return estimateOpenPeriod(Plan.getPlan(planId), periodId);
    }

    public Invoice estimateOpenPeriod(Plan plan, int periodId) throws Exception {
        return estimateOpenPeriod(plan, periodId, null);
    }

    public Invoice estimateOpenPeriod(Plan plan, int periodId, List resIds) throws Exception {
        double total = 0.0d;
        TypeCounter testTC = new TypeCounter(getTypeCounter());
        Date now = TimeUtils.getDate();
        Date nextDate = getPlan().getNextPaymentDate(now, periodId);
        List result = new LinkedList();
        Iterator i = resIds == null ? getChildManager().getAllResources().iterator() : resIds.iterator();
        while (i.hasNext()) {
            ResourceId r = (ResourceId) i.next();
            if (r.get() == null) {
                Session.getLog().warn("Cant get resource, skipping");
            } else {
                InitToken token = new InitToken(plan, periodId, testTC);
                token.setRange(now, nextDate);
                Resource res = r.get();
                String recurrentCalc = "";
                if (res != null) {
                    token.set(r.getType(), res, res.getCurrentInitValues());
                    recurrentCalc = plan.getValue(r.getType(), "_RECURRENT_CALC");
                }
                if (recurrentCalc != null && !"".equals(recurrentCalc)) {
                    double recurr = Resource.calc(recurrentCalc, token);
                    String description = token.getRecurrentChangeDescripton(now, nextDate);
                    total += recurr;
                    if (recurr > 0.0d) {
                        StringBuffer discountComment = new StringBuffer();
                        double discount = calculatePromoDiscount(3, recurr, discountComment, r.getType());
                        result.add(new InvoiceEntry(2, description + discountComment.toString(), recurr - discount, r));
                    }
                }
                testTC.dec(r.getType(), token.getAmount());
            }
        }
        return new Invoice(result, fix(total));
    }

    public Date estimateBalanceExhaustionDate() throws Exception {
        Session.getLog().debug("Balance for acc is " + getBill().getBalance());
        Date runOutDay = TimeUtils.getDate();
        if (getBill().getBalance() > 0.0d) {
            Invoice res = estimateOpenPeriod(getPlan(), getPeriodId());
            double amount = res.getTotal();
            Session.getLog().debug("Total is " + res.getTotal());
            if (amount > 0.0d) {
                int billPeriodAmount = (int) Math.floor(getBill().getBalance() / amount);
                Session.getLog().debug("PeriodEnd " + getPeriodEnd() + "billPeriodAmount is " + billPeriodAmount + " multiplier is " + Calc.getMultiplier(getPlan(), getPeriodId()));
                runOutDay = new Date(getPeriodEnd().getTime() + ((long) (((((billPeriodAmount * Calc.getMultiplier(getPlan(), getPeriodId())) * 86400.0d) * 1000.0d) * 365.0d) / 12.0d)));
                Calendar runOutDayCal = TimeUtils.getCalendar(runOutDay);
                if (runOutDayCal.get(1) > 9999) {
                    runOutDayCal.set(1, 9999);
                    runOutDay = runOutDayCal.getTime();
                }
                Session.getLog().debug("For account #" + getId() + " money left to " + runOutDay);
            } else {
                return null;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO balance_runout_date (id , runout_date) VALUES(?, ?)");
            ps.setLong(1, getId().getId());
            ps.setDate(2, new java.sql.Date(runOutDay.getTime()));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            setBalanceExhaustionDate(runOutDay);
            return runOutDay;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getPeriodInWords() {
        return getPlan().getPeriodInWords(getPeriodId());
    }

    @Override // psoft.hsphere.Resource
    public String getMonthPeriodInWords() {
        return getPlan().getMonthPeriodInWords();
    }

    public TemplateModel FM_getPeriodAsString(int pId) throws Exception {
        return new TemplateString(getPeriodAsString(pId));
    }

    protected String getPeriodAsString(int pId) throws Exception {
        return getPlan().getValue("_PERIOD_SIZE_" + pId) + " " + getPlan().getValue("_PERIOD_TYPE_" + pId);
    }

    public TemplateModel FM_estimateChangePaymentPeriod(int periodId) throws Exception {
        return FM_estimateChangePlan(getPlan().getId(), periodId);
    }

    public TemplateModel FM_changePaymentPeriod(int periodId) throws Exception {
        changePaymentPeriod(periodId);
        return this;
    }

    protected void changePaymentPeriod(int newPeriodId) throws Exception {
        changePlan(getPlan().getId(), newPeriodId);
    }

    public TemplateModel FM_compatiblePlans() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT group_id FROM cmp_plan_group WHERE plan_id = ?");
            ps2.setInt(1, this.planId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                int groupId = rs.getInt(1);
                ps2.close();
                ps = con.prepareStatement("SELECT plan_id FROM cmp_plan_group WHERE group_id = ?");
                ps.setInt(1, groupId);
                ResultSet rs2 = ps.executeQuery();
                TemplateList l = new TemplateList();
                String oldPlanCreated = getPlan().getValue("_CREATED_BY_");
                if (oldPlanCreated == null || "".equals(oldPlanCreated)) {
                    oldPlanCreated = FMACLManager.ADMIN;
                }
                while (rs2.next()) {
                    int tmp_plan_id = rs2.getInt(1);
                    if (this.planId != tmp_plan_id) {
                        String newPlanCreated = Plan.getPlan(tmp_plan_id).getValue("_CREATED_BY_");
                        if (newPlanCreated == null || "".equals(newPlanCreated)) {
                            newPlanCreated = FMACLManager.ADMIN;
                        }
                        if (oldPlanCreated.equals(newPlanCreated)) {
                            l.add(rs2.getString(1));
                        }
                    }
                }
                return l;
            }
            TemplateList templateList = new TemplateList();
            Session.closeStatement(ps2);
            con.close();
            return templateList;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_getPlan(String id) {
        try {
            return Plan.getPlan(id);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Error getting Plan:", ex);
            return null;
        }
    }

    public TemplateModel FM_changePlan(int planId, int periodId) throws Exception {
        changePlan(planId, periodId);
        return this;
    }

    public TemplateModel FM_estimateChangePlan(int planId, int periodId) throws Exception {
        Plan p = Plan.getPlan(planId);
        Hashtable result = estimateChangePlan(p, periodId, false);
        return new TemplateHash(result);
    }

    public Hashtable estimateChangePlan(Plan p, int periodId, boolean force) throws Exception {
        Date start;
        Date refundStart;
        Date end;
        Date refundEnd;
        double discountTotal = 0.0d;
        Hashtable result = new Hashtable();
        List entries = new ArrayList();
        getLog().debug("Estimating Changing Plan for " + getId());
        int newPlanId = p.getId();
        Plan currentPlan = getPlan();
        if (currentPlan.getId() != newPlanId) {
            if (!force && !p.isAccessible(this.planId)) {
                throw new HSUserException("account.planaccess");
            }
            String oldCreated = getPlan().getValue("_CREATED_BY_");
            String newCreated = p.getValue("_CREATED_BY_");
            if (oldCreated != null && newCreated != null && !oldCreated.equals(newCreated)) {
                throw new HSUserException("account.incompatible_created_by");
            }
            if (periodId == getPeriodId()) {
                Session.setBillingNote(Localizer.translateMessage("accountmanager.change_plan", new Object[]{currentPlan.getDescription(), p.getDescription()}));
            } else {
                Session.setBillingNote(Localizer.translateMessage("accountmanager.change_plan_period", new Object[]{currentPlan.getDescription(), getPeriodAsString(getPeriodId()), p.getDescription(), p.getPeriodAsString(periodId)}));
            }
        } else if (periodId == getPeriodId()) {
            throw new HSUserException("account.changing_to_same_plan");
        } else {
            Session.setBillingNote(Localizer.translateMessage("accountmanager.change_payment_period", new Object[]{getPeriodAsString(getPeriodId()), getPeriodAsString(periodId)}));
        }
        Date now = TimeUtils.getDate();
        double total = 0.0d;
        InitToken baseToken = getTokenByPlanPeriod(p, periodId);
        for (ResourceId rid : getChildManager().getAllResources()) {
            int type = rid.getType();
            Resource r = rid.get();
            if (r != null) {
                try {
                    InitToken token = new InitToken(baseToken);
                    InitToken refundToken = new InitToken(this, baseToken.getTypeCounter());
                    if (r.getId().isMonthly()) {
                        start = new Date(Math.max(baseToken.getStartDate().getTime(), getPeriodBegin().getTime()));
                        refundStart = r.getPeriodBegin();
                        end = nextMonthlyBillingDate(start);
                        refundEnd = nextMonthlyBillingDate(refundStart);
                    } else {
                        start = now;
                        refundStart = now;
                        end = baseToken.getEndDate();
                        refundEnd = getPeriodEnd();
                    }
                    token.setRange(start, end, baseToken.getPeriodSize());
                    refundToken.setRange(refundStart, refundEnd, baseToken.getPeriodSize());
                    if (!currentPlan.getResourceType(type).compatible(p.getResourceType(type))) {
                        String note = Session.getBillingNote();
                        Session.setBillingNote(Localizer.translateMessage("billview.note.delete_uncompatible_resource", new Object[]{r.getDescription()}));
                        double refund = r.estimateRefund(refundToken);
                        double usage = r.estimateUsage();
                        boolean processed = false;
                        if (refund < 0.0d) {
                            entries.add(new InvoiceEntry(4, r.getRecurrentRefundDescription(now, getAccount().getPeriodEnd()), refund));
                            total += refund;
                            processed = true;
                        }
                        if (usage > 0.0d) {
                            entries.add(new InvoiceEntry(3, r.getUsageChargeDescription(r.getPeriodBegin(), now), usage));
                            total += usage;
                            processed = true;
                        }
                        if (!processed) {
                            Session.setBillingNote(note);
                            entries.add(new InvoiceEntry(4, Localizer.translateMessage("billview.note.delete_uncompatible_resource", new Object[]{r.getDescription()}), 0.0d));
                        }
                        Session.setBillingNote(note);
                    } else if (rid.isChangeable()) {
                        refundToken.set(r.getId().getType(), r);
                        token.set(r.getId().getType(), r);
                        Collection valuesToEstimate = token.getChangePlanInitValue(refundToken);
                        token.set(r.getId().getType(), r, valuesToEstimate);
                        refundToken.set(r.getId().getType(), r, valuesToEstimate);
                        double recurrent = Resource.calc(token, 2);
                        double refund2 = r.estimateRefund(refundToken);
                        if (recurrent + refund2 > 0.0d) {
                            double recurrent2 = recurrent + refund2;
                            total += recurrent2;
                            if (recurrent2 > 0.0d) {
                                StringBuffer discountComment = new StringBuffer();
                                double discount = calculatePromoDiscount(p, 2, recurrent2, discountComment, rid.getType());
                                double recurrent3 = recurrent2 - discount;
                                String recurrentDescr = r.getChangePlanPeriodDescription(token, start, end);
                                entries.add(new InvoiceEntry(2, recurrentDescr + discountComment.toString(), recurrent3));
                                discountTotal += discount;
                            }
                        } else if (recurrent + refund2 < 0.0d) {
                            double refund3 = refund2 + recurrent;
                            total += refund3;
                            if (refund3 < 0.0d) {
                                String refundDscr = r.getChangePlanPeriodDescription(token, start, end);
                                entries.add(new InvoiceEntry(4, refundDscr, -refund3));
                            }
                        }
                        if (baseToken.getStartDate().after(getPeriodBegin())) {
                            double usage2 = estimateUsage();
                            if (usage2 > 0.0d) {
                                StringBuffer discountComment2 = new StringBuffer();
                                double discount2 = calculatePromoDiscount(p, 3, usage2, discountComment2, rid.getType());
                                double usage3 = usage2 - discount2;
                                entries.add(new InvoiceEntry(3, r.getUsageChargeDescription(start, end) + discountComment2.toString(), usage3));
                                discountTotal += discount2;
                                total += usage3;
                            }
                        }
                    } else {
                        List entry = getRefundedEntry(r.getId(), refundStart, refundEnd);
                        double refund4 = r.calc(4, refundStart, refundEnd, entry);
                        token.set(type, r, r.getCurrentInitValues());
                        refundToken.set(type, r, r.getCurrentInitValues());
                        double recurent = calc(token, 2);
                        if (refund4 + recurent < 0.0d) {
                            entries.add(new InvoiceEntry(4, r.getChangePlanPeriodDescription(token, refundStart, refundEnd), fix(-(refund4 + recurent))));
                            total += refund4 + recurent;
                        } else if (recurent + refund4 > 0.0d) {
                            StringBuffer discountComment3 = new StringBuffer();
                            double discount3 = calculatePromoDiscount(p, 2, recurent + refund4, discountComment3, rid.getType());
                            entries.add(new InvoiceEntry(2, r.getChangePlanPeriodDescription(token, start, end) + discountComment3.toString(), fix((recurent + refund4) - discount3)));
                            discountTotal += discount3;
                            total += (refund4 + recurent) - discount3;
                        }
                        if (baseToken.getStartDate().after(r.getPeriodBegin())) {
                            double usage4 = r.estimateUsage();
                            if (usage4 > 0.0d) {
                                StringBuffer discountComment4 = new StringBuffer();
                                double discount4 = calculatePromoDiscount(p, 3, usage4, discountComment4, rid.getType());
                                double usage5 = usage4 - discount4;
                                entries.add(new InvoiceEntry(3, r.getUsageChargeDescription(r.getPeriodBegin(), now) + discountComment4.toString(), usage5));
                                discountTotal += discount4;
                                total += usage5;
                            }
                        }
                    }
                } finally {
                    baseToken.getTypeCounter().dec(rid.getType(), r.getAmount());
                }
            }
        }
        double total2 = fix(total);
        result.put("entries", new TemplateList(entries));
        result.put("total", total2 > 0.0d ? USFormat.format(total2) : USFormat.format(-total2));
        result.put("sign_total", USFormat.format(total2));
        result.put("discount", USFormat.format(discountTotal));
        result.put("status", "OK");
        return result;
    }

    /* JADX WARN: Finally extract failed */
    public void changePlan(Plan p, int periodId, boolean force) throws Exception {
        Date start;
        Date refundStart;
        Date end;
        Date refundEnd;
        synchronized (this) {
            if (isBlocked()) {
                throw new HSUserException("content.move_lock_resource");
            }
            Account a = Session.getAccount();
            int newPlanId = p.getId();
            a.setLocked(true);
            Plan currentPlan = getPlan();
            if (getAccount().getBillingInfo().getBillingType() != 1) {
                Hashtable esRes = estimateChangePlan(p, periodId, force);
                double total = USFormat.parseDouble(esRes.get("sign_total").toString());
                if (getBill().getBalance() - total < (-getBill().getCredit())) {
                    throw new HSUserException("resource.credit");
                }
            }
            if (currentPlan.getId() != newPlanId) {
                if (!p.isAccessible(this.planId) && !force) {
                    throw new HSUserException("account.planaccess");
                }
                String oldCreated = getPlan().getValue("_CREATED_BY_");
                String newCreated = p.getValue("_CREATED_BY_");
                if (oldCreated != null && newCreated != null && !oldCreated.equals(newCreated)) {
                    throw new HSUserException("account.incompatible_created_by");
                }
                if (periodId == getPeriodId()) {
                    Session.setBillingNote(Localizer.translateMessage("accountmanager.change_plan", new Object[]{currentPlan.getDescription(), p.getDescription()}));
                } else {
                    Session.setBillingNote(Localizer.translateMessage("accountmanager.change_plan_period", new Object[]{currentPlan.getDescription(), getPeriodAsString(getPeriodId()), p.getDescription(), p.getPeriodAsString(periodId)}));
                }
            } else if (periodId == getPeriodId()) {
                throw new HSUserException("account.changing_to_same_plan");
            } else {
                Session.setBillingNote(Localizer.translateMessage("accountmanager.change_payment_period", new Object[]{getPeriodAsString(getPeriodId()), getPeriodAsString(periodId)}));
            }
            Date now = TimeUtils.getDate();
            LinkedList recoveryCache = new LinkedList();
            initBill(now, true);
            synchronized (getBill()) {
                for (ResourceId rid : getChildManager().getAllResources()) {
                    int type = rid.getType();
                    Resource r = rid.get();
                    if (!currentPlan.getResourceType(type).compatible(p.getResourceType(type))) {
                        String note = Session.getBillingNote();
                        try {
                            Session.setBillingNote(Localizer.translateMessage("billview.note.delete_uncompatible_resource", new Object[]{r.getDescription()}));
                            r.delete(true, 2);
                            Session.setBillingNote(note);
                        } catch (Throwable th) {
                            Session.setBillingNote(note);
                            throw th;
                        }
                    }
                }
                Connection con = Session.getTransConnection();
                try {
                    try {
                        InitToken baseToken = getTokenByPlanPeriod(p, periodId);
                        for (ResourceId rid2 : getChildManager().getAllResources()) {
                            Resource r2 = rid2.get();
                            if (r2 != null) {
                                try {
                                    int type2 = rid2.getType();
                                    InitToken token = new InitToken(baseToken);
                                    InitToken refundToken = new InitToken(this, baseToken.getTypeCounter());
                                    if (r2.getId().isMonthly()) {
                                        start = new Date(Math.max(baseToken.getStartDate().getTime(), getPeriodBegin().getTime()));
                                        refundStart = r2.getPeriodBegin();
                                        end = nextMonthlyBillingDate(start);
                                        refundEnd = nextMonthlyBillingDate(refundStart);
                                    } else {
                                        start = now;
                                        refundStart = now;
                                        end = baseToken.getEndDate();
                                        refundEnd = getPeriodEnd();
                                    }
                                    token.setRange(start, end, baseToken.getPeriodSize());
                                    refundToken.setRange(refundStart, refundEnd, getPeriodSize());
                                    if (!currentPlan.getResourceType(type2).compatible(p.getResourceType(type2))) {
                                        Session.getLog().error("Incompatible resource" + r2.getDescription() + " for " + p.getDescription());
                                    } else if (rid2.isChangeable()) {
                                        token.set(type2, r2, r2.getCurrentInitValues());
                                        refundToken.set(type2, r2, r2.getCurrentInitValues());
                                        Collection valuesToEstimate = token.getChangePlanInitValue(refundToken);
                                        r2.changeResourceBilling(p, periodId, valuesToEstimate);
                                        double oldSize = r2.changeResource(p, periodId, valuesToEstimate);
                                        if (oldSize != r2.getAmount()) {
                                            r2.setNewAmountResource(oldSize);
                                            recoveryCache.add(rid2);
                                        }
                                    } else {
                                        token.set(type2, r2, r2.getCurrentInitValues());
                                        refundToken.set(type2, r2, r2.getCurrentInitValues());
                                        double recurrent = calc(token, 2);
                                        List entry = getRefundedEntry(r2.getId(), refundStart, refundEnd);
                                        double refund = r2.calc(4, refundStart, refundEnd, entry);
                                        double toPay = recurrent + refund;
                                        double resRecurrent = 0.0d;
                                        double resRefund = 0.0d;
                                        if (Session.getResellerId() != 1) {
                                            resRecurrent = r2.resellerCalc(2, start, end);
                                            resRefund = r2.resellerCalc(4, refundStart, refundEnd, entry);
                                        }
                                        double resellerToPay = resRecurrent + resRefund;
                                        String changeDescr = r2.getChangePlanPeriodDescription(token, start, end);
                                        String resellerToPayDescription = "";
                                        if (Session.getResellerId() != 1) {
                                            if (resellerToPay > 0.0d) {
                                                resellerToPayDescription = r2.getResellerRecurrentChangeDescripton(start, end);
                                            } else {
                                                resellerToPayDescription = r2.getResellerRecurrentRefundDescription(start, end);
                                            }
                                        }
                                        if (toPay > 0.0d) {
                                            r2.addEntry(2, toPay, changeDescr, resellerToPay, resellerToPayDescription, start, end);
                                        } else {
                                            r2.addEntry(4, toPay, changeDescr, resellerToPay, resellerToPayDescription, refundStart, refundEnd);
                                        }
                                        if (baseToken.getStartDate().after(r2.getPeriodBegin())) {
                                            double usage = r2.estimateUsage();
                                            double resellerUsage = 0.0d;
                                            String resellerUsageDescription = "";
                                            if (Session.getResellerId() != 1) {
                                                resellerUsage = r2.resellerCalc(3, start, end);
                                                if (resellerUsage > 0.0d) {
                                                    resellerUsageDescription = r2.getResellerUsageChargeDescription(start, end);
                                                }
                                            }
                                            if (usage > 0.0d) {
                                                r2.addEntry(3, usage, r2.getUsageChargeDescription(r2.getPeriodBegin(), now), resellerUsage, resellerUsageDescription, start, end);
                                            }
                                            if (r2.getId().getType() != 0) {
                                                r2.setPeriodBegin(baseToken.getStartDate());
                                            }
                                        }
                                    }
                                    baseToken.getTypeCounter().dec(rid2.getType(), r2.getAmount());
                                    getTypeCounter().dec(rid2.getType(), r2.getAmount());
                                } catch (Throwable th2) {
                                    baseToken.getTypeCounter().dec(rid2.getType(), r2.getAmount());
                                    getTypeCounter().dec(rid2.getType(), r2.getAmount());
                                    throw th2;
                                }
                            }
                        }
                        if (baseToken.getStartDate().after(getPeriodBegin())) {
                            setPeriodBegin(baseToken.getStartDate());
                        }
                        setNoRefund(true);
                        this.planId = newPlanId;
                        this.periodId = periodId;
                        PreparedStatement ps = con.prepareStatement("UPDATE accounts SET plan_id = ?, period_id = ? WHERE id = ?");
                        ps.setInt(1, this.planId);
                        ps.setInt(2, this.periodId);
                        ps.setLong(3, getId().getId());
                        ps.executeUpdate();
                        setPeriodEnd(p.getNextPaymentDate(getPeriodBegin(), this.periodId));
                        Reseller res = Reseller.getReseller(Session.getUser().getId());
                        if (res != null && res.getAccount().getId().getId() == getId().getId()) {
                            res.setResellerPlanId(newPlanId);
                            long resellerId = Session.getResellerId();
                            try {
                                try {
                                    Session.setResellerId(res.getId());
                                    Plan adminPlan = Plan.getPlan(res.getPlanId());
                                    adminPlan.FM_accessChange(Integer.toString(adminPlan.getId()) + "," + Integer.toString(newPlanId) + ",");
                                    Session.setResellerId(resellerId);
                                } catch (Exception ex) {
                                    Session.getLog().debug("Unable to change access for reseller's admin plan ", ex);
                                    Session.setResellerId(resellerId);
                                }
                            } catch (Throwable th3) {
                                Session.setResellerId(resellerId);
                                throw th3;
                            }
                        }
                        loadTypeCounter();
                        this.noRefund = false;
                        Session.closeStatement(ps);
                        Session.commitTransConnection(con);
                    } catch (Exception ex2) {
                        Session.getLog().error("Problem changing plan... id= " + newPlanId, ex2);
                        con.rollback();
                        Iterator iterator = recoveryCache.iterator();
                        while (iterator.hasNext()) {
                            ResourceId resourceId = (ResourceId) iterator.next();
                            getCache().remove(resourceId);
                            Resource r3 = resourceId.get();
                            if (r3 != null) {
                                List values = new ArrayList();
                                values.add(new Double(r3.getAmount()).toString());
                                r3.changeResource(values);
                            }
                        }
                        throw ex2;
                    }
                } catch (Throwable th4) {
                    loadTypeCounter();
                    this.noRefund = false;
                    Session.closeStatement(null);
                    Session.commitTransConnection(con);
                    throw th4;
                }
            }
            a.setLocked(false);
        }
    }

    protected void changePlan(int planId, int periodId) throws Exception {
        Plan p = Plan.getPlan(planId);
        changePlan(p, periodId, false);
        getBill().charge(getBillingInfo());
        sendInvoice(TimeUtils.getDate());
    }

    public void charge() throws Exception {
        charge(true, null);
    }

    public void charge(boolean force) throws Exception {
        charge(force, null);
    }

    public void charge(List resIds) throws Exception {
        charge(true, resIds);
    }

    public synchronized void charge(boolean force, Collection resIds) throws Exception {
        if (isSuspended() || isDeleted()) {
            return;
        }
        Date now = TimeUtils.getDate();
        Date start = getPeriodEnd();
        if (TimeUtils.getDate().before(getPeriodEnd())) {
            if (force) {
                getBill().charge(getBillingInfo());
            }
            sendInvoice(now);
            return;
        }
        getLog().debug("Closing billing period for " + getId());
        initBill(start, true);
        synchronized (this.bill) {
            Connection con = Session.getTransConnection();
            Session.setBillingNote(Localizer.translateMessage("accountmanager.open_new_period_note", new Object[]{f42df.format(start), f42df.format(nextPaymentDate(start))}));
            closeBillingPeriod(start, false, resIds, true);
            openBillingPeriod(start, resIds, true);
            Session.commitTransConnection(con);
            Session.resetBillingNote();
            if (force) {
                getBill().charge(getBillingInfo(), isAnniversaryBillingType());
            }
            if (!sendInvoice(now) && "billing_period".equals(Settings.get().getValue("send_no_charge"))) {
                sendNoChargeNotification();
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        return 1.0d;
    }

    @Override // psoft.hsphere.Resource
    public double getUsageMultiplier() {
        return 1.0d;
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        return 1.0d;
    }

    public static Account getAccount(long id) throws Exception {
        return (Account) get(new ResourceId(id, 0));
    }

    protected void moneyBack() throws Exception {
        int day_back = -1;
        try {
            day_back = Integer.parseInt(getPlan().getValue("MONEY_BACK_DAYS"));
        } catch (NumberFormatException e) {
        }
        if ((TimeUtils.getDate().getTime() - this.created.getTime()) / 86400000 > day_back) {
            throw new HSUserException("account.moneyback");
        }
        Date tmpDate = TimeUtils.getDate();
        getBill().addEntry(10, tmpDate, getId(), "Money Back " + this, tmpDate, null, null, calc(10, (Date) null, (Date) null));
        suspend("Money back request", true);
        Session.getLog().debug("Sending ModeyBack request");
        try {
            SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
            CustomEmailMessage.send("MONEY_BACK", (String) null, (TemplateModelRoot) root);
        } catch (Throwable e2) {
            Session.getLog().warn("Error Sending Email: ", e2);
        }
    }

    public TemplateModel FM_moneyBack() throws Exception {
        synchronized (Session.getAccount()) {
            moneyBack();
        }
        return this;
    }

    public void initBill(Date date) throws Exception {
        initBill(date, false);
    }

    public void initBill(Date date, boolean force) throws Exception {
        synchronized (this.bill) {
            if (this.bill.getBill() != null) {
                this.bill.close();
            }
            String planDescription = "";
            try {
                planDescription = getPlan().getDescription();
            } catch (Exception ex) {
                Session.getLog().error("Unable to get Plan:", ex);
            }
            synchronized (this.actualBill) {
                this.actualBill.clear();
                this.bill.setBill(new Bill(planDescription + " #" + getId().getId(), getId().getId(), date));
            }
            Session.getLog().debug("New Bill ID" + this.bill.getId());
            if (force) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("UPDATE accounts SET bill_id = ? WHERE id = ?");
                ps.setLong(1, this.bill.getId());
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    public Date getPeriodEnd() {
        if (this.pEnd == null) {
            try {
                this.pEnd = getPeriodBegin();
                setPeriodEnd(nextPaymentDate());
            } catch (Exception e) {
            }
        }
        return this.pEnd;
    }

    protected void setPeriodEnd(Date next) throws SQLException {
        if (this.pEnd == null || !this.pEnd.equals(next)) {
            this.pEnd = next;
            try {
                this.pEnd = next;
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("UPDATE accounts SET p_end = ? WHERE id = ?");
                ps.setDate(1, new java.sql.Date(next.getTime()));
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException se) {
                getLog().error("Unable to set billing period end, account: " + getId() + ": value:" + this.pEnd, se);
                throw se;
            }
        }
    }

    public Timestamp getCreated() {
        return this.created;
    }

    public BillingInfoObject getBillingInfo() {
        if (this.f21bi == null) {
            getLog().debug("Loading billing info for account " + getId());
            try {
                this.f21bi = new BillingInfoObject(this.biId);
            } catch (Exception e) {
                getLog().error("Can't get billing info", e);
            }
        }
        return this.f21bi;
    }

    public ContactInfoObject getContactInfo() {
        if (this.f22ci == null) {
            getLog().debug("Loading contact info for account " + getId());
            try {
                this.f22ci = new ContactInfoObject(this.ciId);
            } catch (Exception e) {
                getLog().error("Can't get contact info", e);
            }
        }
        return this.f22ci;
    }

    public void FM_deleteBillingInfo(long id) throws Exception {
        Connection con = Session.getDb();
        try {
            if (id == this.biId) {
                throw new HSUserException("bill.exception.delete_active");
            }
            PreparedStatement ps = con.prepareStatement("DELETE FROM user_billing_infos WHERE user_id = ? AND billing_info_id = ?");
            ps.setLong(1, Session.getUser().getId());
            ps.setLong(2, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void FM_setBillingInfo(long id) throws Exception {
        BillingInfoObject newBi = new BillingInfoObject(id);
        Session.getLog().debug("Try to activate Billing Info:" + id + " than consist in list of Billing p: " + Session.getUser().getBillingInfos().contains(newBi));
        if (Session.getUser().getBillingInfos().contains(newBi)) {
            saveBillingInfoId(newBi);
            return;
        }
        throw new HSUserException("account.billing");
    }

    public TemplateModel FM_updateContactInfo(String name, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        Session.getLog().debug("Contact info update 1");
        if (getContactInfo().equals(name, address1, address2, city, state, postalCode, country, phone, email)) {
            return this;
        }
        this.f22ci = new ContactInfoObject(name, address1, address2, city, state, postalCode, country, phone, email);
        saveContactInfoId();
        sendChangeInfoNotification(this.f22ci);
        return this;
    }

    public TemplateModel FM_updateContactInfo(String first_name, String last_name, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        Session.getLog().debug("Contact info update");
        if (getContactInfo().equals(first_name, last_name, company, address1, address2, city, state, postalCode, country, phone, email)) {
            return this;
        }
        this.f22ci = new ContactInfoObject(first_name, last_name, company, address1, address2, city, state, postalCode, country, phone, email);
        Session.getLog().debug("NewContact info Save");
        saveContactInfoId();
        sendChangeInfoNotification(this.f22ci);
        return this;
    }

    public TemplateModel FM_updateContactInfo(String first_name, String last_name, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) throws Exception {
        Session.getLog().debug("Contact info update 2");
        if (getContactInfo().equals(first_name, last_name, company, address1, address2, city, state, state2, postalCode, country, phone, email)) {
            return this;
        }
        this.f22ci = new ContactInfoObject(first_name, last_name, company, address1, address2, city, state, state2, postalCode, country, phone, email);
        Session.getLog().debug("NewContact info Save");
        saveContactInfoId();
        sendChangeInfoNotification(this.f22ci);
        return this;
    }

    public TemplateModel FM_updateBillingInfo(String name, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        if (getBillingInfo().equals(name, address1, address2, city, state, "", postalCode, country, phone, email)) {
            return this;
        }
        Session.getLog().info("Creating new bi");
        BillingInfoObject newbi = new BillingInfoObject(name, address1, address2, city, state, "", postalCode, country, phone, email, getBillingInfo().getType(), getBillingInfo().getPaymentInstrument());
        saveBillingInfoId(newbi);
        sendChangeInfoNotification(newbi);
        return this;
    }

    public TemplateModel FM_updateBillingInfo(String name, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) throws Exception {
        if (getBillingInfo().equals(name, address1, address2, city, state, state2, postalCode, country, phone, email)) {
            return this;
        }
        Session.getLog().info("Creating new bi");
        BillingInfoObject newbi = new BillingInfoObject(name, address1, address2, city, state, state2, postalCode, country, phone, email, getBillingInfo().getType(), getBillingInfo().getPaymentInstrument());
        saveBillingInfoId(newbi);
        sendChangeInfoNotification(newbi);
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/Account$BIChanger.class */
    public class BIChanger implements TemplateMethodModel {
        BIChanger() {
            Account.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                BillingInfoObject newbi = new BillingInfoObject(new NameModifier("_bi_"));
                Account.this.saveBillingInfoId(newbi);
                Account.this.sendChangeInfoNotification(newbi);
                return Account.this;
            } catch (Throwable th) {
                t = th;
                if (t instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) t).getTargetException();
                }
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error changing BI: ", t);
                Ticket.create(t, this, "Change Billing Info");
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/Account$PIChanger.class */
    public class PIChanger implements TemplateMethodModel {
        PIChanger() {
            Account.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                l = HTMLEncoder.decode(l);
                BillingInfoObject newbi = new BillingInfoObject(Account.this.getBillingInfo(), l.iterator());
                Account.this.saveBillingInfoId(newbi);
                Account.this.sendChangeInfoNotification(newbi);
                return Account.this;
            } catch (Throwable th) {
                t = th;
                if (t instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) t).getTargetException();
                }
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error change PI: " + l, t);
                Ticket.create(t, this, "Change Payment Info");
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    protected void saveContactInfoId() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET ci_id = ? WHERE id = ?");
            ps.setLong(1, getContactInfo().getId());
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.ciId = getContactInfo().getId();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateBillingInfoId(BillingInfoObject newbi) throws Exception {
        Session.getLog().debug("New Billing Info is " + newbi.getBillingTypeString());
        this.f21bi = newbi;
        this.biId = newbi.getId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET bi_id = ? WHERE id = ?");
            ps.setLong(1, getBillingInfo().getId());
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void saveBillingInfoId(BillingInfoObject newbi) throws Exception {
        String email;
        boolean wasTrial = false;
        if (getBillingInfo().getBillingType() == -1) {
            wasTrial = true;
        }
        if (newbi.getBillingType() == 1) {
            Date now = getPeriodEnd();
            getBill().charge(newbi);
            sendInvoice(now);
        }
        updateBillingInfoId(newbi);
        boolean wasSuspended = this.suspended;
        Session.getLog().debug("Account in Debt:" + getBill().isInDebt());
        if (wasTrial && !getBill().isInDebt() && this.suspended) {
            resume();
        }
        if (wasTrial) {
            try {
                Session.getLog().debug("Became paid user EMAIL");
                if (getBillingInfo().getEmail() != null) {
                    email = getBillingInfo().getEmail();
                } else {
                    email = getContactInfo().getEmail();
                }
                Session.getLog().debug("Email:" + email + " BIEmail:" + getBillingInfo().getEmail() + " CIEmail:" + getContactInfo().getEmail());
                if (email != null) {
                    SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                    if (wasSuspended) {
                        CustomEmailMessage.send("TRIAL_SUSP_REGISTER", email, (TemplateModelRoot) root);
                    } else {
                        CustomEmailMessage.send("TRIAL_REGISTER", email, (TemplateModelRoot) root);
                    }
                }
            } catch (Throwable e) {
                Session.getLog().warn("Error Sending Email: ", e);
            }
        }
    }

    public User getUser() {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT u.username FROM user_account u_a, users u WHERE u_a.user_id = u.id AND u_a.account_id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = User.getUser(rs.getString(1));
                Session.closeStatement(ps);
                con.close();
                return user;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Exception ex) {
            Session.getLog().debug("Account password", ex);
            return null;
        }
    }

    public synchronized void suspend(String reason) throws Exception {
        suspend(reason, false);
    }

    public synchronized void suspend(String reason, boolean skipBilling) throws Exception {
        String email;
        Resource r;
        if (this.suspended && !this.partlySuspended) {
            return;
        }
        Date now = TimeUtils.getDate();
        boolean sendEmail = false;
        synchronized (this) {
            Session.setBillingNote(Localizer.translateMessage("accountmanager.suspend_note", new Object[]{Long.toString(getId().getId())}));
            PreparedStatement ps = null;
            Connection con = Session.getTransConnection();
            this.isAllowChildMod = false;
            if (!this.suspended) {
                if (getBillingInfo().getBillingType() != 0) {
                    closeBillingPeriod(now, suspendRefundAll, skipBilling, false);
                    sendEmail = true;
                }
                ps = con.prepareStatement("UPDATE accounts SET suspended=?,suspend_reason=? WHERE id=?");
                this.suspendDate = new Timestamp(now.getTime());
                ps.setTimestamp(1, this.suspendDate);
                if (reason.length() > 2048) {
                    reason = reason.substring(0, 2047);
                }
                ps.setString(2, reason);
                ps.setLong(3, getId().getId());
                ps.executeUpdate();
                this.suspendReason = reason;
            }
            this.partlySuspended = false;
            for (ResourceId rId : getChildManager().getAllResources()) {
                if (rId.getType() != 0) {
                    try {
                        r = Resource.get(rId);
                    } catch (HSUserException ex) {
                        Session.addMessage(ex.getMessage());
                    } catch (Exception ex2) {
                        this.partlySuspended = true;
                        Session.getLog().error("Account: Error suspending " + rId.toString(), ex2);
                        Ticket.create(ex2, this, "Suspending error: " + reason);
                    }
                    if (r == null) {
                        Session.getLog().debug("Unable to get Resource:" + rId);
                    } else if (!r.isSuspended()) {
                        r.suspend();
                    }
                }
            }
            if (!this.partlySuspended) {
                super.suspend();
            }
            this.suspended = true;
            this.isAllowChildMod = true;
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            Session.resetBillingNote();
        }
        if (!sendEmail) {
            return;
        }
        Session.getLog().debug("Suspend EMAIL");
        try {
            if (getBillingInfo().getEmail() != null) {
                email = getBillingInfo().getEmail();
            } else {
                email = getContactInfo().getEmail();
            }
            Session.getLog().debug("Email:" + email + " BIEmail:" + getBillingInfo().getEmail() + " CIEmail:" + getContactInfo().getEmail());
            if (email != null) {
                SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                root.put("reason", reason);
                CustomEmailMessage.send("SUSPEND", email, (TemplateModelRoot) root);
            }
        } catch (Throwable e) {
            Session.getLog().warn("Error Sending Email: ", e);
        }
        try {
            getBill().charge(getBillingInfo());
            sendInvoice(now);
        } catch (Throwable th) {
        }
    }

    @Override // psoft.hsphere.Resource
    public synchronized void resume() throws Exception {
        String email;
        if (!this.suspended && !this.partlySuspended) {
            return;
        }
        Date now = TimeUtils.getDate();
        boolean sendEmail = false;
        synchronized (this) {
            Session.setBillingNote(Localizer.translateMessage("accountmanager.resume_note", new Object[]{Long.toString(getId().getId())}));
            Connection con = Session.getTransConnection();
            this.isAllowChildMod = false;
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET suspended = ? WHERE id = ?");
            ps.setNull(1, 93);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            if (isSuspended() && getBillingInfo().getBillingType() != 0) {
                openBillingPeriod(now, false);
                sendEmail = true;
            }
            this.partlySuspended = false;
            for (ResourceId rId : getChildManager().getAllResources()) {
                Resource r = null;
                try {
                } catch (Exception ex) {
                    Session.getLog().error("Account: Error resuming" + rId.toString(), ex);
                }
                if (rId.getType() != 0) {
                    r = Resource.get(rId);
                    if (r == null) {
                        Session.getLog().debug("Unable to get Resource:" + rId);
                    } else if (r.isSuspended()) {
                        if (r != null) {
                            try {
                                r.resume();
                            } catch (HSUserException ex2) {
                                Session.addMessage(ex2.getMessage());
                            } catch (Exception ex3) {
                                this.partlySuspended = true;
                                Session.getLog().error("Account: Error resuming" + rId.toString(), ex3);
                                Ticket.create(ex3, this, "Resuming error:");
                            }
                        }
                    }
                }
            }
            if (!this.partlySuspended) {
                super.resume();
            }
            this.suspendDate = null;
            this.suspended = false;
            this.isAllowChildMod = true;
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            Session.resetBillingNote();
        }
        if (!sendEmail) {
            return;
        }
        Session.getLog().debug("Resumed EMAIL");
        try {
            if (getBillingInfo().getEmail() != null) {
                email = getBillingInfo().getEmail();
            } else {
                email = getContactInfo().getEmail();
            }
            Session.getLog().debug("Email:" + email + " BIEmail:" + getBillingInfo().getEmail() + " CIEmail:" + getContactInfo().getEmail());
            if (email != null) {
                SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                CustomEmailMessage.send("RESUME", email, (TemplateModelRoot) root);
            }
        } catch (Throwable e) {
            Session.getLog().warn("Error Sending Email: ", e);
        }
        try {
            getBill().charge(getBillingInfo());
            sendInvoice(now);
        } catch (Throwable th) {
        }
    }

    public void FM_turnReciveInvoiceByEmail(String state) throws Exception {
        Session.getLog().debug("Receive email by email:" + this.receiveInvoice);
        this.receiveInvoice = "ON".equals(state.toUpperCase());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET receive_invoice = ? WHERE id = ?");
            ps.setInt(1, this.receiveInvoice ? 1 : 0);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_turnTooltips(String state) throws Exception {
        this.showTooltips = "ON".equals(state.toUpperCase());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE accounts SET tooltips = ? WHERE id = ?");
            ps.setInt(1, this.showTooltips ? 1 : 0);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public long getTrialDayLeftTill() {
        int trial_period = -1;
        try {
            trial_period = Integer.parseInt(getPlan().getValue("_TRIAL_PERIOD"));
        } catch (NumberFormatException e) {
        }
        long tmpDateNow = TimeUtils.currentTimeMillis();
        if ((tmpDateNow - this.created.getTime()) / 86400000 > trial_period) {
            return 0L;
        }
        return trial_period - ((tmpDateNow - this.created.getTime()) / 86400000);
    }

    public TemplateModel FM_checkForIncompatibleResources(int planId) throws Exception {
        HashMap irQuantity = new HashMap();
        TemplateList irList = new TemplateList();
        Plan p = Plan.getPlan(planId);
        Plan currentPlan = getPlan();
        for (ResourceId rid : getChildManager().getAllResources()) {
            int typeId = rid.getType();
            String type = Integer.toString(typeId);
            Session.getLog().debug("Type id=" + typeId);
            ResourceType rt = currentPlan.getResourceType(typeId);
            if (rt == null) {
                Session.getLog().warn("Plan contains resources that is not supported by this plan typeId=" + typeId);
            } else if (!currentPlan.getResourceType(typeId).compatible(p.getResourceType(typeId))) {
                if (irQuantity.containsKey(type)) {
                    int currQ = Integer.parseInt((String) irQuantity.get(type));
                    irQuantity.put(type, Integer.toString(currQ + 1));
                } else {
                    irQuantity.put(type, "1");
                }
            }
        }
        for (String key : irQuantity.keySet()) {
            TemplateHash rs = new TemplateHash();
            rs.put("description", TypeRegistry.getDescription(key));
            rs.put("quantity", irQuantity.get(key));
            irList.add((TemplateModel) rs);
        }
        return irList;
    }

    public void setBalanceExhaustionDate(Date dt) {
        this.balanceExhaustionDate = dt;
    }

    public Date getBalanceExhaustionDate() {
        return this.balanceExhaustionDate;
    }

    public void moveMailToHost(long newHostId, ToolLogger logger) throws Exception {
        moveToHost(newHostId, MailHostDependentTypeList.hostDepTypes, logger);
    }

    public void moveVHostToHost(long newHostId, ToolLogger logger) throws Exception {
        moveToHost(newHostId, VHostingHostDependentTypeList.hostDepTypes, logger);
    }

    public void moveWinVHostToHost(long newHostId, ToolLogger logger) throws Exception {
        moveToHost(newHostId, VHostingHostDependentTypeList.winHostDepTypes, logger);
    }

    public void moveToHost(long newHostId, ToolLogger logger) throws Exception {
        moveToHost(newHostId, HostDependentTypeList.hostDepTypes, logger);
    }

    public void moveToHost(long newHostId, List typeList, ToolLogger logger) throws Exception {
        List<ResourceId> rids = new HostDependentResourceIterator(getId().getAllChildrenByPriority(), typeList).getValues();
        List holder = new ArrayList();
        setBeingMoved(true);
        StringBuffer reason = new StringBuffer("");
        logger.outMessage("Checking moveability of resources\n");
        logger.setPrfx("\t");
        for (ResourceId rid : rids) {
            Session.getLog().debug("Loading and checking " + rid);
            HostDependentResource r = (HostDependentResource) rid.get();
            try {
                logger.outMessage(rid.toString());
                r.canBeMovedTo(newHostId);
                logger.outOK();
            } catch (Exception ex) {
                reason.append("\n" + ex.getMessage());
                logger.outFail();
            }
        }
        if (reason.length() > 0) {
            setBeingMoved(false);
            throw new ResourceMoveException(reason.toString());
        }
        logger.setPrfx("");
        logger.outMessage("Physical creation of resources on " + HostManager.getHost(newHostId).getName() + " logical server\n");
        logger.setPrfx("\t");
        for (ResourceId rid2 : rids) {
            HostDependentResource r2 = (HostDependentResource) rid2.get();
            try {
                logger.outMessage(rid2.toString());
                r2.physicalCreate(newHostId);
                holder.add(rid2);
                logger.outOK();
            } catch (Exception ex2) {
                logger.outFail();
                ListIterator j = holder.listIterator(holder.size());
                while (j.hasPrevious()) {
                    try {
                        ResourceId rid1 = (ResourceId) j.previous();
                        ((HostDependentResource) rid1.get()).physicalDelete(newHostId);
                    } catch (Exception e) {
                    }
                }
                Session.getLog().error("Errors during physical move resource ", ex2);
                setBeingMoved(false);
                throw ex2;
            }
        }
        ArrayList postponedResources = new ArrayList();
        ContentMoveItem cmi = null;
        logger.setPrfx("");
        logger.outMessage("Physical deletion/deletion postponing of the resources\n");
        logger.setPrfx("\t");
        ListIterator i = rids.listIterator(rids.size());
        while (i.hasPrevious()) {
            ResourceId rid3 = (ResourceId) i.previous();
            HostDependentResource r3 = (HostDependentResource) rid3.get();
            if (((Resource) r3).hasCMI()) {
                logger.outMessage("Initializing content move module\n");
                cmi = ((Resource) r3).initContentMove(r3.getHostId(), newHostId);
            }
            if (((Resource) r3).getTTL() == 0) {
                try {
                    logger.outMessage(rid3.toString() + " deletion");
                    r3.physicalDelete(r3.getHostId());
                    logger.outOK();
                } catch (Exception ex3) {
                    Session.getLog().error(rid3 + " physical deletion errors ", ex3);
                    logger.outFail();
                }
            } else {
                logger.outMessage(rid3.toString() + " postponing deletion");
                postponedResources.add(r3);
                logger.outOK();
            }
            Session.getLog().debug("Setting host_id for " + rid3.toString());
            r3.setHostId(newHostId);
        }
        if (cmi != null) {
            for (int i2 = 0; i2 < postponedResources.size(); i2++) {
                cmi.addPostponedResource(((Resource) postponedResources.get(i2)).getId());
            }
        }
        setBeingMoved(false);
    }

    public AccountPreferences getPreferences() {
        return this.preferences;
    }

    public TemplateModel FM_design() throws Exception {
        return this.preferences.get(AccountPreferences.DESIGN_ID);
    }

    public TemplateModel FM_change_design(String designId) throws Exception {
        this.preferences.setValue(AccountPreferences.DESIGN_ID, designId);
        this.preferences.removeValue(AccountPreferences.ICON_IMAGE_SET);
        return this;
    }

    public TemplateModel FM_icon_image_set() throws Exception {
        return this.preferences.get(AccountPreferences.ICON_IMAGE_SET);
    }

    public TemplateModel FM_change_icon_image_set(String iconImageSet) throws Exception {
        this.preferences.setValue(AccountPreferences.ICON_IMAGE_SET, iconImageSet);
        return this;
    }

    public TemplateModel FM_skill_icon_set() throws Exception {
        return this.preferences.get(AccountPreferences.SKILL_ICON_SET);
    }

    public TemplateModel FM_change_skill_icon_set(String set) throws Exception {
        this.preferences.setValue(AccountPreferences.SKILL_ICON_SET, set);
        return this;
    }

    public static String getDescription(InitToken token) throws Exception {
        return Localizer.translateMessage("description.account", new Object[]{token.getPlan().getDescription()});
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.account.setup", new Object[]{getPlan().getDescription()});
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.account.recurrent", new Object[]{getPeriodInWords(), getPlan().getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.account.refundall", new Object[]{getPlan().getDescription(), f42df.format(begin), f42df.format(end)});
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.account.refund", new Object[]{getPlan().getDescription(), f42df.format(begin), f42df.format(end)});
    }

    public boolean isDemoAccount() {
        return this.isDemo;
    }

    public static void addPayment(long accountId, double amount, String text, Date now, int type) throws Exception {
        try {
            Session.save();
            long currentResellerId = Session.getResellerId();
            Session.getLog().info("ADDING PAYMENT: " + amount);
            Account a = (Account) get(new ResourceId(accountId, 0));
            long accountResellerId = a.getResellerId();
            if (currentResellerId != accountResellerId) {
                throw new HSUserException("User doesn't belong to this reseller");
            }
            Session.setUser(a.getUser());
            Session.setAccount(a);
            synchronized (a) {
                Bill bill = a.getBill();
                bill.addEntry(type, now, -1L, -1, text, (Date) null, (Date) null, (String) null, -amount);
            }
        } finally {
            Session.restore();
        }
    }

    public static void addCharge(long accountId, double amount, String text, Date now) throws Exception {
        addPayment(accountId, amount, text, now, 5);
    }

    public static void addCredit(long accountId, double amount, String text, Date now, boolean inclTaxes) throws Exception {
        addPayment(accountId, amount, text, now, inclTaxes ? 17 : 6);
    }

    public static void addCredit(long accountId, double amount, String text, Date now) throws Exception {
        addCredit(accountId, amount, text, now, false);
    }

    public List getChargeLogEntries() throws Exception {
        Connection con = Session.getDb();
        List res = new ArrayList();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, created, message_out, message_in, error_message FROM charge_log WHERE account_id=? ORDER BY created");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap t = new HashMap();
                t.put("id", rs.getString("id"));
                t.put("created", rs.getTimestamp("created").toString());
                t.put("request", rs.getString("message_out"));
                t.put("response", rs.getString("message_in"));
                t.put("error", rs.getString("error_message"));
                res.add(t);
            }
        } catch (Exception e) {
            Session.getLog().error("Unable to get charge log entries");
        }
        return res;
    }

    public TemplateModel FM_getChargeLogEntries() throws Exception {
        return new TemplateList(getChargeLogEntries());
    }

    public AccountType getAccountType() throws Exception {
        if (this.accountType == null) {
            this.accountType = AccountType.getType(this);
        }
        return this.accountType;
    }

    public void setAllowChildMod(boolean value) {
        this.isAllowChildMod = value;
    }

    public boolean isPartlySuspended() {
        return this.partlySuspended;
    }

    public synchronized List getRefundedEntry(ResourceId resId, Date begin, Date end) throws Exception {
        LinkedList billEntries = getActualBillEntries(resId, begin, end);
        if (billEntries.size() > 0) {
            try {
                Collections.sort(billEntries);
                BillEntry be = (BillEntry) billEntries.getFirst();
                if (be.getType() == 104 && resId.isMonthly()) {
                    billEntries.remove(be);
                    Session.getLog().debug("The extra bill entry on the begin billin period was removed:" + be.getDescription());
                }
                return billEntries;
            } catch (Exception ex) {
                Session.getLog().error("Unable to sor entries :", ex);
                return null;
            }
        }
        return null;
    }

    private LinkedList getActualBillEntries(ResourceId resId, Date begin, Date end) throws Exception {
        LinkedList billEntries = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            String query = "SELECT bill.id, bill_entry.id FROM bill_entry, bill WHERE bill_id = bill.id AND rid = ? AND type IN(" + new Integer(2).toString() + "," + new Integer(4).toString() + "," + new Integer((int) Resource.B_REFUND_ALL).toString() + ") AND account_id = ?";
            ps = con.prepareStatement(query);
            ps.setLong(1, resId.getId());
            ps.setLong(2, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Bill curBill = getBill(rs.getLong(1));
                BillEntry entry = curBill.getBillEntry(rs.getLong(2));
                if (entry != null && entry.getStarted().getTime() <= end.getTime() && entry.getEnded().getTime() >= begin.getTime()) {
                    billEntries.add(entry);
                }
            }
            Session.closeStatement(ps);
            con.close();
            return billEntries;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isBeingMoved() {
        return this.beingMoved;
    }

    public void setBeingMoved(boolean beingMoved) {
        this.beingMoved = beingMoved;
    }

    public TemplateModel FM_updateExemptionCode(String exemptionCode, String suspendAccount) throws Exception {
        BillingInfoObject bi = getBillingInfo();
        bi.updateExemptionCode(exemptionCode);
        if (exemptionCode != null && !"".equals(exemptionCode)) {
            if ("true".equals(suspendAccount)) {
                suspend(Localizer.translateMessage("bi.suspend.exemption_needs_approval"), true);
            }
            Session.getLog().debug("Creating a confirmation ticket");
            User user = Session.getUser();
            Ticket.create(Localizer.translateMessage("bi.suspend.exemption_needs_approval"), 1, Localizer.translateMessage("bi.suspend.exemption_needs_approval_mes", new Object[]{user.getLogin()}), null, 1, 0, 0, 0, 0, 0);
        }
        return this;
    }

    public void sendChangeInfoNotification(Object infoObject) {
        String notificationReciever = "";
        StringBuffer subject = new StringBuffer("Account ").append(getId().getId()).append(" ");
        Session.getLog().debug("Sending ci/bi change notification e-mail");
        try {
            if (infoObject instanceof ContactInfoObject) {
                notificationReciever = Session.getPropertyString("EMAIL_CONTACT_CHANGE");
            } else if (infoObject instanceof BillingInfoObject) {
                notificationReciever = Session.getPropertyString("EMAIL_BILLING_CHANGE");
            }
            if (notificationReciever.length() != 0) {
                Template t = Session.getTemplate("mail/change-info-notification.txt");
                StringWriter out = new StringWriter();
                PrintWriter writer = new PrintWriter(out);
                SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
                root.put("account", Session.getAccount());
                root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
                root.put("info", (TemplateModel) infoObject);
                if (infoObject instanceof ContactInfoObject) {
                    root.put("info_type", "contact");
                    root.put("info_name", Localizer.translateMessage("contact_info.desc"));
                    subject.append(Localizer.translateMessage("contact_info.desc"));
                } else if (infoObject instanceof BillingInfoObject) {
                    root.put("info_type", "billing");
                    root.put("info_name", Localizer.translateMessage("billing_info.desc"));
                    subject.append(Localizer.translateMessage("billing_info.desc"));
                }
                root.put("config", new ConfigModel("CLIENT"));
                t.process(root, writer);
                writer.flush();
                writer.close();
                subject.append(" changement notification");
                Session.getMailer().sendMessage(notificationReciever, subject.toString(), out.toString(), Session.getCurrentCharset());
            }
        } catch (Exception ex) {
            Session.getLog().error("Error sending change CI notification", ex);
        }
    }

    public void sendBillingMessage(String messageTag) {
        try {
            String email = this.f21bi.getEmail();
            if (email == null) {
                email = this.f22ci.getEmail();
            }
            SimpleHash root = CustomEmailMessage.getDefaultRoot(this);
            root.put("ci", this.f22ci);
            root.put("bi", this.f21bi);
            root.put("date", DateFormat.getDateInstance(2).format(new Date()));
            CustomEmailMessage.send(messageTag, email, (TemplateModelRoot) root);
        } catch (NullPointerException npe) {
            Session.getLog().warn("NPE", npe);
        } catch (Throwable t) {
            Session.getLog().warn("Error", t);
        }
    }

    public String getPlatformType() {
        if (this.platformType == null) {
            this.platformType = "";
            try {
                ResourceId rid = FM_getChild("unixuser");
                if (rid == null) {
                    if (!getPlan().areResourcesAvailable("unixuser")) {
                        if (getPlan().areResourcesAvailable("mail_service")) {
                            this.platformType = "mail_only";
                        } else if (getPlan().areResourcesAvailable("bizorg")) {
                            this.platformType = "msexchange";
                        }
                    }
                } else {
                    Resource res = rid.get();
                    if (res instanceof UnixUserResource) {
                        this.platformType = HostManager.getHost(((UnixUserResource) res).getHostId()).platformType();
                    } else {
                        this.platformType = HostManager.getHost(((WinUserResource) res).getHostId()).platformType();
                    }
                }
            } catch (Exception e) {
                Session.getLog().debug("Unable to get the account platform type", e);
            }
        }
        return this.platformType;
    }

    public TemplateModel FM_getPlatformType() {
        return new TemplateString(getPlatformType());
    }

    public Timestamp getSuspendDate() {
        return this.suspendDate;
    }

    public boolean isBlocked() {
        try {
            return isMoved();
        } catch (Exception ex) {
            Session.getLog().error("Error while executing isMoved() methode ", ex);
            return false;
        }
    }

    public boolean isMoved() throws Exception {
        if (this.isMoved == null) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                try {
                    ps = con.prepareStatement("SELECT id FROM transfer_process WHERE stage < ? AND account_id = ?");
                    ps.setInt(1, 8);
                    ps.setLong(2, this.accountId.getId());
                    this.isMoved = new Boolean(ps.executeQuery().next());
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception e) {
                    this.isMoved = new Boolean(false);
                    Session.closeStatement(ps);
                    con.close();
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return this.isMoved.booleanValue();
    }

    public void isMoved(boolean moved) throws Exception {
        this.isMoved = new Boolean(moved);
    }

    public TemplateModel FM_addAntiSpamResources(String range) throws Exception {
        HashMap hmEmails = prepareNewAntiSpams(range);
        for (String fullemail : hmEmails.keySet()) {
            MailDomain mail_domain = (MailDomain) hmEmails.get(fullemail);
            ArrayList args = new ArrayList();
            args.add(fullemail.substring(0, fullemail.indexOf("@")));
            mail_domain.addChild("antispam", "", args);
        }
        return this;
    }

    public TemplateModel FM_addAntiVirusResources(String range) throws Exception {
        HashMap hmEmails = prepareNewAntiViruses(range);
        for (String fullemail : hmEmails.keySet()) {
            MailDomain mail_domain = (MailDomain) hmEmails.get(fullemail);
            ArrayList args = new ArrayList();
            args.add(fullemail.substring(0, fullemail.indexOf("@")));
            mail_domain.addChild("antivirus", "", args);
        }
        return this;
    }

    public TemplateModel FM_getPrepareNewAntiVirusesNumber(String range) throws Exception {
        return new TemplateString(prepareNewAntiViruses(range).keySet().size());
    }

    public TemplateModel FM_getPrepareNewAntiSpamsNumber(String range) throws Exception {
        return new TemplateString(prepareNewAntiSpams(range).keySet().size());
    }

    private HashMap prepareNewAntiViruses(String range) throws Exception {
        ArrayList existsAntiVirusEmails = new ArrayList();
        HashMap mailObjects = new HashMap();
        for (ResourceId resourceId : getChildManager().getAllResources()) {
            Resource r = resourceId.get();
            if (r instanceof AntiVirus) {
                existsAntiVirusEmails.add(((AntiVirus) r).getEmail());
            } else if ((range == null || range.equalsIgnoreCase("all")) && ((r instanceof Mailbox) || (r instanceof MailForward) || (r instanceof MailAlias) || (r instanceof Autoresponder))) {
                mailObjects.put(r.get("fullemail").toString(), r.getParent().get());
            } else if (range.equalsIgnoreCase("mailbox") && (r instanceof Mailbox)) {
                mailObjects.put(r.get("fullemail").toString(), r.getParent().get());
            } else if (r instanceof MailDomain) {
                mailObjects.put(r.get("fullemail").toString(), r);
            }
        }
        HashMap mailObjectsWithoutAntivirus = new HashMap();
        for (String fullemail : mailObjects.keySet()) {
            if (!existsAntiVirusEmails.contains(fullemail)) {
                mailObjectsWithoutAntivirus.put(fullemail, mailObjects.get(fullemail));
            }
        }
        return mailObjectsWithoutAntivirus;
    }

    private HashMap prepareNewAntiSpams(String range) throws Exception {
        ArrayList existsAntiSpamEmails = new ArrayList();
        HashMap mailObjects = new HashMap();
        for (ResourceId resourceId : getChildManager().getAllResources()) {
            Resource r = resourceId.get();
            if (r instanceof AntiSpam) {
                existsAntiSpamEmails.add(((AntiSpam) r).getEmail());
            } else if ((range == null || range.equalsIgnoreCase("all")) && ((r instanceof Mailbox) || (r instanceof MailForward) || (r instanceof MailAlias) || (r instanceof Autoresponder))) {
                mailObjects.put(r.get("fullemail").toString(), r.getParent().get());
            } else if (range.equalsIgnoreCase("mailbox") && (r instanceof Mailbox)) {
                mailObjects.put(r.get("fullemail").toString(), r.getParent().get());
            } else if (r instanceof MailDomain) {
                mailObjects.put(r.get("fullemail").toString(), r);
            }
        }
        HashMap mailObjectsWithoutAntispam = new HashMap();
        for (String fullemail : mailObjects.keySet()) {
            if (!existsAntiSpamEmails.contains(fullemail)) {
                mailObjectsWithoutAntispam.put(fullemail, mailObjects.get(fullemail));
            }
        }
        return mailObjectsWithoutAntispam;
    }

    public boolean isDeleted() {
        return this.deleted != null;
    }

    public void setDeleted(Timestamp t) {
        this.deleted = t;
    }

    public TemplateModel FM_addTTResource() throws Exception {
        Connection con;
        ResourceId id;
        synchronized (getAccount()) {
            if (getAccount().isBlocked()) {
                throw new HSUserException("content.move_lock_resource");
            }
            if (getAccount().isDeleted()) {
                throw new Exception("An attempt to add tt to a deleted account " + getAccount().getId().getId());
            }
            Resource r = null;
            try {
                con = Session.getTransConnection();
                try {
                    getLog().info("Adding tt:->" + ((Object) null));
                    Resource r2 = create(this, new InitResource(TypeRegistry.getTypeId("tt"), "", 0), null, "");
                    getLog().info("created tt:->" + ((Object) null));
                    process(r2);
                    r2.init("");
                    Session.commitTransConnection(con);
                    sendOnChildCreate(r2);
                    id = r2.getId();
                } finally {
                    Session.commitTransConnection(con);
                }
            } catch (Exception e) {
                getLog().warn("error create", e);
                if (0 != 0) {
                    con = Session.getTransConnection();
                    try {
                        r.delete(true, 2);
                        Session.commitTransConnection(con);
                    } catch (NotInitDoneException e2) {
                        Session.commitTransConnection(con);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                throw e;
            }
        }
        return id;
    }

    public SignupRecord getSignupRecord() throws UnknownResellerException, SQLException {
        SignupRecord sr = null;
        if (this.signupRecord > 0) {
            return SignupManager.getSafeSignupRecord(this.signupRecord);
        }
        if (this.signupRecord == 0) {
            return null;
        }
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT signup_id FROM signup_record WHERE account_id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sr = SignupManager.getSafeSignupRecord(rs.getLong(1));
                this.signupRecord = sr.getId();
            } else {
                this.signupRecord = 0L;
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Exception e) {
            Session.getLog().error("Unable to get signup record from different reseller", e);
            this.signupRecord = 0L;
        }
        return sr;
    }

    public boolean isAnniversaryBillingType() throws Exception {
        if ("anniversary".equalsIgnoreCase(Settings.get().getValue("billing_mode"))) {
            return true;
        }
        return false;
    }

    public List getDSTPrices() throws Exception {
        List<DedicatedServerTemplate> templates = HsphereToolbox.availableDSTemplates(getPlan().getId());
        Collection<ResourceId> dsResources = getChildHolder().getChildrenByName("ds");
        Hashtable dstUsage = new Hashtable();
        for (ResourceId _rid : dsResources) {
            try {
                DedicatedServerResource _dsr = (DedicatedServerResource) _rid.get();
                if (_dsr.getDSObject().isTemplatedServer()) {
                    long _dstId = _dsr.getDSObject().getParent().getId();
                    if (dstUsage.keySet().contains(new Long(_dstId))) {
                        Integer usage = (Integer) dstUsage.get(new Long(_dstId));
                        dstUsage.put(new Long(_dstId), new Integer(usage.intValue() + 1));
                    } else {
                        dstUsage.put(new Long(_dstId), new Integer(1));
                    }
                }
            } catch (Exception ex) {
                Session.getLog().error("Unable to get dedicated server resource for " + _rid.toString(), ex);
            }
        }
        double setupDisc = 0.0d;
        double recDisc = 0.0d;
        String _setupDiscount = getPlan().getValue("_PERIOD_SETUP_DISC_" + getPeriodId());
        if (_setupDiscount != null && !"0".equals(_setupDiscount)) {
            setupDisc = USFormat.parseDouble(_setupDiscount);
        }
        String _recurrentDiscount = getPlan().getValue("_PERIOD_UNIT_DISC_" + getPeriodId());
        if (_recurrentDiscount != null && !"0".equals(_recurrentDiscount)) {
            recDisc = USFormat.parseDouble(_recurrentDiscount);
        }
        double periodMultiplier = Calc.getMultiplier();
        ResourceType rt = getPlan().getResourceType(7100);
        ArrayList result = new ArrayList();
        for (DedicatedServerTemplate _dst : templates) {
            double setup = Calc.getPrice(getPlan(), rt, getPeriodId(), "DST_" + _dst.getId() + "_SETUP");
            if (Double.isNaN(setup)) {
                setup = Calc.getDefaultPrice(getPlan(), rt, getPeriodId(), "DST_" + _dst.getId() + "_SETUP") * periodMultiplier;
            }
            double recurrent = Calc.getPrice(getPlan(), rt, getPeriodId(), "DST_" + _dst.getId() + "_REC");
            if (Double.isNaN(recurrent)) {
                recurrent = Calc.getDefaultPrice(getPlan(), rt, getPeriodId(), "DST_" + _dst.getId() + "_REC") * periodMultiplier;
            }
            Hashtable t = new Hashtable();
            t.put("name", _dst.getName());
            t.put("setup", Double.toString(setup - ((setup * setupDisc) / 100.0d)));
            t.put("recurrent", Double.toString(recurrent - ((recurrent * recDisc) / 100.0d)));
            t.put("avail", Integer.toString(_dst.getFreeServers()));
            if (dstUsage.keySet().contains(new Long(_dst.getId()))) {
                t.put("in_use", ((Integer) dstUsage.get(new Long(_dst.getId()))).toString());
            } else {
                t.put("in_use", "0");
            }
            result.add(t);
        }
        return result;
    }

    public TemplateModel FM_getDSTPrices() throws Exception {
        return new TemplateList(getDSTPrices());
    }
}
