package psoft.hsphere;

import java.util.Hashtable;
import psoft.hsphere.global.Globals;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServerTemplate;

/* loaded from: hsphere.zip:psoft/hsphere/ResellerPriceHolder.class */
public class ResellerPriceHolder {
    protected Hashtable prices = new Hashtable();
    protected int resellerPlanId;

    public ResellerPriceHolder(int resellerPlanId) throws Exception {
        this.resellerPlanId = resellerPlanId;
        reload();
    }

    public ResellerPrices getPrices(String typeName) {
        ResellerPrices price = (ResellerPrices) this.prices.get(typeName);
        return price != null ? price : new ResellerPrices(typeName);
    }

    protected void setPrice(String typeName, Plan resellerPlan) {
        try {
            ResellerPrices price = getPrices(typeName);
            ResellerPrices priceNew = new ResellerPrices(typeName, resellerPlan);
            if (price != null && price.equals(priceNew)) {
                return;
            }
            this.prices.remove(typeName);
            if (!priceNew.isNull()) {
                this.prices.put(typeName, priceNew);
            }
        } catch (NoSuchTypeException e) {
        }
    }

    public void reload() throws Exception {
        Plan resellerPlan = Plan.getPlan(this.resellerPlanId);
        for (String str : TypeRegistry.getTypes()) {
            setPrice(str, resellerPlan);
        }
        for (String tld : DomainRegistrar.getActiveTLDs()) {
            for (int j = 1; j < 11; j++) {
                setPrice("TLD_" + tld + "_" + j, resellerPlan);
            }
            setPrice("TRANSFER_" + tld, resellerPlan);
        }
        for (DedicatedServerTemplate dst : DSHolder.getAccessibleDSTemplates()) {
            String dsPrefix = Globals.getAccessor().getSet("ds_templates").getPrefix();
            String prefix = dsPrefix + dst.getId();
            setPrice(prefix, resellerPlan);
        }
    }
}
