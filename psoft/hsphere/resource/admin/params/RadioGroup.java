package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/RadioGroup.class */
public class RadioGroup extends BaseParamsList {
    public RadioGroup(String name) {
        super(new BaseParamImpl(name, null, null, "radiogroup", null, null, null), "");
    }

    public RadioGroup(String name, String[] customParams) {
        super(new BaseParamImpl(name, null, null, "radiogroup", null, null, customParams), "");
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList
    protected void parseParamsList() {
    }

    protected void uncheckAll() {
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            param.setValue(false);
        }
    }

    public void setValue(String name, String value) {
        uncheckAll();
        StringBuffer input = new StringBuffer();
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            if (param.currParamName.equalsIgnoreCase(value)) {
                param.setValue(true);
                input.append(value + "= 1\n");
            } else {
                input.append(value + "= 0\n");
            }
        }
        this.currParamValue = input.toString();
        super.setChanged(true);
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

    public boolean isExistParam(String name) {
        BaseParam param = getParam(name);
        if (param != null) {
            return true;
        }
        return false;
    }

    public BaseParam getParam(String name) {
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            if (param.isParam(name)) {
                return param;
            }
        }
        return null;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("group_name")) {
            return new TemplateString(this.currParamName);
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        RadioGroup group = new RadioGroup(this.currParamName);
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            group.addParam(param.copy());
        }
        return group;
    }
}
