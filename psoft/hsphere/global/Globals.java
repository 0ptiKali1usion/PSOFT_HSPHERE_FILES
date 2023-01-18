package psoft.hsphere.global;

import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.global.ResourceDependences;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.util.XMLManager;
import psoft.util.xml.XMLElementTracker;

/* loaded from: hsphere.zip:psoft/hsphere/global/Globals.class */
public class Globals {
    protected int initErrors = 0;
    protected XMLElementTracker trackedElement;
    protected static final String CLASS_DESCRIPTION = "Globals";
    protected static Globals accessor = null;
    public static final String GLB_VALUE_PREFIX = "_GLB_";
    public static final int ENABLED = 0;
    public static final int DISABLED = 1;
    public static final int DISABLED_GLOBALLY = 2;
    public static final int NOT_CONFIGURED = 3;

    public static Globals getAccessor() throws Exception {
        if (accessor == null) {
            accessor = new Globals();
            try {
                accessor.initialize();
            } catch (Exception ex) {
                accessor = null;
                Session.getLog().error("Unable to initialize 'Globals'.", ex);
                throw ex;
            }
        }
        return accessor;
    }

    protected synchronized void initialize() throws Exception {
        Session.getLog().debug("Globals. Started to parse the 'globals' config file");
        this.trackedElement = new XMLElementTracker();
        try {
            Document globals = XMLManager.getXML("GLOBALS_CONFIG");
            Element root = globals.getDocumentElement();
            this.trackedElement.setElement(root);
            NodeList sections = root.getElementsByTagName("section");
            List resourceTypes = TypeRegistry.getTypes();
            for (int si = 0; si < sections.getLength(); si++) {
                Element section = (Element) sections.item(si);
                this.trackedElement.setElement(section);
                String sectionId = section.getAttribute("id");
                String label = section.getAttribute("label");
                String description = section.getAttribute("description");
                String show = section.getAttribute("show");
                String store = section.getAttribute("store");
                String onlineHelp = section.getAttribute("online_help");
                VisSection vs = VisSection.addSection(sectionId, label, description, show, store, onlineHelp);
                NodeList objects = section.getElementsByTagName("object");
                for (int oi = 0; oi < objects.getLength(); oi++) {
                    Element object = (Element) objects.item(oi);
                    this.trackedElement.setElement(object);
                    String objName = object.getAttribute("name");
                    String objLabel = object.getAttribute("label");
                    String objLabelEnabled = object.getAttribute("label_enabled");
                    String objLabelDisabled = object.getAttribute("label_disabled");
                    boolean isObjDisabledByDefault = "disabled".equals(object.getAttribute(AntiSpam.DEFAULT_LEVEL_VALUE));
                    boolean isObjConfigured = checkConfigured(object);
                    boolean isHsResource = resourceTypes.contains(objName);
                    GlobalObject gr = GlobalObject.addObject(objName, objLabel, objLabelEnabled, objLabelDisabled, sectionId, isObjDisabledByDefault, isObjConfigured, isHsResource, true);
                    vs.addObject(gr);
                }
                NodeList sets = section.getElementsByTagName("set");
                for (int ksi = 0; ksi < sets.getLength(); ksi++) {
                    Element set = (Element) sets.item(ksi);
                    this.trackedElement.setElement(set);
                    String setName = set.getAttribute("name");
                    String setLabel = set.getAttribute("label");
                    String setClass = set.getAttribute("class");
                    boolean isSetDisabledByDefault = "disabled".equals(set.getAttribute(AntiSpam.DEFAULT_LEVEL_VALUE));
                    boolean isSetConfigured = checkConfigured(set);
                    boolean managedGlobally = "globally".equals(set.getAttribute("managed"));
                    GlobalKeySet gs = AnyGlobalSet.addSet(setName, setLabel, sectionId, isSetDisabledByDefault, isSetConfigured, setClass, managedGlobally);
                    vs.addSet(gs);
                }
            }
            Session.getLog().debug("Globals. Finished to parse the 'globals' config file. " + (this.initErrors == 0 ? "No errors occured." : String.valueOf(this.initErrors) + " error(s) occured."));
        } catch (Exception e) {
            String tag = this.trackedElement.getTagSequence();
            Session.getLog().error("Globals. Error occured during parse the 'globals' config file." + (tag != null ? " Problem XML tag:\n" + tag : "") + "\n", e);
            throw e;
        }
    }

    private boolean checkConfigured(Element parentNode) throws Exception {
        Element vElement;
        NodeList checkNodes = parentNode.getElementsByTagName("check_config");
        if (checkNodes.getLength() == 0) {
            return true;
        }
        Element checkElement = (Element) checkNodes.item(0);
        this.trackedElement.setElement(checkElement);
        NodeList variantNodes = checkElement.getElementsByTagName("variant");
        int vLength = variantNodes.getLength();
        if (vLength == 0) {
            vElement = checkElement;
        } else {
            vElement = (Element) variantNodes.item(0);
        }
        boolean result = true;
        int vi = 1;
        while (true) {
            NodeList propNodes = vElement.getElementsByTagName("property");
            for (int ii = 0; ii < propNodes.getLength(); ii++) {
                Element reqProperty = (Element) propNodes.item(ii);
                this.trackedElement.setElement(reqProperty);
                String key = reqProperty.getAttribute(MerchantGatewayManager.MG_KEY_PREFIX);
                String value = reqProperty.getAttribute("value");
                String propertyValue = Session.getPropertyString(key);
                result = propertyValue != null && (("*".equals(value) && !"".equals(propertyValue)) || propertyValue.equals(value));
                if (!result) {
                    break;
                }
            }
            if (vi >= vLength || result) {
                break;
            }
            vElement = (Element) variantNodes.item(vi);
            vi++;
        }
        return result;
    }

