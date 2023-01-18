package psoft.hsphere.tools;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Hashtable;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MoveHostDependentResources.class */
public abstract class MoveHostDependentResources extends C0004CP {
    private Document doc;
    private long lserverId;
    private long accId;
    private ToolLogger logger;

    /* renamed from: go */
    abstract void mo11go(long j, long j2) throws Exception;

    public MoveHostDependentResources(Hashtable h) throws Exception {
        super("psoft_config.hsphere");
        this.doc = null;
        this.lserverId = -1L;
        this.accId = -1L;
        this.logger = null;
        if (h.keySet().contains("doc")) {
            this.doc = (Document) h.get("doc");
            return;
        }
        this.accId = ((Long) h.get("account")).longValue();
        this.lserverId = ((Long) h.get("server")).longValue();
    }

    public static void printHelpMessage() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.MoveVHostResources\n\t\t- H-Sphere moving accounts between H-Sphere servers\n\t\twithin the same control panel utility");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.MoveVHostResources [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t-acc|--accId ACCOUNT_ID -lid|----lserverId LOGICAL_SERVER_ID\n\t\twhere\n\t\tACCOUNT_ID - id of the account you want to move;\n\t\tLOGICAL_SERVER_ID - id of the logical server you want to move account to;\n\t-c|--config CONFIG_FILE\n\t\twhere\n\t\tCONFIG_FILE - is path to the config file of the following format:\n\t\t<accounts>\n\t\t    <account id=\"ACCOUNT_ID1\" serverId=\"SERVER_ID1\"/>\n\t\t    <account id=\"ACCOUNT_ID2\" serverId=\"SERVER_ID2\"/>\n\t\t    <account id=\"ACCOUNT_ID3\" serverId=\"SERVER_ID3\"/>\n\t\t    ................................................\n\t\t    <account id=\"ACCOUNT_IDN\" serverId=\"SERVER_IDN\"/>\n\t\t</accounts>\n\t\twhere\n\t\tACCOUNT_ID1-N is an ID of the account you want to move;\n\t\tSERVER_ID1-N - is and ID of the logical server you want to move an account to.\n");
    }

    /* renamed from: go */
    private void m12go(Document doc) throws Exception {
        Element root = doc.getDocumentElement();
        NodeList accounts = root.getElementsByTagName("account");
        for (int i = 0; i < accounts.getLength(); i++) {
            Element account = (Element) accounts.item(i);
            try {
                long accountId = Long.parseLong(account.getAttributes().getNamedItem("id").getNodeValue());
                long serverId = Long.parseLong(account.getAttributes().getNamedItem("serverId").getNodeValue());
                mo11go(accountId, serverId);
            } catch (NumberFormatException e) {
                System.out.println("Unparseable accountId " + account.getAttributes().getNamedItem("account").getNodeValue() + " or lserverId " + account.getAttributes().getNamedItem("lserverId").getNodeValue());
            }
        }
    }

    /* renamed from: go */
    public void m13go() throws Exception {
        if (this.doc != null) {
            if (checkInputParameters(this.doc)) {
                m12go(this.doc);
            }
        } else if (checkInputParameters(this.accId, this.lserverId)) {
            mo11go(this.accId, this.lserverId);
        }
    }

    private boolean checkInputParameters(Document doc) throws Exception {
        boolean z;
        boolean result = true;
        Element root = doc.getDocumentElement();
        NodeList accounts = root.getElementsByTagName("account");
        for (int i = 0; i < accounts.getLength(); i++) {
            Element account = (Element) accounts.item(i);
            try {
                long accountId = Long.parseLong(account.getAttributes().getNamedItem("id").getNodeValue());
                long serverId = Long.parseLong(account.getAttributes().getNamedItem("serverId").getNodeValue());
                z = result && checkInputParameters(accountId, serverId);
            } catch (NumberFormatException e) {
                System.out.println("Unparseable accountId " + account.getAttributes().getNamedItem("account").getNodeValue() + " or lserverId " + account.getAttributes().getNamedItem("lserverId").getNodeValue());
                z = false;
            }
            result = z;
        }
        return result;
    }

    private boolean checkInputParameters(long accId, long tServerId) throws Exception {
        Connection con = Session.getDb();
        try {
            try {
                System.out.println("Checking moveability of account with ID " + accId);
                PreparedStatement ps = con.prepareStatement("SELECT user_id FROM user_account WHERE account_id = ?");
                ps.setLong(1, accId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    System.out.println("\tCan not find user for the " + accId + " account. Please check passed account ID");
                    Session.closeStatement(ps);
                    con.close();
                    return false;
                }
                try {
                    User u = User.getUser(rs.getLong(1));
                    User oldUser = Session.getUser();
                    Account oldAcc = Session.getAccount();
                    try {
                        Session.setUser(u);
                        Account a = u.getAccount(new ResourceId(accId, 0));
                        Session.setAccount(a);
                        String createdBy = a.getPlan().getValue("_CREATED_BY_");
                        if (!"1".equals(createdBy) && !"2".equals(createdBy)) {
                            System.out.println("\tOnly unix or windows based accounts can be moved.");
                            Session.setUser(oldUser);
                            Session.setAccount(oldAcc);
                            Session.closeStatement(ps);
                            con.close();
                            return false;
                        }
                        try {
                            Collection c = a.getId().findAllChildren("ssl");
                            if (c != null && c.size() > 0) {
                                System.out.println("\tCan not move accounts which are having SSL turned on.");
                                System.out.println("\tTurn off SSL resources and then try again.");
                                Session.setUser(oldUser);
                                Session.setAccount(oldAcc);
                                Session.closeStatement(ps);
                                con.close();
                                return false;
                            }
                            ResourceId unixUserId = a.FM_getChild("unixuser");
                            if (unixUserId == null) {
                                System.out.println("\tCan not find unix user resource. Moving impossible.");
                                Session.setUser(oldUser);
                                Session.setAccount(oldAcc);
                                Session.closeStatement(ps);
                                con.close();
                                return false;
                            }
                            try {
                                HostDependentResource uu = (HostDependentResource) unixUserId.get();
                                try {
                                    HostEntry sServer = HostManager.getHost(uu.getHostId());
                                    try {
                                        HostEntry tServer = HostManager.getHost(tServerId);
                                        System.out.println("\tSource Server: " + sServer.getName() + "\n\tTarget Server: " + tServer.getName());
                                        if (sServer.getId() == tServer.getId()) {
                                            System.out.println("\tThe account with ID " + accId + " already on the server with ID " + tServerId);
                                            Session.setUser(oldUser);
                                            Session.setAccount(oldAcc);
                                            Session.closeStatement(ps);
                                            con.close();
                                            return false;
                                        } else if (sServer.getGroupType() != tServer.getGroupType()) {
                                            System.out.println("\tSource and target servers are having different types. Moving impossible");
                                            Session.setUser(oldUser);
                                            Session.setAccount(oldAcc);
                                            Session.closeStatement(ps);
                                            con.close();
                                            return false;
                                        } else {
                                            System.out.println("\t Moving is possible");
                                            Session.setUser(oldUser);
                                            Session.setAccount(oldAcc);
                                            Session.closeStatement(ps);
                                            con.close();
                                            return true;
                                        }
                                    } catch (Exception e) {
                                        System.out.println("\tUnable to get target server with ID " + tServerId + ". Please check server ID");
                                        Session.setUser(oldUser);
                                        Session.setAccount(oldAcc);
                                        Session.closeStatement(ps);
                                        con.close();
                                        return false;
                                    }
                                } catch (Exception e2) {
                                    System.out.println("\tUnable to get source server with ID " + uu.getHostId() + ".");
                                    Session.setUser(oldUser);
                                    Session.setAccount(oldAcc);
                                    Session.closeStatement(ps);
                                    con.close();
                                    return false;
                                }
                            } catch (Exception ex) {
                                System.out.println("\tUnable to get unix user resource. Moving impossible");
                                Session.getLog().error("\tUnable to get unix user resource: ", ex);
                                Session.setUser(oldUser);
                                Session.setAccount(oldAcc);
                                Session.closeStatement(ps);
                                con.close();
                                return false;
                            }
                        } catch (Exception ex2) {
                            System.out.println("\tAn error occured while checking avaliability of SSL resources.");
                            System.out.println("\tMore details can be found in H-Sphere logs.");
                            Session.getLog().error("An error occured while checking avaliability of SSL resources.", ex2);
                            Session.setUser(oldUser);
                            Session.setAccount(oldAcc);
                            Session.closeStatement(ps);
                            con.close();
                            return false;
                        }
                    } catch (Throwable th) {
                        Session.setUser(oldUser);
                        Session.setAccount(oldAcc);
                        throw th;
                    }
                } catch (Exception e3) {
                    System.out.println("Unable to get user for the " + accId + " account.");
                    Session.closeStatement(ps);
                    con.close();
                    return false;
                }
            } catch (Exception ex3) {
                System.out.println("Error while checking moveability of account with ID " + accId);
                Session.getLog().error("Error while checking moveability of account with ID " + accId, ex3);
                System.out.println("Stacktrace:");
                ex3.printStackTrace();
                Session.closeStatement(null);
                con.close();
                return false;
            }
        } catch (Throwable th2) {
            Session.closeStatement(null);
            con.close();
            throw th2;
        }
    }

    public static Hashtable parseInitParams(String[] argv) throws Exception {
        long lserverId = -1;
        long accId = -1;
        Hashtable result = new Hashtable();
        Document doc = null;
        if (argv.length == 0) {
            printHelpMessage();
            System.exit(0);
        }
        int i = 0;
        while (i < argv.length) {
            if ("-c".equals(argv[i]) || "--config".equals(argv[i])) {
                InputSource dataFile = new InputSource(new FileInputStream(argv[i + 1]));
                DOMParser parser = new DOMParser();
                parser.parse(dataFile);
                doc = parser.getDocument();
                i++;
            } else if ("-lid".equals(argv[i]) || "--lserverId".equals(argv[i])) {
                try {
                    lserverId = Long.parseLong(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.out.println("Unparseable logical server id specified");
                    System.exit(-1);
                }
            } else if ("-acc".equals(argv[i]) || "--accId".equals(argv[i])) {
                try {
                    accId = Long.parseLong(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e2) {
                    System.out.println("Unparseable  account id specified");
                    System.exit(-1);
                }
            } else if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                printHelpMessage();
                System.exit(0);
            }
            i++;
        }
        if (doc != null) {
            result.put("doc", doc);
            return result;
        } else if (accId != -1 && lserverId != -1) {
            result.put("account", new Long(accId));
            result.put("server", new Long(lserverId));
            return result;
        } else {
            System.out.println("Wrong init parameters are supplied.");
            printHelpMessage();
            System.exit(-1);
            return null;
        }
    }

    public ToolLogger getLogger() {
        return this.logger;
    }

    public void setLogger(ToolLogger logger) {
        this.logger = logger;
    }
}
