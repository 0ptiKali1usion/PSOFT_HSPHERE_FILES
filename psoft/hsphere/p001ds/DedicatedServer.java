package psoft.hsphere.p001ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.AccountType;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.composite.Item;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* renamed from: psoft.hsphere.ds.DedicatedServer */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DedicatedServer.class */
public class DedicatedServer extends Item implements TemplateHashModel {
    private String name;

    /* renamed from: ip */
    private String f87ip;
    private URL rebootURL;
    private String internalId;
    private String superUser;
    private String suPasswd;
    private double setup;
    private double recurrent;
    protected DedicatedServerState state;
    private long rid;

    /* renamed from: os */
    private String f88os;
    private String cpu;
    private String ram;
    private String storage;
    private Timestamp created;
    private Timestamp taken;
    private Timestamp cancellation;
    private long resellerId;

    public DedicatedServer(long id, String name, String ip, URL rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, int state, long rid, String os, String cpu, String ram, String storage, Timestamp created, Timestamp taken, Timestamp cancellation, long resellerId) {
        super(id);
        this.f88os = null;
        this.cpu = null;
        this.ram = null;
        this.storage = null;
        this.created = null;
        this.taken = null;
        this.cancellation = null;
        this.name = name;
        this.f87ip = ip;
        this.rebootURL = rebootURL;
        this.internalId = internalId;
        this.superUser = superUser;
        this.suPasswd = suPasswd;
        this.setup = setup;
        this.recurrent = recurrent;
        this.state = DedicatedServerState.get(state);
        this.rid = rid;
        this.f88os = os;
        this.cpu = cpu;
        this.ram = ram;
        this.storage = storage;
        this.created = created;
        this.taken = taken;
        this.cancellation = cancellation;
        this.resellerId = resellerId;
    }

