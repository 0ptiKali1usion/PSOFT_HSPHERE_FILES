package psoft.hsphere.resource.admin;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.manager.Entity;
import psoft.hsphere.manager.Manager;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/RegistrarManager.class */
public class RegistrarManager extends Resource {
    public RegistrarManager(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public RegistrarManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_setActive(int id, String tld) throws Exception {
        Registrar registrar = (Registrar) getManager().getEntity(id);
        getManager().activateEntity(id, tld);
        return this;
    }

    public TemplateModel FM_deleteRegistrar(int id) throws Exception {
        getManager().deleteEntity(id);
        return this;
    }

    public TemplateModel FM_listActiveKeys() throws Exception {
        return new TemplateList(DomainRegistrar.getActiveTLDs());
    }

    public TemplateModel FM_getRegistrar(String tld) throws Exception {
        return new TemplateMap(getRegistrar(getManager().getEntity(tld).getId()));
    }

    public TemplateModel FM_get(int id) throws Exception {
        return new TemplateMap(getRegistrar(id));
    }

    protected Map getRegistrar(int id) throws Exception {
        Entity e = getManager().getEntity(id);
        Map res = new HashMap();
        for (String key : e.getKeys()) {
            Object value = e.getValues(key);
            if (((String[]) value).length == 1) {
                res.put(key, e.get(key));
            } else {
                String[] intermediate = (String[]) value;
                ArrayList arrayList = new ArrayList();
                for (String str : intermediate) {
                    arrayList.add(str);
                }
                res.put(key, arrayList);
            }
        }
        res.put("description", e.getDescription());
        res.put("id", Integer.toString(id));
        return res;
    }

    public TemplateModel FM_getRegistrarTlds(int id) throws Exception {
        Entity e = getManager().getEntity(id);
        String[] tlds = ((Registrar) e).getSupportedTLDs();
        TemplateList tldsList = new TemplateList();
        for (String str : tlds) {
            tldsList.add((TemplateModel) new TemplateString(str));
        }
        return tldsList;
    }

    public Manager getManager() throws Exception {
        return DomainRegistrar.getManager();
    }

    public TemplateModel FM_resetActive(int id, String tld) throws Exception {
        getManager().deactivateEntity(id, tld);
        return this;
    }

    public TemplateModel FM_getTLDs() throws Exception {
        String[] SUPPORTED_TLD = {"com", "net", "org", "name", "info", "biz", "us", "ca", "cc", "tv", "bz", "nu", "ws"};
        return new TemplateList(Arrays.asList(SUPPORTED_TLD));
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/RegistrarManager$RegistrarCreator.class */
    class RegistrarCreator implements TemplateMethodModel {
        HashMap map = new HashMap();

        RegistrarCreator() {
            RegistrarManager.this = r5;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            String[] values;
            try {
                List l2 = HTMLEncoder.decode(l);
                if (!l2.isEmpty()) {
                    Iterator i = l2.iterator();
                    while (i.hasNext()) {
                        Object key = i.next();
                        Object value = i.next();
                        String endValue = null;
                        if (value != null) {
                            endValue = value.toString();
                        }
                        String[] values2 = (String[]) this.map.get(key);
                        if (values2 == null) {
                            values = new String[]{endValue};
                        } else {
                            List<String> tmpList = new ArrayList(Arrays.asList(values2));
                            tmpList.add(value);
                            values = new String[tmpList.size()];
                            int index = 0;
                            for (String str : tmpList) {
                                int i2 = index;
                                index++;
                                values[i2] = str;
                            }
                        }
                        this.map.put(key, values);
                    }
                    return null;
                }
                String[] className = (String[]) this.map.get("CLASS_NAME");
                this.map.remove("CLASS_NAME");
                String[] description = (String[]) this.map.get("DESCRIPTION");
                this.map.remove("DESCRIPTION");
                RegistrarManager.this.getManager().addEntity(className[0], description[0], 0L, this.map);
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating registrar", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/RegistrarManager$RegistrarUpdator.class */
    class RegistrarUpdator implements TemplateMethodModel {
        HashMap map = new HashMap();

        RegistrarUpdator() {
            RegistrarManager.this = r5;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            String[] values;
            try {
                List l2 = HTMLEncoder.decode(l);
                if (!l2.isEmpty()) {
                    Iterator i = l2.iterator();
                    while (i.hasNext()) {
                        Object key = i.next();
                        Object value = i.next();
                        String endValue = null;
                        if (value != null) {
                            endValue = value.toString();
                        }
                        String[] values2 = (String[]) this.map.get(key);
                        if (values2 == null) {
                            values = new String[]{endValue};
                        } else {
                            List<String> tmpList = new ArrayList(Arrays.asList(values2));
                            tmpList.add(value);
                            values = new String[tmpList.size()];
                            int index = 0;
                            for (String str : tmpList) {
                                int i2 = index;
                                index++;
                                values[i2] = str;
                            }
                        }
                        this.map.put(key, values);
                    }
                    return null;
                }
                int id = Integer.parseInt(((String[]) this.map.get("ID"))[0]);
                this.map.remove("ID");
                String[] className = (String[]) this.map.get("CLASS_NAME");
                this.map.remove("CLASS_NAME");
                String[] description = (String[]) this.map.get("DESCRIPTION");
                this.map.remove("DESCRIPTION");
                RegistrarManager.this.getManager().updateEntity(id, className[0], description[0], 0L, this.map);
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error creating registrar", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    public TemplateModel FM_list() throws Exception {
        List res = new ArrayList();
        for (Entity e : getManager().getEntities()) {
            res.add(Integer.toString(e.getId()));
        }
        return new TemplateList(res);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "new".equals(key) ? new RegistrarCreator() : "update".equals(key) ? new RegistrarUpdator() : super.get(key);
    }
}
