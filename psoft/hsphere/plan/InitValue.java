package psoft.hsphere.plan;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/plan/InitValue.class */
public class InitValue implements TemplateHashModel, TemplateScalarModel {
    public static final int STATIC = 0;
    public static final int FIELD = 1;
    public static final int RELATIVE = 2;
    public static final int ABSOLUTE = 3;
    public static final int RELATIVE_REC = 4;
    public static final int ABSOLUTE_REC = 5;
    public static final int HOST_GROUP = 6;
    public static final int PLAN_FREE = 7;
    public static final int PLAN_VALUE = 8;
    protected String label;
    protected String value;
    protected int type;

    public boolean isEmpty() {
        return false;
    }

    public String getAsString() {
        return this.value;
    }

    public TemplateModel get(String key) {
        if (key.equals("label")) {
            return new TemplateString(this.label);
        }
        if (key.equals("type")) {
            return new TemplateString(this.type);
        }
        if (key.equals("value")) {
            return new TemplateString(this.value);
        }
        return null;
    }

    public boolean equals(Object o) {
        return this.value.equals(o.toString());
    }

    public int hashCode() {
        return this.value.hashCode();
    }

    public String toString() {
        return this.value;
    }

    public InitValue(String value) {
        this(value, 0);
    }

    public InitValue(String value, int type) {
        this(value, type, null);
    }

    public InitValue(String value, int type, String label) {
        this.value = value;
        this.type = type;
        this.label = label;
    }

    public String getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }

    public int getType() {
        return this.type;
    }

    public static int getType(String name) {
        if ("static".equals(name)) {
            return 0;
        }
        if ("field".equals(name)) {
            return 1;
        }
        if ("relative".equals(name)) {
            return 2;
        }
        if ("absolute".equals(name)) {
            return 3;
        }
        if ("relative_rec".equals(name)) {
            return 4;
        }
        if ("absolute_rec".equals(name)) {
            return 5;
        }
        if ("hostgroup".equals(name)) {
            return 6;
        }
        if ("plan_free".equals(name)) {
            return 7;
        }
        return "plan_value".equals(name) ? 8 : 0;
    }
}
