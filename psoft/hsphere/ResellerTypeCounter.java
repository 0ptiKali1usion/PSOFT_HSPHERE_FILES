package psoft.hsphere;

import java.util.Date;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/ResellerTypeCounter.class */
public class ResellerTypeCounter {
    protected long resellerId;
    protected int type;
    protected String typeName;
    protected long resourceId;
    protected double amount;
    protected double totalAmount;
    protected Date created;
    protected ResellerPriceHolder prices;

    public void inc(double delta, ResourceId rid) {
        if (Session.getAccount().isDemoAccount()) {
            return;
        }
        double free = this.prices.getPrices(this.typeName).getFreeUnits();
        this.totalAmount += delta;
        Session.getLog().debug("Increment ResTypeCounter " + this.resellerId + " typeName:" + this.typeName + " Total:" + this.totalAmount + " amount:" + this.amount + " resource ID:" + this.resourceId + " curentId:" + rid.getId());
        if (free > this.totalAmount) {
            return;
        }
        if (this.resourceId == rid.getId()) {
            this.amount += delta;
        }
        if (this.resourceId != 0) {
            return;
        }
        this.amount = this.totalAmount - free;
        this.resourceId = rid.getId();
        Session.getLog().debug("Increment ResTypeCounter " + this.resellerId + " typeName:" + this.typeName + " created:" + this.created + " Total amout:" + this.totalAmount + " amount:" + this.amount + " resource ID:" + this.resourceId);
    }

    /* JADX WARN: Code restructure failed: missing block: B:65:0x0134, code lost:
        r6.resourceId = r0.getLong(1);
        r6.amount = r15 - r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void dec(double r7, psoft.hsphere.ResourceId r9) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 453
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.ResellerTypeCounter.dec(double, psoft.hsphere.ResourceId):void");
    }

    public double getValue() {
        return this.totalAmount;
    }

    public double getBillableValue(ResourceId rid, double curAmount) {
        if (this.resourceId == 0 || this.resourceId > rid.getId()) {
            return 0.0d;
        }
        if (this.resourceId == rid.getId()) {
            return this.amount;
        }
        return curAmount;
    }

    public double getBillableValueEstimate(ResourceId rid, double curAmount, double newAmount) {
        double delta = getBillableValue(rid, curAmount);
        double deltaCur = newAmount - curAmount;
        if (delta > 0.0d) {
            if (delta + deltaCur >= 0.0d) {
                return delta + deltaCur;
            }
            return 0.0d;
        }
        double free = this.prices.getPrices(this.typeName).getFreeUnits();
        if (free > this.totalAmount + deltaCur) {
            return 0.0d;
        }
        double sum = (this.totalAmount + deltaCur) - free;
        if (sum < Math.abs(deltaCur)) {
            return sum;
        }
        return deltaCur;
    }

    public void clear() {
        this.resourceId = 0L;
        this.amount = 0.0d;
        this.totalAmount = 0.0d;
    }

    public ResellerTypeCounter(long resellerId, String type, ResellerPriceHolder prices) throws Exception {
        this(resellerId, Integer.parseInt(TypeRegistry.getTypeId(type)), prices);
    }

    public ResellerTypeCounter(long resellerId, int type, ResellerPriceHolder prices) throws Exception {
        this.resellerId = 0L;
        this.resourceId = 0L;
        this.amount = 0.0d;
        this.totalAmount = 0.0d;
        this.resellerId = resellerId;
        this.type = type;
        this.typeName = TypeRegistry.getType(type);
        this.prices = prices;
        this.created = TimeUtils.getDate();
        init();
        Session.getLog().debug("ResTypeCounter " + resellerId + " typeName:" + this.typeName + " created:" + this.created + " Total amout:" + this.totalAmount + " amount:" + this.amount + " resource ID:" + this.resourceId);
    }

    /* JADX WARN: Code restructure failed: missing block: B:44:0x00bd, code lost:
        r6.resourceId = r0.getLong(1);
        r6.amount = r12 - r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void init() throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 241
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.ResellerTypeCounter.init():void");
    }

    public Date getCreated() {
        return this.created;
    }
}
