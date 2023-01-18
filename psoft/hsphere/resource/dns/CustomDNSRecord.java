package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/CustomDNSRecord.class */
public class CustomDNSRecord extends DNSRecord {
    public CustomDNSRecord(ResourceId id) throws Exception {
        super(id);
    }

    public CustomDNSRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
        initDNS(initValues);
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if ("MX".equals(this.type)) {
                ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND data = ? AND type = 'MX'");
                ps.setString(1, this.name);
                ps.setString(2, this.data);
            }
            if ("A".equals(this.type)) {
                ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND type = 'A'");
                ps.setString(1, this.name);
            }
            if ("TXT".equals(this.type)) {
                ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND type = 'TXT'");
                ps.setString(1, this.name);
            }
            if (ps != null) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new HSUserException("customdnsrecord.dns");
                }
            }
            super.initDone();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "rec_type".equals(key) ? new TemplateString(this.type) : super.get(key);
    }

    protected String _getName() {
        return this.name + " " + this.type;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cust_dns_record.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.cust_dns_record.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cust_dns_record.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.cust_dns_record.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
