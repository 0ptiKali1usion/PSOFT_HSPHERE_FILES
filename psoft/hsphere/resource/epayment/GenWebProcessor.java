package psoft.hsphere.resource.epayment;

import java.util.Iterator;
import psoft.epayment.PaymentInstrument;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/GenWebProcessor.class */
public class GenWebProcessor extends GenCheck {
    private String type;

    @Override // psoft.hsphere.resource.epayment.GenCheck, psoft.epayment.PaymentInstrument
    public PaymentInstrument copy(BillingInfo bi) throws Exception {
        return new GenWebProcessor(bi, this);
    }

    public GenWebProcessor(BillingInfo bi) {
        this.type = bi.getType();
    }

    public GenWebProcessor(BillingInfo bi, GenWebProcessor gc) {
        this.type = bi.getType();
    }

    public GenWebProcessor(BillingInfo bi, Accessor a, NameModifier nm) {
        this.type = bi.getType();
    }

    public GenWebProcessor(BillingInfo bi, Iterator i) {
        this.type = bi.getType();
    }

    @Override // psoft.hsphere.resource.epayment.GenCheck, psoft.epayment.PaymentInstrument
    public void checkValid() {
    }

    @Override // psoft.hsphere.resource.epayment.GenCheck, psoft.epayment.PaymentInstrument
    public int getBillingType() {
        return 3;
    }

    @Override // psoft.hsphere.resource.epayment.GenCheck, psoft.epayment.PaymentInstrument
    public String getType() {
        return this.type;
    }
}
