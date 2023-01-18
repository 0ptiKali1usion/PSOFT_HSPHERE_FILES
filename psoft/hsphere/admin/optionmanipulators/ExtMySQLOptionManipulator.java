package psoft.hsphere.admin.optionmanipulators;

import psoft.hsphere.Session;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/ExtMySQLOptionManipulator.class */
public class ExtMySQLOptionManipulator extends DefaultOptionManipulator {
    public ExtMySQLOptionManipulator(String optionName) {
        super(optionName);
    }

    public ExtMySQLOptionManipulator(String optionName, Boolean useXML) {
        super(optionName, useXML);
    }

    public ExtMySQLOptionManipulator(String optionName, boolean useXML) {
        super(optionName, useXML);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public void setOption(long lserverId, Object optionValue) {
        super.setOption(lserverId, optionValue);
        String optValueStr = (String) optionValue;
        if ("".equals(optValueStr)) {
            return;
        }
        long mysqlServerId = Long.parseLong(optValueStr);
        DefaultOptionManipulator extHordePassManipulator = new DefaultOptionManipulator("ext_horde_pass", this.useXML);
        if (extHordePassManipulator.getOption(mysqlServerId) == null) {
            try {
                String password = new Salt().getNext(15);
                extHordePassManipulator.setOption(mysqlServerId, password);
            } catch (Exception e) {
                Session.getLog().error("Failed to set option ext_horde_pass for id " + lserverId, e);
            }
        }
        DefaultOptionManipulator extSAPassManipulator = new DefaultOptionManipulator("ext_sa_pass", this.useXML);
        if (extSAPassManipulator.getOption(mysqlServerId) == null) {
            try {
                String password2 = new Salt().getNext(15);
                extSAPassManipulator.setOption(mysqlServerId, password2);
            } catch (Exception e2) {
                Session.getLog().error("Failed to set option ext_sa_pass for id " + lserverId, e2);
            }
        }
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public Object getOption(long lserverId) {
        return super.getOption(lserverId);
    }
}
