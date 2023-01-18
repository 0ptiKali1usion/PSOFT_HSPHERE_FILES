package psoft.hsphere.migrator;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/CPANELUserInfoCollector.class */
public class CPANELUserInfoCollector implements UserInfoCollector {
    PhysicalSource source;

    public CPANELUserInfoCollector(PhysicalSource s) {
        this.source = null;
        this.source = s;
    }

    public CPANELUserInfoCollector(long id) throws Exception {
        this.source = null;
        Hashtable params = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, value FROM msources WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                params.put(rs.getString("name"), rs.getString("value"));
            }
            Session.closeStatement(ps);
            con.close();
            this.source = new LinuxPhysicalSource(params);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.migrator.UserInfoCollector
    public Document getUsersXML(ArrayList requestedUsers) throws Exception {
        DOMParser parser = new DOMParser();
        try {
            Reader r = this.source.exec2("/hsphere/shared/scripts/migrate/cpanel/collect_data_new.pl", (String[]) requestedUsers.toArray(new String[]{""}), null);
            parser.parse(new InputSource(r));
            Document doc = parser.getDocument();
            return doc;
        } catch (Exception ex) {
            System.out.println("Error parsing");
            throw ex;
        }
    }

    @Override // psoft.hsphere.migrator.UserInfoCollector
    public Document getResellersXML(ArrayList requestedResellers) throws Exception {
        DOMParser parser = new DOMParser();
        try {
            Reader r = this.source.exec2("/hsphere/shared/scripts/migrate/cpanel/get_reseller_conf.pl", (String[]) requestedResellers.toArray(new String[]{""}), null);
            parser.parse(new InputSource(r));
            Document doc = parser.getDocument();
            return doc;
        } catch (Exception ex) {
            System.out.println("Error parsing");
            throw ex;
        }
    }

    @Override // psoft.hsphere.migrator.UserInfoCollector
    public List getResellersList() throws Exception {
        List rl = new ArrayList();
        BufferedReader r = (BufferedReader) this.source.exec2("/hsphere/shared/scripts/migrate/cpanel/get_resellers.pl", new String[]{""}, null);
        while (true) {
            String tstr = r.readLine();
            if (null != tstr) {
                rl.add(tstr);
            } else {
                return rl;
            }
        }
    }

    @Override // psoft.hsphere.migrator.UserInfoCollector
    public List getUsersList() throws Exception {
        List u = new ArrayList();
        BufferedReader r = (BufferedReader) this.source.exec2("/hsphere/shared/scripts/migrate/cpanel/get_users.pl", new String[]{""}, null);
        while (true) {
            String tstr = r.readLine();
            if (null != tstr) {
                u.add(tstr);
            } else {
                return u;
            }
        }
    }

    public static void main(String[] argv) throws Exception {
    }
}
