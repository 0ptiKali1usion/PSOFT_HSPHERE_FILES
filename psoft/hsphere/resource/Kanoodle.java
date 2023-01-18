package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.functions.KanoodleTransClient;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Kanoodle.class */
public class Kanoodle extends Resource {
    private String userId;
    private String partnerId;
    private static final String SERVER = "safe.kanoodle.com";
    private static final int PORT = 443;
    private static final String PATH = "/client_services/acctshare/rxdata.cool";
    private static final String kanoodleURL = "http://www.kanoodle.com/client_services/foKus_form.cool";

    public Kanoodle(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT userid, partnerId  FROM kanoodle WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.userId = rs.getString(1);
                this.partnerId = rs.getString(2);
            } else {
                this.userId = "";
                this.partnerId = Settings.get().getValue("kanoodle_userid");
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Kanoodle(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.partnerId = Settings.get().getValue("kanoodle_userid");
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM kanoodle WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_registerAdvertiser(String id, String passw, String firstName, String lastName, String companyName, String addr1, String addr2, String city, String province, String pZCode, String country, String email, String phoneNo, String spam, String billFirstName, String billLastName, String billCompanyName, String billAddr1, String billAddr2, String billCity, String billProvince, String billPZCode, String billCountry, String billPhoneNo, String url) throws Exception {
        HashMap data = new HashMap();
        this.partnerId = Settings.get().getValue("kanoodle_userid");
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
        KanoodleTransClient client = new KanoodleTransClient(SERVER, PORT, PATH, this.partnerId);
        HashMap result = client.createNewAdvertiser(data);
        String acc = (String) result.get("acc");
        if (!"1".equals(acc)) {
            String error = (String) result.get("error");
            throw new HSUserException("Advertiser account creation problem : " + error);
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
        PreparedStatement ps;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT userid, partnerId  FROM kanoodle WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                ps = con.prepareStatement("UPDATE kanoodle SET userId=?, partnerId=?  WHERE id=?");
                ps.setString(1, this.userId);
                ps.setString(2, this.partnerId);
                ps.setLong(3, getId().getId());
                ps.executeUpdate();
            } else {
                ps = con.prepareStatement("INSERT INTO kanoodle(id, userId, partnerId)  VALUES (?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.userId);
                ps.setString(3, this.partnerId);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "userId".equals(key) ? new TemplateString(this.userId) : "partnerId".equals(key) ? new TemplateString(this.partnerId) : "kanoodleurl".equals(key) ? new TemplateString(kanoodleURL) : super.get(key);
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
        return Localizer.translateMessage("bill.kanoodle.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.kanoodle.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.kanoodle.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.kanoodle.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
