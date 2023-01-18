package psoft.hsphere.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Language;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/admin/TransferProcess.class */
public class TransferProcess extends SharedObject implements TemplateHashModel {
    public static final int STAGE_ADDED = 0;
    public static final int STAGE_GOT_RESOURCES = 1;
    public static final int STAGE_MOVEABILITY_OK = 2;
    public static final int STAGE_PHCREATED = 3;
    public static final int STAGE_CONTENT_MOVED = 4;
    public static final int STAGE_DNSOPS = 5;
    public static final int STAGE_CONFIGS_REPOSTED = 6;
    public static final int STAGE_SCR_CLEAN = 7;
    public static final int STAGE_FINISHED = 8;
    public static final long DNS_PROPAGATION_TIME_SLACK = 86400;

    /* renamed from: id */
    private long f63id;
    private int stage;
    private Timestamp started;
    private Timestamp finished;
    private Timestamp suspended;
    private long accId;
    private List resourcesInMove;
    private long srcHostId;
    private long targetHostId;
    private HostEntry sourceServer;
    private HostEntry targetServer;

    public TransferProcess(long id, Timestamp started, Timestamp finished, Timestamp suspended, long src, long target, int stage, long accountId) throws Exception {
        super(id);
        this.resourcesInMove = new ArrayList();
        this.sourceServer = null;
        this.targetServer = null;
        this.f63id = id;
        this.started = started;
        this.finished = finished;
        this.suspended = suspended;
        this.srcHostId = src;
        this.targetHostId = target;
        this.stage = stage;
        this.accId = accountId;
        loadHDResourcesInMove();
    }

