package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.billing.estimators.ComplexEstimator;
import psoft.hsphere.billing.estimators.ComplexEstimatorManager;
import psoft.hsphere.billing.estimators.EstimateCreateCopies;
import psoft.hsphere.billing.estimators.EstimateCreateMass;
import psoft.hsphere.billing.estimators.EstimateDeleteMass;
import psoft.hsphere.cron.Accounting;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.promotion.Promo;
import psoft.hsphere.resource.admin.ContentMoveItem;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.LockableCacheEntry;
import psoft.util.NFUCache;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/Resource.class */
public abstract class Resource implements TemplateHashModel, LockableCacheEntry {

    /* renamed from: id */
    protected ResourceId f41id;
    protected ResourceId accountId;
    protected boolean initialized;
    protected boolean deleting;
    protected boolean suspended;
    protected boolean deletePrev;
    public static final double UNLIMITED = -1.0d;
    public static final int POSTPONE_PHYSICAL_DELETION = 1;
    public static final int PERFORM_PHYSICAL_DELETION = 2;
    protected boolean deleteState;
    public static final int BA_NOTBILL = 1;
    public static final int BA_REFUND_NO_CHARGE = 2;
    public static final int BA_REFUND_CHARGE = 3;
    public static final int BA_CHARGE = 4;
    public static final int AM_DELETE = 1;
    public static final int AM_UPDATE = 0;
    public static final int AM_CREATE = 0;
    public static final int AM_LAST = 0;
    protected Date pBegin;
    public static final int B_SETUP = 1;
    public static final int B_RECURRENT = 2;
    public static final int B_USAGE = 3;
    public static final int B_REFUND = 4;
    public static final int B_CHARGE = 5;
    public static final int B_CREDIT = 6;
    public static final int B_AUTH = 7;
    public static final int B_CAPTURE = 8;
    public static final int B_VOID = 9;
    public static final int B_MONEYBACK = 10;
    public static final int B_DEBIT = 11;
    public static final int B_RESELLER_SETUP = 12;
    public static final int B_RESELLER_RECURRENT = 13;
    public static final int B_RESELLER_USAGE = 14;
    public static final int B_RESELLER_REFUND = 15;
    public static final int B_DEBIT_TAX = 16;
    public static final int B_CREDIT_TAX = 17;
    public static final int B_REFUND_ALL = 104;
    public static final TemplateString STATUS_OK = new TemplateString("OK");
    protected static NFUCache cache = null;
    private static Class[] _argT = {ResourceId.class};

    /* renamed from: df */
    protected static DateFormat f42df = DateFormat.getDateInstance(3);
    protected static Class[] billingParams = {Resource.class, Date.class, Date.class};
    protected static Class[] billingParamsEntry = {Resource.class, Date.class, Date.class, List.class};
    protected static Class[] tokenBillingParams = {InitToken.class};
    protected static Class[] tokenBillingParamsEntry = {InitToken.class, List.class};
    protected static Class[] params = {InitToken.class};
    private static final Object[] emptyDescriptionParams = new Object[0];

