package psoft.hsphere.promotion;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateXML;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/PromoConfig.class */
public class PromoConfig {
    private static Hashtable promoValidators;
    private static Hashtable promoCalcs;
    private static Element root;

    static {
        try {
            root = XMLManager.getXML("PROMO_CONFIG").getDocumentElement();
            promoValidators = loadComponentInfo("promotions/promo");
            promoCalcs = loadComponentInfo("calculators/calc");
            Session.getLog().debug("Promo config file got parsed and loaded.");
        } catch (Exception ex) {
            Session.getLog().error("UNABLE TO INITIALIZE PROMO CONFIG. PROMOTIONS WILL NOT AFFECT", ex);
        }
    }

    private static Hashtable loadComponentInfo(String query) throws TransformerException {
        Hashtable iholder = new Hashtable();
        NodeList nl = XPathAPI.selectNodeList(root, query);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            Hashtable t = new Hashtable();
            t.put("class", n.getAttributes().getNamedItem("class").getNodeValue());
            t.put("params", getParams(n));
            t.put("node", new TemplateXML(n));
            iholder.put(n.getAttributes().getNamedItem("id").getNodeValue(), t);
        }
        return iholder;
    }

    private static List getParams(Node holder) throws TransformerException {
        List result = new ArrayList();
        NodeList nl = XPathAPI.selectNodeList(holder, "params/param");
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            result.add(n.getAttributes().getNamedItem("name").getNodeValue());
        }
        return result;
    }

    public static List getPromoValidatorParams(String promoValidatorId) {
        Session.getLog().debug("Getting param names for validator with id " + promoValidatorId);
        Session.getLog().debug("It is " + promoValidators.get(promoValidatorId));
        return (List) ((Hashtable) promoValidators.get(promoValidatorId)).get("params");
    }

    public static List getPromoCalcParams(String promoCalcId) {
        Session.getLog().debug("Getting param names for calc with id " + promoCalcId);
        Session.getLog().debug("It is " + promoCalcs.get(promoCalcId));
        return (List) ((Hashtable) promoCalcs.get(promoCalcId)).get("params");
    }

    public static Class getPromoValidatorClass(String promoValidatorId) throws Exception {
        try {
            String className = (String) ((Hashtable) promoValidators.get(promoValidatorId)).get("class");
            Class promoValidator = Class.forName(className);
            return promoValidator;
        } catch (ClassNotFoundException e) {
            Session.getLog().error("Error while accuiring class for promo validator with id " + promoValidatorId, e);
            throw e;
        }
    }

    public static Class getPromoCalculatorClass(String promoCalcId) throws Exception {
        try {
            String className = (String) ((Hashtable) promoCalcs.get(promoCalcId)).get("class");
            Class promoCalc = Class.forName(className);
            return promoCalc;
        } catch (ClassNotFoundException e) {
            Session.getLog().error("Error while accuiring class for promo validator with id " + promoCalcId, e);
            throw e;
        }
    }

    public static TemplateXML getPromoValidatorNode(String validatorId) {
        return (TemplateXML) ((Hashtable) promoValidators.get(validatorId)).get("node");
    }

    public static TemplateXML getPromoCalcNode(String calcId) {
        return (TemplateXML) ((Hashtable) promoCalcs.get(calcId)).get("node");
    }

    public static String getValidatorIdByClassName(String className) {
        for (Object key : promoValidators.keySet()) {
            if (className.equals(((Hashtable) promoValidators.get(key)).get("class"))) {
                return (String) key;
            }
        }
        return null;
    }

    public static String getCalcIdByClassName(String className) {
        for (Object key : promoCalcs.keySet()) {
            if (className.equals(((Hashtable) promoCalcs.get(key)).get("class"))) {
                return (String) key;
            }
        }
        return null;
    }
}
