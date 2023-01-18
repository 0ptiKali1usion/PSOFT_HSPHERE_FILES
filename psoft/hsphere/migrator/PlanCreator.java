package psoft.hsphere.migrator;

import java.io.PrintStream;
import java.util.Hashtable;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.global.Globals;
import psoft.hsphere.migrator.extractor.InfoExtractorUtils;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.hsphere.tools.ExternalCP;
import psoft.util.FakeRequest;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/PlanCreator.class */
public class PlanCreator extends ExternalCP {
    private InfoExtractorUtils utils;
    private Document doc;
    private boolean createAsActive;
    private boolean createWithPrices;
    private AccountManager manager;

    public PlanCreator(Document document, boolean makeActive, boolean createPrices) throws Exception {
        this.utils = null;
        this.doc = null;
        this.createAsActive = false;
        this.createWithPrices = true;
        this.manager = null;
        this.utils = new InfoExtractorUtils(document);
        this.doc = document;
        this.createAsActive = makeActive;
        this.createWithPrices = createPrices;
        this.manager = PlanUtils.getAccountManager();
    }

    public void createPlansFromXML(int resume, ResellerPlanHolder resellerPlanHolder) throws Exception {
        PrintStream oldStream = PlanUtils.setOutputStream("migrate_plans.log");
        boolean foundResumed = false;
        Element root = this.doc.getDocumentElement();
        NodeList plans = root.getElementsByTagName("plan");
        if (resume > 0) {
            PlanUtils.message("Skipping all plans before resumed plan - " + resellerPlanHolder.getResumePlan() + " ..\n");
        }
        for (int i = 0; i < plans.getLength(); i++) {
            Element plan = (Element) plans.item(i);
            String planName = plan.getAttribute("name");
            if (resume > 0) {
                if (planName.equals(resellerPlanHolder.getResumePlan()) && !foundResumed) {
                    foundResumed = true;
                    if (resume == 2) {
                        deleteResumedPlan(planName);
                    }
                    createPlan(plan);
                } else if (!foundResumed) {
                }
            }
            System.out.println("Creating plan [" + planName + "]");
            createPlan((Element) plans.item(i));
        }
        PlanUtils.unsetOutputStream(oldStream);
    }

    private void deleteResumedPlan(String planName) {
        try {
            Plan planObj = getPlanForName(planName, null);
            if (planObj == null) {
                PlanUtils.message("Resumed plan " + planName + " not exist - skipped delete .. ");
                return;
            }
            PlanUtils.message("Resumed plan " + planName + " exist - delete resumed ");
            this.manager.FM_deletePlan(String.valueOf(planObj.getId()));
            PlanUtils.outOK();
        } catch (Exception exc) {
            PlanUtils.outFail(exc);
        }
    }

    protected void createPriceParam(Hashtable reqParams, String prefix, String paramName, String id, String paramValue) {
        if (paramName == null) {
            return;
        }
        if (!"".equals(id)) {
            paramName = paramName + "_" + id;
        }
        if (paramValue == null) {
            reqParams.put(prefix + paramName, new String[]{""});
        } else {
            reqParams.put(prefix + paramName, new String[]{paramValue});
        }
    }

    protected void createSpecialParam(Hashtable reqParams, Element special) {
        reqParams.put(special.getAttribute("name"), new String[]{special.getAttribute("value")});
    }

    protected void createSpecialParams(Hashtable reqParams, Element resource) {
        NodeList specials = resource.getElementsByTagName("special");
        for (int i = 0; i < specials.getLength(); i++) {
            createSpecialParam(reqParams, (Element) specials.item(0));
        }
    }

