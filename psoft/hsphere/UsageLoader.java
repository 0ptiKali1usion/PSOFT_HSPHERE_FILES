package psoft.hsphere;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.email.MailDomain;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/UsageLoader.class */
public class UsageLoader extends C0004CP {
    private static final int DOMAINTR = 1;

    public UsageLoader() {
        super("psoft_config.hsphere");
    }

    /* loaded from: hsphere.zip:psoft/hsphere/UsageLoader$Info.class */
    public class Info {
        private HashMap info;
        private int ttType;

        public Info(int type) {
            UsageLoader.this = r5;
            this.info = null;
            this.info = new HashMap();
            init(type);
            this.ttType = type;
        }

        public int getSize() {
            if (this.info != null) {
                return this.info.size();
            }
            return 0;
        }

        public Hashtable get(String name) {
            Hashtable res = new Hashtable();
            Ids ids = (Ids) this.info.get(name);
            if (ids != null) {
                res.put("Name", name);
                res.put("resource_id", new Long(ids.resId));
                res.put("account_id", new Long(ids.accId));
                return res;
            }
            return null;
        }

        public Hashtable get(String name, long serverId) {
            String serverID = new Long(serverId).toString();
            return get(name + "_" + serverID);
        }

        public void init(int usageType) {
            PreparedStatement ps = null;
            this.info.clear();
            try {
                Connection con = Session.getDb();
                try {
                    switch (usageType) {
                        case 1:
                        case 5:
                            ps = con.prepareStatement("SELECT login, unix_user.id, account_id, l_server.p_server_id  FROM unix_user, parent_child, l_server  WHERE unix_user.id = parent_child.child_id  AND unix_user.hostid = l_server.id");
                            ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                String name = rs.getString(1) + "_" + new Long(rs.getLong(4)).toString();
                                this.info.put(name, new Ids(rs.getLong(2), rs.getLong(3)));
                            }
                            break;
                        case 3:
                            ps = con.prepareStatement("SELECT mb.full_email, mb.id, pc1.account_id, l_server.p_server_id FROM mailboxes mb, parent_child pc1, mail_services ms, l_server WHERE mb.id = pc1.child_id AND ms.id = pc1.parent_id AND ms.mail_server = l_server.id");
                            ResultSet rs2 = ps.executeQuery();
                            while (rs2.next()) {
                                String name2 = rs2.getString(1) + "_" + new Long(rs2.getLong(4)).toString();
                                this.info.put(name2, new Ids(rs2.getLong(2), rs2.getLong(3)));
                            }
                            break;
                        case 4:
                            ps = con.prepareStatement("SELECT db.db_name, db.id, pc1.account_id, l_server.p_server_id FROM mysqldb db, parent_child pc1, mysqlres msr, l_server WHERE db.id = pc1.child_id AND msr.id = pc1.parent_id AND msr.mysql_host_id = l_server.id");
                            ResultSet rs3 = ps.executeQuery();
                            while (rs3.next()) {
                                String name3 = rs3.getString(1) + "_" + new Long(rs3.getLong(4)).toString();
                                this.info.put(name3, new Ids(rs3.getLong(2), rs3.getLong(3)));
                            }
                            break;
                        case 12:
                            ps = con.prepareStatement("SELECT vps.name, vps.id, account_id, l_server.p_server_id FROM vps, parent_child, l_server WHERE vps.id = parent_child.child_id AND hostid = l_server.id");
                            ResultSet rs4 = ps.executeQuery();
                            while (rs4.next()) {
                                String name4 = rs4.getString(1) + "_" + new Long(rs4.getLong(4)).toString();
                                this.info.put(name4, new Ids(rs4.getLong(2), rs4.getLong(3)));
                            }
                            break;
                        case 15:
                            ps = con.prepareStatement("SELECT db.mssql_db_name, db.id, pc1.account_id, l_server.p_server_id FROM mssql_dbs db, parent_child pc1, parent_child pc2, mssqlres msr, l_server WHERE db.id = pc1.child_id AND pc2.child_id = pc1.parent_id AND msr.id = pc2.parent_id AND msr.mssql_host_id = l_server.id");
                            ResultSet rs5 = ps.executeQuery();
                            while (rs5.next()) {
                                String name5 = rs5.getString(1) + "_" + new Long(rs5.getLong(4)).toString();
                                this.info.put(name5, new Ids(rs5.getLong(2), rs5.getLong(3)));
                            }
                            break;
                        case 18:
                            ps = con.prepareStatement("SELECT db.db_name, db.id, pc1.account_id, l_server.p_server_id FROM pgsqldb db, parent_child pc1,  pgsqlres msr, l_server WHERE db.id = pc1.child_id AND msr.id = pc1.parent_id AND msr.pgsql_host_id = l_server.id");
                            ResultSet rs6 = ps.executeQuery();
                            while (rs6.next()) {
                                String name6 = rs6.getString(1) + "_" + new Long(rs6.getLong(4)).toString();
                                this.info.put(name6, new Ids(rs6.getLong(2), rs6.getLong(3)));
                            }
                            break;
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception ex) {
                    this.info.clear();
                    Session.getLog().error("Usage calculation error:  Can't get info for usage type:  " + new Integer(usageType).toString(), ex);
                    Session.closeStatement(null);
                    con.close();
                }
            } catch (Exception ex2) {
                Session.getLog().error("Usaage calculation error:  Can't open database connection" + new Integer(usageType).toString(), ex2);
            }
        }

