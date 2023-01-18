package psoft.hsphere.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.global.Globals;
import psoft.web.utils.UnicodeCoder;

/* loaded from: hsphere.zip:psoft/hsphere/admin/Settings.class */
public class Settings {
    protected Map resellerMap = new HashMap();
    protected static Settings settings;
    protected static boolean useFile;

    static {
        try {
            useFile = Session.getPropertyString("LARGE_VALUE_DIR").length() != 0;
            settings = new Settings();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Settings get() {
        return settings;
    }

    protected Settings() throws Exception {
        String value;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT name, value, reseller_id FROM settings");
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                getMap(rs.getLong(3)).put(rs.getString(1), UnicodeCoder.decode(rs.getString(2)));
            }
            ps2.close();
            ps = con.prepareStatement("SELECT name, value, reseller_id FROM settings_l");
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                if (useFile) {
                    value = loadFile(rs2.getString(1));
                } else {
                    value = Session.getClobValue(rs2, 2);
                }
                getMap(rs2.getLong(3)).put(rs2.getString(1), UnicodeCoder.decode(value));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Map getMap() throws UnknownResellerException {
        return getMap(Session.getResellerId());
    }

    protected Map getMap(long resellerId) {
        String reseller = Long.toString(resellerId);
        HashMap map = (Map) this.resellerMap.get(reseller);
        if (map == null) {
            map = new HashMap();
            this.resellerMap.put(reseller, map);
        }
        return map;
    }

    public String getValue(String name) {
        try {
            return (String) getMap().get(name);
        } catch (UnknownResellerException e) {
            return "";
        } catch (Exception e2) {
            Session.getLog().warn("Cant get settings value", e2);
            return null;
        }
    }

    public String getAdminGlobalValue(String key) {
        try {
            return (String) getMap(1L).get(Globals.GLB_VALUE_PREFIX + key);
        } catch (Exception e) {
            Session.getLog().warn("Can't get settings' value", e);
            return null;
        }
    }

    public String getGlobalValue(String key) {
        return getValue(Globals.GLB_VALUE_PREFIX + key);
    }

    protected String getFileName(String name) throws Exception {
        return Session.getPropertyString("LARGE_VALUE_DIR") + '/' + name + "_" + Session.getResellerId();
    }

    protected void deleteFile(String name) throws Exception {
        File f = new File(getFileName(name));
        f.delete();
    }

    protected String loadFile(String name) {
        try {
            FileReader file = new FileReader(getFileName(name));
            BufferedReader in = new BufferedReader(file);
            StringBuffer result = new StringBuffer();
            while (true) {
                String tmp = in.readLine();
                if (tmp == null) {
                    in.close();
                    file.close();
                    return result.toString();
                }
                result.append(tmp).append("\n");
            }
        } catch (Throwable th) {
            return "";
        }
    }

    protected void saveFile(String name, String value) throws Exception {
        String fileName = getFileName(name);
        int lIndex = fileName.lastIndexOf("/");
        if (lIndex > -1) {
            File f = new File(fileName.substring(0, lIndex));
            f.mkdirs();
        }
        FileWriter file = new FileWriter(fileName);
        PrintWriter out = new PrintWriter(file);
        out.print(value);
        out.close();
        file.close();
    }

    public void setLargeValue(String name, String value) throws Exception {
        PreparedStatement ps;
        if (name.equals("license")) {
            C0004CP.setLicense(value);
        }
        Connection con = Session.getDb();
        try {
            if (value != null) {
                try {
                    if (value.length() != 0) {
                        if (useFile) {
                            saveFile(name, value);
                        }
                        if (((String) getMap().get(name)) == null) {
                            ps = con.prepareStatement("INSERT INTO settings_l (value, name, reseller_id) VALUES (?, ?, ?)");
                        } else {
                            ps = con.prepareStatement("UPDATE settings_l SET value = ? WHERE name = ? AND reseller_id = ?");
                        }
                        if (useFile) {
                            ps.setString(1, "USING FILES");
                        } else {
                            Session.setClobValue(ps, 1, value);
                        }
                        ps.setString(2, name);
                        ps.setLong(3, Session.getResellerId());
                        ps.executeUpdate();
                        getMap().put(name, value);
                        Session.closeStatement(ps);
                        con.close();
                    }
                } catch (Exception e) {
                    Session.getLog().info("SOMETHING WRONG", e);
                    throw e;
                }
            }
            getMap().remove(name);
            if (useFile) {
                deleteFile(name);
            }
            ps = con.prepareStatement("DELETE FROM settings_l WHERE name = ? AND reseller_id = ?");
            ps.setString(1, name);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void setValue(String name, String value) throws Exception {
        PreparedStatement ps;
        Connection con = Session.getDb();
        if (value != null) {
            try {
                if (value.length() != 0) {
                    if (((String) getMap().get(name)) == null) {
                        ps = con.prepareStatement("INSERT INTO settings (value, name, reseller_id) VALUES (?, ?, ?)");
                    } else {
                        ps = con.prepareStatement("UPDATE settings SET value = ? WHERE name = ? AND reseller_id = ?");
                    }
                    ps.setString(1, value);
                    ps.setString(2, name);
                    ps.setLong(3, Session.getResellerId());
                    ps.executeUpdate();
                    getMap().put(name, value);
                    Session.closeStatement(ps);
                    con.close();
                }
            } catch (Throwable th) {
                Session.closeStatement(null);
                con.close();
                throw th;
            }
        }
        getMap().remove(name);
        ps = con.prepareStatement("DELETE FROM settings WHERE name = ? AND reseller_id = ?");
        ps.setString(1, name);
        ps.setLong(2, Session.getResellerId());
        ps.executeUpdate();
        Session.closeStatement(ps);
        con.close();
    }

    public boolean isPromoEnabled() {
        String val = getValue("_PROMO_ENABLED");
        return !isEmpty(val) && "TRUE".equals(val.toUpperCase());
    }

    protected boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
