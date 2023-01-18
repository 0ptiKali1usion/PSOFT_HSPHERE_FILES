package psoft.hsphere;

import java.util.Enumeration;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/ResellerGlobalValueHolder.class */
public class ResellerGlobalValueHolder {
    protected int planId;
    static final String keyPrefix = "_GLB_";
    protected Hashtable values = new Hashtable();
    protected boolean isUpdated = false;

    public ResellerGlobalValueHolder(int resellerPlanId) throws Exception {
        this.planId = resellerPlanId;
        reload();
    }

    public void update() throws Exception {
        this.isUpdated = false;
        reload();
    }

    public String getValue(String key) throws Exception {
        if (!this.isUpdated) {
            reload();
        }
        return (String) this.values.get(key);
    }

    public boolean includesByPrefix(String prefix) throws Exception {
        if (prefix != null && !"".equals(prefix)) {
            if (!this.isUpdated) {
                reload();
            }
            Enumeration e = this.values.keys();
            while (e.hasMoreElements()) {
                if (e.nextElement().toString().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    protected synchronized void reload() throws Exception {
        long resellerId = Session.getResellerId();
        if (resellerId != 1) {
            try {
                Session.setResellerId(1L);
            } finally {
                if (resellerId != 1) {
                    Session.setResellerId(resellerId);
                }
            }
        }
        Plan plan = Plan.getPlan(this.planId);
        if (plan == null) {
            throw new Exception("Unable to get a plan with id " + String.valueOf(this.planId) + ". Reseller ID is " + resellerId + ".");
        }
        for (String key : plan.getValueKeys()) {
            if (key != null && key.startsWith("_GLB_")) {
                this.values.put(key, plan.getValue(key));
            }
        }
        this.isUpdated = true;
    }
}
