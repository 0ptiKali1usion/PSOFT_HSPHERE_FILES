package psoft.hsphere.resource.epayment;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.HashMap;
import java.util.Iterator;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.resource.registrar.EnomRequest;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/GenCheck.class */
public class GenCheck implements PaymentInstrument, TemplateHashModel {
    public void charge(String description, double amount) throws Exception {
    }

    public void charge(long id, String description, double amount) throws Exception {
    }

    public HashMap auth(String description, double amount) throws Exception {
        return null;
    }

    @Override // psoft.epayment.PaymentInstrument
    public void checkValid() {
    }

    public HashMap auth(long id, String description, double amount) throws Exception {
        return null;
    }

    public void capture(String description, HashMap data) throws Exception {
    }

    public void capture(long id, String description, HashMap data) throws Exception {
    }

    public void void_auth(String description, HashMap data) throws Exception {
    }

    public void void_auth(long id, String description, HashMap data) throws Exception {
    }

    @Override // psoft.epayment.PaymentInstrument
    public PaymentInstrument copy(BillingInfo bi) throws Exception {
        return new GenCheck(bi, this);
    }

    public GenCheck() {
    }

    public GenCheck(BillingInfo billingInfo) throws Exception {
    }

    public GenCheck(BillingInfo bi, GenCheck gc) throws Exception {
    }

    public GenCheck(BillingInfo bi, Accessor a, NameModifier nm) throws Exception {
    }

    public GenCheck(BillingInfo bi, Iterator i) throws Exception {
    }

    @Override // psoft.epayment.PaymentInstrument
    public int getBillingType() {
        return 2;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.epayment.PaymentInstrument
    public String getType() {
        return EnomRequest.CHECK;
    }

    public TemplateModel get(String key) {
        return null;
    }
}
