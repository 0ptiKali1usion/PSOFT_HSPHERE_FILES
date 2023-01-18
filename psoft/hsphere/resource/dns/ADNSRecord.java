package psoft.hsphere.resource.dns;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Reconfigurable;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/ADNSRecord.class */
public class ADNSRecord extends DNSRecord implements Reconfigurable {
    private String prefix;

    public ADNSRecord(ResourceId id) throws Exception {
        super(id);
        this.prefix = "";
    }

    public ADNSRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.prefix = "";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.prefix = ((String) i.next()) + ".";
        }
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("in initdone in a new A DNS record");
        LinkedList ll = new LinkedList();
        ll.add(this.prefix + recursiveGet("name").toString());
        ll.add("A");
        ll.add("86400");
        ll.add(recursiveGet("ip").toString());
        initDNS(ll);
        super.initDone();
    }

    @Override // psoft.hsphere.resource.Reconfigurable
    public void reconfigure() throws Exception {
        String newA = recursiveGet("ip").toString();
        Session.getLog().debug("New A data = " + newA);
        if (newA != null) {
            physicalDelete(this.data);
            this.data = newA;
            physicalCreate(newA);
        }
    }

    public String getPrefix() {
        return this.prefix;
    }
}
