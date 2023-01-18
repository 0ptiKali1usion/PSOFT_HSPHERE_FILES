package psoft.hsphere.resource.admin.p002ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.User;
import psoft.hsphere.composite.Item;
import psoft.hsphere.exception.DSTemplateNotFoundException;
import psoft.hsphere.exception.DServerNotFoundException;
import psoft.hsphere.p001ds.DSFactory;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DSNetInterface;
import psoft.hsphere.p001ds.DSNetInterfaceManager;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.p001ds.NetSwitch;
import psoft.hsphere.p001ds.NetSwitchManager;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;

/* renamed from: psoft.hsphere.resource.admin.ds.DSManager */
/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ds/DSManager.class */
public class DSManager extends Resource {
    DSHolder dsh;

    public DSManager(int type, Collection c) throws Exception {
        super(type, c);
        this.dsh = null;
        this.dsh = new DSHolder(Session.getResellerId());
    }

    public DSManager(ResourceId id) throws Exception {
        super(id);
        this.dsh = null;
        this.dsh = new DSHolder(Session.getResellerId());
    }

    public List getServersList() throws Exception {
        return this.dsh.getAccessibleDedicatedServers();
    }

    public TemplateHashModel FM_getDSTemplate(long id) throws Exception {
        return getManageableDSTemplate(id);
    }

    public TemplateHashModel FM_getDServer(long id) throws Exception {
        return getAccessibleDedicatedServer(id);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("servers".equals(key)) {
            List servers = null;
            try {
                servers = getServersList();
            } catch (Exception ex) {
                Session.getLog().error("Unable to get list of dedicated servers:", ex);
            }
            return new TemplateList(servers);
        } else if ("templates".equals(key)) {
            List templates = null;
            try {
                templates = getAccessibleDedicatedServerTemplates();
            } catch (Exception ex2) {
                Session.getLog().error("Unable to get list of dedicated server templates:", ex2);
            }
            return new TemplateList(templates);
        } else if ("all_ds_states".equals(key)) {
            return new TemplateList(DedicatedServer.getAllStrStates());
        } else {
            return super.get(key);
        }
    }

    public DedicatedServerTemplate addDSTemplate(String name, String os, String cpu, String ram, String storage) throws Exception {
        DedicatedServerTemplate dst = DSFactory.createDSTemplate(name, os, cpu, ram, storage);
        this.dsh.addChild(dst);
        return dst;
    }

    public DedicatedServer addDServer(String name, String rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, String mainIP, long templateId) throws Exception {
        DedicatedServerTemplate dst = null;
        if (templateId > 0) {
            dst = this.dsh.getDSTemplate(templateId);
            os = dst.getOS();
            cpu = dst.getCPU();
            ram = dst.getRAM();
            storage = dst.getStorage();
        }
        DedicatedServer ds = DSFactory.createDedicatedServer(name, rebootURL, internalId, superUser, suPasswd, setup, recurrent, os, cpu, ram, storage, mainIP, templateId);
        if (dst == null) {
            this.dsh.addChild(ds);
        } else {
            dst.addChild(ds);
        }
        return ds;
    }

    public TemplateModel FM_addDServer(String name, String rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, String mainIP, long templateId) throws Exception {
        return addDServer(name, rebootURL, internalId, superUser, suPasswd, setup, recurrent, os, cpu, ram, storage, mainIP, templateId);
    }

    public TemplateModel FM_addDSTemplate(String name, String os, String cpu, String ram, String storage) throws Exception {
        return addDSTemplate(name, os, cpu, ram, storage);
    }

    public List getAccessibleDedicatedServerTemplates() throws Exception {
        return DSHolder.getAccessibleDSTemplates();
    }

    public DedicatedServer getAccessibleDedicatedServer(long id) throws Exception {
        return DSHolder.getAcessibleDedicatedServer(id);
    }

    public TemplateHashModel getAccessibleDedicatedServerTemplate(long id) throws Exception {
        DedicatedServerTemplate dst = DSHolder.getAccessibleDSTemplate(id);
        if (dst != null && dst.getResellerId() != Session.getResellerId()) {
            return new DSAccessWrapper(dst);
        }
        return dst;
    }

    public TemplateHashModel getManageableDSTemplate(long id) throws Exception {
        DedicatedServerTemplate dst = DSHolder.getManageableDSTemplate(id);
        if (dst != null && dst.getResellerId() != Session.getResellerId()) {
            return new DSAccessWrapper(dst);
        }
        return dst;
    }

