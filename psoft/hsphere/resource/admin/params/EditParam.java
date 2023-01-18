package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/EditParam.class */
public class EditParam extends BaseParam implements TemplateHashModel {
    private int minSize;
    private int maxSize;

    public EditParam(BaseParam param) {
        super(param);
        this.minSize = 20;
        this.maxSize = 56;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValueFlags() {
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("param_size")) {
            if (this.currParamValue.length() > this.maxSize) {
                return new TemplateString(this.maxSize);
            }
            if (this.currParamValue.length() < this.minSize) {
                return new TemplateString(this.minSize);
            }
            return new TemplateString(this.currParamValue.length());
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        if (value == null) {
            value = "";
        }
        super.setValue(value);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new EditParam(this);
    }
}
