package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.InitToken;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.OpenSRS;

/* loaded from: hsphere.zip:psoft/hsphere/calc/RegistrationSetupCalc.class */
public class RegistrationSetupCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        OpenSRS r = (OpenSRS) _r;
        String name = r.getDomainName();
        int period = r.getPeriod();
        String tld = DomainRegistrar.getTLD(name);
        int periodToPay = Math.min((int) r.getSetupMultiplier(), period);
        if (periodToPay <= 0) {
            return 0.0d;
        }
        double amount = getAmount(r, periodToPay, tld);
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainPrice(tld, periodToPay);
        }
        if (Double.isNaN(amount)) {
            throw new Exception("Price is not set for TLD " + tld + " and period " + period + " YEARS to pay Period:" + periodToPay);
        }
        return amount;
    }

    public static double calc(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        int period = Integer.parseInt((String) i.next());
        i.next();
        i.next();
        String name = (String) i.next();
        String tld = DomainRegistrar.getTLD(name);
        int periodToPay = Math.min((int) token.getSetupMultiplier(), period);
        if (periodToPay <= 0) {
            return 0.0d;
        }
        double amount = getAmount(token, periodToPay, tld);
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainPrice(tld, periodToPay);
        }
        if (Double.isNaN(amount)) {
            throw new Exception("Price is not set for TLD " + tld + " and period " + period + " YEARS to pay Period:" + periodToPay);
        }
        return amount;
    }

    protected static double getAmount(Resource r, int period, String tld) {
        Session.getLog().debug("Get amount with resource");
        return getAmountFinal(r.getResourceType(), period, tld);
    }

    protected static double getAmount(InitToken token, int period, String tld) {
        Session.getLog().debug("Get amount with init token");
        return getAmountFinal(token.getResourceType(), period, tld);
    }

    protected static double getAmountFinal(ResourceType rt, int period, String tld) {
        double result;
        try {
        } catch (Exception e) {
            result = Double.NaN;
            Session.getLog().debug("Unable to get amount from plan settings!");
        }
        if (Plan.getPlan(rt.getPlanId()).isWitoutBilling()) {
            return 0.0d;
        }
        String strValue = "_TLD_" + tld + "_" + String.valueOf(period);
        String strTLD = rt.getValue(strValue);
        Session.getLog().debug("strTLD = " + strTLD + " strValue = " + strValue);
        result = Double.parseDouble(strTLD);
        return result;
    }
}
