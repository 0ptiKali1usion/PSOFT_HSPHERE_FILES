package psoft.hsphere.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import psoft.hsphere.Language;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/admin/HDResourceInMove.class */
public class HDResourceInMove implements TemplateHashModel {
    private TransferProcess parent;
    private Timestamp created;
    private Timestamp deleted;
    private boolean hasContent;
    private ResourceId rid;

    /* renamed from: r */
    private Resource f61r;
    private long transferJobId;
    private long ttId;
    private ResourceTransport transport;

    public HDResourceInMove(TransferProcess tp, ResourceId rid, Timestamp created, Timestamp deleted, long transferJobId, long ttId) throws Exception {
        this.hasContent = false;
        this.ttId = 0L;
        this.transport = null;
        this.parent = tp;
        this.created = created;
        this.deleted = deleted;
        this.rid = rid;
        this.f61r = rid.get();
        this.transferJobId = transferJobId;
        this.transport = accuireTransport();
        this.ttId = ttId;
    }

    public static HDResourceInMove create(TransferProcess tp, Resource r, int order) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO resources_in_move(tp_id, rid, rtype, created, deleted, job_id, ord) VALUES (?,?,?,?,?,?,?)");
            ps.setLong(1, tp.getId());
            ps.setLong(2, r.getId().getId());
            ps.setInt(3, r.getId().getType());
            ps.setNull(4, 93);
            ps.setNull(5, 93);
            ps.setLong(6, 0L);
            ps.setInt(7, order);
            ps.executeUpdate();
            HDResourceInMove hDResourceInMove = new HDResourceInMove(tp, r.getId(), null, null, 0L, 0L);
            Session.closeStatement(ps);
            con.close();
            return hDResourceInMove;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public HDResourceInMove(TransferProcess tp, ResourceId rid, boolean hasContent) {
        this.hasContent = false;
        this.ttId = 0L;
        this.transport = null;
        this.parent = tp;
        this.rid = rid;
        this.hasContent = hasContent;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getRid().getId());
        }
        if ("description".equals(key)) {
            try {
                return new TemplateString(TypeRegistry.getDescription(getRid().getType()));
            } catch (Exception ex) {
                Session.getLog().error("Error getting description", ex);
                return null;
            }
        } else if ("created".equals(key)) {
            return new TemplateString(getCreatedStr());
        } else {
            if ("deleted".equals(key)) {
                return new TemplateString(getDeletedStr());
            }
            if ("has_content".equals(key)) {
                return new TemplateString(hasContent());
            }
            if ("cmove_started".equals(key)) {
                if (getCMoveStarted() == null) {
                    return new TemplateString(Localizer.translateMessage("eeman.tp.cmove.notstarted"));
                }
                return new TemplateString(getCMoveStarted());
            } else if ("cmove_finished".equals(key)) {
                if (getCMoveFinished() == null) {
                    return new TemplateString(Localizer.translateMessage("eeman.tp.cmove.notfinished"));
                }
                return new TemplateString(getCMoveFinished());
            } else if ("error_detected".equals(key)) {
                return new TemplateString(errorDetected());
            } else {
                if ("tt".equals(key)) {
                    try {
                        return Ticket.getTicket(getTTId());
                    } catch (Exception ex2) {
                        Session.getLog().error("Error getting TT ", ex2);
                        return null;
                    }
                } else if ("transport_id".equals(key)) {
                    try {
                        return new TemplateString(getTTId());
                    } catch (Exception ex3) {
                        Session.getLog().error("Error getting transport ", ex3);
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public void physicalCreate(long hostId) throws Exception {
        Resource r = this.rid.get();
        try {
            ((HostDependentResource) r).physicalCreate(hostId);
            if (hostId == this.parent.getTargetHostId()) {
                setCreated(new Timestamp(System.currentTimeMillis()));
            }
        } catch (Exception ex) {
            createTT("Physical creation problem:", null, ex);
            throw ex;
        }
    }

    public void physicalDelete(long hostId) throws Exception {
        Resource r = this.rid.get();
        ((HostDependentResource) r).physicalDelete(hostId);
        if (hostId == this.parent.getTargetHostId()) {
            setCreated(null);
        } else if (hostId == this.parent.getSrcHostId()) {
            setDeleted(new Timestamp(System.currentTimeMillis()));
        }
    }

    private void setCreated(Timestamp ts) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE resources_in_move SET created = ? WHERE rid = ?");
            if (ts == null) {
                ps.setNull(1, 93);
            } else {
                ps.setTimestamp(1, ts);
            }
            ps.setLong(2, this.rid.getId());
            ps.executeUpdate();
            this.created = ts;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void setDeleted(Timestamp ts) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE resources_in_move SET deleted = ? WHERE rid = ?");
            if (ts == null) {
                ps.setNull(1, 93);
            } else {
                ps.setTimestamp(1, ts);
            }
            ps.setLong(2, this.rid.getId());
            ps.executeUpdate();
            this.deleted = ts;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isCreated() {
        return this.created != null;
    }

    public boolean isDeleted() {
        return this.deleted != null;
    }

    public boolean hasContent() {
        return this.f61r.hasCMI();
    }

    public ResourceTransport accuireTransport() throws Exception {
        if (this.transferJobId > 0) {
            Resource r = this.rid.get();
            Class transportClass = r.getResourceTransportClass();
            Method get = transportClass.getMethod("accuireTransport", Long.class);
            return (ResourceTransport) get.invoke(this, new Long(this.transferJobId));
        }
        return null;
    }

    public int transferContent() throws Exception {
        Session.getLog().debug("Inside HDResourceInMove::transferContent() transportAvailable = " + isTransportAvailable());
        if (!isTransportAvailable()) {
            initTransport();
        }
        return this.transport.executeTransfer();
    }

    public boolean isTransportAvailable() {
        return this.transferJobId > 0;
    }

    public ResourceId getRid() {
        return this.rid;
    }

    public ResourceTransport initTransport() throws Exception {
        Session.getLog().debug("Inside HDResourceInMove::initTransport");
        PreparedStatement ps = null;
        Resource r = this.rid.get();
        Collection c = new ArrayList();
        c.add(new Long(this.parent.getSrcHostId()));
        c.add(new Long(this.parent.getTargetHostId()));
        c.addAll(r.getTransportData());
        Class transportClass = r.getResourceTransportClass();
        Method create = transportClass.getMethod("createTransport", Collection.class);
        ResourceTransport rt = (ResourceTransport) create.invoke(this, c);
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE resources_in_move SET job_id = ? WHERE rid = ?");
            ps.setLong(1, rt.getId());
            ps.setLong(2, this.rid.getId());
            ps.executeUpdate();
            this.transferJobId = rt.getId();
            this.transport = rt;
            Session.closeStatement(ps);
            con.close();
            return rt;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Resource getResource() {
        return this.f61r;
    }

    public boolean updateHostId() {
        try {
            HostDependentResource r = (HostDependentResource) getResource();
            r.setHostId(this.parent.getTargetHostId());
            return true;
        } catch (Exception ex) {
            createTT("Error while switching server", "Unable to update host id", ex);
            return false;
        }
    }

    public void createTT(String title, String message, Exception ex) {
        Session.setLanguage(new Language(null));
        StringWriter st = new StringWriter();
        PrintWriter out = new PrintWriter(st);
        try {
            out.println(title + " " + TypeRegistry.getDescription(getRid().getType()));
            out.println("Account ID: " + this.parent.getAccId());
            out.println("Source server: " + this.parent.getSourceServer().getName());
            out.println("Target Server: " + this.parent.getTargetServer().getName());
            out.println("Resource ID: " + getRid().getId());
        } catch (Exception ex1) {
            out.println("Error creating TT: unable to generate TT body");
            ex1.printStackTrace(out);
        }
        if (message != null) {
            out.println(message);
        }
        if (ex != null) {
            ex.printStackTrace(out);
        }
        try {
            Ticket tt = Ticket.create("Transfer process " + this.parent.getId() + " error: " + title, 50, st.toString(), getRid().getId(), getRid().getType(), null, 0, 1, 0, 0, 0, 1);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE resources_in_move SET tt_id = ? WHERE rid = ?");
            ps.setLong(1, tt.getId());
            ps.setLong(2, getRid().getId());
            ps.executeUpdate();
            this.ttId = tt.getId();
            Session.closeStatement(ps);
            con.close();
        } catch (Exception ex12) {
            Session.getLog().error("Failed to create TT", ex12);
        }
    }

    public String getCreatedStr() {
        if (isCreated()) {
            return this.created.toString();
        }
        return Localizer.translateMessage("eeman.tp.hdr.notcreated");
    }

    public String getDeletedStr() {
        if (isDeleted()) {
            return this.deleted.toString();
        }
        return Localizer.translateMessage("eeman.tp.hdr.notdeleted");
    }

    public Timestamp getCMoveStarted() {
        if (this.transport != null) {
            return this.transport.getStarted();
        }
        return null;
    }

    public Timestamp getCMoveFinished() {
        if (this.transport != null) {
            return this.transport.getFinished();
        }
        return null;
    }

    public ResourceTransport getTransport() {
        return this.transport;
    }

    public void delete() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM resources_in_move WHERE rid = ?");
            ps.setLong(1, getRid().getId());
            if (this.transport != null) {
                this.transport.delete();
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean canBeMoved() {
        try {
            return ((HostDependentResource) this.f61r).canBeMovedTo(this.parent.getTargetHostId());
        } catch (Exception ex) {
            try {
                createTT("Can't move resource " + getRid().toString(), ex.getMessage(), null);
                return false;
            } catch (Exception ex1) {
                Session.getLog().error("Error during creation TT in HDResourceInMove::canBeMoved()", ex1);
                return false;
            }
        }
    }

    public boolean errorDetected() {
        return this.ttId > 0;
    }

    public long getTTId() {
        return this.ttId;
    }

    public void closeTT() throws Exception {
        PreparedStatement ps = null;
        if (this.ttId > 0) {
            Connection con = Session.getDb();
            try {
                Ticket tt = Ticket.getTicket(this.ttId);
                ps = con.prepareStatement("UPDATE resources_in_move SET tt_id=? WHERE rid = ? ");
                ps.setNull(1, 4);
                ps.setLong(2, getRid().getId());
                tt.FM_close();
                ps.executeUpdate();
                this.ttId = 0L;
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof HDResourceInMove) {
            HDResourceInMove hdResourceInMove = (HDResourceInMove) o;
            return this.parent.equals(hdResourceInMove.parent) && this.rid.equals(hdResourceInMove.rid);
        }
        return false;
    }
}
