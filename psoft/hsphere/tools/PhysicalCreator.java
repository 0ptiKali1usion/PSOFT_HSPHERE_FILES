package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import org.apache.regexp.RE;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.ResourceTool;
import psoft.hsphere.resource.admin.LServerIP;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PhysicalCreator.class */
public class PhysicalCreator extends C0004CP {
    private boolean doDelete;
    private boolean doCreate;
    private long lServerId;
    private long startAccId;
    private int rGroup;
    private String accountIds;
    private ToolLogger logger;
    static final String ipCheckStr = "(([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])";
    private static Category log = Category.getInstance(PhysicalCreator.class.getName());

    public PhysicalCreator(long lServerId, int rGroup, boolean doDelete, boolean doCreate, String accountIds, long startAccId) throws Exception {
        super("psoft_config.hsphere");
        this.doDelete = false;
        this.doCreate = false;
        this.lServerId = -1L;
        this.startAccId = -1L;
        this.rGroup = -1;
        this.accountIds = null;
        this.logger = null;
        this.lServerId = lServerId;
        this.rGroup = rGroup;
        this.doDelete = doDelete;
        this.doCreate = doCreate;
        this.accountIds = accountIds;
        this.startAccId = startAccId;
        this.logger = ToolLogger.getDefaultLogger();
        Session.setResellerId(1L);
    }

    static boolean isGroupSupported(int group) {
        switch (group) {
            case 1:
            case 3:
            case 4:
            case 5:
            case 15:
            case 18:
                return true;
            case 2:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 16:
            case 17:
            default:
                return false;
        }
    }

    public String getDescription() {
        String action = "";
        String grDescription = "";
        switch (this.rGroup) {
            case 1:
                grDescription = "unix web resources";
                break;
            case 3:
                grDescription = "mail resources";
                break;
            case 4:
                grDescription = "MySQL resources";
                break;
            case 5:
                grDescription = "windows web resources";
                break;
            case 15:
                grDescription = "MSSQL resources";
                break;
            case 18:
                grDescription = "PGSQL resources";
                break;
        }
        if (this.doDelete && this.doCreate) {
            action = "re-creation of ";
        } else if (this.doDelete) {
            action = "deletion of ";
        } else if (this.doCreate) {
            action = "creation of ";
        }
        return action + grDescription + " on logical server #" + this.lServerId;
    }

