package psoft.hsphere.async;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMethodWrapper;

/* loaded from: hsphere.zip:psoft/hsphere/async/AsyncManager.class */
public class AsyncManager implements TemplateHashModel {
    private static final Category log = Category.getInstance(AsyncManager.class.getName());

    /* renamed from: AM */
    private static final AsyncManager f67AM = new AsyncManager();
    Collection descriptors = new ArrayList();
    Boolean loadedDescription = Boolean.FALSE;
    boolean processing = false;

    private AsyncManager() {
    }

    public static AsyncManager getManager() {
        return f67AM;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public Collection getDescriptors() {
        Collection unmodifiableCollection;
        synchronized (this.descriptors) {
            if (!this.loadedDescription.booleanValue()) {
                try {
                    this.descriptors.clear();
                    this.descriptors.addAll(AsyncDescriptor.load());
                    this.loadedDescription = Boolean.TRUE;
                } catch (SQLException e) {
                    log.error("Unable to load async descriptors", e);
                    return new ArrayList();
                }
            }
            unmodifiableCollection = Collections.unmodifiableCollection(this.descriptors);
        }
        return unmodifiableCollection;
    }

    public void check() throws SQLException {
        synchronized (this) {
            if (this.processing) {
                return;
            }
            this.processing = true;
            synchronized (this.descriptors) {
                ArrayList arrayList = new ArrayList();
                List done = new ArrayList();
                List deleted = new ArrayList();
                for (AsyncDescriptor d : getDescriptors()) {
                    try {
                        d.check();
                        d.update();
                    } catch (Exception e) {
                        handleException(e, d);
                    }
                    switch (d.state) {
                        case 0:
                            done.add(d);
                            break;
                        case 5:
                            deleted.add(d);
                            break;
                        default:
                            arrayList.add(d);
                            break;
                    }
                    processDone(done);
                    processCanceled(deleted);
                }
                this.descriptors.clear();
                if (arrayList.size() > 0) {
                    this.descriptors.addAll(arrayList);
                }
                synchronized (this) {
                    this.processing = false;
                }
            }
        }
    }

    public void add(AsyncResource r) throws SQLException {
        getDescriptors();
        synchronized (this.descriptors) {
            this.descriptors.add(new AsyncDescriptor(r));
        }
    }

    void processCanceled(Collection deleted) {
        sendMail("ASYNC_CANCELED", deleted);
        removeRecords(deleted);
    }

    void processDone(Collection done) {
        sendMail("ASYNC_DONE", done);
        removeRecords(done);
    }

    void sendMail(String tag, Collection col) {
        if (col.isEmpty()) {
            return;
        }
        SimpleHash root = new SimpleHash();
        root.put("d", new TemplateList(col));
        root.put("toolbox", HsphereToolbox.toolbox);
        try {
            Session.setResellerId(1L);
            CustomEmailMessage.send("ASYNC_DONE", root);
        } catch (Exception e) {
            log.error("Error while sending custom email", e);
        }
    }

    void removeRecords(Collection col) {
        Iterator i = col.iterator();
        while (i.hasNext()) {
            AsyncDescriptor d = (AsyncDescriptor) i.next();
            try {
                d.remove();
            } catch (SQLException e) {
                log.error("Error removing async_desc# " + d.getRid(), e);
            }
        }
    }

    private void handleException(Exception e, AsyncDescriptor d) {
        log.error("Error doing check on asycn_desc# " + d.getRid(), e);
        d.setError(100, e.getMessage());
        try {
            d.update();
        } catch (SQLException e1) {
            log.warn("Unable to save async descriptor:" + d.getRid(), e1);
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "descriptors".equals(key) ? new TemplateList(getDescriptors()) : TemplateMethodWrapper.getMethod(this, key);
    }

    private AsyncDescriptor find(String rid) throws Exception {
        for (AsyncDescriptor ad : getDescriptors()) {
            if (ad.getRid().toString().equals(rid)) {
                return ad;
            }
        }
        throw new Exception("Unknown async process: " + rid);
    }

    public void FM_cancel(String rid) throws Exception {
        find(rid).cancel();
    }

    public void FM_reset(String rid) throws Exception {
        try {
            AsyncDescriptor ad = find(rid);
            ad.reset();
            ad.update();
        } catch (Exception e) {
            log.error("UNABLE TO RESET: ", e);
        }
    }

    public void FM_check(String rid) throws Exception {
        AsyncDescriptor ad = find(rid);
        ad.reset();
        ad.check();
        ad.update();
    }
}
