package psoft.hsphere.exception.allocation;

import psoft.hsphere.Localizer;

/* loaded from: hsphere.zip:psoft/hsphere/exception/allocation/NoAvailableAllocatedServerForPlan.class */
public class NoAvailableAllocatedServerForPlan extends Exception {
    public NoAvailableAllocatedServerForPlan(long planId) {
        super(Localizer.translateMessage("msg.exception.no_available_allocated_server_for_reseller", new String[]{Long.toString(planId)}));
    }
}
