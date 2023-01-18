package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.resource.admin.AllocatedServerResource;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/Reseller.class */
public class Reseller implements Comparable {
    protected static Hashtable cache = new Hashtable();

    /* renamed from: id */
    protected long f40id;
    protected long adminId;
    protected int planId;
    protected int resellerPlanId;
    protected String URL;
    protected String protocol;
    protected String port;
    protected String user;
    protected Account account;
    protected Date suspended;
    protected long resourceId;
    protected AllocatedPServer aps;
    protected boolean apsNeedsToBeChecked;
    protected ResellerTypeCounterHolder typeCounter;
    protected static final String defaultPort;
    protected static final String defaultProtocol;
    protected static Hashtable priceHolders;
    protected static Hashtable globalValueHolders;
    protected Hashtable taxes;
    public static final String SERVER_GROUP_LIMITED = "LIMITED";

    static {
        priceHolders = null;
        globalValueHolders = null;
        priceHolders = new Hashtable();
        globalValueHolders = new Hashtable();
        loadAllResellers();
        String tmpDefaultPort = Session.getPropertyString("DEFAULT_CP_PORT");
        defaultPort = (tmpDefaultPort == null || "".equals(tmpDefaultPort)) ? "8080" : tmpDefaultPort;
        String tmpDefaultProtocol = Session.getPropertyString("DEFAULT_RESELLER_CP_PROTOCOL");
        defaultProtocol = (tmpDefaultProtocol == null || "".equals(tmpDefaultProtocol)) ? "http://" : tmpDefaultProtocol;
    }

    protected Reseller(long id, String URL, String protocol, String port, int planId, long adminId, int resellerPlanId, Date suspended, long resourceId) throws Exception {
        this.adminId = 0L;
        this.aps = null;
        this.apsNeedsToBeChecked = true;
        this.typeCounter = null;
        this.taxes = null;
        this.f40id = id;
        this.adminId = adminId;
        this.planId = planId;
        this.resellerPlanId = resellerPlanId;
        this.URL = URL;
        this.protocol = protocol;
        this.port = port;
        this.suspended = suspended;
        this.resourceId = resourceId;
        cache.put(new Long(id), this);
    }

    protected Reseller(long id, String URL, String protocol, String port, int resellerPlanId, long resourceId) throws Exception {
        this(id, URL, protocol, port, 0, 0L, resellerPlanId, null, resourceId);
    }

    public static Reseller getReseller(long id) throws UnknownResellerException {
        try {
            return (Reseller) cache.get(new Long(id));
        } catch (Exception ex) {
            throw new UnknownResellerException(ex);
        }
    }

    protected static void loadAllResellers() {
        try {
            new Reseller(1L, Session.getPropertyString("CP_HOST"), Session.getPropertyString("CP_PROTOCOL"), Session.getPropertyString("CP_PORT"), 3, 0L);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, cp_host, cp_protocol, cp_port, admin_plan, admin_id, reseller_plan_id, suspended, res_id FROM resellers");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                new Reseller(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getLong(6), rs.getInt(7), rs.getTimestamp(8) == null ? null : new Date(rs.getTimestamp(8).getTime()), rs.getLong(9));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Exception se) {
            Session.getLog().error("load reseller list", se);
        }
    }

    public static synchronized Collection<Reseller> getResellerList() {
        Object[] arr = cache.values().toArray();
        Arrays.sort(arr);
        List<Reseller> list = new ArrayList<>();
        for (Object o : arr) {
            list.add((Reseller) o);
        }
        return list;
    }

    public static Reseller getReseller(String URL) throws Exception {
        String tURL;
        if (URL.indexOf(":") < 0) {
            tURL = URL;
        } else {
            tURL = URL.substring(0, URL.indexOf(":"));
        }
        Session.getLog().debug("Reseller URL:" + URL);
        for (Reseller reseller : getResellerList()) {
            if (tURL.toLowerCase().equals(reseller.getURL().toLowerCase()) && !reseller.isSuspended()) {
                return reseller;
            }
        }
        return null;
    }

    public static Reseller getReseller(ResourceId resourceId) throws Exception {
        Session.getLog().debug("Reseller resourceId:" + resourceId);
        if (resourceId == null) {
            return null;
        }
        for (Reseller reseller : getResellerList()) {
            if (reseller.getResourceId() != null && reseller.getResourceId().equals(resourceId)) {
                return reseller;
            }
        }
        return null;
    }

