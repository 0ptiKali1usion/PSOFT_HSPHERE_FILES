package psoft.validators;

/* loaded from: hsphere.zip:psoft/validators/NameModifier.class */
public class NameModifier {
    public static NameModifier none = new NameModifier("", "", null);

    /* renamed from: nm */
    protected NameModifier f280nm;
    String prefix;
    String suffix;

    public static NameModifier blank() {
        return new NameModifier("", "", null);
    }

    public String getName(Object name) {
        return this.f280nm != null ? this.f280nm.getName(this.prefix + name + this.suffix) : this.prefix + name + this.suffix;
    }

    public NameModifier(String prefix) {
        this(prefix, "", null);
    }

    public NameModifier(String prefix, String suffix, NameModifier nm) {
        this.prefix = "";
        this.suffix = "";
        setPrefix(prefix);
        setSuffix(suffix);
        this.f280nm = nm;
    }

    public NameModifier setNameModifier(NameModifier nm) {
        this.f280nm = nm;
        return this;
    }

    public void setPrefix(String prefix) {
        if (prefix != null) {
            this.prefix = prefix;
        }
    }

    public void setSuffix(String suffix) {
        if (suffix != null) {
            this.suffix = suffix;
        }
    }
}
