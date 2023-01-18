package psoft.hsphere.global;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.xml.XMLElementTracker;

/* loaded from: hsphere.zip:psoft/hsphere/global/ResourceDependences.class */
public class ResourceDependences {
    protected int initErrors = 0;
    protected XMLElementTracker trackedElement;
    protected static final String CLASS_DESCRIPTION = "Resource Dependences";
    protected static final String CFG_FILE = "the 'resource_dependences.xml' config file";
    protected static final String AND_DELIMITER = " & ";
    protected static final String OR_DELIMITER = " | ";
    protected static ResourceDependences accessor = null;

    public static ResourceDependences getAccessor() throws Exception {
        if (accessor == null) {
            accessor = new ResourceDependences();
            try {
                accessor.initialize();
            } catch (Exception ex) {
                accessor = null;
                Session.getLog().error("Unable to initialize 'Resource dependences'.", ex);
                throw ex;
            }
        }
        return accessor;
    }

    protected synchronized void initialize() throws Exception {
        Session.getLog().debug("Resource Dependences. Started to parse the 'resource_dependences.xml' config file");
        this.trackedElement = new XMLElementTracker();
        try {
            Document gr = XMLManager.getXML("RESOURCE_DEPENDENCES_CONFIG");
            Element root = gr.getDocumentElement();
            this.trackedElement.setElement(root);
            NodeList resNodes = root.getElementsByTagName("resource");
            for (int ri = 0; ri < resNodes.getLength(); ri++) {
                parseResource((Element) resNodes.item(ri));
            }
            postResourceInit();
            Session.getLog().debug("Resource Dependences. Finished to parse the 'resource_dependences.xml' config file. " + (this.initErrors == 0 ? "No errors occured." : String.valueOf(this.initErrors) + " error(s) occured."));
        } catch (Exception e) {
            String tag = this.trackedElement.getTagSequence();
            Session.getLog().error("Resource Dependences. Error occured during parse the 'resource_dependences.xml' config file." + (tag != null ? " Problem XML tag:\n" + tag : "") + "\n", e);
            throw e;
        }
    }

    private void parseResource(Element resourceNode) throws Exception {
        Element vElement;
        this.trackedElement.setElement(resourceNode);
        String resName = resourceNode.getAttribute("name");
        NodeList variantNodes = resourceNode.getElementsByTagName("variant");
        VariantCollection requiredVariants = new VariantCollection();
        int vLength = variantNodes.getLength();
        if (vLength == 0) {
            vElement = resourceNode;
        } else {
            vElement = (Element) variantNodes.item(0);
        }
        int vi = 1;
        while (true) {
            RequiredSet requiredAnd = new RequiredSet();
            String parent = null;
            NodeList reqNodes = vElement.getElementsByTagName("requires");
            for (int ii = 0; ii < reqNodes.getLength(); ii++) {
                Element reqResource = (Element) reqNodes.item(ii);
                this.trackedElement.setElement(reqResource);
                String reqName = reqResource.getAttribute("name");
                if (resName.equals(reqName)) {
                    processSelfDependentError(resName);
                } else {
                    if (!requiredAnd.contains(reqName)) {
                        requiredAnd.addResource(reqName);
                    }
                    if ("parent".equals(reqResource.getAttribute("relation"))) {
                        parent = reqName;
                    }
                }
            }
            requiredVariants.addVariant(requiredAnd, parent);
            if (vi < vLength) {
                vElement = (Element) variantNodes.item(vi);
                vi++;
            } else {
                DependentResource.addResource(resName, requiredVariants);
                return;
            }
        }
    }

    public ResourceCollection getGlobalResources(String typeName) throws Exception {
        DependentResource dr = DependentResource.getResource(typeName);
        if (dr != null) {
            return dr.getGlobalResources();
        }
        return null;
    }

    public VariantCollection getRequiredResources(String typeName) throws Exception {
        DependentResource dr = DependentResource.getResource(typeName);
        if (dr != null) {
            return dr.getRequiredResources();
        }
        return null;
    }

