package psoft.hsphere;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.plan.ResourceType;

/* loaded from: hsphere.zip:psoft/hsphere/InitToken.class */
public class InitToken implements Cloneable {
    protected Plan plan;
    protected int periodId;

    /* renamed from: rt */
    protected ResourceType f35rt;
    protected Resource res;
    protected Collection values;
    protected TypeCounter typeCounter;
    protected Date start;
    protected Date end;
    protected long size;
    protected static Class[] params = {InitToken.class};

    public Resource getRes() {
        return this.res;
    }

    public TypeCounter getTypeCounter() {
        return this.typeCounter;
    }

    public InitToken(InitToken token) {
        this.res = null;
        this.plan = token.getPlan();
        this.periodId = token.getPeriodId();
        this.typeCounter = token.getTypeCounter();
        this.res = token.getRes();
        this.f35rt = token.getResourceType();
        this.values = token.getValues();
        setRange(token.getStartDate(), token.getEndDate(), token.getPeriodSize());
    }

    public InitToken(Plan p, int periodId, TypeCounter typeCounter) {
        this.res = null;
        this.plan = p;
        this.periodId = periodId;
        this.typeCounter = typeCounter;
    }

    public InitToken(Account a) {
        this.res = null;
        this.plan = a.getPlan();
        this.periodId = a.getPeriodId();
        this.typeCounter = new TypeCounter(a.getTypeCounter());
    }

    public InitToken(Account a, TypeCounter typeCounter) {
        this.res = null;
        this.plan = a.getPlan();
        this.periodId = a.getPeriodId();
        this.typeCounter = typeCounter;
    }

    public void setRange(Date start, Date end) {
        setRange(start, end, end.getTime() - start.getTime());
    }

    public void setRange(Date start, Date end, long size) {
        this.start = start;
        this.end = end;
        this.size = size;
    }

    public void set(int type, Collection values) {
        this.f35rt = this.plan.getResourceType(type);
        if (this.f35rt == null) {
            try {
                Session.getLog().error("Type is not supported: " + TypeRegistry.getDescription(type));
            } catch (NoSuchTypeException e) {
                Session.getLog().error("Type: " + type, e);
            }
        }
        this.values = values;
        this.res = null;
    }

    public void set(int type, Resource res) {
        this.f35rt = this.plan.getResourceType(type);
        this.res = res;
    }

    public void set(int type, Resource res, Collection values) {
        this.f35rt = this.plan.getResourceType(type);
        this.res = res;
        this.values = values;
    }

    public Plan getPlan() {
        return this.plan;
    }

    public int getPeriodId() {
        return this.periodId;
    }

    public ResourceType getResourceType() {
        return this.f35rt;
    }

    public double getCurrentAmount() {
        return this.typeCounter.getValue(this.f35rt.getId());
    }

    public Collection getValues() {
        return this.values;
    }

    public Date getStartDate() {
        return this.start;
    }

    public Date getEndDate() {
        return this.end;
    }

    public long getPeriodSize() {
        return this.size;
    }

    public Object invokeMethod(String methodName) throws Exception {
        Object[] values = {this};
        return invokeMethod(methodName, params, values);
    }

    public Object invokeMethod(String methodName, Class[] params2, Object[] values) throws Exception {
        Class aClass = this.plan.getResourceClass(Integer.toString(this.f35rt.getId()));
        Session.getLog().info("--Invoking-->" + aClass.getName() + "#" + methodName);
        Method method = aClass.getMethod(methodName, params2);
        if (method == null) {
            throw new Exception("Unable to get method:" + methodName + "#" + params2);
        }
        try {
            Object result = method.invoke(null, values);
            return result;
        } catch (InvocationTargetException ex) {
            throw ((Exception) ex.getTargetException());
        }
    }