    public static Reseller createReseller(long id, String url, int resellerPlanId, long resourceId) throws Exception {
        if (getReseller(id) != null) {
            throw new HSUserException("reseller.exist");
        }
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO resellers (id, cp_host, admin_plan, admin_id, reseller_plan_id, res_id) VALUES (?, ?, 0, 0, ?, ?)");
            ps.setLong(1, id);
            ps.setString(2, url);
            ps.setInt(3, resellerPlanId);
            ps.setLong(4, resourceId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            Reseller res = new Reseller(id, url, null, null, resellerPlanId, resourceId);
            res.setPrices();
            return res;
        } catch (SQLException se) {
            throw new Exception("Unable to add new reseller:" + se.getMessage());
        }
    }

    public long getId() {
        return this.f40id;
    }

    public int getPlanId() {
        return this.planId;
    }

    public ResourceId getResourceId() {
        if (this.resourceId == 0) {
            return null;
        }
        return new ResourceId(this.resourceId, 3010);
    }

    public int getResellerPlanId() {
        return this.resellerPlanId;
    }

    public ResellerPrices getPrices(String typeName) {
        ResellerPrices prices;
        ResellerPriceHolder prices2 = getPriceHolder();
        if (prices2 != null) {
            synchronized (prices2) {
                prices = prices2.getPrices(typeName);
            }
            return prices;
        }
        return null;
    }

    public ResellerPrices getPrices(int typeId) throws NoSuchTypeException {
        return getPrices(TypeRegistry.getType(typeId));
    }

    public ResellerPriceHolder getPriceHolder() {
        ResellerPriceHolder prices = (ResellerPriceHolder) priceHolders.get(String.valueOf(getResellerPlanId()));
        if (prices == null) {
            try {
                setPrices();
                prices = (ResellerPriceHolder) priceHolders.get(String.valueOf(getResellerPlanId()));
            } catch (Exception ex) {
                Session.getLog().error("Error getting prices reseller plan id " + getResellerPlanId(), ex);
            }
        }
        return prices;
    }

    public static void setToNullPrices(int planId) throws Exception {
        priceHolders.remove(String.valueOf(planId));
        Session.getLog().debug("The prices for Plan ID:" + planId + " set to null");
    }

    public void setPrices() throws Exception {
        long oldResellerId = 0;
        try {
            oldResellerId = Session.getResellerId();
        } catch (UnknownResellerException e) {
        }
        try {
            ResellerPriceHolder prices = (ResellerPriceHolder) priceHolders.get(String.valueOf(getResellerPlanId()));
            Session.setResellerId(1L);
            Session.getLog().debug("Resellers Plan:" + getResellerPlanId());
            if (prices != null) {
                prices.reload();
            } else {
                priceHolders.put(String.valueOf(getResellerPlanId()), new ResellerPriceHolder(getResellerPlanId()));
            }
            getTypeCounter().reload();
            if (oldResellerId != 0) {
                Session.setResellerId(oldResellerId);
            }
        } catch (Throwable th) {
            if (oldResellerId != 0) {
                Session.setResellerId(oldResellerId);
            }
            throw th;
        }
    }

