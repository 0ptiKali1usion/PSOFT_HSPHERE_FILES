package psoft.hsphere.p001ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.composite.Holder;
import psoft.hsphere.composite.Item;
import psoft.hsphere.exception.NoAvailableDedicatedServerException;
import psoft.hsphere.global.Globals;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.ds.DedicatedServerTemplate */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DedicatedServerTemplate.class */
public class DedicatedServerTemplate extends Holder implements TemplateHashModel {
    private String name;

    /* renamed from: os */
    private String f89os;
    private String cpu;
    private String ram;
    private String storage;
    private long resellerId;

    public DedicatedServerTemplate(long id, String name, String os, String cpu, String ram, String storage, long resellerId) {
        super(id);
        this.name = name;
        this.f89os = os;
        this.cpu = cpu;
        this.ram = ram;
        this.storage = storage;
        this.resellerId = resellerId;
    }

    public String getName() {
        return this.name;
    }

    public String getOS() {
        return this.f89os;
    }

    public String getCPU() {
        return this.cpu;
    }

    public String getRAM() {
        return this.ram;
    }

    public String getStorage() {
        return this.storage;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public int getFreeServers() {
        int freeServers = 0;
        synchronized (this) {
            for (Item it : getChildren().values()) {
                if ((it instanceof DedicatedServer) && ((DedicatedServer) it).getState() == DedicatedServerState.AVAILABLE) {
                    freeServers++;
                }
            }
        }
        return freeServers;
    }

    public boolean areServersAvailable() {
        return getFreeServers() > 0;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("name".equals(key)) {
            return new TemplateString(getName());
        }
        if ("os".equals(key)) {
            return new TemplateString(getOS());
        }
        if ("cpu".equals(key)) {
            return new TemplateString(getCPU());
        }
        if ("ram".equals(key)) {
            return new TemplateString(getRAM());
        }
        if ("storage".equals(key)) {
            return new TemplateString(getStorage());
        }
        if ("q_available_servers".equals(key)) {
            return new TemplateString(getFreeServers());
        }
        if ("has_available_servers".equals(key)) {
            return new TemplateString(areServersAvailable());
        }
        if ("status".equals(key)) {
            return Resource.STATUS_OK;
        }
        if ("ro".equals(key)) {
            return new TemplateString(isReadOnly());
        }
        if ("can_be_deleted".equals(key)) {
            return new TemplateString(canBeDeleted());
        }
        if ("reseller_id".equals(key)) {
            return new TemplateString(getResellerId());
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public synchronized boolean isReadOnly() {
        for (Item it : getChildren().values()) {
            if ((it instanceof DedicatedServer) && ((DedicatedServer) it).getState() != DedicatedServerState.AVAILABLE) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean canBeDeleted() {
        return getChildren().size() == 0;
    }

    public synchronized void save(String name, String os, String cpu, String ram, String storage) throws Exception {
        Session.getLog().debug("Inside DedicatedServerTemplate::save() ro = " + isReadOnly());
        if (!isReadOnly()) {
            DSFactory.saveDedicatedServerTemplate(getId(), name, os, cpu, ram, storage);
            this.name = name;
            this.f89os = os;
            this.cpu = cpu;
            this.ram = ram;
            this.storage = storage;
            return;
        }
        throw new HSUserException(Localizer.translateMessage("admin.ds.error_saving_ro_template", new String[]{getName()}));
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public synchronized DedicatedServer getAvailableServer() throws Exception {
        if (areServersAvailable()) {
            for (DedicatedServer ds : getChildren().values()) {
                if (ds.getState() == DedicatedServerState.AVAILABLE) {
                    return ds;
                }
            }
        }
        throw new NoAvailableDedicatedServerException("No available dedicated servers for template " + getId());
    }

    public TemplateModel FM_save(String name, String os, String cpu, String ram, String storage) throws Exception {
        Session.getLog().debug("Inside DedicatedServerTemplate::FM_save");
        save(name, os, cpu, ram, storage);
        return this;
    }

    public boolean isAccessible() {
        try {
            return Globals.isSetKeyDisabled("ds_templates", String.valueOf(getId())) == 0;
        } catch (Exception ex) {
            Session.getLog().error("An error occured while testing accessability of the " + getId() + " dedicated server template:", ex);
            return false;
        }
    }

    public String getFullDescription() throws Exception {
        return Localizer.translateMessage("dst.name_mes", new String[]{getName()}) + "\n" + Localizer.translateMessage("dst.os_name_mes", new String[]{getOS()}) + "\n" + Localizer.translateMessage("dst.cpu_mes", new String[]{getCPU()}) + "\n" + Localizer.translateMessage("dst.ram_mes", new String[]{getRAM()}) + "\n" + Localizer.translateMessage("dst.storage_mes", new String[]{getStorage()});
    }
}
