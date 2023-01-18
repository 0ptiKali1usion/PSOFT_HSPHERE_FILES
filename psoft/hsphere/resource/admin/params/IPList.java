package psoft.hsphere.resource.admin.params;

import java.util.StringTokenizer;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/IPList.class */
public class IPList extends ListParam {
    public IPList(BaseParam param) {
        super(param, ":");
    }

    public IPList(BaseParam param, String delimiter) {
        super(param, delimiter);
    }

    private void addErrorMessage(String value) {
        Session.addMessage("Incorrect value - " + value + " in IPs list - " + this.currParamName);
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam
    protected boolean isValid(String value) {
        try {
            StringTokenizer groups = new StringTokenizer(value, ".");
            int count = 0;
            while (groups.hasMoreTokens()) {
                String str = groups.nextToken().trim();
                int val = 0;
                for (int j = 0; j < str.length(); j++) {
                    if (j == 0 && str.length() > 1 && str.charAt(j) == '0') {
                        addErrorMessage(value);
                        return false;
                    } else if (str.charAt(j) >= '0' && str.charAt(j) <= '9') {
                        val = (val * 10) + Integer.parseInt(str.substring(j, j + 1));
                    } else {
                        addErrorMessage(value);
                        return false;
                    }
                }
                if (val < 0 || val > 255) {
                    addErrorMessage(value);
                    return false;
                }
                count++;
            }
            if (count == 4) {
                return true;
            }
            addErrorMessage(value);
            return false;
        } catch (Exception e) {
            Session.getLog().info(e);
            return false;
        }
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam, psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new IPList(this);
    }
}
