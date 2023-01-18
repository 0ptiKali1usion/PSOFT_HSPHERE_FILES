package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.global.Globals;
import psoft.hsphere.plan.wizard.PlanChanger;
import psoft.hsphere.plan.wizard.PlanCreator;
import psoft.hsphere.plan.wizard.PlanEditor;
import psoft.hsphere.plan.wizard.PlanWizardXML;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.util.FakeRequest;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PlanReCreator.class */
public class PlanReCreator {
    private long planId;

    /* renamed from: pc */
    private PlanChanger f231pc;

    /* renamed from: p */
    private Plan f232p;
    private Document planDoc;
    private int periods;
    private ToolLogger log;
    private static Category hslog = Category.getInstance(PlanReCreator.class.getName());
    private Hashtable pricename = new Hashtable();
    private Hashtable planParams = new Hashtable();

    public PlanReCreator(long planId) throws Exception {
        this.planId = planId;
        this.pricename.put("_FREE_UNITS_", "f_");
        this.pricename.put("_SETUP_PRICE_", "s_");
        this.pricename.put("_UNIT_PRICE_", "m_");
        this.pricename.put("_USAGE_PRICE_", "u_");
        this.log = ToolLogger.getDefaultLogger();
    }

    public void recreatePlan() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT reseller_id FROM plans WHERE id = ?");
            ps.setLong(1, this.planId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Session.setResellerId(rs.getLong(1));
                this.f232p = Plan.getPlan(Long.toString(this.planId));
                this.log.outMessage("Re creating plan " + this.f232p.getDescription());
                this.f231pc = new PlanChanger(this.f232p);
                this.planDoc = PlanWizardXML.getWizardXML(this.f232p.getValue("_CREATED_BY_"));
                this.periods = Integer.parseInt(this.f232p.getValue("_PERIOD_TYPES"));
                Element root = this.planDoc.getDocumentElement();
                for (String key : this.f232p.getValueKeys()) {
                    if (key.startsWith("_HOST_")) {
                        this.planParams.put(key.substring(6, key.length()), new String[]{this.f232p.getValue(key)});
                    } else if ("_SEND_INVOICE".equals(key)) {
                        this.planParams.put("send_invoice", new String[]{"on"});
                    } else if ("_TRIAL_PERIOD".equals(key)) {
                        if (!this.planParams.keySet().contains("trial")) {
                            this.planParams.put("trial", new String[]{"2"});
                        }
                        this.planParams.put("trial_duration", new String[]{this.f232p.getValue(key)});
                    } else if ("_TRIAL_CREDIT".equals(key)) {
                        if (!this.planParams.keySet().contains("trial")) {
                            this.planParams.put("trial", new String[]{"2"});
                        }
                        this.planParams.put("trial_credit", new String[]{this.f232p.getValue(key)});
                    } else if ("_HARD_CREDIT".equals(key)) {
                        this.planParams.put("hard_credit", new String[]{this.f232p.getValue(key)});
                    } else if ("MONEY_BACK_DAYS".equals(key)) {
                        this.planParams.put("money_back", new String[]{"on"});
                        this.planParams.put("money_back_days", new String[]{this.f232p.getValue(key)});
                    } else {
                        this.planParams.put(key, new String[]{this.f232p.getValue(key)});
                    }
                }
                this.planParams.put("wizard", new String[]{this.f232p.getValue("_CREATED_BY_")});
                this.planParams.put("plan_name", new String[]{this.f232p.getDescription()});
                Node categoriesTop = XPathAPI.selectSingleNode(root, "categories");
                NodeList categories = XPathAPI.selectNodeList(categoriesTop, "category");
                for (int i = 0; i < categories.getLength(); i++) {
                    Node category = categories.item(i);
                    NodeList resources = XPathAPI.selectNodeList(category, "*");
                    for (int j = 0; j < resources.getLength(); j++) {
                        Node resource = resources.item(j);
                        if ("resource".equals(resource.getNodeName()) || "ifresource".equals(resource.getNodeName())) {
                            String rName = getAttribute(resource, "name");
                            if ("1".equals(getAttribute(resource, "required"))) {
                                addResource(rName);
                            } else if ("1".equals(getAttribute(resource, "adminonly")) && !isReseller()) {
                                addResource(rName);
                            } else if (isReseller() && "1".equals(getAttribute(resource, "reselleronly"))) {
                                addResource(rName);
                            } else if (this.f232p.areResourcesAvailable(rName)) {
                                addResource(rName);
                            }
                        } else if ("LogicalGroup".equals(resource.getNodeName())) {
                            String groupType = getAttribute(resource, "type");
                            String key2 = getAttribute(resource, "name");
                            String groupId = "";
                            Hashtable groups = EnterpriseManager.getLServerTypeGroups(HostEntry.getGroupTypeToId().get(groupType).toString());
                            if (groups != null) {
                                Enumeration e = groups.keys();
                                while (e.hasMoreElements()) {
                                    String grId = e.nextElement().toString();
                                    if (Globals.isSetKeyDisabled("server_groups", grId) == 0) {
                                        groupId = grId;
                                    }
                                }
                            }
                            this.planParams.put(key2, new String[]{groupId});
                        }
                    }
                }
                for (String key3 : this.planParams.keySet()) {
                    Session.getLog().debug(key3 + "=" + this.planParams.get(key3));
                }
                Session.setRequest(new FakeRequest(this.planParams));
                Plan newPlan = PlanCreator.process();
                this.planParams.put("plan_id", new String[]{Integer.toString(newPlan.getId())});
                PlanEditor.process();
                swapPlans(this.f232p, newPlan);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void addResource(String rName) throws Exception {
        this.planParams.put("i_" + rName, new String[]{"on"});
        if (this.f231pc.isResourceEnabled(rName)) {
            this.planParams.put("e_" + rName, new String[]{"on"});
        }
        getPrices(rName);
    }

    private void getPrices(String rName) throws Exception {
        String rNameId = TypeRegistry.getTypeId(rName);
        int i = 0;
        while (i < this.periods + 1) {
            for (String prefix : this.pricename.keySet()) {
                String key = prefix + (i == 0 ? "" : "" + (i - 1));
                String val = this.f232p.getValue(rNameId, key);
                this.log.outMessage("Got " + key + " price=" + val + " for " + rName + '\n');
                if (val != null && val.length() > 0) {
                    this.planParams.put(this.pricename.get(prefix) + rName + (i == 0 ? "" : "_" + (i - 1)), new String[]{val});
                }
            }
            i++;
        }
    }

    protected String getAttribute(Node n, String attName) {
        Node att = n.getAttributes().getNamedItem(attName);
        return att == null ? "" : att.getNodeValue();
    }

    protected boolean isReseller() throws UnknownResellerException {
        return Session.getResellerId() != 1;
    }

    /* JADX WARN: Finally extract failed */
    protected void swapPlans(Plan oldPlan, Plan newPlan) throws Exception {
        PreparedStatement ps = null;
        long oldPlanId = oldPlan.getId();
        long idToBeSetForOldPlan = -1;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT max(id) FROM plans");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idToBeSetForOldPlan = rs.getInt(1) + 1;
            }
            Session.closeStatement(ps);
            con.close();
            try {
                try {
                    con = Session.getTransConnection();
                    oldPlan.softDelete();
                    if (idToBeSetForOldPlan > 0) {
                        updatePlanId(oldPlanId, idToBeSetForOldPlan);
                        updatePlanId(newPlan.getId(), oldPlanId);
                    }
                    Session.commitTransConnection(con);
                    Session.closeStatement(ps);
                } catch (Throwable th) {
                    Session.commitTransConnection(con);
                    Session.closeStatement(ps);
                    throw th;
                }
            } catch (Exception e) {
                con.rollback();
                Session.commitTransConnection(con);
                Session.closeStatement(ps);
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    protected void updatePlanId(long oldPlanId, long newPlanId) throws Exception {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        PreparedStatement ps5 = null;
        PreparedStatement ps6 = null;
        PreparedStatement ps7 = null;
        PreparedStatement ps8 = null;
        Connection con = Session.isTransConnection() ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                ps1 = con.prepareStatement("UPDATE plan_access SET id = ? WHERE id = ?");
                ps2 = con.prepareStatement("UPDATE plan_access SET a_id = ? WHERE a_id = ?");
                ps3 = con.prepareStatement("UPDATE plan_imod SET plan_id = ? WHERE plan_id = ?");
                ps4 = con.prepareStatement("UPDATE plan_iresource  SET plan_id = ? WHERE plan_id = ?");
                ps5 = con.prepareStatement("UPDATE plan_ivalue SET plan_id = ? WHERE plan_id = ?");
                ps6 = con.prepareStatement("UPDATE plan_resource SET plan_id = ? WHERE plan_id = ?");
                ps7 = con.prepareStatement("UPDATE plans SET id = ? WHERE id = ?");
                ps8 = con.prepareStatement("UPDATE plan_value SET plan_id = ? WHERE plan_id = ?");
                ps1.setLong(1, newPlanId);
                ps1.setLong(2, oldPlanId);
                ps2.setLong(1, newPlanId);
                ps2.setLong(2, oldPlanId);
                ps3.setLong(1, newPlanId);
                ps3.setLong(2, oldPlanId);
                ps4.setLong(1, newPlanId);
                ps4.setLong(2, oldPlanId);
                ps5.setLong(1, newPlanId);
                ps5.setLong(2, oldPlanId);
                ps6.setLong(1, newPlanId);
                ps6.setLong(2, oldPlanId);
                ps7.setLong(1, newPlanId);
                ps7.setLong(2, oldPlanId);
                ps8.setLong(1, newPlanId);
                ps8.setLong(2, oldPlanId);
                ps1.executeUpdate();
                ps2.executeUpdate();
                ps3.executeUpdate();
                ps4.executeUpdate();
                ps5.executeUpdate();
                ps6.executeUpdate();
                ps7.executeUpdate();
                ps8.executeUpdate();
                Session.commitTransConnection(con);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
            } catch (Exception e) {
                con.rollback();
                Session.commitTransConnection(con);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
            }
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            Session.closeStatement(ps7);
            Session.closeStatement(ps8);
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        boolean configured = false;
        long planId = -1;
        ToolLogger log = new ToolLogger(argv);
        log.outMessage("Initializing....");
        ExternalCP.initCP();
        log.outOK();
        for (int i = 0; i < argv.length; i++) {
            if ("-p".equals(argv[i]) || "--plan_id".equals(argv[i])) {
                try {
                    planId = Long.parseLong(argv[i + 1]);
                    configured = true;
                } catch (NumberFormatException e) {
                    log.outMessage("Unparseable plan id " + argv[i + 1] + '\n');
                }
            }
        }
        if (configured) {
            log.outMessage("Parameters are accepted\n");
            PlanReCreator pr = new PlanReCreator(planId);
            pr.recreatePlan();
        }
    }
}