        /* loaded from: hsphere.zip:psoft/hsphere/UsageLoader$Info$Ids.class */
        public class Ids {
            public long resId;
            public long accId;

            public Ids(long resourceId, long accountId) {
                Info.this = r5;
                this.resId = resourceId;
                this.accId = accountId;
            }
        }
    }

    private Long getPserverId(HostEntry host) {
        try {
            return new Long(host.getPServer().getId());
        } catch (Exception ex) {
            Session.getLog().debug("Error getting pServer ID: ", ex);
            return new Long(0L);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v16, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r3v19, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r3v5, types: [java.lang.String[], java.lang.String[][]] */
    public void load() {
        List processedServers = new ArrayList();
        List<HostEntry> hosts = HostManager.getHostsByGroupType(1);
        processedServers.clear();
        for (HostEntry host : hosts) {
            Long pserver = getPserverId(host);
            if (!processedServers.contains(pserver)) {
                try {
                    load(host.exec("usage-cat", Arrays.asList("disk")), pserver.longValue(), 1);
                } catch (Exception e) {
                    Session.getLog().debug("Error loading disk usage from host" + host.getName(), e);
                }
                processedServers.add(pserver);
            }
        }
        processedServers.clear();
        List<HostEntry> hosts2 = HostManager.getHostsByGroupType(3);
        for (HostEntry host2 : hosts2) {
            Long pserver2 = getPserverId(host2);
            if (!processedServers.contains(pserver2)) {
                try {
                    load(host2.exec("usage-cat", Arrays.asList("mail")), pserver2.longValue(), 3);
                } catch (Exception e2) {
                    Session.getLog().debug("Error loading disk usage from host" + host2.getName(), e2);
                }
                processedServers.add(pserver2);
            }
        }
        processedServers.clear();
        List<HostEntry> hosts3 = HostManager.getHostsByGroupType(6);
        for (HostEntry host3 : hosts3) {
            Long pserver3 = getPserverId(host3);
            if (!processedServers.contains(pserver3)) {
                try {
                    load(host3.exec("usage-cat", Arrays.asList("disk")), pserver3.longValue(), 1);
                } catch (Exception e3) {
                    Session.getLog().debug("Error loading disk usage from host" + host3.getName(), e3);
                }
                processedServers.add(pserver3);
            }
        }
        processedServers.clear();
        List<WinHostEntry> hosts4 = HostManager.getHostsByGroupType(5);
        for (WinHostEntry host4 : hosts4) {
            Long pserver4 = getPserverId(host4);
            if (!processedServers.contains(pserver4)) {
                try {
                    load(host4.exec("get-usage.asp", (String[][]) new String[]{new String[]{"type", "disk"}}), pserver4.longValue(), 5);
                } catch (Exception e4) {
                    Session.getLog().debug("Error loading disk usage from host" + host4.getName(), e4);
                }
                processedServers.add(pserver4);
            }
        }
        processedServers.clear();
        List<WinHostEntry> hosts5 = HostManager.getHostsByGroupType(7);
        for (WinHostEntry host5 : hosts5) {
            Long pserver5 = getPserverId(host5);
            if (!processedServers.contains(pserver5)) {
                try {
                    load(host5.exec("get-usage.asp", (String[][]) new String[]{new String[]{"type", "disk"}}), pserver5.longValue(), 5);
                } catch (Exception e5) {
                    Session.getLog().debug("Error loading disk usage from host" + host5.getName(), e5);
                }
                processedServers.add(pserver5);
            }
        }
        processedServers.clear();
        List<HostEntry> hosts6 = HostManager.getHostsByGroupType(4);
        for (HostEntry host6 : hosts6) {
            Long pserver6 = getPserverId(host6);
            if (!processedServers.contains(pserver6)) {
                try {
                    load(host6.exec("usage-cat", Arrays.asList("mysql")), pserver6.longValue(), 4);
                } catch (Exception e6) {
                    Session.getLog().debug("Error loading disk usage from host" + host6.getName(), e6);
                }
                processedServers.add(pserver6);
            }
        }
        processedServers.clear();
        List<HostEntry> hosts7 = HostManager.getHostsByGroupType(18);
        for (HostEntry host7 : hosts7) {
            Long pserver7 = getPserverId(host7);
            if (!processedServers.contains(pserver7)) {
                try {
                    load(host7.exec("usage-cat", Arrays.asList("pgsql")), pserver7.longValue(), 18);
                } catch (Exception e7) {
                    Session.getLog().debug("Error loading disk usage from host" + host7.getName(), e7);
                }
                processedServers.add(pserver7);
            }
        }
        processedServers.clear();
        List<WinHostEntry> hosts8 = HostManager.getHostsByGroupType(15);
        for (WinHostEntry host8 : hosts8) {
            Long pserver8 = getPserverId(host8);
            if (!processedServers.contains(pserver8)) {
                try {
                    load(host8.exec("get-usage.asp", (String[][]) new String[]{new String[]{"type", "mssql"}}), pserver8.longValue(), 15);
                } catch (Exception e8) {
                    Session.getLog().debug("Error loading disk usage from host" + host8.getName(), e8);
                }
                processedServers.add(pserver8);
            }
        }
        Session.getLog().debug("VPS DISK USAGE LOADING....");
        processedServers.clear();
        List<HostEntry> hosts9 = HostManager.getHostsByGroupType(12);
        for (HostEntry host9 : hosts9) {
            Long pserver9 = getPserverId(host9);
            if (!processedServers.contains(pserver9)) {
                try {
                    load(host9.exec("usage-cat", Arrays.asList("vps")), pserver9.longValue(), 12);
                } catch (Exception e9) {
                    Session.getLog().debug("Error loading disk usage from host" + host9.getName(), e9);
                }
                processedServers.add(pserver9);
            }
        }
    }

    protected void load(Collection usageLog, long pServerId, int type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Date now = new Date(TimeUtils.dropMinutes(TimeUtils.getDate()).getTime());
        try {
            ps = con.prepareStatement("INSERT INTO usage_log(p_server_id, usage_type, cdate, name, used) VALUES(?, ?, ?, ?, ?)");
            deleteFromDb(now, pServerId, type);
            Iterator i = usageLog.iterator();
            while (i.hasNext()) {
                String str = (String) i.next();
                try {
                    UsageLogString ur = new UsageLogString(str);
                    if (ur.getUsage() > 0.0f) {
                        ps.setLong(1, pServerId);
                        ps.setInt(2, type);
                        ps.setDate(3, now);
                        ps.setString(4, ur.getName());
                        ps.setFloat(5, ur.getUsage());
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    Session.getLog().warn("Bad usage line, " + str, e);
                }
            }
            con.commit();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void parse() throws SQLException {
        updateLog(1);
        updateLog(3);
        updateLog(4);
        updateLog(18);
        updateLog(15);
        updateLog(12);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM usage_log WHERE account_id IS NULL OR account_id = 0");
            int count = ps.executeUpdate();
            if (count > 0) {
                Session.getLog().info(count + " no resolved records were deleted ");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void updateLog(int usageType) throws SQLException {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        long counter = 0;
        Info usageInfo = new Info(usageType);
        Connection con = Session.getTransConnection();
        try {
            ps1 = con.prepareStatement("UPDATE usage_log SET account_id = ?, resource_id = ? WHERE account_id IS NULL AND name = ? AND p_server_id = ? AND usage_type = ?");
            ps2 = con.prepareStatement("DELETE FROM usage_log WHERE account_id IS NULL OR account_id = 0");
            switch (usageType) {
                case 1:
                case 5:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, usage_type FROM usage_log WHERE account_id IS NULL AND (usage_type=? OR usage_type=?)");
                    ps.setInt(1, 1);
                    ps.setInt(2, 5);
                    break;
                case 3:
                case 4:
                case 12:
                case 15:
                case 18:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, usage_type FROM usage_log WHERE account_id IS NULL AND usage_type=?");
                    ps.setInt(1, usageType);
                    break;
            }
            ResultSet rs = ps.executeQuery();
            int uCount = 0;
            while (rs.next()) {
                Hashtable hs = null;
                String name = rs.getString(1);
                long pServerId = rs.getLong(2);
                int ttype = rs.getInt(3);
                switch (ttype) {
                    case 1:
                    case 5:
                        hs = usageInfo.get(name, pServerId);
                        break;
                    case 3:
                        hs = usageInfo.get(MailDomain.postmaster + "@" + name, pServerId);
                        break;
                    case 4:
                    case 12:
                    case 15:
                    case 18:
                        hs = usageInfo.get(name, pServerId);
                        break;
                }
                long account_id = 0;
                long rid = 0;
                if (hs != null) {
                    try {
                        account_id = ((Long) hs.get("account_id")).longValue();
                        rid = ((Long) hs.get("resource_id")).longValue();
                        Session.getLog().debug("Type:" + ttype + " Name:" + name + " account_id:" + account_id + " resource_id:" + rid);
                    } catch (Exception e) {
                        Session.getLog().error("Usage problem on:" + name, e);
                    }
                } else {
                    Session.getLog().error("Didn`t find name:" + name);
                }
                ps1.setLong(1, account_id);
                ps1.setLong(2, rid);
                ps1.setString(3, name);
                ps1.setLong(4, pServerId);
                ps1.setInt(5, ttype);
                ps1.executeUpdate();
                long j = counter + 1;
                counter = j;
                if (j % 100 == 0) {
                    con.commit();
                    Session.getLog().debug("Commiting another 100 record");
                }
                uCount++;
            }
            con.commit();
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    protected void deleteFromDb(Date dateLogs, long pServerId, int usageType) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM usage_log WHERE p_server_id = ? AND cdate = ? AND usage_type = ?");
            ps.setLong(1, pServerId);
            ps.setDate(2, dateLogs);
            ps.setInt(3, usageType);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/UsageLoader$UsageLogString.class */
    public class UsageLogString {
        String domainName;
        float usage;

        public UsageLogString(String str) throws Exception {
            UsageLoader.this = r6;
            try {
                StringTokenizer reTrafficLog = new StringTokenizer(str, "|");
                if (reTrafficLog.hasMoreTokens()) {
                    this.domainName = reTrafficLog.nextToken().toString();
                    if (reTrafficLog.hasMoreTokens()) {
                        this.usage = Float.parseFloat(reTrafficLog.nextToken().toString());
                        return;
                    }
                    throw new Exception("Wrong parsing usage:" + str);
                }
                throw new Exception("Wrong parsing Name:" + str);
            } catch (NumberFormatException e) {
                throw new Exception("Wrong parsing");
            }
        }

        public String getName() {
            return this.domainName;
        }

        public float getUsage() {
            return this.usage;
        }
    }

    public static void main(String[] args) throws Exception {
        UsageLoader usage = new UsageLoader();
        Session.getLog().info("Starting Usage Loader");
        long startDate = TimeUtils.currentTimeMillis();
        Connection con = Session.getTransConnection();
        try {
            try {
                usage.load();
                Session.commitTransConnection(con);
            } catch (Exception e) {
                Session.getLog().error("Problem loading", e);
                Session.commitTransConnection(con);
            }
            Session.getLog().info("Usage loader. starting usage parsing");
            try {
                usage.parse();
            } catch (Exception e2) {
                Session.getLog().warn("Exception during usage parse", e2);
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate;
            Session.getLog().info("Usage Loader FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            System.exit(0);
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            throw th;
        }
    }
}