    /*  JADX ERROR: Method load error
        jadx.core.utils.exceptions.DecodeException: Load method exception: JavaClassParseException: Unknown opcode: 0xa8 in method: psoft.hsphere.Resource.estimateDelete():freemarker.template.TemplateModel, file: hsphere.zip:psoft/hsphere/Resource.class
        	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:155)
        	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:410)
        	at jadx.core.ProcessClass.process(ProcessClass.java:67)
        	at jadx.core.ProcessClass.generateCode(ProcessClass.java:107)
        	at jadx.core.dex.nodes.ClassNode.decompile(ClassNode.java:384)
        	at jadx.core.dex.nodes.ClassNode.getCode(ClassNode.java:332)
        Caused by: jadx.plugins.input.java.utils.JavaClassParseException: Unknown opcode: 0xa8
        	at jadx.plugins.input.java.data.code.JavaCodeReader.visitInstructions(JavaCodeReader.java:71)
        	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:45)
        	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:145)
        	... 5 more
        */
    protected freemarker.template.TemplateModel estimateDelete() throws java.lang.Exception {
        /*
        // Can't load method instructions: Load method exception: JavaClassParseException: Unknown opcode: 0xa8 in method: psoft.hsphere.Resource.estimateDelete():freemarker.template.TemplateModel, file: hsphere.zip:psoft/hsphere/Resource.class
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.Resource.estimateDelete():freemarker.template.TemplateModel");
    }

    public Resource() throws Exception {
        this.deleteState = false;
        this.f41id = new ResourceId(getNewId(), 0);
    }

    public boolean locked() {
        return !this.initialized || this.deleting;
    }

    public boolean isDeleting() {
        return this.deleting;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public Resource(int type, Collection initValues) throws Exception {
        this.deleteState = false;
        this.initialized = false;
        this.deleting = false;
        this.f41id = new ResourceId(getNewId(), type);
        this.accountId = Session.getAccount().getId();
    }

    public Resource(ResourceId id) throws Exception {
        this.deleteState = false;
        this.f41id = id;
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT account_id, suspended FROM parent_child WHERE child_id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.accountId = new ResourceId(rs.getLong(1), 0);
                this.suspended = rs.getInt(2) == 1;
                Session.closeStatement(ps);
                con.close();
                this.initialized = true;
                this.deleting = false;
                return;
            }
            throw new AccessError("Resource " + id + " not found");
        } catch (SQLException se) {
            getLog().error("Error validating resource", se);
            throw new AccessError("Resource " + id + " account not found");
        }
    }

    public String getPeriodInWords() {
        if (getId().isMonthly()) {
            return getAccount().getMonthPeriodInWords();
        }
        return getAccount().getPeriodInWords();
    }

    public String getMonthPeriodInWords() {
        return getAccount().getMonthPeriodInWords();
    }

    public Resource create(Resource parent, InitResource rType, Collection initValues, String mod) throws Exception {
        try {
            accessCheck(rType.getType(), 0);
            Class aClass = _getPlan().getResourceClass(rType.getType());
            getLog().debug("Creating class " + aClass.getName() + " " + rType.getType() + initValues);
            if (initValues == null || initValues.isEmpty()) {
                initValues = _getPlan().getDefaultInitValues(parent, rType.getType(), rType.getMod(mod));
            }
            Class[] argT = {Integer.TYPE, Collection.class};
            Object[] argV = {new Integer(rType.getType()), initValues};
            getLog().debug("Class to create: " + aClass.getName() + initValues);
            Resource r = (Resource) aClass.getConstructor(argT).newInstance(argV);
            cache.put(r.getId(), r);
            return r;
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                Throwable thr = ((InvocationTargetException) e).getTargetException();
                Session.getLog().error("Resource creating error:", thr);
                if (thr instanceof Exception) {
                    throw ((Exception) thr);
                }
            } else {
                Session.getLog().error("Resource creating error:", e);
            }
            throw e;
        }
    }

    public String textDump() {
        try {
            return TypeRegistry.getType(getId().getType()) + "[" + getId() + "]";
        } catch (NoSuchTypeException e) {
            return getId().toString();
        }
    }

    public TemplateModel FM_suspend() throws Exception {
        synchronized (Session.getAccount()) {
            suspend();
        }
        return this;
    }

    public TemplateModel FM_resume() throws Exception {
        synchronized (Session.getAccount()) {
            resume();
        }
        return this;
    }

    public void suspend() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE parent_child SET suspended = 1 WHERE child_id = ? AND child_type = ?");
            ps.setLong(1, getId().getId());
            ps.setInt(2, getId().getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.suspended = true;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void resume() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE parent_child SET suspended = 0 WHERE child_id = ? AND child_type = ?");
            ps.setLong(1, getId().getId());
            ps.setInt(2, getId().getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.suspended = false;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void delete(boolean force) throws Exception {
        delete(force, 2);
    }

    public synchronized void delete(boolean force, int billingAction) throws Exception {
        if (getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        this.deleteState = force;
        if (this.deleting) {
            throw new HSUserException("resource.deleting");
        }
        if (this instanceof Account) {
            Session.getAccount().setNoRefund(true);
        }
        if (billingAction == 1 && Session.getResellerId() != 1) {
            billingAction = 2;
        }
        try {
            try {
                this.deleting = true;
                deleteReal(billingAction);
                if (this instanceof Account) {
                    Session.getAccount().setNoRefund(false);
                }
            } catch (Exception ex) {
                this.deleting = false;
                throw ex;
            }
        } catch (Throwable th) {
            if (this instanceof Account) {
                Session.getAccount().setNoRefund(false);
            }
            throw th;
        }
    }

    protected void onDeleteNote() {
    }

    public TemplateModel FM_cdelete(int i) {
        return FM_cdelete(i, 2);
    }

    public TemplateModel FM_cdelete(int i, int billingAction) {
        onDeleteNote();
        TemplateHash result = new TemplateHash();
        try {
            Date now = TimeUtils.getDate();
            Account a = getAccount();
            synchronized (a) {
                a.charge(false);
                Connection con = Session.getTransConnection();
                try {
                    try {
                        delete(i == 1, billingAction);
                        Session.commitTransConnection(con);
                    } catch (Throwable th) {
                        Session.commitTransConnection(con);
                        throw th;
                    }
                } catch (NotInitDoneException nie) {
                    Session.getLog().debug("Error :", nie);
                    Session.commitTransConnection(con);
                }
            }
            a.sendInvoice(now);
            result.put("status", "OK");
            result.put("msg", "Delete Successful");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("msg", e.getMessage());
            getLog().warn("error deleting resource ", e);
        }
        return result;
    }

    public void delete() throws Exception {
    }

    public void deletePrev() throws Exception {
    }

    protected synchronized void deleteReal(int billingActionType) throws Exception {
        accessCheck(1);
        Session.getLog().info("Deleting --->" + this.deleteState);
        Session.getLog().info("Remove begin ->" + this);
        try {
            synchronized (getAccount()) {
                getAccount().setAllowChildMod(false);
                Object[] children = getChildHolder().getChildrenByPriority().toArray();
                deletePrev();
                sendOnChildDelete(this);
                int childBillingAction = billingActionType;
                switch (billingActionType) {
                    case 3:
                        childBillingAction = 2;
                        break;
                    case 4:
                        childBillingAction = 1;
                        break;
                }
                for (int i = children.length - 1; i >= 0; i--) {
                    if (!getId().equals(children[i])) {
                        try {
                            Resource r = get((ResourceId) children[i]);
                            Session.getLog().info(this + " deleting " + r + " initialized " + r.initialized);
                            if (r != null) {
                                try {
                                    r.delete(this.deleteState, childBillingAction);
                                } catch (Throwable t) {
                                    Session.getLog().error("DELETE!!!", t);
                                }
                            }
                            Session.getLog().info(this + " done deleting " + r);
                        } catch (Throwable t2) {
                            Session.getLog().error("DELETE !!!", t2);
                        }
                    }
                }
                Session.getLog().info("Removed childs " + getId());
                Session.getLog().info("Removing self physically " + getId());
                try {
                    delete();
                } catch (Throwable t3) {
                    Session.getLog().error("DELETE!!!", t3);
                    Ticket.create(t3, this, "Physical remove resource error, ID = " + getId());
                }
                try {
                    if (this.deleteState || !this.initialized) {
                        getAccount().getBill().cancel(getId());
                    } else if (billingActionType != 1 && billingActionType != 4) {
                        usageCharge(TimeUtils.getDate());
                        if (!Session.getAccount().isNoRefund() && !getId().isNonRefundable()) {
                            recurrentRefund(TimeUtils.getDate(), getAccount().getPeriodEnd());
                        }
                    }
                } catch (Throwable t4) {
                    Session.getLog().error("DELETE!!!", t4);
                }
                getAccount().decType(getId(), getAmount());
                try {
                    removeAmount();
                    if (!(this instanceof Account)) {
                        setDescription("");
                        getAccount().getChildManager().deleteChild(this.f41id);
                        Session.getLog().info("Removed self from parent " + getId());
                    }
                } catch (Throwable t5) {
                    Session.getLog().error("DELETE!!!", t5);
                }
                if (billingActionType == 3 || billingActionType == 4) {
                    try {
                        getAccount().getBill().charge(getAccount().getBillingInfo());
                    } catch (Exception ex) {
                        Accounting.sendBillingException(ex);
                    } catch (Throwable t6) {
                        Session.getLog().error("Unable to charge deleted account", t6);
                    }
                }
                cache.remove(getId());
                Session.getLog().info("Removed self " + getId());
            }
        } catch (Throwable t7) {
            try {
                Session.getLog().error("DELETE!!!", t7);
                getAccount().setAllowChildMod(true);
            } finally {
                getAccount().setAllowChildMod(true);
            }
        }
    }

    public void notFound() throws Exception {
        throw new Exception("Resource not found " + this);
    }

    public TemplateModel FM_recursive(String key) throws Exception {
        return recursiveGet(key);
    }

    public Resource recursiveGetResource(String key) throws Exception {
        ResourceId t;
        Resource r = this;
        ResourceId a = getAccount().getId();
        do {
            t = r.getParent();
            if (t.getNamedType().equals(key)) {
                return t.get();
            }
            r = t.get();
        } while (!t.equals(a));
        return null;
    }

    public TemplateModel recursiveGet(String key) throws Exception {
        TemplateModel t;
        Resource r = this;
        do {
            r = r.getParent().get();
            t = r.get(key);
            if (t != null) {
                break;
            }
        } while (r != getAccount());
        return t;
    }

    public ResourceId getParent() {
        return getAccount().getChildManager().getParent(this.f41id);
    }

    public static Category getLog() {
        return Session.getLog();
    }

    public static void setCache(NFUCache newCache) {
        cache = newCache;
    }

    public static NFUCache getCache() {
        return cache;
    }

    public static void resetCache() {
        cache.clear();
    }

    public TemplateModel FM_getPlanValue(String key) throws Exception {
        return new TemplateString(getPlanValue(key));
    }

    public ChildHolder getChildHolder() {
        return getId().getChildHolder();
    }

    public Collection getChangePlanInitValueForRes(InitToken refundToken, InitToken initToken) throws Exception {
        return getCurrentInitValues();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Resource$AddChild.class */
    public class AddChild implements TemplateMethodModel {
        AddChild() {
            Resource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            ResourceId addChild;
            try {
                l = HTMLEncoder.decode(l);
                synchronized (Session.getAccount()) {
                    addChild = Resource.this.addChild((String) l.get(0), (String) l.get(1), l.subList(2, l.size()));
                }
                return addChild;
            } catch (Exception t) {
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error adding new child: " + l, t);
                Ticket.create(t, this, "AddChild: type = " + ((String) l.get(0)) + ", mod = " + ((String) l.get(1)) + ", params = " + l.subList(2, l.size()));
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Resource$Change.class */
    public class Change implements TemplateMethodModel {
        Change() {
            Resource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            ResourceId change;
            try {
                l = HTMLEncoder.decode(l);
                synchronized (Session.getAccount()) {
                    change = Resource.this.change(l);
                }
                return change;
            } catch (Exception t) {
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error changing resource id:" + Resource.this.getId() + " with params:" + l, t);
                Ticket.create(t, this, "ChangeResource: with  params = " + l);
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Resource$ChildFinderByList.class */
    public class ChildFinderByList implements TemplateMethodModel {
        ChildFinderByList() {
            Resource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List strTypes) {
            LinkedList res = new LinkedList();
            try {
                Iterator iter = strTypes.iterator();
                while (iter.hasNext()) {
                    String type = HTMLEncoder.decode((String) iter.next());
                    Resource.this.getId().findAllChildren(type, res);
                }
                return new TemplateList(res);
            } catch (Exception ex) {
                Session.getLog().debug("findChildren error:", ex);
                return null;
            }
        }
    }

    public ResourceId addChild(String type, String mod, Collection values) throws Exception {
        Connection con;
        ResourceId id;
        Session.getLog().debug("!!! Resource addChild!!! type=" + type);
        synchronized (getAccount()) {
            if (getAccount().isBlocked()) {
                throw new HSUserException("content.move_lock_resource");
            }
            if (getAccount().isDeleted()) {
                throw new Exception("An attempt to add " + type + " to a deleted account " + getAccount().getId().getId());
            }
            Date now = TimeUtils.getDate();
            getAccount().charge();
            if (getAccount().getBillingInfo().getBillingType() != 1) {
                TemplateHash esRes = estimateCreate(type, mod, values);
                double total = USFormat.parseDouble(esRes.get("total").toString());
                Bill bill = getAccount().getBill();
                if (bill.paymentNeedsCharge(total)) {
                    throw new HSUserException("resource.credit");
                }
            }
            Resource r = null;
            try {
                con = Session.getTransConnection();
            } catch (Exception e) {
                if (0 == 0) {
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
                    if (0 != 0) {
                        Session.getLog().info("Voiding auth");
                        getAccount().getBill().void_auth(getAccount().getBillingInfo(), null);
                    }
                    throw e;
                }
                Ticket.create(e, this, "Unable to rollback resource creation process after at least one critical resource(like OpenSRS) has been created");
                Session.getLog().error("Critical error on third party resource creation " + e);
            }
            try {
                getLog().info("Adding " + type + ":" + mod + "->" + values);
                r = create(this, new InitResource(TypeRegistry.getTypeId(type), mod, 0), values, mod);
                getLog().info("created " + type + ":" + mod + "->" + values);
                process(r);
                r.init(mod);
                Session.commitTransConnection(con);
                Iterator i = new ThirdPartyIterator(r.getId());
                if (i.hasNext()) {
                    Session.getLog().info("Authorising");
                    HashMap authRes = getAccount().getBill().auth(getAccount().getBillingInfo());
                    while (i.hasNext()) {
                        ((ThirdPartyResource) i.next()).thirdPartyAction();
                    }
                    Session.getLog().info("Post auth charging");
                    try {
                        getAccount().getBill().capture(getAccount().getBillingInfo(), authRes);
                    } catch (Exception e3) {
                        Ticket.create(e3, this, "PostAuth(CC Capture) error after successful third party resource(like OpenSRS) registration");
                        Session.getLog().error("PostAuth error " + e3);
                    }
                } else {
                    Session.getLog().info("Charging");
                    getAccount().getBill().charge(getAccount().getBillingInfo());
                }
                getAccount().sendInvoice(now);
                sendOnChildCreate(r);
                id = r.getId();
            } finally {
                Session.commitTransConnection(con);
            }
        }
        return id;
    }

    protected void onChildDelete(Resource curr) {
    }

    protected void sendOnChildDelete(Resource curr) {
        ResourceId rid = getParent();
        if (rid != null && rid.getType() != 0) {
            try {
                Resource r = rid.get();
                if (r != null) {
                    r.onChildDelete(curr);
                    r.sendOnChildDelete(curr);
                }
            } catch (Exception ex) {
                Session.getLog().debug("Unable to send onChildDelete event", ex);
            }
        }
    }

    protected void onChildCreate(Resource curr) {
    }

    public void sendOnChildCreate(Resource curr) {
        onChildCreate(curr);
        ResourceId rid = getParent();
        if (rid != null && rid.getType() != 0) {
            try {
                Resource r = rid.get();
                if (r != null) {
                    r.sendOnChildCreate(curr);
                }
            } catch (Exception ex) {
                Session.getLog().debug("Unable to send onChildCreate event", ex);
            }
        }
    }

    public synchronized double changeResource(Plan newPlan, int periodId, Collection values) throws Exception {
        return changeResource(values);
    }

    public synchronized double changeResource(Collection values) throws Exception {
        return 0.0d;
    }

    public synchronized void setNewAmountResource(double oldSize) throws Exception {
        double newSize = getAmount();
        if (newSize - oldSize == 0.0d) {
            return;
        }
        if (newSize - oldSize >= 0.0d) {
            getAccount().incType(getId(), newSize - oldSize);
        } else {
            getAccount().decType(getId(), -(newSize - oldSize));
        }
        removeAmount();
        saveAmount(newSize);
    }

    public synchronized BillEntry changeResourceBilling(Plan newPlan, int periodId, Collection values) throws Exception {
        Date start;
        Date refundStart;
        Date end;
        Date refundEnd;
        String chargeDescr;
        int chargeType;
        double resRecurrent = 0.0d;
        double resRefund = 0.0d;
        Date now = TimeUtils.dropMinutes(TimeUtils.getDate());
        double delta = toChange(newPlan, periodId, values);
        TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
        InitToken token = new InitToken(newPlan, periodId, typeCounter);
        token.set(getId().getType(), this, values);
        InitToken baseToken = getTokenByPlanPeriod(newPlan, periodId);
        if (getId().isMonthly()) {
            start = new Date(Math.max(baseToken.getStartDate().getTime(), getPeriodBegin().getTime()));
            refundStart = getPeriodBegin();
            end = nextMonthlyBillingDate(start);
            refundEnd = getAccount().getPeriodEnd();
        } else {
            start = now;
            refundStart = now;
            end = baseToken.getEndDate();
            refundEnd = getAccount().getPeriodEnd();
        }
        token.setRange(start, end, baseToken.getPeriodSize());
        List entry = getAccount().getRefundedEntry(getId(), refundStart, refundEnd);
        double refund = calc(4, start, end, entry);
        double recurrent = calc(token, 2);
        double toPay = recurrent + refund;
        if (Session.getResellerId() != 1) {
            resRecurrent = resellerCalc(token, 2);
            resRefund = resellerCalc(4, refundStart, refundEnd, entry);
        }
        double resellerToPay = resRecurrent + resRefund;
        if (toPay >= 0.0d) {
            if (delta != 0.0d) {
                chargeDescr = getRecurrentChangeDescripton(token, delta);
            } else if (token.getPlan().getId() == _getPlan().getId() && token.getPeriodId() == getAccount().getPeriodId()) {
                chargeDescr = token.getRecurrentChangeDescripton(start, end);
            } else {
                chargeDescr = getChangePlanPeriodDescription(token, start, end);
            }
            chargeType = 2;
        } else {
            if (delta != 0.0d) {
                chargeDescr = getRecurrentRefundDescription(start, end, delta, values);
            } else if (token.getPlan().getId() == _getPlan().getId() && token.getPeriodId() == getAccount().getPeriodId()) {
                chargeDescr = token.getRecurrentChangeDescripton(start, end);
            } else {
                chargeDescr = getChangePlanPeriodDescription(token, start, end);
            }
            chargeType = 4;
        }
        String resellerToPayDescription = "";
        if (Session.getResellerId() != 1) {
            double deltaReseller = token.getAmount() - getAmount();
            if (deltaReseller >= 0.0d) {
                resellerToPayDescription = token.getResellerRecurrentChangeDescripton(start, end);
            } else {
                resellerToPayDescription = token.getResellerRecurrentRefundDescription(start, end);
            }
        }
        if (toPay < 0.0d && !getId().isMonthly()) {
            start = refundStart;
            end = refundEnd;
        }
        BillEntry en = addEntry(chargeType, toPay, chargeDescr, resellerToPay, resellerToPayDescription, start, end);
        if (baseToken.getStartDate().after(getPeriodBegin())) {
            double usage = estimateUsage();
            double resellerUsage = 0.0d;
            String resellerUsageDescription = "";
            if (Session.getResellerId() != 1) {
                resellerUsage = resellerCalc(3, getPeriodBegin(), now);
                if (resellerUsage > 0.0d) {
                    resellerUsageDescription = getResellerUsageChargeDescription(getPeriodBegin(), now);
                }
            }
            if (usage > 0.0d) {
                addEntry(3, usage, getUsageChargeDescription(getPeriodBegin(), now), resellerUsage, resellerUsageDescription, getPeriodBegin(), now);
            }
            setPeriodBegin(baseToken.getStartDate());
            return null;
        }
        return en;
    }

    public ResourceId change(Collection values) throws Exception {
        return change(_getPlan(), getAccount().getPeriodId(), values);
    }

    public ResourceId change(Plan newPlan, int periodId, Collection values) throws Exception {
        ResourceId id;
        if (getAccount().isBlocked()) {
            throw new HSUserException("content.move_lock_resource");
        }
        synchronized (this) {
            Date now = TimeUtils.getDate();
            getAccount().charge();
            if (getAccount().getBillingInfo().getBillingType() != 1) {
                TemplateHash esRes = estimateChange(newPlan, periodId, values);
                double total = USFormat.parseDouble(esRes.get("sign_total").toString());
                Bill bill = getAccount().getBill();
                if (bill.paymentNeedsCharge(total)) {
                    throw new HSUserException("resource.credit");
                }
            }
            TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
            InitToken token = new InitToken(newPlan, periodId, typeCounter);
            token.set(getId().getType(), this, values);
            double delta = token.getAmount() - getAmount();
            double maxNumber = token.getMaxNumber();
            if (maxNumber != -1.0d && getTotalAmount() + delta > maxNumber) {
                throw new HSUserException("resource.maxnumber_change", new Object[]{String.valueOf(maxNumber), TypeRegistry.getDescription(getId().getType())});
            }
            if (Session.getResellerId() != 1) {
                Reseller resel = Session.getReseller();
                double maxNumber2 = resel.getPrices(getId().getType()).getMax();
                double newAmount = resel.getTypeCounter().getValue(getId().getType()) + delta;
                if (maxNumber2 != -1.0d && newAmount > maxNumber2) {
                    throw new HSUserException("resource.limit", new Object[]{TypeRegistry.getDescription(getId().getType())});
                }
            }
            BillEntry entry = null;
            try {
                Connection con = Session.getTransConnection();
                try {
                    changeResourceBilling(newPlan, periodId, values);
                    double oldSize = changeResource(newPlan, periodId, values);
                    setNewAmountResource(oldSize);
                    Session.commitTransConnection(con);
                    Session.getLog().info("Charging");
                    getAccount().getBill().charge(getAccount().getBillingInfo());
                    getAccount().sendInvoice(now);
                } catch (Throwable th) {
                    Session.commitTransConnection(con);
                    throw th;
                }
            } catch (Exception e) {
                if (0 != 0) {
                    try {
                        Session.getLog().debug("Canceling billing entry for resource:" + getId());
                        getAccount().getBill().cancel(entry.getId());
                    } catch (Exception e2) {
                        Session.getLog().error("Unable to cancel bill entry for resource :" + getId());
                    }
                }
                Session.getLog().error("Critical error on change resource " + e);
            }
            id = getId();
        }
        return id;
    }

    public ResourceId FM_findChild(String type) throws Exception {
        try {
            return getId().findChild(type);
        } catch (Exception ex) {
            Session.getLog().debug("findChildren error:", ex);
            return null;
        }
    }

    public TemplateModel FM_findChildren(String type) throws Exception {
        try {
            return new TemplateList(getId().findChildren(type, new LinkedList()));
        } catch (Exception ex) {
            Session.getLog().debug("findChildren error:", ex);
            return null;
        }
    }

    public TemplateModel FM_findAllChildren(String type) throws Exception {
        try {
            return new TemplateList(getId().findAllChildren(type, new LinkedList()));
        } catch (Exception ex) {
            Session.getLog().debug("findChildren error:", ex);
            return null;
        }
    }

    public TemplateListModel FM_getChildren() {
        return new ListAdapter(getChildHolder().getChildren());
    }

    public TemplateModel FM_getChildrenSorted(String type) throws Exception {
        try {
            return getId().FM_getChildrenSorted(type);
        } catch (Throwable t) {
            Session.getLog().info("I have to catch IT: ", t);
            return null;
        }
    }

    public TemplateModel FM_getChildren(String type) throws Exception {
        return new ListAdapter(getChildHolder().getChildrenByName(type));
    }

    public ResourceId FM_getChild(String type) throws Exception {
        return getId().FM_getChild(type);
    }

    public void process(Resource r) throws Exception {
        getAccount().getChildManager().addChild(this.f41id, r.getId());
        r.process();
    }

    public void process() throws Exception {
    }

    public ResourceId getId() {
        return this.f41id;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("status".equals(key)) {
                return STATUS_OK;
            }
            if ("id".equals(key)) {
                return getId();
            }
            if ("parent".equals(key)) {
                return getParent();
            }
            if ("addChild".equals(key)) {
                return new AddChild();
            }
            if ("change".equals(key)) {
                return new Change();
            }
            if ("findAllChildrenList".equals(key)) {
                return new ChildFinderByList();
            }
            if ("estimateCreate".equals(key)) {
                return new EstimateCreate();
            }
            if ("estimateCreateCopies".equals(key)) {
                return new EstimateCreateCopies(this);
            }
            if ("estimateCreateMass".equals(key)) {
                return new EstimateCreateMass(this);
            }
            if ("estimateDeleteMass".equals(key)) {
                return new EstimateDeleteMass(this);
            }
            if ("estimateChange".equals(key)) {
                return new EstimateChange();
            }
            if ("delete".equals(key)) {
                return FM_cdelete(0);
            }
            if ("suspended".equals(key)) {
                return new TemplateString(this.suspended);
            }
            if ("deleting".equals(key)) {
                return new TemplateString(this.deleting);
            }
            if ("p_begin".equals(key)) {
                return new TemplateString(DateFormat.getDateInstance(2).format(getPeriodBegin()));
            }
            if ("getSortedChildrenList".equals(key)) {
                return new SortedChildrenLister(getId());
            }
            if ("ext".equals(key)) {
                return new ExtentionObject(this);
            }
            return AccessTemplateMethodWrapper.getMethod(this, key);
        } catch (Exception e) {
            Ticket.create(e, getId());
            return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
        }
    }

    public String getResourcePlanValue(String key) {
        return _getPlan().getResourceType(getId().getType()).getValue(key);
    }

    public ResourceType getResourceType() {
        return _getPlan().getResourceType(getId().getType());
    }

    public String getPlanValue(String key) {
        return _getPlan().getValue(getId().getType(), key);
    }

    public void initDone() throws Exception {
    }

    public void postInitDone() throws Exception {
    }

    public void billingInit() throws Exception {
        getLog().debug("Resource.billingInit " + getClass());
        setPeriodBegin(TimeUtils.dropMinutes(TimeUtils.getDate()));
        setupCharge(getPeriodBegin());
        if (getId().isMonthly()) {
            recurrentCharge(getPeriodBegin(), nextMonthlyBillingDate());
        } else {
            recurrentCharge(getPeriodBegin(), getAccount().getPeriodEnd());
        }
    }

    public void init() throws Exception {
        init("");
    }

    public void init(String modId) throws Exception {
        getLog().info("Resource.init('" + modId + "')" + getClass());
        LinkedList l = new LinkedList();
        LinkedList mod = new LinkedList();
        l.add(this);
        mod.add(modId);
        LinkedList reverseOrderList = new LinkedList();
        while (!l.isEmpty()) {
            Resource current = (Resource) l.getFirst();
            getAccount().incType(current.getId(), current.getAmount());
            current.saveAmount(current.getAmount());
            String modId2 = (String) mod.getFirst();
            Collection<InitResource> resources = _getPlan().getInitResources(current.getId().getType(), modId2);
            if (null != resources) {
                for (InitResource rType : resources) {
                    if (!rType.isDisabled() && !_getPlan().getResourceType(rType.getType()).isDisabled()) {
                        Resource tmp = create(current, rType, null, modId2);
                        current.process(tmp);
                        l.addLast(tmp);
                        mod.add(rType.getMod(modId2));
                    }
                }
            }
            double maxNumber = current.getMaxNumber();
            if (maxNumber != -1.0d && current.getAmount() != 0.0d && current.getTotalAmount() > maxNumber) {
                throw new HSUserException("resource.maxnumber", new Object[]{String.valueOf(maxNumber), TypeRegistry.getDescription(current.getId().getType())});
            }
            if (Session.getResellerId() != 1) {
                Reseller resel = Session.getReseller();
                double maxNumber2 = resel.getPrices(current.getId().getType()).getMax();
                if (maxNumber2 != -1.0d && current.getAmount() != 0.0d && resel.getTypeCounter().getValue(current.getId().getType()) > maxNumber2) {
                    throw new HSUserException("resource.limit", new Object[]{TypeRegistry.getDescription(current.getId().getType())});
                }
            }
            try {
                getLog().debug("Resource.initDone() call initDone:" + current.getClass());
                current.billingInit();
                current.initDone();
                current.initialized = true;
                reverseOrderList.addFirst(current);
                l.removeFirst();
                mod.removeFirst();
            } catch (Exception e) {
                getLog().info("Error initDone", e);
                throw e;
            }
        }
        Iterator i = reverseOrderList.iterator();
        while (i.hasNext()) {
            Resource r = (Resource) i.next();
            try {
                r.postInitDone();
            } catch (Exception ex) {
                if (!(ex instanceof HSUserException)) {
                    getLog().warn("Error in postInitDone on resource: " + r.getDescription(), ex);
                    Ticket.create(ex, this, "postInitDone: rId=" + r.getId());
                }
            }
        }
    }

    public static Resource softGet(ResourceId id) {
        return (Resource) cache.get(id);
    }

    public static Resource get(ResourceId id) throws Exception {
        Resource r;
        try {
            synchronized (id.getLock()) {
                r = (Resource) cache.get(id);
                if (r == null) {
                    Class aClass = _getPlan().getResourceClass(id.getType());
                    Object[] argV = {id};
                    try {
                        r = (Resource) aClass.getConstructor(_argT).newInstance(argV);
                        cache.put(r.getId(), r);
                    } catch (Exception e) {
                        Session.getLog().debug("Exception in Resource.get: " + id, e);
                        return null;
                    }
                }
            }
            id.releaseLock();
            if (Session.getAccount().getId().equals(r.accountId) || Session.getUser().getAccountIds().contains(r.accountId)) {
                return r;
            }
            throw new HSUserException("resource.user", new Object[]{r, Session.getUser().getLogin(), r.getAccount()});
        } finally {
            id.releaseLock();
        }
    }

    public static long getNewId() throws SQLException {
        return Session.getNewIdAsLong();
    }

    public static String getDescription(ResourceId id) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT descr FROM descriptions WHERE resource_id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            String result = rs.next() ? rs.getString(1) : "No description";
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setDescription(String text) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        if (text != null) {
            try {
                if (!"".equals(text)) {
                    PreparedStatement ps2 = con.prepareStatement("UPDATE descriptions SET descr = ? WHERE resource_id = ?");
                    ps2.setString(1, text);
                    ps2.setLong(2, getId().getId());
                    int count = ps2.executeUpdate();
                    if (1 != count) {
                        ps2.close();
                        ps = con.prepareStatement("INSERT INTO descriptions(resource_id, descr) VALUES (?, ?)");
                        ps.setLong(1, getId().getId());
                        ps.setString(2, text);
                        ps.executeUpdate();
                        Session.closeStatement(ps);
                        con.close();
                    }
                    Session.closeStatement(ps2);
                    con.close();
                    return;
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        ps = con.prepareStatement("DELETE FROM  descriptions WHERE resource_id = ?");
        ps.setLong(1, getId().getId());
        ps.executeUpdate();
        Session.closeStatement(ps);
        con.close();
    }

    public static Plan _getPlan() {
        return Session.getAccount().getPlan();
    }

    public Account getAccount() {
        return Session.getAccount();
    }

    public void accessCheck(int mask) throws AccessError {
        if (!Session.getAccessTable().check(getId(), mask)) {
            throw new AccessError(getId() + "[" + mask + "]");
        }
    }

    protected void accessCheck(int type, int mask) throws AccessError {
        if (!Session.getAccessTable().check(getId(), type, mask)) {
            throw new AccessError(getId() + ":" + type + "[" + mask + "]");
        }
    }

    public Date getPeriodBegin() {
        if (this.pBegin != null) {
            return this.pBegin;
        }
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT p_begin FROM parent_child WHERE child_id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.pBegin = rs.getDate(1);
            } else {
                getLog().error("Unable to get pBegin for " + this);
            }
            Session.closeStatement(ps);
            con.close();
            return this.pBegin;
        } catch (SQLException se) {
            getLog().error("Unable to get pBegin for " + this, se);
            return null;
        }
    }

    public void setPeriodBegin(Date begin) {
        if (this.pBegin == null || !this.pBegin.equals(begin)) {
            this.pBegin = begin;
            try {
                Connection con = Session.getDb();
                getLog().info("setPeriodBegin for " + getId());
                PreparedStatement ps = con.prepareStatement("UPDATE parent_child SET p_begin = ? WHERE child_id = ?");
                ps.setDate(1, new java.sql.Date(this.pBegin.getTime()));
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException se) {
                getLog().error("Unable to set billing period begin, resource: " + getId() + ": value:" + this.pBegin, se);
            }
        }
    }

    protected String getRecurrentRefundDescription(Date begin, Date end, double delta, Collection values) throws Exception {
        return getRecurrentRefundDescription(begin, end, delta);
    }

    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return getRecurrentRefundDescription(begin, end);
    }

    private String getResellerSetupRefundDescription(Date begin) {
        return getSetupRefundDescription(begin);
    }

    private String getSetupRefundDescription(Date begin) {
        return Localizer.translateMessage("bill.b_setup_refund", new Object[]{this, f42df.format(begin)});
    }

    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_refund", new Object[]{this, f42df.format(begin), f42df.format(end)});
    }

    protected String getResellerRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return getRecurrentRefundDescription(begin, end);
    }

    public static String getResellerRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.reseller.recurrent", new Object[]{token.getPeriodInWords(), TypeRegistry.getDescription(token.getResourceType().getId()), f42df.format(begin), f42df.format(end)});
    }

    public static String getResellerRecurrentRefundDescription(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.reseller.refund", new Object[]{TypeRegistry.getDescription(token.getResourceType().getId()), f42df.format(begin), f42df.format(end)});
    }

    public String getResellerRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return getRecurrentRefundDescription(begin, end);
    }

    public void setupRefund(Date begin) throws Exception {
        Date now = TimeUtils.getDate();
        synchronized (this) {
            BillEntry entry = Bill.findEntry(getId(), 1);
            if (entry == null) {
                return;
            }
            double amount = -entry.getAmount();
            String description = getSetupRefundDescription(begin);
            String resDesc = null;
            double resAmount = 0.0d;
            if (Session.getResellerId() != 1) {
                resAmount = -entry.getResellerEntry().getAmount();
                resDesc = getResellerSetupRefundDescription(begin);
            }
            if (amount < 0.0d || resAmount < 0.0d) {
                BillEntry be = getAccount().getBill().addEntry(4, now, getId(), description, begin, now, null, amount);
                if (Session.getResellerId() != 1 && resAmount < 0.0d) {
                    be.createResellerEntry(resAmount, resDesc);
                }
            }
            Session.billingLog(amount, description, resAmount, resDesc, "REFUND");
        }
    }

    public BillEntry recurrentRefund(Date begin, Date end) throws Exception {
        BillEntry billEntry;
        getLog().info("Calc refund for " + getId() + "(" + begin + ":" + end + ")");
        Account a = Session.getAccount();
        synchronized (this) {
            List entry = a.getRefundedEntry(getId(), begin, end);
            double amount = calc(4, begin, end, entry);
            if (amount > 0.0d) {
                throw new Exception("Refund fee can't be positive");
            }
            String descr = getRecurrentRefundDescription(begin, end);
            String resDescr = null;
            double resAmount = 0.0d;
            if (Session.getResellerId() != 1) {
                resAmount = resellerCalc(4, begin, end, entry);
            }
            BillEntry be = null;
            if (amount < 0.0d || resAmount < 0.0d) {
                be = getAccount().getBill().addEntry(4, TimeUtils.getDate(), getId(), descr, begin, end, null, amount);
            }
            if (Session.getResellerId() != 1) {
                resDescr = getResellerRecurrentRefundDescription(begin, end);
                if (resAmount < 0.0d) {
                    be.createResellerEntry(resAmount, resDescr);
                }
            }
            Session.billingLog(amount, descr, resAmount, resDescr, "REFUND");
            billEntry = be;
        }
        return billEntry;
    }

    protected String getRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_refund_all", new Object[]{this, f42df.format(begin), f42df.format(end)});
    }

