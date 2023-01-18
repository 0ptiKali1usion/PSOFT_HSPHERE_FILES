package psoft.hsphere.p001ds;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.composite.Holder;
import psoft.hsphere.composite.Item;
import psoft.hsphere.exception.DServerNotFoundException;
import psoft.hsphere.global.GlobalKeySet;
import psoft.hsphere.global.Globals;

/* renamed from: psoft.hsphere.ds.DSHolder */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DSHolder.class */
public class DSHolder {
    private static Hashtable resellerMap = new Hashtable();
    private Holder holder;

    public DSHolder(long resellerId) throws Exception {
        this.holder = null;
        this.holder = getResellerHolder(resellerId);
    }

    public DSHolder() throws Exception {
        this.holder = null;
        this.holder = getResellerHolder();
    }

    protected static Holder getResellerHolder(long resellerId) throws Exception {
        Holder result = (Holder) resellerMap.get(new Long(resellerId));
        if (result == null) {
            result = DSFactory.buildResellerHolder(resellerId);
            resellerMap.put(new Long(resellerId), result);
        }
        return result;
    }

    public static Holder getResellerHolder() throws Exception {
        return getResellerHolder(Session.getResellerId());
    }

    public static Holder getAdminHolder() throws Exception {
        return getResellerHolder(1L);
    }

    public List getDSTemplates() throws Exception {
        ArrayList l = new ArrayList();
        for (Item it : this.holder.getAllChildren()) {
            if (it instanceof DedicatedServerTemplate) {
                l.add((DedicatedServerTemplate) it);
            }
        }
        return l;
    }

    public List getDServers() throws Exception {
        ArrayList l = new ArrayList();
        for (Item it : this.holder.getAllChildren()) {
            if (it instanceof DedicatedServer) {
                l.add((DedicatedServer) it);
            }
        }
        return l;
    }

    public DedicatedServer getDServer(long id) throws Exception {
        Item i = Holder.findChild(id);
        if ((i instanceof DedicatedServer) && this.holder.getAllChildren().contains(i)) {
            return (DedicatedServer) i;
        }
        return null;
    }

    public DedicatedServerTemplate getDSTemplate(long id) throws Exception {
        Item i = Holder.findChild(id);
        if ((i instanceof DedicatedServerTemplate) && this.holder.getAllChildren().contains(i)) {
            return (DedicatedServerTemplate) i;
        }
        return null;
    }

    public void addChild(Item dst) throws Exception {
        this.holder.addChild(dst);
    }

    public static List getAccessibleDSTemplates() throws Exception {
        Session.getLog().debug("Inside DSHolder::getUserAcessibleDSTemplates");
        List result = new ArrayList();
        GlobalKeySet gks = Globals.getAccessor().getSet("ds_templates");
        for (String str : gks.getEnabledKeys()) {
            long key = Long.parseLong(str);
            DedicatedServerTemplate dst = getAccessibleDSTemplate(key);
            if (dst != null) {
                result.add(dst);
            }
        }
        return result;
    }

    public List getAccessibleDedicatedServers() throws Exception {
        List result = new ArrayList();
        result.addAll(getDServers());
        DSHolder mainDsh = new DSHolder(1L);
        if (Session.getResellerId() != 1) {
            GlobalKeySet gks = Globals.getAccessor().getSet("ds_templates");
            for (String str : gks.getEnabledKeys()) {
                long key = Long.parseLong(str);
                DedicatedServer ds = mainDsh.getDServer(key);
                if (ds != null && ((ds.isTemplatedServer() && ds.getTemplate().isAccessible()) || (!ds.isTemplatedServer() && ds.getResellerId() == Session.getResellerId()))) {
                    result.add(ds);
                }
            }
        }
        return result;
    }

    public static DedicatedServer getAcessibleDedicatedServer(long dsId) throws Exception {
        Session.getLog().debug("Inside DSHolder::getAccessibleDedicatedServer");
        DedicatedServer ds = new DSHolder().getDServer(dsId);
        if (ds == null && Session.getResellerId() != 1) {
            DedicatedServer ds2 = new DSHolder(1L).getDServer(dsId);
            if (ds2 != null && ds2.isTemplatedServer() && ds2.getTemplate().isAccessible()) {
                return ds2;
            }
            return null;
        }
        return ds;
    }

    public static DedicatedServer getServerByRid(long rid) throws Exception {
        DSHolder dsh = new DSHolder(Session.getResellerId());
        for (DedicatedServer ds : dsh.getDServers()) {
            if (ds.getRid() == rid) {
                return ds;
            }
        }
        DSHolder dsh2 = new DSHolder(1L);
        for (DedicatedServer ds2 : dsh2.getDServers()) {
            if (ds2.getRid() == rid) {
                return ds2;
            }
        }
        throw new DServerNotFoundException("No dedicated server found for resource id  " + rid);
    }

