package psoft.hsphere.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/manager/Entity.class */
public class Entity {
    protected Map data;

    /* renamed from: id */
    protected Integer f96id;
    protected String description;

    public void setDescription(String d) {
        this.description = d;
    }

    public String getDescription() {
        return this.description;
    }

    public String get(String key) {
        if (this.data.get(key) != null) {
            return ((String[]) this.data.get(key))[0];
        }
        return null;
    }

    public String[] getValues(String key) {
        return (String[]) this.data.get(key);
    }

    public Set getKeys() {
        return this.data.keySet();
    }

    public int getId() {
        return this.f96id.intValue();
    }

    public Entity(Integer id, String description) throws ManagerException, SQLException {
        String[] values;
        this.f96id = id;
        this.description = description;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mkey, value FROM managed_entity_data WHERE id = ?");
            ps.setInt(1, id.intValue());
            ResultSet rs = ps.executeQuery();
            this.data = new HashMap();
            while (rs.next()) {
                String[] values2 = (String[]) this.data.get(rs.getString(1));
                if (values2 == null) {
                    values = new String[]{rs.getString(2)};
                } else {
                    List tmpList = new ArrayList(Arrays.asList(values2));
                    tmpList.add(rs.getString(2));
                    values = (String[]) tmpList.toArray(values2);
                }
                this.data.put(rs.getString(1), values);
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
