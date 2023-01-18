package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.admin.Settings;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SiteToolboxManager.class */
public class SiteToolboxManager extends Resource {
    private String referralID;
    private String toolboxURL;

    public SiteToolboxManager(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.referralID = (String) i.next();
        this.toolboxURL = (String) i.next();
        Settings.get().setValue("toolbox_referralid", this.referralID);
        Settings.get().setValue("toolbox_url", this.toolboxURL);
    }

    public SiteToolboxManager(ResourceId rId) throws Exception {
        super(rId);
        this.referralID = Settings.get().getValue("toolbox_referralid");
        this.toolboxURL = Settings.get().getValue("toolbox_url");
    }

    public TemplateModel FM_editProp(String referralId, String toolboxUrl) throws Exception {
        Settings.get().setValue("toolbox_referralid", referralId);
        Settings.get().setValue("toolbox_url", toolboxUrl);
        this.referralID = referralId;
        this.toolboxURL = toolboxUrl;
        return this;
    }

    public TemplateModel FM_getRefId() throws Exception {
        return new TemplateString(this.referralID);
    }

    public TemplateModel FM_getUrl() throws Exception {
        return new TemplateString(this.toolboxURL);
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.sitestudio.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sitestudio.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sitestudio.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.sitestudio.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        Settings.get().setValue("userstoolbox", "off");
        Settings.get().setValue("toolbox_referralid", "");
    }
}
