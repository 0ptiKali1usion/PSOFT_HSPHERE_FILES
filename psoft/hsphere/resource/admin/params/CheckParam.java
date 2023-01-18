package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/CheckParam.class */
public class CheckParam extends BaseParam implements TemplateHashModel {
    public String checked;
    private boolean selected;
    private int checkType;
    public static int ON_OFF = 0;
    public static int YES_NO = 1;
    public static int ENABLED_DISABLED = 2;
    public static int BOOLEAN_DIGIT = 3;
    public static int TEXT_VALUE = 4;

    public CheckParam(BaseParam param) {
        super(param);
        this.checkType = -1;
        if (this.currParamValue != null) {
            setCheckType(this.currParamValue);
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValueFlags() {
        if (this.checkType == ON_OFF || this.checkType == YES_NO || this.checkType == ENABLED_DISABLED || this.checkType == BOOLEAN_DIGIT) {
            if (this.currParamValue.trim().equalsIgnoreCase("On") || this.currParamValue.trim().equalsIgnoreCase("Yes") || this.currParamValue.trim().equalsIgnoreCase("Enabled") || this.currParamValue.trim().equalsIgnoreCase("1")) {
                this.checked = "CHECKED";
                return;
            } else {
                this.checked = "";
                return;
            }
        }
        this.selected = true;
        this.checked = "CHECKED";
    }

    public void setChecked(String onoff) {
        if (onoff == null) {
            this.checked = "";
            this.selected = false;
        } else if (onoff.equalsIgnoreCase("On")) {
            this.checked = "CHECKED";
            this.selected = true;
        } else if (onoff.equalsIgnoreCase("Off")) {
            this.checked = "";
            this.selected = false;
        } else {
            this.checked = "CHECKED";
            this.selected = true;
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("full_name")) {
            return new TemplateString(this.currParamName + "-" + this.currParamValue);
        }
        if (key.equals("checked")) {
            return new TemplateString(this.checked);
        }
        if (key.equals("onoff")) {
            if (this.checkType == ON_OFF) {
                return new TemplateString(true);
            }
            return new TemplateString(false);
        } else if (key.equals("yesno")) {
            if (this.checkType == YES_NO) {
                return new TemplateString(true);
            }
            return new TemplateString(false);
        } else if (key.equals("digit")) {
            if (this.checkType == BOOLEAN_DIGIT) {
                return new TemplateString(true);
            }
            return new TemplateString(false);
        } else if (key.equals("text")) {
            if (this.checkType == TEXT_VALUE) {
                return new TemplateString(this.selected);
            }
            return super.get(key);
        } else {
            return super.get(key);
        }
    }

    protected boolean isOnOff(String value) {
        if (value == null) {
            return false;
        }
        if (value.trim().equalsIgnoreCase("On") || value.trim().equalsIgnoreCase("Off")) {
            return true;
        }
        return false;
    }

    protected boolean isYesNo(String value) {
        if (value == null) {
            return false;
        }
        if (value.trim().equalsIgnoreCase("Yes") || value.trim().equalsIgnoreCase("No")) {
            return true;
        }
        return false;
    }

    protected boolean isEnabledDisabled(String value) {
        if (value == null) {
            return false;
        }
        if (value.trim().equalsIgnoreCase("Enabled") || value.trim().equalsIgnoreCase("Disabled")) {
            return true;
        }
        return false;
    }

    protected boolean isBooleanDigit(String value) {
        if (value == null) {
            return false;
        }
        if (value.trim().equalsIgnoreCase("1") || value.trim().equalsIgnoreCase("0")) {
            return true;
        }
        return false;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        if (this.checkType == -1) {
            if (value == null) {
                setCheckType(this.currParamValue);
            } else {
                setCheckType(value);
            }
        }
        if (value == null) {
            if (this.type.equalsIgnoreCase("check")) {
                if (this.checkType == ON_OFF) {
                    super.setValue("Off");
                } else if (this.checkType == YES_NO) {
                    super.setValue("No");
                } else if (this.checkType == ENABLED_DISABLED) {
                    super.setValue("Disabled");
                } else if (this.checkType == BOOLEAN_DIGIT) {
                    super.setValue("0");
                } else if (this.checkType == TEXT_VALUE) {
                    super.setValue(value);
                    setChecked("Off");
                }
            } else {
                this.currParamValue = "";
            }
        } else if (this.checkType == ON_OFF) {
            super.setValue("On");
        } else if (this.checkType == YES_NO) {
            super.setValue("Yes");
        } else if (this.checkType == ENABLED_DISABLED) {
            super.setValue("Enable");
        } else if (this.checkType == BOOLEAN_DIGIT) {
            super.setValue("1");
        } else {
            super.setValue(value);
            setChecked("On");
        }
        setValueFlags();
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    private void setCheckType(String value) {
        if (isOnOff(value)) {
            this.checkType = ON_OFF;
        } else if (isYesNo(value)) {
            this.checkType = YES_NO;
        } else if (isEnabledDisabled(value)) {
            this.checkType = ENABLED_DISABLED;
        } else if (isBooleanDigit(value)) {
            this.checkType = BOOLEAN_DIGIT;
        } else {
            this.checkType = TEXT_VALUE;
        }
        setValueFlags();
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void init(String value) {
        if (value != null) {
            setCheckType(value);
            this.currParamValue = value.trim();
            this.changed = false;
            setValueFlags();
            return;
        }
        setCheckType(this.currParamValue);
    }

    public void setValue(boolean cond) {
        if (this.checkType == ON_OFF) {
            if (cond) {
                super.setValue("On");
            } else {
                super.setValue("Off");
            }
        } else if (this.checkType == YES_NO) {
            if (cond) {
                super.setValue("Yes");
            } else {
                super.setValue("No");
            }
        } else if (this.checkType == ENABLED_DISABLED) {
            if (cond) {
                super.setValue("Enabled");
            } else {
                super.setValue("Disabled");
            }
        } else if (this.checkType == BOOLEAN_DIGIT) {
            if (cond) {
                super.setValue("1");
            } else {
                super.setValue("0");
            }
        }
        setChanged(true);
    }

    public void invertValue() {
        if (this.checkType == ON_OFF) {
            if (getValue().equalsIgnoreCase("On")) {
                super.setValue("Off");
            } else {
                super.setValue("On");
            }
        } else if (this.checkType == YES_NO) {
            if (getValue().equalsIgnoreCase("Yes")) {
                super.setValue("No");
            } else {
                super.setValue("Yes");
            }
        } else if (this.checkType == ENABLED_DISABLED) {
            if (getValue().equalsIgnoreCase("Enabled")) {
                super.setValue("Disabled");
            } else {
                super.setValue("Enabled");
            }
        } else if (this.checkType == BOOLEAN_DIGIT) {
            if (getValue().equalsIgnoreCase("1")) {
                super.setValue("0");
            } else {
                super.setValue("1");
            }
        }
        setChanged(true);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        CheckParam param = new CheckParam(this);
        if (this.checked != null) {
            if (this.checked.equalsIgnoreCase("CHECKED")) {
                param.setChecked("On");
            } else {
                param.setChecked("Off");
            }
        }
        param.checkType = this.checkType;
        param.selected = this.selected;
        return param;
    }
}
