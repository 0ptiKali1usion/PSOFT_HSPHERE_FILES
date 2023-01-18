package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/InvoiceEntry.class */
public class InvoiceEntry implements TemplateHashModel {
    protected int type;
    protected String description;
    protected String note;
    protected double amount;
    protected ResourceId resourceId;
    private static final TemplateString SETUP = new TemplateString("SETUP");
    private static final TemplateString RECURRENT = new TemplateString("RECURRENT");
    private static final TemplateString REFUND = new TemplateString("REFUND");
    private static final TemplateString USAGE = new TemplateString("USAGE");
    private static final TemplateString UNKNOWN = new TemplateString("UNKNOWN");

    public double getAmount() {
        return this.amount;
    }

    public String getDescription() {
        return this.description;
    }

    public String getNote() {
        return this.note;
    }

    public ResourceId getResourceId() {
        return this.resourceId;
    }

    public int getType() {
        return this.type;
    }

    public InvoiceEntry(int t, String d, double a) {
        this(t, d, a, null);
    }

    public InvoiceEntry(int t, String d, double a, ResourceId resId) {
        this.type = t;
        this.description = d;
        this.amount = a;
        this.note = Session.getBillingNote();
        this.resourceId = resId;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("type".equals(key)) {
            switch (this.type) {
                case 1:
                    return SETUP;
                case 2:
                    return RECURRENT;
                case 3:
                    return USAGE;
                case 4:
                    return REFUND;
                default:
                    return UNKNOWN;
            }
        } else if ("typeId".equals(key)) {
            return new TemplateString(this.type);
        } else {
            if ("description".equals(key)) {
                return new TemplateString(this.description);
            }
            if ("note".equals(key)) {
                return new TemplateString(this.note);
            }
            if ("amount".equals(key)) {
                return new TemplateString(this.amount);
            }
            if ("resource_id".equals(key)) {
                return this.resourceId;
            }
            throw new TemplateModelException("No such key (psoft.hsphere.InvoiceEntry): " + key);
        }
    }
}
