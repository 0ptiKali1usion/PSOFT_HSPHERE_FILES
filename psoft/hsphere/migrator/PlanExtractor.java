package psoft.hsphere.migrator;

import java.io.PrintStream;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Plan;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.global.Globals;
import psoft.hsphere.migrator.extractor.InfoExtractorUtils;
import psoft.hsphere.plan.InitModifier;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.plan.wizard.PlanChanger;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.hsphere.tools.ExternalCP;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateXML;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/PlanExtractor.class */
public class PlanExtractor extends ExternalCP {
    private InfoExtractorUtils utils;
    private String file;
    private boolean force;

    public PlanExtractor(Document document, String file, boolean force) throws Exception {
        this.utils = null;
        this.file = null;
        this.force = false;
        this.utils = new InfoExtractorUtils(document);
        this.file = file;
        this.force = force;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:82:0x0089  */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0091  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x009d  */
    /* JADX WARN: Removed duplicated region for block: B:90:0x00cc  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public org.w3c.dom.Node extractPlans(java.util.ArrayList r7) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 321
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.migrator.PlanExtractor.extractPlans(java.util.ArrayList):org.w3c.dom.Node");
    }

    protected Node extractPlan(String planStr, AccountManager accManager, String resellerName, boolean isUsingPlansIDs) throws Exception {
        Element node = this.utils.createNode("plan");
        PlanChanger planChanger = null;
        TemplateXML categoryXML = null;
        Plan plan = null;
        String planName = planStr;
        try {
            if (isUsingPlansIDs) {
                plan = Plan.getPlan(planStr);
            } else {
                int planId = Plan.getPlanIdByName(planStr);
                plan = Plan.getPlan(planId);
            }
        } catch (Exception exc) {
            PlanUtils.messageErr("\nError getting plan - " + planName + ":\n");
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (plan == null) {
            PlanUtils.message("Reseller [" + resellerName + "] : Failed to find plan [" + planName + "].  Skipping.\n");
            return null;
        }
        PlanUtils.message("Reseller [" + resellerName + "] : Extracting plan [" + planName + "]...\n");
        if (isUsingPlansIDs) {
            planName = plan.getDescription();
        }
        planChanger = new PlanChanger(plan);
        TemplateXML wizard = planChanger.FM_getWizard();
        String wizardName = ((Element) wizard.getCurrentNode()).getAttribute("name").toString();
        node.setAttribute("name", planName);
        node.setAttribute("wizard", wizardName);
        if (resellerName != null && !FMACLManager.ADMIN.equals(resellerName)) {
            node.setAttribute(FMACLManager.RESELLER, resellerName);
        }
        String periods = plan.getValue("_PERIOD_TYPES");
        Element intervals = getPlanIntervals(plan, periods);
        if (intervals != null) {
            this.utils.appendChildNode(node, intervals);
        }
        Element options = getPlanOptions(plan, planChanger);
        if (options != null) {
            this.utils.appendChildNode(node, options);
        }
        TemplateXML categoriesXML = wizard.get("categories");
        categoryXML = (TemplateXML) categoriesXML.get("category");
        categoryXML.rewind();
        getListCat(node, accManager, plan, planChanger, categoryXML);
        return this.utils.checkChildren(node);
    }

    private Element getPlanIntervals(Plan plan, String periods) throws Exception {
        Element intervals = this.utils.createNode("periods");
        if (periods == null) {
            return null;
        }
        int periodsCounter = Integer.parseInt(periods);
        for (int i = 0; i < periodsCounter; i++) {
            Element interval = this.utils.createNode("period");
            String periodType = plan.getValue("_PERIOD_TYPE_" + i);
            String periodSize = plan.getValue("_PERIOD_SIZE_" + i);
            String periodSetup = plan.getValue("_PERIOD_SETUP_DISC_" + i);
            String periodUsage = plan.getValue("_PERIOD_USAGE_DISC_" + i);
            String periodUnit = plan.getValue("_PERIOD_UNIT_DISC_" + i);
            addPlanPeriodValues(interval, i, periodType, periodSize, periodSetup, periodUsage, periodUnit);
            if (this.utils.checkChildren(interval) != null) {
                this.utils.appendChildNode(intervals, interval);
            }
        }
        return this.utils.checkChildren(intervals);
    }

    private Element getPlanOptions(Plan plan, PlanChanger planChanger) throws Exception {
        String moneyBackDays;
        Element planOptions = this.utils.createNode("options");
        String periods = plan.getValue("_PERIOD_TYPES");
        TemplateXML wizard = planChanger.FM_getWizard();
        TemplateXML optionsXML = wizard.get("options");
        Element options = (Element) optionsXML.getCurrentNode();
        String trialPeriod = plan.getValue("_TRIAL_PERIOD");
        if (trialPeriod != null) {
            addOptionValue(planOptions, "trial_duration", trialPeriod);
            String trialCredit = plan.getValue("_TRIAL_CREDIT");
            addOptionValue(planOptions, "trial_credit", trialCredit);
        }
        String hardCredit = plan.getValue("_HARD_CREDIT");
        if (hardCredit != null) {
            addOptionValue(planOptions, "hard_credit", hardCredit);
        }
        String sendInvoice = plan.getValue("_SEND_INVOICE");
        if ("1".equals(sendInvoice)) {
            addOptionValue(planOptions, "send_invoice", "on");
        }
        String billingType = plan.get("b_info").toString();
        addOptionValue(planOptions, "trial", billingType);
        String ipTypeAttr = options.getAttribute("iptype");
        if ("1".equals(ipTypeAttr)) {
            try {
                String ipType = planChanger.FM_getIPType().toString();
                addOptionValue(planOptions, "mixedip", ipType);
            } catch (Exception e) {
                PlanUtils.message("\tFailed to get IP type.  Using shared.\n");
                addOptionValue(planOptions, "mixedip", "shared");
            }
        }
        String sharedIPAttr = options.getAttribute("sharediptag");
        if ("1".equals(sharedIPAttr)) {
            String sharedIPTag = planChanger.FM_getSharedIPTag().toString();
            addOptionValue(planOptions, "shared_ip_tag", sharedIPTag);
        }
        String ialiasAttr = options.getAttribute("ialias");
        if ("1".equals(ialiasAttr)) {
            ResourceType vidomainAliasRes = plan.FM_getResourceType("idomain_alias");
            InitModifier initMod = vidomainAliasRes.get("modDefault");
            if (initMod != null) {
                String vidomainAlias = initMod.FM_getInitValue(0).getAsString();
                String vidomainAliasPrefix = initMod.FM_getInitValue(1).getAsString();
                String ialias = "dNNN" + vidomainAliasPrefix + "NNN" + vidomainAlias;
                addOptionValue(planOptions, "calias", ialias);
            }
        }
        String vstopgapaliasAttr = options.getAttribute("stopgap");
        if ("1".equals(vstopgapaliasAttr)) {
            ResourceType vidomainAliasRes2 = plan.FM_getResourceType("nodomain");
            InitModifier initMod2 = vidomainAliasRes2.get("modDefault");
            if (initMod2 != null) {
                String vdummyDomain = initMod2.FM_getInitValue(0).getAsString();
                String vdummyDomainPrefix = initMod2.FM_getInitValue(1).getAsString();
                String vstopgapalias = "username" + vdummyDomainPrefix + "NNN" + vdummyDomain;
                addOptionValue(planOptions, "stopgapalias", vstopgapalias);
            }
        }
        String billableAttr = options.getAttribute("billable");
        if ("1".equals(billableAttr) && (moneyBackDays = plan.getValue("MONEY_BACK_DAYS")) != null) {
            addOptionValue(planOptions, "money_back_days", moneyBackDays);
            addOptionValue(planOptions, "money_back", "on");
        }
        if (periods != null && !"".equals(periods)) {
            addOptionValue(planOptions, "periods", periods);
        }
        String contactInfo = plan.get("c_info").toString();
        if (contactInfo != null) {
            addPostCreateParamValue(planOptions, "contactinfo", contactInfo);
        }
        String billingInfo = plan.get("b_info").toString();
        if (billingInfo != null) {
            addPostCreateParamValue(planOptions, "billinginfo", billingInfo);
        }
        String template = plan.getValue("_template");
        if (template != null) {
            addPostCreateParamValue(planOptions, "_template", template);
        }
        String templatesDirectory = plan.getValue("_TEMPLATES_DIR");
        if (templatesDirectory != null) {
            addPostCreateParamValue(planOptions, "_TEMPLATES_DIR", templatesDirectory);
        }
        TemplateList keys = plan.get("valueKeys");
        while (keys.hasNext()) {
            String name = keys.next().toString();
            if (name.indexOf("_") != 0) {
                String value = plan.getValue(name);
                addCustomOptionValue(planOptions, name, value);
            }
        }
        return this.utils.checkChildren(planOptions);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void addPlanPeriodValues(Element period, int id, String periodType, String periodSize, String periodSetup, String periodUsage, String periodUnit) {
        String[] strArr = {new String[]{"type", periodType}, new String[]{"size", periodSize}, new String[]{"discountsetup", periodSetup}, new String[]{"discountusage", periodUsage}, new String[]{"discountunit", periodUnit}};
        period.setAttributeNode(this.utils.createAttribute("id", String.valueOf(id)));
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i][1] != 0 && !"".equals(strArr[i][1])) {
                period.setAttributeNode(this.utils.createAttribute(strArr[i][0], strArr[i][1]));
            }
        }
    }

    private void addPostCreateParamValue(Element planOptions, String paramName, String paramValue) {
        Element value = this.utils.createNode("postparam");
        this.utils.createAttribute(value, "name", paramName);
        this.utils.createAttribute(value, "value", paramValue);
        this.utils.appendChildNode(planOptions, value);
    }

    private void addOptionValue(Element planOptions, String paramName, String paramValue) {
        Element value = this.utils.createNode("param");
        this.utils.createAttribute(value, "name", paramName);
        this.utils.createAttribute(value, "value", paramValue);
        this.utils.appendChildNode(planOptions, value);
    }

    private void addCustomOptionValue(Element planOptions, String paramName, String paramValue) {
        Element value = this.utils.createNode("customparam");
        this.utils.createAttribute(value, "name", paramName);
        this.utils.createAttribute(value, "value", paramValue);
        this.utils.appendChildNode(planOptions, value);
    }

    protected void getListCat(Element parent, AccountManager accManager, Plan plan, PlanChanger planChanger, TemplateXML categoryXML) throws Exception {
        NodeList categories = categoryXML.getElementsList();
        for (int i = 0; i < categories.getLength(); i++) {
            NodeList elements = this.utils.getNodeList(categories.item(i), "*");
            for (int j = 0; j < elements.getLength(); j++) {
                this.utils.appendChildNode(parent, getResourceAsXML(accManager, plan, planChanger, (Element) elements.item(j)));
            }
        }
    }

    protected void getListSubResources(Element parent, AccountManager manager, Plan plan, PlanChanger changer, NodeList resources) throws Exception {
        for (int i = 0; i < resources.getLength(); i++) {
            this.utils.appendChildNode(parent, getResourceAsXML(manager, plan, changer, (Element) resources.item(i)));
        }
    }

    protected Element getResourceAsXML(AccountManager accManager, Plan plan, PlanChanger planChanger, Element element) throws Exception {
        String tagType = element.getTagName();
        Element node = this.utils.createNode(tagType);
        String resName = element.getAttribute("name").toString();
        boolean canBeEnabled = false;
        try {
            if (!"".equals(element.getAttribute("active").toString())) {
                canBeEnabled = true;
            }
            TemplateXML wizardXML = planChanger.FM_getWizard();
            if ("resource".equals(tagType)) {
                boolean isRequired = true;
                if (!"1".equals(element.getAttribute("required").toString())) {
                    isRequired = false;
                }
                if (!"1".equals(element.getAttribute("adminonly").toString())) {
                }
                boolean isResellerOnly = true;
                if (!"1".equals(element.getAttribute("reselleronly").toString())) {
                    isResellerOnly = false;
                }
                if ((!isRequired || !isResellerOnly) && Globals.isObjectDisabled(resName) == 0) {
                    boolean isAvaliable = true;
                    ResourceType resAval = plan.isResourceAvailable(resName);
                    if (resAval == null) {
                        isAvaliable = false;
                    }
                    node.setAttributeNode(this.utils.createAttribute("name", resName));
                    this.utils.createBoolAttr(node, "include", isAvaliable);
                    this.utils.createBoolAttr(node, "enable", canBeEnabled);
                    if (canBeEnabled) {
                        this.utils.createBoolAttr(node, "active", planChanger.isResourceEnabled(resName));
                    }
                    String resTypeID = TypeRegistry.getTypeId(resName);
                    ResourceType resType = plan.getResourceType(resTypeID);
                    if (resType != null) {
                        String periodTypesCount = plan.getValue("_PERIOD_TYPES");
                        appendPrices(node, resType, periodTypesCount);
                    }
                    getSpecialValues(node, wizardXML, resName, resType);
                    getFieldsValues(node, element, resType);
                }
            } else if ("ifresource".equals(tagType)) {
                if (!"1".equals(accManager.FM_isResourceDisabled(resName).toString())) {
                    getListSubResources(node, accManager, plan, planChanger, this.utils.getNodeList(element, "*"));
                }
            } else if ("ifgroup".equals(tagType)) {
                if (EnterpriseManager.areEnabledLServerGroups(resName)) {
                    getListSubResources(node, accManager, plan, planChanger, this.utils.getNodeList(element, "*"));
                }
            } else if ("LogicalGroup".equals(tagType)) {
                String lsName = element.getAttribute("name");
                String lsType = element.getAttribute("type");
                String lsgroupID = planChanger.FM_getLogicalServerGroup(resName).toString();
                try {
                    Integer.parseInt(lsgroupID);
                    node.setAttributeNode(this.utils.createAttribute("name", lsName));
                    node.setAttributeNode(this.utils.createAttribute("type", lsType));
                    node.setAttributeNode(this.utils.createAttribute("groupid", lsgroupID));
                } catch (Exception e) {
                }
            }
        } catch (Exception exc) {
            PlanUtils.messageErr("\nError getting resource - " + resName + "\n");
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private void appendPrices(Node node, ResourceType resType, String periodTypesCount) throws Exception {
        Element price = createPrice(resType, "");
        if (price != null) {
            node.appendChild(price);
        }
        if (!"".equals(periodTypesCount)) {
            try {
                int periodTypes = Integer.parseInt(periodTypesCount);
                for (int i = 0; i < periodTypes; i++) {
                    Element price2 = createPrice(resType, String.valueOf(i));
                    if (price2 != null) {
                        node.appendChild(price2);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private void getSpecialValues(Node parent, TemplateXML wizardXML, String resName, ResourceType resType) throws Exception {
        TemplateXML specialXML;
        TemplateXML fieldXML;
        Element field;
        TemplateXML specXML = wizardXML.get("special");
        if (specXML == null || (specialXML = specXML.get("res_" + resName)) == null || (fieldXML = (TemplateXML) specialXML.get("field")) == null || (field = getField(fieldXML, resType)) == null) {
            return;
        }
        parent.appendChild(field);
    }

    private void getFieldsValues(Node parent, Element resource, ResourceType resType) throws Exception {
        Element fields = this.utils.createNode("fields");
        NodeList srcFields = resource.getElementsByTagName("field");
        for (int i = 0; i < srcFields.getLength(); i++) {
            Element special = getField((Element) srcFields.item(i), resType);
            if (this.utils.checkChildren(special) != null) {
                fields.appendChild(special);
            }
        }
        if (this.utils.checkChildren(fields) != null) {
            parent.appendChild(fields);
        }
    }

    private Element getField(TemplateXML fieldXML, ResourceType resType) throws Exception {
        if (fieldXML != null) {
            Element field = (Element) fieldXML.getCurrentNode();
            return getField(field, resType);
        }
        return null;
    }

    private Element getField(Element field, ResourceType resType) throws Exception {
        Element special = this.utils.createNode("special");
        String fieldName = field.getAttribute("name").toString();
        try {
            String fieldValue = resType.getValue(fieldName.toUpperCase());
            special.setAttributeNode(this.utils.createAttribute("name", fieldName));
            special.setAttributeNode(this.utils.createAttribute("value", fieldValue));
        } catch (Exception e) {
        }
        return this.utils.checkChildren(special);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private Element createPrice(ResourceType resType, String priceID) throws Exception {
        Element price = this.utils.createNode("price");
        String[] strArr = {new String[]{"_FREE_UNITS_", "freeunits"}, new String[]{"_SETUP_PRICE_", "setup"}, new String[]{"_UNIT_PRICE_", "unit"}, new String[]{"_USAGE_PRICE_", "usage"}};
        for (int i = 0; i < strArr.length; i++) {
            createPriceAttribute(price, resType, strArr[i][0] + priceID, strArr[i][1]);
        }
        Element result = this.utils.checkChildren(price);
        if (result != null) {
            price.setAttributeNode(this.utils.createAttribute("id", priceID));
        }
        return result;
    }

    private void createPriceAttribute(Element parent, ResourceType resType, String valueName, String tagName) {
        String value = resType.getValue(valueName);
        this.utils.createNotemptyAttribute(parent, tagName, value);
    }

    public void createXML(ArrayList resellers, Document document, String plansLogFile) throws Exception {
        PrintStream oldStream = PlanUtils.setErrorStream(plansLogFile);
        try {
            Node root = extractPlans(resellers);
            if (root != null) {
                document.appendChild(root);
                InfoExtractorUtils.serializeDocument(document, this.file);
                InfoExtractorUtils.validateDocument(this.file, true);
            } else {
                PlanUtils.message("Plans not found!\n");
            }
            PlanUtils.unsetErrorStream(oldStream);
        } catch (Exception e) {
            PlanUtils.unsetErrorStream(oldStream);
        } catch (Throwable th) {
            PlanUtils.unsetErrorStream(oldStream);
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        String plansResultFile = "plans.xml";
        String plansDTDFile = "/hsphere/local/home/cpanel/plans.dtd";
        String plansLogFile = "migrate_plans.log";
        if (argv.length == 0) {
            PlanUtils.message("\nCommand syntax :\njava psoft.hsphere.migrator.PlanExtractor [-force] [-xml <xml_filename>] [-dtd <dtd_filename>] [-log <log_filename>] -resellername res_name[-plans plans_names] [-ids plans_ids]\n-force - ignore all errors in process migration\n-xml - target XML file name, by default: plans.xml\n-dtd - DTD file name, by default: plans.dtd\n-log - log file nane, by default: migrate_plans.log\n-resellername -> reseller name\n-plans plans_names -> list of plan names\n-ids plans_ids -> list of plans ids\nExamples : java psoft.hsphere.migrator.PlanExtractor -force -resellername RESELLER -plans plan1 plan2\n           java psoft.hsphere.migrator.PlanExtractor -force -resellername RESELLER -ids 7564 7675\n           java psoft.hsphere.migrator.PlanExtractor -force -resellername RESELLER\n           java psoft.hsphere.migrator.PlanExtractor -force -resellername admin -plans 'Windows Plan'\n\t\t\tUnixPlan 'Win 2000' -resellername RESELLER -ids 7259 7260\n");
            return;
        }
        boolean force = false;
        String reseller = null;
        ArrayList resellers = new ArrayList();
        boolean isUsingPlanIds = false;
        ResellerPlanHolder resellerPlanHolder = null;
        int i = 0;
        while (i < argv.length) {
            if ("-force".equals(argv[i])) {
                force = true;
            } else if ("-xml".equals(argv[i])) {
                i++;
                plansResultFile = argv[i];
            } else if ("-dtd".equals(argv[i])) {
                i++;
                plansDTDFile = argv[i];
            } else if ("-log".equals(argv[i])) {
                i++;
                plansLogFile = argv[i];
            } else if ("-resellername".equals(argv[i])) {
                i++;
                reseller = argv[i];
                resellerPlanHolder = new ResellerPlanHolder(reseller);
                if (argv.length <= i + 1 || (!"-plans".equals(argv[i + 1]) && !"-ids".equals(argv[i + 1]))) {
                    resellers.add(resellerPlanHolder);
                }
            } else if ("-plans".equals(argv[i]) || "-ids".equals(argv[i])) {
                if (reseller == null) {
                    resellerPlanHolder = new ResellerPlanHolder(FMACLManager.ADMIN);
                }
                if ("-ids".equals(argv[i])) {
                    isUsingPlanIds = true;
                    resellerPlanHolder.setUsingPlansIDs(true);
                }
                while (true) {
                    i++;
                    if (i >= argv.length || argv[i].indexOf("-") == 0) {
                        break;
                    }
                    PlanUtils.message("Filing plan [" + argv[i] + "]...\n");
                    if (isUsingPlanIds) {
                        try {
                            Integer.parseInt(argv[i]);
                        } catch (NumberFormatException e) {
                            throw new Exception("Invalid plan ID: " + e);
                        }
                    }
                    resellerPlanHolder.addPlan(argv[i]);
                }
                i--;
                resellers.add(resellerPlanHolder);
            }
            i++;
        }
        Document document = InfoExtractorUtils.createDocument("plans", plansDTDFile);
        PlanUtils.message("Creating a CP instance...\n");
        PlanExtractor extractor = new PlanExtractor(document, plansResultFile, force);
        extractor.createXML(resellers, document, plansLogFile);
    }
}