    public List getAllGlobalObjectNames() {
        return GlobalObject.getObjectNames();
    }

    public List getAllGlobalSets() {
        return AnyGlobalSet.getAllSets();
    }

    public boolean isGlobalObject(String typeName) {
        return GlobalObject.contains(typeName);
    }

    public GlobalObject getObject(String typeName) {
        return GlobalObject.getResource(typeName);
    }

    public GlobalKeySet getSet(String groupName) {
        return AnyGlobalSet.getSet(groupName);
    }

    public VisSection getSection(String id) {
        return VisSection.getSection(id);
    }

    public List getAllSections() throws Exception {
        return VisSection.getSections();
    }

    public static int isObjectDisabled(String key) throws Exception {
        Globals accessor2 = getAccessor();
        Reseller r = Session.getReseller();
        int res = GlobalValueProvider.keyStatus(key, r, accessor2.getObject(key));
        if (res != 0) {
            return res;
        }
        ResourceDependences.ResourceCollection globResources = ResourceDependences.getAccessor().getGlobalResources(key);
        if (globResources != null) {
            Iterator orIter = globResources.iterator();
            while (orIter.hasNext()) {
                ResourceDependences.RequiredSet andSet = (ResourceDependences.RequiredSet) orIter.next();
                Iterator andIter = andSet.iterator();
                while (andIter.hasNext() && res == 0) {
                    String nKey = (String) andIter.next();
                    res = GlobalValueProvider.keyStatus(nKey, r, accessor2.getObject(nKey));
                    if (res != 0) {
                        break;
                    }
                }
                if (res == 0) {
                    return res;
                }
            }
        }
        return res;
    }

    public static int isObjectDisabled(String key, Reseller r) throws Exception {
        Globals accessor2 = getAccessor();
        int res = GlobalValueProvider.keyStatus(key, r, accessor2.getObject(key));
        if (res != 0) {
            return res;
        }
        ResourceDependences.ResourceCollection globResources = ResourceDependences.getAccessor().getGlobalResources(key);
        if (globResources != null) {
            Iterator orIter = globResources.iterator();
            while (orIter.hasNext()) {
                ResourceDependences.RequiredSet andSet = (ResourceDependences.RequiredSet) orIter.next();
                Iterator andIter = andSet.iterator();
                while (andIter.hasNext() && res == 0) {
                    String nKey = (String) andIter.next();
                    res = GlobalValueProvider.keyStatus(nKey, r, accessor2.getObject(nKey));
                    if (res != 0) {
                        break;
                    }
                }
                if (res == 0) {
                    return res;
                }
            }
        }
        return res;
    }

    public static int isObjectDisabled(String key, int reselPlanId) throws Exception {
        Globals accessor2 = getAccessor();
        int res = GlobalValueProvider.keyStatus(key, reselPlanId, accessor2.getObject(key));
        if (res != 0) {
            return res;
        }
        ResourceDependences.ResourceCollection globResources = ResourceDependences.getAccessor().getGlobalResources(key);
        if (globResources != null) {
            Iterator orIter = globResources.iterator();
            while (orIter.hasNext()) {
                ResourceDependences.RequiredSet andSet = (ResourceDependences.RequiredSet) orIter.next();
                Iterator andIter = andSet.iterator();
                while (andIter.hasNext() && res == 0) {
                    String nKey = (String) andIter.next();
                    res = GlobalValueProvider.keyStatus(nKey, reselPlanId, accessor2.getObject(nKey));
                    if (res != 0) {
                        break;
                    }
                }
                if (res == 0) {
                    return res;
                }
            }
        }
        return res;
    }

    public static int isObjectDisabled(String key, ServletRequest rq) throws Exception {
        GlobalObject go = GlobalObject.getResource(key);
        int res = 0;
        if (go != null) {
            res = GlobalValueProvider.keyStatus(key, rq, go);
            if (res != 0) {
                return res;
            }
        }
        ResourceDependences.ResourceCollection globResources = ResourceDependences.getAccessor().getGlobalResources(key);
        if (globResources != null) {
            Iterator orIter = globResources.iterator();
            while (orIter.hasNext()) {
                ResourceDependences.RequiredSet andSet = (ResourceDependences.RequiredSet) orIter.next();
                Iterator andIter = andSet.iterator();
                while (andIter.hasNext() && res == 0) {
                    String curKey = (String) andIter.next();
                    res = GlobalValueProvider.keyStatus(curKey, rq, GlobalObject.getResource(curKey));
                    if (res != 0) {
                        break;
                    }
                }
                if (res == 0) {
                    return res;
                }
            }
        }
        return res;
    }

    public static int isSetKeyDisabled(String setName, String key) throws Exception {
        GlobalKeySet os = getAccessor().getSet(setName);
        if (os != null) {
            return os.isKeyDisabled(key);
        }
        return 3;
    }

    public static int isSetKeyDisabled(String setName, String key, Reseller r) throws Exception {
        GlobalKeySet os = getAccessor().getSet(setName);
        if (os != null) {
            return os.isKeyDisabled(key, r);
        }
        return 3;
    }

    public static int isSetKeyDisabled(String setName, String key, int reselPlanId) throws Exception {
        GlobalKeySet os = getAccessor().getSet(setName);
        if (os != null) {
            return os.isKeyDisabled(key, reselPlanId);
        }
        return 3;
    }
}