    public TemplateList getGlobalsAsTemplateList(String typeName) throws Exception {
        DependentResource dr = DependentResource.getResource(typeName);
        if (dr != null) {
            ResourceCollection grc = dr.getGlobalResources();
            Globals grAccessor = Globals.getAccessor();
            TemplateList resList = new TemplateList();
            Iterator andIter = grc.iterator();
            while (andIter.hasNext()) {
                TemplateList andList = new TemplateList();
                RequiredSet rs = (RequiredSet) andIter.next();
                Iterator orIter = rs.iterator();
                while (orIter.hasNext()) {
                    GlobalObject gr = grAccessor.getObject((String) orIter.next());
                    andList.add((TemplateModel) gr);
                }
                if (!andList.isEmpty()) {
                    resList.add((TemplateModel) andList);
                }
            }
            return resList;
        }
        return null;
    }

    public TemplateList getRequiredsAsTemplateList(String typeName) throws Exception {
        DependentResource dr = DependentResource.getResource(typeName);
        if (dr != null) {
            ResourceCollection rrc = dr.getRequiredResources();
            TemplateList resList = new TemplateList();
            Iterator andIter = rrc.iterator();
            while (andIter.hasNext()) {
                TemplateList andList = new TemplateList();
                RequiredSet rs = (RequiredSet) andIter.next();
                Iterator orIter = rs.iterator();
                while (orIter.hasNext()) {
                    andList.add((String) orIter.next());
                }
                if (!andList.isEmpty()) {
                    resList.add((TemplateModel) andList);
                }
            }
            return resList;
        }
        return null;
    }

