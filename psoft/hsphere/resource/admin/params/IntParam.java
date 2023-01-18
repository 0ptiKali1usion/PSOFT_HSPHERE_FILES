package psoft.hsphere.resource.admin.params;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/IntParam.class */
public class IntParam extends BaseParam {
    private Integer min;
    private Integer max;

    public IntParam(BaseParam param) {
        super(param);
        this.min = null;
        this.max = null;
    }

    public Integer getMin() {
        return this.min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return this.max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setParamsForType(Node param) {
        Element current = (Element) param;
        NamedNodeMap attrs = current.getAttributes();
        Node minimum = attrs.getNamedItem("min");
        if (minimum != null) {
            try {
                this.min = new Integer(minimum.getNodeValue());
            } catch (Exception e) {
                Session.getLog().info(e);
            }
        }
        Node maximum = attrs.getNamedItem("max");
        if (maximum != null) {
            try {
                this.max = new Integer(maximum.getNodeValue());
            } catch (Exception e2) {
                Session.getLog().info(e2);
            }
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        if (value != null) {
            try {
                value = value.trim();
                int val = Integer.parseInt(value);
                if (!this.currParamValue.equals(value)) {
                    if (this.min != null && this.min.intValue() > val) {
                        Session.addMessage("Value - " + value + " less min=" + this.min);
                    }
                    if (this.max != null && this.max.intValue() < val) {
                        Session.addMessage("Value - " + value + " greate max=" + this.max);
                    }
                    super.setValue(value);
                }
            } catch (Exception e) {
                Session.getLog().info(e);
                Session.addMessage("Value - " + value + " is not integer");
            }
        }
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void init(String value) {
        if (value != null) {
            this.currParamValue = value.trim();
        } else {
            this.currParamValue = value;
        }
        this.changed = false;
        setValueFlags();
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        IntParam param = new IntParam(this);
        param.setMin(getMin());
        param.setMax(getMax());
        return param;
    }
}
