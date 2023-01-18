package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.global.Globals;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerDedicatedServerSetupCalc.class */
public class ResellerDedicatedServerSetupCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        double amount = 0.0d;
        DedicatedServerResource r = (DedicatedServerResource) _r;
        DedicatedServer ds = r.getDSObject();
        if (ds.getResellerId() != 1) {
            return 0.0d;
        }
        if (ds.isTemplatedServer()) {
            String prefix = getDSTemplateName(ds);
            amount = Session.getReseller().getPrices(prefix).getSetupPrice();
        }
        Session.getLog().debug("ResellerDedicatedServerSetupCalc::calc(Resource, Date, Date) result is " + (amount * r.getSetupMultiplier()) + " amount is " + amount);
        return amount * r.getSetupMultiplier();
    }

    private static String getDSTemplateName(DedicatedServer ds) throws Exception {
        String dsPrefix = Globals.getAccessor().getSet("ds_templates").getPrefix();
        String prefix = dsPrefix + ds.getParent().getId();
        return prefix;
    }

    public static double calc(InitToken token) throws Exception {
        double amount = 0.0d;
        Iterator i = token.getValues().iterator();
        String _dsoId = (String) i.next();
        long dsoId = Long.parseLong(_dsoId);
        DedicatedServer ds = DSHolder.getDedicatedServerObject(dsoId);
        if (ds.getResellerId() != 1) {
            return 0.0d;
        }
        if (ds.isTemplatedServer()) {
            String prefix = getDSTemplateName(ds);
            amount = Session.getReseller().getPrices(prefix).getSetupPrice();
        }
        Session.getLog().debug("ResellerDedicatedServerSetupCalc::calc(InitToken) result is " + (amount * token.getSetupMultiplier()) + " amount is " + amount);
        return amount * token.getSetupMultiplier();
    }
}
