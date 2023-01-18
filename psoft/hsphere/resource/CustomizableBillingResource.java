package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.calc.Calc;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/CustomizableBillingResource.class */
public class CustomizableBillingResource extends Resource {
    private double price;
    private String description;
    private String note;
    private String adminNote;

    public CustomizableBillingResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.description = (String) i.next();
        this.note = (String) i.next();
        this.adminNote = (String) i.next();
        this.price = Double.parseDouble((String) i.next());
        try {
            this.price /= Calc.getMultiplier();
        } catch (Exception ex) {
            Session.getLog().error("Can`t get Multiplier", ex);
        }
    }

    public CustomizableBillingResource(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT description, note, admin_note, price FROM custom_billing WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.description = rs.getString(1);
                this.note = rs.getString(2);
                this.adminNote = rs.getString(3);
                this.price = rs.getDouble(4);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO custom_billing (id, description, note, admin_note, price) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.description);
            ps.setString(3, this.note);
            ps.setString(4, this.adminNote);
            ps.setDouble(5, this.price);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() {
        return this.description;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.custom_billing.refund", new Object[]{getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.custom_billing.recurrent", new Object[]{getPeriodInWords(), getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.custom_billing.refundall", new Object[]{getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        return this.price;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("custom_desc")) {
            return new TemplateString(getDescription());
        }
        if (key.equals("custom_note")) {
            return new TemplateString(this.note);
        }
        if (key.equals("custom_admin_note")) {
            return new TemplateString(this.adminNote);
        }
        if (key.equals("price")) {
            try {
                return new TemplateString(this.price * Calc.getMultiplier());
            } catch (Exception ex) {
                Session.getLog().error("Can`t get Multiplier", ex);
                return new TemplateString(this.price);
            }
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        List values = new ArrayList();
        values.add(this.description);
        values.add(this.note);
        values.add(this.adminNote);
        double priceToInit = this.price;
        values.add(Double.toString(priceToInit));
        return values;
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        String str = (String) i.next();
        String str2 = (String) i.next();
        String str3 = (String) i.next();
        double price = Double.parseDouble((String) i.next());
        return price;
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        Iterator i = token.getValues().iterator();
        String descr = (String) i.next();
        return Localizer.translateMessage("bill.custom_billing.recurrent", new Object[]{token.getMonthPeriodInWords(), descr, f42df.format(begin), f42df.format(end)});
    }
}