    public static TransferProcess createTransferProcess(long srcServerId, long targetServerId, long accountId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            long newId = Session.getNewIdAsLong("transfer_id");
            ps = con.prepareStatement("INSERT INTO transfer_process(id, account_id, src_server, target_server, started) VALUES(?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setLong(2, accountId);
            ps.setLong(3, srcServerId);
            ps.setLong(4, targetServerId);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            TransferProcess tp = new TransferProcess(newId, null, null, null, srcServerId, targetServerId, 0, accountId);
            tp.setStage(0);
            Session.closeStatement(ps);
            con.close();
            return tp;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static TransferProcess createTransferProcess(long srcServerId, long targetServerId, Account a) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            long newId = Session.getNewIdAsLong("transfer_id");
            ps = con.prepareStatement("INSERT INTO transfer_process(id, account_id, src_server, target_server, started) VALUES(?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setLong(2, a.getId().getId());
            ps.setLong(3, srcServerId);
            ps.setLong(4, targetServerId);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            TransferProcess tp = new TransferProcess(newId, null, null, null, srcServerId, targetServerId, 0, a.getId().getId());
            tp.setStage(0);
            Session.closeStatement(ps);
            con.close();
            return tp;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private synchronized void getResoucesInMoveInfo() throws Exception {
        PreparedStatement ps = null;
        Session.getLog().debug("Inside TransferProcess::getResourcesInMoveInfo()");
        Connection con = Session.getDb();
        Session.save();
        try {
            Account a = (Account) Account.get(new ResourceId(this.accId, 0));
            User u = a.getUser();
            Session.setUser(u);
            Session.setAccount(a);
            ps = con.prepareStatement("DELETE FROM resources_in_move WHERE tp_id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Collection<ResourceId> c = a.getId().getAllChildrenByPriority();
            int order = 0;
            List<Resource> domainAliases = new ArrayList();
            for (ResourceId rid : c) {
                try {
                    Resource r = rid.get();
                    if (r == null) {
                        createTT("Error  processing transfer process with id " + getId(), "Unable to get resource", rid, null);
                        throw new Exception("Unable to get resource " + rid.toString());
                    }
                    Session.getLog().debug("getResourcesInMoveInfo(): got and checking " + rid.toString() + " is a HostDependentResource - " + (r instanceof HostDependentResource ? " true" : "false"));
                    if (r instanceof HostDependentResource) {
                        Session.getLog().debug("getResourcesInMoveInfo(): host_id=" + ((HostDependentResource) r).getHostId());
                    }
                    if ((r instanceof HostDependentResource) && ((HostDependentResource) r).getHostId() == this.srcHostId) {
                        if (r.getId().getType() == 6400) {
                            domainAliases.add(r);
                        } else {
                            int i = order;
                            order++;
                            this.resourcesInMove.add(HDResourceInMove.create(this, r, i));
                            Session.getLog().debug("getResourcesInMoveInfo(): " + rid.toString() + " has been added");
                        }
                    }
                } catch (Exception ex) {
                    createTT("Error processing transfer process with id " + getId(), "Unable to get resource", rid, ex);
                    throw ex;
                }
            }
            for (Resource r2 : domainAliases) {
                int i2 = order;
                order++;
                this.resourcesInMove.add(HDResourceInMove.create(this, r2, i2));
                Session.getLog().debug("getResourcesInMoveInfo(): " + r2.getId().toString() + " has been added");
            }
            Session.closeStatement(ps);
            con.close();
            Session.restore();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            Session.restore();
            throw th;
        }
    }

    public void setStage(int newStage) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE transfer_process SET stage = ?, finished = ? WHERE id = ?");
            Timestamp fn = new Timestamp(TimeUtils.currentTimeMillis());
            ps.setInt(1, newStage);
            ps.setTimestamp(2, fn);
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.stage = newStage;
            this.finished = fn;
            if (this.stage == 8) {
                Account.getAccount(this.accId).isMoved(false);
            } else {
                Account.getAccount(this.accId).isMoved(true);
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.SharedObject
    public long getId() {
        return this.f63id;
    }

    public synchronized void process() throws Exception {
        process(false);
    }

    public synchronized void process(boolean force) throws Exception {
        if (hasErrors()) {
            return;
        }
        Session.getLog().debug("Processing of " + getId() + " transfer process with force = " + force + " has been started");
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        Session.save();
        try {
            ResourceId aid = new ResourceId(getAccId(), 0);
            Account a = (Account) Account.get(aid);
            Session.setUser(a.getUser());
            Session.setAccount(a);
            switch (this.stage) {
                case 0:
                    Session.getLog().debug("TRANSFER: Getting resources info");
                    getResoucesInMoveInfo();
                    setStage(1);
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 1:
                    try {
                        Session.getLog().debug("TRANSFER: Checking moveability");
                        if (checkMoveability()) {
                            setStage(2);
                        } else {
                            suspendProcess();
                        }
                    } catch (Exception ex) {
                        Session.getLog().error("Unable to check moveability for ID" + this.f63id, ex);
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 2:
                    Session.getLog().debug("TRANSFER: Physical creation");
                    if (physicalCreate()) {
                        setStage(3);
                    } else {
                        suspendProcess();
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 3:
                    Session.getLog().debug("TRANSFER: Transfering content");
                    if (force) {
                        setStage(4);
                    } else {
                        transferContent();
                        if (isContentTransfered()) {
                            setStage(4);
                        }
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 4:
                    Session.getLog().debug("TRANSFER: Switching server");
                    if (switchServer()) {
                        setStage(5);
                    } else {
                        suspendProcess();
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 5:
                    Session.getLog().debug("TRANSFER: reposting IP depended configs");
                    if (repostConfigs()) {
                        setStage(6);
                    } else {
                        suspendProcess();
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                case 6:
                    Session.getLog().debug("TRANSFER: Cleaning the source server");
                    if (clearSource(force)) {
                        setStage(8);
                    }
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
                default:
                    Session.restore();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return;
            }
        } catch (Throwable th) {
            Session.restore();
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        try {
        } catch (Exception ex) {
            Session.getLog().error("An error occured:", ex);
        }
        if ("stage_description".equals(key)) {
            return new TemplateString(getStringDescription(false));
        }
        if ("stage".equals(key)) {
            return new TemplateString(this.stage);
        }
        if ("resources".equals(key)) {
            return new TemplateList(this.resourcesInMove);
        }
        if ("stage_description_long".equals(key)) {
            return new TemplateString(getStringDescription(true));
        }
        if ("errorsDetected".equals(key)) {
            return new TemplateString(hasErrors());
        }
        if ("isSuspended".equals(key)) {
            return new TemplateString(isSuspended());
        }
        return super.get(key);
    }

    public HostEntry getSourceServer() throws Exception {
        if (this.sourceServer == null) {
            this.sourceServer = HostManager.getHost(this.srcHostId);
        }
        return this.sourceServer;
    }

    public HostEntry getTargetServer() throws Exception {
        if (this.targetServer == null) {
            this.targetServer = HostManager.getHost(this.targetHostId);
        }
        return this.targetServer;
    }

    public long getSrcHostId() {
        return this.srcHostId;
    }

    public long getTargetHostId() {
        return this.targetHostId;
    }

    private void loadHDResourcesInMove() throws Exception {
        PreparedStatement ps = null;
        this.resourcesInMove = new ArrayList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT rid, rtype,created, deleted, job_id, tt_id FROM resources_in_move WHERE tp_id =? ORDER BY ord");
            ps.setLong(1, this.f63id);
            ResultSet rs = ps.executeQuery();
            Session.save();
            if (Session.getAccountId() != getAccId()) {
                ResourceId aid = new ResourceId(getAccId(), 0);
                Account a = (Account) Account.get(aid);
                Session.setUser(a.getUser());
                Session.setAccount(a);
            }
            while (rs.next()) {
                HDResourceInMove hdr = new HDResourceInMove(this, new ResourceId(rs.getLong("rid"), rs.getInt("rtype")), rs.getTimestamp("created"), rs.getTimestamp("deleted"), rs.getInt("job_id"), rs.getLong("tt_id"));
                this.resourcesInMove.add(hdr);
            }
            Session.restore();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private boolean checkMoveability() {
        boolean result = true;
        Session.getLog().debug("Inside TransferProcess::checkMoveability()");
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (!hdr.canBeMoved()) {
                result = false;
                Session.getLog().debug("Resource: " + hdr.getRid() + "can't be moved");
            }
        }
        return result;
    }

    private boolean physicalCreate() throws Exception {
        Session.getLog().debug("Inside TransferProcess::physicalCreate()");
        loadHDResourcesInMove();
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (!hdr.isCreated()) {
                try {
                    hdr.physicalCreate(this.targetHostId);
                } catch (Exception ex) {
                    Session.getLog().error("Error during physical create rid=" + hdr.getRid().getId(), ex);
                    return false;
                }
            }
        }
        return true;
    }

    private void transferContent() throws Exception {
        Session.getLog().debug("Inside TransferProcess::transferContent()");
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (hdr.hasContent()) {
                try {
                    hdr.transferContent();
                } catch (Throwable th) {
                    Session.getLog().error("Error transfering content for " + hdr.getRid(), th);
                }
            }
        }
    }

    private boolean switchServer() throws SQLException {
        String oldIp;
        String newIp;
        Session.getLog().debug("Inside TransferProcess::switchServer()");
        Connection con = Session.getTransConnection();
        try {
            try {
                Hashtable ips = new Hashtable();
                for (int i = 0; i < this.resourcesInMove.size(); i++) {
                    HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
                    Session.getLog().debug("switchServer(): got and processing " + hdr.getRid().toString());
                    if (hdr.getRid().getType() == 8) {
                        Hashtable t = new Hashtable();
                        MixedIPResource ipr = (MixedIPResource) hdr.getResource();
                        if (ipr.isDedicated()) {
                            try {
                                oldIp = getSourceServer().getIPbyRid(hdr.getRid()).toString();
                                try {
                                    newIp = getTargetServer().getIPbyRid(hdr.getRid()).toString();
                                } catch (Exception ex) {
                                    hdr.createTT("Error while switching server", "Unable to get new dedicated IP by resource id", ex);
                                    throw ex;
                                }
                            } catch (Exception ex2) {
                                hdr.createTT("Error while switching server", "Unable to get old dedicated IP by resource id", ex2);
                                throw ex2;
                            }
                        } else {
                            try {
                                oldIp = getSourceServer().getSharedIP(ipr.getSharedIPTag()).toString();
                                try {
                                    newIp = getTargetServer().getSharedIP(ipr.getSharedIPTag()).toString();
                                } catch (Exception ex3) {
                                    hdr.createTT("Error while switching server", "Unable to get new shared IP", ex3);
                                    throw ex3;
                                }
                            } catch (Exception ex4) {
                                hdr.createTT("Error while switching server", "Unable to get new shared IP", ex4);
                                throw ex4;
                            }
                        }
                        t.put("old_ip", oldIp);
                        t.put("new_ip", newIp);
                        ips.put(hdr.getRid(), t);
                    }
                    hdr.updateHostId();
                }
                for (ResourceId rid : ips.keySet()) {
                    try {
                        MixedIPResource ipr2 = (MixedIPResource) rid.get();
                        String oldIp2 = (String) ((Hashtable) ips.get(rid)).get("old_ip");
                        String newIp2 = (String) ((Hashtable) ips.get(rid)).get("new_ip");
                        ipr2.changeDNS(oldIp2, newIp2);
                        if (ipr2.isDedicated()) {
                            ipr2.addIP(getTargetHostId());
                        }
                    } catch (Exception ex5) {
                        HDResourceInMove r = getHDResourceInMoveByRid(rid);
                        r.createTT("Error while switching server", "", ex5);
                        throw ex5;
                    }
                }
                Session.commitTransConnection(con);
                return true;
            } catch (Exception ex6) {
                Session.getLog().error("Error while switching server", ex6);
                con.rollback();
                Session.commitTransConnection(con);
                return false;
            }
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public boolean repostConfigs() throws Exception {
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            try {
                if (hdr.getResource() instanceof IPDependentResource) {
                    IPDependentResource idr = (IPDependentResource) hdr.getResource();
                    idr.ipRestart();
                }
                try {
                    if (Session.getAccount().isSuspended() || Session.getAccount().isPartlySuspended()) {
                        hdr.getResource().suspend();
                    }
                } catch (Exception ex) {
                    hdr.createTT("An error occured while suspending resource", ex.getMessage(), ex);
                    return false;
                }
            } catch (Exception ex2) {
                hdr.createTT("Problem reposting config", "", ex2);
                return false;
            }
        }
        return true;
    }

    private void cleanUp() throws Exception {
        Session.getLog().debug("Inside TransferProcess::cleanUp()");
        setStage(8);
    }

    public long getAccId() {
        return this.accId;
    }

    public static TransferProcess get(long id) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, started, finished, suspended, src_server, target_server, stage, account_id  FROM transfer_process WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TransferProcess transferProcess = new TransferProcess(id, rs.getTimestamp("started"), rs.getTimestamp("finished"), rs.getTimestamp("suspended"), rs.getLong("src_server"), rs.getLong("target_server"), rs.getInt("stage"), rs.getLong("account_id"));
                Session.closeStatement(ps);
                con.close();
                return transferProcess;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static TransferProcess getByAccId(long accId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, started, finished, suspended, src_server, target_server, stage, account_id  FROM transfer_process WHERE account_id = ?");
            ps.setLong(1, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (!rs.isLast()) {
                    throw new Exception("Multiple transfer processes for account # [" + accId + "]");
                }
                TransferProcess transferProcess = new TransferProcess(rs.getLong("id"), rs.getTimestamp("started"), rs.getTimestamp("finished"), rs.getTimestamp("suspended"), rs.getLong("src_server"), rs.getLong("target_server"), rs.getInt("stage"), rs.getLong("account_id"));
                Session.closeStatement(ps);
                con.close();
                return transferProcess;
            }
            return null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public boolean isContentTransfered() {
        Session.getLog().debug("Inside TransferProcess::isContentTransfered()");
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (hdr.hasContent()) {
                if (hdr.getTransport() == null || !hdr.getTransport().isFinished()) {
                    return false;
                }
                Session.getLog().debug("Content move for HDR " + hdr.getRid() + " is finished");
            }
        }
        Session.getLog().debug("Transfer process " + getId() + " content transfer is finished");
        return true;
    }

    private void suspendProcess() throws Exception {
        PreparedStatement ps = null;
        if (isSuspended()) {
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE transfer_process SET suspended = ? WHERE id = ?");
            ps.setTimestamp(1, now);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.suspended = now;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getStringDescription(boolean detailed) {
        String time;
        SimpleDateFormat df;
        String pref = "";
        if (detailed) {
            pref = "_detailed";
        }
        switch (this.stage) {
            case 0:
                return Localizer.translateMessage("eeman.tp.stage.getting_res_info" + pref);
            case 1:
                return Localizer.translateMessage("eeman.tp.stage.got_res_info" + pref);
            case 2:
                return Localizer.translateMessage("eeman.tp.stage.moveability_ok" + pref);
            case 3:
                return Localizer.translateMessage("eeman.tp.stage.ph_created" + pref);
            case 4:
                return Localizer.translateMessage("eeman.tp.stage.content_transfered" + pref);
            case 5:
                return Localizer.translateMessage("eeman.tp.stage.dns_reconfigured" + pref);
            case 6:
                Timestamp now = new Timestamp(TimeUtils.currentTimeMillis());
                Timestamp dayX = new Timestamp(this.finished.getTime() + 86400000);
                if (dayX.after(now)) {
                    String format = null;
                    try {
                        format = Settings.get().getValue("meddate").trim();
                    } catch (Exception e) {
                        Session.getLog().debug("Failed to get meddate value from settings");
                    }
                    if (format == null || format.equals("")) {
                        df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    } else {
                        df = new SimpleDateFormat(format);
                    }
                    Session.getLog().debug("DateFormatPattern = " + df.toPattern());
                    time = df.format((Date) dayX);
                } else {
                    time = "now";
                }
                return Localizer.translateMessage("eeman.tp.stage.configs_reposted" + pref, new Object[]{time});
            case 7:
                return Localizer.translateMessage("eeman.tp.stage.src_cleaned" + pref);
            case 8:
                return Localizer.translateMessage("eeman.tp.stage.finished" + pref);
            default:
                return "";
        }
    }

    public boolean clearSource(boolean force) throws Exception {
        Timestamp now = new Timestamp(TimeUtils.currentTimeMillis());
        Timestamp dayX = new Timestamp(this.finished.getTime() + 86400000);
        Session.getLog().debug("Inside TransferProcess::clearSource now = " + now.toString() + " dayX = " + dayX.toString());
        if (now.after(dayX) || force) {
            Session.getLog().debug("Deleting resources on source server");
            ListIterator i = this.resourcesInMove.listIterator(this.resourcesInMove.size());
            while (i.hasPrevious()) {
                HDResourceInMove hdr = (HDResourceInMove) i.previous();
                try {
                    if (!hdr.isDeleted()) {
                        hdr.physicalDelete(getSrcHostId());
                    }
                } catch (Exception ex) {
                    Session.getLog().error("Error while creaning source server for process " + getId(), ex);
                }
            }
            return true;
        }
        return false;
    }

    public void delete() throws Exception {
        PreparedStatement ps = null;
        loadHDResourcesInMove();
        if (this.resourcesInMove.size() > 0) {
            for (int i = 0; i < this.resourcesInMove.size(); i++) {
                HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
                hdr.delete();
            }
        }
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM transfer_process WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            remove(this.f63id, TransferProcess.class);
            Session.closeStatement(ps);
            con.close();
            Account.getAccount(this.accId).isMoved(false);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delete() throws Exception {
        delete();
        return new TemplateOKResult();
    }

    public boolean isSuspended() {
        return this.suspended != null;
    }

    public boolean hasErrors() {
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (hdr.errorDetected()) {
                return true;
            }
        }
        return false;
    }

    public TemplateModel FM_process(String force) throws Exception {
        if (hasErrors()) {
            throw new HSUserException("eeman.tp.errors_detected_exception");
        }
        process(force != null && "ON".equals(force.toUpperCase()));
        return this;
    }

    public TemplateModel FM_clearTT(long hdId) throws Exception {
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            if (hdr.getRid().getId() == hdId) {
                hdr.closeTT();
                return this;
            }
        }
        throw new Exception("HDR with id " + hdId + " not found");
    }

    public void clearAllTTs() throws Exception {
        for (int i = 0; i < this.resourcesInMove.size(); i++) {
            HDResourceInMove hdr = (HDResourceInMove) this.resourcesInMove.get(i);
            hdr.closeTT();
        }
    }

    public TemplateModel FM_clearAllTTs() throws Exception {
        clearAllTTs();
        return this;
    }

    public TemplateModel FM_resume() throws Exception {
        resume();
        return this;
    }

    public TemplateModel FM_suspend() throws Exception {
        suspend();
        return this;
    }

    public void resume() throws Exception {
        PreparedStatement ps = null;
        if (!isSuspended()) {
            return;
        }
        if (hasErrors()) {
            clearAllTTs();
        }
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE transfer_process SET suspended =? WHERE id = ?");
            ps.setNull(1, 93);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.suspended = null;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void suspend() throws Exception {
        PreparedStatement ps = null;
        if (isSuspended()) {
            return;
        }
        Connection con = Session.getDb();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try {
            ps = con.prepareStatement("UPDATE transfer_process SET suspended =? WHERE id = ?");
            ps.setTimestamp(1, now);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.suspended = now;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:42:0x0098, code lost:
        return new psoft.hsphere.TemplateOKResult();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public freemarker.template.TemplateModel FM_launchSync(long r6) throws java.lang.Exception {
        /*
            r5 = this;
            r0 = r5
            java.util.List r0 = r0.resourcesInMove
            java.util.Iterator r0 = r0.iterator()
            r8 = r0
        La:
            r0 = r8
            boolean r0 = r0.hasNext()
            if (r0 == 0) goto L91
            r0 = r8
            java.lang.Object r0 = r0.next()
            psoft.hsphere.admin.HDResourceInMove r0 = (psoft.hsphere.admin.HDResourceInMove) r0
            r9 = r0
            r0 = r9
            psoft.hsphere.ResourceId r0 = r0.getRid()
            long r0 = r0.getId()
            r1 = r6
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 != 0) goto L8e
            r0 = r9
            psoft.hsphere.admin.ResourceTransport r0 = r0.getTransport()
            r10 = r0
            r0 = r10
            if (r0 == 0) goto L58
            r0 = r10
            boolean r0 = r0.isFinished()
            if (r0 != 0) goto L4d
            r0 = r10
            r1 = 1
            int r0 = r0.executeTransfer(r1)
            goto L8e
        L4d:
            psoft.hsphere.HSUserException r0 = new psoft.hsphere.HSUserException
            r1 = r0
            java.lang.String r2 = "content.move.finished"
            r1.<init>(r2)
            throw r0
        L58:
            psoft.hsphere.Account r0 = psoft.hsphere.Session.getAccount()
            r11 = r0
            psoft.hsphere.User r0 = psoft.hsphere.Session.getUser()
            r12 = r0
            r0 = r9
            psoft.hsphere.ResourceId r0 = r0.getRid()
            psoft.hsphere.Account r0 = r0.getAccount()
            psoft.hsphere.Session.setAccount(r0)
            r0 = r9
            psoft.hsphere.ResourceId r0 = r0.getRid()
            psoft.hsphere.Account r0 = r0.getAccount()
            psoft.hsphere.User r0 = r0.getUser()
            psoft.hsphere.Session.setUser(r0)
            r0 = r9
            int r0 = r0.transferContent()
            r0 = r11
            psoft.hsphere.Session.setAccount(r0)
            r0 = r12
            psoft.hsphere.Session.setUser(r0)
            goto L91
        L8e:
            goto La
        L91:
            psoft.hsphere.TemplateOKResult r0 = new psoft.hsphere.TemplateOKResult
            r1 = r0
            r1.<init>()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.admin.TransferProcess.FM_launchSync(long):freemarker.template.TemplateModel");
    }

    private HDResourceInMove getHDResourceInMoveByRid(ResourceId rid) {
        for (HDResourceInMove r : this.resourcesInMove) {
            if (rid.equals(r.getRid())) {
                return r;
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TransferProcess) {
            TransferProcess transferProcess = (TransferProcess) o;
            return this.f63id == transferProcess.f63id;
        }
        return false;
    }

    public void createTT(String title, String message, ResourceId rid, Exception ex) {
        Session.setLanguage(new Language(null));
        StringWriter st = new StringWriter();
        PrintWriter out = new PrintWriter(st);
        if (rid != null) {
            try {
                out.println(title + " " + TypeRegistry.getDescription(rid.getType()));
            } catch (Exception ex1) {
                out.println("Error creating TT: unable to generate TT body");
                ex1.printStackTrace(out);
            }
        }
        out.println("Account ID: " + getAccId());
        out.println("Source server: " + getSourceServer().getName());
        out.println("Target Server: " + getTargetServer().getName());
        out.println("Resource ID: " + rid.toString());
        if (message != null) {
            out.println(message);
        }
        if (ex != null) {
            ex.printStackTrace(out);
        }
        try {
            Ticket.create("Transfer process " + getId() + " error: " + title, 50, st.toString(), rid.getId(), rid.getType(), null, 0, 1, 0, 0, 0, 1);
            Session.getDb();
        } catch (Exception ex12) {
            Session.getLog().error("Failed to create TT", ex12);
        }
    }
}
