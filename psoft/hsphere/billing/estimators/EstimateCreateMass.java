package psoft.hsphere.billing.estimators;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeCounter;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/EstimateCreateMass.class */
public class EstimateCreateMass implements TemplateMethodModel {
    private Resource resource;
    private List resList = null;
    private static Category log = null;
    static final String estimateCreateMassResourceSeparator = "|";

    public EstimateCreateMass(Resource resource) {
        this.resource = resource;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        TemplateModel result;
        List l2 = HTMLEncoder.decode(l);
        if (this.resList == null) {
            this.resList = new LinkedList();
        }
        if (l2.isEmpty()) {
            try {
                synchronized (Session.getAccount()) {
                    result = estimateCreateMass(this.resList);
                    this.resList.clear();
                }
                return result;
            } catch (Exception t) {
                if (t instanceof HSUserException) {
                    return new TemplateErrorResult(t.getMessage());
                }
                getLog().warn("Error estimating price: " + l2, t);
                Ticket.create(t, this, "EstimateCreateMass: " + l2);
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        }
        this.resList.addAll(l2);
        this.resList.add(estimateCreateMassResourceSeparator);
        return new TemplateOKResult();
    }

    protected synchronized Category getLog() {
        if (log == null) {
            log = Category.getInstance(EstimateCreateMass.class.getName());
        }
        return log;
    }

    public TemplateModel estimateCreateMass(List ll) throws Exception {
        Hashtable res = new Hashtable();
        List entries = new LinkedList();
        List<ResourceInitializer> resources = new LinkedList();
        boolean useValues = ll.contains(estimateCreateMassResourceSeparator);
        Iterator i = ll.iterator();
        while (i.hasNext()) {
            String rmod = "";
            Collection values = new ArrayList();
            String rtype = (String) i.next();
            if (i.hasNext()) {
                rmod = (String) i.next();
            }
            if (useValues) {
                while (i.hasNext()) {
                    String value = (String) i.next();
                    if (estimateCreateMassResourceSeparator.equals(value)) {
                        break;
                    }
                    values.add(value);
                }
            }
            resources.add(new ResourceInitializer(rtype, rmod, values));
        }
        double total = 0.0d;
        double setup = 0.0d;
        double recurrent = 0.0d;
        double recurrentAll = 0.0d;
        boolean z = "1";
        TypeCounter typeCounter = new TypeCounter(Session.getAccount().getTypeCounter());
        for (ResourceInitializer ri : resources) {
            TemplateHash tt = this.resource.estimateCreate(typeCounter, ri.getType(), ri.getMode(), ri.getInitValues());
            TemplateList x = tt.get("entries");
            total += USFormat.parseDouble(tt.get("total").toString());
            setup += USFormat.parseDouble(tt.get("setup").toString());
            recurrent += USFormat.parseDouble(tt.get("recurrent").toString());
            recurrentAll += USFormat.parseDouble(tt.get("recurrentAll").toString());
            entries.addAll(x.getCollection());
            if (tt.get("free").toString().equals("0")) {
                z = "0";
            }
        }
        res.put("entries", new TemplateList(entries));
        res.put("total", total > 0.0d ? USFormat.format(total) : "0");
        res.put("setup", setup > 0.0d ? USFormat.format(setup) : "0");
        res.put("recurrent", recurrent > 0.0d ? USFormat.format(recurrent) : "0");
        res.put("recurrentAll", recurrentAll > 0.0d ? USFormat.format(recurrentAll) : "0");
        res.put("free", z);
        return new TemplateHash(res);
    }
}