    public static DedicatedServerTemplate getAccessibleDSTemplate(long dstId) throws Exception {
        DedicatedServerTemplate dst;
        Session.getLog().debug("Inside DSHolder::getAccessibleDSTemplate");
        DSHolder dsh = new DSHolder();
        DedicatedServerTemplate dst2 = dsh.getDSTemplate(dstId);
        if (dst2 == null) {
            if (Session.getResellerId() != 1 && (dst = new DSHolder(1L).getDSTemplate(dstId)) != null && dst.isAccessible()) {
                Session.getLog().debug("DS template '" + dst + "' is taken from the upstream provider holder.");
                return dst;
            }
        } else if (dst2.isAccessible()) {
            return dst2;
        }
        Session.getLog().debug("The requested Dedivcated Server template #'" + dstId + "' is not accessible.");
        return null;
    }

    public static DedicatedServerTemplate getManageableDSTemplate(long dstId) throws Exception {
        DedicatedServerTemplate dst;
        Session.getLog().debug("Inside DSHolder::getManageableDSTemplate");
        DSHolder dsh = new DSHolder();
        DedicatedServerTemplate dst2 = dsh.getDSTemplate(dstId);
        if (dst2 != null) {
            return dst2;
        }
        if (Session.getResellerId() != 1 && (dst = new DSHolder(1L).getDSTemplate(dstId)) != null && dst.isAccessible()) {
            Session.getLog().debug("DS template '" + dst + "' is taken from the upstream provider holder.");
            return dst;
        }
        Session.getLog().debug("Cannot get the requested Dedivcated Server template #'" + dstId + "' to manage.");
        return null;
    }

    public static DedicatedServer getDedicatedServerObject(long objectId) throws Exception {
        DedicatedServer ds = null;
        Item i = Holder.findChild(objectId);
        if (i != null) {
            synchronized (i) {
                if (i instanceof DedicatedServer) {
                    ds = (DedicatedServer) i;
                    if (!ds.isTemplatedServer() && ds.getResellerId() != Session.getResellerId()) {
                        throw new HSUserException("ds.unavailable_server_mes", new String[]{String.valueOf(objectId)});
                    }
                } else if (i instanceof DedicatedServerTemplate) {
                    DedicatedServerTemplate dst = (DedicatedServerTemplate) i;
                    if (dst.areServersAvailable() && dst.isAccessible()) {
                        ds = dst.getAvailableServer();
                    }
                    if (ds == null) {
                        throw new HSUserException("ds.unavailable_template_mes", new String[]{String.valueOf(objectId)});
                    }
                }
            }
            return ds;
        }
        throw new HSUserException("ds.incorrect_ds_id", new String[]{String.valueOf(objectId)});
    }

    public static DedicatedServer lockDedicatedServer(long objectId, ResourceId rid, DedicatedServerState state) throws Exception {
        DedicatedServer dedicatedServer;
        DedicatedServer ds = null;
        Item i = Holder.findChild(objectId);
        synchronized (i) {
            if (i instanceof DedicatedServer) {
                ds = (DedicatedServer) i;
            } else if (i instanceof DedicatedServerTemplate) {
                DedicatedServerTemplate dst = (DedicatedServerTemplate) i;
                if (dst.areServersAvailable() && dst.isAccessible()) {
                    ds = dst.getAvailableServer();
                }
            }
            if (ds != null) {
                DedicatedServerState oldDsState = ds.getState();
                ds.lock(rid, state);
                List netInterfaces = ds.getNetInterfaces();
                int nii = 0;
                while (nii < netInterfaces.size()) {
                    try {
                        DSNetInterface ni = (DSNetInterface) netInterfaces.get(nii);
                        ni.postConfig();
                        ni.setStartDate();
                        nii++;
                    } catch (Exception ex) {
                        while (true) {
                            try {
                                nii--;
                                if (nii < 0) {
                                    break;
                                }
                                DSNetInterface ni2 = (DSNetInterface) netInterfaces.get(nii);
                                ni2.resetStartDate();
                                ni2.delConfig();
                            } catch (Exception e2) {
                                Session.getLog().debug("Exception during reset configs of network interfaces: ", e2);
                            }
                        }
                        try {
                            ds.unlock(DedicatedServerState.CLEAN_UP);
                            ds.setState(oldDsState);
                        } catch (Exception e3) {
                            Session.getLog().debug("An error occured during unlock of the dedicated server (id #" + ds.getId() + ").", e3);
                        }
                        throw ex;
                    }
                }
            }
            dedicatedServer = ds;
        }
        return dedicatedServer;
    }

    public static DedicatedServer unlockDedicatedServer(long objectId, DedicatedServerState state) throws Exception {
        DedicatedServer ds = null;
        Item i = Holder.findChild(objectId);
        if (i != null && (i instanceof DedicatedServer)) {
            ds = (DedicatedServer) i;
            synchronized (ds) {
                for (DSNetInterface ni : ds.getNetInterfaces()) {
                    ni.resetStartDate();
                    ni.delConfig();
                }
                ds.unlock(state);
            }
        } else {
            Session.getLog().error("Unable to detach a dedicated server from an account (skipped).", new DServerNotFoundException("id #" + objectId + " does not correspond to any dedicated server."));
        }
        return ds;
    }
}
