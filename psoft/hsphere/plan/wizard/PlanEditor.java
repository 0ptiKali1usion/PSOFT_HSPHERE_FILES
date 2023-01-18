package psoft.hsphere.plan.wizard;

import java.util.Hashtable;
import javax.servlet.ServletRequest;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.resource.admin.EnterpriseManager;

/* loaded from: hsphere.zip:psoft/hsphere/plan/wizard/PlanEditor.class */
public class PlanEditor extends PlanManipulator {
    private static Category log = Category.getInstance(PlanEditor.class.getName());

    /* renamed from: pe */
    psoft.hsphere.resource.plan_wizard.PlanEditor f113pe;

    public PlanEditor() throws Exception {
        this.f113pe = new psoft.hsphere.resource.plan_wizard.PlanEditor(this.f114rq);
        this.root = (Element) XPathAPI.selectSingleNode(PlanWizardXML.getWizardXML(this.f113pe.getPlan().getValue("_CREATED_BY_")), "PlanWizard");
    }

    public PlanEditor(ServletRequest rq) throws Exception {
        this.f113pe = new psoft.hsphere.resource.plan_wizard.PlanEditor(rq);
        this.root = (Element) XPathAPI.selectSingleNode(PlanWizardXML.getWizardXML(this.f113pe.getPlan().getValue("_CREATED_BY_")), "PlanWizard");
    }

    public static Plan process() throws Exception {
        PlanEditor pe = new PlanEditor();
        return pe._process();
    }

    public static Plan processRequest(ServletRequest rq) throws Exception {
        PlanEditor pe = new PlanEditor(rq);
        return pe._process();
    }

    protected void setGenericValues() throws Exception {
        String _billType = this.f114rq.getParameter("trial");
        if (_billType != null && _billType.length() > 0) {
            if (this.f114rq.getParameter("send_invoice") != null) {
                this.f113pe.setValue("_SEND_INVOICE", "1");
            } else {
                this.f113pe.deleteValue("_SEND_INVOICE");
            }
            if (this.f114rq.getParameter("money_back") != null) {
                this.f113pe.setValue("MONEY_BACK_CALC", "psoft.hsphere.calc.StandardMoneyBackCalc");
                this.f113pe.setValue("MONEY_BACK_DAYS", this.f114rq.getParameter("money_back_days"));
            } else {
                this.f113pe.deleteValue("MONEY_BACK_CALC");
                this.f113pe.deleteValue("MONEY_BACK_DAYS");
            }
            int billType = Integer.parseInt(_billType);
            this.f113pe.setBInfo(billType);
            if (billType == 2) {
                this.f113pe.setValue("_TRIAL_PERIOD", this.f114rq.getParameter("trial_duration"));
                this.f113pe.setValue("_TRIAL_CREDIT", this.f114rq.getParameter("trial_credit"));
            } else {
                this.f113pe.deleteValue("_TRIAL_PERIOD");
                this.f113pe.deleteValue("_TRIAL_CREDIT");
            }
            if (!isEmpty(this.f114rq.getParameter("hard_credit"))) {
                this.f113pe.setValue("_HARD_CREDIT", this.f114rq.getParameter("hard_credit"));
            } else {
                this.f113pe.deleteValue("_HARD_CREDIT");
            }
        }
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void setResources(NodeList list) throws Exception {
        setResources(list, true);
    }

    protected void setResources(NodeList list, boolean flag) throws Exception {
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            Session.getLog().debug("Processing node " + n.toString());
            if (n.getNodeName().equals("resource")) {
                processResource(n, flag);
            } else if (n.getNodeName().equals("ifresource")) {
                if (!Session.isResourceDisabled(getAttribute(n, "name"))) {
                    setResources(XPathAPI.selectNodeList(n, "*"), flag);
                } else {
                    setResources(XPathAPI.selectNodeList(n, "*"), false);
                }
            } else if (n.getNodeName().equals("ifgroup")) {
                if (EnterpriseManager.areEnabledLServerGroups(getAttribute(n, "name"))) {
                    setResources(XPathAPI.selectNodeList(n, "*"), flag);
                } else {
                    setResources(XPathAPI.selectNodeList(n, "*"), false);
                }
            } else if (n.getNodeName().equals("LogicalGroup")) {
                String varName = getAttribute(n, "name");
                if (flag) {
                    this.f113pe.setValue("_HOST_" + varName, this.f114rq.getParameter(varName));
                } else {
                    this.f113pe.deleteValue("_HOST_" + varName);
                }
            }
        }
    }

