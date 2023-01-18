package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/DedicatedServerSetupCalc.class */
public class DedicatedServerSetupCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        double amount;
        DedicatedServerResource r = (DedicatedServerResource) _r;
        DedicatedServer ds = r.getDSObject();
        if (!ds.isTemplatedServer()) {
            amount = ds.getSetup();
        } else {
            amount = Calc.getPrice(r, "DST_" + ds.getTemplate().getId() + "_SETUP");
            if (Double.isNaN(amount)) {
                amount = Calc.getDefaultPrice(r, "DST_" + ds.getTemplate().getId() + "_SETUP");
            }
        }
        Session.getLog().debug("DedicatedServerSetupCalc::calc(Resource, Date, Date) result is " + (amount * r.getSetupMultiplier()) + " amount is " + amount);
        return amount * r.getSetupMultiplier();
    }

    public static double calc(InitToken token) throws Exception {
        double amount;
        Iterator i = token.getValues().iterator();
        String _dsoId = (String) i.next();
        long dsoId = Long.parseLong(_dsoId);
        DedicatedServer ds = DSHolder.getDedicatedServerObject(dsoId);
        if (ds.isTemplatedServer()) {
            amount = Calc.getPrice(token, "DST_" + dsoId + "_SETUP");
            if (Double.isNaN(amount)) {
                amount = Calc.getDefaultPrice(token, "DST_" + dsoId + "_SETUP");
            }
        } else {
            amount = ds.getSetup();
        }
        Session.getLog().debug("DedicatedServerSetupCalc::calc(InitToken) result is " + (amount * token.getSetupMultiplier()) + " amount is " + amount);
        return amount * token.getSetupMultiplier();
    }
}
