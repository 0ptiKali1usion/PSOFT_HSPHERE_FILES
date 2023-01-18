package psoft.hsphere.admin.optionmanipulators;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/DefaultOptionManipulator.class */
public class DefaultOptionManipulator implements OptionManipulator {
    protected String optionName;
    protected boolean useXML;

    public DefaultOptionManipulator(String optionName) {
        this(optionName, false);
    }

    public DefaultOptionManipulator(String optionName, Boolean useXML) {
        this(optionName, useXML.booleanValue());
    }

    public DefaultOptionManipulator(String optionName, boolean useXML) {
        this.optionName = optionName;
        this.useXML = useXML;
    }

    public String getOptionName() {
        return this.optionName;
    }

    @Override // psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public TemplateModel FM_getOption(long lserverId) {
        return new TemplateString(getOption(lserverId));
    }

    @Override // psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public TemplateModel FM_setOption(long lserverId, String optionValue) {
        setOption(lserverId, optionValue);
        return new TemplateOKResult();
    }

    @Override // psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public TemplateModel FM_useXML() {
        return new TemplateString(isUsingXML());
    }

    @Override // psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public Object getOption(long lserverId) {
        Object result;
        if (isUsingXML()) {
            result = getOptionFromXML(lserverId);
        } else {
            result = getOptionFromDB(lserverId);
        }
        return result;
    }

    private boolean isUsingXML() {
        return this.useXML;
    }

    private Object getOptionFromDB(long lserverId) {
        try {
            HostEntry entry = HostManager.getHost(lserverId);
            return entry.getOption(getOptionName());
        } catch (Exception e) {
            Session.getLog().error("Unable to get LogcalServer ID " + lserverId, e);
            return null;
        }
    }

    private Object getOptionFromXML(long lserverId) {
        return null;
    }

    @Override // psoft.hsphere.admin.optionmanipulators.OptionManipulator
    public void setOption(long lserverId, Object optionValue) {
        if (isUsingXML()) {
            setOptionInXML(lserverId, optionValue);
        } else {
            setOptionInDB(lserverId, optionValue);
        }
    }

    private void setOptionInDB(long lserverId, Object optionValue) {
        try {
            HostEntry entry = HostManager.getHost(lserverId);
            if (entry != null) {
                entry.setOption(getOptionName(), (String) optionValue);
            }
        } catch (Exception e) {
            Session.getLog().error("Unable to get LogcalServer ID " + lserverId, e);
        }
    }

    private void setOptionInXML(long lserverId, Object optionValue) {
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
