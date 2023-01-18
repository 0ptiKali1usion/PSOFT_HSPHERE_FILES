package psoft.hsphere.resource.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailMan.class */
public class MailMan {
    protected static MailMan mailman = null;
    protected static Map resellerMap = new HashMap();

    public static MailMan getMailMan() throws Exception {
        if (mailman == null) {
            mailman = new MailMan();
        }
        if (mailman.getTypes().size() <= 0) {
            mailman.init();
        }
        return mailman;
    }

    protected Map getTypes() throws UnknownResellerException {
        return getTypes(Session.getResellerId());
    }

    protected static synchronized Map getTypes(long resellerId) {
        HashMap types = (Map) resellerMap.get(new Long(resellerId));
        if (types == null) {
            types = new HashMap();
            resellerMap.put(new Long(resellerId), types);
        }
        return types;
    }

    protected void addEntry(String type, String email, String name) throws UnknownResellerException {
        ArrayList entries = (List) getTypes().get(type);
        if (entries == null) {
            entries = new ArrayList();
            getTypes().put(type, entries);
        }
        HashMap hashMap = new HashMap();
        hashMap.put("email", email);
        hashMap.put("name", name);
        entries.add(hashMap);
    }

    public void init() throws Exception {
        Map tmpMap = getTypes();
        synchronized (tmpMap) {
            if (tmpMap.size() > 0) {
                return;
            }
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT action, email, name FROM mailman WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                addEntry(rs.getString(1), rs.getString(2), rs.getString(3));
            }
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Address[] getEmails(int type) throws Exception {
        List<Map> entries = getRecipients(type);
        if (entries == null) {
            return new Address[0];
        }
        Address[] result = new Address[entries.size()];
        int count = 0;
        for (Map entry : entries) {
            if (entry.get("name") != null) {
                int i = count;
                count++;
                result[i] = new InternetAddress((String) entry.get("email"), (String) entry.get("name"));
            } else {
                int i2 = count;
                count++;
                result[i2] = new InternetAddress((String) entry.get("email"));
            }
        }
        return result;
    }

    public List getRecipients(String type) throws Exception {
        return (List) getTypes().get(type);
    }

    public List getRecipients(int type) throws Exception {
        return (List) getTypes().get(Integer.toString(type));
    }

    public Map getRecipients() throws Exception {
        return getTypes();
    }

    public void addRecipient(int type, String email, String name) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO mailman (action, email, name, reseller_id) VALUES (?, ?, ?, ?)");
            ps.setInt(1, type);
            ps.setString(2, email);
            ps.setString(3, name);
            ps.setLong(4, Session.getResellerId());
            ps.executeUpdate();
            addEntry(Integer.toString(type), email, name);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteAllRecipients(int type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mailman WHERE action= ? AND reseller_id = ?");
            ps.setInt(1, type);
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            getTypes().put(Integer.toString(type), new ArrayList());
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void deleteEntry(String type, String email) throws UnknownResellerException {
        List entries = (List) getTypes().get(type);
        if (entries == null) {
            return;
        }
        ListIterator i = entries.listIterator();
        while (i.hasNext()) {
            Map entry = (Map) i.next();
            if (email.equals(entry.get("email"))) {
                i.remove();
            }
        }
    }

    public void deleteRecipient(int type, String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mailman WHERE action = ? AND LOWER(email) = LOWER(?) AND reseller_id = ?");
            ps.setInt(1, type);
            ps.setString(2, email);
            ps.setLong(3, Session.getResellerId());
            ps.executeUpdate();
            deleteEntry(Integer.toString(type), email);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
