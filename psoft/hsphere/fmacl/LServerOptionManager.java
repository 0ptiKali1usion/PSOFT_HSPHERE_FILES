package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.hsphere.admin.optionmanipulators.DefaultOptionManipulator;
import psoft.hsphere.admin.optionmanipulators.OptionManipulator;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.TemplateXML;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/LServerOptionManager.class */
public class LServerOptionManager implements TemplateHashModel {
    protected static LServerOptionManager lServerOptionManager;
    protected static final String XPATH_OPTIONSWITH_MANIPULATOR_QUERY = "/options/group/option[@manipulator != '']";
    protected static Hashtable optionManipulators;
    protected final Class[] constructorParameters = {String.class, Boolean.class};
    public static final TemplateString STATUS_OK = new TemplateString("OK");
    protected static boolean isManipulatorMapInitialized = false;
    protected static final Class DEFAULT_MANIPULATOR = DefaultOptionManipulator.class;

    public Hashtable getOptionManipulators() {
        synchronized (LServerOptionManager.class) {
            try {
                loadManipulators();
            } catch (Exception ex) {
                Session.getLog().error("Unable to load logical server option manipulators default manipulator will be used ", ex);
            }
        }
        return optionManipulators;
    }

    public LServerOptionManager() {
        lServerOptionManager = this;
    }

    public LServerOptionManager getManager() {
        return lServerOptionManager;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return STATUS_OK;
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_getStatus() {
        return STATUS_OK;
    }

    protected static synchronized void loadManipulators() throws TransformerException, IOException, SAXException {
        if (isManipulatorMapInitialized) {
            return;
        }
        if (optionManipulators == null) {
            optionManipulators = new Hashtable();
        }
        NodeIterator i = XPathAPI.selectNodeIterator(getConfig(), XPATH_OPTIONSWITH_MANIPULATOR_QUERY);
        Node nextNode = i.nextNode();
        while (true) {
            Node node = nextNode;
            if (node != null) {
                String optionName = node.getAttributes().getNamedItem("name").getNodeValue();
                String manipulatorClass = node.getAttributes().getNamedItem("manipulator").getNodeValue();
                addManipulator(optionName, manipulatorClass);
                nextNode = i.nextNode();
            } else {
                isManipulatorMapInitialized = true;
                return;
            }
        }
    }

    private static void addManipulator(String optionName, String manipulatorClass) {
        if (optionName == null || "".equals(optionName) || manipulatorClass == null || "".equals(manipulatorClass)) {
            return;
        }
        try {
            Class manipulator = Class.forName(manipulatorClass);
            optionManipulators.put(optionName, manipulator);
        } catch (ClassNotFoundException e) {
            Session.getLog().error("Unable to initialize Logical server option manipulator name:" + optionName + " class:" + manipulatorClass);
        }
    }

    protected OptionManipulator instatiateManipulator(String optionName, boolean useXML) {
        Class maipulatorClass = (Class) getOptionManipulators().get(optionName);
        if (maipulatorClass == null) {
            maipulatorClass = DEFAULT_MANIPULATOR;
        }
        if (OptionManipulator.class.isAssignableFrom(maipulatorClass)) {
            try {
                Constructor constructor = maipulatorClass.getConstructor(this.constructorParameters);
                return (OptionManipulator) constructor.newInstance(optionName, new Boolean(useXML));
            } catch (Exception e) {
                Session.getLog().error("Unable to create lserver option manipulator for " + optionName + " option class:" + maipulatorClass.getName(), e);
                return null;
            }
        }
        return null;
    }

    public OptionManipulator getManipulator(String optionName, boolean useXML) {
        return instatiateManipulator(optionName, useXML);
    }

    public TemplateModel FM_getManipulator(String optionName, boolean useXML) {
        return getManipulator(optionName, useXML);
    }

    public static Document getConfig() throws TransformerException, IOException, SAXException {
        return XMLManager.getXML("LS_OPTIONS");
    }

    public TemplateModel FM_getConfig() throws Exception, IOException, SAXException {
        return new TemplateXML(getConfig(), "options");
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
