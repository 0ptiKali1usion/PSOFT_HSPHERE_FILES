package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.calc.Calc;
import psoft.hsphere.global.Globals;
import psoft.hsphere.global.ResourceDependences;
import psoft.hsphere.plan.InitModifier;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.plan.wizard.PlanWizardXML;
import psoft.hsphere.promotion.Promo;
import psoft.hsphere.resource.admin.BillingManager;
import psoft.hsphere.resource.admin.SiteToolboxManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.LingualScalar;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/Plan.class */
public class Plan implements TemplateHashModel, TemplateMethodModel, Comparable {

    /* renamed from: id */
    protected int f36id;
    protected String description;
    protected boolean disabled;
    protected int billing;
    protected int cinfo;
    protected int groupId;
    protected boolean deleted;
    protected String groupName;
    protected HashMap resources;
    protected HashMap values;
    protected long resellerId;
    protected static HashMap menus;
    protected List promotions;
    protected Set access;
    public static final String HOST_PLATFORM_PREFIX = "allow_";
    public static final String CP_SSL_PREFIX = "cp_ssl_";
    public static final String LS_GROUP_PREFIX = "ls_group_";
    public static final String RESELLER_DNS_IPS_PARAM = "reseller_dns_ips";
    public static HashMap resellerMap = new HashMap();
    protected static HashMap showableResourcesMap = new HashMap();
    private static final String[] PR_TYPES = {"_FREE_UNITS_", "_SETUP_PRICE_", "_UNIT_PRICE_", "_USAGE_PRICE_", "_MAX"};
    public static final boolean isWebshell3Used = "3".equals(Session.getPropertyString("WEBSHELL"));
    public static final boolean isWebshell4Used = "4".equals(Session.getPropertyString("WEBSHELL"));
    protected static final TemplateString TS_1 = new TemplateString("1");
    protected static final TemplateString TS_0 = new TemplateString("0");

    /* renamed from: OK */
    protected static final TemplateString f37OK = new TemplateString("OK");

    static {
        Session.getLog().debug("Going to load menu");
        buildMenu();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Plan$Filter.class */
    public class Filter {
        private String wizardName;

        /* loaded from: hsphere.zip:psoft/hsphere/Plan$Filter$NodeListImplementation.class */
        public class NodeListImplementation extends ArrayList implements NodeList {
            private NodeListImplementation() {
                Filter.this = r4;
            }

            @Override // org.w3c.dom.NodeList
            public Node item(int index) {
                return (Node) get(index);
            }

            @Override // org.w3c.dom.NodeList
            public int getLength() {
                return super.size();
            }
        }

        private boolean isNodeDescriptionEqualsTo(Node category, String to) {
            try {
                return category.getAttributes().getNamedItem("description").getNodeValue().equals(to);
            } catch (NullPointerException e) {
                return false;
            }
        }

        private boolean isShowable(Node node) {
            try {
                if (node.getNodeName().equals("resource")) {
                    if (node.getAttributes().getNamedItem("showable").getNodeValue().equals("1")) {
                        return true;
                    }
                }
                return false;
            } catch (NullPointerException e) {
                return false;
            }
        }

        private String obtainName(Node node) {
            try {
                return node.getAttributes().getNamedItem("name").getNodeValue();
            } catch (NullPointerException e) {
                Session.getLog().error("Resource in xml file does not contain the name", e);
                return null;
            }
        }

        private String obtainDescription(Node node) {
            String resourceName = obtainName(node);
            return new String("common_signup_begin." + resourceName);
        }

        private void getResourcesFromCategory(Node node, NodeListImplementation resultList) {
            if (node.hasChildNodes()) {
                NodeList nl = node.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeName().equals("resource")) {
                        resultList.add(nl.item(i));
                    } else {
                        getResourcesFromCategory(nl.item(i), resultList);
                    }
                }
            }
        }

        private void addShowableResource(HashMap showable, TemplateList reciever) {
            List<String> fullResourceSet = TypeRegistry.getTypes();
            for (String resourceName : fullResourceSet) {
                try {
                    if (showable.containsKey(resourceName) && !Plan.this.isResourceAvailable(resourceName).isDisabled() && Plan.this.isResourceAvailable(resourceName).isResourceShowable()) {
                        TemplateMap intermediateMap = new TemplateMap();
                        intermediateMap.put("resource", new TemplateString(resourceName));
                        LingualScalar lingualScalar = new HSLingualScalar().get((String) showable.get(resourceName));
                        if (lingualScalar.getAsString().equals(showable.get(resourceName))) {
                            intermediateMap.put("description", new TemplateString(resourceName));
                            Session.getLog().error("Resource \"" + resourceName + "\" doesn't have description");
                        } else {
                            intermediateMap.put("description", lingualScalar);
                        }
                        reciever.add((TemplateModel) intermediateMap);
                    }
                } catch (NullPointerException e) {
                }
            }
        }

