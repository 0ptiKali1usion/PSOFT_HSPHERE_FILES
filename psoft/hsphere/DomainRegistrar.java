package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import psoft.hsphere.manager.Manager;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/DomainRegistrar.class */
public class DomainRegistrar {
    public static Collection getActiveTLDs() throws Exception {
        return getManager().getActiveKeys();
    }

    public static synchronized Collection getActivePricedTLDs(ResourceType rt) throws Exception {
        Collection result = new HashSet();
        Collection<String> col = getManager().getActiveKeys();
        for (String tld : col) {
            if (getTLDPrices(tld, rt).size() > 0) {
                result.add(tld);
            }
        }
        return result;
    }

    public static Collection getActiveDomainTransferTLDs(ResourceType rt) throws Exception {
        List l = new ArrayList();
        for (String tld : getManager().getActiveKeys()) {
            if (getTransferPrice(tld, rt) != null) {
                l.add(tld);
            }
        }
        return l;
    }

    public static Manager getManager() throws Exception {
        Manager manager = Manager.getManager("REGISTRAR");
        if (manager == null) {
            throw new Exception("Can't find REGISTRAR Manager");
        }
        return manager;
    }

    public static boolean isTransferable(String name) throws RegistrarException, Exception {
        String tld = getTLD(name);
        Registrar reg = (Registrar) getManager().getEntity(tld);
        reg.checkLogin();
        try {
            return reg.isTransferable(getName(name), tld);
        } catch (Exception ex) {
            if (ex instanceof RegistrarException) {
                throw ((RegistrarException) ex);
            }
            throw new RegistrarException(RegistrarException.OTHER_ERROR, ex.getMessage());
        }
    }

    public static boolean lookup(String name) throws RegistrarException, Exception {
        String tld = getTLD(name);
        Registrar reg = (Registrar) getManager().getEntity(tld);
        reg.checkLogin();
        try {
            return reg.lookup(getName(name), tld);
        } catch (Exception ex) {
            throw new RegistrarException(RegistrarException.OTHER_ERROR, ex.getMessage());
        }
    }

    public static String getTLD(String fqdn) {
        int ind = fqdn.indexOf(46);
        return ind == -1 ? "" : fqdn.substring(ind + 1);
    }

    public static String getName(String fqdn) {
        int ind = fqdn.indexOf(46);
        return ind == -1 ? "" : fqdn.substring(0, ind);
    }

    public static double getDomainPrice(String tld, int duration) throws Exception {
        return getDomainPrice(tld, duration, Session.getResellerId());
    }

    public static double getDomainTransferPrice(String tld, ResourceType rt) throws Exception {
        String value = (String) rt.getValues().get("_TRANSFER_" + tld);
        return value != null ? Double.parseDouble(value) : getDomainTransferPrice(tld, Session.getResellerId());
    }

    public static double getDomainTransferPrice(String tld) throws Exception {
        return getDomainTransferPrice(tld, Session.getResellerId());
    }

    public static double getDomainTransferPrice(String tld, long resellerId) throws Exception {
        return getDomainPrice(tld, -1, resellerId);
    }

    public static double getDomainPrice(String tld, int duration, long resellerId) throws Exception {
        String val = getDomainPriceAsStr(tld, duration, resellerId);
        if (val == null) {
            return Double.NaN;
        }
        return USFormat.parseDouble(val);
    }

