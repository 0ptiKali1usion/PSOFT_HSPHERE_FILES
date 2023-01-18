package psoft.hsphere.resource.admin;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CmpPlanGroup.class */
public class CmpPlanGroup extends Resource {
    public static final int NOT_FOUND = -1;

    public CmpPlanGroup(int typeId, Collection initValues) throws Exception {
        super(typeId, initValues);
    }

    public CmpPlanGroup(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_getGroupedPlans() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT plan_id, group_id, name FROM cmp_plan_group WHERE reseller_id = ?  AND plan_id NOT IN (SELECT id FROM plans WHERE deleted = 1) ORDER BY group_id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            ArrayList arrayList = null;
            List groups = new ArrayList();
            int group_id = -1;
            while (rs.next()) {
                int tmp = rs.getInt(2);
                if (tmp != group_id) {
                    arrayList = new ArrayList();
                    HashMap hashMap = new HashMap();
                    group_id = tmp;
                    hashMap.put("id", Integer.toString(tmp));
                    hashMap.put("name", rs.getString(3));
                    hashMap.put("list", arrayList);
                    groups.add(hashMap);
                }
                arrayList.add(rs.getString(1));
            }
            TemplateList templateList = new TemplateList(groups);
            Session.closeStatement(ps);
            con.close();
            return templateList;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deleteGroup(int groupId) throws Exception {
        for (String str : getPlans(groupId)) {
            Plan pl = Plan.getPlan(str);
            pl.setGroup(0);
            pl.setGroupName(null);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM cmp_plan_group WHERE group_id = ? AND reseller_id = ?");
            ps.setInt(1, groupId);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "createGroup".equals(key) ? new GroupCreator() : super.get(key);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CmpPlanGroup$GroupCreator.class */
    class GroupCreator implements TemplateMethodModel {
        List list = new ArrayList();

        GroupCreator() {
            CmpPlanGroup.this = r5;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                List l2 = HTMLEncoder.decode(l);
                if (l2.isEmpty()) {
                    return CmpPlanGroup.this.createGroup(this.list);
                }
                this.list.add(l2.get(0));
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating cmp_plan_group", e);
                return new TemplateErrorResult(e);
            }
        }
    }

    protected TemplateModel createGroup(List elements) throws Exception {
        int id = Session.getNewId("newid");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Iterator i = elements.iterator();
            if (i.hasNext()) {
                ps = con.prepareStatement("INSERT INTO cmp_plan_group(plan_id, group_id, name, reseller_id) VALUES (?, ?, ?, ?)");
                String groupName = (String) i.next();
                if (groupName != null) {
                    ps.setString(3, groupName);
                } else {
                    ps.setNull(3, 12);
                }
                ps.setInt(2, id);
                ps.setLong(4, Session.getResellerId());
                while (i.hasNext()) {
                    int planId = Integer.parseInt((String) i.next());
                    Plan.getPlan(planId).setGroup(id);
                    Plan.getPlan(planId).setGroupName(groupName);
                    ps.setInt(1, planId);
                    ps.executeUpdate();
                }
                Session.closeStatement(ps);
                con.close();
                return this;
            }
            Session.closeStatement(null);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getPlansByPlanId(int planId) throws Exception {
        return new TemplateList(getPlans(getGroupId(planId)));
    }

    public TemplateModel FM_getPlans(int groupId) throws Exception {
        return new TemplateList(getPlans(groupId));
    }

    public TemplateModel FM_getGroupId(int planId) throws Exception {
        return new TemplateString(getGroupId(planId));
    }

    protected List getPlans(int groupId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT plan_id FROM cmp_plan_group WHERE group_id = ? AND reseller_id = ?  AND plan_id NOT IN (SELECT id FROM plans WHERE deleted = 1)");
            ps.setInt(1, groupId);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected int getGroupId(int planId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT group_id FROM cmp_plan_group WHERE plan_id = ?");
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int i = rs.getInt(1);
                Session.closeStatement(ps);
                con.close();
                return i;
            }
            Session.closeStatement(ps);
            con.close();
            return -1;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
