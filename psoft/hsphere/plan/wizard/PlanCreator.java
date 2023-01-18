package psoft.hsphere.plan.wizard;

import java.util.Hashtable;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.hsphere.resource.plan_wizard.PlanWizard;
import psoft.util.USFormat;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/plan/wizard/PlanCreator.class */
public class PlanCreator extends PlanManipulator {
    private static Category log = Category.getInstance(PlanCreator.class.getName());

    /* renamed from: pw */
    PlanWizard f112pw;

    public PlanCreator() throws Exception {
        this.root = (Element) XPathAPI.selectSingleNode(PlanWizardXML.getWizardXML(this.f114rq.getParameter("wizard")), "PlanWizard");
        this.f112pw = new PlanWizard(this.f114rq);
    }

    protected void setDefaultValues() throws Exception {
        this.f112pw.setValue("_CREATED_BY_", getAttribute(this.root, "name"));
        NodeList list = XPathAPI.selectNodeList(this.root, "DefaultValues/value");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            this.f112pw.setValue(getAttribute(n, "name"), parseValue(getNodeValue(n)));
        }
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void setValues(String name, NodeList list, boolean flag) throws Exception {
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            String mask = getAttribute(n, "mask");
            if (mask != null) {
                Hashtable t = parseValues(mask);
                for (String key : t.keySet()) {
                    String val = (String) t.get(key);
                    if (val != null && val.length() > 0) {
                        this.f112pw.setValue(name, key, val);
                    }
                }
            } else {
                log.info("Setting value: " + getAttribute(n, "name") + parseValue(getNodeValue(n)));
                String val2 = parseValue(getNodeValue(n));
                if (val2 != null) {
                    this.f112pw.setValue(name, getAttribute(n, "name"), val2);
                }
            }
        }
    }

    protected void setGenericValues() throws Exception {
        if (this.f114rq.getParameter("send_invoice") != null) {
            this.f112pw.setValue("_SEND_INVOICE", "1");
        }
        if (this.f114rq.getParameter("demo_only") != null) {
            this.f112pw.setValue("_EMULATION_MODE", "1");
        }
        if (this.f114rq.getParameter("money_back") != null) {
            this.f112pw.setValue("MONEY_BACK_CALC", "psoft.hsphere.calc.StandardMoneyBackCalc");
            this.f112pw.setValue("MONEY_BACK_DAYS", this.f114rq.getParameter("money_back_days"));
        }
        if ("2".equals(this.f114rq.getParameter("trial"))) {
            this.f112pw.setValue("_TRIAL_PERIOD", this.f114rq.getParameter("trial_duration"));
            this.f112pw.setValue("_TRIAL_CREDIT", USFormat.parseString(this.f114rq.getParameter("trial_credit")));
        }
        if (!isEmpty(this.f114rq.getParameter("hard_credit"))) {
            this.f112pw.setValue("_HARD_CREDIT", USFormat.parseString(this.f114rq.getParameter("hard_credit")));
        }
        this.f112pw.setValue("_PERIOD_TYPES", "1");
        this.f112pw.setValue("_PERIOD_TYPE_0", "MONTH");
        this.f112pw.setValue("_PERIOD_SIZE_0", "1");
    }

    protected void processMod(String name, String modName, Node mod, int ivCount, int irCount) throws Exception {
        String value;
        log.info("Procesing mod:" + name + ":" + modName);
        if (mod == null) {
            return;
        }
        NodeList list = XPathAPI.selectNodeList(mod, "*");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            String nodeName = n.getNodeName();
            log.info("Getting nodes:" + nodeName);
            if ("initvalue".equals(nodeName)) {
                int initValueType = InitValue.getType(getAttribute(n, "type"));
                if (initValueType != 7) {
                    value = parseValue(getNodeValue(n));
                } else {
                    value = "NA";
                }
                this.f112pw.addIValue(name, modName, initValueType, value, getAttribute(n, "label", ""), ivCount);
                ivCount++;
            } else if ("initresource".equals(nodeName)) {
                if (this.includedResources.contains(getAttribute(n, "name")) && ifActiveAttr(getAttribute(n, "ifactive")) && ifResourceAttr(getAttribute(n, "ifresource")) == 1) {
                    log.debug("Init resource: " + name + ":" + modName + ":" + getAttribute(n, "name") + ":" + getAttribute(n, "mod", ""));
                    this.f112pw.addIResource(name, modName, getAttribute(n, "name"), parseValue(getAttribute(n, "mod", "")), irCount);
                    this.enabledResources.add(name);
                    irCount++;
                }
            } else if ("if".equals(nodeName)) {
                Session.getLog().debug("mod name=" + mod.getNodeName());
                if (testNode(n)) {
                    int i2 = irCount;
                    irCount++;
                    processMod(name, modName, XPathAPI.selectSingleNode(n, "true"), ivCount, i2);
                } else {
                    int i3 = irCount;
                    irCount++;
                    processMod(name, modName, XPathAPI.selectSingleNode(n, "false"), ivCount, i3);
                }
            }
        }
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void processMods(String name, NodeList list, boolean flag) throws Exception {
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (ifResourceAttr(getAttribute(n, "ifresource")) == 1) {
                String modName = getAttribute(n, "name", "");
                this.f112pw.addMod(name, modName);
                processMod(name, modName, n, 0, 0);
            }
        }
    }

    protected void addResource(Node n) throws Exception {
        String name = getAttribute(n, "name");
        if ("1".equals(getAttribute(n, "required")) || this.f114rq.getParameter("e_" + name) != null) {
            this.enabledResources.add(name);
        }
        if (!this.includedResources.contains(name)) {
            log.info("adding resource-->" + name);
            this.f112pw.addResource(name, getAttribute(n, "class"));
            this.includedResources.add(name);
        }
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void processResource(Node n) throws Exception {
        String name = getAttribute(n, "name");
        if (this.f114rq.getParameter("i_" + name) != null) {
            addResource(n);
        } else if ("1".equals(getAttribute(n, "required")) || !isEmpty(getAttribute(n, "ifresource"))) {
            switch (ifResourceAttr(getAttribute(n, "ifresource"))) {
                case -1:
                default:
                    return;
                case 0:
                    this.unknownResources.put(name, n);
                    return;
                case 1:
                    addResource(n);
                    return;
            }
        }
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void setResources(NodeList list) throws Exception {
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            log.debug("Processign node: " + n.getNodeName());
            if (n.getNodeName().equals("resource")) {
                processResource(n);
            } else if (n.getNodeName().equals("ifresource")) {
                log.debug("Resource disabled:" + getAttribute(n, "name") + ":" + Session.isResourceDisabled(getAttribute(n, "name")));
                if (!Session.isResourceDisabled(getAttribute(n, "name"))) {
                    setResources(XPathAPI.selectNodeList(n, "*"));
                }
            } else if (n.getNodeName().equals("ifgroup")) {
                if (EnterpriseManager.areEnabledLServerGroups(getAttribute(n, "name"))) {
                    setResources(XPathAPI.selectNodeList(n, "*"));
                }
            } else if (n.getNodeName().equals("LogicalGroup")) {
                String varName = getAttribute(n, "name");
                this.f112pw.setValue("_HOST_" + varName, this.f114rq.getParameter(varName));
            }
        }
    }

    public static Plan process() throws Exception {
        PlanCreator pc = new PlanCreator();
        return pc._process();
    }

    protected void deleteValues(String name, NodeList list) {
    }

    protected void deleteMods(String name, NodeList list) {
    }

    private Plan _process() throws Exception {
        try {
            if (FMACLManager.ADMIN.equals(getAttribute(this.root, "name"))) {
                this.f112pw.setName(this.f114rq.getParameter("plan_name"), 0, 0, 0L);
            } else {
                this.f112pw.setName(this.f114rq.getParameter("plan_name"), this.f114rq.getParameter("trial"));
            }
            prepare();
            setDefaultValues();
            setGenericValues();
            this.f112pw.startResources();
            setResources();
            this.f112pw.doneResoures();
            this.f112pw.doneIResources();
            return this.f112pw.done();
        } catch (Exception e) {
            this.f112pw.abort();
            throw e;
        }
    }
}
