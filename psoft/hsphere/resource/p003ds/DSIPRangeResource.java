package psoft.hsphere.resource.p003ds;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResellerPrices;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.ipmanagement.IPRange;
import psoft.hsphere.ipmanagement.IPSubnet;
import psoft.hsphere.ipmanagement.IPSubnetManager;
import psoft.util.freemarker.TemplateList;

/* renamed from: psoft.hsphere.resource.ds.DSIPRangeResource */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/DSIPRangeResource.class */
public class DSIPRangeResource extends Resource {
    private static Category log = Category.getInstance(DSIPRangeResource.class.getName());
    private Hashtable ranges;

    public DSIPRangeResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.ranges = new Hashtable();
        Iterator i = initValues.iterator();
        while (i.hasNext()) {
            IPRange ipr = (IPRange) i.next();
            addRange(ipr);
        }
    }

    public DSIPRangeResource(ResourceId id) throws Exception {
        super(id);
        this.ranges = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id, parent_id FROM ip_ranges WHERE rid = ?");
        ps.setLong(1, getId().getId());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            IPSubnet ips = IPSubnetManager.getInstance().getSubnet(rs.getLong("parent_id"));
            IPRange ipr = ips.getRange(rs.getLong("id"));
            addRange(ipr);
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE ip_ranges SET r_type = ?, rid = ? WHERE id = ?");
            for (IPRange ipr : this.ranges.values()) {
                synchronized (ipr) {
                    ps.setInt(1, 3);
                    ps.setLong(2, getId().getId());
                    ps.setLong(3, ipr.getId());
                    ps.executeUpdate();
                    ipr.setType(3);
                    ipr.setRid(getId().getId());
                    IPSubnetManager.getInstance().unlockRange(ipr.getId());
                }
            }
            Session.closeStatement(ps);
            con.close();
            ResellerDSIPRangeCounter rc = ResellerDSIPRangeCounter.getResellerCounter(Session.getResellerId());
            rc.addResource(getId().getId(), getAmount());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void addRange(IPRange ipr) {
        synchronized (this.ranges) {
            this.ranges.put(new Long(ipr.getId()), ipr);
        }
    }

    private void delRange(long rangeId) {
        synchronized (this.ranges) {
            IPRange ipr = (IPRange) this.ranges.get(new Long(rangeId));
            if (ipr != null) {
                ipr.setRid(-1L);
                this.ranges.remove(new Long(rangeId));
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        if (this.initialized) {
            lockIPRanges();
            try {
                ps = con.prepareStatement("UPDATE ip_ranges SET r_type = ?, rid = ? WHERE id = ?");
                ps.setInt(1, 1);
                ps.setNull(2, 4);
                for (IPRange ipr : this.ranges.values()) {
                    ps.setLong(3, ipr.getId());
                    ps.executeUpdate();
                }
                for (IPRange ipr2 : this.ranges.values()) {
                    ipr2.setType(1);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        unlockIPRanges();
        ResellerDSIPRangeCounter rc = ResellerDSIPRangeCounter.getResellerCounter(Session.getResellerId());
        rc.removeResource(getId().getId());
    }

    private void unlockIPRanges() {
        for (Long l : this.ranges.keySet()) {
            IPSubnetManager.getInstance().unlockRange(l.longValue());
        }
    }

    private void lockIPRanges() throws Exception {
        for (Long l : this.ranges.keySet()) {
            IPSubnetManager.getInstance().lockRange(l.longValue());
        }
    }

    @Override // psoft.hsphere.Resource
    public synchronized double changeResource(Collection values) throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        double oldAmount = getAmount();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE ip_ranges SET rid = ?, r_type=? WHERE rid = ?");
            ps.setNull(1, 4);
            ps.setInt(2, 1);
            ps.setLong(3, getId().getId());
            ps1 = con.prepareStatement("UPDATE ip_ranges SET rid = ?, r_type=? WHERE id = ?");
            ps1.setLong(1, getId().getId());
            ps1.setLong(2, 3L);
            ps.executeUpdate();
            synchronized (this.ranges) {
                for (IPRange ipr : this.ranges.values()) {
                    ipr.setType(1);
                    ipr.setRid(-1L);
                }
                this.ranges = new Hashtable();
                Iterator i = values.iterator();
                while (i.hasNext()) {
                    IPRange ipr2 = (IPRange) i.next();
                    ps1.setLong(3, ipr2.getId());
                    ps1.executeUpdate();
                    ipr2.setType(3);
                    addRange(ipr2);
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            ResellerDSIPRangeCounter rc = ResellerDSIPRangeCounter.getResellerCounter(Session.getResellerId());
            rc.addResource(getId().getId(), getAmount());
            return oldAmount;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        double amount;
        synchronized (this.ranges) {
            amount = getAmount(this.ranges.values());
        }
        return amount;
    }

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        return new ArrayList(this.ranges.values());
    }

    public static double getRecurrentMultiplier(InitToken token) {
        double freeUnits = 0.0d;
        double currAmount = 0.0d;
        try {
            freeUnits = token.getFreeUnits();
        } catch (Exception ex) {
            log.debug("Unable to get free units for" + token.getRes().getId(), ex);
        }
        double delta = 0.0d;
        if (token.getRes() != null) {
            Collection<IPRange> currValues = token.getRes().getCurrentInitValues();
            currAmount = getAmount(currValues);
            if (token.getValues().size() > currValues.size()) {
                for (IPRange ipr : token.getValues()) {
                    if (!currValues.contains(ipr)) {
                        delta += ipr.getNumberOfIPs();
                    }
                }
            } else {
                for (IPRange ipr2 : currValues) {
                    if (!token.getValues().contains(ipr2)) {
                        delta -= ipr2.getNumberOfIPs();
                    }
                }
            }
        }
        double res = (currAmount - freeUnits) + delta;
        if (res >= 0.0d) {
            return res;
        }
        return 0.0d;
    }

    public static double getAmount(InitToken token) {
        return getAmount(token.getValues());
    }

    private static double getAmount(Collection rangeSet) {
        double amount = 0.0d;
        Iterator i = rangeSet.iterator();
        while (i.hasNext()) {
            IPRange ipr = (IPRange) i.next();
            amount += ipr.getNumberOfIPs();
        }
        return amount;
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return getChangeDescription(token);
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return getChangeDescription(token);
    }

    public static String getChangeDescription(InitToken token) throws Exception {
        return getChangeDescription(token.getRes(), token.getFreeUnits(), token.getValues(), token.getPeriodInWords());
    }

    public static String getChangeDescription(Resource _r, double freeUnits, Collection newSet, String period) throws Exception {
        Collection<IPRange> currSet;
        if (_r != null) {
            currSet = _r.getCurrentInitValues();
        } else {
            currSet = new ArrayList();
        }
        StringBuffer affectedRanges = new StringBuffer();
        if (newSet.size() > currSet.size()) {
            Iterator i = newSet.iterator();
            while (i.hasNext()) {
                IPRange ipr = (IPRange) i.next();
                if (!currSet.contains(ipr)) {
                    affectedRanges.append(ipr.toString()).append(" ");
                }
            }
            return Localizer.translateMessage("bill.ds.ip_pool.recurrent", new String[]{period, affectedRanges.toString(), Double.toString(freeUnits)});
        } else if (currSet.size() > newSet.size()) {
            for (IPRange ipr2 : currSet) {
                if (!newSet.contains(ipr2)) {
                    affectedRanges.append(ipr2.toString()).append(" ");
                }
            }
            return Localizer.translateMessage("bill.ds.ip_pool.refund", new String[]{affectedRanges.toString()});
        } else {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta, Collection values) throws Exception {
        return getChangeDescription(this, getFreeNumber(), values, getAccount().getPeriodInWords());
    }

    public Collection getRanges() {
        return this.ranges.values();
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("ranges".equals(key)) {
            return new TemplateList(getRanges());
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public double getResellerRecurrentMultiplier() {
        try {
            Reseller resell = Session.getReseller();
            ResellerPrices prices = resell.getPrices(getId().getType());
            double freeUnits = prices.getFreeUnits();
            ResellerDSIPRangeCounter rc = null;
            try {
                rc = ResellerDSIPRangeCounter.getResellerCounter(Session.getResellerId());
            } catch (Exception ex) {
                log.error("Unable to load reseller IP range counter for reseller with id " + getId(), ex);
            }
            if (rc != null) {
                rc.setFreeUnits(freeUnits);
                double mult = rc.getResellerRecurrentMultiplier(getId().getId());
                return mult;
            }
            return 0.0d;
        } catch (Exception ex2) {
            log.error("Unable to get reseller free units for ds_ip_range resource", ex2);
            return 0.0d;
        }
    }

    public static double getResellerRecurrentMultiplier(InitToken token) throws Exception {
        Reseller resell = Session.getReseller();
        ResellerPrices prices = resell.getPrices(token.getResourceType().getType());
        double freeUnits = prices.getFreeUnits();
        ResellerDSIPRangeCounter rc = ResellerDSIPRangeCounter.getResellerCounter(Session.getResellerId());
        rc.setFreeUnits(freeUnits);
        return rc.getResellerRecurrentMultiplier(token);
    }

    public static String getResellerRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.reseller.recurrent", new Object[]{token.getPeriodInWords(), TypeRegistry.getDescription(token.getResourceType().getId()), f42df.format(begin), f42df.format(end)});
    }

    public static String getResellerRecurrentRefundDescription(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.reseller.refund", new Object[]{TypeRegistry.getDescription(token.getResourceType().getId()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{getPeriodInWords(), getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        synchronized (this.ranges) {
            if (this.ranges.values().size() == 1) {
                IPRange _ipr = (IPRange) this.ranges.values().iterator().next();
                return Localizer.translateMessage("ds.ip_pool.bill.description1", new Object[]{_ipr.toString()});
            }
            return Localizer.translateMessage("ds.ip_pool.bill.description2", new Object[]{Integer.toString(this.ranges.values().size()), Double.toString(getAmount(this.ranges.values()))});
        }
    }
}
