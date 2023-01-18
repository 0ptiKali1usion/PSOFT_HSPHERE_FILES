package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/LabelParam.class */
public class LabelParam extends BaseParam implements TemplateHashModel {
    public LabelParam(BaseParam param) {
        super(param);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValueFlags() {
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void init(String value) {
        super.setValue(value);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new LabelParam(this);
    }
}