    protected void createReqParam(Hashtable reqParams, Element resource, AccountManager manager) throws Exception {
        String name = resource.getAttribute("name").toString();
        if (!"".equals(manager.FM_isResourceDisabled(name).toString())) {
            PlanUtils.message("Resource - " + name + " disabled. Skip resource creation.\n");
            return;
        }
        String avaliable = resource.getAttribute("include").toString();
        String canenabled = resource.getAttribute("enable").toString();
        if ("1".equals(avaliable)) {
            reqParams.put("i_" + name, new String[]{"on"});
            if ("1".equals(canenabled)) {
                String enabled = resource.getAttribute("active");
                if ("1".equals(enabled)) {
                    reqParams.put("e_" + name, new String[]{"on"});
                }
            }
            createSpecialParams(reqParams, resource);
            NodeList prices = resource.getElementsByTagName("price");
            for (int i = 0; i < prices.getLength(); i++) {
                Element price = (Element) prices.item(i);
                String id = price.getAttribute("id");
                String freeunits = this.utils.getAttributeValue(price, "freeunits");
                createPriceParam(reqParams, "f_", name, id, freeunits);
                String setup = this.utils.getAttributeValue(price, "setup");
                createPriceParam(reqParams, "s_", name, id, setup);
                String unit = this.utils.getAttributeValue(price, "unit");
                createPriceParam(reqParams, "m_", name, id, unit);
                String usage = this.utils.getAttributeValue(price, "usage");
                createPriceParam(reqParams, "u_", name, id, usage);
            }
        }
    }

    protected void createResources(NodeList resources, Hashtable reqParams, AccountManager manager) throws Exception {
        for (int i = 0; i < resources.getLength(); i++) {
            createReqParam(reqParams, (Element) resources.item(i), manager);
        }
    }

    private Plan getPlanForName(String planName, String message) {
        if (message != null) {
            try {
                PlanUtils.message(message);
            } catch (Exception exc) {
                PlanUtils.outFail(exc);
                return null;
            }
        }
        int planId = Plan.getPlanIdByName(planName);
        Plan planObj = Plan.getPlan(planId);
        if (message != null) {
            if (planObj != null) {
                PlanUtils.outOK();
            } else {
                PlanUtils.outFail();
            }
        }
        return planObj;
    }

    private void setOptions(Hashtable reqParams, Element options) throws Exception {
        NodeList params = XPathAPI.selectNodeList(options, "param");
        for (int i = 0; i < params.getLength(); i++) {
            String name = ((Element) params.item(i)).getAttribute("name");
            setOptionValue(reqParams, options, name);
        }
    }

    public int createPlan(Element plan) throws Exception {
        Hashtable reqParams = new Hashtable();
        String planName = plan.getAttribute("name").toString();
        String wizardName = plan.getAttribute("wizard").toString();
        String reseller = plan.getAttribute(FMACLManager.RESELLER);
        if (reseller != null && !"".equals(reseller)) {
            PlanUtils.message("\nSetting reseller - " + reseller + " ..");
            this.manager = PlanUtils.getAccountManager(reseller);
        } else {
            PlanUtils.message("\nSetting reseller - admin ..");
            this.manager = PlanUtils.getAccountManager();
        }
        if (getPlanForName(planName, "\nChecking if plan [" + planName + "] already exists.") != null) {
            PlanUtils.message("Plan [" + planName + "] exists. Skipping plan creation.\n");
            return -1;
        }
        int returnPlanId = -1;
        try {
            PlanUtils.message("Create plan - " + planName);
            Element options = (Element) plan.getElementsByTagName("options").item(0);
            setOptions(reqParams, options);
            NodeList resources = XPathAPI.selectNodeList(plan, "resource");
            createResources(resources, reqParams, this.manager);
            NodeList ifresources = XPathAPI.selectNodeList(plan, "ifresource/resource");
            createResources(ifresources, reqParams, this.manager);
            NodeList ifgroups = XPathAPI.selectNodeList(plan, "ifresource/ifgroup/resource");
            createResources(ifgroups, reqParams, this.manager);
            NodeList ifgroups2 = XPathAPI.selectNodeList(plan, "ifgroup/resource");
            createResources(ifgroups2, reqParams, this.manager);
            NodeList lsGroups = plan.getElementsByTagName("LogicalGroup");
            for (int i = 0; i < lsGroups.getLength(); i++) {
                Element lsElement = (Element) lsGroups.item(i);
                String lsgroupID = lsElement.getAttribute("groupid");
                if (lsgroupID != null && !"".equals(lsgroupID) && Globals.isSetKeyDisabled("server_groups", lsgroupID) == 0) {
                    String lsname = lsElement.getAttribute("name");
                    reqParams.put(lsname, new String[]{lsgroupID});
                }
            }
            reqParams.put("plan_name", new String[]{planName});
            reqParams.put("wizard", new String[]{wizardName});
            FakeRequest request = new FakeRequest(reqParams);
            Session.setRequest(request);
            Plan createdPlan = psoft.hsphere.plan.wizard.PlanCreator.process();
            returnPlanId = createdPlan.getId();
            PlanUtils.outOK();
            createPlanPeriods(createdPlan, plan);
            if (this.createWithPrices) {
                PlanUtils.message("Change plan prices ");
                reqParams.put("plan_id", new String[]{String.valueOf(returnPlanId)});
                this.manager.FM_saveChangedPlan(new PlanChangeRequest(reqParams));
                PlanUtils.outOK();
            }
            if (this.createAsActive) {
                PlanUtils.message("Activate plan - " + planName);
                createdPlan.FM_enable();
                PlanUtils.outOK();
            }
            setPostCreateParamsForPlan(createdPlan, options);
            setCustomParamsForPlan(createdPlan, options);
        } catch (Exception exc) {
            PlanUtils.outFail(exc);
            Plan planObj = getPlanForName(planName, "Plan creation error- " + planName);
            if (planObj != null) {
                PlanUtils.message("Delete plan - " + planObj.get("name").toString());
                this.manager.FM_deletePlan(String.valueOf(planObj.getId()));
                PlanUtils.outOK();
            }
        }
        return returnPlanId;
    }

