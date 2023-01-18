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
import psoft.hsphere.Session;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SystemInfoByReseller.class */
public class SystemInfoByReseller implements TemplateHashModel {
    protected TemplateModel getInfo() throws Exception {
        ArrayList arrayList = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT DISTINCT group_id, name FROM cmp_plan_group WHERE reseller_id = ? ORDER BY group_id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash group = new TemplateHash();
                group.put("group_id", rs.getString(1));
                group.put("name", rs.getString(2));
                group.put("plans", new ListAdapter(getPlanList(rs.getLong(1))));
                arrayList.add(group);
            }
            Session.closeStatement(ps);
            con.close();
            List ungroupedPlans = getUngroupedPlanList();
            if (ungroupedPlans != null) {
                TemplateHash group2 = new TemplateHash();
                group2.put("group_id", "0");
                group2.put("name", "");
                group2.put("plans", new ListAdapter(ungroupedPlans));
                arrayList.add(group2);
            }
            TreeSet sortedGroup = new TreeSet(new Comparator() { // from class: psoft.hsphere.resource.admin.SystemInfoByReseller.1
                @Override // java.util.Comparator
                public int compare(Object o1, Object o2) {
                    int num1;
                    int num2;
                    TemplateHash hs1 = (TemplateHash) o1;
                    TemplateHash hs2 = (TemplateHash) o2;
                    if (hs1 == hs2) {
                        return 0;
                    }
                    String str1 = hs1.get("name").toString();
                    String str2 = hs2.get("name").toString();
                    if (!"".equals(str1) || "".equals(str2)) {
                        if (!"".equals(str2) || "".equals(str1)) {
                            try {
                                num1 = Integer.parseInt(hs1.get("group_id").toString());
                            } catch (Exception e) {
                                num1 = 0;
                            }
                            try {
                                num2 = Integer.parseInt(hs2.get("group_id").toString());
                            } catch (Exception e2) {
                                num2 = 0;
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
            return new TemplateList(sortedGroup);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected List getPlanList(long groupId) throws Exception {
        List plans = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT g.plan_id, p.description  FROM cmp_plan_group g, plans p WHERE g.group_id = ? AND  g.plan_id = p.id AND p.reseller_id = ? AND (p.deleted IS NULL OR p.deleted<>1) ORDER BY p.description");
            ps.setLong(1, groupId);
            ps.setLong(2, Session.getResellerId());
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

    protected List getUngroupedPlanList() throws Exception {
        List plans = new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, description  FROM plans p WHERE reseller_id = ? AND not exists  (  SELECT plan_id FROM cmp_plan_group WHERE plan_id = id) AND (p.deleted IS NULL OR p.deleted<>1) ORDER BY description");
            ps.setLong(1, Session.getResellerId());
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

    protected String getUsers() throws Exception {
        new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(DISTINCT user_id) FROM user_account, users WHERE user_id = users.id AND users.reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String getAccounts() throws Exception {
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(DISTINCT account_id) FROM user_account, accounts WHERE account_id = accounts.id AND accounts.reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String getModeratedUsers() throws Exception {
        new ArrayList();
        Connection con = Session.getDb("SYSTEM_INFO");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(DISTINCT users.id) FROM users, request_record WHERE users.reseller_id = ? AND users.id = request_record.user_id AND  request_record.deleted IS NULL");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            return "";
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
            ps = con.prepareStatement("SELECT count(*) FROM accounts WHERE plan_id=? AND deleted IS NULL");
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
            if ("groups".equals(key)) {
                return getInfo();
            }
            if ("users".equals(key)) {
                return new TemplateString(getUsers());
            }
            if ("accounts".equals(key)) {
                return new TemplateString(getAccounts());
            }
            if ("moderated_users".equals(key)) {
                return new TemplateString(getModeratedUsers());
            }
            return null;
        } catch (Exception ex) {
            Session.getLog().error("Error geting System information", ex);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }
}
