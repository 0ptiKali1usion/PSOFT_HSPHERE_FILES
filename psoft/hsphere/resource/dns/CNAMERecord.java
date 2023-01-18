package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Reconfigurable;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/CNAMERecord.class */
public class CNAMERecord extends DNSRecord implements Reconfigurable {
    private String prefix;
    private String cname;

    public CNAMERecord(ResourceId id) throws Exception {
        super(id);
    }

    public CNAMERecord(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.prefix = ((String) i.next()) + ".";
        this.cname = (String) i.next();
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("in initdone in a new CNAME DNS record");
        LinkedList ll = new LinkedList();
        ll.add(this.prefix + recursiveGet("name").toString());
        ll.add("CNAME");
        ll.add("86400");
        ll.add(this.cname);
        initDNS(ll);
        super.initDone();
    }

    @Override // psoft.hsphere.resource.Reconfigurable
    public void reconfigure() throws Exception {
        TemplateModel newCname = recursiveGet("mail_server_name");
        if (newCname != null) {
            physicalDelete(this.cname);
            this.cname = newCname.toString();
            Session.getLog().debug("New CNAME data = " + this.cname);
            physicalCreate(this.cname);
        }
    }

    public String getPrefix() {
        return this.prefix;
    }
}
