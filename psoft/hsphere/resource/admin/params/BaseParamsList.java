package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.ListIterator;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/BaseParamsList.class */
public abstract class BaseParamsList extends BaseParam implements TemplateHashModel, TemplateMethodModel {
    protected ArrayList params;
    protected String delim;
    protected char delimChar;

    public BaseParamsList(BaseParam param) {
        super(param);
        this.delim = null;
        this.params = new ArrayList();
    }

    public BaseParamsList(BaseParam param, String delimiter) {
        super(param);
        this.delim = null;
        this.params = new ArrayList();
        if (!delimiter.equals("")) {
            this.delim = delimiter;
            this.delimChar = delimiter.charAt(0);
        }
    }

    public BaseParamsList(BaseParamsList param) {
        super(param);
        this.delim = null;
        ListIterator i = param.params.listIterator();
        while (i.hasNext()) {
            BaseParam par = (BaseParam) i.next();
            this.params.add(par.copy());
        }
    }

    public BaseParamsList(String paramName, String paramValue, String defaultValue, String type, String descr, String help, String[] customParams) {
        super(paramName, paramValue, defaultValue, type, descr, help, customParams);
        this.delim = null;
    }

    protected void parseParamsList() {
        this.params.clear();
    }

    public TemplateModel FM_getParamsList() {
        return new TemplateList(this.params);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("param_list")) {
            return new TemplateList(this.params);
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        super.setValue(value);
        if (this.params != null) {
            this.params.clear();
        }
        parseParamsList();
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void init(String value) {
        this.currParamValue = value;
        this.changed = false;
        parseParamsList();
    }

    public void addParam(BaseParam param) {
        this.params.add(param);
    }

    public String getDelimiter() {
        return this.delim;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return null;
    }

    public ArrayList getParamsList() {
        return this.params;
    }
}
