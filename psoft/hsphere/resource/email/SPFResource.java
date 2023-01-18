package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.dns.TXTDNSRecord;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/SPFResource.class */
public class SPFResource extends Resource {
    protected String processing;
    public static final String DEFAULT_PROCESSING_VALUE = "fail";
    protected static HashMap processingMap = new HashMap();

    static {
        processingMap.put(DEFAULT_PROCESSING_VALUE, "-");
        processingMap.put("softfail", "~");
        processingMap.put("pass", "+");
        processingMap.put("neutral", "?");
    }

    public SPFResource(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT processing FROM spf WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.processing = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public SPFResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        if (i.hasNext()) {
            this.processing = (String) i.next();
        } else {
            this.processing = DEFAULT_PROCESSING_VALUE;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO spf(id, processing) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.processing);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM spf WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "processing".equals(key) ? new TemplateString(this.processing) : super.get(key);
    }

    public String getTXTRecordData() throws Exception {
        try {
            return "v=spf1 a mx " + processingMap.get(this.processing) + "all";
        } catch (Exception ex) {
            throw new Exception("Can't get processing parameter " + ex);
        }
    }

    public TemplateModel FM_getName() throws Exception {
        return new TemplateString(recursiveGet("name").toString());
    }

    public TemplateModel FM_update(String newProcessing, String useInDomainAliases) throws Exception {
        update(newProcessing, useInDomainAliases);
        return this;
    }

    public void update(String newProcessing, String useInDomainAliases) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE spf SET processing = ? WHERE id = ?");
            ps.setString(1, newProcessing);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.processing = newProcessing;
            ResourceId rid = getId().findChild("txt_record");
            if (rid != null) {
                TXTDNSRecord txt = (TXTDNSRecord) rid.get();
                txt.update(getTXTRecordData());
            }
            if (!useInDomainAliases.equalsIgnoreCase("")) {
                Resource r = getParent().get();
                if (r instanceof MailService) {
                    List al = ((MailService) r).getSPFOfDomainAliases();
                    for (int i = 0; i < al.size(); i++) {
                        SPFResource sp = (SPFResource) ((ResourceId) al.get(i)).get();
                        sp.update(this.processing, "");
                    }
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getProperties() throws Exception {
        List prop = new ArrayList();
        prop.add(this.processing);
        return prop;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.spf.refund", new Object[]{recursiveGet("name").toString(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.spf.setup", new Object[]{recursiveGet("name").toString()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.spf.recurrent", new Object[]{getPeriodInWords(), recursiveGet("name").toString(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.spf.refundall", new Object[]{recursiveGet("name").toString(), f42df.format(begin), f42df.format(end)});
    }
}
