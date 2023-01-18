package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SystemInfo.class */
public class SystemInfo implements TemplateHashModel {
    protected List getInfo() throws Exception {
        List resellers = new ArrayList();
        TemplateHash reseller = new TemplateHash();
        reseller.put("id", "1");
        reseller.put("name", "General admin");
        reseller.put("groups", getInfo(1L));
        resellers.add(reseller);
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT resellers.id, users.username FROM users, resellers WHERE resellers.id = users.id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash reseller2 = new TemplateHash();
                reseller2.put("id", rs.getString(1));
                reseller2.put("name", rs.getString(2));
                reseller2.put("groups", getInfo(rs.getLong(1)));
                resellers.add(reseller2);
            }
            Session.closeStatement(ps);
            con.close();
            return resellers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected TreeSet getInfo(long resellerId) throws Exception {
        ArrayList arrayList = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT DISTINCT group_id, name FROM cmp_plan_group WHERE reseller_id =  ?ORDER BY group_id");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash group = new TemplateHash();
                group.put("group_id", rs.getString(1));
                group.put("name", rs.getString(2));
                group.put("plans", new ListAdapter(getPlanList(rs.getLong(1), resellerId)));
                arrayList.add(group);
            }
            Session.closeStatement(ps);
            con.close();
            List ungroupedPlans = getUngroupedPlanList(resellerId);
            if (ungroupedPlans != null) {
                TemplateHash group2 = new TemplateHash();
                group2.put("group_id", "0");
                group2.put("name", "");
                group2.put("plans", new ListAdapter(ungroupedPlans));
                arrayList.add(group2);
            }
            TreeSet sortedGroup = new TreeSet(new Comparator() { // from class: psoft.hsphere.resource.admin.SystemInfo.1
                @Override // java.util.Comparator
                public int compare(Object o1, Object o2) {
                    TemplateHash hs1 = (TemplateHash) o1;
                    TemplateHash hs2 = (TemplateHash) o2;
                    if (hs1 == hs2) {
                        return 0;
                    }
                    String str1 = hs1.get("name").toString();
                    String str2 = hs2.get("name").toString();
                    if (!"".equals(str1) || "".equals(str2)) {
                        if (!"".equals(str2) || "".equals(str1)) {
                            int num1 = 0;
                            try {
                                num1 = Integer.parseInt(hs1.get("group_id").toString());
                            } catch (Exception e) {
                            }
                            int num2 = 0;
                            try {
                                num2 = Integer.parseInt(hs2.get("group_id").toString());
                            } catch (Exception e2) {
                            }
                            int res = str1.compareTo(str2);
                            if (res == 0) {
                                return num2 - num1;
                            }
                            return res;
                        }
                        return -1;
                    }
                    return 1;
                }

                @Override // java.util.Comparator
                public boolean equals(Object obj) {
                    return equals(obj);
                }
            });
            sortedGroup.addAll(arrayList);
            return sortedGroup;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected List getPlanList(long groupId, long resellerId) throws Exception {
        List plans = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT g.plan_id, p.description FROM cmp_plan_group g, plans p WHERE g.group_id = ? AND g.plan_id = p.id AND p.reseller_id = ? AND (p.deleted IS NULL OR p.deleted<>1) ORDER BY p.description");
            ps.setLong(1, groupId);
            ps.setLong(2, resellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(new TemplateHash(getInfoForPlan(rs.getString(1), rs.getString(2))));
            }
            Session.closeStatement(ps);
            con.close();
            return plans;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected List getUngroupedPlanList(long resellerId) throws Exception {
        List plans = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, description  FROM plans p WHERE reseller_id = ? AND not exists  (  SELECT plan_id FROM cmp_plan_group WHERE plan_id = id) AND (p.deleted IS NULL OR p.deleted<>1) ORDER BY description");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(new TemplateHash(getInfoForPlan(rs.getString(1), rs.getString(2))));
            }
            Session.closeStatement(ps);
            con.close();
            if (plans.size() == 0) {
                return null;
            }
            return plans;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected List getUsers() throws Exception {
        List resellers = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT resellers.id, users.username FROM users, resellers WHERE resellers.id = users.id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash reseller = new TemplateHash();
                reseller.put("id", rs.getString(1));
                reseller.put("name", rs.getString(2));
                reseller.put("users", new Long(getUsers(rs.getLong(1))));
                reseller.put("moderated_users", new Long(getModeratedUsers(rs.getLong(1))));
                resellers.add(reseller);
            }
            Session.closeStatement(ps);
            con.close();
            return resellers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected long getUsers(long resellerId) throws Exception {
        new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(DISTINCT user_id) FROM user_account, users WHERE user_id = users.id AND users.reseller_id = ?");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                Session.closeStatement(ps);
                con.close();
                return j;
            }
            Session.closeStatement(ps);
            con.close();
            return 0L;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected long getModeratedUsers(long resellerId) throws Exception {
        new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(DISTINCT users.id) FROM users, request_record WHERE users.reseller_id = ? AND users.id = request_record.user_id  AND request_record.deleted is null");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                Session.closeStatement(ps);
                con.close();
                return j;
            }
            Session.closeStatement(ps);
            con.close();
            return 0L;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected Hashtable getInfoForPlan(String planId, String description) throws Exception {
        Hashtable plan = new Hashtable();
        plan.put("plan_id", planId);
        plan.put("description", description);
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT COUNT(*) FROM domains d, parent_child p, accounts a WHERE p.child_id = d.id AND p.account_id = a.id and a.plan_id = ?");
            ps2.setString(1, planId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                plan.put("domains", rs.getString(1));
            }
            ps2.close();
            ps = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE plan_id = ? AND deleted IS NULL");
            ps.setString(1, planId);
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                plan.put("accounts", rs2.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            return plan;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("resellers".equals(key)) {
                return new TemplateList(getInfo());
            }
            if ("users".equals(key)) {
                return new TemplateList(getUsers());
            }
            if ("admin_users_count".equals(key)) {
                return new TemplateString(getAdminUsersCount());
            }
            if ("reseller_users_count".equals(key)) {
                return new TemplateString(getResellerUsersCount());
            }
            if ("resellers_list".equals(key)) {
                return new TemplateList(getResellers());
            }
            if ("total_accounts".equals(key)) {
                return new TemplateString(getAllAccountsCount());
            }
            return AccessTemplateMethodWrapper.getMethod(this, key);
        } catch (Exception ex) {
            Session.getLog().error("Error geting System information", ex);
            return null;
        }
    }

    public long getAllAccountsCount() throws Exception {
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        long result = 0;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) AS user_count FROM accounts WHERE deleted IS NULL");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getLong("user_count");
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected long getAdminUsersCount() throws Exception {
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        long result = 0;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) AS user_count FROM accounts WHERE reseller_id = 1 AND deleted IS NULL");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getLong("user_count");
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected long getResellerUsersCount() throws Exception {
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        long result = 0;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) AS user_count FROM accounts WHERE reseller_id > 1 AND deleted IS NULL");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getLong("user_count");
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected List getResellers() throws Exception {
        List resellers = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT resellers.id, users.username AS name, COUNT(accounts.id) AS users FROM users, resellers, accounts WHERE resellers.id = users.id AND accounts.reseller_id = resellers.id AND accounts.deleted IS NULL GROUP BY resellers.id, users.username");
            Session.getLog().debug("SQL:" + ps.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash reseller = new TemplateHash();
                reseller.put("id", rs.getString("id"));
                reseller.put("name", rs.getString("name"));
                reseller.put("users", rs.getString("users"));
                reseller.put("moderated_users", new Long(getModeratedUsers(rs.getLong("id"))));
                resellers.add(reseller);
            }
            Session.closeStatement(ps);
            con.close();
            return resellers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getReseller(long resellerId) throws Exception {
        Session.getLog().debug("FM_getReseller");
        Hashtable reseller = new Hashtable();
        if (resellerId == 1) {
            reseller.put("id", new Long(1L));
            reseller.put("name", "General admin");
            reseller.put("groups", new TemplateList(getInfo(1L)));
        } else {
            Connection con = Session.getDb("SYSTEM_INFO");
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT resellers.id, users.username FROM users, resellers WHERE resellers.id = users.id AND resellers.id = ?");
                ps.setLong(1, resellerId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    reseller.put("id", new Long(rs.getLong("id")));
                    reseller.put("name", rs.getString("username"));
                    reseller.put("groups", new TemplateList(getInfo(rs.getLong("id"))));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return new TemplateHash(reseller);
    }

    public boolean isEmpty() {
        return false;
    }
}