    protected void postResourceInit() throws Exception {
        Enumeration e = DependentResource.getResourceEnumerator();
        while (e.hasMoreElements()) {
            ((DependentResource) e.nextElement()).getGlobalResources();
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/global/ResourceDependences$DependentResource.class */
    public static class DependentResource implements TemplateHashModel {
        protected static Hashtable resHash = new Hashtable();
        protected String name;
        protected VariantCollection requiredResources;
        protected boolean isInitialized = false;
        protected boolean initializing = false;
        protected ResourceCollection globalResources = null;

        protected DependentResource(String name, VariantCollection requiredResources) {
            this.name = name;
            this.requiredResources = requiredResources;
        }

        public static void addResource(String name, VariantCollection requiredResources) throws Exception {
            if (name == null || "".equals(name)) {
                throw new Exception("Field 'name' cannot be empty.");
            }
            DependentResource dr = new DependentResource(name, requiredResources);
            resHash.put(name, dr);
        }

        private synchronized void checkInitialized() throws Exception {
            if (!this.isInitialized) {
                this.initializing = true;
                try {
                    Globals accessor = Globals.getAccessor();
                    this.globalResources = new ResourceCollection();
                    Iterator vIter = this.requiredResources.iterator();
                    while (vIter.hasNext()) {
                        ResourceCollection curCollection = new ResourceCollection();
                        RequiredSet variant = (RequiredSet) vIter.next();
                        Iterator reqIter = variant.iterator();
                        while (reqIter.hasNext()) {
                            String resName = (String) reqIter.next();
                            if (accessor.isGlobalObject(resName)) {
                                curCollection.makeConjunction(resName);
                            } else {
                                DependentResource dr = getResource(resName);
                                if (dr != null) {
                                    if (dr.initializing) {
                                        ResourceDependences.processSelfDependentError(dr.getName());
                                    } else {
                                        ResourceCollection ancResCol = dr.getGlobalResources();
                                        if (!ancResCol.isEmpty()) {
                                            curCollection.makeConjunction(ancResCol);
                                        }
                                    }
                                }
                            }
                        }
                        this.globalResources.addCollection(curCollection);
                    }
                    this.isInitialized = true;
                } finally {
                    this.initializing = false;
                }
            }
        }

        public String getName() {
            return this.name;
        }

        public static DependentResource getResource(String name) {
            return (DependentResource) resHash.get(name);
        }

        public VariantCollection getRequiredResources() {
            return this.requiredResources;
        }

        public ResourceCollection getGlobalResources() throws Exception {
            checkInitialized();
            return this.globalResources;
        }

        public RequiredSet getRequiredByParent(String parent) {
            return this.requiredResources.getRequiredSetByParent(parent);
        }

        public static Enumeration getResourceEnumerator() {
            return resHash.elements();
        }

        public TemplateModel get(String key) throws TemplateModelException {
            if (key.equals("name")) {
                return new TemplateString(this.name);
            }
            return null;
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/global/ResourceDependences$RequiredSet.class */
    public static class RequiredSet extends TreeSet {
        public RequiredSet() {
        }

        public RequiredSet(String name) {
            add(name);
        }

        public void addResource(String name) {
            if (!contains(name)) {
                add(name);
            }
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            StringBuffer res = new StringBuffer();
            Iterator iter = iterator();
            if (iter.hasNext()) {
                res.append(iter.next().toString());
            }
            while (iter.hasNext()) {
                res.append(ResourceDependences.AND_DELIMITER).append(iter.next().toString());
            }
            return res.toString();
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/global/ResourceDependences$ResourceCollection.class */
    public static class ResourceCollection {
        Collection collection = new ArrayList();

        public synchronized void addSet(RequiredSet set) {
            if (!this.collection.isEmpty()) {
                Iterator iter = this.collection.iterator();
                while (true) {
                    if (!iter.hasNext()) {
                        break;
                    }
                    RequiredSet rs = (RequiredSet) iter.next();
                    if (rs.containsAll(set)) {
                        this.collection.remove(rs);
                        break;
                    }
                }
            }
            this.collection.add(set);
        }

        public void addCollection(ResourceCollection rc) {
            if (rc == null || rc.isEmpty()) {
                return;
            }
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                addSet((RequiredSet) iter.next());
            }
        }

        public boolean contains(RequiredSet obj) {
            for (RequiredSet requiredSet : this.collection) {
                if (requiredSet.containsAll(obj)) {
                    return true;
                }
            }
            return false;
        }

        public synchronized void makeConjunction(String resName) {
            if (resName == null) {
                return;
            }
            if (!this.collection.isEmpty()) {
                for (RequiredSet requiredSet : this.collection) {
                    requiredSet.addResource(resName);
                }
                return;
            }
            this.collection.add(new RequiredSet(resName));
        }

        public synchronized void makeConjunction(RequiredSet set) {
            if (set == null || set.isEmpty()) {
                return;
            }
            if (!this.collection.isEmpty()) {
                Iterator iter = this.collection.iterator();
                while (iter.hasNext()) {
                    RequiredSet curSet = (RequiredSet) iter.next();
                    Iterator is = set.iterator();
                    while (iter.hasNext()) {
                        curSet.addResource((String) is.next());
                    }
                }
                return;
            }
            this.collection.add(set);
        }

        public synchronized void makeConjunction(ResourceCollection rc) {
            if (rc == null || rc.isEmpty()) {
                return;
            }
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                makeConjunction((RequiredSet) iter.next());
            }
        }

        public Iterator iterator() {
            return this.collection.iterator();
        }

        public boolean isEmpty() {
            return this.collection.isEmpty();
        }

        public String toString() {
            StringBuffer res = new StringBuffer();
            Iterator iter = this.collection.iterator();
            if (iter.hasNext()) {
                res.append(((RequiredSet) iter.next()).toString());
            }
            while (iter.hasNext()) {
                res.append(ResourceDependences.OR_DELIMITER).append(((RequiredSet) iter.next()).toString());
            }
            return res.toString();
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/global/ResourceDependences$VariantCollection.class */
    public static class VariantCollection extends ResourceCollection {
        Map parentMap = new HashMap();

        VariantCollection() {
        }

        public void addVariant(RequiredSet set, String parent) throws Exception {
            addSet(set);
            if (parent != null) {
                this.parentMap.put(parent, set);
            }
        }

        public Map getParentMap() {
            return this.parentMap;
        }

        public RequiredSet getRequiredSetByParent(String parent) {
            return (RequiredSet) this.parentMap.get(parent);
        }
    }

    public static void processSelfDependentError(String name) {
        String errMes = "Critical error. Resource '" + name + "' depends on itself.";
        Ticket.create(new Exception(errMes), null, "Incorrect resource depences. Immediately check the 'resource_dependences.xml' config file.");
        Session.getLog().error("Resource Dependences. " + errMes);
    }
}
