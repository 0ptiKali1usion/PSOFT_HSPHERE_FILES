package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.resource.OpenSRS;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerRegistrationCalc.class */
public class ResellerRegistrationCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        OpenSRS r = (OpenSRS) _r;
        String name = r.get("domain_name").toString();
        int period = Integer.parseInt(r.get("period").toString());
        String tld = DomainRegistrar.getTLD(name);
        double amount = Session.getReseller().getPrices("TLD_" + tld + "_" + period).getSetupPrice();
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainPrice(tld, period, 1L);
        }
        if (Double.isNaN(amount)) {
            Session.getLog().debug("Price is not set for TLD " + tld + " and period " + period + " YEARS");
            return 0.0d;
        }
        return amount;
    }

    public static double calc(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        int period = getPeriod((String) i.next());
        i.next();
        i.next();
        String name = (String) i.next();
        String tld = DomainRegistrar.getTLD(name);
        double amount = Session.getReseller().getPrices("TLD_" + tld + "_" + period).getSetupPrice();
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainPrice(tld, period, 1L);
        }
        if (Double.isNaN(amount)) {
            Session.getLog().debug("Price is not set for TLD " + tld + " and period " + period + " YEARS");
            return 0.0d;
        }
        return amount;
    }

    private static int getPeriod(String val) throws Exception {
        if (val == null || val.length() == 0) {
            return 1;
        }
        return Integer.parseInt(val);
    }
}
