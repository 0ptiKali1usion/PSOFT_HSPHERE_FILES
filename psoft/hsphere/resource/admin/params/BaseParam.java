package psoft.hsphere.resource.admin.params;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.w3c.dom.Node;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Localizer;
import psoft.util.XMLEncoder;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/BaseParam.class */
public abstract class BaseParam implements TemplateHashModel {
    protected String currParamName;
    protected String currParamValue;
    protected String defaultParamValue;
    protected String type;
    protected String description;
    protected String help;
    protected boolean changed;
    protected String[] customParams;
    protected String[] typeParams;

    public BaseParam() {
        this.customParams = null;
        this.typeParams = null;
    }

    public BaseParam(BaseParam param) {
        this(param.currParamName, param.currParamValue, param.defaultParamValue, param.type, param.description, param.help, param.customParams);
    }

    public BaseParam(String paramName, String paramValue, String defaultValue, String type, String descr, String hlp, String[] customPars) {
        this.customParams = null;
        this.typeParams = null;
        this.currParamName = paramName;
        if (paramValue != null) {
            this.currParamValue = paramValue.trim();
        } else {
            this.currParamValue = paramValue;
        }
        this.defaultParamValue = defaultValue;
        this.type = type;
        this.description = descr;
        this.help = hlp;
        this.customParams = customPars;
        this.changed = false;
        if (paramValue == null && defaultValue != null) {
            this.currParamValue = defaultValue.trim();
        }
    }

    public void setValueFlags() {
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("param_name")) {
            return new TemplateString(this.currParamName);
        }
        if (key.equals("param_value")) {
            return new TemplateString(XMLEncoder.encode(this.currParamValue));
        }
        if (key.equals("param_type")) {
            return new TemplateString(this.type);
        }
        if (key.equals("param_description")) {
            return new TemplateString(Localizer.translateMessage(this.description, null));
        }
        if (key.equals("param_defaultvalue")) {
            return new TemplateString(this.defaultParamValue);
        }
        if (key.equals("param_help")) {
            return new TemplateString(this.help);
        }
        if (key.equals("param_template")) {
            return new TemplateString(ParamsTypesMapper.getTemplateForType(this.type));
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return this.currParamName == null;
    }

    public boolean isParam(String paramName) {
        return paramName.equals(this.currParamName);
    }

    public void setValue(String value) {
        if (value != null) {
            String value2 = value.trim();
            if (this.currParamValue == null || !this.currParamValue.trim().equals(value2)) {
                setChanged(true);
            } else {
                setChanged(false);
            }
            this.currParamValue = value2;
        }
        setValueFlags();
    }

    public String getValue() {
        return this.currParamValue;
    }

    public static String remove(String str, String pattern) {
        int s = 0;
        StringBuffer result = new StringBuffer();
        while (true) {
            int e = str.indexOf(pattern, s);
            if (e >= 0) {
                result.append(str.substring(s, e));
                s = e + pattern.length();
            } else {
                result.append(str.substring(s));
                return result.toString();
            }
        }
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void init(String value) {
        if (value != null) {
            this.currParamValue = value.trim();
        } else {
            this.currParamValue = value;
        }
        this.changed = false;
    }

    public String getCurrParamName() {
        return this.currParamName;
    }

    public String getCurrParamValue() {
        return this.currParamValue;
    }

    public String getDefaultValue() {
        return this.defaultParamValue;
    }

    public boolean isChanged() {
        return this.changed;
    }

    public String getType() {
        return this.type;
    }

    public String[] getCustomParams() {
        return this.customParams;
    }

    public void setCustomParams(String[] params) {
        this.customParams = params;
    }

    public void addCustomParamsEntry(String param) {
        String[] tmp = this.customParams;
        this.customParams = new String[tmp.length + 1];
        for (int i = 0; i < tmp.length; i++) {
            this.customParams[i] = tmp[i];
        }
        this.customParams[this.customParams.length] = param;
    }

    public String[] getParamsForType() {
        return this.typeParams;
    }

    public void setParamsForType(String[] params) {
        this.typeParams = params;
    }

    public void setParamsForType(Node param) {
    }

    public BaseParam copy() {
        return null;
    }

    public TemplateModel FM_ischanged(String value) throws Exception {
        if (value != null) {
            value = value.trim();
        }
        if (this.currParamValue.equalsIgnoreCase(value)) {
            return new SimpleScalar(false);
        }
        return new SimpleScalar(true);
    }
}
