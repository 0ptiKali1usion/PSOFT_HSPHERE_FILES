package psoft.hsphere.admin.optionmanipulators;

import psoft.hsphere.Session;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/PHPMyAdminOptionManipulator.class */
public class PHPMyAdminOptionManipulator extends DefaultOptionManipulator {
    public PHPMyAdminOptionManipulator(String optionName) {
        super(optionName);
    }

    public PHPMyAdminOptionManipulator(String optionName, Boolean useXML) {
        super(optionName, useXML);
    }

    public PHPMyAdminOptionManipulator(String optionName, boolean useXML) {
        super(optionName, useXML);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public void setOption(long lserverId, Object optionValue) {
        super.setOption(lserverId, optionValue);
        String optValueStr = (String) optionValue;
        if ("".equals(optValueStr)) {
            return;
        }
        DefaultOptionManipulator phpUserPassManipulator = new DefaultOptionManipulator("phpuserpass", this.useXML);
        long webServerId = Long.parseLong(optValueStr);
        if (phpUserPassManipulator.getOption(webServerId) == null) {
            try {
                String password = new Salt().getNext(15);
                phpUserPassManipulator.setOption(webServerId, password);
            } catch (Exception e) {
                Session.getLog().error("Failed to set option phpuserpass for id " + lserverId, e);
            }
        }
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public Object getOption(long lserverId) {
        return super.getOption(lserverId);
    }
}
