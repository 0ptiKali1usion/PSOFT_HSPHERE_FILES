package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.plan.InitResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/Invoice.class */
public class Invoice implements TemplateHashModel {
    protected double total;
    protected double subtotal;
    protected String free;
    protected List entries;
    protected Hashtable taxes;

    public double getTotal() {
        return this.total;
    }

    public List getEntries() {
        return this.entries;
    }

    public Invoice(List entries, double total) {
        this.subtotal = total;
        this.total = total;
        this.entries = entries;
        if (total != 0.0d) {
            this.free = "0";
        } else {
            this.free = "1";
        }
    }

    protected Invoice() {
        this.subtotal = 0.0d;
        this.total = 0.0d;
        this.free = "1";
        this.entries = new LinkedList();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return Resource.STATUS_OK;
        }
        if ("localized_total".equals(key)) {
            DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance();
            DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
            dfs.setCurrencySymbol("");
            df.setDecimalFormatSymbols(dfs);
            return new TemplateString(df.format(this.total));
        } else if ("total".equals(key)) {
            return new TemplateString(this.total);
        } else {
            if ("subtotal".equals(key)) {
                return new TemplateString(this.subtotal);
            }
            if ("entries".equals(key)) {
                return new ListAdapter(this.entries);
            }
            if ("taxes".equals(key)) {
                return new TemplateHash(this.taxes);
            }
            if ("free".equals(key)) {
                return new TemplateString(this.free);
            }
            throw new TemplateModelException("No such key (psoft.hsphere.Invoice): " + key);
        }
    }

    public static Invoice getInvoice() {
        try {
            String planId = Session.getRequest().getParameter("plan_id");
            Plan p = Plan.getPlan(planId);
            if (p.getBilling() == 0) {
                return new Invoice();
            }
            String modId = Session.getRequest().getParameter("_mod");
            String periodId = Session.getRequest().getParameter("_bp");
            String promoCode = Session.getRequest().getParameter("_promo_code");
            return getInvoice(p, modId, Integer.parseInt(periodId), promoCode);
        } catch (Exception e) {
            Session.getLog().warn("Unable to generate invoice", e);
            return null;
        }
    }

    public static Invoice getInvoice(String modId) {
        try {
            String planId = Session.getRequest().getParameter("plan_id");
            Plan p = Plan.getPlan(planId);
            if (p.getBilling() == 0) {
                return new Invoice();
            }
            String periodId = Session.getRequest().getParameter("_bp");
            String promoCode = Session.getRequest().getParameter("_promo_code");
            return getInvoice(p, modId, Integer.parseInt(periodId), promoCode);
        } catch (Exception e) {
            Session.getLog().error("Unable to generate invoice", e);
            return null;
        }
    }

    public static Invoice getInvoice(Plan plan, String modId, int periodId, String promoCode) throws Exception {
        Date start = TimeUtils.getDate();
        Date end = plan.getNextPaymentDate(start, periodId);
        TypeCounter typeCounter = new TypeCounter();
        InitToken token = new InitToken(plan, periodId, typeCounter);
        token.setRange(start, end);
        LinkedList l = new LinkedList();
        LinkedList mod = new LinkedList();
        l.add(new InitResource(0, modId, 0));
        mod.add(modId);
        List result = new LinkedList();
        double total = 0.0d;
        while (!l.isEmpty()) {
            InitResource current = (InitResource) l.getFirst();
            String modId2 = (String) mod.getFirst();
            Collection initValues = plan.getDefaultInitValues(token, current.getType(), current.getMod(modId2));
            token.set(current.getType(), initValues);
            typeCounter.inc(current.getType(), token.getAmount());
            Collection<InitResource> resources = plan.getInitResources(current.getType(), current.getMod(modId2));
            if (null != resources) {
                for (InitResource rType : resources) {
                    if (!rType.isDisabled() && !plan.getResourceType(rType.getType()).isDisabled()) {
                        l.addLast(rType);
                        mod.add(rType.getMod(modId2));
                    }
                }
            }
            String setupCalc = plan.getValue(current.getType(), "_SETUP_CALC");
            String recurrentCalc = plan.getValue(current.getType(), "_RECURRENT_CALC");
            if (!isEmpty(setupCalc) || !isEmpty(recurrentCalc)) {
                token.getDescription();
                if (!isEmpty(setupCalc)) {
                    double setupFee = Resource.calc(setupCalc, token);
                    if (setupFee != 0.0d && !Double.isNaN(setupFee)) {
                        StringBuffer discountComment = new StringBuffer();
                        double discount = Resource.calculatePromoDiscount(plan, 1, setupFee, discountComment, promoCode, current.getType());
                        double setupFee2 = setupFee - discount;
                        result.add(new InvoiceEntry(1, token.getSetupChargeDescription(start) + discountComment.toString(), setupFee2));
                        total += setupFee2;
                    }
                }
                if (!isEmpty(recurrentCalc)) {
                    double rFee = Resource.calc(recurrentCalc, token);
                    if (rFee != 0.0d && !Double.isNaN(rFee)) {
                        StringBuffer discountComment2 = new StringBuffer();
                        double discount2 = Resource.calculatePromoDiscount(plan, 2, rFee, discountComment2, promoCode, current.getType());
                        double rFee2 = rFee - discount2;
                        result.add(new InvoiceEntry(2, token.getRecurrentChangeDescripton(start, end) + discountComment2.toString(), rFee2));
                        total += rFee2;
                    }
                }
            }
            l.removeFirst();
            mod.removeFirst();
        }
        return new Invoice(result, total);
    }

    protected static String getDescription(InitToken token) throws Exception {
        return token.getResourceType().getType();
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
