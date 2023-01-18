package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Urchin4Resource;

/* loaded from: hsphere.zip:psoft/hsphere/tools/UrchinReconfig.class */
public class UrchinReconfig extends C0004CP {
    protected ArrayList accounts;
    protected ArrayList servers;

    UrchinReconfig() {
        super("psoft_config.hsphere");
        this.accounts = null;
        this.servers = null;
    }

    protected void reconfig() throws Exception {
        Connection con = Session.getDb();
        String query = "SELECT p.account_id, u.id FROM urchin4 u, parent_child p WHERE p.child_id=u.id";
        if (this.accounts != null) {
            String list = "";
            Iterator i = this.accounts.iterator();
            while (i.hasNext()) {
                if (list.length() > 0) {
                    list = list + ",";
                }
                list = list + i.next();
            }
            query = query + " AND p.account_id IN (" + list + ")";
        }
        PreparedStatement ps = con.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            try {
                String aid = rs.getString(1);
                System.out.println("Account #" + aid);
                ResourceId accountId = new ResourceId(aid + "_0");
                Account newAccount = (Account) Account.get(accountId);
                Session.setAccount(newAccount);
                Session.setUser(newAccount.getUser());
                ResourceId rid = new ResourceId(rs.getLong(2), 65);
                Urchin4Resource urchin = (Urchin4Resource) rid.get();
                if (this.servers != null) {
                    String server1 = urchin.recursiveGet("host_id").toString();
                    Iterator i2 = this.servers.iterator();
                    while (true) {
                        if (!i2.hasNext()) {
                            break;
                        }
                        String server2 = (String) i2.next();
                        if (server2.equals(server1)) {
                            System.out.println("Domain " + urchin.recursiveGet("name"));
                            urchin.ipRestart();
                            break;
                        }
                    }
                } else {
                    System.out.println("Domain " + urchin.recursiveGet("name"));
                    urchin.ipRestart();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected boolean readParameters(String[] args) throws Exception {
        if (args.length <= 0) {
            return false;
        }
        int i = 0;
        while (i < args.length) {
            if ("-a".equals(args[i]) || "--accounts".equals(args[i])) {
                StringTokenizer tokenizer = new StringTokenizer(args[i + 1], ",");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (token.equals("all")) {
                        break;
                    }
                    if (this.accounts == null) {
                        this.accounts = new ArrayList();
                    }
                    this.accounts.add(token);
                }
                i++;
            } else if ("-s".equals(args[i]) || "--servers".equals(args[i])) {
                StringTokenizer tokenizer2 = new StringTokenizer(args[i + 1], ",");
                while (tokenizer2.hasMoreTokens()) {
                    String token2 = tokenizer2.nextToken();
                    if (token2.equals("all")) {
                        break;
                    }
                    if (this.servers == null) {
                        this.servers = new ArrayList();
                    }
                    this.servers.add(token2);
                }
                i++;
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                return false;
            }
            i++;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        UrchinReconfig urchinReconfig = new UrchinReconfig();
        if (urchinReconfig.readParameters(args)) {
            urchinReconfig.reconfig();
            return;
        }
        System.out.println("NAME:\n\t psoft.hsphere.tools.UrchinReconfig\n\t\t- Regenerate Urchin config");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.UrchinReconfig [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t-a|--accounts list of account IDs, or all for 'all' accounts, ',' - delimiter");
        System.out.println("\t-s|--servers list of logical server IDs, or 'all' for all servers, ',' - delimiter");
        System.out.println("SAMPLE:");
        System.out.println("\tjava psoft.hsphere.tools.UrchinReconfig -a '1002,8383,1237' -s '12,35,37'");
        System.out.println("\tjava psoft.hsphere.tools.UrchinReconfig -a all -s all");
    }
}