    public DedicatedServer(long id, String name, URL rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, int state, long rid, String os, String cpu, String ram, String storage, Timestamp created, Timestamp taken, Timestamp cancellation, long resellerId) {
        this(id, name, null, rebootURL, internalId, superUser, suPasswd, setup, recurrent, state, rid, os, cpu, ram, storage, created, taken, cancellation, resellerId);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public DedicatedServerTemplate getTemplate() {
        Session.getLog().debug("Getting ds " + getId() + " template. Template is " + getParent());
        if (getParent().getParent() != null) {
            return (DedicatedServerTemplate) getParent();
        }
        return null;
    }

    public boolean isTemplatedServer() {
        return getParent().getId() != 0;
    }

    public void save(DedicatedServer ds) {
        if (!modificationGranted()) {
            return;
        }
        this.name = ds.getName();
        this.f87ip = ds.getIp();
        this.rebootURL = ds.getRebootURL();
        this.superUser = ds.getSuperUser();
        this.suPasswd = ds.getSuPasswd();
        this.setup = ds.getSetup();
        this.recurrent = ds.getRecurrent();
        this.f88os = ds.getOs();
        this.cpu = ds.getCpu();
        this.ram = ds.getRam();
        this.storage = ds.getStorage();
        this.state = ds.getState();
    }

    public DedicatedServer copy() {
        DedicatedServer ds = new DedicatedServer(getId(), getName(), getIp(), getRebootURL(), getInternalId(), getSuperUser(), getSuPasswd(), getSetup(), getRecurrent(), getIntState(), getRid(), getOs(), getCpu(), getRam(), getStorage(), getCreated(), getTaken(), getCancellation(), getResellerId());
        ds.setParent(getParent());
        return ds;
    }

    public TemplateModel FM_save(String name, String ip, String rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage) throws Exception {
        if (modificationGranted()) {
            URL r = new URL(rebootURL);
            int newState = this.state != null ? this.state.toInt() : DedicatedServerState.DISABLED.toInt();
            save(name, ip, r, internalId, superUser, suPasswd, setup, recurrent, os, cpu, ram, storage, newState);
        }
        return this;
    }

    public void save(String name, String ip, URL rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, int state) throws Exception {
        DedicatedServer ds = new DedicatedServer(getId(), name, ip, rebootURL, internalId, superUser, suPasswd, setup, recurrent, state, getRid(), os, cpu, ram, storage, getCreated(), getTaken(), getCancellation(), getResellerId());
        if (!modificationGranted()) {
            return;
        }
        ds.setParent(getParent());
        ds.setState(DedicatedServerState.get(state));
        DedicatedServer result = DSFactory.saveDedicatedServer(ds, this);
        save(result);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("name".equals(key)) {
            return new TemplateString(getName());
        }
        if ("full_name".equals(key)) {
            return new TemplateString(getFullName());
        }
        if ("ip".equals(key)) {
            return new TemplateString(getIp());
        }
        if ("rebootURL".equals(key)) {
            return new TemplateString(getRebootURL());
        }
        if ("internalId".equals(key)) {
            return new TemplateString(getInternalId());
        }
        if ("su".equals(key)) {
            return new TemplateString(getSuperUser());
        }
        if ("su_passwd".equals(key)) {
            return new TemplateString(getSuPasswd());
        }
        if ("setup".equals(key)) {
            return new TemplateString(getSetup());
        }
        if ("recurrent".equals(key)) {
            return new TemplateString(getRecurrent());
        }
        if ("rid".equals(key)) {
            return new TemplateString(getRid());
        }
        if ("os".equals(key)) {
            return new TemplateString(getOs());
        }
        if ("cpu".equals(key)) {
            return new TemplateString(getCpu());
        }
        if ("ram".equals(key)) {
            return new TemplateString(getRam());
        }
        if ("storage".equals(key)) {
            return new TemplateString(getStorage());
        }
        if ("can_be_deleted".equals(key)) {
            return new TemplateString(canBeDeleted());
        }
        if ("template".equals(key)) {
            return getTemplate();
        }
        if ("state".equals(key)) {
            return new TemplateString(getStrState());
        }
        if ("available_states".equals(key)) {
            return new TemplateList(getAvailableStrStates());
        }
        if ("created".equals(key)) {
            if (this.taken != null) {
                return new TemplateString(HsphereToolbox.getShortDateTimeStr(this.created));
            }
            return null;
        } else if ("taken".equals(key)) {
            if (this.taken != null) {
                return new TemplateString(HsphereToolbox.getShortDateTimeStr(this.taken));
            }
            return null;
        } else if ("taken_by".equals(key)) {
            return new TemplateHash(getTakenBy());
        } else {
            if ("scheduled_cancel".equals(key)) {
                if (this.cancellation != null) {
                    return new TemplateString(HsphereToolbox.getShortDateTimeStr(this.cancellation));
                }
                return null;
            } else if ("reseller_id".equals(key)) {
                return new TemplateString(getResellerId());
            } else {
                if ("state_id".equals(key)) {
                    return new TemplateString(getState().toInt());
                }
                if ("status".equals(key)) {
                    return Resource.STATUS_OK;
                }
                if ("isTemplated".equals(key)) {
                    if (isTemplatedServer()) {
                        return new TemplateString("1");
                    }
                    return null;
                }
                return AccessTemplateMethodWrapper.getMethod(this, key);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getIp() {
        return this.f87ip;
    }

    public URL getRebootURL() {
        return this.rebootURL;
    }

    public String getInternalId() {
        return this.internalId;
    }

    public String getSuperUser() {
        return this.superUser;
    }

    public String getSuPasswd() {
        return this.suPasswd;
    }

    public double getSetup() {
        if (!isTemplatedServer()) {
            return this.setup;
        }
        return Double.NaN;
    }

    public double getRecurrent() {
        if (!isTemplatedServer()) {
            return this.recurrent;
        }
        return Double.NaN;
    }

    public DedicatedServerState getState() {
        return this.state;
    }

    public int getIntState() {
        return this.state.toInt();
    }

    public String getStrState() {
        return this.state.toString();
    }

    public static String getStrState(int state) {
        return DedicatedServerState.get(state).toString();
    }

    public static int getIntState(String state) {
        return DedicatedServerState.get(state).toInt();
    }

    public static List getAllStrStates() {
        return DedicatedServerState.getAllStrStates();
    }

    public List getAvailableStrStates() {
        List res = new ArrayList();
        List<DedicatedServerState> states = getAvailableStates();
        if (states != null) {
            for (DedicatedServerState dedicatedServerState : states) {
                res.add(dedicatedServerState.toString());
            }
        }
        return res;
    }

    public List getAvailableStates() {
        if (this.state == DedicatedServerState.DISABLED) {
            return Arrays.asList(DedicatedServerState.AVAILABLE);
        }
        if (this.state == DedicatedServerState.AVAILABLE) {
            return Arrays.asList(DedicatedServerState.IN_USE, DedicatedServerState.ON_HOLD, DedicatedServerState.DISABLED);
        }
        if (this.state == DedicatedServerState.IN_USE) {
            return Arrays.asList(DedicatedServerState.ON_HOLD, DedicatedServerState.CLEAN_UP);
        }
        if (this.state == DedicatedServerState.ON_HOLD) {
            return Arrays.asList(DedicatedServerState.IN_USE, DedicatedServerState.CLEAN_UP);
        }
        if (this.state == DedicatedServerState.CLEAN_UP) {
            return Arrays.asList(DedicatedServerState.AVAILABLE, DedicatedServerState.DISABLED);
        }
        return null;
    }

    public TemplateModel FM_setState(String strState) throws Exception {
        setState(DedicatedServerState.get(strState));
        return new TemplateOKResult();
    }

    public TemplateModel FM_requiresAssignment(String strState) {
        if (requiresAssignment(DedicatedServerState.get(strState))) {
            return new TemplateString(1);
        }
        return null;
    }

    public boolean requiresAssignment(DedicatedServerState state) {
        return this.rid == 0 && (state == DedicatedServerState.IN_USE || state == DedicatedServerState.ON_HOLD);
    }

    public TemplateModel FM_requiresUnassignment(String strState) {
        if (requiresUnassignment(DedicatedServerState.get(strState))) {
            return new TemplateString(1);
        }
        return null;
    }

    public boolean requiresUnassignment(DedicatedServerState state) {
        Session.getLog().debug("this.rid = " + this.rid);
        return this.rid != 0 && (state == DedicatedServerState.CLEAN_UP || state == DedicatedServerState.DISABLED || state == DedicatedServerState.AVAILABLE);
    }

    public long getRid() {
        return this.rid;
    }

    public String getOs() {
        if (!isTemplatedServer()) {
            return this.f88os;
        }
        return getTemplate().getOS();
    }

    public String getCpu() {
        if (!isTemplatedServer()) {
            return this.cpu;
        }
        return getTemplate().getCPU();
    }

    public String getRam() {
        if (!isTemplatedServer()) {
            return this.ram;
        }
        return getTemplate().getRAM();
    }

    public String getStorage() {
        if (!isTemplatedServer()) {
            return this.storage;
        }
        return getTemplate().getStorage();
    }

    public Timestamp getCreated() {
        return this.created;
    }

    public Timestamp getTaken() {
        return this.taken;
    }

    public Timestamp getCancellation() {
        return this.cancellation;
    }

    public String getStateDescription() {
        if (this.state != null) {
            return Localizer.translateLabel("admin.ds.status." + this.state.toString());
        }
        return null;
    }

    public boolean canBeDeleted() {
        try {
            if (this.state == DedicatedServerState.DISABLED || this.state == DedicatedServerState.CLEAN_UP) {
                if (getResellerId() == Session.getResellerId()) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to figure out whether the dedicated server with id #" + this.f79id + " can be deleted.", ex);
            return false;
        }
    }

    public void setName(String name) {
        if (!modificationGranted()) {
            return;
        }
        this.name = name;
    }

    public void setIP(String _ip) {
        if (!modificationGranted()) {
            return;
        }
        this.f87ip = _ip;
    }

    public void setRebootURL(URL rebootURL) {
        if (!modificationGranted()) {
            return;
        }
        this.rebootURL = rebootURL;
    }

    public void setInternalId(String internalId) {
        if (!modificationGranted()) {
            return;
        }
        this.internalId = internalId;
    }

    public void setSuperUser(String superUser) {
        if (!modificationGranted()) {
            return;
        }
        this.superUser = superUser;
    }

    public void setSuPasswd(String suPasswd) {
        if (!modificationGranted()) {
            return;
        }
        this.suPasswd = suPasswd;
    }

    public void setSetup(double setup) {
        if (!modificationGranted()) {
            return;
        }
        this.setup = setup;
    }

    public void setRecurrent(double recurrent) {
        if (!modificationGranted()) {
            return;
        }
        this.recurrent = recurrent;
    }

    public void setState(DedicatedServerState state) throws Exception {
        if (!modificationGranted()) {
            return;
        }
        if (!requiresAssignment(state)) {
            DSFactory.updateDedicatedServerState(getId(), state);
            this.state = state;
            return;
        }
        throw new HSUserException(Localizer.translateMessage("admin.ds.cannot_set_server_state"));
    }

    public void setOs(String os) {
        if (!modificationGranted()) {
            return;
        }
        this.f88os = os;
    }

    public void setCpu(String cpu) {
        if (!modificationGranted()) {
            return;
        }
        this.cpu = cpu;
    }

    public void setRam(String ram) {
        if (!modificationGranted()) {
            return;
        }
        this.ram = ram;
    }

    public void setStorage(String storage) {
        if (!modificationGranted()) {
            return;
        }
        this.storage = storage;
    }

    public synchronized void scheduleCancellation(Date date) throws Exception {
        if (date == null) {
            throw new HSUserException("ds.cancel_date_notspecified");
        }
        if (this.taken != null && date.after(this.taken)) {
            Timestamp ct = new Timestamp(TimeUtils.dropMinutes(date).getTime());
            DSFactory.scheduleDedicatedServerCancel(getId(), ct);
            this.cancellation = ct;
            return;
        }
        throw new HSUserException("ds.wrong_cancel_date");
    }

    public synchronized void discardCancellation() throws Exception {
        if (this.cancellation != null) {
            DSFactory.discardDedicatedServerCancel(getId());
            this.cancellation = null;
        }
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public synchronized void lock(ResourceId rid, DedicatedServerState state) throws Exception {
        Timestamp curTime = new Timestamp(TimeUtils.currentTimeMillis());
        if (state == DedicatedServerState.IN_USE || state == DedicatedServerState.ON_HOLD) {
            DSFactory.lockDedicatedServer(getId(), rid.getId(), state, curTime);
            this.rid = rid.getId();
            this.state = state;
            this.taken = curTime;
            return;
        }
        String[] strArr = new String[1];
        strArr[0] = state != null ? state.toString() : "null";
        throw new HSUserException(Localizer.translateMessage("admin.ds.cannot_assign_server_state", strArr));
    }

    public synchronized void unlock(DedicatedServerState state) throws Exception {
        if (state == DedicatedServerState.CLEAN_UP) {
            DSFactory.unlockDedicatedServer(getId(), state);
            this.cancellation = null;
            Session.getLog().debug("this.rid = " + this.rid);
            this.rid = 0L;
            Session.getLog().debug("this.rid = " + this.rid);
            this.state = DedicatedServerState.CLEAN_UP;
            return;
        }
        String[] strArr = new String[1];
        strArr[0] = state != null ? state.toString() : "null";
        throw new HSUserException(Localizer.translateMessage("admin.ds.cannot_assign_server_state", strArr));
    }

    public String getFullName() {
        return isTemplatedServer() ? getName() + " (" + ((DedicatedServerTemplate) getParent()).getName() + ")" : getName();
    }

    public String getFullDescription() throws Exception {
        return Localizer.translateMessage("ds.name_mes", new String[]{getFullName()}) + "\n" + Localizer.translateMessage("ds.os_name_mes", new String[]{getOs()}) + "\n" + Localizer.translateMessage("ds.cpu_mes", new String[]{getCpu()}) + "\n" + Localizer.translateMessage("ds.ram_mes", new String[]{getRam()}) + "\n" + Localizer.translateMessage("ds.storage_mes", new String[]{getStorage()}) + "\n" + Localizer.translateMessage("ds.ip_mes", new String[]{getIp()}) + "\n" + Localizer.translateMessage("ds.state_mes", new String[]{getStateDescription()}) + "\n";
    }

    public ResourceId getServerOwnerId() {
        if (getRid() > 0) {
            return new ResourceId(getRid(), 7100);
        }
        return null;
    }

    public DedicatedServerResource getServerOwner() throws Exception {
        DedicatedServerResource owner = null;
        if (getRid() > 0) {
            Session.save();
            try {
                ResourceId ownerId = getServerOwnerId();
                Session.setAccount(ownerId.getAccount());
                Session.setUser(ownerId.getAccount().getUser());
                owner = (DedicatedServerResource) ownerId.get();
                Session.restore();
            } catch (Throwable th) {
                Session.restore();
                throw th;
            }
        }
        return owner;
    }

    public List getNetInterfaces() throws Exception {
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        return netInterfaceMan.getInterfacesByDS(this.f79id);
    }

    public TemplateModel FM_getNetInterfaces() throws Exception {
        return new TemplateList(getNetInterfaces());
    }

    private boolean modificationGranted() {
        try {
            if (Session.getResellerId() == this.resellerId) {
                return true;
            }
            Session.getLog().debug("Detected attempt to modify the dedicated server network interface #" + this.f79id + ", created by reseller #" + this.resellerId + ", out of another reseller #" + Session.getResellerId() + ".");
            return false;
        } catch (UnknownResellerException e) {
            Session.getLog().debug("Unknown reseller. Modification of dedicated server network interface #" + this.f79id + " is disabled.");
            return false;
        }
    }

    public Hashtable getTakenBy() {
        Hashtable res = new Hashtable();
        if (this.taken != null) {
            res.put("taken", this.taken);
            boolean isAdmin = false;
            try {
                isAdmin = AccountType.getType(Session.getAccount()).isAdmin();
            } catch (Exception ex) {
                Session.getLog().debug("Unable to check whether the current user in the session is admin.", ex);
            }
            try {
                long curResellerId = Session.getResellerId();
                if (getRid() > 0 && curResellerId > 0 && isAdmin) {
                    long aResellerId = 0;
                    Session.save();
                    try {
                        Account a = getServerOwnerId().getAccount();
                        res.put("account_id", String.valueOf(a.getId().getId()));
                        if (curResellerId == 1) {
                            aResellerId = a.getResellerId();
                        }
                        Session.restore();
                    } catch (Exception ex2) {
                        Session.getLog().debug("Problem with getting account of the user who has taken the dedicated server #" + getId(), ex2);
                        Session.restore();
                    }
                    if (aResellerId > 1) {
                        try {
                            res.put(FMACLManager.RESELLER, Reseller.getReseller(aResellerId).getUser());
                        } catch (Exception ex3) {
                            Session.getLog().debug("Problem with getting the reseller name", ex3);
                        }
                    }
                    if (getCancellation() != null) {
                        res.put("cancellation", getCancellation());
                    }
                }
            } catch (UnknownResellerException ure) {
                Session.getLog().debug("Unknow reseller in the session.", ure);
            } catch (Exception ex4) {
                Session.getLog().error("Critical Error: ", ex4);
            }
        }
        return res;
    }
}
