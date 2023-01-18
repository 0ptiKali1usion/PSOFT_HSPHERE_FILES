package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import psoft.hsphere.plan.ResourceType;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/ResourceId.class */
public class ResourceId implements TemplateHashModel, TemplateScalarModel, Serializable {

    /* renamed from: id */
    protected long f44id;
    protected int type;
    protected int hash;
    protected long accountId = -1;
    public static final int SERVICE_TYPE = -1000;
    public static final Map locks = new HashMap();
    public static final int CUSTOM_JOB = -1001;

    public ResourceId(String encoded) {
        int i = encoded.indexOf(95);
        init(Long.parseLong(encoded.substring(0, i)), Integer.parseInt(encoded.substring(i + 1, encoded.length())));
    }

    public Account getAccount() throws Exception {
        ResourceId rid = new ResourceId(getAccountId(), 0);
        return (Account) Account.get(rid);
    }

    public static int getTypeById(long childId) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT child_type FROM parent_child WHERE child_id = ?");
            ps.setLong(1, childId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int i = rs.getInt(1);
                con.close();
                return i;
            }
            throw new Exception("Unknown Resource " + childId);
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    private void init(long id, int type) {
        this.f44id = id;
        this.type = type;
        this.hash = toString().hashCode();
    }

    public static ResourceId getServiceId(long id) {
        return new ResourceId(id, -1000);
    }

    public ResourceId(long id, int type) {
        init(id, type);
    }

    public long getId() {
        return this.f44id;
    }

    public String getNamedType() {
        try {
            return TypeRegistry.getType(this.type);
        } catch (NoSuchTypeException nste) {
            nste.printStackTrace();
            throw new Error(nste.getMessage());
        }
    }

    public int getType() {
        return this.type;
    }

    public boolean equals(Object o) {
        ResourceId rhs = (ResourceId) o;
        return this.f44id == rhs.f44id && this.type == rhs.type;
    }

    public int hashCode() {
        return this.hash;
    }

    public String toString() {
        return getAsString();
    }

    public String getAsString() {
        return Long.toString(this.f44id) + "_" + Long.toString(this.type);
    }

    public boolean isEmpty() {
        return false;
    }

    public Resource get() throws Exception {
        return Resource.get(this);
    }

