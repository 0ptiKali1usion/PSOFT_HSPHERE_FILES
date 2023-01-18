package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/ComodoManager.class */
public class ComodoManager implements TemplateHashModel {
    private static final Category log = Category.getInstance(ComodoManager.class.getName());

    public TemplateModel get(String key) throws TemplateModelException {
        if ("login".equals(key)) {
            try {
                return new TemplateString(getInfo().getLogin());
            } catch (Exception e) {
                log.warn(e);
                return null;
            }
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public void FM_setLoginPassword(String login, String password) throws SQLException, UnknownResellerException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT login FROM comodo_accounts WHERE reseller_id = ?");
            ps2.setLong(1, Session.getResellerId());
            ResultSet rs = ps2.executeQuery();
            boolean update = rs.next();
            rs.close();
            Session.closeStatement(ps2);
            if (update) {
                ps = con.prepareStatement("UPDATE comodo_accounts SET login = ?, password = ? WHERE reseller_id = ?");
            } else {
                ps = con.prepareStatement("INSERT INTO comodo_accounts (login, password, reseller_id) VALUES (?, ?, ?)");
            }
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setLong(3, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean FM_isEnabled() throws UnknownResellerException, SQLException, ParseException {
        try {
            getInfo();
            return FM_getPrices().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public TemplateList FM_listProducts() throws SQLException, UnknownResellerException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT period, product, price FROM comodo_prices WHERE reseller_id = ? GROUP BY product, period, price ORDER BY price");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            TemplateList l = new TemplateList();
            while (rs.next()) {
                TemplateMap m = new TemplateMap();
                m.put("id", rs.getString(2));
                m.put("term", rs.getString(1));
                m.put("price", rs.getString(3));
                l.add((TemplateModel) m);
            }
            rs.close();
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateMap FM_getPrices() throws SQLException, UnknownResellerException, ParseException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT period, product, price FROM comodo_prices WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            Map map = new HashMap();
            while (rs.next()) {
                map.put(rs.getString(2) + "_" + rs.getString(1), USFormat.format(rs.getDouble(3)));
            }
            rs.close();
            TemplateMap templateMap = new TemplateMap(map);
            Session.closeStatement(ps);
            con.close();
            return templateMap;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_setPrices() throws SQLException, UnknownResellerException {
        Connection con = Session.isTransConnection() ? Session.getDb() : Session.getTransConnection();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM comodo_prices WHERE reseller_id = ?");
            ps2.setLong(1, Session.getResellerId());
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("INSERT INTO comodo_prices (product, period, price, reseller_id) VALUES (?, ?, ?, ?)");
            ps.setLong(4, Session.getResellerId());
            Map m = Session.getRequest().getParameterMap();
            for (String key : m.keySet()) {
                if (key.startsWith("product_")) {
                    String value = ((String[]) m.get(key))[0];
                    if (value != null && value.length() != 0) {
                        int index = key.lastIndexOf(95);
                        String product = key.substring(8, index);
                        String period = key.substring(index + 1);
                        ps.setString(1, product);
                        ps.setInt(2, Integer.parseInt(period));
                        try {
                            ps.setDouble(3, USFormat.parseDouble(value));
                            ps.addBatch();
                        } catch (ParseException pe) {
                            log.warn("Unable to parse: " + value, pe);
                        }
                    }
                }
            }
            ps.executeBatch();
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
        } catch (Throwable t) {
            try {
                con.rollback();
                if (t instanceof SQLException) {
                    throw ((SQLException) t);
                }
                if (t instanceof UnknownResellerException) {
                    throw ((UnknownResellerException) t);
                }
                log.error("ERROR:", t);
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (Throwable th) {
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
                throw th;
            }
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public static ComodoManagerInfo getInfo() throws SQLException, UnknownResellerException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM comodo_accounts WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            ComodoManagerInfo comodoManagerInfo = new ComodoManagerInfo(rs.getString(1), rs.getString(2));
            Session.closeStatement(ps);
            con.close();
            return comodoManagerInfo;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static double getPrice(int period, String product) throws SQLException, UnknownResellerException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT price FROM comodo_prices WHERE reseller_id = ? AND period = ? AND product = ?");
            ps.setLong(1, Session.getResellerId());
            ps.setInt(2, period);
            ps.setString(3, product);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double d = rs.getDouble(1);
                Session.closeStatement(ps);
                con.close();
                return d;
            }
            Session.closeStatement(ps);
            con.close();
            return Double.NaN;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
