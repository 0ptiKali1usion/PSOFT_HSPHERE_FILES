package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MonthlyRevenue.class */
public class MonthlyRevenue implements TemplateHashModel {
    protected Date begin;
    protected Date end;

    public MonthlyRevenue(int year, int month) {
        Calendar cal = TimeUtils.getCalendar();
        cal.set(year, month - 1, 1);
        java.util.Date begin = cal.getTime();
        cal.add(2, 1);
        java.util.Date end = cal.getTime();
        this.begin = new Date(begin.getTime());
        this.end = new Date(end.getTime());
    }

    public MonthlyRevenue() {
        Calendar cal = TimeUtils.getCalendar();
        cal.set(cal.get(1), cal.get(2), 1);
        java.util.Date begin = cal.getTime();
        cal.add(2, 1);
        java.util.Date end = cal.getTime();
        this.begin = new Date(begin.getTime());
        this.end = new Date(end.getTime());
    }

    protected TemplateModel getInfo() throws Exception {
        ArrayList arrayList = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT DISTINCT group_id, name  FROM cmp_plan_group WHERE reseller_id = ?  ORDER BY group_id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash group = new TemplateHash();
                group.put("group_id", rs.getString(1));
                group.put("name", rs.getString(2));
                List planList = getPlanList(rs.getLong(1));
                if (planList.size() > 0) {
                    group.put("plans", new ListAdapter(planList));
                    arrayList.add(group);
                }
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
            TreeSet sortedGroup = new TreeSet(new Comparator() { // from class: psoft.hsphere.resource.admin.MonthlyRevenue.1
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
                            Session.getLog().debug("Name1:" + str1 + " num1:" + num1 + "   Name2:" + str2 + " num2:" + num2);
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
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT g.plan_id, p.description  FROM cmp_plan_group g, plans p WHERE billing <> 0 AND g.group_id = ? AND  g.plan_id = p.id  AND p.reseller_id = ?  ORDER BY p.description");
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
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, description  FROM plans  WHERE deleted = 0 AND reseller_id = ? AND  billing <> 0 AND id NOT IN (  SELECT plan_id FROM cmp_plan_group) ORDER BY description");
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

    protected Hashtable getInfoForPlan(String planId, String description) throws Exception {
        Hashtable plan = new Hashtable();
        plan.put("plan_id", planId);
        plan.put("description", description);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" SELECT SUM(setup), SUM(special), SUM(recurrent),  SUM(usage), SUM(refund), SUM(moneyback) FROM revenue  WHERE  plan_id = ? AND cdate >= ? AND cdate < ?");
            ps.setString(1, planId);
            ps.setDate(2, this.begin);
            ps.setDate(3, this.end);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                plan.put("setup", new Double(rs.getDouble(1)));
                plan.put("special", new Double(rs.getDouble(2)));
                plan.put("recurrent", new Double(rs.getDouble(3)));
                plan.put("usage", new Double(rs.getDouble(4)));
                plan.put("refund", new Double(rs.getDouble(5)));
                plan.put("moneyback", new Double(rs.getDouble(6)));
                double total = ((((rs.getDouble(1) + rs.getDouble(2)) + rs.getDouble(3)) + rs.getDouble(4)) - rs.getDouble(5)) - rs.getDouble(6);
                plan.put("total", new Double(total));
            } else {
                plan.put("setup", new Double(0.0d));
                plan.put("special", new Double(0.0d));
                plan.put("recurrent", new Double(0.0d));
                plan.put("usage", new Double(0.0d));
                plan.put("refund", new Double(0.0d));
                plan.put("moneyback", new Double(0.0d));
                plan.put("total", new Double(0.0d));
            }
            return plan;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("groups".equals(key)) {
                return getInfo();
            }
            return null;
        } catch (Exception ex) {
            Session.getLog().error("Error getting monthly revenue.", ex);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }
}
