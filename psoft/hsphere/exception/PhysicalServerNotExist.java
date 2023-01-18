package psoft.hsphere.exception;

import psoft.hsphere.Localizer;

/* loaded from: hsphere.zip:psoft/hsphere/exception/PhysicalServerNotExist.class */
public class PhysicalServerNotExist extends Exception {
    public PhysicalServerNotExist(long psId) {
        super(Localizer.translateMessage("msg.exception.ps_not_found", new String[]{Long.toString(psId)}));
    }
}
