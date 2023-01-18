package psoft.hsphere.exception.allocation;

import psoft.hsphere.Localizer;

/* loaded from: hsphere.zip:psoft/hsphere/exception/allocation/NoAvailableAllocatedServerForReseller.class */
public class NoAvailableAllocatedServerForReseller extends Exception {
    public NoAvailableAllocatedServerForReseller(long resellerId) {
        super(Localizer.translateMessage("msg.exception.no_available_allocated_server_for_reseller", new String[]{Long.toString(resellerId)}));
    }
}
