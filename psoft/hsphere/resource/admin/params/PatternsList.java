package psoft.hsphere.resource.admin.params;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import org.w3c.dom.Node;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/PatternsList.class */
public class PatternsList extends ListParam {
    private String pattern;

    public PatternsList(BaseParam param) {
        super(param, " ");
        this.pattern = null;
    }

    public PatternsList(BaseParam param, String delimiter) {
        super(param, delimiter);
        this.pattern = null;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setParamsForType(Node param) {
        Node patn = param.getAttributes().getNamedItem("pattern");
        if (patn != null) {
            setPattern(patn.getNodeValue());
        }
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam
    protected boolean isValid(String value) {
        try {
            if (this.pattern == null) {
                new RE(value.trim());
                return true;
            }
            try {
                RE regular = new RE(this.pattern);
                REMatch mt = regular.getMatch(value);
                if (mt != null) {
                    return true;
                }
                Session.addMessage("Incorrect value - " + value + " in patterns list - " + this.currParamName);
                return false;
            } catch (Exception e) {
                Session.addMessage("Incorrect pattern for check value in patterns list - " + this.currParamName);
                Session.getLog().info(e);
                return false;
            }
        } catch (Exception e2) {
            Session.addMessage("Incorrect value - " + value + " in patterns list - " + this.currParamName);
            Session.getLog().info(e2);
            return false;
        }
    }

    public String getPattern() {
        return this.pattern;
    }

    protected void setPattern(String patn) {
        this.pattern = patn;
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam, psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        PatternsList list = new PatternsList(this, this.delim);
        list.setPattern(getPattern());
        return list;
    }
}
