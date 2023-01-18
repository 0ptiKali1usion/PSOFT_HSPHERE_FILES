package psoft.hsphere.resource.moderate_signup;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.util.Date;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.util.FakeRequest;
import psoft.util.freemarker.HtmlEncodedHashListScalar;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/moderate_signup/TempAccount.class */
public class TempAccount extends Resource {

    /* renamed from: bi */
    protected BillingInfoObject f200bi;

    /* renamed from: ci */
    protected ContactInfoObject f201ci;
    protected Plan plan;
    protected OtherParameters other;
    protected HttpServletRequest oldRequest;

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
    }

    public TempAccount(long uid, int planId, long bid, long cid) throws Exception {
        this.f200bi = new BillingInfoObject(bid);
        this.f201ci = new ContactInfoObject(cid);
        this.plan = Plan.getPlan(planId);
        this.other = new OtherParameters(uid, bid, cid);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("bi") ? new HtmlEncodedHashListScalar(this.f200bi) : key.equals("ci") ? new HtmlEncodedHashListScalar(this.f201ci) : key.equals("plan") ? new HtmlEncodedHashListScalar(this.plan) : key.equals("other") ? new HtmlEncodedHashListScalar(this.other) : super.get(key);
    }

    public TemplateModel FM_setTempFakeRequest() throws Exception {
        this.oldRequest = Session.getRequest();
        Hashtable tmpRequest = this.other.getTempFakeRequest();
        Session.setRequest(new FakeRequest(tmpRequest));
        return this;
    }

    public TemplateModel FM_setOldRequest() throws Exception {
        Session.setRequest(this.oldRequest);
        return this;
    }

    public BillingInfoObject getBi() {
        return this.f200bi;
    }

    public ContactInfoObject getCi() {
        return this.f201ci;
    }

    public Plan getPlan() {
        return this.plan;
    }

    public OtherParameters getOther() {
        return this.other;
    }

    public HttpServletRequest getOldRequest() {
        return this.oldRequest;
    }

    public void sendBillingMessage(String messageTag) throws Exception {
        FakeRequest fRequest = new FakeRequest(this.other.getTempFakeRequest());
        HttpServletRequest oldReq = Session.getRequest();
        Session.setRequest(fRequest);
        try {
            try {
                String email = this.f200bi.getEmail();
                if (email == null) {
                    email = this.f201ci.getEmail();
                }
                SimpleHash root = new SimpleHash();
                root.put("ci", this.f201ci);
                root.put("bi", this.f200bi);
                root.put("plan", this.plan);
                root.put("other", this.other);
                root.put("toolbox", HsphereToolbox.toolbox);
                root.put("settings", new MapAdapter(Settings.get().getMap()));
                root.put("date", new TemplateString(new Date().toLocaleString()));
                root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
                root.put("request", fRequest);
                try {
                    CustomEmailMessage.send(messageTag, email, (TemplateModelRoot) root);
                } catch (Throwable se) {
                    Session.getLog().warn("Error sending message", se);
                }
                Session.setRequest(oldReq);
            } catch (NullPointerException npe) {
                Session.getLog().warn("NPE", npe);
                Session.setRequest(oldReq);
            } catch (Throwable t) {
                Session.getLog().warn("Error", t);
                Session.setRequest(oldReq);
            }
        } catch (Throwable th) {
            Session.setRequest(oldReq);
            throw th;
        }
    }
}
