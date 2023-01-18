package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Category;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool.class */
public class ResourceTool {
    private static Category log = Category.getInstance(ResourceTool.class.getName());
    private boolean doDelete;
    private boolean doCreate;
    private int rGroup;
    private long lServerId;
    ToolLogger logger = ToolLogger.getDefaultLogger();

    public ResourceTool(boolean doDelete, int rGroup, boolean doCreate, long lServerId) {
        this.doDelete = false;
        this.doCreate = false;
        this.lServerId = -1L;
        this.doDelete = doDelete;
        this.doCreate = doCreate;
        this.rGroup = rGroup;
        this.lServerId = lServerId;
    }

    public void reset(Resource r) throws Exception {
        TypeList tl;
        if (this.rGroup == 5) {
            tl = new WindowsDependentTypeList();
        } else if (this.rGroup == 1) {
            tl = new UnixVHostingDependentTypeList();
        } else if (this.rGroup == 4) {
            tl = new MySQLDependentTypeList();
        } else if (this.rGroup == 18) {
            tl = new PGSQLDependentTypeList();
        } else if (this.rGroup == 3) {
            tl = new MailDependentTypeList();
        } else if (this.rGroup == 15) {
            tl = new MSSQLDependentTypeList();
        } else {
            throw new Exception("Unsupported resource group " + this.rGroup);
        }
        List<ResourceId> rids = new HostDependentResourceIterator(r.getId().getAllChildrenByPriority(), tl.getTypeList()).getValues();
        this.logger.outMessage("Found " + rids.size() + " for " + r.getId().getId() + '\n');
        if (this.doDelete) {
            this.logger.outMessage("Deleting resources\n");
            ListIterator i = rids.listIterator(rids.size());
            while (i.hasPrevious()) {
                ResourceId rId = (ResourceId) i.previous();
                try {
                    this.logger.outMessage("Loading " + rId.toString());
                    HostDependentResource tr = (HostDependentResource) rId.get();
                    this.logger.outOK();
                    if (this.lServerId <= 0 || tr.getHostId() == this.lServerId) {
                        try {
                            this.logger.outMessage("Deleting");
                            tr.physicalDelete(tr.getHostId());
                            this.logger.outOK();
                        } catch (Exception ex1) {
                            this.logger.outFail("Error deleting " + rId.toString(), ex1);
                        }
                    }
                } catch (Exception ex) {
                    this.logger.outFail("Error getting " + rId.toString(), ex);
                }
            }
        }
        if (this.doCreate) {
            this.logger.outMessage("Creating resources\n");
            for (ResourceId rId2 : rids) {
                try {
                    this.logger.outMessage("Loading " + rId2.toString());
                    HostDependentResource tr2 = (HostDependentResource) rId2.get();
                    if (this.lServerId <= 0 || tr2.getHostId() == this.lServerId) {
                        this.logger.outOK();
                        try {
                            this.logger.outMessage("Creating ");
                            tr2.physicalCreate(tr2.getHostId());
                            this.logger.outOK();
                        } catch (Exception ex12) {
                            this.logger.outFail("Error creating " + rId2.toString(), ex12);
                        }
                    }
                } catch (Exception ex2) {
                    this.logger.outFail("Error getting " + rId2.toString(), ex2);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$HostDependentResourceIterator.class */
    public class HostDependentResourceIterator implements Iterator {

        /* renamed from: l */
        protected List f158l = new ArrayList();

        /* renamed from: i */
        protected Iterator f159i;

        public HostDependentResourceIterator(List rids, List typeList) {
            ResourceTool.this = r6;
            Iterator iter = rids.iterator();
            while (iter.hasNext()) {
                ResourceId rid = (ResourceId) iter.next();
                if (typeList.contains(new Integer(rid.getType()))) {
                    this.f158l.add(rid);
                }
            }
            this.f159i = this.f158l.iterator();
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.f159i.hasNext();
        }

        @Override // java.util.Iterator
        public Object next() throws NoSuchElementException {
            return this.f159i.next();
        }

        @Override // java.util.Iterator
        public void remove() throws UnsupportedOperationException, IllegalStateException {
            this.f159i.remove();
        }

        public List getValues() {
            return this.f158l;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$TypeList.class */
    public abstract class TypeList {
        public abstract List getTypeList();

        public TypeList() {
            ResourceTool.this = r4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$WindowsDependentTypeList.class */
    public class WindowsDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        WindowsDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer(7), new Integer(9), new Integer(10), new Integer(11), new Integer(12), new Integer(15), new Integer(16), new Integer(18), new Integer(22), new Integer(23), new Integer(25), new Integer(26), new Integer(27), new Integer(28), new Integer(30), new Integer(50), new Integer(55), new Integer(60), new Integer(61), new Integer(63), new Integer(66), new Integer(2001), new Integer(2002), new Integer(2010), new Integer(2153), new Integer(4001), new Integer(6003), new Integer(6004), new Integer(6006), new Integer(6106), new Integer(6300), new Integer(6400), new Integer(6500), new Integer(6550), new Integer(6800), new Integer(6801), new Integer(6802), new Integer(6803), new Integer(6804));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$UnixVHostingDependentTypeList.class */
    public class UnixVHostingDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        UnixVHostingDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer(7), new Integer(9), new Integer(17), new Integer(18), new Integer(19), new Integer(20), new Integer(23), new Integer(24), new Integer(28), new Integer(29), new Integer(30), new Integer(60), new Integer(2001), new Integer(2003), new Integer(2004), new Integer(2010), new Integer(2150), new Integer(4001), new Integer(6003), new Integer(6004), new Integer(6006));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$MySQLDependentTypeList.class */
    public class MySQLDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MySQLDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer(6001), new Integer(6002));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$PGSQLDependentTypeList.class */
    public class PGSQLDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        PGSQLDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer(6901), new Integer(6902));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$MailDependentTypeList.class */
    public class MailDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MailDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer((int) HostEntry.VPS_IP), new Integer((int) HostEntry.TAKEN_VPS_IP), new Integer(1002), new Integer(1003), new Integer(1005), new Integer(1008), new Integer(1011), new Integer(1012));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ResourceTool$MSSQLDependentTypeList.class */
    public class MSSQLDependentTypeList extends TypeList {
        public final List hostDepTypes;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MSSQLDependentTypeList() {
            super();
            ResourceTool.this = r9;
            this.hostDepTypes = Arrays.asList(new Integer(6800), new Integer(6801), new Integer(6802), new Integer(6803), new Integer(6804));
        }

        @Override // psoft.hsphere.resource.ResourceTool.TypeList
        public List getTypeList() {
            return this.hostDepTypes;
        }
    }
}