        private HashMap obtainShowableResources(String wizardName) throws Exception {
            Document particularWizard = PlanWizardXML.getWizardXML(wizardName);
            HashMap resultHash = new HashMap();
            NodeList categories = particularWizard.getElementsByTagName("category");
            for (int i = 0; i < categories.getLength(); i++) {
                Node category = categories.item(i);
                if (isNodeDescriptionEqualsTo(category, "planeditor.web_services") || isNodeDescriptionEqualsTo(category, "label.statistics") || isNodeDescriptionEqualsTo(category, "planeditor.ecommerce")) {
                    if (!resultHash.containsKey("hosting")) {
                        resultHash.put("hosting", new HashMap());
                    }
                    NodeListImplementation childNodes = new NodeListImplementation();
                    getResourcesFromCategory(category, childNodes);
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node currentNode = childNodes.item(j);
                        if (isShowable(currentNode)) {
                            ((HashMap) resultHash.get("hosting")).put(obtainName(currentNode), obtainDescription(currentNode));
                        }
                    }
                } else if (isNodeDescriptionEqualsTo(category, "planeditor.mail_services")) {
                    if (!resultHash.containsKey("mail")) {
                        resultHash.put("mail", new HashMap());
                    }
                    NodeListImplementation childNodes2 = new NodeListImplementation();
                    getResourcesFromCategory(category, childNodes2);
                    for (int j2 = 0; j2 < childNodes2.getLength(); j2++) {
                        Node currentNode2 = childNodes2.item(j2);
                        if (isShowable(currentNode2)) {
                            ((HashMap) resultHash.get("mail")).put(obtainName(currentNode2), obtainDescription(currentNode2));
                        }
                    }
                } else {
                    if (!resultHash.containsKey("other")) {
                        resultHash.put("other", new HashMap());
                    }
                    NodeListImplementation childNodes3 = new NodeListImplementation();
                    getResourcesFromCategory(category, childNodes3);
                    for (int j3 = 0; j3 < childNodes3.getLength(); j3++) {
                        Node currentNode3 = childNodes3.item(j3);
                        if (isShowable(currentNode3)) {
                            ((HashMap) resultHash.get("other")).put(obtainName(currentNode3), obtainDescription(currentNode3));
                        }
                    }
                }
            }
            return resultHash;
        }

        public Filter() throws Exception {
            Plan.this = r6;
            this.wizardName = null;
            this.wizardName = r6.getValue("_CREATED_BY_");
            try {
                Integer.parseInt(this.wizardName);
            } catch (Exception e) {
                if (this.wizardName != null && !Plan.showableResourcesMap.containsKey(this.wizardName)) {
                    Plan.showableResourcesMap.put(this.wizardName, obtainShowableResources(this.wizardName));
                }
            }
        }

        public TemplateModel getCompleteListOfShowableResourceTypes() throws Exception {
            this.wizardName = Plan.this.getValue("_CREATED_BY_");
            try {
                Integer.parseInt(this.wizardName);
                return new TemplateList();
            } catch (Exception e) {
                if (this.wizardName == null) {
                    return new TemplateList();
                }
                if (!Plan.showableResourcesMap.containsKey(this.wizardName)) {
                    Plan.showableResourcesMap.put(this.wizardName, obtainShowableResources(this.wizardName));
                }
                TemplateList resultList = new TemplateList();
                HashMap partOfMap = (HashMap) Plan.showableResourcesMap.get(this.wizardName);
                LinkedList resourceNames = new LinkedList();
                if (partOfMap.containsKey("hosting")) {
                    resourceNames.addAll(((HashMap) partOfMap.get("hosting")).keySet());
                }
                if (partOfMap.containsKey("mail")) {
                    resourceNames.addAll(((HashMap) partOfMap.get("mail")).keySet());
                }
                if (partOfMap.containsKey("other")) {
                    resourceNames.addAll(((HashMap) partOfMap.get("other")).keySet());
                }
                if (!resourceNames.isEmpty()) {
                    Iterator i = resourceNames.iterator();
                    while (i.hasNext()) {
                        String resourceTypeName = (String) i.next();
                        ResourceType resourceType = Plan.this.isResourceAvailable(resourceTypeName);
                        if (resourceType != null) {
                            resultList.add((TemplateModel) resourceType);
                        }
                    }
                }
                return resultList;
            }
        }

        public TemplateModel getShowableDomainResouces() throws Exception {
            TemplateList showableDomainResources = new TemplateList();
            if (Plan.this.isResourceAvailable("domain") != null && !Plan.this.isResourceAvailable("domain").isDisabled()) {
                TemplateMap intermediateMap = new TemplateMap();
                if (Plan.this.isResourceAvailable("opensrs") != null && !Plan.this.isResourceAvailable("opensrs").isDisabled()) {
                    intermediateMap.put("resource", "opensrs");
                    intermediateMap.put("description", new HSLingualScalar().get("common_signup_begin_opensrs"));
                    showableDomainResources.add((TemplateModel) intermediateMap);
                } else {
                    intermediateMap.put("resource", "domain");
                    intermediateMap.put("description", new HSLingualScalar().get("common_signup_begin_withoutopensrs"));
                    showableDomainResources.add((TemplateModel) intermediateMap);
                }
            }
            return showableDomainResources;
        }

        public TemplateModel getShowableMailServiceResources() throws Exception {
            TemplateList showableMailServiceResources = new TemplateList();
            if (Plan.this.isResourceAvailable("mailbox") != null && !Plan.this.isResourceAvailable("mailbox").isDisabled()) {
                TemplateMap intermediateMap = new TemplateMap();
                intermediateMap.put("resource", "mailbox");
                intermediateMap.put("description", new HSLingualScalar().get("common_signup_begin_mailserver"));
                showableMailServiceResources.add((TemplateModel) intermediateMap);
                TemplateMap intermediateMap2 = new TemplateMap();
                try {
                    String value = (String) Plan.this.isResourceAvailable("mailbox").getValues().get("_FREE_UNITS_");
                    HSLingualScalar lingualScalar = new HSLingualScalar().get("common_signup_begin_freemail");
                    if (value != null && Integer.parseInt(value) > 0) {
                        intermediateMap2.put("resource", "mailbox");
                        intermediateMap2.put("description", new TemplateString(MessageFormat.format(lingualScalar.getAsString(), value)));
                    }
                    showableMailServiceResources.add((TemplateModel) intermediateMap2);
                } catch (NullPointerException e) {
                }
            }
            addShowableResource((HashMap) ((HashMap) Plan.showableResourcesMap.get(this.wizardName)).get("mail"), showableMailServiceResources);
            return showableMailServiceResources;
        }

        public TemplateModel getShowableHostingResources() throws Exception {
            TemplateList showableHostingResources = new TemplateList();
            if (Plan.this.isResourceAvailable("hosting") != null && !Plan.this.isResourceAvailable("hosting").isDisabled()) {
                TemplateMap intermediateMap = new TemplateMap();
                intermediateMap.put("resource", "hosting");
                intermediateMap.put("description", new HSLingualScalar().get("common_signup_begin_hosting"));
                showableHostingResources.add((TemplateModel) intermediateMap);
            }
            addShowableResource((HashMap) ((HashMap) Plan.showableResourcesMap.get(this.wizardName)).get("hosting"), showableHostingResources);
            return showableHostingResources;
        }

        public TemplateModel getShowableRemainResources() throws Exception {
            TemplateList showableRemainResources = new TemplateList();
            addShowableResource((HashMap) ((HashMap) Plan.showableResourcesMap.get(this.wizardName)).get("other"), showableRemainResources);
            return showableRemainResources;
        }
    }

    public int getId() {
        return this.f36id;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public String getDescription() {
        if (getValue("_EMULATION_MODE") != null && "1".equals(getValue("_EMULATION_MODE"))) {
            return this.description + " (DEMO MODE)";
        }
        return this.description;
    }

    public TemplateModel FM_getShowableResources() throws Exception {
        Filter resourceFilter = new Filter();
        return resourceFilter.getCompleteListOfShowableResourceTypes();
    }

    public static Map getPlansMap() throws UnknownResellerException {
        return getPlansMap(Session.getResellerId());
    }

    protected static Map getPlansMap(long resellerId) throws UnknownResellerException {
        String reseller = Long.toString(resellerId);
        TreeMap map = null;
        Reseller res = Reseller.getReseller(resellerId);
        if (res != null) {
            synchronized (res) {
                map = (TreeMap) resellerMap.get(reseller);
                if (map == null) {
                    map = new TreeMap();
                    resellerMap.put(reseller, map);
                    loadAllPlans(resellerId);
                }
            }
        }
        return map;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        Plan pl = (Plan) o;
        if (getId() == pl.getId()) {
            return 0;
        }
        if (getDescription().equals(pl.getDescription())) {
            return getId() - pl.getId();
        }
        return getDescription().compareTo(pl.getDescription());
    }

    public boolean isAccessible(String id) {
        int pid = 0;
        try {
            pid = Integer.parseInt(id);
        } catch (Exception e) {
        }
        return isAccessible(pid);
    }

    public boolean isAccessible(int id) {
        return this.access.isEmpty() || this.access.contains(Integer.toString(id));
    }

    public ResourceType getResourceType(int id) {
        return getResourceType(Integer.toString(id));
    }

    public ResourceType getResourceType(String id) {
        return (ResourceType) this.resources.get(id);
    }

    public int getBilling() {
        return this.billing;
    }

    public boolean isWitoutBilling() {
        return this.billing == 0;
    }

    public int getCInfo() {
        return this.cinfo;
    }

    public double getCredit() {
        String creditValue = getValue("_HARD_CREDIT");
        if (creditValue != null) {
            try {
                if (!"".equals(creditValue)) {
                    return USFormat.parseDouble(creditValue);
                }
                return 0.0d;
            } catch (ParseException e) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public double getTrialCredit() {
        String trialValue = getValue("_TRIAL_CREDIT");
        if (trialValue != null) {
            try {
                return USFormat.parseDouble(trialValue);
            } catch (ParseException e) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public TemplateModel FM_accessChange(String str) throws Exception {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        Session.getLog().info("changing to ----------->" + str);
        Connection con = Session.getDb();
        try {
            ps1 = con.prepareStatement("INSERT INTO plan_access (id, a_id) VALUES (?, ?)");
            ps1.setInt(1, this.f36id);
            ps2 = con.prepareStatement("DELETE FROM plan_access WHERE id = ?");
            ps2.setInt(1, this.f36id);
            ps2.executeUpdate();
            synchronized (this.access) {
                this.access.clear();
            }
            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                String pid = st.nextToken();
                ps1.setInt(2, Integer.parseInt(pid));
                ps1.executeUpdate();
                synchronized (this.access) {
                    this.access.add(pid);
                }
            }
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_accessCheck(String id) {
        return new TemplateString(isAccessible(id));
    }

    public TemplateModel FM_getResourceType(String typeName) throws Exception {
        return getResourceType(TypeRegistry.getTypeId(typeName));
    }

    public ResourceType isResourceAvailable(String typeName) {
        try {
            ResourceType rt = getResourceType(TypeRegistry.getTypeId(typeName));
            if (rt.isDisabled()) {
                return null;
            }
            return rt;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isResourceTypeAvailable(String typeName) {
        try {
            ResourceType rt = getResourceType(TypeRegistry.getTypeId(typeName));
            if (rt != null) {
                if (!rt.isDisabled()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isObjectAvailable(String name) throws Exception {
        if (name == null || "".equals(name)) {
            return false;
        }
        if ("BILLING".equals(name)) {
            return this.billing != 0;
        } else if ("WITHOUT_BILLING".equals(name)) {
            return this.billing == 0;
        } else if ("MONEY_BACK".equals(name)) {
            String moneyBack = getValue("MONEY_BACK_CALC");
            return (moneyBack == null || "".equals(moneyBack)) ? false : true;
        } else if ("WEBSHELL3".equals(name)) {
            return isWebshell3Used;
        } else {
            if ("WEBSHELL4".equals(name)) {
                return isWebshell4Used;
            }
            if ("WEBSHELL".equals(name)) {
                return (isWebshell3Used || isWebshell4Used) ? false : true;
            } else if ("SITETOOLBOX".equals(name)) {
                try {
                    String toolBoxState = Settings.get().getValue("userstoolbox");
                    int isDisabled = Globals.isObjectDisabled("sitetoolboxmanager");
                    Session.getLog().debug("manager state: " + Globals.isObjectDisabled("sitetoolboxmanager"));
                    return "on".equals(toolBoxState) && isDisabled == 0;
                } catch (Exception ex) {
                    Session.getLog().debug("Can't get information about sitetoolboxmanager resource from reseller" + ex.getMessage());
                    return false;
                }
            } else if ("M_ADMIN".equals(name)) {
                return isResourceTypeAvailable(FMACLManager.ADMIN) && Session.getResellerId() == 1;
            } else if ("R_ADMIN".equals(name)) {
                return isResourceTypeAvailable(FMACLManager.ADMIN) && Session.getResellerId() != 1;
            } else if ("sitetoolboxmanager".equals(name)) {
                SiteToolboxManager sm = null;
                try {
                    sm = (SiteToolboxManager) Session.getAccount().FM_findChild("sitetoolboxmanager").get();
                } catch (Exception e) {
                }
                if (sm == null) {
                    return false;
                }
                return isResourceTypeAvailable(name);
            } else if ("MSEXCHANGE".equals(name)) {
                return Globals.isObjectDisabled("msexchange_platform") == 0;
            } else if (name.startsWith("!")) {
                return !isObjectAvailable(name.substring(1));
            } else if (TypeRegistry.isResourceName(name)) {
                return isResourceTypeAvailable(name);
            } else {
                return Globals.getAccessor().isGlobalObject(name) && Globals.isObjectDisabled(name) == 0;
            }
        }
    }

    public TemplateModel FM_isResourceAvailable(String typeName) throws Exception {
        if (isObjectAvailable(typeName)) {
            return new TemplateString("1");
        }
        return null;
    }

    public boolean areResourcesAvailable(String pattern) throws Exception {
        boolean res = false;
        StringTokenizer orGroups = new StringTokenizer(pattern, ";");
        while (orGroups.hasMoreTokens() && !res) {
            StringTokenizer andElements = new StringTokenizer(orGroups.nextToken(), ",");
            while (andElements.hasMoreTokens()) {
                res = isObjectAvailable(andElements.nextToken().trim());
                if (!res) {
                    break;
                }
            }
        }
        return res;
    }

    public TemplateModel FM_areResourcesAvailable(String pattern) throws Exception {
        if (areResourcesAvailable(pattern)) {
            return new TemplateString("1");
        }
        return null;
    }

    public TemplateModel FM_enable() throws Exception {
        if (this.deleted) {
            return this;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plans SET disabled=0 WHERE id = ?");
            ps.setInt(1, this.f36id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.disabled = false;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_disable() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plans SET disabled=1 WHERE id = ?");
            ps.setInt(1, this.f36id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.disabled = true;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_enableResource(String typeName) throws Exception {
        String typeId = TypeRegistry.getTypeId(typeName);
        getResourceType(typeId).enable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plan_resource SET disabled=0 WHERE plan_id = ? AND type_id = ?");
            ps.setInt(1, this.f36id);
            ps.setInt(2, Integer.parseInt(typeId));
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

    public TemplateModel FM_disableResource(String typeName) throws Exception {
        String typeId = TypeRegistry.getTypeId(typeName);
        getResourceType(typeId).disable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plan_resource SET disabled=1 WHERE plan_id = ? AND type_id = ?");
            ps.setInt(1, this.f36id);
            ps.setInt(2, Integer.parseInt(typeId));
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

    public TemplateModel FM_showResourceChange(String typeName) throws Exception {
        Filter filter = new Filter();
        LinkedList gotResourcesNames = new LinkedList();
        StringTokenizer stringTokenizer = new StringTokenizer(typeName, ",");
        while (stringTokenizer.hasMoreTokens()) {
            gotResourcesNames.add(stringTokenizer.nextToken());
        }
        TemplateList listOfResourceTypes = filter.getCompleteListOfShowableResourceTypes();
        listOfResourceTypes.rewind();
        while (listOfResourceTypes.hasNext()) {
            ResourceType showableResourceType = listOfResourceTypes.next();
            if (gotResourcesNames.contains(showableResourceType.getType())) {
                showableResourceType.makeShown();
            } else {
                showableResourceType.makeHidden();
            }
        }
        return this;
    }

    public TemplateModel FM_copy() throws Exception {
        Plan p = new Plan(this);
        for (Promo _promo : getPlanPromotions()) {
            p.addPromo(_promo.getId());
        }
        p.getPlanPromotions(true);
        return p;
    }

    public Plan() {
        this.promotions = null;
    }

    protected Plan(Plan p) throws Exception {
        this(p, " copy", p.resellerId);
    }

    public Plan(Plan p, String ending, long resellerId) throws Exception {
        this.promotions = null;
        this.description = p.description + ending;
        this.disabled = p.disabled;
        this.billing = p.billing;
        this.cinfo = p.cinfo;
        this.resellerId = resellerId;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT max(id) FROM plans");
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                this.f36id = rs.getInt(1) + 1;
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO plans (id, description, disabled, billing, cinfo, reseller_id, deleted) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps3.setInt(1, this.f36id);
            ps3.setString(2, this.description);
            ps3.setInt(3, this.disabled ? 1 : 0);
            ps3.setInt(4, this.billing);
            ps3.setInt(5, this.cinfo);
            ps3.setLong(6, resellerId);
            ps3.setInt(7, this.deleted ? 1 : 0);
            ps3.executeUpdate();
            this.access = new HashSet();
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("INSERT INTO plan_access (id, a_id) VALUES (?, ?)");
            ps4.setInt(1, this.f36id);
            for (Object aId : p.access) {
                ps4.setInt(2, Integer.parseInt((String) aId));
                ps4.executeUpdate();
                this.access.add(aId);
            }
            this.values = new HashMap();
            ps4.close();
            ps = con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            ps.setObject(1, new Integer(this.f36id));
            ps.setNull(3, 4);
            for (Object key : p.values.keySet()) {
                Object value = p.values.get(key);
                this.values.put(key, value);
                ps.setObject(2, key);
                ps.setObject(4, value);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
            this.resources = new HashMap();
            for (Object key2 : p.resources.keySet()) {
                this.resources.put(key2, ((ResourceType) p.resources.get(key2)).copy(this.f36id));
            }
            getPlansMap(resellerId).put(Integer.toString(this.f36id), this);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void addResourceType(String typeId, String className, int disabled, int showable) {
        this.resources.put(typeId, new ResourceType(this.f36id, Integer.parseInt(typeId), className, disabled, showable));
    }

    public Plan(int id, String description, int disabled, int billing, int cinfo, boolean deleted) throws UnknownResellerException {
        this(id, description, disabled, billing, cinfo, Session.getResellerId(), deleted);
    }

    public Plan(int id, String description, int disabled, int billing, int cinfo, long resellerId, boolean deleted) throws UnknownResellerException {
        this.promotions = null;
        this.f36id = id;
        this.description = description;
        this.disabled = disabled == 1;
        this.billing = billing;
        this.cinfo = cinfo;
        this.resellerId = resellerId;
        this.deleted = deleted;
        this.resources = new HashMap();
        this.values = new HashMap();
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT group_id, name FROM cmp_plan_group WHERE plan_id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                setGroup(rs.getInt(1));
                setGroupName(rs.getString(2));
            } else {
                setGroup(0);
                setGroupName(null);
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("SELECT type_id, class_name, disabled, showable FROM plan_resource WHERE plan_id = ?");
            ps2.setInt(1, id);
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                this.resources.put(rs2.getString(1), new ResourceType(id, rs2.getInt(1), rs2.getString(2), rs2.getInt(3), rs2.getInt(4)));
            }
            this.access = new HashSet();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT a_id FROM plan_access WHERE id = ?");
            ps3.setInt(1, id);
            ResultSet rs3 = ps3.executeQuery();
            while (rs3.next()) {
                this.access.add(rs3.getString(1));
            }
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT type_id, mod_id, disabled FROM plan_imod WHERE plan_id = ?");
            ps4.setInt(1, id);
            ResultSet rs4 = ps4.executeQuery();
            while (rs4.next()) {
                String mod_id = rs4.getString(2);
                if (null == mod_id) {
                    mod_id = "";
                }
                ResourceType resourceType = (ResourceType) this.resources.get(rs4.getString(1));
                getResourceType(rs4.getString(1)).addInitModifier(mod_id, rs4.getInt(3));
            }
            ps4.close();
            PreparedStatement ps5 = con.prepareStatement("SELECT type_id,mod_id,new_type_id,new_mod_id,disabled FROM plan_iresource WHERE plan_id = ? ORDER BY order_id");
            ps5.setInt(1, id);
            ResultSet rs5 = ps5.executeQuery();
            while (rs5.next()) {
                String mod_id2 = rs5.getString(2);
                try {
                    getResourceType(rs5.getString(1)).getInitModifier(null == mod_id2 ? "" : mod_id2).addInitResource(rs5.getInt(3), rs5.getString(4) == null ? "" : rs5.getString(4), rs5.getInt(5));
                } catch (NullPointerException npe) {
                    Session.getLog().info("--------->" + id + ":" + rs5.getString(1) + ":" + rs5.getString(2) + ":" + rs5.getString(3) + ":" + rs5.getString(4) + ":" + rs5.getString(5), npe);
                }
            }
            ps5.close();
            PreparedStatement ps6 = con.prepareStatement("SELECT type_id, mod_id, value,type, label FROM plan_ivalue WHERE plan_id = ? ORDER BY order_id");
            ps6.setInt(1, id);
            ResultSet rs6 = ps6.executeQuery();
            while (rs6.next()) {
                String mod_id3 = rs6.getString(2);
                if (null == mod_id3) {
                    mod_id3 = "";
                }
                getResourceType(rs6.getString(1)).getInitModifier(mod_id3).addInitValue(new InitValue(rs6.getString(3), rs6.getInt(4), rs6.getString(5)));
            }
            ps6.close();
            PreparedStatement ps7 = con.prepareStatement("SELECT type_id, name, value FROM plan_value WHERE plan_id = ?");
            ps7.setInt(1, id);
            ResultSet rs7 = ps7.executeQuery();
            while (rs7.next()) {
                String tmp = rs7.getString(1);
                if (tmp == null) {
                    this.values.put(rs7.getString(2), rs7.getString(3));
                } else {
                    getResourceType(tmp).putValue(rs7.getString(2), rs7.getString(3));
                }
            }
            Session.closeStatement(ps7);
            con.close();
            getPlansMap(resellerId).put(Integer.toString(id), this);
        } catch (SQLException se) {
            Session.getLog().error("Plan Init " + id, se);
            throw new Error("SQL Exception");
        }
    }

    public String getValue(int type, String key) {
        return getValue(Integer.toString(type), key);
    }

    public String getValue(String type, String key) {
        String val = getResourceType(type).getValue(key);
        if (val == null) {
            val = getValue(key);
        }
        return val;
    }

    public String getValue(String key) {
        return (String) this.values.get(key);
    }

    public String getResourceValue(int type, String key) {
        return getResourceType(type)._getValue(key);
    }

    protected InitModifier getEnabledInitModifier(int type, String mod) throws Exception {
        Session.getLog().debug("Plan getEnabledInitModifier(" + type + ", " + mod + ")");
        ResourceType rt = getResourceType(type);
        if (rt == null) {
            throw new HSUserException("plan.resourcedisabled");
        }
        if (rt.isDisabled()) {
            try {
                throw new HSUserException("plan.named_res_disabled", new Object[]{TypeRegistry.getDescription(type)});
            } catch (NoSuchTypeException e) {
                throw new HSUserException("plan.typedisabled", new Object[]{String.valueOf(type)});
            }
        }
        InitModifier im = rt.getInitModifier(mod);
        if (im != null && im.isDisabled()) {
            throw new HSUserException("plan.resourcedisabled");
        }
        if (im == null) {
            if ("".equals(mod)) {
                Session.getLog().debug("Plan getEnabledInitModifier returns null");
                return null;
            }
            Session.getLog().debug("Unable to get mod:" + mod);
            im = rt.getInitModifier("");
        }
        return im;
    }

    public List getInitResources(int type, String mod) throws Exception {
        InitModifier im = getEnabledInitModifier(type, mod);
        return im == null ? new ArrayList() : im.getInitResources();
    }

    public TemplateList FM_getInitResources(int type, String mod) throws Exception {
        return new TemplateList(getInitResources(type, mod));
    }

    public List getDefaultInitValues(Resource r, int type, String mod) throws Exception {
        InitModifier im = getEnabledInitModifier(type, mod);
        Session.getLog().info("type -->" + type + " mod " + mod + " im=" + im);
        return im == null ? new ArrayList() : im.getDefaultInitValues(r, type);
    }

    public List getDefaultInitValues(InitToken token, int type, String mod) throws Exception {
        InitModifier im = getEnabledInitModifier(type, mod);
        Session.getLog().info("type -->" + type + " mod " + mod + " im=" + im);
        return im == null ? new ArrayList() : im.getDefaultInitValues(token, type);
    }

    public Class getResourceClassByName(String type) throws Exception {
        return getResourceClass(TypeRegistry.getTypeId(type));
    }

    public Class getResourceClass(int type) throws Exception {
        return getResourceClass(Integer.toString(type));
    }

    public Class getResourceClass(String type) throws Exception {
        Session.getLog().debug(" Plan.getResourceClass ( " + type + " ) ");
        ResourceType rt = getResourceType(type);
        if (rt == null) {
            throw new HSUserException("plan.typedisabled", new Object[]{TypeRegistry.getDescription(type)});
        }
        return rt.getResourceClass();
    }

    public TemplateModel exec(List l) {
        try {
            List l2 = HTMLEncoder.decode(l);
            if (l2.size() > 1) {
                return new TemplateString(getValue(TypeRegistry.getTypeId((String) l2.get(0)), (String) l2.get(1)));
            }
            return new TemplateString(getValue((String) l2.get(0)));
        } catch (Exception e) {
            Session.getLog().warn("Plan exec", e);
            return null;
        }
    }

    public TemplateModel FM_setDescription(String desc) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plans SET description = ? WHERE id = ?");
            ps.setString(1, desc);
            ps.setInt(2, this.f36id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.description = desc;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getPeriodAsString(int pId) throws Exception {
        return getValue("_PERIOD_SIZE_" + pId) + " " + getValue("_PERIOD_TYPE_" + pId);
    }

    public TemplateModel FM_getPeriodAsString(int pId) throws Exception {
        return new TemplateString(getPeriodAsString(pId));
    }

    public Date getNextPaymentDate(Date start, int periodId) {
        String periodType;
        int periodSize;
        int periodTypeId;
        try {
            periodType = getValue("_PERIOD_TYPE_" + periodId).toUpperCase();
        } catch (NullPointerException e) {
            periodType = "MONTH";
        }
        try {
            periodSize = Integer.parseInt(getValue("_PERIOD_SIZE_" + periodId));
        } catch (NullPointerException e2) {
            periodSize = 1;
        } catch (NumberFormatException e3) {
            periodSize = 1;
        }
        if (periodType.equals("DAY")) {
            periodTypeId = 5;
        } else if (periodType.equals("WEEK")) {
            periodTypeId = 3;
        } else if (periodType.equals("MONTH")) {
            periodTypeId = 2;
        } else if (periodType.equals("YEAR")) {
            periodTypeId = 1;
        } else {
            periodTypeId = 2;
        }
        Calendar next = TimeUtils.getCalendar(start);
        next.add(periodTypeId, periodSize);
        return TimeUtils.dropMinutes(next.getTime());
    }

    public TemplateModel FM_putValue(String key, String value) throws Exception {
        putValue(key, value);
        return this;
    }

    public void putValue(String key, String value) throws Exception {
        if (value.equals(this.values.get(key))) {
            return;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id is NULL and name = ?");
            ps.setInt(1, this.f36id);
            ps.setString(2, key);
            ps.executeUpdate();
            if (value.length() > 0) {
                ps.close();
                ps = con.prepareStatement("INSERT INTO plan_value (plan_id, type_id, name, value) VALUES (?, null, ?, ?)");
                ps.setInt(1, this.f36id);
                ps.setString(2, key);
                ps.setString(3, value);
                ps.executeUpdate();
                this.values.put(key, value);
            } else {
                this.values.remove(key);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void setGroup(int groupId) {
        this.groupId = groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroup() {
        return this.groupId;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public static TemplateList getGroupedPlanTree(List planList) {
        Hashtable groups = new Hashtable();
        Iterator i = planList.iterator();
        while (i.hasNext()) {
            Plan pl = (Plan) i.next();
            Hashtable group = (Hashtable) groups.get(new Integer(pl.getGroup()));
            if (group == null) {
                group = new Hashtable();
                group.put("id", Integer.toString(pl.getGroup()));
                if (pl.getGroupName() != null) {
                    group.put("name", pl.getGroupName());
                } else {
                    group.put("name", "");
                }
                group.put("plans", new TreeSet());
                groups.put(new Integer(pl.getGroup()), group);
            }
            ((SortedSet) group.get("plans")).add(pl);
        }
        TreeSet sortedGroup = new TreeSet(new Comparator() { // from class: psoft.hsphere.Plan.1
            @Override // java.util.Comparator
            public int compare(Object o1, Object o2) {
                int num1;
                int num2;
                Hashtable hs1 = (Hashtable) o1;
                Hashtable hs2 = (Hashtable) o2;
                if (hs1 == hs2) {
                    return 0;
                }
                String str1 = hs1.get("name").toString();
                String str2 = hs2.get("name").toString();
                if (!"".equals(str1) || "".equals(str2)) {
                    if (!"".equals(str2) || "".equals(str1)) {
                        try {
                            num1 = Integer.parseInt(hs1.get("id").toString());
                        } catch (Exception e) {
                            num1 = 0;
                        }
                        try {
                            num2 = Integer.parseInt(hs2.get("id").toString());
                        } catch (Exception e2) {
                            num2 = 0;
                        }
                        int res = str1.compareTo(str2);
                        if (res == 0) {
                            return num1 - num2;
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
        sortedGroup.addAll(groups.values());
        return new TemplateList(sortedGroup);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("status")) {
            return f37OK;
        }
        if (key.equals("id")) {
            return new TemplateString(this.f36id);
        }
        if (key.equals("description")) {
            return new TemplateString(getDescription());
        }
        if (key.equals("values")) {
            return new TemplateMap(this.values);
        }
        if (key.equals("valueKeys")) {
            return new TemplateList(this.values.keySet());
        }
        if (key.equals("access")) {
            return new TemplateList(this.access);
        }
        if (key.equals("b_info")) {
            return new TemplateString(this.billing);
        }
        if (key.equals("c_info")) {
            return new TemplateString(this.cinfo);
        }
        if (key.equals("deleted")) {
            return new TemplateString(isDeleted());
        }
        if (key.equals("reseller_id")) {
            return new TemplateString(getResellerId());
        }
        if (key.equals("disabled")) {
            return new TemplateString(this.disabled);
        }
        if (key.equals("resourceMap")) {
            return new MapAdapter(this.resources);
        }
        if (key.equals("isPromocodeApplicable")) {
            return new TemplateString(isPromoCodeApplicable());
        }
        if (key.equals("assigned_promos")) {
            return new TemplateList(getPlanPromotions());
        }
        if (key.equals("available_promos")) {
            try {
                return new TemplateList(getAssignablePromotions());
            } catch (Exception e) {
                Session.getLog().error("Error getting assignable promotions ", e);
                return null;
            }
        } else if (key.equals("signuped_users")) {
            try {
                return new TemplateString(qntySignupedUsers());
            } catch (Exception e2) {
                return null;
            }
        } else if (key.equals("resources")) {
            return new TemplateList(new TreeSet(this.resources.values()));
        } else {
            if (key.equals("type")) {
                return new TemplateString(getValue("_CREATED_BY_"));
            }
            if (key.equals("is_reseller_plan")) {
                if (isResellerPlan()) {
                    return new TemplateString(1);
                }
                return null;
            }
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public Set getValueKeys() {
        return this.values.keySet();
    }

    public TemplateModel FM_setCInfo(int cinfo) throws Exception {
        if (isResourceAvailable(FMACLManager.ADMIN) != null && cinfo > 0) {
            throw new HSUserException("lan.settings.enableciinfo");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plans SET cinfo = ? WHERE id = ?");
            ps.setInt(1, cinfo);
            ps.setInt(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.cinfo = cinfo;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setBInfo(int binfo) throws Exception {
        if (isResourceAvailable(FMACLManager.ADMIN) != null && binfo > 0) {
            throw new HSUserException("plan.settings.enablebilling");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plans SET billing = ? WHERE id = ?");
            ps.setInt(1, binfo);
            ps.setInt(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.billing = binfo;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized void loadAllPlans() {
        resellerMap = new HashMap();
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, reseller_id FROM plans");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    load(rs.getInt(1), rs.getLong(2));
                } catch (Exception e) {
                    Session.getLog().error("Skip plan #" + rs.getInt(1), e);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException e2) {
            Session.getLog().debug("Problem loading plans", e2);
        }
    }

    public static synchronized void loadAllPlans(long resellerId) {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM plans WHERE reseller_id = ?");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    load(rs.getInt(1), resellerId);
                    Session.getLog().debug("Plan #" + rs.getInt(1) + " reseller #" + resellerId);
                } catch (Exception e) {
                    Session.getLog().error("Skip plan #" + rs.getInt(1), e);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException e2) {
            Session.getLog().debug("Problem loading plans", e2);
        }
    }

    public static Plan load(int id) throws Exception {
        return load(id, Session.getResellerId());
    }

    public static Plan load(int id, long reseller_id) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, description, disabled, billing, cinfo, deleted FROM plans WHERE id = ? and reseller_id = ?");
            ps.setInt(1, id);
            ps.setLong(2, reseller_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Plan plan = new Plan(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), reseller_id, rs.getInt(6) == 1);
                Session.closeStatement(ps);
                con.close();
                return plan;
            }
            throw new Exception("No Plan With ID:" + id);
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static Collection getPlanList() {
        return getPlanList(false);
    }

    public static Collection getPlanList(boolean isSorted) {
        try {
            if (!isSorted) {
                return getPlansMap().values();
            }
            TreeMap plans = new TreeMap(new Comparator() { // from class: psoft.hsphere.Plan.2
                @Override // java.util.Comparator
                public int compare(Object o1, Object o2) {
                    try {
                        Plan plan1 = Plan.getPlan(Integer.parseInt((String) o1));
                        Plan plan2 = Plan.getPlan(Integer.parseInt((String) o2));
                        if (!plan1.getDescription().equals(plan2.getDescription())) {
                            return plan1.getDescription().compareTo(plan2.getDescription());
                        }
                        return new Integer(plan1.getId()).compareTo(new Integer(plan2.getId()));
                    } catch (Exception e) {
                        return -1;
                    }
                }
            });
            plans.putAll(getPlansMap());
            return plans.values();
        } catch (UnknownResellerException e) {
            Session.getLog().error("Unknown reseller ID");
            return null;
        }
    }

    public static synchronized void buildMenu() {
        menus = MenuBuilder.getAllMenus("MENU_CONFIG");
    }

    public static MenuItemsHolder getPlanMenu(String menuId) {
        return (MenuItemsHolder) menus.get(menuId);
    }

    public static TemplateModel FM_getPlanMenu(String menuId) {
        return getPlanMenu(menuId);
    }

    public static List getAccessiblePlanList(int id) throws UnknownResellerException {
        List availablePlans = new ArrayList();
        for (Plan pl : getPlanList()) {
            if (pl.isAccessible(id) && !pl.isDisabled()) {
                Session.getLog().debug("getAccessiblePlanList " + pl.getId() + " access" + pl.access);
                availablePlans.add(pl);
            }
        }
        return availablePlans;
    }

    public TemplateModel FM_getAccessiblePlanList(int Id) throws UnknownResellerException {
        return getGroupedPlanTree(getAccessiblePlanList(Id));
    }

    public static Plan getPlan(int id) throws UnknownResellerException {
        return getPlan(Integer.toString(id));
    }

    public static Plan getPlan(String id) throws UnknownResellerException {
        return (Plan) getPlansMap().get(id);
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public static TreeMap getPlans() {
        try {
            return (TreeMap) getPlansMap();
        } catch (UnknownResellerException ure) {
            Session.getLog().error("Unknown reseller ID", ure);
            return null;
        }
    }

    private void realDelete() throws Exception {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        PreparedStatement ps5 = null;
        PreparedStatement ps6 = null;
        PreparedStatement ps7 = null;
        PreparedStatement ps8 = null;
        boolean wasTrans = Session.isTransConnection();
        Connection con = wasTrans ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                ps1 = con.prepareStatement("DELETE FROM plans WHERE id = ?");
                ps1.setInt(1, getId());
                ps2 = con.prepareStatement("DELETE FROM plan_resource WHERE plan_id = ?");
                ps2.setInt(1, getId());
                ps3 = con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ?");
                ps3.setInt(1, getId());
                ps4 = con.prepareStatement("DELETE FROM plan_imod WHERE plan_id = ?");
                ps4.setInt(1, getId());
                ps5 = con.prepareStatement("DELETE FROM plan_iresource WHERE plan_id = ?");
                ps5.setInt(1, getId());
                ps6 = con.prepareStatement("DELETE FROM plan_ivalue WHERE plan_id = ?");
                ps6.setInt(1, getId());
                ps7 = con.prepareStatement("DELETE FROM plan_access WHERE id = ?");
                ps7.setInt(1, getId());
                ps8 = con.prepareStatement("DELETE FROM cmp_plan_group WHERE plan_id = ?");
                ps8.setInt(1, getId());
                ps8.executeUpdate();
                ps7.executeUpdate();
                ps6.executeUpdate();
                ps5.executeUpdate();
                ps4.executeUpdate();
                ps3.executeUpdate();
                ps2.executeUpdate();
                ps1.executeUpdate();
                con.commit();
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
                if (wasTrans) {
                    con.close();
                } else {
                    Session.commitTransConnection(con);
                }
            } catch (SQLException ex) {
                Session.getLog().error("Failed to delete plan " + this.description, ex);
                if (con != null) {
                    con.rollback();
                }
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                Session.closeStatement(ps4);
                Session.closeStatement(ps5);
                Session.closeStatement(ps6);
                Session.closeStatement(ps7);
                Session.closeStatement(ps8);
                if (wasTrans) {
                    con.close();
                } else {
                    Session.commitTransConnection(con);
                }
            }
            getPlansMap().remove(Integer.toString(getId()));
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            Session.closeStatement(ps7);
            Session.closeStatement(ps8);
            if (wasTrans) {
                con.close();
            } else {
                Session.commitTransConnection(con);
            }
            throw th;
        }
    }

    public void softDelete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("UPDATE plans SET disabled= 1 ,deleted = 1 WHERE id = ?");
            ps2.setInt(1, this.f36id);
            ps2.executeUpdate();
            this.disabled = true;
            this.deleted = true;
            ps = con.prepareStatement("DELETE FROM cmp_plan_group WHERE plan_id = ?");
            ps.setInt(1, this.f36id);
            ps.executeUpdate();
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
            ps = con.prepareStatement("SELECT count(*) as quantity FROM accounts WHERE plan_id = ? AND (deleted IS NOT NULL OR (deleted IS NOT NULL AND failed = 0))");
            ps.setInt(1, this.f36id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                softDelete();
            } else {
                realDelete();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public int qntySignupedUsers() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM accounts WHERE deleted IS NULL AND plan_id = ?");
            ps2.setInt(1, this.f36id);
            ResultSet rs = ps2.executeQuery();
            rs.next();
            int qty = 0 + rs.getInt(1);
            ps = con.prepareStatement("SELECT count(*) FROM request_record WHERE deleted IS NULL AND plan_id = ?");
            ps.setInt(1, getId());
            ResultSet rs2 = ps.executeQuery();
            rs2.next();
            int qty2 = qty + rs2.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return qty2;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public static int getPlanIdByName(String planName) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM plans WHERE description = ? AND deleted = 0 AND reseller_id = ?");
            ps.setString(1, planName);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int i = rs.getInt("id");
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

    public HashMap getResources() {
        return this.resources;
    }

    public String getPeriodInWords(int pId) {
        String key = "";
        try {
            key = "bill.recurrent." + getValue("_PERIOD_SIZE_" + pId) + "_" + getValue("_PERIOD_TYPE_" + pId);
            String result = Localizer.translateMessage(key, new Object[]{null});
            if (!result.equals(key)) {
                return result;
            }
        } catch (Exception e) {
            Session.getLog().warn("GET_PERIOD_IN_WORDS:Error getting: " + key, e);
        }
        return Localizer.translateMessage("bill.recurrent.generic", new Object[]{null});
    }

    public String getMonthPeriodInWords() {
        String key = "";
        try {
            key = "bill.recurrent.1_MONTH";
            String result = Localizer.translateMessage(key, new Object[]{null});
            if (!result.equals(key)) {
                return result;
            }
        } catch (Exception e) {
            Session.getLog().warn("GET_PERIOD_IN_WORDS:Error getting: " + key, e);
        }
        return Localizer.translateMessage("bill.recurrent.generic", new Object[]{null});
    }

    public boolean isTotalyEqual(Plan p) {
        if (this.billing != p.getBilling() || this.cinfo != p.getCInfo() || !this.description.equals(p.getDescription()) || !getValue("_CREATED_BY_").equals(p.getValue("_CREATED_BY_"))) {
            return false;
        }
        for (String key : this.resources.keySet()) {
            ResourceType rt = getResourceType(key);
            if (!rt.compatible(p.getResourceType(key))) {
                return false;
            }
        }
        if (!getValue("_PERIOD_TYPES").equals(p.getValue("_PERIOD_TYPES"))) {
            return false;
        }
        for (int i = 0; i < Integer.parseInt(getValue("_PERIOD_TYPES")); i++) {
            if (!getValue("_PERIOD_TYPE_" + i).equals(p.getValue("_PERIOD_TYPE_" + i)) || !getValue("_PERIOD_SIZE_" + i).equals(p.getValue("_PERIOD_SIZE_" + i))) {
                return false;
            }
        }
        if (!checkPrices(p, "")) {
            return false;
        }
        for (int i2 = 0; i2 < Integer.parseInt(getValue("_PERIOD_TYPES")); i2++) {
            if (!checkPrices(p, "" + i2)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPrices(Plan p, String bpId) {
        for (String key : this.resources.keySet()) {
            getResourceType(key);
            for (int a = 0; a < PR_TYPES.length; a++) {
                String val1 = getValue(key, PR_TYPES[a] + bpId);
                String val2 = p.getValue(key, PR_TYPES[a] + bpId);
                if (!compareValues(val1, val2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean compareValues(String val1, String val2) {
        try {
            Float v1 = new Float(val1 == null ? "0.0" : USFormat.parseString(val1));
            Float v2 = new Float(val2 == null ? "0.0" : USFormat.parseString(val2));
            return v1.equals(v2);
        } catch (Exception ex) {
            Session.getLog().error("An error has occured while comparing plan prices ", ex);
            return false;
        }
    }

    public boolean isDemoPlan() throws Exception {
        String confVal = Session.getPropertyString("EMULATION_MODE");
        return ("".equals(confVal) || "FALSE".equals(confVal.toUpperCase())) && "1".equals(getValue("_EMULATION_MODE"));
    }

    public TemplateModel FM_isDemoPlan() throws Exception {
        return new TemplateString(isDemoPlan());
    }

    protected String getPrefix(String promo) throws Exception {
        if ("1".equals(promo)) {
            return "_SPONSOR_";
        }
        if ("2".equals(promo)) {
            return "_PROMO_";
        }
        return "_NOT_CONFIGURED_";
    }

    public TemplateModel FM_getPromotions() throws Exception {
        List list = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            String enPromo = getValue("_ENABLE_PROMO");
            String prefix = getPrefix(enPromo);
            ps = con.prepareStatement("SELECT name, value FROM plan_value WHERE plan_id = ? and name like ?");
            ps.setInt(1, this.f36id);
            ps.setString(2, prefix + "%");
            ResultSet rs = ps.executeQuery();
            String promo_key = new String();
            String promo_disc = new String();
            String promo_desc = new String();
            String currentKey = new String();
            while (rs.next()) {
                String promoName = rs.getString(1);
                String promoValue = rs.getString(2);
                if ("1".equals(enPromo)) {
                    if (promoName.startsWith("_SPONSOR_DESC_")) {
                        promo_desc = promoValue;
                        int ind = "_SPONSOR_DESC_".length();
                        currentKey = promoName.substring(ind);
                    } else if (promoName.startsWith("_SPONSOR_")) {
                        int ind2 = "_SPONSOR_".length();
                        promo_disc = promoValue;
                        promo_key = promoName.substring(ind2);
                    }
                } else if ("2".equals(enPromo)) {
                    if (promoName.startsWith("_PROMO_DESC_")) {
                        promo_desc = promoValue;
                        int ind3 = "_PROMO_DESC_".length();
                        currentKey = promoName.substring(ind3);
                    } else if (promoName.startsWith("_PROMO_")) {
                        int ind4 = "_PROMO_".length();
                        promo_disc = promoValue;
                        promo_key = promoName.substring(ind4);
                    }
                }
                if (currentKey.equals(promo_key)) {
                    Map map = new HashMap();
                    map.put(MerchantGatewayManager.MG_KEY_PREFIX, promo_key);
                    map.put("disc", promo_disc);
                    map.put("desc", promo_desc);
                    list.add(new TemplateMap(map));
                }
            }
            Session.closeStatement(ps);
            con.close();
            return new TemplateList(list);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getPromoValuesByKey(String key) throws Exception {
        Map map = new HashMap();
        String enPromo = getValue("_ENABLE_PROMO");
        map.put(MerchantGatewayManager.MG_KEY_PREFIX, key);
        if ("1".equals(enPromo)) {
            map.put("disc", getValue("_SPONSOR_" + key));
            map.put("desc", getValue("_SPONSOR_DESC_" + key));
        } else if ("2".equals(enPromo)) {
            map.put("disc", getValue("_PROMO_" + key));
            map.put("desc", getValue("_PROMO_DESC_" + key));
        }
        return new TemplateMap(map);
    }

    public TemplateModel FM_isPromoKeyExist(String key) throws Exception {
        String enPromo = getValue("_ENABLE_PROMO");
        String prefix = getPrefix(enPromo);
        String value = getValue(prefix + key);
        if (value == null || "".equals(value)) {
            return new TemplateString("0");
        }
        return new TemplateString("1");
    }

    protected void deleteValueFromDBAndValues(String value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND name=?");
            ps.setInt(1, this.f36id);
            ps.setString(2, value);
            Session.getLog().debug("Remove value with name: " + value);
            ps.executeUpdate();
            this.values.remove(value);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deletePromoEntry(String key) throws Exception {
        String promoType = getValue("_ENABLE_PROMO");
        String prefix = getPrefix(promoType);
        deleteValueFromDBAndValues(prefix + key);
        deleteValueFromDBAndValues(prefix + "DESC_" + key);
        return this;
    }

    public TemplateModel FM_deletePromoValues() throws Exception {
        Session.getLog().debug("in Plan.FM_deletePromoValues()");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            String promoType = getValue("_ENABLE_PROMO");
            String prefix = getPrefix(promoType);
            Session.getLog().debug("promo type = " + promoType);
            ps = con.prepareStatement("SELECT name FROM plan_value WHERE plan_id = ? AND name LIKE ?");
            ps.setInt(1, this.f36id);
            ps.setString(2, prefix + "%");
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            List keys = new LinkedList();
            while (rs.next()) {
                String promoName = rs.getString(1);
                int ind = prefix.length();
                keys.add(promoName.substring(ind));
            }
            int size = keys.size();
            for (int i = 0; i < size; i++) {
                String key = (String) keys.get(i);
                deleteValueFromDBAndValues(prefix + key);
                deleteValueFromDBAndValues(prefix + "DESC_" + key);
            }
            deleteValueFromDBAndValues("_ENABLE_PROMO");
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public double getPeriodSize(int periodId) throws Exception {
        return Calc.getMultiplier(this, periodId);
    }

    public boolean isResellerPlan() {
        return "6".equals(getValue("_CREATED_BY_"));
    }

    public TemplateModel FM_isResourceDisabled(String typeName) throws Exception {
        boolean isDisabled = false;
        if (isResellerPlan()) {
            isDisabled = Globals.isObjectDisabled(typeName, this.f36id) != 0;
        } else {
            ResourceType rt = getResourceType(TypeRegistry.getTypeId(typeName));
            if (rt == null || rt.isDisabled()) {
                isDisabled = true;
            } else {
                ResourceDependences.ResourceCollection rc = ResourceDependences.getAccessor().getRequiredResources(typeName);
                if (rc != null) {
                    Iterator orIter = rc.iterator();
                    while (orIter.hasNext()) {
                        ResourceDependences.RequiredSet andSet = (ResourceDependences.RequiredSet) orIter.next();
                        Iterator andIter = andSet.iterator();
                        while (andIter.hasNext()) {
                            String name = (String) andIter.next();
                            isDisabled = TypeRegistry.isResourceName(name) ? !isResourceTypeAvailable(name) : (Globals.getAccessor().isGlobalObject(name) && Globals.isObjectDisabled(name) == 0) ? false : true;
                            if (isDisabled) {
                                break;
                            }
                        }
                        if (!isDisabled) {
                            break;
                        }
                    }
                }
            }
        }
        if (isDisabled) {
            return new TemplateString("1");
        }
        return null;
    }

    public TemplateModel FM_getShowableDomainResources() throws Exception {
        Filter filter = new Filter();
        return filter.getShowableDomainResouces();
    }

    public TemplateModel FM_getShowableMailServiceResources() throws Exception {
        Filter filter = new Filter();
        return filter.getShowableMailServiceResources();
    }

    public TemplateModel FM_getShowableHostingResources() throws Exception {
        Filter filter = new Filter();
        return filter.getShowableHostingResources();
    }

    public TemplateModel FM_getShowableRemainResources() throws Exception {
        Filter filter = new Filter();
        return filter.getShowableRemainResources();
    }

    public Promo getPromo() {
        return null;
    }

    public List getPlanPromotions() {
        return getPlanPromotions(false);
    }

    public List getPlanPromotions(boolean force) {
        if (this.promotions != null && !force) {
            return this.promotions;
        }
        Session.getLog().debug("Inside Plan::getPlanPromotions: Re-loading promotions");
        this.promotions = new ArrayList();
        if (Settings.get().isPromoEnabled()) {
            String p = getValue("_PROMOTIONS");
            Session.getLog().debug("Inside Plan::getPlanPromotions: _PROMOTIONS=" + p);
            if (p != null && p.length() != 0) {
                StringTokenizer st = new StringTokenizer(p, "|");
                while (st.hasMoreElements()) {
                    String promoId = st.nextToken();
                    Session.getLog().debug("Inside Plan::getPlanPromotions: got code " + promoId);
                    try {
                        Promo promo = Promo.getPromo(Long.parseLong(promoId));
                        this.promotions.add(promo);
                    } catch (Exception ex) {
                        Session.getLog().error("Unable to load promotion with id " + promoId, ex);
                    }
                }
            }
        }
        return this.promotions;
    }

    public void addPromo(long promoId) throws Exception {
        Session.getLog().debug("Inside Plan::addPromo(long)");
        Promo promo = Promo.getPromo(promoId);
        String p = getValue("_PROMOTIONS");
        if (p != null) {
            StringTokenizer tokenizer = new StringTokenizer(p, "|");
            while (tokenizer.hasMoreTokens()) {
                String code = tokenizer.nextToken();
                if (Long.parseLong(code) == promoId) {
                    return;
                }
            }
        }
        putValue("_PROMOTIONS", p == null ? promoId + "|" : p + promoId + "|");
        if (!getPlanPromotions().contains(promo)) {
            synchronized (this.promotions) {
                this.promotions.add(promo);
            }
        }
    }

    public void disablePromo(long promoId) throws Exception {
        Session.getLog().debug("Inside Plan::disablePromo(long)");
        String sPromoId = new Long(promoId).toString();
        Promo promo = Promo.getPromo(promoId);
        if (getPlanPromotions().contains(promo)) {
            String p = getValue("_PROMOTIONS");
            String newP = "";
            if (p != null && p.length() > 0) {
                StringTokenizer st = new StringTokenizer(p, "|");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (!token.equals(sPromoId)) {
                        newP = newP + token + "|";
                    }
                }
            }
            putValue("_PROMOTIONS", newP);
            synchronized (this.promotions) {
                this.promotions.remove(promo);
            }
        }
    }

    public TemplateMethodModel FM_addPromo(long promoId) throws Exception {
        addPromo(promoId);
        return this;
    }

    public TemplateModel FM_disablePromo(long promoId) throws Exception {
        disablePromo(promoId);
        return this;
    }

    public List getAssignablePromotions() throws Exception {
        ArrayList res = new ArrayList(BillingManager.getActivePropmotions());
        res.removeAll(getPlanPromotions());
        Session.getLog().debug("Inside getAssignablePromotions: total active promotions " + BillingManager.getActivePropmotions().size() + " promotions assigned to plan " + getPlanPromotions().size() + " assignable promotions " + res.size());
        return res;
    }

    public boolean isPromoCodeApplicable() {
        Session.getLog().debug("Inside Plan::isPromoCodeApplicable; billing=" + getBilling() + " isPromoenabled=" + Settings.get().isPromoEnabled() + " plan promotions list size = " + getPlanPromotions().size());
        if (getBilling() == 0 || !Settings.get().isPromoEnabled()) {
            return false;
        }
        for (Promo p : getPlanPromotions()) {
            Session.getLog().debug("Inside Plan::isPromoCodeApplicable; got promo " + p.getId() + " isActive = " + p.isActive() + " isCodeless=" + p.isCodeLess());
            if (p.isActive() && !p.isCodeLess()) {
                return true;
            }
        }
        return false;
    }

    public List getAppliablePromotions(String promoCode) {
        Session.getLog().debug("Inside Plan::getApplicablePromotions(String)");
        List res = new ArrayList();
        if (Settings.get().isPromoEnabled()) {
            Session.getLog().debug("Promotioning is enabled");
            for (Promo p : getPlanPromotions()) {
                if (p.isCodeLess() || p.getCode().equals(promoCode)) {
                    Session.getLog().debug("Promo with id=" + p.getId() + " is applicable");
                    res.add(p);
                } else {
                    Session.getLog().debug("Promo with id=" + p.getId() + " is NOT applicable");
                }
            }
        }
        return res;
    }

    public boolean isFullyAccessible() {
        return this.access.isEmpty() && !isDisabled();
    }
}
