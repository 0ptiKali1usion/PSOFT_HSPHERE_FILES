package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ThrottleResource.class */
public class ThrottleResource extends VHResource {
    private static String POLICY_NONE = "None";
    private static String POLICY_CONCURRENT = "Concurrent";
    private static String POLICY_DOCUMENT = "Document";
    private static String POLICY_IDLE = "Idle";
    private static String POLICY_ORIGINAL = "Original";
    private static String POLICY_RANDOM = "Random";
    private static String POLICY_REQUEST = "Request";
    private static String POLICY_SPEED = "Speed";
    private static String POLICY_VOLUME = "Volume";
    private PolicyValues policy;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ThrottleResource$PolicyValues.class */
    public class PolicyValues {
        public String type = ThrottleResource.POLICY_NONE;
        public int limit = 0;
        public int interval = 0;
        public String limitUn = "";
        public String intervalUn = "";

        public PolicyValues() {
            ThrottleResource.this = r4;
        }

        public void setValues(String polycyType, String polycyLimit, String policyLimitUn, String policyInterv, String policyIntervUn) throws Exception {
            String limit_un;
            String interval_un;
            if ("K".equals(policyLimitUn) || "M".equals(policyLimitUn) || "G".equals(policyLimitUn)) {
                limit_un = policyLimitUn;
            } else {
                limit_un = "";
            }
            if ("s".equals(policyIntervUn) || "m".equals(policyIntervUn) || "h".equals(policyIntervUn) || "d".equals(policyIntervUn) || "w".equals(policyIntervUn)) {
                interval_un = policyIntervUn;
            } else {
                interval_un = "";
            }
            if (!ThrottleResource.POLICY_ORIGINAL.equals(polycyType) && !ThrottleResource.POLICY_SPEED.equals(polycyType) && !ThrottleResource.POLICY_VOLUME.equals(polycyType)) {
                if (!ThrottleResource.POLICY_CONCURRENT.equals(polycyType) && !ThrottleResource.POLICY_DOCUMENT.equals(polycyType) && !ThrottleResource.POLICY_REQUEST.equals(polycyType) && !ThrottleResource.POLICY_RANDOM.equals(polycyType)) {
                    if (ThrottleResource.POLICY_IDLE.equals(polycyType)) {
                        this.type = polycyType;
                        this.limit = 0;
                        this.interval = Integer.parseInt(policyInterv);
                        this.limitUn = "";
                        this.intervalUn = "";
                        return;
                    }
                    this.type = ThrottleResource.POLICY_NONE;
                    this.limit = 0;
                    this.interval = 0;
                    this.limitUn = "";
                    this.intervalUn = "";
                    return;
                }
                this.type = polycyType;
                this.limit = Integer.parseInt(polycyLimit);
                this.interval = Integer.parseInt(policyInterv);
                this.limitUn = "";
                this.intervalUn = interval_un;
                return;
            }
            this.type = polycyType;
            this.limit = Integer.parseInt(polycyLimit);
            this.interval = Integer.parseInt(policyInterv);
            this.limitUn = limit_un;
            this.intervalUn = interval_un;
        }

        public void setValues(PolicyValues val) {
            this.type = val.type;
            this.limit = val.limit;
            this.limitUn = val.limitUn;
            this.interval = val.interval;
            this.intervalUn = val.intervalUn;
        }
    }

    public ThrottleResource(int type, Collection values) throws Exception {
        super(type, values);
        this.policy = new PolicyValues();
        Iterator i = values.iterator();
        String polycyType = i.hasNext() ? (String) i.next() : "";
        String polycyLimit = i.hasNext() ? (String) i.next() : "";
        String policyLimitUn = i.hasNext() ? (String) i.next() : "";
        String policyInterv = i.hasNext() ? (String) i.next() : "";
        String policyIntervUn = i.hasNext() ? (String) i.next() : "";
        this.policy.setValues(polycyType, polycyLimit, policyLimitUn, policyInterv, policyIntervUn);
    }

    public ThrottleResource(ResourceId rid) throws Exception {
        super(rid);
        this.policy = new PolicyValues();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT policy, policy_limit, policy_limitu,  policy_interval, policy_intervalu FROM throttle_param WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.policy.type = rs.getString(1);
                this.policy.limit = rs.getInt(2);
                this.policy.limitUn = rs.getString(3);
                this.policy.interval = rs.getInt(4);
                this.policy.intervalUn = rs.getString(5);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO throttle_param (id, policy, policy_limit, policy_limitu, policy_interval, policy_intervalu) VALUES (?, ?, ?, ?, ? ,?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.policy.type);
            ps.setInt(3, this.policy.limit);
            ps.setString(4, this.policy.limitUn);
            ps.setInt(5, this.policy.interval);
            ps.setString(6, this.policy.intervalUn);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("policy".equals(key)) {
            return new TemplateString(this.policy.type);
        }
        if ("limit".equals(key)) {
            return new TemplateString(new Integer(this.policy.limit).toString());
        }
        if ("limitUn".equals(key)) {
            return new TemplateString(this.policy.limitUn);
        }
        if ("interval".equals(key)) {
            return new TemplateString(new Integer(this.policy.interval).toString());
        }
        if ("intervalUn".equals(key)) {
            return new TemplateString(this.policy.intervalUn);
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM throttle_param WHERE id = ?");
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

    public TemplateModel FM_update(String policy_type, String limit, String limitUn, String interval, String intervalUn) throws Exception {
        PolicyValues new_policy = new PolicyValues();
        new_policy.setValues(policy_type, limit, limitUn, interval, intervalUn);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE throttle_param SET  policy=?, policy_limit=?, policy_limitu=?, policy_interval=?, policy_intervalu=?WHERE id = ?");
            ps.setString(1, new_policy.type);
            ps.setInt(2, new_policy.limit);
            ps.setString(3, new_policy.limitUn);
            ps.setInt(4, new_policy.interval);
            ps.setString(5, new_policy.intervalUn);
            ps.setLong(6, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.policy.setValues(new_policy);
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("<IfModule mod_throttle.c>\n");
        buf.append("ThrottlePolicy " + this.policy.type);
        if (!POLICY_NONE.equals(this.policy.type)) {
            if (POLICY_IDLE.equals(this.policy.type)) {
                buf.append(" " + this.policy.interval);
            } else {
                buf.append(" " + this.policy.limit);
                if ("K".equals(this.policy.limitUn) || "M".equals(this.policy.limitUn) || "G".equals(this.policy.limitUn)) {
                    buf.append(this.policy.limitUn);
                }
                buf.append(" " + this.policy.interval);
                if ("s".equals(this.policy.intervalUn) || "m".equals(this.policy.intervalUn) || "h".equals(this.policy.intervalUn) || "d".equals(this.policy.intervalUn) || "w".equals(this.policy.intervalUn)) {
                    buf.append(this.policy.intervalUn);
                }
            }
        }
        buf.append("\n");
        buf.append("<Location /throttle-me>\n");
        buf.append("SetHandler throttle-me\n");
        buf.append("</Location>\n");
        buf.append("</IfModule>\n");
        return buf.toString();
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.throttle.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.throttle.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.throttle.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.throttle.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
