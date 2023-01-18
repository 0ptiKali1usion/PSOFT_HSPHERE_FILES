package psoft.hsphere.promotion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/AbstractPromoDataStorage.class */
public abstract class AbstractPromoDataStorage {
    protected Hashtable data;
    protected long promoId;

    public abstract int getDataType();

    public AbstractPromoDataStorage() {
    }

    public AbstractPromoDataStorage(long promoId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        this.promoId = promoId;
        try {
            ps = con.prepareStatement("SELECT name, value FROM promo_values WHERE type = ? AND promo_id = ? ");
            ps.setInt(1, getDataType());
            ps.setLong(2, promoId);
            ResultSet rs = ps.executeQuery();
            this.data = new Hashtable();
            while (rs.next()) {
                this.data.put(rs.getString("name"), rs.getString("value"));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public AbstractPromoDataStorage(long promoId, Hashtable data) throws Exception {
        saveData(promoId, data);
    }

    private void saveData(long promoId, Hashtable dataMap) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO  promo_values(promo_id, type, name, value) VALUES (?, ?, ?, ?)");
            ps.setLong(1, promoId);
            ps.setLong(2, getDataType());
            Session.getLog().debug("INSIDE AbstractPromoDataStorage data=" + this.data);
            this.data = new Hashtable();
            for (String key : dataMap.keySet()) {
                ps.setString(3, key);
                ps.setString(4, (String) dataMap.get(key));
                ps.executeUpdate();
                this.data.put(key, (String) dataMap.get(key));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateData(long promoId, Hashtable data) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM promo_values WHERE promo_id = ? AND type = ?");
            ps.setLong(1, promoId);
            ps.setInt(2, getDataType());
            ps.executeUpdate();
            saveData(promoId, data);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete(long promoId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM promo_values WHERE promo_id = ? AND type = ?");
            ps.setLong(1, promoId);
            ps.setInt(2, getDataType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Hashtable getData() {
        return this.data;
    }
}
