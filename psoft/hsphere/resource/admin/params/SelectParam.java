package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/SelectParam.class */
public class SelectParam extends BaseParamsList {
    public SelectParam(String name, String help) {
        super(new BaseParamImpl(name, null, null, "select", null, help, null), "");
    }

    public SelectParam(String name, String help, String[] customParams) {
        super(new BaseParamImpl(name, null, null, "select", null, help, customParams), "");
    }

    public void setValue(String name, String value) {
        StringBuffer input = new StringBuffer();
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            CheckParam param = (CheckParam) i.next();
            if (param.currParamName.equalsIgnoreCase(value)) {
                param.setValue(true);
                input.append(name + "=" + value + "\n");
            } else {
                param.setValue(false);
            }
        }
        this.currParamValue = value;
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
    public void init(String value) {
        if (value != null && !value.startsWith(this.currParamName)) {
            value = this.currParamName + "_" + value;
        }
        this.currParamValue = value;
        this.changed = false;
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
        SelectParam group = new SelectParam(this.currParamName, this.help);
        Iterator i = this.params.iterator();
        while (i.hasNext()) {
            BaseParam param = (BaseParam) i.next();
            group.addParam(param.copy());
        }
        return group;
    }
}
