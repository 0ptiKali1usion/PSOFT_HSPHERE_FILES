package psoft.hsphere.migrator.creator;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.user.UserException;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/MigratedUser.class */
public class MigratedUser implements TemplateHashModel {
    public static final int ACCOUNT = 1;
    public static final int CONTENT = 2;
    private String userName;
    private String newUserName;
    private String planName;
    private String owner;
    private boolean isReseller;
    private boolean canBeCreated;
    private boolean hasAccount;
    private boolean hasContent;
    private int qErrors;
    private List reasons = new ArrayList();
    private List ttIds = new ArrayList();

    public MigratedUser(String uname, String newUserName, String plan, String owner, boolean isReseller, boolean hasAccount, boolean hasContent, int qerrors) throws Exception {
        this.userName = uname;
        this.newUserName = newUserName;
        this.planName = plan;
        this.owner = owner;
        this.isReseller = isReseller;
        this.hasAccount = hasAccount;
        this.hasContent = hasContent;
        this.qErrors = qerrors;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ttid FROM merrors WHERE username = ?");
            ps.setString(1, this.userName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.ttIds.add(rs.getString("ttid"));
            }
            Session.closeStatement(ps);
            con.close();
            checkCreate();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean checkCreate() {
        this.reasons.clear();
        boolean canBeCreated = true;
        try {
            int planId = Plan.getPlanIdByName(this.planName);
            if (planId == -1) {
                canBeCreated = false;
                addReason("There is no plan " + this.planName);
            } else {
                Plan plan = Plan.getPlan(planId);
                if (this.isReseller) {
                    if (!"6".equals(plan.getValue("_CREATED_BY_"))) {
                        canBeCreated = false;
                        addReason("Plan " + this.planName + " is not a reseller plan");
                    }
                } else {
                    if (plan.isResourceAvailable("ip") == null) {
                        canBeCreated = false;
                        addReason("Resource ip is disabled in plan " + this.planName);
                    }
                    if (plan.isResourceAvailable("domain") == null) {
                        canBeCreated = false;
                        addReason("Resource domain is disabled in plan " + this.planName);
                    }
                    if (plan.isResourceAvailable("nodomain") == null) {
                        canBeCreated = false;
                        addReason("Resource nodomain is disabled in plan " + this.planName);
                    }
                }
            }
            try {
                User.getUser(this.userName);
                canBeCreated = false;
                addReason("User " + this.userName + " already exists. Change user name.");
            } catch (UserException e) {
            }
        } catch (Exception ex) {
            Session.getLog().error("Error during creation check", ex);
            canBeCreated = false;
        }
        return canBeCreated;
    }

    private void addReason(String reason) {
        this.reasons.add(reason);
    }

    public List getReasons() {
        return this.reasons;
    }

    public boolean isCreateAble() {
        return checkCreate();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "username".equals(key) ? new TemplateString(this.userName) : "plan".equals(key) ? new TemplateString(this.planName) : "owner".equals(key) ? new TemplateString(this.owner) : "reasons".equals(key) ? new ListAdapter(this.reasons) : "canBeCreated".equals(key) ? new TemplateString(isCreateAble()) : "hasAccount".equals(key) ? new TemplateString(hasAccount()) : "hasContent".equals(key) ? new TemplateString(hasContent()) : "errors".equals(key) ? new TemplateString(this.qErrors) : "isReseller".equals(key) ? new TemplateString(isReseller()) : "tts".equals(key) ? new ListAdapter(this.ttIds) : "tt_ids".equals(key) ? getTTIds() : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isReseller() {
        return this.isReseller;
    }

    public void changePlanName(int newPlan) throws Exception {
        Plan plan = Plan.getPlan(newPlan);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE musers SET plan = ? WHERE username = ? ");
            ps.setString(1, plan.getDescription());
            ps.setString(2, this.userName);
            ps.executeUpdate();
            this.planName = plan.getDescription();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void changeUserName(String newUname) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE musers SET new_username = ? WHERE username = ? ");
            ps.setString(1, newUname);
            ps.setString(2, this.userName);
            ps.executeUpdate();
            this.userName = newUname;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void createUser(long migrationId, String uname, String pname, String owner, int reseller) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO musers(id, username, new_username, plan, owner, reseller, account, content, qerrors) values(?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, migrationId);
            ps.setString(2, uname);
            ps.setString(3, uname);
            ps.setString(4, pname);
            ps.setString(5, owner);
            ps.setInt(6, reseller);
            ps.setInt(7, 0);
            ps.setInt(8, 0);
            ps.setInt(9, 0);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setAccountMark() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE musers set account = ? WHERE username = ?");
            ps.setInt(1, 1);
            ps.setString(2, this.userName);
            ps.executeUpdate();
            this.hasAccount = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setContentMark() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE musers set content = ? WHERE username = ?");
            ps.setInt(1, 1);
            ps.setString(2, this.userName);
            ps.executeUpdate();
            this.hasContent = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean hasAccount() {
        return this.hasAccount;
    }

    public boolean hasContent() {
        return this.hasContent;
    }

    public String getUserName() {
        return this.newUserName;
    }

    public String getOrigUserName() {
        return this.userName;
    }

    public String getPlanName() {
        return this.planName;
    }

    public void submitError(Ticket tt) throws Exception {
        Session.getLog().debug("SUBMITING ERROR " + tt.get("title").toString());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("UPDATE musers SET qerrors = qerrors + 1 WHERE username= ?");
            ps2.setString(1, this.userName);
            ps2.executeUpdate();
            ps = con.prepareStatement("INSERT INTO merrors(username, ttid) VALUES(?,?)");
            ps.setString(1, this.userName);
            ps.setLong(2, tt.getId());
            ps.executeUpdate();
            this.ttIds.add(Long.toString(tt.getId()));
            this.qErrors++;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM merrors WHERE username = ?");
            ps2.setString(1, this.userName);
            ps2.executeUpdate();
            ps = con.prepareStatement("DELETE FROM musers WHERE username = ?");
            ps.setString(1, this.userName);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int getErrorsNumber() {
        return this.qErrors;
    }

    public void clearErrors() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM merrors WHERE username = ?");
            ps2.setString(1, this.userName);
            ps2.executeUpdate();
            ps = con.prepareStatement("UPDATE musers SET account = 0, qerrors = 0 WHERE username = ?");
            ps.setString(1, this.userName);
            ps.executeUpdate();
            this.ttIds.clear();
            this.qErrors = 0;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private TemplateModel getTTIds() {
        Iterator i = this.ttIds.iterator();
        StringBuffer ids = new StringBuffer();
        while (i.hasNext()) {
            ids.append(((String) i.next()) + (i.hasNext() ? "," : ""));
        }
        return new TemplateString(ids.toString());
    }

    public boolean isCreateable() {
        return checkCreate();
    }
}
