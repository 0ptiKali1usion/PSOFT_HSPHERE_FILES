package psoft.hsphere.resource.moderate_signup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.Category;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.payment.ExternalPayment;
import psoft.hsphere.payment.UnknownPaymentException;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.util.FakeRequest;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/moderate_signup/RequestRecord.class */
public class RequestRecord {
    private static Category log = Category.getInstance(RequestRecord.class.getName());

    /* renamed from: id */
    int f199id;
    long userId;
    long bId;
    long cId;
    int planId;
    long resellerId;
    long signupId = -1;
    Timestamp created;
    Timestamp deleted;
    String username;

    public int getId() {
        return this.f199id;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public long getUserId() {
        return this.userId;
    }

    public User getUser() throws Exception {
        return User.getUser(getUserId());
    }

    public long getBillingId() {
        return this.bId;
    }

    public BillingInfoObject getBillingInfo() throws Exception {
        return new BillingInfoObject(getBillingId());
    }

    public long getContactId() {
        return this.cId;
    }

    public ContactInfoObject getContactInfo() throws Exception {
        return new ContactInfoObject(getContactId());
    }

    public int getPlanId() {
        return this.planId;
    }

    public Date getCreated() {
        return this.created;
    }

    public String getUsername() {
        return this.username;
    }

    public long getSignupId() throws Exception {
        if (this.signupId == -1) {
            this.signupId = SignupManager.getRequestSignupId(getFakeRequest());
        }
        return this.signupId;
    }

    protected String[] getValues(Vector v) {
        String[] values = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            values[i] = (String) v.elementAt(i);
        }
        return values;
    }

    public FakeRequest getFakeRequest() throws Exception {
        return new FakeRequest(getRequest());
    }

    public Hashtable getRequest() throws Exception {
        Hashtable hash = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, value FROM request WHERE id = ? ORDER BY name");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            Vector v = new Vector();
            String oldName = null;
            String name = null;
            while (rs.next()) {
                name = rs.getString(1);
                if (!name.equals(oldName)) {
                    if (oldName != null) {
                        hash.put(oldName, getValues(v));
                    }
                    v = new Vector();
                    oldName = name;
                }
                v.addElement(rs.getString(2));
            }
            if (name != null) {
                hash.put(name, getValues(v));
            }
            hash.put("request_record_id", new String[]{Integer.toString(getId())});
            Session.closeStatement(ps);
            con.close();
            if (hash.size() == 0) {
                throw new HSUserException("signupresource.empty");
            }
            return hash;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void dump() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM request WHERE id = ?");
            ps2.setLong(1, getId());
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("UPDATE request_record SET deleted = ? WHERE request_id = ?");
            ps.setTimestamp(1, TimeUtils.getSQLTimestamp());
            ps.setInt(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isOnlyUserRecord() throws Exception {
        Connection con = Session.getDb();
        boolean ps = null;
        try {
            boolean ps2 = con.prepareStatement("SELECT * FROM request_record WHERE deleted IS NULL AND user_id = ?");
            ps2.setLong(1, getUserId());
            ResultSet rs = ps2.executeQuery();
            return !rs.next();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public ExternalPayment getPayment() throws Exception {
        try {
            return ExternalPayment.getPaymentBySignup(getId());
        } catch (UnknownPaymentException e) {
            return null;
        }
    }

    public RequestRecord(int rId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT user_id,bid,cid,plan_id,created,username,deleted FROM request_record, users WHERE request_id = ? AND users.id = user_id");
            ps.setInt(1, rId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.f199id = rId;
                this.userId = rs.getLong(1);
                this.bId = rs.getLong(2);
                this.cId = rs.getLong(3);
                this.planId = rs.getInt(4);
                this.created = rs.getTimestamp(5);
                this.username = rs.getString(6);
                this.deleted = rs.getTimestamp(7);
                return;
            }
            throw new HSUserException("No such record");
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public boolean equals(Object o) {
        return getId() == ((RequestRecord) o).getId();
    }

    public boolean isDeleted() {
        return this.deleted != null;
    }
}