    public void delDedicatedServer(long id) throws Exception {
        DedicatedServer ds = this.dsh.getDServer(id);
        if (ds != null) {
            synchronized (ds) {
                if (ds.canBeDeleted()) {
                    List<DSNetInterface> netInterfaces = ds.getNetInterfaces();
                    if (netInterfaces != null) {
                        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
                        for (DSNetInterface dSNetInterface : netInterfaces) {
                            netInterfaceMan.delInterface(dSNetInterface);
                        }
                    }
                    DSFactory.deleteItem(ds);
                    if (ds.isTemplatedServer()) {
                        ds.getParent().delChild(ds);
                    }
                } else {
                    throw new DServerNotFoundException("Dedicated server with id " + id + (ds == null ? " not found for reseller " + Session.getResellerId() : " can not be deleted"));
                }
            }
        }
    }

    public void delDedicatedServerTemplate(long id) throws Exception {
        DedicatedServerTemplate dst = this.dsh.getDSTemplate(id);
        if (dst != null) {
            synchronized (dst) {
                if (dst.canBeDeleted()) {
                    DSFactory.deleteItem(dst);
                    dst.delete();
                } else {
                    throw new DSTemplateNotFoundException("Dedicated server with id " + id + (dst == null ? " not found " : " can not be deleted because there are dedicated servers assigned to it."));
                }
            }
        }
    }

    public TemplateModel FM_delDedicatedServer(long id) throws Exception {
        delDedicatedServer(id);
        return this;
    }

    public TemplateModel FM_delDedicatedServerTemplate(long id) throws Exception {
        delDedicatedServerTemplate(id);
        return this;
    }

    public TemplateModel FM_addChildToAccount(long account_id, String mod, long ds_id, String state_id) throws Exception {
        addChildToAccount(account_id, mod, ds_id, state_id);
        return this;
    }

    protected void addChildToAccount(long account_id, String mod, long ds_id, String state_id) throws Exception {
        Session.save();
        try {
            Account a = Account.getAccount(account_id);
            User u = a.getUser();
            Session.setUser(u);
            Session.setAccount(a);
            ArrayList args = new ArrayList();
            args.add(String.valueOf(ds_id));
            args.add(state_id);
            a.addChild("ds", mod, args);
        } finally {
            Session.restore();
        }
    }

    public TemplateModel FM_deleteDSResource(long ds_id, long account_id) throws Exception {
        deleteDSResource(ds_id, account_id);
        return this;
    }

    public void deleteDSResource(long ds_id, long account_id) throws Exception {
        long sessionResellerId = Session.getResellerId();
        Session.save();
        try {
            try {
                Account a = Account.getAccount(account_id);
                User u = a.getUser();
                Session.setUser(u);
                Session.setAccount(a);
                DedicatedServerResource dsRes = getResourceByDSId(ds_id);
                long dsResellerId = dsRes.getDSObject().getResellerId();
                if (dsResellerId != sessionResellerId && sessionResellerId != a.getResellerId()) {
                    throw new Exception(Localizer.translateMessage("admin.ds.not_authorized_cancel"));
                }
                Session.getLog().debug("Cancellation of the server #" + dsRes.getDSObject().getId());
                dsRes.delete(false);
            } catch (Exception e) {
                Session.getLog().error("Error deleting Dedicated Server Resource", e);
                throw new HSUserException("Error while deleting a dedicated server resource: " + e.getMessage());
            }
        } finally {
            Session.restore();
        }
    }

    public DedicatedServerResource getResourceByDSId(long ds_id) throws Exception {
        DedicatedServer ds = this.dsh.getDServer(ds_id);
        long rid = ds.getRid();
        Session.getLog().debug("ds internal id = " + ds.getInternalId());
        Session.getLog().debug("rid = " + rid);
        ResourceId rId = new ResourceId(rid, ResourceId.getTypeById(rid));
        Session.getLog().debug("rId =" + rId);
        DedicatedServerResource res = (DedicatedServerResource) DedicatedServerResource.get(rId);
        Session.getLog().debug("res = " + res);
        return res;
    }

    public TemplateModel FM_addExtraIP(String ip, long serverId) throws Exception {
        addExtraIP(ip, serverId);
        return this;
    }

    public void addExtraIP(String ip, long serverId) throws Exception {
        DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(serverId);
        try {
            Session.save();
            Session.setUser(ds.getServerOwnerId().getAccount().getUser());
            Session.setAccount(ds.getServerOwnerId().getAccount());
            ds.getServerOwner().addChild("ds_ip", "", Arrays.asList(ip));
        } finally {
            Session.restore();
        }
    }

    /* renamed from: psoft.hsphere.resource.admin.ds.DSManager$DSAccessWrapper */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ds/DSManager$DSAccessWrapper.class */
    public class DSAccessWrapper implements TemplateHashModel {
        Item dsObject;

