package psoft.hsphere;

import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/ResellerTypeCounterHolder.class */
public class ResellerTypeCounterHolder {
    protected Hashtable typeCounters = new Hashtable();
    protected ResellerPriceHolder prices;
    protected long resellerId;

    public ResellerTypeCounterHolder(ResellerPriceHolder prices, long resellerId) {
        this.prices = null;
        this.prices = prices;
        this.resellerId = resellerId;
        reload();
    }

    public ResellerTypeCounter getTypeCounter(String typeName) {
        ResellerTypeCounter type = (ResellerTypeCounter) this.typeCounters.get(typeName);
        return type;
    }

    public ResellerTypeCounter getTypeCounter(int typeId) {
        try {
            ResellerTypeCounter type = (ResellerTypeCounter) this.typeCounters.get(TypeRegistry.getType(typeId));
            return type;
        } catch (NoSuchTypeException e) {
            return null;
        }
    }

    public void reload() {
        ResellerPrices price;
        Session.getLog().debug("Reload typeCounter for ResellerId " + this.resellerId);
        for (String typeName : TypeRegistry.getTypes()) {
            try {
                price = this.prices.getPrices(typeName);
            } catch (Exception ex) {
                Session.getLog().debug("Reseller Type counter error", ex);
            }
            if (!price.isNull()) {
                ResellerTypeCounter type = getTypeCounter(typeName);
                if (type == null || !type.getCreated().after(price.getCreated())) {
                    this.typeCounters.put(typeName, new ResellerTypeCounter(this.resellerId, typeName, this.prices));
                }
            } else {
                this.typeCounters.remove(typeName);
            }
        }
    }

    public void inc(ResourceId rid, double delta) {
        ResellerTypeCounter type = getTypeCounter(rid.getType());
        if (type != null) {
            type.inc(delta, rid);
        }
    }

    public void dec(ResourceId rid, double delta) throws Exception {
        ResellerTypeCounter type = getTypeCounter(rid.getType());
        if (type != null) {
            type.dec(delta, rid);
        }
    }

    public double getBillableValue(ResourceId rid, double amount) {
        ResellerTypeCounter type = getTypeCounter(rid.getType());
        if (type != null) {
            return type.getBillableValue(rid, amount);
        }
        return amount;
    }

    public double getBillableValueEstimate(ResourceId rid, double curAmount, double newAmount) {
        ResellerTypeCounter type = getTypeCounter(rid.getType());
        if (type != null) {
            return type.getBillableValueEstimate(rid, curAmount, newAmount);
        }
        return 0.0d;
    }

    public double getValue(int rid) {
        ResellerTypeCounter type = getTypeCounter(rid);
        if (type != null) {
            return type.getValue();
        }
        return 0.0d;
    }
}
