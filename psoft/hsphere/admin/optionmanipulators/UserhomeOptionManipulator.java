package psoft.hsphere.admin.optionmanipulators;

import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/UserhomeOptionManipulator.class */
public class UserhomeOptionManipulator extends DefaultOptionManipulator {
    public UserhomeOptionManipulator(String optionName) {
        super(optionName);
    }

    public UserhomeOptionManipulator(String optionName, Boolean useXML) {
        super(optionName, useXML);
    }

    public UserhomeOptionManipulator(String optionName, boolean useXML) {
        super(optionName, useXML);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public void setOption(long lserverId, Object optionValue) {
        super.setOption(lserverId, optionValue);
    }

    @Override // psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator, psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public Object getOption(long lserverId) {
        String homePrefix = (String) super.getOption(lserverId);
        if (homePrefix == null || "".equals(homePrefix)) {
            return HostEntry.UnixUserDefaultHomeDir;
        }
        return homePrefix;
    }
}
