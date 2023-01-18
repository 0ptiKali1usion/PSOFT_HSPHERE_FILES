package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/RadioList.class */
public class RadioList extends BaseParamsList implements TemplateHashModel, TemplateMethodModel {
    public RadioList(BaseParam param) {
        super(param, " ");
    }

    public RadioList(BaseParam param, String delim) {
        super(param, delim);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList
    protected void parseParamsList() {
        this.params = new ArrayList();
        StringTokenizer st = new StringTokenizer(this.defaultParamValue, this.delim);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            BaseParamImpl base = new BaseParamImpl(this.currParamName, value, value, this.type, this.description, this.help, this.customParams);
            this.params.add(new CheckParam(base));
        }
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            if (this.currParamValue.indexOf(param.currParamValue) >= 0) {
                param.setChecked("On");
            } else {
                param.setChecked("Off");
            }
        }
    }

    protected void uncheckAll() {
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            param.setChecked("Off");
        }
    }

    public void setValue(String name, String value) {
        uncheckAll();
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            if (param.currParamValue.equalsIgnoreCase(value)) {
                this.currParamValue = value;
                param.setChecked("On");
                setChanged(true);
            }
        }
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        try {
            List l2 = HTMLEncoder.decode(l);
            if (l2.isEmpty()) {
                return null;
            }
            String key = (String) l2.get(0);
            String value = (String) l2.get(1);
            setValue(key, value);
            return null;
        } catch (Exception e) {
            Session.getLog().error("Set physical server params error", e);
            return new TemplateErrorResult(e);
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new RadioList(this);
    }
}
