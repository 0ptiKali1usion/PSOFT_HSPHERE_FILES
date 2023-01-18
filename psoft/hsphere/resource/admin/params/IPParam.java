package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/IPParam.class */
public class IPParam extends EditParam implements TemplateHashModel {
    public IPParam(BaseParam param) {
        super(param);
    }

    @Override // psoft.hsphere.resource.admin.params.EditParam, psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.admin.params.EditParam, psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        super.setValue(value);
    }

    @Override // psoft.hsphere.resource.admin.params.EditParam, psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    @Override // psoft.hsphere.resource.admin.params.EditParam, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new IPParam(this);
    }

    public TemplateModel FM_checkNewValue(long mserverId, String value) throws Exception {
        if (this.currParamName.equals("outgoingip")) {
            HostEntry he = HostManager.getHost(mserverId);
            List l = new ArrayList();
            l.add(value);
            Iterator output = he.exec("qmail-check-outgoingip", l).iterator();
            String result = null;
            if (output.hasNext()) {
                result = output.next().toString();
            }
            if (result == null || !"0".equals(result)) {
                return new TemplateString("1");
            }
        }
        return new TemplateString("0");
    }
}