    public void setPlanId(int planId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET admin_plan = ? WHERE id = ?");
            ps.setInt(1, planId);
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.planId = planId;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setResourceId(long resourceId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET res_id= ? WHERE id = ?");
            ps.setLong(1, resourceId);
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.resourceId = resourceId;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setResellerPlanId(int resellerPlanId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET reseller_plan_id = ? WHERE id = ?");
            ps.setInt(1, resellerPlanId);
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.resellerPlanId = resellerPlanId;
            this.typeCounter = null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setURL(String URL, String protocol, String port) throws Exception {
        Session.getLog().debug("in Reseller.setURL()");
        Reseller oldRes = getReseller(URL);
        if (oldRes != null && oldRes.getId() != getId()) {
            throw new HSUserException("reseller.url");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE resellers SET cp_host = ?, cp_protocol = ?, cp_port = ? WHERE id = ?");
                ps.setString(1, URL.toLowerCase());
                ps.setString(2, protocol);
                ps.setString(3, port);
                ps.setLong(4, getId());
                Session.getLog().debug("URL = " + URL.toLowerCase());
                Session.getLog().debug("protocol = " + protocol);
                Session.getLog().debug("port = " + port);
                Session.getLog().debug("id = " + String.valueOf(getId()));
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().error("Some errors", e);
                Session.closeStatement(ps);
                con.close();
            }
            this.URL = URL.toLowerCase();
            this.protocol = protocol;
            this.port = port;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setAdmin(long adminId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET admin_id = ? WHERE id = ?");
            ps.setLong(1, adminId);
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.adminId = adminId;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public long getAdmin() {
        return this.adminId;
    }

    public String getURL() {
        return this.URL;
    }

    public String getProtocol() {
        return this.protocol == null ? defaultProtocol : this.protocol;
    }

    public String getPort() {
        return this.port == null ? defaultPort : this.port;
    }

    public boolean isSuspended() {
        return this.suspended != null;
    }

    public void suspend() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET suspended = ?WHERE id = ?");
            ps.setTimestamp(1, TimeUtils.getSQLTimestamp());
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.suspended = TimeUtils.getDate();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void resume() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE resellers SET suspended = null WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.suspended = null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getUser() throws Exception {
        if (this.user != null) {
            return this.user;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT username FROM users WHERE id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.user = rs.getString(1);
            }
            Session.closeStatement(ps);
            con.close();
            return this.user;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Account getAccount() throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            try {
                if (getId() == 1) {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return null;
                }
                if (getResourceId() != null) {
                    this.account = getResourceId().getAccount();
                }
                User user = User.getUser(getUser());
                if (user == null) {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    return null;
                }
                Session.setUser(user);
                Iterator i = user.getAccountIds().iterator();
                while (i.hasNext()) {
                    Account a = user.getAccount((ResourceId) i.next());
                    Session.setAccount(a);
                    ResourceId resRsourceId = a.FM_getChild(FMACLManager.RESELLER);
                    if (resRsourceId != null) {
                        setResourceId(resRsourceId.getId());
                        if (a.isSuspended() && !isSuspended()) {
                            suspend();
                        }
                        this.account = a;
                        Account account = this.account;
                        Session.setUser(oldUser);
                        Session.setAccount(oldAccount);
                        return account;
                    }
                }
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                return null;
            } catch (Exception e) {
                Session.getLog().error("Error found Account#:" + getId());
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                return null;
            }
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM resellers WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            cache.remove(new Long(getId()));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ResellerTypeCounterHolder getTypeCounter() {
        if (this.typeCounter == null) {
            try {
                this.typeCounter = new ResellerTypeCounterHolder(getPriceHolder(), this.f40id);
            } catch (Exception e) {
                Session.getLog().error("Error loading reseller TypeCounter", e);
            }
        }
        return this.typeCounter;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object r) {
        Reseller res = (Reseller) r;
        Long id = new Long(getId());
        return id.compareTo(new Long(res.getId()));
    }

    public Tax getTax(long id) throws Exception {
        return (Tax) this.taxes.get(new Long(id));
    }

    public Hashtable getTaxes() throws Exception {
        return getTaxes(false);
    }

    public Hashtable getTaxes(boolean force) throws Exception {
        if (this.taxes != null && !force) {
            return this.taxes;
        }
        if (this.taxes != null) {
            this.taxes.clear();
        } else {
            this.taxes = new Hashtable();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, tax_percent, description, deleted, flag, country, state FROM taxes WHERE reseller_id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.taxes.put(rs.getString(1), new Tax(rs.getLong(1), rs.getDouble(2), rs.getString(3), rs.getInt(4) == 1, rs.getInt(5), rs.getString(6), rs.getString(7)));
            }
            Session.closeStatement(ps);
            con.close();
            return this.taxes;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Tax addTax(double percent, String description, String country, String state, int outsideCountry, int outsideState) throws Exception {
        Tax newTax = Tax.create(percent, description, country, state, outsideCountry, outsideState);
        getTaxes().put(String.valueOf(newTax.getId()), newTax);
        return newTax;
    }

    public void delTax(long taxId) throws Exception {
        Tax delTax = (Tax) getTaxes().get(String.valueOf(taxId));
        if (delTax.delete() == null) {
            getTaxes().remove(String.valueOf(taxId));
        }
    }

    public ResellerGlobalValueHolder getGlobalValueHolder() throws Exception {
        return getGlobalValueHolder(getResellerPlanId());
    }

    public static ResellerGlobalValueHolder getGlobalValueHolder(int planId) throws Exception {
        String strPlanId = String.valueOf(planId);
        ResellerGlobalValueHolder values = (ResellerGlobalValueHolder) globalValueHolders.get(strPlanId);
        if (values == null) {
            long oldResellerId = Session.getResellerId();
            if (oldResellerId != 1) {
                try {
                    Session.setResellerId(1L);
                } catch (Throwable th) {
                    if (oldResellerId != 1) {
                        Session.setResellerId(oldResellerId);
                    }
                    throw th;
                }
            }
            values = new ResellerGlobalValueHolder(planId);
            globalValueHolders.put(strPlanId, values);
            if (oldResellerId != 1) {
                Session.setResellerId(oldResellerId);
            }
        }
        return values;
    }

    public void updateGlobalValues() throws Exception {
        getGlobalValueHolder().update();
    }

    public static void updateGlobalValues(int planId) throws Exception {
        getGlobalValueHolder(planId).update();
    }

    public Hashtable checkAllowToDelete() throws Exception {
        Hashtable result = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM users u, user_account ua, accounts a WHERE u.id = ua.user_id AND a.id = ua.account_id AND a.reseller_id = ? AND u.id <> ? AND a.deleted IS NULL");
            ps2.setLong(1, getId());
            ps2.setLong(2, getAdmin());
            ResultSet rs = ps2.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                result.put("LIVE_USER", Localizer.translateMessage("reseller.check_avail.live_user", new Object[]{new Integer(rs.getInt(1))}));
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT count(*) FROM users u, f_user_account ua, accounts a WHERE u.id = ua.user_id AND a.id = ua.account_id AND a.reseller_id = ?");
            ps3.setLong(1, getId());
            ResultSet rs2 = ps3.executeQuery();
            if (rs2.next() && rs2.getInt(1) > 0) {
                result.put("FAKE_USER", Localizer.translateMessage("reseller.check_avail.fake_user", new Object[]{new Integer(rs2.getInt(1))}));
            }
            ps3.close();
            ps = con.prepareStatement("SELECT count(*) FROM users u, request_record r WHERE u.id = r.user_id AND u.reseller_id = ? AND r.deleted IS NULL");
            ps.setLong(1, getId());
            ResultSet rs3 = ps.executeQuery();
            if (rs3.next() && rs3.getInt(1) > 0) {
                result.put("MODERATION_USER", Localizer.translateMessage("reseller.check_avail.mod_user", new Object[]{new Integer(rs3.getInt(1))}));
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static String getDefaultPort() {
        return defaultPort;
    }

    public static String getDefaultProtocol() {
        return defaultProtocol;
    }

    public void loadTypeCounter() throws Exception {
        this.typeCounter = null;
    }

    public AllocatedPServer getAllocatedPServer() throws Exception {
        if (getId() == 1) {
            return null;
        }
        if (this.apsNeedsToBeChecked) {
            Session.save();
            try {
                Session.setUser(getAccount().getUser());
                Session.setAccount(getAccount());
                ResourceId allocatedId = getAccount().FM_getChild("allocated_server");
                if (allocatedId != null) {
                    try {
                        AllocatedServerResource asr = (AllocatedServerResource) allocatedId.get();
                        this.aps = asr.getAllocatedServer();
                        this.apsNeedsToBeChecked = false;
                    } catch (Exception ex) {
                        Session.getLog().debug("Error getting AllocatedServerResource: ", ex);
                    }
                } else {
                    this.apsNeedsToBeChecked = false;
                }
                Session.restore();
            } catch (Throwable th) {
                Session.restore();
                throw th;
            }
        }
        return this.aps;
    }

    public static long getResellerIdByAccId(long accId) throws Exception {
        long resId = 0;
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT r.id FROM resellers r JOIN parent_child pc ON r.res_id = pc.child_id WHERE pc.child_type = ? AND pc.account_id = ?");
            ps.setLong(1, 3010L);
            ps.setLong(2, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                resId = rs.getLong(1);
            }
            Session.closeStatement(ps);
            con.close();
            return resId;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
