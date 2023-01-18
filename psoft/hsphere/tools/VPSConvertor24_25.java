package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.migrator.PlanCreator;
import psoft.hsphere.migrator.PlanExtractor;
import psoft.hsphere.migrator.ResellerPlanHolder;
import psoft.hsphere.migrator.extractor.InfoExtractorUtils;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/VPSConvertor24_25.class */
public class VPSConvertor24_25 extends C0004CP {
    private ToolLogger logger;
    private HashMap<Long, Long> convertedPlans;
    private static List<Long> chosenPlans = new ArrayList();
    private static List<Long> chosenAccs = new ArrayList();
    private static final String outdatedStr = "(outdated since HS 2.5.0)";

    public VPSConvertor24_25(ToolLogger logger) throws Exception {
        super("psoft_config.hsphere");
        this.convertedPlans = new HashMap<>();
        this.logger = logger;
    }

    public static void main(String[] args) throws Exception {
        ToolLogger logger = new ToolLogger(args);
        boolean areArgsCorrect = false;
        boolean convertPlans = false;
        boolean convertAccs = false;
        if (args.length > 0) {
            if ("--all".equals(args[0])) {
                convertPlans = true;
                convertAccs = true;
                areArgsCorrect = true;
            } else {
                int i = 0;
                while (i < args.length) {
                    if ("--plans".equals(args[i])) {
                        convertPlans = true;
                        if (i < args.length - 1 && args[i + 1].indexOf("--") == -1) {
                            StringTokenizer tokenizer = new StringTokenizer(args[i + 1], ",");
                            while (tokenizer.hasMoreTokens()) {
                                String token = tokenizer.nextToken();
                                chosenPlans.add(Long.valueOf(token));
                            }
                            i++;
                        }
                        areArgsCorrect = true;
                    } else if ("--accounts".equals(args[i])) {
                        convertAccs = true;
                        if (i < args.length - 1 && args[i + 1].indexOf("--") == -1) {
                            StringTokenizer tokenizer2 = new StringTokenizer(args[i + 1], ",");
                            while (tokenizer2.hasMoreTokens()) {
                                String token2 = tokenizer2.nextToken();
                                chosenAccs.add(Long.valueOf(token2));
                            }
                            i++;
                        }
                        areArgsCorrect = true;
                    }
                    i++;
                }
            }
            if (areArgsCorrect) {
                VPSConvertor24_25 vpsc = new VPSConvertor24_25(logger);
                vpsc.m0go(convertPlans, convertAccs);
                return;
            }
        }
        System.out.println("NAME:\n\t psoft.hsphere.tools.VPSConvertor24_25\n\t\t- Convert VPS plans and accounts from H-Sphere 2.4 to 2.5");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.VPSConvertor24_25 [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t--all convert plans and accounts");
        System.out.println("\t--plans [id1,id2...] convert plans only (use plan ids to convert particular plans)");
        System.out.println("\t--accounts [id1,id2...] convert accounts only (use account ids to convert particular accounts)");
    }

    /* renamed from: go */
    private void m0go(boolean convertPlans, boolean convertAccs) throws Exception {
        String version = getVersion();
        if (!"2.5".equals(version.substring(0, 3))) {
            this.logger.outMessage("Current H-Sphere version is " + version + ".Convertion of old VPS plans and accounts works only for H-Sphere 2.5");
            System.exit(0);
        }
        Session.save();
        try {
            processPlans(convertPlans, chosenPlans);
            if (convertAccs) {
                if (this.convertedPlans.keySet().isEmpty()) {
                    this.logger.outMessage("No plans were converted. VPS accounts convertion will be skipped");
                    System.exit(0);
                }
                processAccounts(chosenAccs);
            }
        } catch (Exception e) {
            this.logger.outMessage("Failed to finish VPS plans and accounts convertion", e);
        } finally {
            Session.restore();
        }
    }

    private void processPlans(boolean convertPlans, List<Long> planIds) throws Exception {
        Connection con = Session.getDb();
        StringBuffer plansIdsInsert = new StringBuffer("");
        Iterator i = planIds.iterator();
        while (i.hasNext()) {
            plansIdsInsert.append(i.next());
            if (i.hasNext()) {
                plansIdsInsert.append(",");
            }
        }
        PreparedStatement ps1 = null;
        try {
            ps1 = con.prepareStatement("SELECT DISTINCT pv.plan_id, u.username, p.reseller_id FROM plan_value pv JOIN plans p ON pv.plan_id=p.id JOIN users u ON p.reseller_id=u.id WHERE pv.name='_CREATED_BY_' AND pv.value='vps' AND p.deleted = 0 AND p.description NOT LIKE '%(outdated since HS 2.5.0)' " + (planIds.isEmpty() ? "" : "AND p.id IN (" + ((Object) plansIdsInsert) + ") ") + "AND p.id NOT IN (SELECT plan_id FROM plan_value WHERE name='_new_scheme')");
            ResultSet rs1 = ps1.executeQuery();
            if (convertPlans) {
                this.logger.outMessage("VPS plans convertion started...\n");
            }
            while (rs1.next()) {
                long curPlanId = rs1.getLong(1);
                if (convertPlans) {
                    try {
                        processPlan(curPlanId, rs1.getString(2), Long.valueOf(rs1.getLong(3)));
                    } catch (Exception ex) {
                        this.logger.outMessage("Failed to convert vps plan " + curPlanId, ex);
                    }
                } else {
                    this.convertedPlans.put(Long.valueOf(curPlanId), Long.valueOf(rs1.getLong(3)));
                }
            }
            if (convertPlans) {
                this.logger.outMessage("VPS plans convertion finished\n\n");
            }
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    private void processAccounts(List<Long> accIds) throws Exception {
        StringBuffer accIdsInsert = new StringBuffer("");
        Iterator i = accIds.iterator();
        while (i.hasNext()) {
            accIdsInsert.append(i.next());
            if (i.hasNext()) {
                accIdsInsert.append(",");
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        StringBuffer plansInsert = new StringBuffer("");
        try {
            for (Long l : this.convertedPlans.keySet()) {
                long planId = l.longValue();
                long reselId = this.convertedPlans.get(Long.valueOf(planId)).longValue();
                Plan.load((int) planId, reselId);
            }
            this.logger.outMessage("VPS accounts convertion started...\n");
            try {
                Iterator i2 = this.convertedPlans.keySet().iterator();
                while (i2.hasNext()) {
                    plansInsert.append(i2.next());
                    if (i2.hasNext()) {
                        plansInsert.append(",");
                    }
                }
                if ("".equals(plansInsert.toString())) {
                    this.logger.outMessage("No VPS plans were converted. VPS accounts convertion is not needed");
                } else {
                    ps2 = con.prepareStatement("SELECT pc.parent_id, pc.child_id, v.name, a.id FROM accounts a JOIN parent_child pc ON a.id=pc.account_id JOIN vps v ON pc.parent_id=v.id WHERE a.plan_id IN (" + plansInsert.toString() + ") " + (accIds.isEmpty() ? "" : "AND a.id IN (" + ((Object) accIdsInsert) + ") ") + "AND pc.parent_type=7000 AND pc.child_type=3001");
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next()) {
                        long vpsResId = rs2.getLong(1);
                        long dnsZoneId = rs2.getLong(2);
                        String vpsHostName = rs2.getString(3);
                        long accId = rs2.getLong(4);
                        try {
                            this.logger.outMessage("\nProcessing account with id " + accId + "\n");
                            processAccount(vpsResId, dnsZoneId, vpsHostName, accId);
                            this.logger.outOK();
                        } catch (Exception e2) {
                            this.logger.outMessage("\tFailed to convert vps account " + accId, e2);
                        }
                    }
                }
            } catch (Exception e1) {
                this.logger.outMessage("Failed to convert vps accounts", e1);
            }
            this.logger.outMessage("VPS accounts convertion finished\n");
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    private void processPlan(long curPlanId, String resellerName, Long reselId) throws Exception {
        this.logger.outMessage("Processing plan with id " + curPlanId + " ...\n");
        Document document = getNewPlanXML(curPlanId, resellerName);
        if (document == null) {
            return;
        }
        long newPlanId = createNewPlan(document);
        if (newPlanId < 1) {
            this.logger.outMessage("\tFailed to create new plan\n");
            return;
        }
        boolean result = replaceOldPlanWithNew(curPlanId, newPlanId);
        if (result) {
            this.convertedPlans.put(Long.valueOf(curPlanId), reselId);
            this.logger.outOK();
            return;
        }
        this.logger.outFail();
    }

    private Document getNewPlanXML(long curPlanId, String resellerName) {
        ArrayList resellers = new ArrayList();
        try {
            Document document = InfoExtractorUtils.createDocument("plans", "plans.dtd");
            PlanExtractor extractor = new PlanExtractor(document, "plans.xml", true);
            ResellerPlanHolder resellerPlanHolder = new ResellerPlanHolder(resellerName);
            resellerPlanHolder.setUsingPlansIDs(true);
            resellerPlanHolder.addPlan(Long.toString(curPlanId));
            resellers.add(resellerPlanHolder);
            Node root = extractor.extractPlans(resellers);
            document.appendChild(root);
            boolean isOldPlan = true;
            NodeList lGroups = document.getElementsByTagName("LogicalGroup");
            for (int i = 0; i < lGroups.getLength(); i++) {
                Element lGroup = (Element) lGroups.item(i);
                String lGroupName = lGroup.getAttribute("name");
                if ("mail".equals(lGroupName)) {
                    isOldPlan = false;
                }
            }
            if (isOldPlan) {
                NodeList resources = document.getElementsByTagName("resource");
                for (int i2 = 0; i2 < resources.getLength(); i2++) {
                    Element resource = (Element) resources.item(i2);
                    String resName = resource.getAttribute("name");
                    if ("domain".equals(resName) || "3l_dns_zone".equals(resName) || "billing_info".equals(resName) || "contact_info".equals(resName)) {
                        resource.setAttribute("include", "1");
                    }
                }
                Element mailLGroup = document.createElement("LogicalGroup");
                mailLGroup.setAttribute("groupid", "3");
                mailLGroup.setAttribute("name", "mail");
                mailLGroup.setAttribute("type", "mail");
                NodeList ifgroups = document.getElementsByTagName("ifgroup");
                Element ifgroup = (Element) ifgroups.item(0);
                ifgroup.appendChild(mailLGroup);
                NodeList plans = document.getElementsByTagName("plan");
                Element plan = (Element) plans.item(0);
                String planName = plan.getAttribute("name");
                plan.setAttribute("name", planName + " (New VPS Plan Converted)");
                return document;
            }
            this.logger.outMessage("\tThe plan matches new scheme. There is no need to convert it\n");
            return null;
        } catch (Exception e) {
            this.logger.outMessage("Failed to extract xml for the plan", e);
            return null;
        }
    }

    private long createNewPlan(Document document) {
        int newPlanId = -1;
        try {
            PlanCreator creator = new PlanCreator(document, true, true);
            NodeList plans = document.getElementsByTagName("plan");
            this.logger.outMessage("\tCreating new plan...");
            newPlanId = creator.createPlan((Element) plans.item(0));
            if (newPlanId > 0) {
                this.logger.outMessage("\tThe plan created with id " + newPlanId + "\n");
                return newPlanId;
            }
        } catch (Exception e) {
            this.logger.outMessage("Failed to create new plan", e);
        }
        return newPlanId;
    }

    private boolean replaceOldPlanWithNew(long curPlanId, long newPlanId) throws Exception {
        boolean result = false;
        Plan curPlan = Plan.getPlan((int) curPlanId);
        String planName = curPlan.getDescription();
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config/hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getTransConnection();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        PreparedStatement ps5 = null;
        PreparedStatement ps6 = null;
        PreparedStatement ps7 = null;
        PreparedStatement ps8 = null;
        PreparedStatement ps9 = null;
        PreparedStatement ps10 = null;
        PreparedStatement ps11 = null;
        PreparedStatement ps12 = null;
        long newIdForOldPlan = -1;
        this.logger.outMessage("\tUpdating plan tables...\n");
        try {
            try {
                ps = con.prepareStatement("SELECT max(id) FROM plans");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    newIdForOldPlan = rs.getLong(1) + 1;
                }
                ps1 = con.prepareStatement("UPDATE plan_imod SET plan_id = ? WHERE plan_id = ? ");
                ps1.setLong(1, newIdForOldPlan);
                ps1.setLong(2, curPlanId);
                ps1.executeUpdate();
                ps2 = con.prepareStatement("UPDATE plan_iresource SET plan_id = ? WHERE plan_id = ? ");
                ps2.setLong(1, newIdForOldPlan);
                ps2.setLong(2, curPlanId);
                ps2.executeUpdate();
                ps3 = con.prepareStatement("UPDATE plan_resource SET plan_id = ? WHERE plan_id = ? ");
                ps3.setLong(1, newIdForOldPlan);
                ps3.setLong(2, curPlanId);
                ps3.executeUpdate();
                ps4 = con.prepareStatement("UPDATE plan_ivalue SET plan_id = ? WHERE plan_id = ? ");
                ps4.setLong(1, newIdForOldPlan);
                ps4.setLong(2, curPlanId);
                ps4.executeUpdate();
                ps5 = con.prepareStatement("UPDATE plan_value SET plan_id = ? WHERE plan_id = ? ");
                ps5.setLong(1, newIdForOldPlan);
                ps5.setLong(2, curPlanId);
                ps5.executeUpdate();
                ps6 = con.prepareStatement("UPDATE plans SET id = ?, description = ?, disabled = ? WHERE id = ? ");
                ps6.setLong(1, newIdForOldPlan);
                ps6.setString(2, planName + outdatedStr);
                ps6.setInt(3, 1);
                ps6.setLong(4, curPlanId);
                ps6.executeUpdate();
                ps7 = con.prepareStatement("UPDATE plan_imod SET plan_id = ? WHERE plan_id = ? ");
                ps7.setLong(1, curPlanId);
                ps7.setLong(2, newPlanId);
                ps7.executeUpdate();
                ps8 = con.prepareStatement("UPDATE plan_iresource SET plan_id = ? WHERE plan_id = ? ");
                ps8.setLong(1, curPlanId);
                ps8.setLong(2, newPlanId);
                ps8.executeUpdate();
                ps9 = con.prepareStatement("UPDATE plan_resource SET plan_id = ? WHERE plan_id = ? ");
                ps9.setLong(1, curPlanId);
                ps9.setLong(2, newPlanId);
                ps9.executeUpdate();
                ps10 = con.prepareStatement("UPDATE plan_ivalue SET plan_id = ? WHERE plan_id = ? ");
                ps10.setLong(1, curPlanId);
                ps10.setLong(2, newPlanId);
                ps10.executeUpdate();
                ps11 = con.prepareStatement("UPDATE plan_value SET plan_id = ? WHERE plan_id = ? ");
                ps11.setLong(1, curPlanId);
                ps11.setLong(2, newPlanId);
                ps11.executeUpdate();
                ps12 = con.prepareStatement("UPDATE plans SET id = ?, description = ? WHERE id = ? ");
                ps12.setLong(1, curPlanId);
                ps12.setString(2, planName);
                ps12.setLong(3, newPlanId);
                ps12.executeUpdate();
                result = true;
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
                Session.closeStatement(ps9);
                Session.closeStatement(ps10);
                Session.closeStatement(ps11);
                Session.closeStatement(ps12);
                Session.commitTransConnection(con);
            } catch (Exception e) {
                this.logger.outMessage("\tFailed to change plan ids of the old and new VPS plans. Changes will be rolled back.\n", e);
                con.rollback();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
                Session.closeStatement(ps9);
                Session.closeStatement(ps10);
                Session.closeStatement(ps11);
                Session.closeStatement(ps12);
                Session.commitTransConnection(con);
            }
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            Session.closeStatement(ps7);
            Session.closeStatement(ps8);
            Session.closeStatement(ps9);
            Session.closeStatement(ps10);
            Session.closeStatement(ps11);
            Session.closeStatement(ps12);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    private void processAccount(long vpsResId, long dnsZoneId, String vpsHostName, long accId) throws Exception {
        Account curAcc = (Account) Account.get(new ResourceId(accId, 0));
        if (curAcc == null) {
            this.logger.outMessage("\tFailed to get account " + accId);
            return;
        }
        User u = curAcc.getUser();
        Session.setUser(u);
        Resource vpsRes = Resource.get(new ResourceId(vpsResId, 7000));
        List l = new ArrayList();
        l.add(vpsHostName);
        this.logger.outMessage("\tAdding domain...");
        double credit = curAcc.getBill().getCustomCredit();
        double ballance = curAcc.getBill().getBalance();
        if (ballance < 0.0d) {
            try {
                curAcc.getBill().setCredit(((-1.0d) * ballance) + 1.0d);
            } catch (Throwable th) {
                if (ballance < 0.0d) {
                    curAcc.getBill().setCredit(credit);
                }
                throw th;
            }
        }
        ResourceId domainResId = vpsRes.addChild("domain", "hs243", l);
        if (ballance < 0.0d) {
            curAcc.getBill().setCredit(credit);
        }
        this.logger.outMessage("\tDone\n");
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        this.logger.outMessage("\tUpdating dns_zone record...");
        try {
            try {
                ps1 = con.prepareStatement("UPDATE parent_child SET parent_id=?, parent_type=? WHERE child_type=3001 AND child_id=? AND account_id=?");
                ps1.setLong(1, domainResId.getId());
                ps1.setInt(2, domainResId.getType());
                ps1.setLong(3, dnsZoneId);
                ps1.setLong(4, accId);
                ps1.executeUpdate();
                this.logger.outMessage("\tDone\n");
                Session.closeStatement(ps1);
                con.close();
            } catch (Throwable th2) {
                Session.closeStatement(ps1);
                con.close();
                throw th2;
            }
        } catch (SQLException e) {
            this.logger.outMessage("\n\tFailed to update dns_zone record ", e);
            Session.closeStatement(ps1);
            con.close();
        }
    }
}
