package psoft.hsphere.exception.allocation;

import psoft.hsphere.Localizer;

/* loaded from: hsphere.zip:psoft/hsphere/exception/allocation/AllocatedPServerNotFoundException.class */
public class AllocatedPServerNotFoundException extends Exception {
    public AllocatedPServerNotFoundException(long apsId) {
        super(Localizer.translateMessage("msg.exception.aps_not_found", new String[]{Long.toString(apsId)}));
    }
}
