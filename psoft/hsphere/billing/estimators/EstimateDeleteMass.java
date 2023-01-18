package psoft.hsphere.billing.estimators;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/EstimateDeleteMass.class */
public class EstimateDeleteMass implements TemplateMethodModel {
    List deleteList = new ArrayList();
    private Resource resource;

    public EstimateDeleteMass(Resource resource) {
        this.resource = resource;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        TemplateModel result;
        try {
            List l2 = HTMLEncoder.decode(l);
            if (l2.isEmpty()) {
                try {
                    synchronized (Session.getAccount()) {
                        result = estimateDeleteMass(this.deleteList);
                        this.deleteList.clear();
                    }
                    return result;
                } catch (Exception t) {
                    if (t instanceof HSUserException) {
                        return new TemplateErrorResult(t.getMessage());
                    }
                    Ticket.create(t, this, "EstimateDeleteMass: " + l2);
                    return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
                }
            }
            String rid = (String) l2.get(0);
            if (rid != null) {
                ResourceId rrid = new ResourceId(rid);
                this.deleteList.add(rrid);
                return null;
            }
            return null;
        } catch (Exception e) {
            Session.getLog().error("Error estimate delete resources list ", e);
            return new TemplateErrorResult(e);
        }
    }

    protected TemplateModel estimateDeleteMass(List ll) throws Exception {
        Resource rs = null;
        Hashtable res = new Hashtable();
        double total = 0.0d;
        double setup = 0.0d;
        double sub_total = 0.0d;
        double refund = 0.0d;
        boolean z = "1";
        for (int i = 0; i < ll.size(); i++) {
            try {
                rs = ((ResourceId) ll.get(i)).get();
            } catch (Exception e) {
                Session.getLog().debug("Can't get resource: " + ll.get(i));
            }
            if (rs != null) {
                TemplateHash tt = rs.FM_estimateDelete();
                total += USFormat.parseDouble(tt.get("total").toString());
                sub_total += USFormat.parseDouble(tt.get("sub_total").toString());
                setup += USFormat.parseDouble(tt.get("setup").toString());
                refund += USFormat.parseDouble(tt.get("refund").toString());
                if (tt.get("free").toString().equals("0")) {
                    z = "0";
                }
            }
        }
        res.put("total", total != 0.0d ? USFormat.format(total) : "0");
        res.put("sub_total", sub_total != 0.0d ? USFormat.format(sub_total) : "0");
        res.put("setup", setup > 0.0d ? USFormat.format(setup) : "0");
        res.put("refund", refund > 0.0d ? USFormat.format(refund) : "0");
        res.put("free", z);
        return new TemplateHash(res);
    }
}
