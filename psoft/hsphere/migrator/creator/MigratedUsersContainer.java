package psoft.hsphere.migrator.creator;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/MigratedUsersContainer.class */
public class MigratedUsersContainer implements TemplateHashModel {
    private Hashtable users = new Hashtable();
    private int totalUsers;
    private int totalResellers;
    private int rAccountsCreated;
    private int uAccountsCreated;

    public MigratedUsersContainer(long migrationId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT username, new_username, plan, owner, reseller, account, content, qerrors FROM musers WHERE id = ?");
            ps.setLong(1, migrationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MigratedUser mu = new MigratedUser(rs.getString("username"), rs.getString("new_username"), rs.getString("plan"), rs.getString("owner"), rs.getInt(FMACLManager.RESELLER) == 1, rs.getInt("account") == 1, rs.getInt("content") == 1, rs.getInt("qerrors"));
                this.users.put(rs.getString("username"), mu);
                if (mu.isReseller()) {
                    this.totalResellers++;
                    if (mu.hasAccount()) {
                        this.rAccountsCreated++;
                    }
                } else {
                    this.totalUsers++;
                    if (mu.hasAccount()) {
                        this.uAccountsCreated++;
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Collection getMigratedUsers() {
        return this.users.values();
    }

    public List getUsers(boolean isReseller) {
        ArrayList l = new ArrayList();
        for (MigratedUser u : this.users.values()) {
            if (u.isReseller() == isReseller) {
                l.add(u.getOrigUserName());
            }
        }
        return l;
    }

    public List getDamagedResellers() {
        ArrayList l = new ArrayList();
        for (MigratedUser u : this.users.values()) {
            if (u.isReseller() && u.getErrorsNumber() != 0) {
                l.add(u.getOrigUserName());
            }
        }
        return l;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString("This is my name");
        }
        if (!"users".equals(key) && !"users".equals(key)) {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
        return new ListAdapter(this.users.values());
    }

    public TemplateModel FM_getUser(String uname) {
        return getUser(uname);
    }

    public MigratedUser getUser(String uname) {
        return (MigratedUser) this.users.get(uname);
    }

    public void removeUser(String userName) {
        this.users.remove(userName);
    }

    public void addUser(String userName, MigratedUser u) {
        this.users.put(userName, u);
    }

    public void deleteUser(String userName) throws Exception {
        MigratedUser u = (MigratedUser) this.users.get(userName);
        if (u != null) {
            u.delete();
            this.users.remove(userName);
        }
    }

    public void setAccountMark(String userName) throws Exception {
        MigratedUser mu = (MigratedUser) this.users.get(userName);
        mu.setAccountMark();
        if (mu.isReseller()) {
            this.rAccountsCreated++;
        } else {
            this.rAccountsCreated++;
        }
    }

    public void setContentMark(String userName) throws Exception {
        MigratedUser mu = (MigratedUser) this.users.get(userName);
        mu.setContentMark();
    }

    public int getNUsersTotal() {
        return this.totalUsers;
    }

    public int getNResellersTotal() {
        return this.totalResellers;
    }

    public int getNUserAccountsCreated() {
        return this.uAccountsCreated;
    }

    public int getNResellerAccountsCreated() {
        return this.rAccountsCreated;
    }

    public int getTotalErrorsNumber() {
        int errors = 0;
        for (MigratedUser u : this.users.values()) {
            errors += u.getErrorsNumber();
        }
        return errors;
    }

    public List getBunchOfUsers(int diapason, int items) {
        List result = new ArrayList();
        int begin = items * (diapason - 1);
        int end = begin + items;
        int curr = 0;
        Session.getLog().debug("DIAPASON=" + diapason + " ITEMS=" + items + " BEGIN=" + begin + " END=" + end);
        for (MigratedUser u : this.users.values()) {
            if (!u.isReseller()) {
                curr++;
                if (begin <= curr && curr <= end) {
                    result.add(u);
                }
            }
        }
        return result;
    }

    public boolean canCreateUsers() {
        for (MigratedUser u : this.users.values()) {
            if (!u.isReseller() && !u.isCreateable()) {
                return false;
            }
        }
        return true;
    }
}
