package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/Params.class */
public class Params implements TemplateListModel, TemplateMethodModel, TemplateHashModel {
    protected ArrayList paramsList = new ArrayList();
    private ListIterator iterator;

    public boolean isEmpty() throws TemplateModelException {
        return this.paramsList.isEmpty();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return null;
    }

    private void checkIterator() {
        if (this.iterator == null) {
            this.iterator = this.paramsList.listIterator();
        }
    }

    public void rewind() throws TemplateModelException {
        this.iterator = null;
    }

    public boolean isRewound() throws TemplateModelException {
        return this.iterator == null;
    }

    public boolean hasNext() throws TemplateModelException {
        checkIterator();
        return this.iterator.hasNext();
    }

    public TemplateModel next() throws TemplateModelException {
        checkIterator();
        if (this.iterator.hasNext()) {
            return (BaseParam) this.iterator.next();
        }
        throw new TemplateModelException("No more elements.");
    }

    public int size() {
        return this.paramsList.size();
    }

    public Iterator iterator() {
        return this.paramsList.iterator();
    }

    public TemplateModel get(int index) {
        return (BaseParam) this.paramsList.get(index);
    }

    public void add(BaseParam param) {
        this.paramsList.add(param);
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        try {
            List l2 = HTMLEncoder.decode(l);
            if (l2.isEmpty()) {
                return null;
            }
            String key = (String) l2.get(0);
            String value = (String) l2.get(1);
            BaseParam param = getParam(key);
            if (param == null) {
                throw new Exception("Parameter - " + key + " not found");
            }
            setValue(key, value);
            return null;
        } catch (Exception e) {
            Session.getLog().error("Set params error", e);
            return new TemplateErrorResult(e);
        }
    }

    protected void setValue(String name, String value) throws Exception {
        BaseParam param = getParam(name);
        if (param != null) {
            param.setValue(value);
            return;
        }
        throw new Exception("Parameter - " + name + " not found");
    }

    public boolean isExistParam(String name) {
        BaseParam param = getParam(name);
        if (param != null) {
            return true;
        }
        return false;
    }

    public TemplateModel FM_getParam(String name) {
        return getParam(name);
    }

    public BaseParam getParam(String name) {
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (param.isParam(name)) {
                return param;
            }
        }
        return null;
    }

    public Params copy() {
        Params copy = new Params();
        Iterator i = this.paramsList.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            copy.add(param.copy());
        }
        return copy;
    }
}
