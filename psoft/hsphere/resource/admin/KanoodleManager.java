package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.functions.KanoodleTransClient;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/KanoodleManager.class */
public class KanoodleManager extends Resource {
    private static final String SERVER = "safe.kanoodle.com";
    private static final int PORT = 443;
    private static final String PATH = "/client_services/acctshare/rxdata.cool";
    private static final String PARTNER = "65032523";
    private String userId;
    private String enableSignup;

    public KanoodleManager(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public KanoodleManager(ResourceId rId) throws Exception {
        super(rId);
        this.userId = Settings.get().getValue("kanoodle_userid");
        this.enableSignup = Settings.get().getValue("kanoodle_signup");
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
        return Localizer.translateMessage("bill.kanoodlemanager.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.kanoodlemanager.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.kanoodlemanager.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.sitestudio.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        Settings.get().setValue("kanoodle_login", "");
        Settings.get().setValue("kanoodle_password", "");
        Settings.get().setValue("kanoodle_userid", "");
    }

    public TemplateModel FM_registerPartner(String id, String passw, String firstName, String lastName, String companyName, String addr1, String addr2, String city, String province, String pZCode, String country, String email, String phoneNo, String spam, String billFirstName, String billLastName, String billCompanyName, String billAddr1, String billAddr2, String billCity, String billProvince, String billPZCode, String billCountry, String billPhoneNo, String url) throws Exception {
        HashMap data = new HashMap();
        data.put("Id", id);
        data.put("Password", passw);
        data.put("FirstName", firstName);
        data.put("LastName", lastName);
        data.put("CompanyName", companyName);
        data.put("Addr1", addr1);
        data.put("Addr2", addr2);
        data.put("City", city);
        data.put("Province", province);
        data.put("PZCode", pZCode);
        data.put("Country", country);
        data.put("Email", email);
        data.put("PhoneNo", phoneNo);
        data.put("Spam", spam);
        data.put("BillFirstName", billFirstName);
        data.put("BillLastName", billLastName);
        data.put("BillCompanyName", billCompanyName);
        data.put("BillAddr1", billAddr1);
        data.put("BillAddr2", billAddr2);
        data.put("BillCity", billCity);
        data.put("BillProvince", billProvince);
        data.put("BillPZCode", billPZCode);
        data.put("BillCountry", billCountry);
        data.put("BillPhoneNo", billPhoneNo);
        data.put("Url", url);
        data.put("Package", "Basic");
        data.put("Existing", "0");
        data.put("FullText", "-1");
        KanoodleTransClient client = new KanoodleTransClient(SERVER, PORT, PATH, PARTNER);
        HashMap result = client.createNewPartner(data);
        String acc = (String) result.get("acc");
        if (!"1".equals(acc)) {
            String error = (String) result.get("error");
            throw new HSUserException("Partner account creation problem : " + error);
        }
        this.userId = (String) result.get("assignedid");
        updateSettings();
        return this;
    }

    public TemplateModel FM_update(String userid) throws Exception {
        this.userId = userid;
        updateSettings();
        return this;
    }

    private void updateSettings() throws Exception {
        Settings.get().setValue("kanoodle_userid", this.userId);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "userId".equals(key) ? new TemplateString(this.userId) : "signup".equals(key) ? new TemplateString(this.enableSignup) : super.get(key);
    }
}
