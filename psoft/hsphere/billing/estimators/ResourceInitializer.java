package psoft.hsphere.billing.estimators;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/ResourceInitializer.class */
public class ResourceInitializer implements Serializable {
    protected ResourceId parent;
    protected String type;
    protected String mode;
    protected Collection initValues;

    public ResourceInitializer(String _type, String _mode) {
        this(_type, _mode, null);
    }

    public ResourceInitializer(String _type, String _mode, Collection _initValues) {
        this(null, _type, _mode, _initValues);
    }

    public ResourceInitializer(ResourceId _parent, String _type, String _mode, Collection _initValues) {
        this.parent = _parent;
        this.type = _type;
        this.mode = _mode;
        this.initValues = new LinkedList(_initValues);
    }

    public Collection getInitValues() {
        return this.initValues;
    }

    public String getType() {
        return this.type;
    }

    public String getMode() {
        return this.mode;
    }

    public int hashCode() {
        int result = this.type != null ? this.type.hashCode() : 0;
        return (29 * ((29 * result) + (this.mode != null ? this.mode.hashCode() : 0))) + (this.initValues != null ? this.initValues.hashCode() : 0);
    }

    public ResourceId getParent() {
        return this.parent;
    }

    public String toString() {
        return "parent=" + getParent().toString() + " type=" + getType() + " mode=" + getMode() + " ivalues=" + getInitValues().toString();
    }
}