    public double getSetupMultiplier() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getSetupMultiplier");
            return result.doubleValue();
        }
        return this.res.getSetupMultiplier();
    }

    public double getRecurrentMultiplier() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getRecurrentMultiplier");
            return result.doubleValue();
        }
        return this.res.getRecurrentMultiplier();
    }

    public double getResellerRecurrentMultiplier() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getResellerRecurrentMultiplier");
            return result.doubleValue();
        }
        return this.res.getResellerRecurrentMultiplier();
    }

    public double getResellerSetupMultiplier() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getResellerSetupMultiplier");
            return result.doubleValue();
        }
        return this.res.getResellerSetupMultiplier();
    }

    public double getFreeUnits() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getFreeUnits");
            return result.doubleValue();
        }
        return this.res.getFreeNumber();
    }

    public double getMaxNumber() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getMaxNumber");
            return result.doubleValue();
        }
        return this.res.getMaxNumber();
    }

    public double getAmount() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getAmount");
            return result.doubleValue();
        }
        return this.res.getAmount();
    }

    public double getTotalAmount() throws Exception {
        if (this.res == null || this.values != null) {
            Double result = (Double) invokeMethod("getTotalAmount");
            return result.doubleValue();
        }
        return this.res.getTotalAmount();
    }

    public String getDescription() throws Exception {
        String result;
        Session.getLog().info("-->getDescr:" + this.f35rt.getResourceClass().getName());
        if (this.res == null || this.values != null) {
            result = (String) invokeMethod("getDescription");
        } else {
            result = Resource.getDescription(this.res.getId());
        }
        Session.getLog().info("-->" + result);
        return result;
    }

    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        Session.getLog().info("-->getRecurrentChangeDescripton:" + this.f35rt.getResourceClass().getName());
        Object[] values = {this, begin, end};
        Class[] params2 = {InitToken.class, Date.class, Date.class};
        if (this.res == null || values != null) {
            String result = (String) invokeMethod("getRecurrentChangeDescripton", params2, values);
            Session.getLog().info("-->" + result);
            return result;
        }
        return this.res.getRecurrentChangeDescripton(begin, end);
    }

    public String getResellerRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        Session.getLog().info("-->getResellerRecurrentChangeDescripton:" + this.f35rt.getResourceClass().getName());
        Object[] values = {this, begin, end};
        Class[] params2 = {InitToken.class, Date.class, Date.class};
        if (this.res == null || values != null) {
            String result = (String) invokeMethod("getResellerRecurrentChangeDescripton", params2, values);
            Session.getLog().info("-->" + result);
            return result;
        }
        return this.res.getResellerRecurrentChangeDescripton(begin, end);
    }

    public String getResellerRecurrentRefundDescription(Date begin, Date end) throws Exception {
        Session.getLog().info("-->getResellerRecurrentRefundDescription:" + this.f35rt.getResourceClass().getName());
        Object[] values = {this, begin, end};
        Class[] params2 = {InitToken.class, Date.class, Date.class};
        if (this.res == null || values != null) {
            String result = (String) invokeMethod("getResellerRecurrentRefundDescription", params2, values);
            Session.getLog().info("-->" + result);
            return result;
        }
        return this.res.getResellerRecurrentRefundDescription(begin, end);
    }

    public String getChangePlanPeriodDescription(Date begin, Date end) throws Exception {
        Session.getLog().info("-->getChangePlanPeriodDescription:" + this.f35rt.getResourceClass().getName());
        Object[] values = {this, begin, end};
        Class[] params2 = {InitToken.class, Date.class, Date.class};
        if (this.res == null || values != null) {
            String result = (String) invokeMethod("getChangePlanPeriodDescription", params2, values);
            Session.getLog().info("-->" + result);
            return result;
        }
        return this.res.getChangePlanPeriodDescription(this, begin, end);
    }

    public String getSetupChargeDescription(Date begin) throws Exception {
        Session.getLog().info("-->getSetupChargeDescription:" + this.f35rt.getResourceClass().getName());
        Object[] values = {this, begin};
        Class[] params2 = {InitToken.class, Date.class};
        if (this.res == null || values != null) {
            String result = (String) invokeMethod("getSetupChargeDescription", params2, values);
            Session.getLog().info("-->" + result);
            return result;
        }
        return this.res.getSetupChargeDescription(begin);
    }

    public String getPeriodInWords() {
        if (this.f35rt.isMonthly()) {
            return this.plan.getMonthPeriodInWords();
        }
        return this.plan.getPeriodInWords(getPeriodId());
    }

    public String getMonthPeriodInWords() {
        return this.plan.getMonthPeriodInWords();
    }

    public Collection getChangePlanInitValue(InitToken refundToken) throws Exception {
        Resource r = getRes();
        getResourceType().getId();
        if (r == null) {
            return getValues();
        }
        return r.getChangePlanInitValueForRes(refundToken, this);
    }
}
