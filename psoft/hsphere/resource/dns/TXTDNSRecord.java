package psoft.hsphere.resource.dns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.email.SPFResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/TXTDNSRecord.class */
public class TXTDNSRecord extends DNSRecord {
    public TXTDNSRecord(ResourceId id) throws Exception {
        super(id);
    }

    public TXTDNSRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.data = "";
        if (i.hasNext()) {
            this.data = (String) i.next();
        }
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("in initdone in a new TXT DNS record");
        if (this.data.equalsIgnoreCase("")) {
            this.data = ((SPFResource) getParent().get()).getTXTRecordData();
        }
        List l = new ArrayList();
        l.add(recursiveGet("name").toString());
        l.add("TXT");
        l.add("86400");
        l.add(this.data);
        initDNS(l);
        super.initDone();
    }

    public void update(String newData) throws Exception {
        if (!this.data.equalsIgnoreCase(newData)) {
            delete();
            this.data = newData;
            initDone();
        }
    }
}
