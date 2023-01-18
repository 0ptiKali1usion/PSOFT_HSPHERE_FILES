package psoft.hsphere.plan;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/plan/InitResource.class */
public class InitResource implements TemplateHashModel {

    /* renamed from: id */
    protected int f107id;
    protected String initMod;
    protected boolean disabled;
    public static final String PARENT_MOD = "_PARENT_";
    protected static final TemplateString TS_1 = new TemplateString("1");
    protected static final TemplateString TS_0 = new TemplateString("0");

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("id")) {
            return new TemplateString(this.f107id);
        }
        try {
            if (key.equals("type")) {
                return new TemplateString(TypeRegistry.getType(this.f107id));
            }
        } catch (Exception e) {
            Session.getLog().warn("no such type ", e);
        }
        if (key.equals("mod")) {
            return new TemplateString(this.initMod);
        }
        if (key.equals("disabled")) {
            return this.disabled ? TS_1 : TS_0;
        }
        return null;
    }

    public void setDisabled(int value) {
        this.disabled = value == 1;
    }

    public InitResource(String id, String initMod, int disabled) {
        this(Integer.parseInt(id), initMod, disabled);
    }

    public InitResource(int id, String initMod, int disabled) {
        this.f107id = id;
        this.initMod = initMod;
        this.disabled = disabled == 1;
    }

    public String getMod() {
        return this.initMod;
    }

    public String getMod(String parentMod) {
        return PARENT_MOD.equals(this.initMod) ? parentMod : this.initMod;
    }

    public int getType() {
        return this.f107id;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void enable() {
        this.disabled = false;
    }

    public void disable() {
        this.disabled = true;
    }
}
