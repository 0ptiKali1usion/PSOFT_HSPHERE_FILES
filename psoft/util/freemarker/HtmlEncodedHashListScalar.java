package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/HtmlEncodedHashListScalar.class */
public class HtmlEncodedHashListScalar implements TemplateHashModel, TemplateListModel, TemplateScalarModel {
    TemplateModel templateModel;

    public HtmlEncodedHashListScalar(TemplateHashModel templateModel) {
        this.templateModel = templateModel;
    }

    public HtmlEncodedHashListScalar(TemplateListModel templateModel) {
        this.templateModel = templateModel;
    }

    private HtmlEncodedHashListScalar(TemplateModel templateModel) {
        this.templateModel = templateModel;
    }

    public boolean isEmpty() throws TemplateModelException {
        return this.templateModel.isEmpty();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (!(this.templateModel instanceof TemplateHashModel)) {
            return null;
        }
        TemplateModel res = this.templateModel.get(key);
        if (res != null && ((res instanceof TemplateScalarModel) || (res instanceof TemplateListModel) || (res instanceof TemplateHashModel))) {
            return new HtmlEncodedHashListScalar(res);
        }
        return res;
    }

    public void rewind() throws TemplateModelException {
        if (this.templateModel instanceof TemplateListModel) {
            this.templateModel.rewind();
        }
    }

    public boolean isRewound() throws TemplateModelException {
        if (this.templateModel instanceof TemplateListModel) {
            return this.templateModel.isRewound();
        }
        return false;
    }

    public boolean hasNext() throws TemplateModelException {
        if (this.templateModel instanceof TemplateListModel) {
            return this.templateModel.hasNext();
        }
        return false;
    }

    public TemplateModel next() throws TemplateModelException {
        if (this.templateModel instanceof TemplateListModel) {
            return new HtmlEncodedHashListScalar(this.templateModel.next());
        }
        return null;
    }

    public TemplateModel get(int i) throws TemplateModelException {
        if (this.templateModel instanceof TemplateListModel) {
            return new HtmlEncodedHashListScalar(this.templateModel.get(i));
        }
        return null;
    }

    public String getAsString() throws TemplateModelException {
        if (this.templateModel != null && (this.templateModel instanceof TemplateScalarModel)) {
            return HTMLEncoder.encode(this.templateModel.getAsString());
        }
        return null;
    }
}
