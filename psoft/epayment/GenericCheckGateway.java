package psoft.epayment;

import java.util.HashMap;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/epayment/GenericCheckGateway.class */
public class GenericCheckGateway implements MerchantGateway {

    /* renamed from: id */
    protected int f3id;

    @Override // psoft.epayment.MerchantGateway
    public int getId() {
        return this.f3id;
    }

    @Override // psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) {
        this.f3id = id;
    }

    @Override // psoft.epayment.MerchantGateway
    public Map getValues() {
        return new HashMap();
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap charge(String description, double amount, PaymentInstrument pi) throws Exception {
        return charge(-1L, description, amount, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap charge(long id, String description, double amount, PaymentInstrument pi) throws Exception {
        if (((CreditCard) pi).getCVV() != null && !"".equals(((CreditCard) pi).getCVV())) {
            ((CreditCard) pi).setCVVChecked(true);
            return null;
        }
        throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. ");
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap authorize(String description, double amount, PaymentInstrument pi) throws Exception {
        return authorize(-1L, description, amount, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap authorize(long id, String description, double amount, PaymentInstrument pi) throws Exception {
        HashMap retVal = new HashMap();
        retVal.put("amount", new Double(amount));
        if (((CreditCard) pi).getCVV() != null && !"".equals(((CreditCard) pi).getCVV())) {
            ((CreditCard) pi).setCVVChecked(true);
            return retVal;
        }
        throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. ");
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap capture(String description, HashMap data, PaymentInstrument pi) throws Exception {
        return capture(-1L, description, data, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap capture(long id, String description, HashMap data, PaymentInstrument pi) throws Exception {
        return null;
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap voidAuthorize(String description, HashMap data, PaymentInstrument pi) throws Exception {
        return voidAuthorize(-1L, description, data, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, PaymentInstrument pi) throws Exception {
        return null;
    }

    @Override // psoft.epayment.MerchantGateway
    public void setLog(MerchantGatewayLog log) {
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "CheckGateway";
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap checkCVV(long acctid, PaymentInstrument pi) throws Exception {
        if (((CreditCard) pi).getCVV() != null && !"".equals(((CreditCard) pi).getCVV())) {
            ((CreditCard) pi).setCVVChecked(true);
            return new HashMap();
        }
        ((CreditCard) pi).setCVVChecked(false);
        throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. ");
    }
}