    private boolean isNotStandartParam(String[] notStandart, String name) {
        if (notStandart == null || name == null) {
            return false;
        }
        for (String str : notStandart) {
            if (str.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void setPostCreateParamsForPlan(Plan createdPlan, Element options) throws Exception {
        String[] notStandartParamsNames = {"contactinfo", "billinginfo"};
        NodeList customParams = options.getElementsByTagName("postparam");
        for (int i = 0; i < customParams.getLength(); i++) {
            Element postParam = (Element) customParams.item(i);
            String name = postParam.getAttribute("name");
            String value = postParam.getAttribute("value");
            if (isNotStandartParam(notStandartParamsNames, name)) {
                if ("contactinfo".equals(name)) {
                    createdPlan.FM_setCInfo(Integer.parseInt(value));
                } else if ("billinginfo".equals(name)) {
                    createdPlan.FM_setBInfo(Integer.parseInt(value));
                } else {
                    throw new Exception("Unknown param name - " + name);
                }
            } else {
                createdPlan.putValue(name, value);
            }
        }
    }

    private void setCustomParamsForPlan(Plan createdPlan, Element options) throws Exception {
        setOptionsParams(createdPlan, options, "customparam");
    }

    private void setOptionsParams(Plan createdPlan, Element options, String paramName) throws Exception {
        NodeList customParams = options.getElementsByTagName(paramName);
        for (int i = 0; i < customParams.getLength(); i++) {
            Element customParam = (Element) customParams.item(i);
            String name = customParam.getAttribute("name");
            String value = customParam.getAttribute("value");
            createdPlan.putValue(name, value);
        }
    }

    private void createPlanPeriods(Plan createdPlan, Element plan) throws Exception {
        Element periodXML = (Element) plan.getElementsByTagName("periods").item(0);
        NodeList periods = periodXML.getElementsByTagName("period");
        if (periods == null) {
            return;
        }
        PlanUtils.message("Create all plan periods ");
        createdPlan.FM_putValue("_PERIOD_TYPES", String.valueOf(periods.getLength()));
        for (int i = 0; i < periods.getLength(); i++) {
            String id = getPeriodValue((Element) periods.item(i), "id");
            createdPlan.FM_putValue("_PERIOD_TYPE_" + id, getPeriodValue((Element) periods.item(i), "type"));
            createdPlan.FM_putValue("_PERIOD_SIZE_" + id, getPeriodValue((Element) periods.item(i), "size"));
            createdPlan.FM_putValue("_PERIOD_SETUP_DISC_" + id, getPeriodValue((Element) periods.item(i), "discountsetup"));
            createdPlan.FM_putValue("_PERIOD_USAGE_DISC_" + id, getPeriodValue((Element) periods.item(i), "discountusage"));
            createdPlan.FM_putValue("_PERIOD_UNIT_DISC_" + id, getPeriodValue((Element) periods.item(i), "discountunit"));
        }
        PlanUtils.outOK();
    }

    protected void setOnOptionValue(Hashtable reqParams, Element options, String name) throws Exception {
        String value = getOptionValue(options, name);
        if (value != null && "on".equalsIgnoreCase(value)) {
            reqParams.put(name, new String[]{value});
        }
    }

    protected void setOptionValue(Hashtable reqParams, Element options, String name) throws Exception {
        String value = getOptionValue(options, name);
        if (value != null) {
            reqParams.put(name, new String[]{value});
        }
    }

    protected String getOptionValue(Element options, String name) throws Exception {
        Element param = (Element) XPathAPI.selectSingleNode(options, "param[@name=\"" + name + "\"]");
        if (param != null) {
            return param.getAttribute("value");
        }
        return null;
    }

    protected String getPeriodValue(Element period, String name) throws Exception {
        return period.getAttribute(name);
    }

    public static void main(String[] argv) throws Exception {
        try {
            if (argv.length >= 2) {
                boolean createAsActive = false;
                boolean createWithPrices = false;
                String plansXMLFile = null;
                int resume = 0;
                String resellerName = FMACLManager.ADMIN;
                boolean isUsingPlanIds = false;
                ResellerPlanHolder resellerPlanHolder = null;
                int i = 0;
                while (i < argv.length) {
                    if ("-active".equals(argv[i])) {
                        createAsActive = true;
                    } else if (!"-force".equals(argv[i])) {
                        if ("-d".equals(argv[i])) {
                            plansXMLFile = argv[i + 1];
                        } else if ("-createprices".equals(argv[i])) {
                            createWithPrices = true;
                        } else if ("-r".equals(argv[i])) {
                            resume = 1;
                        } else if ("-rc".equals(argv[i])) {
                            resume = 2;
                        } else if ("-resellername".equals(argv[i])) {
                            i++;
                            resellerName = argv[i];
                            resellerPlanHolder = new ResellerPlanHolder(resellerName);
                        } else if ("-plan".equals(argv[i]) || "-id".equals(argv[i])) {
                            if (resellerName == null) {
                                resellerName = FMACLManager.ADMIN;
                            }
                            if ("-id".equals(argv[i])) {
                                isUsingPlanIds = true;
                            }
                            i++;
                            if (isUsingPlanIds) {
                                try {
                                    Integer.parseInt(argv[i]);
                                } catch (NumberFormatException e) {
                                    throw new Exception("Invalid plan ID: " + e);
                                }
                            }
                            resellerPlanHolder.setResumePlan(argv[i]);
                        }
                    }
                    i++;
                }
                Document document = InfoExtractorUtils.getDocumentFromFile(plansXMLFile);
                PlanCreator creator = new PlanCreator(document, createAsActive, createWithPrices);
                creator.createPlansFromXML(resume, resellerPlanHolder);
            } else {
                PlanUtils.message("\nCommand syntax :\njava psoft.hsphere.migrator.PlansCreator [<-active>] -d <xml_file_name> [<-createprices>]\n-active - if avaliable, then after created plan, plan is activated\n-createprices - create prices for plansxml_file_name - xml file with plans' names\nExamples : java psoft.hsphere.migrator.PlansCreator -active -d plans.xml\n           java psoft.hsphere.migrator.PlansCreator -active -d plans.xml -createprices\n           java psoft.hsphere.migrator.PlansCreator -d plans.xml\n");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
