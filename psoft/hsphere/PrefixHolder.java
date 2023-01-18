package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/PrefixHolder.class */
public class PrefixHolder {
    private static int MAX_PERIX_LENGTH = 7;
    private static int MAX_SUBPREFIX_LENGTH = 3;
    private static Hashtable prefixes = new Hashtable();
    private static ThreadLocal genPrefixes = new ThreadLocal();

    /* JADX WARN: Finally extract failed */
    static {
        synchronized (prefixes) {
            PreparedStatement ps = null;
            Connection con = Session.getDb();
            try {
                try {
                    ps = con.prepareStatement("SELECT user_id, prefix FROM prefixes");
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        prefixes.put(new Long(rs.getLong(1)), rs.getString(2));
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception e) {
                    Session.getLog().error("Error loading users' prefixes");
                    Session.closeStatement(ps);
                    con.close();
                }
            } catch (Throwable th) {
                Session.closeStatement(null);
                con.close();
                throw th;
            }
        }
    }

    public String getUserPrefix(long userId) throws Exception {
        if (!isGenPrefixes()) {
            return "";
        }
        synchronized (prefixes) {
            if (prefixes.keySet().contains(new Long(userId))) {
                return ((String) prefixes.get(new Long(userId))) + "_";
            }
            return createUserPrefix(userId) + "_";
        }
    }

    private String createUserPrefix(long userId) throws Exception {
        String newPrefix;
        String uname = User.getUser(userId).getLogin();
        if (!prefixes.values().contains(uname.substring(0, Math.min(uname.length(), MAX_PERIX_LENGTH)))) {
            newPrefix = uname.substring(0, Math.min(uname.length(), MAX_PERIX_LENGTH));
        } else {
            newPrefix = genNewPrefix(uname);
        }
        addNewPrefix(userId, newPrefix);
        return newPrefix;
    }

    public String genNewPrefix(String uname) throws Exception {
        for (int discounter = 0; ("" + (discounter * 10)).length() < MAX_SUBPREFIX_LENGTH; discounter++) {
            for (int i = 1; i < 11; i++) {
                String subpref = "" + ((discounter * 10) + i);
                if (subpref.length() > MAX_SUBPREFIX_LENGTH) {
                    throw new Exception("Unable to generate prefix for " + uname);
                }
                String pref = uname.substring(0, Math.min(uname.length(), MAX_PERIX_LENGTH) - subpref.length()) + subpref;
                if (!prefixes.values().contains(pref)) {
                    return pref;
                }
            }
        }
        throw new Exception("Unable to generate prefix for " + uname);
    }

    private void addNewPrefix(long userId, String newPrefix) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO prefixes(user_id, prefix) VALUES(?,?)");
            ps.setLong(1, userId);
            ps.setString(2, newPrefix);
            ps.executeUpdate();
            prefixes.put(new Long(userId), newPrefix);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delPrefix(long userId) throws Exception {
        synchronized (prefixes) {
            if (prefixes.keySet().contains(new Long(userId))) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("DELETE FROM prefixes WHERE user_id = ?");
                ps.setLong(1, userId);
                ps.executeUpdate();
                prefixes.remove(new Long(userId));
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    public static void disablePrefixes() {
        genPrefixes.set(Boolean.FALSE);
    }

    public static void enablePrefixes() {
        genPrefixes.set(Boolean.TRUE);
    }

    public static boolean isGenPrefixes() {
        if (genPrefixes.get() == null) {
            enablePrefixes();
            return true;
        }
        return ((Boolean) genPrefixes.get()).booleanValue();
    }
}
