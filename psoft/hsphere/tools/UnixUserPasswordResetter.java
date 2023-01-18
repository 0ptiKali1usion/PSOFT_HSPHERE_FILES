package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;

/* loaded from: hsphere.zip:psoft/hsphere/tools/UnixUserPasswordResetter.class */
public class UnixUserPasswordResetter extends C0004CP {
    long lserverId;
    String uname;

    public UnixUserPasswordResetter(long sId, String uname) throws Exception {
        super("psoft_config.hsphere");
        this.uname = uname;
        this.lserverId = sId;
    }

    /* renamed from: go */
    public void m1go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        StringBuffer query = new StringBuffer("SELECT a.login, a.password, a.hostid FROM unix_user a, l_server b WHERE a.hostid =  b.id AND b.type_id = ?");
        if (this.lserverId > 0) {
            query.append(" AND a.hostid = ?");
        }
        if (this.uname.length() > 0) {
            query.append(" AND a.login = ?");
        }
        try {
            ps = con.prepareStatement(query.toString());
            int c = 1 + 1;
            ps.setInt(1, 1);
            if (this.lserverId > 0) {
                c++;
                ps.setLong(c, this.lserverId);
            }
            if (this.uname.length() > 0) {
                int i = c;
                int i2 = c + 1;
                ps.setString(i, this.uname);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    System.out.print("Setting password for user " + rs.getString("login"));
                    List args = new ArrayList();
                    args.add(rs.getString("login"));
                    args.add(MailServices.shellQuote(rs.getString("password")));
                    HostEntry host = HostManager.getHost(rs.getLong("hostid"));
                    host.exec("user-changepwd", args);
                    System.out.println("   [    OK    ]");
                } catch (Exception ex) {
                    Session.getLog().error("Unable to re-set password", ex);
                    System.out.println("   [   FAIL   ]");
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        long serverId = -1;
        boolean configured = false;
        String username = "";
        for (int i = 0; i < argv.length; i++) {
            if ("-lid".equals(argv[i]) || "--lserverId".equals(argv[i])) {
                try {
                    String si = argv[i + 1];
                    if ("ALL".equals(si)) {
                        serverId = 0;
                        configured = true;
                    } else {
                        serverId = Long.parseLong(si);
                        configured = true;
                    }
                } catch (NumberFormatException e) {
                    configured = false;
                }
            }
            if ("-u".equals(argv[i]) || "--user".equals(argv[i])) {
                username = argv[i + 1];
                configured = true;
            }
        }
        if (configured) {
            System.out.print("Initializing ...");
            UnixUserPasswordResetter sa = new UnixUserPasswordResetter(serverId, username);
            System.out.println(" Done");
            sa.m1go();
        } else {
            printHelp();
        }
        System.exit(0);
    }

    public static void printHelp() {
        System.out.println("H-Sphere unix user password setter.");
        System.out.println("Usage java psoft.hsphere.tools.UnixUserPasswordResetter -lid serverNum|ALL [-u username]\n\n");
        System.out.println("\tlid|--lserverId Number of the unix web logical server you want\n \t\tpasswords be re-set.\n\t\tAlso possible ALL value to reset paswords on all unix web logical servers\n");
        System.out.println("\t-u username You can specify username for which you are want password be re-set.");
    }
}
