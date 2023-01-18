package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.InitToken;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.OpenSRS;

/* loaded from: hsphere.zip:psoft/hsphere/calc/DomainTransferSetupCalc.class */
public class DomainTransferSetupCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        if (_r.isInitialized()) {
            return RegistrationSetupCalc.calc(_r, begin, end);
        }
        ResourceType rt = _r.getResourceType();
        if (isNotBillable(rt) || _r.getSetupMultiplier() <= 0.0d) {
            return 0.0d;
        }
        OpenSRS r = (OpenSRS) _r;
        String name = r.getDomainName();
        String tld = DomainRegistrar.getTLD(name);
        double amount = getAmountFinal(rt, tld);
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainTransferPrice(tld, rt);
        }
        if (Double.isNaN(amount)) {
            throw new Exception("Price is not set for TLD " + tld + " for transfer");
        }
        return amount;
    }

    public static double calc(InitToken token) throws Exception {
        if (token.getRes() != null) {
            return RegistrationSetupCalc.calc(token);
        }
        ResourceType rt = token.getResourceType();
        if (isNotBillable(token.getResourceType()) || token.getSetupMultiplier() <= 0.0d) {
            return 0.0d;
        }
        Iterator i = token.getValues().iterator();
        i.next();
        i.next();
        i.next();
        String name = (String) i.next();
        String tld = DomainRegistrar.getTLD(name);
        double amount = getAmountFinal(rt, tld);
        if (Double.isNaN(amount)) {
            amount = DomainRegistrar.getDomainTransferPrice(tld, rt);
        }
        if (Double.isNaN(amount)) {
            throw new Exception("Price is not set for TLD " + tld + " for transfer");
        }
        return amount;
    }

    protected static boolean isNotBillable(ResourceType rt) throws UnknownResellerException {
        return Plan.getPlan(rt.getPlanId()).isWitoutBilling();
    }

    protected static double getAmountFinal(ResourceType rt, String tld) {
        double result;
        try {
        } catch (Exception e) {
            result = Double.NaN;
            Session.getLog().debug("Unable to get amount from plan settings!");
        }
        if (Plan.getPlan(rt.getPlanId()).isWitoutBilling()) {
            return 0.0d;
        }
        String strValue = "_TRANSFER" + tld;
        String strTLD = rt.getValue(strValue);
        Session.getLog().debug("strTLD = " + strTLD + " strValue = " + strValue);
        result = Double.parseDouble(strTLD);
        return result;
    }
}
