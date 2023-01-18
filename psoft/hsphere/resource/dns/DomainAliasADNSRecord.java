package psoft.hsphere.resource.dns;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.MixedIPResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/DomainAliasADNSRecord.class */
public class DomainAliasADNSRecord extends DNSRecord implements IPDeletedResource {
    private String prefix;

    public DomainAliasADNSRecord(ResourceId id) throws Exception {
        super(id);
        this.prefix = "";
    }

    public DomainAliasADNSRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.prefix = "";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.prefix = ((String) i.next()) + ".";
        }
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("in initdone in a new Alias A DNS record");
        LinkedList ll = new LinkedList();
        ResourceId IpId = (ResourceId) recursiveGet("ip");
        if (IpId != null) {
            MixedIPResource ip = (MixedIPResource) IpId.get();
            ll.add(this.prefix + ((DNSZone) getParent().get()).getName());
            ll.add("A");
            ll.add("86400");
            ll.add(ip.toString());
            initDNS(ll);
            super.initDone();
            return;
        }
        throw new Exception("IP not found");
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        delete(false);
    }

    public String getPrefix() {
        return this.prefix;
    }
}
