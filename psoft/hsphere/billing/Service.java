package psoft.hsphere.billing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/billing/Service.class */
public class Service {
    public static final int FIRST_DAY_PRO_RATED = 1;
    public static final int FIRST_DAY = 2;
    public static final int ANY_TIME = 3;
    protected long accountId;
    protected boolean isGotAllRecords = false;
    protected Hashtable serviceEntries = new Hashtable();

    public Service(long accountId) {
        this.accountId = accountId;
    }

    public List getServices() throws Exception {
        if (!this.isGotAllRecords) {
            reloadServices();
        }
        return new ArrayList(this.serviceEntries.values());
    }

    public ServiceEntry getService(long serviceId) throws Exception {
        ServiceEntry entry = (ServiceEntry) this.serviceEntries.get(new Long(serviceId));
        Session.getLog().debug("GETTING SERVICE:" + serviceId);
        if (entry != null) {
            return entry;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, created, billing_type,frequency, duration, duration_count,last_bill, next_bill, description, note,adm_note, amount, deleted FROM service WHERE id = ? and account_id = ?");
            ps.setLong(1, serviceId);
            ps.setLong(2, this.accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session.getLog().debug("GETTING SERVICE:" + rs.getLong("id") + " DESC:" + rs.getString("description"));
                entry = new ServiceEntry(rs.getLong("id"), this.accountId, rs.getDate("created"), rs.getInt("billing_type"), rs.getInt("frequency"), rs.getInt("duration"), rs.getInt("duration_count"), rs.getDate("last_bill"), rs.getDate("next_bill"), rs.getString("description"), rs.getString("note"), rs.getString("adm_note"), rs.getDouble("amount"), rs.getDate("deleted"));
                this.serviceEntries.put(new Long(serviceId), entry);
            }
            Session.closeStatement(ps);
            con.close();
            return entry;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void reloadServices() throws Exception {
        Session.getLog().debug("RELOAD SERVICE");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM service WHERE account_id = ? ORDER BY id");
            ps.setLong(1, this.accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                getService(rs.getLong(1));
            }
            this.isGotAllRecords = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ServiceEntry addEntry(int billingType, int frequency, int duration, double amount, String description, String note, String adminNote) throws Exception {
        ServiceEntry entry = ServiceEntry.create(this.accountId, billingType, frequency, duration, amount, description, note, adminNote);
        this.serviceEntries.put(new Long(entry.getId()), entry);
        return entry;
    }

    public void deleteService(long id) throws Exception {
        ServiceEntry entry = getService(id);
        Session.getLog().debug("DELETE ENTRY ID:" + entry.getId());
        entry.delete();
    }

    public void deleteServices() throws Exception {
        Session.getLog().debug("DELETING ALL SERVICES");
        for (ServiceEntry entry : getServices()) {
            entry.remove();
        }
        this.serviceEntries.clear();
    }

    public static void bill() throws Exception {
        Session.getLog().debug("TRY TO BILL SERVICES");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT s.id, s.account_id FROM service s, accounts a WHERE next_bill <= ? AND s.deleted IS NULL  AND a.deleted IS NULL AND a.suspended IS NULL  AND s.account_id = a.id");
            ps.setDate(1, TimeUtils.getSQLDate());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    ServiceEntry entry = new Service(rs.getLong(2)).getService(rs.getLong(1));
                    entry.bill();
                } catch (Exception ex) {
                    Session.getLog().debug("Unable to bill service#" + rs.getLong(1) + " for account#" + rs.getLong(2), ex);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
