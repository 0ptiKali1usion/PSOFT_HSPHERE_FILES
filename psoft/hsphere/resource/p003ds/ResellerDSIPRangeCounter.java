package psoft.hsphere.resource.p003ds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Hashtable;
import java.util.TreeMap;
import psoft.hsphere.InitToken;
import psoft.hsphere.Session;
import psoft.hsphere.ipmanagement.IPRange;
import psoft.hsphere.ipmanagement.IPSubnet;
import psoft.hsphere.ipmanagement.IPSubnetManager;

/* renamed from: psoft.hsphere.resource.ds.ResellerDSIPRangeCounter */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/ResellerDSIPRangeCounter.class */
public class ResellerDSIPRangeCounter {
    private static Hashtable resellerCounterMap = new Hashtable();
    private double freeUnits;
    private TreeMap resources = new TreeMap();
    private long limitRid = -1;

    public ResellerDSIPRangeCounter(long resellerId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT ipr.id, ipr.parent_id, pc.child_id FROM ip_ranges ipr, ip_subnets ips, parent_child pc, accounts a WHERE a.reseller_id = ? AND a.id = pc.account_id AND pc.child_type = ? AND pc.child_id = ipr.rid AND ipr.parent_id = ips.id AND ips.reseller_id = ? ORDER BY pc.child_id");
            ps.setLong(1, resellerId);
            ps.setInt(2, 7108);
            ps.setLong(3, 1L);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                IPSubnet ips = IPSubnetManager.getInstance().getSubnet(rs.getLong("parent_id"));
                IPRange ipr = ips.getRange(rs.getLong("id"));
                if (ipr.getParent().getResellerId() == 1) {
                    long ipnum = ipr.getNumberOfIPs();
                    long rid = rs.getLong("child_id");
                    Double amount = (Double) this.resources.get(new Long(rid));
                    if (amount != null) {
                        this.resources.put(new Long(rid), new Double(amount.doubleValue() + ipnum));
                    } else {
                        this.resources.put(new Long(rid), new Double(ipnum));
                    }
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

    private void findLimitResource() {
        double total = 0.0d;
        this.limitRid = -1L;
        for (Long rid : this.resources.keySet()) {
            Double amount = (Double) this.resources.get(rid);
            total += amount.doubleValue();
            if (this.limitRid == -1 && total > this.freeUnits) {
                this.limitRid = rid.longValue();
            }
        }
    }

    public long getLimitRid() {
        return this.limitRid;
    }

    public void addResource(long rid, double amount) {
        synchronized (this.resources) {
            Double currAmount = (Double) this.resources.get(new Long(rid));
            if (currAmount != null) {
                this.resources.put(new Long(rid), new Double(currAmount.doubleValue() + amount));
            }
            findLimitResource();
        }
    }

    public void removeResource(long rid) {
        synchronized (this.resources) {
            this.resources.remove(new Long(rid));
            findLimitResource();
        }
    }

    public static ResellerDSIPRangeCounter getResellerCounter(long resellerId) {
        ResellerDSIPRangeCounter resellerDSIPRangeCounter;
        synchronized (resellerCounterMap) {
            ResellerDSIPRangeCounter rc = (ResellerDSIPRangeCounter) resellerCounterMap.get(new Long(resellerId));
            if (rc == null) {
                try {
                    rc = new ResellerDSIPRangeCounter(resellerId);
                } catch (Exception ex) {
                    Session.getLog().debug("Unable to load reseller dedicated server IP ranges counter for reseller with id " + resellerId, ex);
                }
            }
            resellerDSIPRangeCounter = rc;
        }
        return resellerDSIPRangeCounter;
    }

    public double getResellerRecurrentMultiplier(InitToken token) throws Exception {
        double delta = 0.0d;
        double totalAmount = getTotalAmount();
        if (token.getRes() != null) {
            Collection<IPRange> currSet = token.getRes().getCurrentInitValues();
            Collection<IPRange> newSet = token.getValues();
            if (newSet.size() > currSet.size()) {
                for (IPRange ipr : newSet) {
                    if (!currSet.contains(ipr)) {
                        delta += ipr.getNumberOfIPs();
                    }
                }
            } else {
                for (IPRange ipr2 : currSet) {
                    if (!newSet.contains(ipr2)) {
                        delta -= ipr2.getNumberOfIPs();
                    }
                }
            }
        } else {
            delta = token.getAmount();
        }
        if (totalAmount >= this.freeUnits) {
            return delta;
        }
        if (totalAmount + delta >= this.freeUnits) {
            return (totalAmount + delta) - this.freeUnits;
        }
        return 0.0d;
    }

    public double getResellerRecurrentMultiplier(long rId) {
        if (getLimitRid() > 0) {
            if (rId == getLimitRid()) {
                double mult = 0.0d;
                synchronized (this.resources) {
                    for (Long key : this.resources.keySet()) {
                        double amount = ((Double) this.resources.get(key)).doubleValue();
                        if (key.longValue() <= rId) {
                            mult += amount;
                        }
                    }
                }
                return mult - getFreeUnits();
            } else if (rId > getLimitRid() && this.resources.keySet().contains(new Long(rId))) {
                return ((Double) this.resources.get(new Long(rId))).doubleValue();
            } else {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    public double getFreeUnits() {
        return this.freeUnits;
    }

    public void setFreeUnits(double freeUnits) {
        this.freeUnits = freeUnits;
        findLimitResource();
    }

    public double getTotalAmount() {
        double d;
        double totalAmount = 0.0d;
        synchronized (this.resources) {
            for (Double d2 : this.resources.values()) {
                totalAmount += d2.doubleValue();
            }
            d = totalAmount;
        }
        return d;
    }
}
