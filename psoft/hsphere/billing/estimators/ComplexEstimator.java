package psoft.hsphere.billing.estimators;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeCounter;
import psoft.hsphere.cache.CacheableObject;
import psoft.hsphere.cache.storedobjects.StoredObject;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/ComplexEstimator.class */
public class ComplexEstimator implements CacheableObject, StoredObject, TemplateHashModel {
    private static transient Category log = null;

    /* renamed from: id */
    private long f77id;
    private long accountId;
    private int currentStepId = 0;
    Hashtable objects2create = new Hashtable();
    protected String type;
    protected String mode;
    protected Collection initValues;

    public ComplexEstimator(long id, long accountId) {
        this.f77id = id;
        this.accountId = accountId;
    }

    public String getCEId() {
        return getAccount() + "-" + getId();
    }

    public long getAccount() {
        return this.accountId;
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f77id;
    }

    public synchronized TemplateModel addResourceInitializer(ResourceInitializer ri) throws TemplateModelException {
        if (getCurrentStepId() > 0) {
            getCurrentObjects2Create().add(ri);
            Session.getLog().debug("Inside ComplexEstimator::addResourceInitializer added new resource initializer: " + ri.toString());
            Session.getLog().debug("NOW size is " + getObjects2create().size());
            return this;
        }
        throw new TemplateModelException("No steps are defined for the complex estimator with id = " + getId());
    }

    private List getCurrentObjects2Create() {
        return (List) getObjects2create().get(new Integer(getCurrentStepId()));
    }

    private Hashtable getObjects2create() {
        return this.objects2create;
    }

    public List getAllObjects2Create() {
        List result = new ArrayList();
        synchronized (this.objects2create) {
            for (Object key : getObjects2create().keySet()) {
                result.addAll((Collection) getObjects2create().get(key));
            }
        }
        return result;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    protected synchronized Category getLog() {
        if (log == null) {
            log = Category.getInstance(EstimateCreateMass.class.getName());
        }
        return log;
    }

    public Hashtable estimateCreate() throws Exception {
        Hashtable res = new Hashtable();
        double total = 0.0d;
        double setup = 0.0d;
        double recurrent = 0.0d;
        double recurrentAll = 0.0d;
        boolean z = "1";
        List entries = new LinkedList();
        Session.getLog().debug("Inside ComplexEstimator::estimateCreate objects to estimate " + getObjects2create().size());
        TypeCounter typeCounter = new TypeCounter(Session.getAccount().getTypeCounter());
        for (ResourceInitializer ri : getAllObjects2Create()) {
            TemplateHash tt = ri.getParent().get().estimateCreate(typeCounter, ri.getType(), ri.getMode(), ri.getInitValues());
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
        return res;
    }

    public TemplateHash FM_estimateCreate() throws Exception {
        try {
            Hashtable res = estimateCreate();
            Session.getCacheFactory().getLockableCache(ComplexEstimator.class).unlock(getId());
            return new TemplateHash(res);
        } catch (Throwable th) {
            Session.getCacheFactory().getLockableCache(ComplexEstimator.class).unlock(getId());
            throw th;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "id".equals(key) ? new TemplateString(getCEId()) : "currentStepId".equals(key) ? new TemplateString(getCurrentStepId()) : "addResource".equals(key) ? new addResourceInitializer() : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public int getCurrentStepId() {
        return this.currentStepId;
    }

    public TemplateModel FM_setCurrentStepId(int _currentStepId) {
        setCurrentStepId(_currentStepId);
        return this;
    }

    public void setCurrentStepId(int _currentStepId) {
        List<Object> keysToRemove = new ArrayList();
        synchronized (this.objects2create) {
            for (Integer num : getObjects2create().keySet()) {
                int key = num.intValue();
                if (key == _currentStepId || key > _currentStepId) {
                    keysToRemove.add(new Integer(key));
                }
            }
            for (Object obj : keysToRemove) {
                getObjects2create().remove(obj);
            }
        }
        getObjects2create().put(new Integer(_currentStepId), new ArrayList());
        this.currentStepId = _currentStepId;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/ComplexEstimator$addResourceInitializer.class */
    class addResourceInitializer implements TemplateMethodModel {
        addResourceInitializer() {
            ComplexEstimator.this = r4;
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            List l2 = HTMLEncoder.decode(l);
            return ComplexEstimator.this.addResourceInitializer(new ResourceInitializer(new ResourceId((String) l2.get(0)), (String) l2.get(1), (String) l2.get(2), l2.subList(3, l2.size())));
        }
    }
}
