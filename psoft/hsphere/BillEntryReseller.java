package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.Date;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/BillEntryReseller.class */
public class BillEntryReseller implements TemplateHashModel {

    /* renamed from: id */
    protected long f27id;
    protected int type;
    protected Date opened;
    protected double amount;
    protected Date canceled;
    protected String description;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("id")) {
            return new TemplateString(new Long(this.f27id));
        }
        if (key.equals("date")) {
            return new TemplateString(new Date(this.opened.getTime()));
        }
        if (key.equals("amount")) {
            return new TemplateString(new Double(this.amount));
        }
        if (key.equals("type")) {
            return new TemplateString(this.type);
        }
        if (key.equals("canceled")) {
            return new TemplateString(this.canceled);
        }
        if (key.equals("description")) {
            return new TemplateString(this.description);
        }
        return null;
    }

    public BillEntryReseller(long id, long resellerId, int type, Date opened, double amount, Date canceled, String description) {
        this.f27id = id;
        this.type = type;
        this.opened = opened;
        this.amount = amount;
        this.canceled = canceled;
        this.description = description;
    }

    public long getId() {
        return this.f27id;
    }

    public double getAmount() {
        return this.amount;
    }

    public int getType() {
        return this.type;
    }

    public Date getOpened() {
        return this.opened;
    }
}