    protected String getResellerRecurrentRefundAllDescription(Date begin, Date end, double delta) throws Exception {
        return getRecurrentRefundAllDescription(begin, end);
    }

    protected String getResellerRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return getRecurrentRefundAllDescription(begin, end);
    }

    public BillEntry recurrentRefundAll(Date begin, Date end) throws Exception {
        BillEntry billEntry;
        getLog().info("Calc refundAll for " + getId() + "(" + begin + ":" + end + ")");
        Account a = Session.getAccount();
        BillEntry be = null;
        synchronized (this) {
            List entry = a.getRefundedEntry(getId(), begin, end);
            double amount = calc(B_REFUND_ALL, begin, end, entry);
            if (amount > 0.0d) {
                throw new Exception("Refund fee can't be positive");
            }
            String descr = getRecurrentRefundAllDescription(begin, end);
            String resDescr = null;
            double resAmount = 0.0d;
            if (Session.getResellerId() != 1) {
                resAmount = resellerCalc(4, begin, end, entry);
            }
            if (amount < 0.0d || resAmount < 0.0d) {
                be = getAccount().getBill().addEntry(4, TimeUtils.getDate(), getId(), descr, begin, end, null, amount);
            }
            if (Session.getResellerId() != 1) {
                resDescr = getResellerRecurrentRefundAllDescription(begin, end);
                if (resAmount < 0.0d) {
                    be.createResellerEntry(resAmount, resDescr);
                }
            }
            Session.billingLog(amount, descr, resAmount, resDescr, "REFUND ALL");
            billEntry = be;
        }
        return billEntry;
    }

    public String getChangePlanPeriodDescription(InitToken token, Date begin, Date end) throws Exception {
        String description = getDescription();
        return Localizer.translateMessage("bill.b_change_plan_period", new Object[]{description, f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{token.getPeriodInWords(), token.getDescription(), f42df.format(begin), f42df.format(end)});
    }

    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return getRecurrentChangeDescripton(token.getStartDate(), token.getEndDate());
    }

    protected String getRecurrentChangeDescripton(Date begin, Date end, double delta) throws Exception {
        return getRecurrentChangeDescripton(begin, end);
    }

    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{getPeriodInWords(), getDescription(), f42df.format(begin), f42df.format(end)});
    }

    protected String getResellerRecurrentChangeDescripton(Date begin, Date end, double delta) throws Exception {
        return getResellerRecurrentChangeDescripton(begin, end);
    }

    public String getResellerRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return getRecurrentChangeDescripton(begin, end);
    }

    public BillEntry addEntry(int type, double amount, String descr, double resAmount, String resDescr, Date begin, Date end) throws Exception {
        Date now = TimeUtils.getDate();
        BillEntry be = null;
        String typeString = "";
        int signMultiplier = 1;
        switch (type) {
            case 1:
                typeString = "SETUP";
                signMultiplier = 1;
                break;
            case 2:
                typeString = "RECURRENT";
                signMultiplier = 1;
                break;
            case 3:
                typeString = "USAGE";
                signMultiplier = 1;
                break;
            case 4:
                typeString = "REFUND";
                signMultiplier = -1;
                break;
            case B_REFUND_ALL /* 104 */:
                typeString = "REFUND_ALL";
                signMultiplier = -1;
                break;
        }
        if (amount * signMultiplier > 0.0d || resAmount * signMultiplier > 0.0d) {
            be = getAccount().getBill().addEntry(type, now, getId(), descr, begin, end, null, amount);
        }
        if (Session.getResellerId() != 1 && be != null && resAmount * signMultiplier > 0.0d) {
            be.createResellerEntry(resAmount, resDescr);
        }
        Session.billingLog(amount, descr, resAmount, resDescr, typeString);
        return be;
    }

    public BillEntry recurrentCharge(Date begin, Date end) throws Exception {
        getLog().info("Calc recurrent for " + getId() + "(" + begin + ":" + end + ")");
        double amount = calc(2, begin, end);
        if (amount < 0.0d) {
            throw new Exception("Recurrent fee can't be negative");
        }
        String descr = getRecurrentChangeDescripton(begin, end);
        String resDescr = null;
        double resAmount = 0.0d;
        if (Session.getResellerId() != 1) {
            resAmount = resellerCalc(2, begin, end);
        }
        BillEntry be = null;
        if (amount > 0.0d || resAmount > 0.0d) {
            be = getAccount().getBill().addEntry(2, TimeUtils.getDate(), getId(), descr, begin, end, null, amount);
        }
        if (Session.getResellerId() != 1) {
            resDescr = getResellerRecurrentChangeDescripton(begin, end);
            if (resAmount > 0.0d) {
                be.createResellerEntry(resAmount, resDescr);
            }
        }
        Session.billingLog(amount, descr, resAmount, resDescr, "RECURRENT CHARGE");
        return be;
    }

    public static String getSetupChargeDescription(InitToken token, Date now) throws Exception {
        return Localizer.translateMessage("bill.b_setup", new Object[]{token.getDescription()});
    }

    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.b_setup", new Object[]{this});
    }

    protected String getResellerSetupChargeDescription(Date now) throws Exception {
        return getSetupChargeDescription(now);
    }

    public BillEntry setupCharge(Date now) throws Exception {
        getLog().info("Calc setup for " + getId() + "(" + now + ")");
        double amount = calc(1, (Date) null, (Date) null);
        String descr = getSetupChargeDescription(now);
        String resDescr = null;
        double resAmount = 0.0d;
        if (Session.getResellerId() != 1) {
            resAmount = resellerCalc(1, (Date) null, (Date) null);
        }
        BillEntry be = null;
        if (amount > 0.0d || resAmount > 0.0d) {
            be = getAccount().getBill().addEntry(1, TimeUtils.getDate(), getId(), descr, now, null, null, amount);
        }
        if (Session.getResellerId() != 1) {
            resDescr = getResellerSetupChargeDescription(now);
            if (resAmount > 0.0d) {
                be.createResellerEntry(resAmount, resDescr);
            }
        }
        Session.billingLog(amount, descr, resAmount, resDescr, "SETUP CHARGE");
        return be;
    }

    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_usage", new Object[]{this, f42df.format(begin), f42df.format(end)});
    }

    public String getResellerUsageChargeDescription(Date begin, Date end) throws Exception {
        return getUsageChargeDescription(begin, end);
    }

    public BillEntry usageCharge(Date end) throws Exception {
        getLog().info("Calc usage for " + getId() + "(" + getPeriodBegin() + ":" + end + ")");
        double amount = calc(3, getPeriodBegin(), end);
        if (amount < 0.0d) {
            throw new Exception("Usage fee can't be negative");
        }
        String descr = getUsageChargeDescription(getPeriodBegin(), end);
        double resAmount = 0.0d;
        String resDescr = null;
        if (Session.getResellerId() != 1) {
            resAmount = resellerCalc(3, getPeriodBegin(), end);
        }
        BillEntry be = null;
        if (amount > 0.0d || resAmount > 0.0d) {
            be = getAccount().getBill().addEntry(3, TimeUtils.getDate(), getId(), descr, getPeriodBegin(), end, null, amount);
        }
        if (Session.getResellerId() != 1) {
            resDescr = getResellerUsageChargeDescription(getPeriodBegin(), end);
            if (resAmount > 0.0d) {
                be.createResellerEntry(resAmount, resDescr);
            }
        }
        Session.billingLog(amount, descr, resAmount, resDescr, "USAGE CHARGE");
        return be;
    }

    public Date nextMonthlyBillingDate() throws Exception {
        return nextMonthlyBillingDate(getPeriodBegin());
    }

    public static Date nextMonthlyBillingDate(Date startDate) throws Exception {
        Calendar next = TimeUtils.getCalendar();
        next.setTime(startDate);
        next.add(2, 1);
        Date nextDate = next.getTime();
        return nextDate;
    }

    public boolean monthlyCharge() throws Exception {
        return monthlyCharge(nextMonthlyBillingDate());
    }

    public boolean monthlyCharge(Date nextDate) throws Exception {
        if (TimeUtils.getDate().before(nextDate)) {
            return false;
        }
        monthlyUsageCharge(nextDate);
        closePeriodMonthlyAction(nextDate);
        setPeriodBegin(nextDate);
        monthlyRecurentCharge(nextDate);
        return true;
    }

    public void monthlyUsageCharge(Date nextDate) throws Exception {
        Session.getLog().debug("Calc monthly usage for " + toString());
        usageCharge(nextDate);
    }

    public void monthlyRecurentCharge(Date nextDate) throws Exception {
        boolean message = false;
        if (Session.getBillingNote() == null || "".equals(Session.getBillingNote())) {
            message = true;
        }
        if (message) {
            try {
                Session.setBillingNote(Localizer.translateMessage("bill.open_new_montly_period", new Object[]{f42df.format(nextDate), f42df.format(nextMonthlyBillingDate())}));
            } catch (Throwable th) {
                if (message) {
                    Session.setBillingNote("");
                }
                throw th;
            }
        }
        changeResourceBilling(_getPlan(), getAccount().getPeriodId(), getCurrentInitValues());
        if (message) {
            Session.setBillingNote("");
        }
    }

    public void openPeriodMonthlyAction(Date nextDate) throws Exception {
        monthlyAction(nextDate);
    }

    public void closePeriodMonthlyAction(Date nextDate) throws Exception {
        monthlyAction(nextDate);
    }

    public void monthlyAction(Date nextDate) throws Exception {
    }

    public double getAmount() {
        return 1.0d;
    }

    public void saveAmount(double amount) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO resource_amount(id, amount) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setDouble(2, amount);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void removeAmount() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM resource_amount WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public double getTotalAmount() {
        return getAccount().getTypeCounter().getValue(getId().getType());
    }

    public static double getTotalAmount(InitToken token) {
        return token.getCurrentAmount();
    }

    public double getRecurrentMultiplier() {
        return getFreeMultiplier();
    }

    public double getUsageMultiplier() {
        return getFreeMultiplier();
    }

    public double getSetupMultiplier() {
        return getFreeMultiplier() > 0.0d ? 1.0d : 0.0d;
    }

    public double getMaxNumber() throws Exception {
        try {
            String maxNumberS = _getPlan().getResourceType(getId().getType()).getValue("_MAX");
            return USFormat.parseDouble(maxNumberS);
        } catch (NullPointerException e) {
            return -1.0d;
        } catch (NumberFormatException e2) {
            return -1.0d;
        }
    }

    public double getFreeNumber() {
        String freeNumberS = getResourcePlanValue("_FREE_UNITS_" + getAccount().getPeriodId());
        if (freeNumberS == null) {
            freeNumberS = getResourcePlanValue("_FREE_UNITS_");
        }
        if (freeNumberS == null) {
            return 0.0d;
        }
        try {
            double freeNumber = USFormat.parseDouble(freeNumberS);
            return freeNumber;
        } catch (ParseException ne) {
            Session.getLog().debug("Illegal FREE_UNITS =" + getAccount().getPeriodId(), ne);
            return 0.0d;
        }
    }

    public double getFreeMultiplier() {
        double newAmount = getAccount().getTypeCounter().getValue(getId().getType());
        double toCharge = Math.min(newAmount - getFreeNumber(), getAmount());
        if (toCharge > 0.0d) {
            return toCharge;
        }
        Session.getLog().debug("Resource " + getId().getType() + " has been purchased for free");
        return 0.0d;
    }

    public static double calc(InitToken token, int type) {
        return calc(token, type, (List) null);
    }

    public static double calc(InitToken token, int type, List en) {
        Method calc;
        Object[] params2;
        Session.getLog().info("BillEntry" + en);
        String className = null;
        switch (type) {
            case 1:
                className = token.getResourceType().getValue("_SETUP_CALC");
                break;
            case 2:
                className = token.getResourceType().getValue("_RECURRENT_CALC");
                break;
            case 3:
                className = token.getResourceType().getValue("_USAGE_CALC");
                break;
            case 4:
                className = token.getResourceType().getValue("_REFUND_CALC");
                break;
            case 10:
                className = token.getPlan().getValue("MONEY_BACK_CALC");
                break;
            case B_REFUND_ALL /* 104 */:
                className = token.getResourceType().getValue("_REFUND_CALC");
                if (className != null && className.length() > 0) {
                    className = className + "_100";
                    break;
                }
                break;
        }
        if (className != null) {
            try {
                if (className.length() == 0) {
                    return 0.0d;
                }
                Class c = Class.forName(className);
                if (en == null) {
                    calc = c.getMethod("calc", tokenBillingParams);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{token};
                } else {
                    calc = c.getMethod("calc", tokenBillingParamsEntry);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{token, (BillEntry) en};
                }
                Double result = (Double) calc.invoke(null, params2);
                return fix(result.doubleValue());
            } catch (InvocationTargetException iex) {
                getLog().error("Erro during calling calc:" + className + ":", iex instanceof InvocationTargetException ? iex.getTargetException() : iex);
                return 0.0d;
            } catch (Exception e) {
                getLog().error("error using calc ", e);
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public double calc(int type, Date begin, Date end) {
        return calc(type, begin, end, null);
    }

    public double calc(int type, Date begin, Date end, List en) {
        Method calc;
        Object[] params2;
        Session.getLog().info("BillEntry" + en);
        String className = null;
        switch (type) {
            case 1:
                className = getPlanValue("_SETUP_CALC");
                break;
            case 2:
                className = getPlanValue("_RECURRENT_CALC");
                break;
            case 3:
                className = getPlanValue("_USAGE_CALC");
                break;
            case 4:
                className = getPlanValue("_REFUND_CALC");
                break;
            case 10:
                className = getAccount().getPlan().getValue("MONEY_BACK_CALC");
                break;
            case B_REFUND_ALL /* 104 */:
                className = getPlanValue("_REFUND_CALC");
                if (className != null && className.length() > 0) {
                    className = className + "_100";
                    break;
                }
                break;
        }
        if (className != null) {
            try {
                if (className.length() == 0) {
                    return 0.0d;
                }
                Class c = Class.forName(className);
                if (en == null) {
                    calc = c.getMethod("calc", billingParams);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{this, begin, end};
                } else {
                    calc = c.getMethod("calc", billingParamsEntry);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{this, begin, end, en};
                }
                Double result = (Double) calc.invoke(null, params2);
                return fix(result.doubleValue());
            } catch (InvocationTargetException iex) {
                getLog().error("Erro during calling calc:" + className + ":", iex instanceof InvocationTargetException ? iex.getTargetException() : iex);
                return 0.0d;
            } catch (Exception e) {
                getLog().error("Error in the method calc: ", e instanceof InvocationTargetException ? e.getCause() : e);
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public double getResellerRecurrentMultiplier() {
        return getResellerFreeMultiplier();
    }

    public static double getResellerRecurrentMultiplier(InitToken token) throws Exception {
        return getResellerFreeMultiplier(token);
    }

    public static double getResellerFreeMultiplier(InitToken token) {
        try {
            Reseller resel = Session.getReseller();
            resel.getPrices(token.getResourceType().getType());
            double toCharge = 0.0d;
            if (token.getRes() != null) {
                try {
                    toCharge = resel.getTypeCounter().getBillableValueEstimate(token.getRes().getId(), token.getRes().getAmount(), token.getAmount());
                } catch (Exception ex) {
                    Session.getLog().error("Unable to get new amount of resource:" + token.getRes().getId(), ex);
                    toCharge = 0.0d;
                }
            }
            if (toCharge > 0.0d) {
                Session.getLog().debug("Resource " + token.getResourceType().getType() + " is paybel " + toCharge + "(for reseller)");
                return toCharge;
            }
            Session.getLog().debug("Resource " + token.getResourceType().getType() + " has been purchased for free(for reseller)");
            return 0.0d;
        } catch (Exception e) {
            Session.getLog().error("Problem in reseller multiplier ", e);
            return 0.0d;
        }
    }

    public double getResellerUsageMultiplier() {
        return getResellerFreeMultiplier();
    }

    public double getResellerSetupMultiplier() {
        return getResellerFreeMultiplier() > 0.0d ? 1.0d : 0.0d;
    }

    protected double getResellerFreeMultiplier() {
        try {
            Reseller resel = Session.getReseller();
            resel.getPrices(getId().getType());
            double toCharge = resel.getTypeCounter().getBillableValue(getId(), getAmount());
            if (toCharge > 0.0d) {
                Session.getLog().debug("Resource " + getId().getType() + " is paybel " + toCharge + "(for reseller)");
                return toCharge;
            }
            Session.getLog().debug("Resource " + getId().getType() + " has been purchased for free(for reseller)");
            return 0.0d;
        } catch (Exception e) {
            Session.getLog().error("Problem in reseller multiplier ", e);
            return 0.0d;
        }
    }

    public double resellerCalc(int type, Date begin, Date end) throws Exception {
        return resellerCalc(type, begin, end, null);
    }

    public static double resellerCalc(InitToken token, int type) throws Exception {
        return resellerCalc(token, type, (List) null);
    }

    public static double resellerCalc(InitToken token, int type, List en) throws Exception {
        Method calc;
        Object[] params2;
        String className = null;
        ResellerPrices prices = Session.getReseller().getPrices(token.getResourceType().getType());
        switch (type) {
            case 1:
                className = prices.getSetupCalc();
                break;
            case 2:
                className = prices.getRecurrentCalc();
                break;
            case 3:
                className = prices.getUsageCalc();
                break;
            case 4:
                className = prices.getRefundCalc();
                break;
            case B_REFUND_ALL /* 104 */:
                className = prices.getRefundCalc();
                if (className != null && className.length() > 0) {
                    className = className + "_100";
                    break;
                }
                break;
        }
        if (className != null) {
            try {
                if (className.length() == 0) {
                    return 0.0d;
                }
                Class c = Class.forName(className);
                if (en == null) {
                    calc = c.getMethod("calc", tokenBillingParams);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{token};
                } else {
                    calc = c.getMethod("calc", tokenBillingParamsEntry);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{token, en};
                }
                Double result = (Double) calc.invoke(null, params2);
                return fix(result.doubleValue());
            } catch (Exception e) {
                getLog().error("error using calc ", e);
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public double resellerCalc(int type, Date begin, Date end, List en) throws Exception {
        Method calc;
        Object[] params2;
        String className = null;
        ResellerPrices prices = Session.getReseller().getPrices(getId().getType());
        switch (type) {
            case 1:
                className = prices.getSetupCalc();
                break;
            case 2:
                className = prices.getRecurrentCalc();
                break;
            case 3:
                className = prices.getUsageCalc();
                break;
            case 4:
                className = prices.getRefundCalc();
                break;
            case B_REFUND_ALL /* 104 */:
                className = prices.getRefundCalc();
                if (className != null && className.length() > 0) {
                    className = className + "_100";
                    break;
                }
                break;
        }
        if (className != null) {
            try {
                if (className.length() == 0) {
                    return 0.0d;
                }
                Class c = Class.forName(className);
                if (en == null) {
                    calc = c.getMethod("calc", billingParams);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{this, begin, end};
                } else {
                    calc = c.getMethod("calc", billingParamsEntry);
                    getLog().info("Classname : " + className + "->" + calc);
                    params2 = new Object[]{this, begin, end, en};
                }
                Double result = (Double) calc.invoke(null, params2);
                return fix(result.doubleValue());
            } catch (Exception e) {
                getLog().error("error using calc ", e);
                return 0.0d;
            }
        }
        return 0.0d;
    }

    protected double estimateRecurent() throws Exception {
        Account a = Session.getAccount();
        List entry = a.getRefundedEntry(getId(), TimeUtils.getDate(), getAccount().getPeriodEnd());
        return calc(4, TimeUtils.getDate(), getAccount().getPeriodEnd(), entry);
    }

    protected double estimateRefund() throws Exception {
        Account a = Session.getAccount();
        if (!getId().isMonthly()) {
            List entry = a.getRefundedEntry(getId(), TimeUtils.getDate(), getAccount().getPeriodEnd());
            return calc(4, TimeUtils.getDate(), getAccount().getPeriodEnd(), entry);
        }
        List entry2 = a.getRefundedEntry(getId(), getPeriodBegin(), nextMonthlyBillingDate());
        return calc(4, getPeriodBegin(), nextMonthlyBillingDate(), entry2);
    }

    public double estimateRefund(InitToken token) throws Exception {
        Account a = Session.getAccount();
        if (!getId().isMonthly()) {
            List entry = a.getRefundedEntry(getId(), TimeUtils.getDate(), token.getEndDate());
            return calc(4, TimeUtils.getDate(), token.getEndDate(), entry);
        }
        List entry2 = a.getRefundedEntry(getId(), token.getStartDate(), nextMonthlyBillingDate(token.getStartDate()));
        return calc(4, token.getStartDate(), nextMonthlyBillingDate(token.getStartDate()), entry2);
    }

    protected double estimateRefundAll() throws Exception {
        Account a = Session.getAccount();
        if (!getId().isMonthly()) {
            List entry = a.getRefundedEntry(getId(), TimeUtils.getDate(), getAccount().getPeriodEnd());
            return calc(B_REFUND_ALL, TimeUtils.getDate(), getAccount().getPeriodEnd(), entry);
        }
        List entry2 = a.getRefundedEntry(getId(), getPeriodBegin(), nextMonthlyBillingDate());
        return calc(B_REFUND_ALL, getPeriodBegin(), nextMonthlyBillingDate(), entry2);
    }

    public double estimateUsage() throws Exception {
        return calc(3, getPeriodBegin(), TimeUtils.getDate());
    }

    protected double estimateSetup() throws Exception {
        return calc(1, (Date) null, (Date) null);
    }

    public static double getSetupMultiplier(InitToken token) throws Exception {
        return (token.getCurrentAmount() <= token.getFreeUnits() || token.getAmount() <= 0.0d) ? 0.0d : 1.0d;
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        return (token.getCurrentAmount() <= token.getFreeUnits() || token.getAmount() <= 0.0d) ? 0.0d : 1.0d;
    }

    public static double getAmount(InitToken token) {
        return 1.0d;
    }

    public static double getFreeUnits(InitToken token) throws Exception {
        String freeUnits = token.getResourceType().getValue("_FREE_UNITS_" + token.getPeriodId());
        if (freeUnits == null) {
            freeUnits = token.getResourceType().getValue("_FREE_UNITS_");
        }
        if (freeUnits == null) {
            return 0.0d;
        }
        try {
            return USFormat.parseDouble(freeUnits);
        } catch (NumberFormatException ne) {
            Session.getLog().debug("Illegal FREE_UNITS =" + token.getPeriodId(), ne);
            return 0.0d;
        }
    }

    public static double getMaxNumber(InitToken token) throws Exception {
        try {
            String maxNumberS = token.getResourceType().getValue("_MAX");
            return USFormat.parseDouble(maxNumberS);
        } catch (NullPointerException e) {
            return -1.0d;
        } catch (NumberFormatException e2) {
            return -1.0d;
        }
    }

    public static double calc(String className, InitToken token) throws Exception {
        Class c = Class.forName(className);
        Method calc = c.getMethod("calc", params);
        Object[] values = {token};
        try {
            Double result = (Double) calc.invoke(null, values);
            return Math.rint(result.doubleValue() * 100.0d) / 100.0d;
        } catch (InvocationTargetException ex) {
            throw ((Exception) ex.getTargetException());
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Resource$EstimateCreate.class */
    public class EstimateCreate implements TemplateMethodModel {
        EstimateCreate() {
            Resource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            TemplateModel estimateCreate;
            try {
                l = HTMLEncoder.decode(l);
                synchronized (Session.getAccount()) {
                    estimateCreate = Resource.this.estimateCreate((String) l.get(0), (String) l.get(1), l.subList(2, l.size()));
                }
                return estimateCreate;
            } catch (Exception t) {
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error estimating price: " + l, t);
                Ticket.create(t, this, "EstimateCreate: type = " + ((String) l.get(0)) + ", mod = " + ((String) l.get(1)) + ", params = " + l.subList(2, l.size()));
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Resource$EstimateChange.class */
    public class EstimateChange implements TemplateMethodModel {
        EstimateChange() {
            Resource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            TemplateModel estimateChange;
            try {
                l = HTMLEncoder.decode(l);
                synchronized (Session.getAccount()) {
                    estimateChange = Resource.this.estimateChange(l);
                }
                return estimateChange;
            } catch (Exception t) {
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                Resource.getLog().warn("Error estimating price: " + l, t);
                Ticket.create(t, this, "EstimateCreate: type = " + ((String) l.get(0)) + ", mod = " + ((String) l.get(1)) + ", params = " + l.subList(2, l.size()));
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
    }

    public TemplateModel estimateCreate(String type, String modId, Collection values) throws Exception {
        TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
        return estimateCreate(typeCounter, type, modId, values);
    }

    public TemplateModel estimateCreate(TypeCounter typeCounter, String type, String modId, Collection values) throws Exception {
        Collection initValues;
        Date now = TimeUtils.getDate();
        getAccount().charge();
        getAccount().sendInvoice(now);
        Hashtable res = new Hashtable();
        int periodId = getAccount().getPeriodId();
        Plan plan = getAccount().getPlan();
        Date start = TimeUtils.dropMinutes(TimeUtils.getDate());
        Date end = getAccount().getPeriodEnd();
        long periodSize = getAccount().getPeriodSize();
        InitToken token = new InitToken(plan, periodId, typeCounter);
        LinkedList l = new LinkedList();
        LinkedList mod = new LinkedList();
        int typeId = Integer.parseInt(TypeRegistry.getTypeId(type));
        InitResource rType = new InitResource(typeId, modId, 0);
        l.add(rType);
        mod.add(rType.getMod(modId));
        List result = new LinkedList();
        double setupTotal = 0.0d;
        double recurrTotal = 0.0d;
        double recurrTotalAll = 0.0d;
        double discountTotal = 0.0d;
        while (!l.isEmpty()) {
            InitResource current = (InitResource) l.getFirst();
            String modId2 = (String) mod.getFirst();
            if (typeId == current.getType() && values != null && !values.isEmpty()) {
                initValues = values;
            } else {
                initValues = plan.getDefaultInitValues(token, current.getType(), current.getMod(modId2));
            }
            token.setRange(start, end, periodSize);
            token.set(current.getType(), initValues);
            typeCounter.inc(current.getType(), token.getAmount());
            Collection<InitResource> resources = plan.getInitResources(current.getType(), current.getMod(modId2));
            if (null != resources) {
                for (InitResource rType2 : resources) {
                    if (!rType2.isDisabled() && (plan.getResourceType(rType2.getType()) == null || !plan.getResourceType(rType2.getType()).isDisabled())) {
                        l.addLast(rType2);
                        mod.add(rType2.getMod(modId2));
                    }
                }
            }
            String setupCalc = plan.getValue(current.getType(), "_SETUP_CALC");
            String recurrentCalc = plan.getValue(current.getType(), "_RECURRENT_CALC");
            if (setupCalc != null && setupCalc.length() == 0) {
                setupCalc = null;
            }
            if (recurrentCalc != null && recurrentCalc.length() == 0) {
                recurrentCalc = null;
            }
            if (setupCalc != null || recurrentCalc != null) {
                if (setupCalc != null) {
                    double setupFee = calc(setupCalc, token);
                    if (setupFee != 0.0d && !Double.isNaN(setupFee)) {
                        StringBuffer discountComment = new StringBuffer();
                        double discount = calculatePromoDiscount(1, setupFee, discountComment, current.getType());
                        double setupFee2 = setupFee - discount;
                        discountTotal += discount;
                        result.add(new InvoiceEntry(1, token.getSetupChargeDescription(start) + discountComment.toString(), setupFee2));
                        setupTotal += setupFee2;
                    }
                }
                if (recurrentCalc != null) {
                    double rFee = calc(recurrentCalc, token);
                    if (rFee != 0.0d && !Double.isNaN(rFee)) {
                        StringBuffer discountComment2 = new StringBuffer();
                        double discount2 = calculatePromoDiscount(2, rFee, discountComment2, current.getType());
                        double rFee2 = rFee - discount2;
                        discountTotal += discount2;
                        result.add(new InvoiceEntry(2, token.getRecurrentChangeDescripton(start, end) + discountComment2.toString(), rFee2));
                        recurrTotal += rFee2;
                    }
                    token.setRange(getAccount().getPeriodBegin(), end, periodSize);
                    double rFee3 = calc(recurrentCalc, token);
                    if (rFee3 != 0.0d && !Double.isNaN(rFee3)) {
                        recurrTotalAll += rFee3;
                    }
                }
            }
            l.removeFirst();
            mod.removeFirst();
        }
        double setupTotal2 = fix(setupTotal);
        double recurrTotal2 = fix(recurrTotal);
        double recurrTotalAll2 = fix(recurrTotalAll);
        double total = setupTotal2 + recurrTotal2;
        Bill bill = getAccount().getBill();
        if (getAccount().getBillingInfo().getBillingType() != 1 && bill.paymentNeedsCharge(total)) {
            throw new HSUserException("resource.credit");
        }
        res.put("entries", new TemplateList(result));
        res.put("total", total > 0.0d ? USFormat.format(total) : "0");
        res.put("setup", setupTotal2 > 0.0d ? USFormat.format(setupTotal2) : "0");
        res.put("recurrent", recurrTotal2 > 0.0d ? USFormat.format(recurrTotal2) : "0");
        res.put("recurrentAll", recurrTotalAll2 > 0.0d ? USFormat.format(recurrTotalAll2) : "0");
        res.put("free", (setupTotal2 == 0.0d && recurrTotalAll2 == 0.0d) ? "1" : "0");
        res.put("discount", discountTotal > 0.0d ? USFormat.format(discountTotal) : "0");
        res.put("creditLimit", new Double(bill.getCredit()).toString());
        res.put("balance", new Double(bill.getBalance()).toString());
        return new TemplateHash(res);
    }

    public TemplateModel FM_estimateDelete() throws Exception {
        TemplateModel estimateDelete;
        synchronized (Session.getAccount()) {
            estimateDelete = estimateDelete();
        }
        return estimateDelete;
    }

    public double toChange(Plan newPlan, int periodId, Collection values) {
        Date start;
        Date end;
        try {
            Date now = TimeUtils.getDate();
            InitToken baseToken = getTokenByPlanPeriod(newPlan, periodId);
            TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
            InitToken token = new InitToken(newPlan, periodId, typeCounter);
            token.set(getId().getType(), this, values);
            if (getId().isMonthly()) {
                start = new Date(Math.max(baseToken.getStartDate().getTime(), getPeriodBegin().getTime()));
                end = nextMonthlyBillingDate(start);
            } else {
                start = now;
                end = baseToken.getEndDate();
            }
            token.setRange(start, end, baseToken.getPeriodSize());
            double oldToPay = getRecurrentMultiplier();
            double newToPay = token.getRecurrentMultiplier();
            return newToPay - oldToPay;
        } catch (Exception ex) {
            Session.getLog().error("Unable to claculate to pay", ex);
            return 0.0d;
        }
    }

    public double inUse() {
        return getAmount();
    }

    protected TemplateModel estimateChange(Collection values) throws Exception {
        return estimateChange(_getPlan(), getAccount().getPeriodId(), values);
    }

    public InitToken getTokenByPlanPeriod(Plan newPlan, int periodId) throws Exception {
        Date now = TimeUtils.dropMinutes(TimeUtils.getDate());
        TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
        InitToken token = new InitToken(newPlan, periodId, typeCounter);
        if (newPlan.getNextPaymentDate(getAccount().getPeriodBegin(), periodId).after(now)) {
            token.setRange(getAccount().getPeriodBegin(), newPlan.getNextPaymentDate(getAccount().getPeriodBegin(), periodId));
        } else {
            token.setRange(now, newPlan.getNextPaymentDate(now, periodId));
        }
        return token;
    }

    protected TemplateModel estimateChange(Plan newPlan, int periodId, Collection values) throws Exception {
        Date start;
        Date refundStart;
        Date end;
        Date refundEnd;
        String refundDscr;
        String recurrentDescr;
        TemplateHash res = new TemplateHash();
        Date now = TimeUtils.dropMinutes(TimeUtils.getDate());
        InitToken baseToken = getTokenByPlanPeriod(newPlan, periodId);
        getAccount().charge(false);
        getAccount().sendInvoice(now);
        List result = new LinkedList();
        double total = 0.0d;
        double discountTotal = 0.0d;
        TypeCounter typeCounter = new TypeCounter(getAccount().getTypeCounter());
        InitToken token = new InitToken(newPlan, periodId, typeCounter);
        InitToken refundToken = new InitToken(newPlan, periodId, typeCounter);
        token.set(getId().getType(), this, values);
        refundToken.set(getId().getType(), this, values);
        if (getId().isMonthly()) {
            start = baseToken.getStartDate();
            refundStart = getPeriodBegin();
            end = nextMonthlyBillingDate(start);
            refundEnd = nextMonthlyBillingDate(refundStart);
        } else {
            start = now;
            refundStart = now;
            end = baseToken.getEndDate();
            refundEnd = getAccount().getPeriodEnd();
        }
        token.setRange(start, end, baseToken.getPeriodSize());
        refundToken.setRange(refundStart, refundEnd, baseToken.getPeriodSize());
        try {
            double delta = toChange(newPlan, periodId, values);
            double recurrent = calc(token, 2);
            double refund = estimateRefund(refundToken);
            double recurentWithRefund = recurrent + refund;
            if (recurentWithRefund > 0.0d) {
                if (recurentWithRefund > 0.0d) {
                    if (delta != 0.0d) {
                        recurrentDescr = getRecurrentChangeDescripton(token, delta);
                    } else if (token.getPlan().getId() == _getPlan().getId() && token.getPeriodId() == getAccount().getPeriodId()) {
                        recurrentDescr = token.getRecurrentChangeDescripton(start, end);
                    } else {
                        recurrentDescr = getChangePlanPeriodDescription(token, start, end);
                    }
                    StringBuffer discountComment = new StringBuffer();
                    double discount = calculatePromoDiscount(newPlan, 2, recurrent, discountComment, getId().getType());
                    double recurentWithRefund2 = recurentWithRefund - discount;
                    discountTotal = 0.0d + discount;
                    total = recurentWithRefund2;
                    result.add(new InvoiceEntry(2, recurrentDescr + discountComment.toString(), recurentWithRefund2));
                }
            } else if (delta < 0.0d) {
                total = recurentWithRefund;
                if (recurentWithRefund < 0.0d) {
                    if (delta != 0.0d) {
                        refundDscr = getRecurrentRefundDescription(start, end, delta);
                    } else if (token.getPlan().getId() == _getPlan().getId() && token.getPeriodId() == getAccount().getPeriodId()) {
                        refundDscr = token.getRecurrentChangeDescripton(start, end);
                    } else {
                        refundDscr = getChangePlanPeriodDescription(token, start, end);
                    }
                    result.add(new InvoiceEntry(4, refundDscr, -recurentWithRefund));
                }
            }
            if (baseToken.getStartDate().after(getPeriodBegin())) {
                double usage = estimateUsage();
                if (usage > 0.0d) {
                    StringBuffer discountComment2 = new StringBuffer();
                    double discount2 = calculatePromoDiscount(newPlan, 3, usage, discountComment2, getId().getType());
                    double usage2 = usage - discount2;
                    discountTotal += discount2;
                    result.add(new InvoiceEntry(3, getUsageChargeDescription(getPeriodBegin(), now) + discountComment2.toString(), usage2));
                    total += usage2;
                }
            }
            if (getAccount().getBillingInfo().getBillingType() != 1) {
                Bill bill = getAccount().getBill();
                if (bill.paymentNeedsCharge(total)) {
                    throw new HSUserException("resource.credit");
                }
            }
            res.put("entries", new TemplateList(result));
            if (total > 0.0d) {
                res.put("total", USFormat.format(total));
            } else if (total < 0.0d) {
                res.put("total", USFormat.format(-total));
            } else {
                res.put("total", "0");
            }
            res.put("sign_total", USFormat.format(total));
            res.put("recurrent", recurrent > 0.0d ? USFormat.format(recurrent) : "0");
            res.put("refund", refund < 0.0d ? USFormat.format(-refund) : "0");
            res.put("discount", discountTotal > 0.0d ? USFormat.format(discountTotal) : "0");
            res.put("free", (recurrent == 0.0d && refund == 0.0d) ? "1" : "0");
            res.put("status", "OK");
            return new TemplateHash(res);
        } catch (Throwable th) {
            if (getAccount().getBillingInfo().getBillingType() != 1) {
                Bill bill2 = getAccount().getBill();
                if (bill2.paymentNeedsCharge(total)) {
                    throw new HSUserException("resource.credit");
                }
            }
            throw th;
        }
    }

    public static double fix(double val) {
        return Math.rint(val * 100.0d) / 100.0d;
    }

    public String toString() {
        try {
            return "[" + TypeRegistry.getType(getId().getType()) + "]";
        } catch (NoSuchTypeException e) {
            return getId().toString();
        }
    }

    public boolean isSuspended() {
        return this.suspended;
    }

    public boolean isDeletePrev() {
        return this.deletePrev;
    }

    public String getDescription() throws Exception {
        return Localizer.translateMessage(TypeRegistry.getType(getId().getType()) + ".desc", getDescriptionParams());
    }

    protected Object[] getDescriptionParams() throws Exception {
        return emptyDescriptionParams;
    }

    public static String getDescription(InitToken token) throws Exception {
        return TypeRegistry.getDescription(token.getResourceType().getId());
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void removeFromCache() {
        if (!locked()) {
            cache.remove(getId());
        }
    }

    public ContentMoveItem initContentMove(long srcHostId, long targetHostId) throws Exception {
        return null;
    }

    public boolean hasCMI() {
        return false;
    }

    public long getTTL() {
        return TypeRegistry.getTTL(Integer.toString(getId().getType()));
    }

    public static Comparator getComparator() {
        return null;
    }

    public static Comparator getComparator(String type) throws Exception {
        Class c = _getPlan().getResourceClassByName(type);
        return (Comparator) c.getMethod("getComparator", null).invoke(null, null);
    }

    public Class getResourceTransportClass() {
        return null;
    }

    public Collection getTransportData() throws Exception {
        return new ArrayList();
    }

    public static double calculatePromoDiscount(int entryType, double amount, StringBuffer buf, int resourceType) {
        return calculatePromoDiscount(Session.getAccount().getPlan(), entryType, amount, buf, resourceType);
    }

    public static double calculatePromoDiscount(Plan targetPlan, int entryType, double amount, StringBuffer buf, int resourceType) {
        return calculatePromoDiscount(targetPlan, entryType, amount, buf, Session.getAccount().getPreferences().getPromoCode(), resourceType);
    }

    public static double calculatePromoDiscount(Plan targetPlan, int entryType, double amount, StringBuffer buf, String promoCode, int resourceType) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        DecimalFormat df1 = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        dfs.setCurrencySymbol("");
        df.setDecimalFormatSymbols(dfs);
        df1.setDecimalFormatSymbols(dfs);
        df1.setMinimumFractionDigits(4);
        df1.setMaximumFractionDigits(4);
        double result = 0.0d;
        StringBuffer stb = new StringBuffer();
        List<Promo> activePlanPromotions = targetPlan.getAppliablePromotions(promoCode);
        Session.getLog().debug("Inside Resource::calculatePromoDiscount; promoCode = " + promoCode + " plan=" + targetPlan.getId() + " passed amount=" + amount + " got  " + activePlanPromotions.size() + " active plan promotions");
        for (Promo p : activePlanPromotions) {
            Session.getLog().debug("Promo with id " + p.getId() + " is " + (p.isPromoValid((long) entryType, Session.getAccount(), resourceType) ? "" : " not ") + " valid");
            if (p.isPromoValid(entryType, Session.getAccount(), resourceType)) {
                try {
                    double dsc = p.calcDiscount(Session.getAccount(), amount);
                    Session.getLog().debug("Discount hs been claculated; amount =  " + amount + " discount = " + dsc);
                    stb.append(p.getBillingDescr()).append(df1.format(dsc)).append('\n');
                    result += dsc;
                } catch (Exception ex) {
                    Session.getLog().debug("Error applying promo with id " + p.getId(), ex);
                }
            }
        }
        if (activePlanPromotions.size() > 0 && result > 0.0d) {
            buf.append('\n').append(Localizer.translateMessage("bill.full_amount", new Object[]{df.format(amount)}));
        }
        if (activePlanPromotions.size() > 1 && result > 0.0d) {
            buf.append('\n');
            buf.append(Localizer.translateMessage("bill.summary_entry_discount", new Object[]{df.format(result)}));
        }
        if (result > 0.0d) {
            buf.append('\n').append(stb.toString());
        }
        return Math.rint(result * 100.0d) / 100.0d;
    }

    public Collection getCurrentInitValues() {
        List values = new ArrayList();
        values.add(Double.toString(getAmount()));
        return values;
    }

    public TemplateModel FM_createComplexEstimator() {
        ComplexEstimator ce;
        synchronized (this) {
            ce = ComplexEstimatorManager.createComplexEstimator();
        }
        return ce;
    }

    public TemplateModel FM_getComplexEstimator(String estimatorId) throws Exception {
        ComplexEstimator complexEstimator;
        synchronized (this) {
            ComplexEstimatorManager cem = (ComplexEstimatorManager) Session.getCacheFactory().getCache(ComplexEstimator.class);
            complexEstimator = cem.getComplexEstimator(estimatorId);
        }
        return complexEstimator;
    }
}
