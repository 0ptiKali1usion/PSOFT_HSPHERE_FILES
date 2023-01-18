package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.dns.ServiceDNSZone;
import psoft.hsphere.tools.DNSCreator;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/RecreationManagerResource.class */
public class RecreationManagerResource extends Resource {
    public static final List typeList = Arrays.asList(new Integer(9), new Integer(10), new Integer(11), new Integer(12), new Integer(15), new Integer(16), new Integer(18), new Integer(22), new Integer(23), new Integer(25), new Integer(26), new Integer(27), new Integer(30), new Integer(50), new Integer(55), new Integer(61), new Integer(63), new Integer(2001), new Integer(2002), new Integer(4001), new Integer(4002), new Integer(6300), new Integer(6500), new Integer(6550), new Integer(7000));

    public RecreationManagerResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public RecreationManagerResource(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_getRecreatableResources(int aid) throws Exception {
        TemplateList listOfRecreatableResources = new TemplateList();
        ResourceId uaid = new ResourceId(aid, 0);
        Account newAccount = (Account) Account.get(uaid);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Session.setUser(newAccount.getUser());
            Session.setAccount(newAccount);
            Collection<ResourceId> resources = newAccount.getChildManager().getAllResources();
            for (ResourceId currentResource : resources) {
                switch (currentResource.getType()) {
                    case 9:
                        TemplateModel templateHash = new TemplateHash();
                        templateHash.put("name", Resource.get(currentResource).recursiveGet("name").toString());
                        templateHash.put("id", currentResource);
                        getHesh("Hosting", listOfRecreatableResources).get("list").add(templateHash);
                        break;
                    case 2001:
                        TemplateModel templateHash2 = new TemplateHash();
                        templateHash2.put("name", Resource.get(currentResource).get("serverName").toString());
                        templateHash2.put("id", currentResource);
                        getHesh("FTP", listOfRecreatableResources).get("list").add(templateHash2);
                        break;
                    case 3001:
                    case 3004:
                    case 3005:
                        TemplateModel templateHash3 = new TemplateHash();
                        templateHash3.put("name", Resource.get(currentResource).recursiveGet("name").toString());
                        templateHash3.put("id", currentResource);
                        getHesh("DNS", listOfRecreatableResources).get("list").add(templateHash3);
                        break;
                    case 4001:
                        TemplateModel templateHash4 = new TemplateHash();
                        templateHash4.put("name", "Disk space quota");
                        templateHash4.put("id", currentResource);
                        getHesh("DiskSpaceQuota", listOfRecreatableResources).get("list").add(templateHash4);
                        break;
                    case 4002:
                        TemplateModel templateHash5 = new TemplateHash();
                        templateHash5.put("name", "Windows quota");
                        templateHash5.put("id", currentResource);
                        getHesh("WinQuota", listOfRecreatableResources).get("list").add(templateHash5);
                        break;
                    case 7000:
                        TemplateModel templateHash6 = new TemplateHash();
                        templateHash6.put("name", Resource.get(currentResource).get("vpsHostName").toString());
                        templateHash6.put("id", currentResource);
                        getHesh("VPS Resource", listOfRecreatableResources).get("list").add(templateHash6);
                        break;
                }
            }
            return listOfRecreatableResources;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    private TemplateHash getHesh(String key, TemplateList from) {
        from.rewind();
        while (from.hasNext()) {
            TemplateHash result = from.next();
            if (result.get("name").toString().equals(key)) {
                return result;
            }
        }
        TemplateHash intermediate = new TemplateHash();
        intermediate.put("name", key);
        intermediate.put("list", new TemplateList());
        from.add((TemplateModel) intermediate);
        return intermediate;
    }

    public TemplateModel FM_recreateResource(String resourceId, int aid) throws Exception {
        TemplateHash returnValue = new TemplateHash();
        TemplateList resultList = new TemplateList();
        returnValue.put("result_list", resultList);
        ResourceId uaid = new ResourceId(aid, 0);
        Account newAccount = (Account) Account.get(uaid);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Session.setUser(newAccount.getUser());
            Session.setAccount(newAccount);
            ResourceId rId = new ResourceId(resourceId);
            List listOfAllSubresources = new RecreateableResourceIterator(rId.getAllChildrenByPriority()).getValues();
            ListIterator li = listOfAllSubresources.listIterator(listOfAllSubresources.size());
            while (li.hasPrevious()) {
                Resource r = ((ResourceId) li.previous()).get();
                if (r instanceof HostDependentResource) {
                    HostDependentResource hdr = (HostDependentResource) r;
                    try {
                        hdr.physicalDelete(hdr.getHostId());
                    } catch (Exception ex) {
                        Session.getLog().info("Recreation manager error imposiible to delete " + r.getId().getId(), ex);
                    }
                }
            }
            ListIterator li2 = listOfAllSubresources.listIterator();
            while (li2.hasNext()) {
                Resource r2 = ((ResourceId) li2.next()).get();
                if (r2 instanceof HostDependentResource) {
                    TemplateHash intermediate = new TemplateHash();
                    intermediate.put("name", r2.getDescription());
                    try {
                        HostDependentResource hdr2 = (HostDependentResource) r2;
                        hdr2.physicalCreate(hdr2.getHostId());
                        intermediate.put("status", "[OK]");
                        resultList.add((TemplateModel) intermediate);
                    } catch (Exception e) {
                        intermediate.put("status", "[FAILED]");
                        resultList.add((TemplateModel) intermediate);
                    }
                }
            }
            return returnValue;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            returnValue.put("status", get("status"));
        }
    }

    public TemplateModel FM_recreateDNS(String resourceId, int method, int aid) throws Exception {
        TemplateHash returnValue = new TemplateHash();
        TemplateList resultList = new TemplateList();
        returnValue.put("result_list", resultList);
        ResourceId uaid = new ResourceId(aid, 0);
        Account newAccount = (Account) Account.get(uaid);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Session.setUser(newAccount.getUser());
            Session.setAccount(newAccount);
            String zone = null;
            TemplateHash intermediate = new TemplateHash();
            try {
                Resource dnsZone = Resource.get(new ResourceId(resourceId));
                if (dnsZone instanceof DNSZone) {
                    zone = ((DNSZone) dnsZone).getName();
                } else if (dnsZone instanceof ServiceDNSZone) {
                    zone = dnsZone.get("name").toString();
                }
                DNSCreator.m17go(method, true, zone);
                intermediate.put("name", dnsZone.getDescription());
                if (Session.getAccount().getPlan().isDemoPlan()) {
                    intermediate.put("status", "[SKIPPED]");
                } else {
                    intermediate.put("status", "[OK]");
                }
                resultList.add((TemplateModel) intermediate);
            } catch (Exception e) {
                Session.getLog().error("Incorrect parameters in DNSCreator", e);
                intermediate.put("name", zone);
                intermediate.put("status", "[FAILED]");
                resultList.add((TemplateModel) intermediate);
            }
            returnValue.put("status", get("status"));
            return returnValue;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/RecreationManagerResource$RecreateableResourceIterator.class */
    class RecreateableResourceIterator implements Iterator {

        /* renamed from: l */
        protected List f173l = new ArrayList();

        /* renamed from: i */
        protected Iterator f174i;

        public RecreateableResourceIterator(List rids) {
            RecreationManagerResource.this = r6;
            Iterator iter = rids.iterator();
            while (iter.hasNext()) {
                ResourceId rid = (ResourceId) iter.next();
                if (RecreationManagerResource.typeList.contains(new Integer(rid.getType()))) {
                    this.f173l.add(rid);
                }
            }
            this.f174i = this.f173l.iterator();
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.f174i.hasNext();
        }

        @Override // java.util.Iterator
        public Object next() throws NoSuchElementException {
            return this.f174i.next();
        }

        @Override // java.util.Iterator
        public void remove() throws UnsupportedOperationException, IllegalStateException {
            this.f174i.remove();
        }

        public List getValues() {
            return this.f173l;
        }
    }
}