    public void softSetPeriodBegin(Date begin) throws Exception {
        Resource r = Resource.softGet(this);
        if (r != null) {
            r.setPeriodBegin(begin);
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.getLog().info("Soft setPeriodBegin for" + this);
            ps = con.prepareStatement("UPDATE parent_child SET p_begin = ? WHERE child_id = ?");
            ps.setDate(1, new java.sql.Date(begin.getTime()));
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_findChildren(String type) throws Exception {
        return new TemplateList(findChildren(type));
    }

    public Collection findChildren(String type) throws Exception {
        return findChildren(type, new LinkedList());
    }

    public Collection findChildren(String type, Collection foundChild) throws Exception {
        Collection c = getChildHolder().getChildrenByName(type);
        if (c != null && c.size() > 0) {
            foundChild.addAll(c);
            return foundChild;
        }
        Collection<ResourceId> children = getChildHolder().getChildren();
        synchronized (children) {
            for (ResourceId rs : children) {
                if (!equals(rs)) {
                    rs.findChildren(type, foundChild);
                }
            }
        }
        return foundChild;
    }

    public TemplateModel FM_findAllChildren(String type) throws Exception {
        return new TemplateList(findAllChildren(type));
    }

    public Collection findAllChildren(String type) throws Exception {
        return findAllChildren(type, new LinkedList());
    }

    public Collection findAllChildren(String type, Collection foundChild) throws Exception {
        int lookedTypeId = Integer.parseInt(TypeRegistry.getTypeId(type));
        for (ResourceId rs : Session.getAccount().getChildManager().getAllResources()) {
            if (rs.getType() == lookedTypeId) {
                foundChild.add(rs);
            }
        }
        return foundChild;
    }

    public TemplateModel FM_findChild(String type) throws Exception {
        return findChild(type);
    }

    public ResourceId findChild(String type) throws Exception {
        ResourceId rsId;
        Collection c = getChildHolder().getChildrenByName(type);
        if (c != null) {
            synchronized (c) {
                Iterator i = c.iterator();
                if (i.hasNext()) {
                    Session.getLog().debug("Find Children " + type + " found " + c);
                    return (ResourceId) i.next();
                }
            }
        }
        Collection<ResourceId> c2 = getChildHolder().getChildren();
        synchronized (c2) {
            for (ResourceId rs : c2) {
                if (!equals(rs) && (rsId = rs.findChild(type)) != null) {
                    return rsId;
                }
            }
            return null;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.f44id);
        }
        if ("type".equals(key)) {
            return new TemplateString(getNamedType());
        }
        if ("type_id".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("description".equals(key)) {
            try {
                return new TemplateString(Resource.getDescription(this));
            } catch (SQLException e) {
                Session.getLog().warn("Error getting description", e);
                return new TemplateString(e.getMessage());
            }
        } else if (key.equals("getSortedChildrenList")) {
            return new SortedChildrenLister(this);
        } else {
            TemplateMethodModel tmm = AccessTemplateMethodWrapper.getMethod(this, key);
            if (tmm == null || tmm.isEmpty()) {
                try {
                    return get().get(key);
                } catch (Exception e2) {
                    Session.getLog().warn("Error getting resource", e2);
                    return null;
                }
            }
            return tmm;
        }
    }

    public TemplateModel FM_getChildren(String type) throws Exception {
        return new TemplateList(getChildHolder().getChildrenByName(type));
    }

    public List getChildrenSorted(List types) throws Exception {
        List children = new ArrayList();
        Iterator i = types.iterator();
        while (i.hasNext()) {
            children.addAll(getChildHolder().getChildrenByName((String) i.next()));
        }
        Collections.sort(children, AlphanumResourceComparator.ASCENDING);
        return children;
    }

    public TemplateModel FM_getChildrenSorted(String type) throws Exception {
        Comparator c = Resource.getComparator(type);
        if (c == null) {
            return FM_getChildren(type);
        }
        List l = new ArrayList();
        l.addAll(getChildHolder().getChildrenByName(type));
        Collections.sort(l, new ResourceComparator(c));
        return new ListAdapter(l);
    }

    public ResourceId FM_getChild(String type) throws Exception {
        Collection c = getChildHolder().getChildrenByName(type);
        if (c != null) {
            synchronized (c) {
                Iterator i = c.iterator();
                if (i.hasNext()) {
                    ResourceId rId = (ResourceId) i.next();
                    try {
                        Plan plan = Resource._getPlan();
                        ResourceType rType = plan.getResourceType(rId.getType());
                        if (rType != null && !rType.isDisabled()) {
                            Session.getLog().debug("rId = " + rId.getId() + ", typeId = " + rId.getType() + ", type = " + type);
                            return rId;
                        }
                    } catch (Exception e) {
                        Session.getLog().warn("FM_getChild, error: ", e);
                        return null;
                    }
                }
                return null;
            }
        }
        return null;
    }

    public ChildHolder getChildHolder() {
        return Session.getAccount().getChildManager().getChildHolder(this);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/ResourceId$Counter.class */
    public class Counter {

        /* renamed from: i */
        private int f45i = 0;

        Counter() {
            ResourceId.this = r4;
        }

        public void inc() {
            this.f45i++;
        }

        public void dec() {
            this.f45i--;
        }

        public boolean isEmpty() {
            return this.f45i == 0;
        }
    }

    public Object getLock() {
        Counter counter;
        synchronized (Counter.class) {
            Counter count = (Counter) locks.get(this);
            if (count == null) {
                count = new Counter();
                locks.put(this, count);
            }
            count.inc();
            counter = count;
        }
        return counter;
    }

    public void releaseLock() {
        synchronized (Counter.class) {
            Counter count = (Counter) locks.get(this);
            count.dec();
            if (count.isEmpty()) {
                locks.remove(this);
            }
        }
    }

    public List getAllChildrenByPriority() {
        return getAllChildrenByPriority(new ArrayList());
    }

    public List getAllChildrenByPriority(List rids) {
        if (!rids.contains(this)) {
            rids.add(this);
        }
        Collection<ResourceId> col = getChildHolder().getChildrenByPriority();
        for (ResourceId rid : col) {
            if (!equals(rid)) {
                rid.getAllChildrenByPriority(rids);
            }
        }
        return rids;
    }

    public static ResourceId getCustomJobId(long id) {
        return new ResourceId(id, -1001);
    }

    public boolean isMonthly() {
        return TypeRegistry.isMonthly(getType());
    }

    public boolean isNonRefundable() {
        return TypeRegistry.isNonRefundable(getType());
    }

    public boolean isChangeable() {
        return TypeRegistry.isChangeable(getType());
    }

    public boolean isDummy() {
        return TypeRegistry.isDummy(getType());
    }

    public synchronized long getAccountId() throws Exception {
        if (this.accountId < 0) {
            Connection con = Session.getDb();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT account_id FROM parent_child WHERE child_id = ? and child_type = ?");
                ps.setLong(1, this.f44id);
                ps.setLong(2, this.type);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    setAccountId(rs.getLong(1));
                } else {
                    throw new Exception("Unknown Resource " + this);
                }
            } finally {
                con.close();
            }
        }
        return this.accountId;
    }

    private void setAccountId(long _accountId) {
        this.accountId = _accountId;
    }
}