    protected void deleteResource(Node n) throws Exception {
        String name = getAttribute(n, "name");
        this.deletedResources.add(name);
        this.f113pe.disableResource(name);
        this.f113pe.eraseResourcePrice(name);
    }

    protected void addResource(Node n) throws Exception {
        String name = getAttribute(n, "name");
        this.includedResources.add(name);
        if ("1".equals(getAttribute(n, "required")) || this.f114rq.getParameter("e_" + name) != null) {
            this.enabledResources.add(name);
        }
        log.info("adding resource-->" + name);
        this.f113pe.addResource(name, getAttribute(n, "class"));
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void processResource(Node n) throws Exception {
        processResource(n, true);
    }

    protected void processResource(Node n, boolean flag) throws Exception {
        if (flag) {
            String name = getAttribute(n, "name");
            if (this.f114rq.getParameter("i_" + name) != null) {
                addResource(n);
                return;
            } else if ("1".equals(getAttribute(n, "required")) || !isEmpty(getAttribute(n, "ifresource"))) {
                switch (ifResourceAttr(getAttribute(n, "ifresource"))) {
                    case -1:
                        deleteResource(n);
                        return;
                    case 0:
                        this.unknownResources.put(name, n);
                        return;
                    case 1:
                        addResource(n);
                        return;
                    default:
                        return;
                }
            } else {
                deleteResource(n);
                return;
            }
        }
        deleteResource(n);
    }

    @Override // psoft.hsphere.plan.wizard.PlanManipulator
    protected void setValues(String name, NodeList list, boolean flag) throws Exception {
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (flag) {
                String mask = getAttribute(n, "mask");
                if (mask != null) {
                    Hashtable t = parseValues(mask);
                    for (String key : t.keySet()) {
                        String val = (String) t.get(key);
                        if (val != null && val.length() > 0) {
                            this.f113pe.setValue(name, key, val);
                        } else {
                            this.f113pe.deleteValue(name, key);
                        }
                    }
                } else {
                    String val2 = parseValue(getNodeValue(n));
                    if (val2 == null) {
                        val2 = "";
                    }
                    this.f113pe.setValue(name, getAttribute(n, "name"), val2);
                }
            } else {
                this.f113pe.deleteValue(name, getAttribute(n, "name"));
            }
        }
    }

    protected void processMod(String name, String modName, Node mod, int ivCount, int irCount, boolean flag) throws Exception {
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
                if (this.includedResources.contains(name) && flag) {
                    Session.getLog().debug("Adding IValue name=" + name + " mod=" + modName + " type=" + initValueType + " value " + value + " label " + getAttribute(n, "label", ""));
                    this.f113pe.addIValue(name, modName, initValueType, value, getAttribute(n, "label", ""), ivCount);
                    ivCount++;
                }
            } else if ("initresource".equals(nodeName)) {
                log.debug("Init resource: " + name + ":" + modName + ":" + getAttribute(n, "name") + ":" + getAttribute(n, "mod", "") + " unique=" + getAttribute(n, "unique"));
                if (this.includedResources.contains(getAttribute(n, "name")) && ifActiveAttr(getAttribute(n, "ifactive")) && ifResourceAttr(getAttribute(n, "ifresource")) == 1 && flag) {
                    String uniq = getAttribute(n, "unique");
                    this.f113pe.addIResource(name, modName, getAttribute(n, "name"), parseValue(getAttribute(n, "mod", "")), "1".equals(uniq));
                    this.enabledResources.add(getAttribute(n, "name"));
                } else {
                    this.f113pe.disableIResource(name, modName, getAttribute(n, "name"), parseValue(getAttribute(n, "mod", "")));
                }
            } else if ("if".equals(nodeName)) {
                if (testNode(n)) {
                    int i2 = irCount;
                    irCount++;
                    processMod(name, modName, XPathAPI.selectSingleNode(n, "true"), ivCount, i2, flag);
                } else {
                    int i3 = irCount;
                    irCount++;
                    processMod(name, modName, XPathAPI.selectSingleNode(n, "false"), ivCount, i3, flag);
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
                if (flag) {
                    this.f113pe.addMod(name, modName);
                } else {
                    this.f113pe.disableMod(name, modName);
                }
                processMod(name, modName, n, 0, 0, flag);
            }
        }
    }

    private Plan _process() throws Exception {
        try {
            prepare();
            setGenericValues();
            setResources();
            return this.f113pe.done();
        } catch (Exception e) {
            throw e;
        }
    }
}
