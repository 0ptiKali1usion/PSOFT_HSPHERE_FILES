package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.apache.LogResource;
import psoft.hsphere.resource.apache.TransferLogResource;

/* loaded from: hsphere.zip:psoft/hsphere/tools/OffLogs.class */
public class OffLogs extends C0004CP {
    boolean[] logs;
    public static final int ERRORLOG = 0;
    public static final int AGENTLOG = 1;
    public static final int REFERRERLOG = 2;
    public static final int TRANSFERRLOG = 3;
    public static final int WEBALIZER = 4;
    public static final int MODLOGAN = 5;
    public static final int AWSTATS = 6;
    protected ArrayList accounts;
    protected ArrayList servers;

    public OffLogs() throws Exception {
        super("psoft_config.hsphere");
        this.logs = new boolean[7];
        this.accounts = null;
        this.servers = null;
    }

    protected boolean readParameters(String[] args) throws Exception {
        Arrays.fill(this.logs, false);
        boolean is_log_chosen = false;
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
            } else if ("-e".equals(args[i]) || "--errorlog".equals(args[i])) {
                this.logs[0] = true;
                is_log_chosen = true;
            } else if ("-ag".equals(args[i]) || "--agentlog".equals(args[i])) {
                this.logs[1] = true;
                is_log_chosen = true;
            } else if ("-r".equals(args[i]) || "--referrerlog".equals(args[i])) {
                this.logs[2] = true;
                is_log_chosen = true;
            } else if ("-t".equals(args[i]) || "--transferlog".equals(args[i])) {
                this.logs[3] = true;
                is_log_chosen = true;
            } else if ("-w".equals(args[i]) || "--webalizer".equals(args[i])) {
                this.logs[4] = true;
                this.logs[3] = true;
                is_log_chosen = true;
            } else if ("-m".equals(args[i]) || "--modlogan".equals(args[i])) {
                this.logs[5] = true;
                this.logs[3] = true;
                is_log_chosen = true;
            } else if ("-aw".equals(args[i]) || "--awstats".equals(args[i])) {
                this.logs[6] = true;
                this.logs[3] = true;
                is_log_chosen = true;
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                return false;
            }
            i++;
        }
        if (!is_log_chosen) {
            Arrays.fill(this.logs, true);
            return true;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        OffLogs Offlogs = new OffLogs();
        if (Offlogs.readParameters(args)) {
            try {
                Offlogs.m10go();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            System.out.println("Convertion finished.");
            return;
        }
        System.out.println("NAME:\n\t psoft.hsphere.tools.OffLogs\n\t\t- Regenerate users' logs and stats config");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.OffLogs [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t-a|--accounts list of account IDs, or all for 'all' accounts, ',' - delimiter");
        System.out.println("\t-s|--servers list of logical server IDs, or 'all' for all servers, ',' - delimiter");
        System.out.println("\t-e|--errorlog re-generate errorlog only");
        System.out.println("\t-ag|--agentlog re-generate agentlog only");
        System.out.println("\t-r|--referrerlog re-generate referrerlog only");
        System.out.println("\t-t|--transferlog re-generate transferlog only");
        System.out.println("\t-w|--webalizer re-generate webalizer only");
        System.out.println("\t-m|--modlogan re-generate modlogan only");
        System.out.println("\t-aw|--awstats re-generate awstats only");
        System.out.println("SAMPLE:");
        System.out.println("\tjava psoft.hsphere.tools.OffLogs -a '1002,8383,1237' -s '12,35,37'");
        System.out.println("\tjava psoft.hsphere.tools.OffLogs -a all -s all");
        System.out.println("\tjava psoft.hsphere.tools.OffLogs -s 24 -aw -w");
    }

    /* renamed from: go */
    protected void m10go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String query = "";
        if (this.accounts != null) {
            String list = "";
            Iterator i = this.accounts.iterator();
            while (i.hasNext()) {
                if (list.length() > 0) {
                    list = list + ",";
                }
                list = list + i.next();
            }
            query = " AND p.account_id IN (" + list + ")";
        }
        if (this.servers != null) {
            String list2 = "";
            Iterator i2 = this.servers.iterator();
            while (i2.hasNext()) {
                if (list2.length() > 0) {
                    list2 = list2 + ",";
                }
                list2 = list2 + i2.next();
            }
            query = query + " AND u.hostid IN (" + list2 + ")";
            System.out.println("Working on logical servers with id " + list2);
        } else {
            System.out.println("Working on all logical servers");
        }
        try {
            ps = con.prepareStatement("SELECT ua.user_id FROM user_account ua, parent_child p, unix_user u WHERE u.id = p.child_id AND ua.account_id = p.account_id " + query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    User u = User.getUser(rs.getLong(1));
                    Session.setUser(u);
                    Iterator aci = u.getAccountIds().iterator();
                    while (aci.hasNext()) {
                        Account newAccount = u.getAccount((ResourceId) aci.next());
                        Session.setAccount(newAccount);
                        processAccount(newAccount);
                    }
                } catch (Exception ex) {
                    System.out.println("Error processing");
                    ex.printStackTrace();
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

    protected void processAccount(Account a) throws Exception {
        ResourceId unixuser = a.getId().findChild("unixuser");
        System.out.println("Working on account " + a.getId());
        if (unixuser == null) {
            return;
        }
        try {
            if (this.servers != null) {
                if (!this.servers.contains(unixuser.get("host_id").toString())) {
                    return;
                }
            }
            Collection<ResourceId> col = a.getId().findAllChildren("hosting");
            for (ResourceId hostId : col) {
                Resource host = hostId.get();
                if (host != null) {
                    if (this.logs[0]) {
                        ErrorLog(hostId);
                    }
                    if (this.logs[1]) {
                        AgentLog(hostId);
                    }
                    if (this.logs[2]) {
                        RefferrerLog(hostId);
                    }
                    if (this.logs[3] && TransferLog(hostId)) {
                        if (this.logs[4]) {
                            Webalizer(hostId);
                        }
                        if (this.logs[5]) {
                            Modlogan(hostId);
                        }
                        if (this.logs[6]) {
                            Awstats(hostId);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Session.getLog().error("Unable to get host Id", ex);
        }
    }

    protected void ErrorLog(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("errorlog") != null) {
                Resource errorlog = hostId.findChild("errorlog").get();
                if (errorlog instanceof LogResource) {
                    System.out.print("Reinitializing errorlog ");
                    ((HostDependentResource) errorlog).physicalDelete(errorlog.recursiveGet("host").getId());
                    ((HostDependentResource) errorlog).physicalCreate(errorlog.recursiveGet("host").getId());
                    System.out.println("   [   OK   ]");
                }
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get errorlog", ex);
        }
    }

    protected void RefferrerLog(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("referrerlog") != null) {
                Resource referrerlog = hostId.findChild("referrerlog").get();
                if (referrerlog instanceof LogResource) {
                    System.out.print("Reinitializing referrerlog ");
                    ((HostDependentResource) referrerlog).physicalDelete(referrerlog.recursiveGet("host").getId());
                    ((HostDependentResource) referrerlog).physicalCreate(referrerlog.recursiveGet("host").getId());
                    System.out.println("   [   OK   ]");
                }
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get referrerlog", ex);
        }
    }

    protected void AgentLog(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("agentlog") != null) {
                Resource agentlog = hostId.findChild("agentlog").get();
                if (agentlog instanceof LogResource) {
                    System.out.print("Reinitializing agentlog ");
                    ((HostDependentResource) agentlog).physicalDelete(agentlog.recursiveGet("host").getId());
                    ((HostDependentResource) agentlog).physicalCreate(agentlog.recursiveGet("host").getId());
                    System.out.println("   [   OK   ]");
                }
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get agentlog", ex);
        }
    }

    protected boolean TransferLog(ResourceId hostId) throws Exception {
        boolean result = true;
        try {
            if (hostId.findChild("transferlog") != null) {
                Resource transferlog = hostId.findChild("transferlog").get();
                if (transferlog instanceof TransferLogResource) {
                    System.out.println("Reinitializing transferlog ");
                    ((HostDependentResource) transferlog).physicalDelete(transferlog.recursiveGet("host").getId());
                    ((HostDependentResource) transferlog).physicalCreate(transferlog.recursiveGet("host").getId());
                    System.out.println("   [   OK   ]");
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get transfertlog", ex);
        }
        return result;
    }

    protected void Webalizer(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("webalizer") != null) {
                System.out.print("     Reinitializing webalizer ");
                Resource webalizer = hostId.findChild("webalizer").get();
                ((HostDependentResource) webalizer).physicalDelete(webalizer.recursiveGet("host").getId());
                ((HostDependentResource) webalizer).physicalCreate(webalizer.recursiveGet("host").getId());
                System.out.println("   [   OK   ]");
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get webalizer", ex);
        }
    }

    protected void Modlogan(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("modlogan") != null) {
                System.out.print("     Reinitializing modlogan ");
                Resource modlogan = hostId.findChild("modlogan").get();
                ((HostDependentResource) modlogan).physicalDelete(modlogan.recursiveGet("host").getId());
                ((HostDependentResource) modlogan).physicalCreate(modlogan.recursiveGet("host").getId());
                System.out.println("   [   OK   ]");
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get modlogan", ex);
        }
    }

    protected void Awstats(ResourceId hostId) throws Exception {
        try {
            if (hostId.findChild("awstats") != null) {
                System.out.print("     Reinitializing awstats ");
                Resource awstats = hostId.findChild("awstats").get();
                ((HostDependentResource) awstats).physicalDelete(awstats.recursiveGet("host").getId());
                ((HostDependentResource) awstats).physicalCreate(awstats.recursiveGet("host").getId());
                System.out.println("   [   OK   ]");
            }
        } catch (Exception ex) {
            System.out.println("   [   FAIL   ]");
            Session.getLog().error("Unable to get awstats", ex);
        }
    }
}