    public void process() throws Exception {
        PreparedStatement ps;
        List<Hashtable> l = new ArrayList();
        Connection con = Session.getDb();
        try {
            if (this.rGroup == 5 || this.rGroup == 1) {
                ps = con.prepareStatement("SELECT DISTINCT parent_child.account_id, user_account.user_id FROM unix_user, parent_child, user_account WHERE unix_user.hostid = ? AND unix_user.id = parent_child.child_id AND parent_child.account_id = user_account.account_id AND parent_child.account_id >=? " + (this.accountIds == null ? "" : " AND parent_child.account_id IN (" + this.accountIds + ")") + " ORDER BY parent_child.account_id");
                ps.setLong(1, this.lServerId);
                ps.setLong(2, this.startAccId);
            } else if (this.rGroup == 4) {
                ps = con.prepareStatement("SELECT DISTINCT parent_child.account_id, user_account.user_id FROM mysqlres, parent_child, user_account WHERE mysqlres.mysql_host_id = ? AND mysqlres.id = parent_child.child_id AND parent_child.account_id = user_account.account_id AND parent_child.account_id >= ? " + (this.accountIds == null ? "" : " AND parent_child.account_id IN (" + this.accountIds + ")") + " ORDER BY parent_child.account_id");
                ps.setLong(1, this.lServerId);
                ps.setLong(2, this.startAccId);
            } else if (this.rGroup == 18) {
                ps = con.prepareStatement("SELECT DISTINCT parent_child.account_id, user_account.user_id FROM pgsqlres, parent_child, user_account WHERE pgsqlres.pgsql_host_id = ?  AND pgsqlres.id = parent_child.child_id AND parent_child.account_id = user_account.account_id AND parent_child.account_id >= ? " + (this.accountIds == null ? "" : " AND parent_child.account_id IN (" + this.accountIds + ")") + " ORDER BY parent_child.account_id");
                ps.setLong(1, this.lServerId);
                ps.setLong(2, this.startAccId);
            } else if (this.rGroup == 3) {
                ps = con.prepareStatement("SELECT DISTINCT parent_child.account_id, user_account.user_id FROM mail_services, parent_child, user_account WHERE mail_services.mail_server = ? AND mail_services.id = parent_child.child_id AND parent_child.account_id = user_account.account_id AND parent_child.account_id >= ? " + (this.accountIds == null ? "" : " AND parent_child.account_id IN (" + this.accountIds + ")") + " ORDER BY parent_child.account_id");
                ps.setLong(1, this.lServerId);
                ps.setLong(2, this.startAccId);
            } else if (this.rGroup == 15) {
                ps = con.prepareStatement("SELECT DISTINCT parent_child.account_id, user_account.user_id FROM mssqlres, parent_child, user_account WHERE mssqlres.mssql_host_id = ? AND mssqlres.id = parent_child.child_id AND parent_child.account_id = user_account.account_id AND parent_child.account_id >= ? " + (this.accountIds == null ? "" : " AND parent_child.account_id IN (" + this.accountIds + ")") + " ORDER BY parent_child.account_id");
                ps.setLong(1, this.lServerId);
                ps.setLong(2, this.startAccId);
            } else {
                throw new Exception("Unsupported resources group");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Hashtable t = new Hashtable();
                t.put("u", new Long(rs.getLong("user_id")));
                t.put("a", new ResourceId(rs.getInt("account_id"), 0));
                l.add(t);
            }
            Session.closeStatement(ps);
            con.close();
            Session.save();
            ResourceTool processor = new ResourceTool(this.doDelete, this.rGroup, this.doCreate, this.lServerId);
            for (Hashtable t2 : l) {
                long userId = ((Long) t2.get("u")).longValue();
                ResourceId rId = (ResourceId) t2.get("a");
                try {
                    User u = User.getUser(userId);
                    Session.setUser(u);
                    Account a = (Account) Account.get(rId);
                    Session.setAccount(a);
                    this.logger.outMessage("Got and processing account#" + a.getId().getId() + '\n');
                    processor.reset(a);
                } catch (Exception e) {
                    log.warn("Unable to reset values for account:" + rId, e);
                }
            }
            Session.restore();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static void setupIps(List lserverList) throws Exception {
        Iterator iter = lserverList.iterator();
        while (iter.hasNext()) {
            long lserverId = Long.parseLong((String) iter.next());
            LogicalServer lserver = LogicalServer.get(lserverId);
            if (lserver != null) {
                List<LServerIP> ips = lserver.getIps();
                if (ips != null) {
                    for (LServerIP lServerIP : ips) {
                        lServerIP.addIPPhysically(false, true);
                    }
                }
            } else {
                throw new Exception("Unable to get a logical server for id '" + lserverId + "'.");
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        boolean doDelete = false;
        boolean doCreate = false;
        boolean doSetupIps = false;
        String accountIds = null;
        long lServerId = -1;
        long startAccId = -1;
        long pServerId = -1;
        String pServerIP = null;
        List lServers = new ArrayList();
        List<PhysicalCreator> creators = new ArrayList();
        int rtype = -1;
        ToolLogger logger = new ToolLogger(argv);
        int i = 0;
        while (i < argv.length) {
            if ("-rg".equals(argv[i]) || "--rgroup".equals(argv[i])) {
                String type = argv[i + 1];
                if (type != null) {
                    String type2 = type.toLowerCase();
                    if ("unixweb".equals(type2)) {
                        rtype = 1;
                    } else if ("winweb".equals(type2)) {
                        rtype = 5;
                    } else if ("mysql".equals(type2)) {
                        rtype = 4;
                    } else if ("pgsql".equals(type2)) {
                        rtype = 18;
                    } else if ("mail".equals(type2)) {
                        rtype = 3;
                    } else if ("mssql".equals(type2)) {
                        rtype = 15;
                    } else {
                        logger.outMessage("Unsupported resource group " + type2 + '\n');
                        printHelp();
                        System.exit(1);
                    }
                } else {
                    logger.outMessage("Resource group is not specified.\n");
                    printHelp();
                    System.exit(1);
                }
            } else if ("-lid".equals(argv[i]) || "--lserverId".equals(argv[i])) {
                try {
                    lServerId = Long.parseLong(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    logger.outMessage("Unrecognized logical server id " + argv[i + 1] + '\n');
                }
            } else if ("-co".equals(argv[i]) || "--create-only".equals(argv[i])) {
                doCreate = true;
            } else if ("-do".equals(argv[i]) || "--delete-only".equals(argv[i])) {
                doDelete = true;
            } else if ("-rc".equals(argv[i]) || "--recreate".equals(argv[i])) {
                doDelete = true;
                doCreate = true;
            } else if ("-accs".equals(argv[i]) || "--accounts".equals(argv[i])) {
                accountIds = argv[i + 1];
                String sid = "";
                List accounts = new ArrayList();
                try {
                    StringTokenizer t = new StringTokenizer(accountIds, ",");
                    while (t.hasMoreTokens()) {
                        sid = t.nextToken();
                        accounts.add(new Long(sid));
                    }
                } catch (NumberFormatException e2) {
                    logger.outMessage("Unparseable account id " + sid + "\n");
                    printHelp();
                    System.exit(1);
                }
                i++;
            } else if ("--start-from".equals(argv[i]) || "-st".equals(argv[i])) {
                String startAcc = argv[i + 1];
                try {
                    startAccId = Long.parseLong(startAcc);
                    i++;
                } catch (NumberFormatException e3) {
                    logger.outMessage("Unparseable start account id " + startAcc + "\n");
                    printHelp();
                    System.exit(1);
                }
            } else if ("-pid".equals(argv[i]) || "--pServerId".equals(argv[i])) {
                try {
                    pServerId = Long.parseLong(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e4) {
                    logger.outMessage("Unrecognized physical server id " + argv[i + 1] + '\n');
                }
            } else if ("-pip".equals(argv[i]) || "--pServerIP".equals(argv[i])) {
                i++;
                pServerIP = argv[i];
                pServerId = 0;
                RE ipRE = new RE(ipCheckStr);
                if (!ipRE.match(pServerIP)) {
                    logger.outMessage("IP '" + pServerIP + "' was specified incorrectly.");
                    System.exit(2);
                }
            } else if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                printHelp();
                System.exit(0);
            } else if ("--setup-ips".equals(argv[i])) {
                doSetupIps = true;
            }
            i++;
        }
        if (((lServerId > 0 && rtype > 0) || pServerId >= 0) && (doCreate || doDelete || doSetupIps)) {
            if (pServerId >= 0) {
                ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
                Database db = Toolbox.getDB(config);
                Connection con = db.getConnection();
                PreparedStatement ps = null;
                try {
                    if (pServerIP == null) {
                        ps = con.prepareStatement("SELECT ls.id, lsg.type_id FROM l_server ls, l_server_groups lsg WHERE ls.p_server_id = ? AND ls.group_id = lsg.id");
                        ps.setLong(1, pServerId);
                    } else {
                        ps = con.prepareStatement("SELECT ls.id, lsg.type_id FROM l_server ls, l_server_groups lsg, p_server p WHERE ls.p_server_id = p.id AND ls.group_id = lsg.id AND p.ip1 = ?");
                        ps.setString(1, pServerIP);
                    }
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int group = rs.getInt("type_id");
                        if ((doCreate || doDelete) && isGroupSupported(group)) {
                            PhysicalCreator creator = new PhysicalCreator(rs.getLong("id"), group, doDelete, doCreate, accountIds, startAccId);
                            creators.add(creator);
                        }
                        if (doSetupIps) {
                            lServers.add(rs.getString("id"));
                        }
                    }
                } finally {
                    Session.closeStatement(ps);
                    con.close();
                }
            } else {
                if (doCreate || doDelete) {
                    PhysicalCreator creator2 = new PhysicalCreator(lServerId, rtype, doDelete, doCreate, accountIds, startAccId);
                    creators.add(creator2);
                }
                if (doSetupIps) {
                    lServers.add(String.valueOf(lServerId));
                }
            }
            for (PhysicalCreator ph : creators) {
                ph = null;
                try {
                    logger.outMessage("STARTING " + ph.getDescription() + '\n');
                    ph.process();
                    logger.outMessage(ph.getDescription() + " FINISHED\n");
                } catch (Exception ex) {
                    logger.outMessage("An error occured while processing " + ph.getDescription(), ex);
                }
            }
            if (doSetupIps && !lServers.isEmpty()) {
                setupIps(lServers);
            }
            logger.outMessage("Processing finished.\n");
        } else {
            logger.outMessage("Invalid parameters.\n");
            printHelp();
            System.exit(1);
        }
        System.exit(0);
    }

    public static void printHelp() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.PhysicalCreator - H-Sphere windows based webhosting resources regenerator utility");
        System.out.println("USAGE EXAMPLE:\n\t java -Xms64M -Xmx512M psoft.hsphere.tools.PhysicalCreator -rg mail -co -lid 22 ");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.PhysicalCreator options");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t- shows this screen");
        System.out.println("\t-rg|--rgroup \t\t- resource group to perform operations on ");
        System.out.println("\t The following resource groups are allowed:");
        System.out.println("\t\t unixweb:\tUnix virtual hosting resources");
        System.out.println("\t\t winweb:\tWindows virtual hosting resources");
        System.out.println("\t\t mysql:\t\tMySQL resources");
        System.out.println("\t\t pgsql:\t\tPGSQL resources");
        System.out.println("\t\t mail:\t\tMail resources");
        System.out.println("\t-co|--create-only \t- performs resource creation routines only");
        System.out.println("\t-do|--delete-only \t- performs resource deletion routines only");
        System.out.println("\t-rc|--recreate \t\t- performs both resource deletion and creation routines");
        System.out.println("\t-lid|--lserverId \t- process accounts on logical server with given number");
        System.out.println("\t-pid|--pServerId \t- process accounts on physical server with given number. In this case -rg and -lid parameters will be ignored");
        System.out.println("\t-accs|--accounts \t- account IDs separated by comma");
        System.out.println("\t-st|--start-from \t- account ID. Process will start from this account ID");
        System.out.println("\t--setup-ips \t\t- set up ips on a physical server at the end of the re-creation process. It can be used without the -co and -do options to set up IPs without creating resources.");
        System.out.println("It is recommeneded to specify Xms Xmx parameters to ");
        System.out.println("the java command when recreating a big number of accounts.");
        System.out.println("To learn more, type java -X");
    }
}