        public DSAccessWrapper(Item i) {
            DSManager.this = r4;
            this.dsObject = i;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel get(String key) throws TemplateModelException {
            if (this.dsObject instanceof DedicatedServer) {
                return ((DedicatedServer) this.dsObject).get(key);
            }
            if (this.dsObject instanceof DedicatedServerTemplate) {
                return ((DedicatedServerTemplate) this.dsObject).get(key);
            }
            return null;
        }
    }

    public List getMrtgLServersList() throws Exception {
        return HostManager.getHostsByGroupType(25);
    }

    public TemplateModel FM_getMrtgLServersList() throws Exception {
        List l = getMrtgLServersList();
        return l != null ? new TemplateList(l) : new TemplateList();
    }

    public TemplateModel FM_addNetSwitch(String device, String communityName, String description, String webURL, int mrtgHostId) throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        HostEntry mrtgHost = HostManager.getHost(mrtgHostId);
        return nsm.addNetSwitch(device, communityName, description, webURL, mrtgHost);
    }

    public TemplateModel FM_updateNetSwitch(long switchId, String device, String communityName, String description, String webURL, int mrtgHostId) throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        HostEntry mrtgHost = HostManager.getHost(mrtgHostId);
        return nsm.updateNetSwitch(switchId, device, communityName, description, webURL, mrtgHost);
    }

    public TemplateModel FM_getNetSwitch(long switchId) throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        return nsm.getNetSwitch(switchId);
    }

    public TemplateModel FM_delNetSwitch(long switchId) throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        nsm.delNetSwitch(switchId);
        return new TemplateOKResult();
    }

    public TemplateModel FM_netSwitchList() throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        return new TemplateList(nsm.getNetSwitches());
    }

    public TemplateModel FM_addNetInterface(long dsId, long nsId, int port, String description) throws Exception {
        DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(dsId);
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        NetSwitch netSwitch = nsm.getNetSwitch(nsId);
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return netInterfaceMan.addInterface(ds, netSwitch, port, description);
    }

    public TemplateModel FM_updateNetInterface(long interfaceId, String description) throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return netInterfaceMan.updateInterface(interfaceId, description);
    }

    public TemplateModel FM_getNetInterface(long interfaceId) throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return netInterfaceMan.getInterface(interfaceId);
    }

    public TemplateModel FM_getNetInterfacesByDS(long dsId) throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return new TemplateList(netInterfaceMan.getInterfacesByDS(dsId));
    }

    public TemplateModel FM_delNetInterface(long interfaceId) throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        netInterfaceMan.delInterface(interfaceId);
        return new TemplateOKResult();
    }

    public TemplateModel FM_netInterfaceList() throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return new TemplateList(netInterfaceMan.getAllAccessibleInterfaces());
    }

    public TemplateModel FM_scheduleCancellation(long dsId, String dateStr) throws Exception {
        Date d;
        DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(dsId);
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateLabel("admin.ds.server_not_accessible_mes", new String[]{String.valueOf(dsId)}));
        }
        if (dateStr != null && !"".equals(dateStr)) {
            try {
                d = new Date(HsphereToolbox.parseShortDate(dateStr).getTime());
            } catch (ParseException pe) {
                Session.getLog().debug("Unable to parse the date string '" + dateStr + "'.", pe);
                return new TemplateErrorResult(Localizer.translateLabel("admin.ds.cancel_server_wrong_date_mes", new String[]{dateStr}));
            }
        } else {
            Session.save();
            try {
                try {
                    Account a = ds.getServerOwnerId().getAccount();
                    Session.setUser(a.getUser());
                    Session.setAccount(a);
                    Calendar ca = TimeUtils.getCalendar(a.getPeriodEnd());
                    ca.add(5, -1);
                    d = TimeUtils.dropMinutes(ca.getTime());
                    Session.restore();
                } catch (Exception ex2) {
                    Session.getLog().error("Problem with getting account of the user who has taken the dedicated server #" + dsId, ex2);
                    TemplateErrorResult templateErrorResult = new TemplateErrorResult(Localizer.translateLabel("admin.ds.unable_get_account_mes", new String[]{String.valueOf(dsId)}));
                    Session.restore();
                    return templateErrorResult;
                }
            } catch (Throwable th) {
                Session.restore();
                throw th;
            }
        }
        ds.scheduleCancellation(d);
        return new TemplateOKResult();
    }

    public TemplateModel FM_discardCancellation(long dsId) throws Exception {
        DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(dsId);
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateLabel("admin.ds.server_not_accessible_mes", new String[]{String.valueOf(dsId)}));
        }
        ds.discardCancellation();
        return new TemplateOKResult();
    }
}
