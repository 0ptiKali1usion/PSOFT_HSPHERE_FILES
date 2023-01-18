package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/resource/AbstractChangeableResource.class */
public abstract class AbstractChangeableResource extends Resource {
    protected int size;

    protected abstract void saveSize(double d) throws Exception;

    protected abstract void changeResourcePhysical(double d) throws Exception;

    public AbstractChangeableResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public AbstractChangeableResource(ResourceId id) throws Exception {
        super(id);
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.size;
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        return 1.0d;
    }

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        try {
            return USFormat.parseDouble((String) i.next());
        } catch (Exception e) {
            Session.getLog().warn("Problem with parsing double", e);
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.Resource
    public double getTotalAmount() {
        return getAmount();
    }

    public static double getSetupMultiplier(InitToken token) {
        return 1.0d;
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        Double tmpSize = new Double(USFormat.parseDouble((String) i.next()));
        int size = tmpSize.intValue();
        double defaultQuota = token.getFreeUnits();
        if (defaultQuota >= size) {
            return 0.0d;
        }
        return size - defaultQuota;
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        Session.getLog().debug("In AbstractChangeableResource own getRecurrentMultiplier");
        double defaultQuota = getFreeNumber();
        if (defaultQuota >= this.size) {
            return 0.0d;
        }
        return this.size - defaultQuota;
    }

    @Override // psoft.hsphere.Resource
    public double getUsageMultiplier() {
        Session.getLog().debug("In AbstractChangeableResource own getUsageMultiplier");
        double defaultQuota = getFreeNumber();
        if (defaultQuota >= this.size) {
            return 0.0d;
        }
        return this.size - defaultQuota;
    }

    @Override // psoft.hsphere.Resource
    public String getResellerRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.resource.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end), TypeRegistry.getDescription(getId().getType())});
    }

    @Override // psoft.hsphere.Resource
    public synchronized double changeResource(Collection values) throws Exception {
        int oldSize = this.size;
        Iterator i = values.iterator();
        try {
            double newSize = USFormat.parseDouble((String) i.next());
            try {
                saveSize(newSize);
                this.size = (int) newSize;
                changeResourcePhysical(oldSize);
                return oldSize;
            } catch (Exception ex) {
                this.size = oldSize;
                saveSize(this.size);
                throw ex;
            }
        } catch (Exception e) {
            Session.getLog().warn("Problem parsing double", e);
            throw e;
        }
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.resource.recurrent_change", new Object[]{getPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate()), TypeRegistry.getDescription(TypeRegistry.getTypeId(token.getResourceType().getType()))});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.resource.refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end), TypeRegistry.getDescription(getId().getType())});
    }

    public static String getResellerRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        double newSize = token.getAmount();
        double oldSize = 0.0d;
        if (token.getRes() != null) {
            oldSize = token.getRes().getAmount();
        }
        if (newSize - oldSize != 0.0d) {
            return Localizer.translateMessage("bill.reseller.resource.recurrent", new Object[]{token.getPeriodInWords(), TypeRegistry.getDescription(token.getResourceType().getId()), new Double(newSize - oldSize), f42df.format(begin), f42df.format(end)});
        }
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{token.getPeriodInWords(), token.getDescription(), f42df.format(begin), f42df.format(end)});
    }

    public static String getResellerRecurrentRefundDescription(InitToken token, Date begin, Date end) throws Exception {
        double newSize = token.getAmount();
        double oldSize = token.getRes().getAmount();
        return Localizer.translateMessage("bill.reseller.resource.refund", new Object[]{TypeRegistry.getDescription(token.getResourceType().getId()), new Double(oldSize - newSize), f42df.format(begin), f42df.format(end), TypeRegistry.getDescription(TypeRegistry.getTypeId(token.getResourceType().getType()))});
    }

    public static String getDescription(InitToken token) throws Exception {
        return Resource.getDescription(token) + " " + getAmount(token);
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return TypeRegistry.getDescription(getId().getType()) + " " + new Integer(this.size).toString();
    }

    @Override // psoft.hsphere.Resource
    public Collection getChangePlanInitValueForRes(InitToken refundToken, InitToken initToken) throws Exception {
        List values = new ArrayList();
        ResourceId rid = getId();
        int type = getId().getType();
        double actualSize = fix((inUse() * 110.0d) / 100.0d);
        values.add(Double.toString(getAmount()));
        initToken.set(type, this, values);
        refundToken.set(type, this, values);
        List valuesToEstimate = new ArrayList();
        getAmount();
        if (initToken.getRecurrentMultiplier() > 0.0d) {
            if (estimateRefund(refundToken) >= 0.0d && getAmount() <= getFreeNumber()) {
                if (actualSize > initToken.getFreeUnits() && !rid.isMonthly()) {
                    valuesToEstimate.add(Double.toString(actualSize));
                } else {
                    valuesToEstimate.add(Double.toString(initToken.getFreeUnits()));
                    initToken.getFreeUnits();
                }
            } else {
                valuesToEstimate.add(Double.toString(getAmount()));
            }
        } else if (initToken.getFreeUnits() < getAmount()) {
            valuesToEstimate.add(Double.toString(getAmount()));
        } else {
            valuesToEstimate.add(Double.toString(initToken.getFreeUnits()));
            initToken.getFreeUnits();
        }
        return valuesToEstimate;
    }
}
