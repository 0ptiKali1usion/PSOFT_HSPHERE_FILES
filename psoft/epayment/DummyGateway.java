package psoft.epayment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/* loaded from: hsphere.zip:psoft/epayment/DummyGateway.class */
public class DummyGateway extends GenericMerchantGateway {
    private static Random rand = new Random();

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        return null;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        boolean success = true;
        String errMessage = "";
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        retval.put("id", Long.toString(Math.abs(rand.nextLong())));
        long trid = writeLog(id, amount, 0);
        String cvv = cc.getCVV();
        String message = "This is a test transaction with the DummyGateay";
        if (cvv != null && !"".equals(cvv)) {
            message = message + " cvv field was included into request";
            if (!"928".equals(cvv)) {
                success = false;
                errMessage = "Your transaction can't be processed. CVV is incorrect";
            }
            cc.setCVVChecked(success);
        }
        writeLog(trid, id, amount, 0, message, message, errMessage, success);
        if (!success) {
            throw new Exception(errMessage);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        boolean success = true;
        String errMessage = "";
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        retval.put("id", Long.toString(Math.abs(rand.nextLong())));
        long trid = writeLog(id, amount, 1);
        String cvv = cc.getCVV();
        String message = "This is a test transaction with the DummyGateay";
        if (cvv != null && !"".equals(cvv)) {
            message = message + " cvv field was included into request";
            if (!"928".equals(cvv)) {
                success = false;
                errMessage = "Your transaction can't be processed. CVV is incorrect";
            }
            cc.setCVVChecked(success);
        }
        writeLog(trid, id, amount, 1, message, message, errMessage, success);
        if (!success) {
            throw new Exception(errMessage);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        HashMap retval = new HashMap();
        double amount = ((Double) data.get("amount")).doubleValue();
        retval.put("amount", new Double(amount));
        retval.put("id", Long.toString(Math.abs(rand.nextLong())));
        long trid = writeLog(id, amount, 4);
        writeLog(trid, id, amount, 4, "This is a test transaction with the DummyGateay", "This is a test transaction with the DummyGateay", "", true);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        HashMap retval = new HashMap();
        double amount = ((Double) data.get("amount")).doubleValue();
        retval.put("amount", new Double(amount));
        retval.put("id", Long.toString(Math.abs(rand.nextLong())));
        long trid = writeLog(id, amount, 3);
        writeLog(trid, id, amount, 3, "This is a test transaction with the DummyGateay", "This is a test transaction with the DummyGateay", "", true);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "This id DummyGateway which can be used onlu for the testing purposes";
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        return checkCCCVV(acctid, cc);
    }
}
