package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.resource.OpenSRS;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerDomainTransferSetupCalc.class */
public class ResellerDomainTransferSetupCalc {
    static double getPrice(String tld) throws Exception {
        double amount = Session.getReseller().getPrices("TRANSFER_" + tld).getSetupPrice();
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainTransferPrice(tld, 1L);
        }
        if (Double.isNaN(amount)) {
            Session.getLog().debug("Transfer Price is not set for TLD " + tld);
            return 0.0d;
        }
        return amount;
    }

    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        OpenSRS r = (OpenSRS) _r;
        if (_r.isInitialized()) {
            return RegistrationSetupCalc.calc(_r, begin, end);
        }
        return getPrice(DomainRegistrar.getTLD(r.getDomainName()));
    }

    public static double calc(InitToken token) throws Exception {
        if (token.getRes() != null) {
            return RegistrationSetupCalc.calc(token);
        }
        Iterator i = token.getValues().iterator();
        i.next();
        i.next();
        i.next();
        String name = (String) i.next();
        return getPrice(DomainRegistrar.getTLD(name));
    }

    private static int getPeriod(String val) throws Exception {
        if (val == null || val.length() == 0) {
            return 1;
        }
        return Integer.parseInt(val);
    }
}
