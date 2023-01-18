package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/BaseParamsListImpl.class */
public class BaseParamsListImpl extends BaseParamsList {
    public BaseParamsListImpl(String paramName, String paramValue, String defaultValue, String type, String description, String help, String[] customParams) {
        super(paramName, paramValue, defaultValue, type, description, help, customParams);
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        return null;
    }
}
