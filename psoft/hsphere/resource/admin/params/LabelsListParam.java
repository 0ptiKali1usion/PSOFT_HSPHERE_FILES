package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/LabelsListParam.class */
public class LabelsListParam extends BaseParamsList {
    public LabelsListParam(BaseParam param) {
        super(param, ",");
    }

    public LabelsListParam(BaseParam param, String delim) {
        super(param, delim);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList
    protected void parseParamsList() {
        this.params = new ArrayList();
        StringTokenizer st = new StringTokenizer(this.currParamValue, this.delim);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            BaseParamImpl base = new BaseParamImpl(this.currParamName, value, value, this.type, this.description, this.help, this.customParams);
            LabelParam param = new LabelParam(base);
            this.params.add(param);
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    public TemplateModel FM_getParamValue() {
        return new TemplateString(this.currParamValue);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValueFlags() {
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        return null;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new LabelsListParam(this);
    }
}
