package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.params.BaseParam;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.admin.params.SelectParam;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.Salt;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailSRS.class */
public class MailSRS implements TemplateHashModel {
    protected String srsKey;
    protected int srsAge;
    public static final int SRS_AGE_DEFAULT = 30;

    public MailSRS() throws Exception {
        this.srsKey = Settings.get().getValue("SRS_KEY");
        if (this.srsKey == null || this.srsKey.equals("")) {
            Salt salt = new Salt();
            this.srsKey = salt.getNext(20);
        }
        String strSrsAge = Settings.get().getValue("SRS_AGE");
        if (strSrsAge != null && !strSrsAge.equalsIgnoreCase("")) {
            this.srsAge = new Integer(strSrsAge).intValue();
        } else {
            this.srsAge = 30;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("enabled".equals(key)) {
            try {
                if (enabled()) {
                    return TemplateString.TRUE;
                }
                return null;
            } catch (Exception e) {
                Ticket.create(e, this, "Can't check if SRS enabled");
                return null;
            }
        } else if ("srs_status".equals(key)) {
            try {
                return new TemplateString(getSRSStatus());
            } catch (Exception e2) {
                Ticket.create(e2, this, "Can't get SRS status string");
                return new TemplateString("");
            }
        } else if ("srs_age".equals(key)) {
            return new TemplateString(new Integer(this.srsAge).toString());
        } else {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel FM_add() throws Exception {
        physicalCreate();
        return new TemplateOKResult();
    }

    public TemplateModel FM_regenerateSRSKey() throws Exception {
        regenerateSRSKey();
        return new TemplateOKResult();
    }

    public void regenerateSRSKey() throws Exception {
        Salt salt = new Salt();
        String rnd = salt.getNext(20);
        List l = new ArrayList();
        l.add("-m " + this.srsAge);
        l.add("-s " + rnd);
        List li = MailSSL.getMailServers();
        for (int i = 0; i < li.size(); i++) {
            HostEntry he = HostManager.getHost(((HostEntry) li.get(i)).getId());
            he.exec("setsrssecret", l);
        }
        Settings.get().setValue("SRS_KEY", rnd);
        this.srsKey = rnd;
    }

    public TemplateModel FM_delete() throws Exception {
        physicalDelete();
        return new TemplateOKResult();
    }

    public TemplateModel FM_updateAge(int age) throws Exception {
        updateAge(age);
        return new TemplateOKResult();
    }

    public void updateAge(int age) throws Exception {
        List li = MailSSL.getMailServers();
        List l = new ArrayList();
        l.add("-m " + age);
        l.add("-s " + this.srsKey);
        for (int i = 0; i < li.size(); i++) {
            HostEntry he = HostManager.getHost(((HostEntry) li.get(i)).getId());
            he.exec("setsrssecret", l);
        }
        Settings.get().setValue("SRS_AGE", new Integer(age).toString());
        this.srsAge = age;
    }

    public void physicalCreate() throws Exception {
        List li = MailSSL.getMailServers();
        for (int i = 0; i < li.size(); i++) {
            HostEntry he = (HostEntry) li.get(i);
            EnterpriseManager eeman = (EnterpriseManager) Session.getAccount().FM_findChild("eeman").get();
            Params params = eeman.readConfigParameters(he.getPServer().getId());
            BaseParam bpSRS = params.getParam("srs");
            bpSRS.setValue("1");
            SelectParam bpSPF = (SelectParam) params.getParam("spfbehavior");
            if (bpSPF.getCurrParamValue() == null) {
                bpSPF.setValue("spfbehavior", "spfbehavior_1");
            }
            eeman.FM_updateMailServerParams(new Long(he.getId()).toString());
            bpSRS.setChanged(false);
        }
        List l = new ArrayList();
        l.add("-m " + this.srsAge);
        l.add("-s " + this.srsKey);
        for (int i2 = 0; i2 < li.size(); i2++) {
            HostManager.getHost(((HostEntry) li.get(i2)).getId()).exec("setsrssecret", l);
        }
    }

    public void physicalDelete() throws Exception {
        List li = MailSSL.getMailServers();
        for (int i = 0; i < li.size(); i++) {
            HostEntry he = (HostEntry) li.get(i);
            EnterpriseManager eeman = (EnterpriseManager) Session.getAccount().FM_findChild("eeman").get();
            Params params = eeman.readConfigParameters(he.getPServer().getId());
            BaseParam bp = params.getParam("srs");
            bp.setValue(null);
            eeman.FM_updateMailServerParams(new Long(he.getId()).toString());
            bp.setChanged(false);
        }
    }

    public int enabledAmount() throws Exception {
        int amount = 0;
        List li = MailSSL.getMailServers();
        for (int i = 0; i < li.size(); i++) {
            EnterpriseManager eeman = (EnterpriseManager) Session.getAccount().FM_findChild("eeman").get();
            Params params = eeman.readConfigParameters(((HostEntry) li.get(i)).getPServer().getId());
            BaseParam bp = params.getParam("srs");
            String value = bp.getValue();
            if (value.equalsIgnoreCase("1")) {
                amount++;
            }
        }
        return amount;
    }

    public boolean enabled() throws Exception {
        if (!this.srsKey.equals("")) {
            int a = enabledAmount();
            if (a > 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    public String getSRSStatus() throws Exception {
        if (!this.srsKey.equals("")) {
            int enabledAmount = enabledAmount();
            if (enabledAmount > 0) {
                int mserversAmount = MailSSL.getMailServers().size();
                if (enabledAmount < mserversAmount) {
                    return Localizer.translateMessage("mailsrs.enabled_partly", new Object[]{String.valueOf(enabledAmount), String.valueOf(mserversAmount)});
                }
                return "";
            }
            return Localizer.translateMessage("mailsrs.disabled");
        }
        return "";
    }
}
