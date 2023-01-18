package psoft.hsphere.billing.estimators;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Bill;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.InitToken;
import psoft.hsphere.InvoiceEntry;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TypeCounter;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/EstimateCreateCopies.class */
public class EstimateCreateCopies implements TemplateMethodModel {
    private static Category log = null;
    private Resource resource;

    protected synchronized Category getLog() {
        if (log == null) {
            log = Category.getInstance(EstimateCreateMass.class.getName());
        }
        return log;
    }

    public EstimateCreateCopies(Resource resource) {
        this.resource = resource;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        TemplateModel estimateCreateCopies;
        try {
            l = HTMLEncoder.decode(l);
            synchronized (Session.getAccount()) {
                estimateCreateCopies = estimateCreateCopies((String) l.get(0), new Integer((String) l.get(1)).intValue(), (String) l.get(2), l.subList(3, l.size()));
            }
            return estimateCreateCopies;
        } catch (Exception t) {
            if (t instanceof HSUserException) {
                return new TemplateErrorResult(t.getMessage());
            }
            getLog().warn("Error estimating price: " + l, t);
            Ticket.create(t, this, "EstimateCreateCopies: type = " + ((String) l.get(0)) + ", number = " + ((Integer) l.get(1)).intValue() + ", mod = " + ((String) l.get(2)) + ", params = " + l.subList(3, l.size()));
            return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
        }
    }

