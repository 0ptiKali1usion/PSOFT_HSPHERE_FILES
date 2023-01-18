package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/Estimator.class */
public class Estimator implements TemplateMethodModel, TemplateHashModel {
    private static Category log = Category.getInstance(Estimator.class.getName());
    boolean isFree = true;
    double total = 0.0d;
    double setup = 0.0d;
    double recurrent = 0.0d;
    double recurrentAll = 0.0d;
    TemplateList entries = new TemplateList();
    TemplateHash result;

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("result".equals(key)) {
            finish();
            return this.result;
        }
        return null;
    }

    public TemplateModel exec(List list) throws TemplateModelException {
        try {
            list = HTMLEncoder.decode(list);
            synchronized (Session.getAccount()) {
                estimate(list);
            }
            return this;
        } catch (Exception t) {
            if (t instanceof HSUserException) {
                return new TemplateErrorResult(t.getMessage());
            }
            log.warn("Error estimating price: " + list, t);
            Ticket.create(t, this, "Estimator:" + list);
            return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
        }
    }

    void estimate(List l) throws Exception {
        estimate(new ResourceId((String) l.get(0)), (String) l.get(1), (String) l.get(2), l.size() > 3 ? l.subList(3, l.size()) : new ArrayList());
    }

    void estimate(ResourceId rid, String type, String mod, List params) throws Exception {
        Resource r = rid.get();
        TemplateHash result = (TemplateHash) r.estimateCreate(type, mod, params);
        TemplateList entries = result.get("entries");
        this.total += getAsDouble(result, "total");
        this.setup += getAsDouble(result, "setup");
        this.recurrent += getAsDouble(result, "recurrent");
        this.recurrentAll += getAsDouble(result, "recurrentAll");
        entries.rewind();
        while (entries.hasNext()) {
            this.entries.add(entries.next());
        }
        if (this.isFree && result.get("free").toString().equals("0")) {
            this.isFree = false;
        }
    }

    void finish() {
        this.result = new TemplateHash();
        this.result.put("entries", this.entries);
        this.result.put("total", toTemplateModel(this.total));
        this.result.put("setup", toTemplateModel(this.setup));
        this.result.put("recurrent", toTemplateModel(this.recurrent));
        this.result.put("recurrentAll", toTemplateModel(this.recurrentAll));
        this.result.put("free", this.isFree ? "1" : "0");
    }

    private String toTemplateModel(double amount) {
        if (amount > 0.0d) {
            try {
                return USFormat.format(amount);
            } catch (ParseException pe) {
                log.error("Parse Exception", pe);
                Ticket.create(pe, this, new Double(amount));
                return "0";
            }
        }
        return "0";
    }

    private double getAsDouble(TemplateHash result, String key) throws ParseException {
        return USFormat.parseDouble(result.get(key).toString());
    }
}
