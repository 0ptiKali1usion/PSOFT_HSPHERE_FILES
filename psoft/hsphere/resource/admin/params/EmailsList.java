package psoft.hsphere.resource.admin.params;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/EmailsList.class */
public class EmailsList extends ListParam {
    public EmailsList(BaseParam param) {
        super(param, " ");
    }

    public EmailsList(BaseParam param, String delimiter) {
        super(param, delimiter);
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam
    protected boolean isValid(String value) {
        try {
            String value2 = value.trim();
            RE regular = new RE("[\\w.\\-]+@[a-zA-Z0-9\\-]+\\.[a-zA-Z0-9\\-](\\.?[a-zA-Z0-9\\-]+)*");
            REMatch mt = regular.getMatch(value2);
            if (mt != null) {
                return true;
            }
            Session.addMessage("Incorrect value - " + value2 + " in emails list - " + this.currParamName);
            return false;
        } catch (Exception e) {
            Session.getLog().info(e);
            return false;
        }
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam, psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new EmailsList(this, this.delim);
    }
}
