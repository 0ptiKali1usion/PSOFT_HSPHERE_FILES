package psoft.hsphere.plan.wizard;

import freemarker.template.TemplateModel;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/plan/wizard/PlanWizardXML.class */
public class PlanWizardXML {
    private static Category log = Category.getInstance(PlanWizardXML.class.getName());

    public static TemplateModel getWizards() throws Exception {
        return XMLManager.getXMLAsTemplate("PLAN_WIZARDS", "wizards");
    }

    public static Document getWizardXML(String name) throws Exception {
        return XMLManager.getXML("PLAN_WIZARDS_DIR", name + ".xml");
    }

    public static TemplateModel getWizard(String name) throws Exception {
        return XMLManager.getXMLAsTemplate("PLAN_WIZARDS_DIR", name + ".xml", "PlanWizard");
    }
}
