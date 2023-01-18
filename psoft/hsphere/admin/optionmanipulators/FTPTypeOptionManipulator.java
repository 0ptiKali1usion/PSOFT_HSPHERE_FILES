package psoft.hsphere.admin.optionmanipulators;

import psoft.hsphere.Session;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/FTPTypeOptionManipulator.class */
public class FTPTypeOptionManipulator extends DefaultOptionManipulator {
    public FTPTypeOptionManipulator(String optionName) {
        super(optionName);
    }

    public FTPTypeOptionManipulator(String optionName, Boolean useXML) {
        super(optionName, useXML);
    }

    public FTPTypeOptionManipulator(String optionName, boolean useXML) {
        super(optionName, useXML);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public void setOption(long lserverId, Object optionValue) {
        super.setOption(lserverId, optionValue);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public Object getOption(long lserverId) {
        Object result = null;
        if (!this.useXML) {
            try {
                WinHostEntry whe = (WinHostEntry) HostManager.getHost(lserverId);
                result = whe.getFTPType();
            } catch (Exception e) {
                Session.getLog().error("Error while getting FTP type for server " + lserverId, e);
            }
        }
        return result;
    }
}