    public TemplateModel estimateCreateCopies(String type, int number, String modId, Collection values) throws Exception {
        Collection initValues;
        Date now = TimeUtils.getDate();
        getResource().getAccount().charge();
        getResource().getAccount().sendInvoice(now);
        Hashtable res = new Hashtable();
        int periodId = getResource().getAccount().getPeriodId();
        Plan plan = getResource().getAccount().getPlan();
        Date start = TimeUtils.dropMinutes(TimeUtils.getDate());
        Date end = getResource().getAccount().getPeriodEnd();
        long periodSize = getResource().getAccount().getPeriodSize();
        TypeCounter typeCounter = new TypeCounter(getResource().getAccount().getTypeCounter());
        InitToken token = new InitToken(plan, periodId, typeCounter);
        LinkedList l = new LinkedList();
        LinkedList mod = new LinkedList();
        int typeId = Integer.parseInt(TypeRegistry.getTypeId(type));
        List result = new LinkedList();
        double setupTotal = 0.0d;
        double recurrTotal = 0.0d;
        double recurrTotalAll = 0.0d;
        double discountTotal = 0.0d;
        HashMap addedEntries = new HashMap();
        HashMap numberWithoutFree = new HashMap();
        for (int cnt = 0; cnt < number; cnt++) {
            InitResource rType = new InitResource(typeId, modId, 0);
            l.add(rType);
            mod.add(rType.getMod(modId));
            if (addedEntries.get(new Integer(typeId)) == null) {
                addedEntries.put(new Integer(typeId), new Integer(0));
            }
            if (numberWithoutFree.get(new Integer(typeId)) == null) {
                numberWithoutFree.put(new Integer(typeId), new Integer(number));
            }
            while (!l.isEmpty()) {
                InitResource current = (InitResource) l.getFirst();
                modId = (String) mod.getFirst();
                if (typeId == current.getType() && values != null && !values.isEmpty()) {
                    initValues = values;
                } else {
                    initValues = plan.getDefaultInitValues(token, current.getType(), current.getMod(modId));
                }
                token.setRange(start, end, periodSize);
                token.set(current.getType(), initValues);
                typeCounter.inc(current.getType(), token.getAmount());
                Collection<InitResource> resources = plan.getInitResources(current.getType(), current.getMod(modId));
                if (null != resources) {
                    for (InitResource rType2 : resources) {
                        if (!rType2.isDisabled() && (plan.getResourceType(rType2.getType()) == null || !plan.getResourceType(rType2.getType()).isDisabled())) {
                            l.addLast(rType2);
                            mod.add(rType2.getMod(modId));
                            if (addedEntries.get(new Integer(rType2.getType())) == null) {
                                addedEntries.put(new Integer(rType2.getType()), new Integer(0));
                            }
                            if (numberWithoutFree.get(new Integer(rType2.getType())) == null) {
                                numberWithoutFree.put(new Integer(rType2.getType()), new Integer(number));
                            }
                        }
                    }
                }
                String setupCalc = plan.getValue(current.getType(), "_SETUP_CALC");
                String recurrentCalc = plan.getValue(current.getType(), "_RECURRENT_CALC");
                if (setupCalc != null && setupCalc.length() == 0) {
                    setupCalc = null;
                }
                if (recurrentCalc != null && recurrentCalc.length() == 0) {
                    recurrentCalc = null;
                }
                if (setupCalc != null || recurrentCalc != null) {
                    double setupFee = 0.0d;
                    if (setupCalc != null) {
                        setupFee = Resource.calc(setupCalc, token);
                        if (setupFee != 0.0d && !Double.isNaN(setupFee)) {
                            StringBuffer discountComment = new StringBuffer();
                            double discount = Resource.calculatePromoDiscount(1, setupFee, discountComment, current.getType());
                            setupFee -= discount;
                            discountTotal += discount;
                            if (((Integer) addedEntries.get(new Integer(token.getResourceType().getId()))).intValue() == 0) {
                                int numberWithoutFree1 = ((Integer) numberWithoutFree.get(new Integer(token.getResourceType().getId()))).intValue();
                                result.add(new InvoiceEntry(1, token.getSetupChargeDescription(start) + discountComment.toString() + " " + Localizer.translateMessage("bill.create_resources", new Object[]{new Integer(numberWithoutFree1).toString(), HsphereToolbox.translateCurrency(new Double(USFormat.format(setupFee)).doubleValue())}), setupFee * numberWithoutFree1));
                                setupTotal += setupFee * numberWithoutFree1;
                                addedEntries.put(new Integer(token.getResourceType().getId()), new Integer(1));
                            }
                        }
                    }
                    if (recurrentCalc != null) {
                        double rFee = Resource.calc(recurrentCalc, token);
                        if (rFee != 0.0d && !Double.isNaN(rFee)) {
                            StringBuffer discountComment2 = new StringBuffer();
                            double discount2 = Resource.calculatePromoDiscount(2, rFee, discountComment2, current.getType());
                            double rFee2 = rFee - discount2;
                            discountTotal += discount2;
                            if (((Integer) addedEntries.get(new Integer(token.getResourceType().getId()))).intValue() != 2) {
                                int numberWithoutFree12 = ((Integer) numberWithoutFree.get(new Integer(token.getResourceType().getId()))).intValue();
                                result.add(new InvoiceEntry(2, token.getRecurrentChangeDescripton(start, end) + discountComment2.toString() + " " + Localizer.translateMessage("bill.create_resources", new Object[]{new Integer(numberWithoutFree12).toString(), HsphereToolbox.translateCurrency(new Double(USFormat.format(rFee2)).doubleValue())}), rFee2 * numberWithoutFree12));
                                recurrTotal += rFee2 * numberWithoutFree12;
                                addedEntries.put(new Integer(token.getResourceType().getId()), new Integer(2));
                            }
                        } else if (rFee == 0.0d && setupFee == 0.0d) {
                            numberWithoutFree.put(new Integer(token.getResourceType().getId()), new Integer(((Integer) numberWithoutFree.get(new Integer(token.getResourceType().getId()))).intValue() - 1));
                        }
                        token.setRange(getResource().getAccount().getPeriodBegin(), end, periodSize);
                        double rFee3 = Resource.calc(recurrentCalc, token);
                        if (rFee3 != 0.0d && !Double.isNaN(rFee3)) {
                            recurrTotalAll += rFee3;
                        }
                    }
                }
                l.removeFirst();
                mod.removeFirst();
            }
        }
        double setupTotal2 = Resource.fix(setupTotal);
        double recurrTotal2 = Resource.fix(recurrTotal);
        double recurrTotalAll2 = Resource.fix(recurrTotalAll);
        double total = setupTotal2 + recurrTotal2;
        if (getResource().getAccount().getBillingInfo().getBillingType() != 1) {
            Bill bill = getResource().getAccount().getBill();
            if (bill.getBalance() - total < (-bill.getCredit())) {
                throw new HSUserException("resource.credit");
            }
        }
        res.put("entries", new TemplateList(result));
        res.put("total", total > 0.0d ? USFormat.format(total) : "0");
        res.put("setup", setupTotal2 > 0.0d ? USFormat.format(setupTotal2) : "0");
        res.put("recurrent", recurrTotal2 > 0.0d ? USFormat.format(recurrTotal2) : "0");
        res.put("recurrentAll", recurrTotalAll2 > 0.0d ? USFormat.format(recurrTotalAll2) : "0");
        res.put("free", (setupTotal2 == 0.0d && recurrTotalAll2 == 0.0d) ? "1" : "0");
        res.put("discount", discountTotal > 0.0d ? USFormat.format(discountTotal) : "0");
        return new TemplateHash(res);
    }

    public Resource getResource() {
        return this.resource;
    }
}
