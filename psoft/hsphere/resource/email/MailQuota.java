package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailQuota.class */
public class MailQuota extends Quota implements HostDependentResource {
    public MailQuota(ResourceId id) throws Exception {
        super(id);
    }

    public MailQuota(int type, Collection values) throws Exception {
        super(type, values);
    }

    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO quotas VALUES(?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.size);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (!Session.getPropertyString("IRIS_USER").equals("")) {
                boolean is_catch_all = false;
                if (((MailDomain) getParent().get().getParent().get()).getCatchAll().equals(recursiveGet("email").toString())) {
                    is_catch_all = true;
                }
                MailServices.setMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), this.size, is_catch_all);
                return;
            }
            MailServices.setMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), this.size, false);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (!Session.getPropertyString("IRIS_USER").equals("")) {
                boolean is_catch_all = false;
                if (((MailDomain) getParent().get().getParent().get()).getCatchAll().equals(recursiveGet("email").toString())) {
                    is_catch_all = true;
                }
                MailServices.setMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), 0, is_catch_all);
            } else if (!getParent().get().isDeletePrev()) {
                MailServices.setMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), 0, false);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM quotas WHERE id = ?");
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
        if ("limitMb".equals(key)) {
            return new TemplateString(quotaReport(1));
        }
        if ("usedMb".equals(key)) {
            return new TemplateString(quotaReport(0));
        }
        if (!"reload".equals(key)) {
            return "info".equals(key) ? new TemplateString(info()) : super.get(key);
        }
        this.report.put("time", null);
        return new TemplateOKResult();
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        try {
            double value = USFormat.parseDouble(quotaReport(0));
            try {
                warn = Integer.parseInt(Settings.get().getValue("quota_warn"));
            } catch (Exception e) {
                warn = 80;
            }
            return (value * 100.0d) / ((double) this.size) > ((double) warn);
        } catch (ParseException e2) {
            return false;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            Resource mbox = getParent().get();
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"Mail Quota", "mailbox", mbox.get("fullemail").toString(), quotaReport(0), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        if (!Session.getPropertyString("IRIS_USER").equals("")) {
            boolean is_catch_all = false;
            if (((MailDomain) getParent().get().getParent().get()).getCatchAll().equals(recursiveGet("email").toString())) {
                is_catch_all = true;
            }
            MailServices.setMboxQuota(HostManager.getHost(targetHostId), recursiveGet("fullemail").toString(), this.size, is_catch_all);
            return;
        }
        MailServices.setMboxQuota(HostManager.getHost(targetHostId), recursiveGet("fullemail").toString(), this.size, false);
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (!Session.getPropertyString("IRIS_USER").equals("")) {
            boolean is_catch_all = false;
            if (((MailDomain) getParent().get().getParent().get()).getCatchAll().equals(recursiveGet("email").toString())) {
                is_catch_all = true;
            }
            MailServices.setMboxQuota(HostManager.getHost(targetHostId), recursiveGet("fullemail").toString(), 0, is_catch_all);
            return;
        }
        MailServices.setMboxQuota(HostManager.getHost(targetHostId), recursiveGet("fullemail").toString(), 0, false);
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("mail_server").getId();
    }

    protected String _getFullEmail() {
        try {
            return recursiveGet("fullemail").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mail_quota.refund", new Object[]{new Double(this.size), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.mail_quota.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mail_quota.refundall", new Object[]{new Double(this.size), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.mail_quota.recurrent_change", new Object[]{getPeriodInWords(), new Double(delta), _getFullEmail(), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.mail_quota.refund_change", new Object[]{new Double(-delta), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getTotalAmount() {
        return getAmount();
    }

    public static double getTotalAmount(InitToken token) {
        try {
            return token.getAmount();
        } catch (Exception e) {
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    protected String[] getActualQuotaReport() {
        Collection retv;
        if (HostEntry.getEmulationMode()) {
            return new String[]{String.valueOf(this.size / 2), String.valueOf(this.size)};
        }
        String[] rep = new String[2];
        try {
            if (!Session.getPropertyString("IRIS_USER").equals("")) {
                boolean is_catch_all = false;
                if (((MailDomain) getParent().get().getParent().get()).getCatchAll().equals(recursiveGet("email").toString())) {
                    is_catch_all = true;
                }
                retv = MailServices.getMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), is_catch_all);
            } else {
                retv = MailServices.getMboxQuota(recursiveGet("mail_server"), recursiveGet("fullemail").toString(), false);
            }
            if (retv.size() != 2) {
                return quotaNA;
            }
            Iterator i = retv.iterator();
            rep[0] = (String) i.next();
            rep[1] = (String) i.next();
            return rep;
        } catch (Throwable e) {
            getLog().warn("can not get quota for " + getId(), e);
            return quotaNA;
        }
    }
}
