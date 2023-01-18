package psoft.hsphere.resource.admin;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SSHManager.class */
public class SSHManager extends Resource {
    protected static final int NO_REWIEWED_ST = 0;
    protected static final int REFUSED_ST = 1;
    protected static final int ACCEPTED_ST = 2;
    protected static final int DISABLED_ST = 3;

    public SSHManager(int type, Collection init) throws Exception {
        super(type, init);
    }

    public SSHManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_acceptReq(long accountId) throws Exception {
        Connection con = Session.getDb();
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            try {
                Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
                User newuser = newAccount.getUser();
                Session.setUser(newuser);
                Session.setAccount(newAccount);
                Resource unixUser = newAccount.FM_getChild("unixuser").get();
                if (unixUser.FM_getChild("sshresource") != null) {
                    throw new HSUserException("sshrequest.enabled");
                }
                synchronized (Session.getAccount()) {
                    unixUser.addChild("sshresource", "", new ArrayList());
                }
                PreparedStatement ps = con.prepareStatement("UPDATE shell_req SET refused = ? WHERE id = ?");
                ps.setInt(1, 2);
                ps.setLong(2, accountId);
                ps.executeUpdate();
                sendNotification(newAccount, 2);
                Session.closeStatement(ps);
                con.close();
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                return this;
            } catch (Exception ex) {
                Session.getLog().debug("Can't enable access: ", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_disableAccess(long accountId) throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            try {
                Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
                User newuser = newAccount.getUser();
                Session.setUser(newuser);
                Session.setAccount(newAccount);
                Resource unixUser = newAccount.FM_getChild("unixuser").get();
                ResourceId rid = unixUser.FM_findChild("sshresource");
                if (rid == null) {
                    throw new HSUserException("sshrequest.disabled");
                }
                Resource sResource = rid.get();
                sResource.delete(false);
                sendNotification(newAccount, 3);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                return this;
            } catch (Exception ex) {
                Session.getLog().debug("Can't disable access: ", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_refuseReq(long accountId, String reason) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE shell_req SET refused = ?, response = ? where id = ?");
            ps.setInt(1, 1);
            ps.setString(2, reason);
            ps.setLong(3, accountId);
            ps.executeUpdate();
            Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
            sendNotification(newAccount, 1);
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getRequests(String accepted) throws Exception {
        deleteRequests();
        ArrayList data = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, req_date, refused FROM shell_req");
            ResultSet rs = ps.executeQuery();
            int query = 0;
            if (accepted.equals("accepted")) {
                query = 2;
            }
            while (rs.next()) {
                if (rs.getInt(3) == query) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("accountId", new Long(rs.getLong(1)));
                    hashMap.put("date", rs.getTimestamp(2));
                    String username = getUserByAccountId(rs.getLong(1));
                    hashMap.put("username", username);
                    if (!"Unknown user".equals(username)) {
                        data.add(hashMap);
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
            return new ListAdapter(data);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void deleteRequests() throws Exception {
        Date now = TimeUtils.getDate();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM shell_req WHERE refused = ? and req_date < ?");
            ps.setInt(1, 1);
            ps.setTimestamp(2, new Timestamp(now.getTime() - 604800000));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String getUserByAccountId(long aid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE id IN (SELECT user_id FROM user_account WHERE account_id = ?)");
            ps.setLong(1, aid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            PreparedStatement ps2 = con.prepareStatement("DELETE from shell_req where id = ?");
            ps2.setLong(1, aid);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            con.close();
            return "Unknown user";
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    private void sendNotification(Account a, int messType) throws Exception {
        String email = a.getContactInfo().getEmail();
        SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
        String subj = "";
        String sshNotif = Settings.get().getValue("sshnotifications");
        if (!"1".equals(sshNotif)) {
            return;
        }
        switch (messType) {
            case 1:
                subj = Localizer.translateLabel("admin.settings.header.ssh_refuse");
                root.put("result", new TemplateString("REFUSED"));
                break;
            case 2:
                subj = Localizer.translateLabel("admin.settings.header.ssh_accept");
                root.put("result", new TemplateString("OK"));
                break;
            case 3:
                subj = Localizer.translateLabel("admin.settings.header.ssh_disable");
                root.put("result", new TemplateString("DISABLED"));
                break;
        }
        root.put("subject", subj);
        try {
            CustomEmailMessage.send("SSH_NOTIFICATION", email, (TemplateModelRoot) root);
        } catch (Throwable se) {
            Session.getLog().warn("Error sending message", se);
        }
    }
}