    public static String getDomainPriceAsStr(String tld, int duration, long resellerId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT price FROM tld_prices WHERE reseller_id = ? AND duration = ? AND tld = ?");
            ps.setLong(1, resellerId);
            ps.setInt(2, duration);
            ps.setString(3, tld);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized String getTransferPrice(String tld, ResourceType rt) throws Exception {
        return getTransferPrice(tld, rt, Session.getResellerId());
    }

    public static synchronized String getTransferPrice(String tld, ResourceType rt, long resellerId) throws Exception {
        String value = (String) rt.getValues().get("_TRANSFER_" + tld);
        return value != null ? value : getDomainPriceAsStr(tld, -1, resellerId);
    }

    public static synchronized Map getTLDPrices(String tld, ResourceType rt) throws Exception {
        return getTLDPrices(tld, rt, Session.getResellerId());
    }

    public static Map getTLDPrices(String tld) throws SQLException, UnknownResellerException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Map prices = new HashMap();
        try {
            ps = con.prepareStatement("SELECT duration, price FROM tld_prices WHERE reseller_id = ? AND tld = ? AND duration > 0");
            ps.setLong(1, Session.getResellerId());
            ps.setString(2, tld);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                prices.put(rs.getString(1), rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
            return prices;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized Map getTLDPrices(String tld, ResourceType rt, long resellerId) throws Exception {
        TreeMap prices = new TreeMap(new Comparator() { // from class: psoft.hsphere.DomainRegistrar.1
            @Override // java.util.Comparator
            public int compare(Object o1, Object o2) {
                int i1 = Integer.parseInt((String) o1);
                int i2 = Integer.parseInt((String) o2);
                if (i1 == i2) {
                    return 0;
                }
                return i1 < i2 ? -1 : 1;
            }

            @Override // java.util.Comparator
            public boolean equals(Object obj) {
                return equals(obj);
            }
        });
        String strValue = "_TLD_" + tld + "_";
        Set<String> keys = rt.getValueKeys();
        if (!keys.isEmpty()) {
            for (String key : keys) {
                if (key.startsWith(strValue)) {
                    int length = strValue.length();
                    String key2 = key.substring(length);
                    Session.getLog().debug("KEY=" + key2);
                    prices.put(key2, rt.getValue(strValue + key2));
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT duration, price FROM tld_prices WHERE reseller_id = ? AND tld = ? AND duration > 0");
            ps.setLong(1, resellerId);
            ps.setString(2, tld);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (!prices.containsKey(rs.getString(1))) {
                    prices.put(rs.getString(1), rs.getString(2));
                }
            }
            Session.closeStatement(ps);
            con.close();
            return prices;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getYearsTLDPerPlan(Plan tmpPlan, String tld) throws UnknownResellerException, NoSuchTypeException, SQLException {
        List tldList = new ArrayList();
        ResourceType rt = tmpPlan.getResourceType(TypeRegistry.getTypeId("opensrs"));
        String strValue = "_TLD_" + tld + "_";
        Set<String> keys = rt.getValueKeys();
        if (!keys.isEmpty()) {
            for (String key : keys) {
                if (key.startsWith(strValue)) {
                    int length = strValue.length();
                    String key2 = key.substring(length);
                    Session.getLog().debug("---->added value (from plan) = " + key2);
                    tldList.add(key2);
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT duration FROM tld_prices WHERE reseller_id = ? AND tld = ? AND duration > 0 ORDER BY duration");
            ps.setLong(1, Session.getResellerId());
            ps.setString(2, tld);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (!tldList.contains(rs.getString(1))) {
                    tldList.add(rs.getString(1));
                    Session.getLog().debug("---->added value (from tld_prices) = " + rs.getString(1));
                }
            }
            Session.closeStatement(ps);
            con.close();
            return tldList;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void checkRegistarPeriod(int renewPeriod, String domainName, Plan plan) throws UnknownResellerException, NoSuchTypeException, SQLException, HSUserException {
        if (renewPeriod == 0) {
            return;
        }
        List activeYears = getYearsTLDPerPlan(plan, getTLD(domainName));
        if (!activeYears.contains(Integer.toString(renewPeriod))) {
            StringBuffer available = new StringBuffer("");
            Iterator i = activeYears.iterator();
            while (i.hasNext()) {
                String year = (String) i.next();
                available.append(year);
                if (i.hasNext()) {
                    available.append(",");
                }
            }
            throw new HSUserException("resource.opensrs.incorrectperiod", new Object[]{new Integer(renewPeriod), available});
        }
    }
}
