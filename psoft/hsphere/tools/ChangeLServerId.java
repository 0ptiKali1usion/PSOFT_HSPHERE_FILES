package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ChangeLServerId.class */
public class ChangeLServerId extends C0004CP {
    protected long from;

    /* renamed from: to */
    protected long f220to;
    protected long accountId;
    protected boolean isHelp = false;

    protected boolean readParameters(String[] args) throws Exception {
        this.from = -1L;
        this.f220to = -1L;
        this.accountId = -1L;
        int i = 0;
        while (i < args.length) {
            if ("-a".equals(args[i]) || "--account".equals(args[i])) {
                this.accountId = Long.parseLong(args[i + 1]);
                i++;
            } else if ("-f".equals(args[i]) || "--from".equals(args[i])) {
                this.from = Long.parseLong(args[i + 1]);
                i++;
            } else if ("-t".equals(args[i]) || "--to".equals(args[i])) {
                this.f220to = Long.parseLong(args[i + 1]);
                i++;
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                this.isHelp = true;
                return false;
            }
            i++;
        }
        if (this.accountId == -1 || this.from == -1 || this.f220to == -1) {
            return false;
        }
        return true;
    }

    /* renamed from: go */
    protected boolean m18go() throws Exception {
        Connection con = Session.getDb();
        try {
            int typeId = -1;
            ResultSet rs = selectTable("SELECT g.type_id FROM l_server s, l_server_groups g WHERE s.group_id=g.id AND (s.id = ? OR s.id = ?)", new Object[]{new Long(this.f220to), new Long(this.from)}, con);
            int count = 0;
            while (rs.next()) {
                if (typeId == -1) {
                    typeId = rs.getInt(1);
                } else if (typeId != rs.getLong(1)) {
                    System.out.println("You cannot change server id from " + this.from + " to " + this.f220to + " (they are from different groups)");
                    con.close();
                    return false;
                }
                count++;
            }
            if (count != 2) {
                System.out.println("You cannot change server id from " + this.from + " to " + this.f220to + " (some of them doesn't exists or servers are identical)");
                con.close();
                return false;
            }
            switch (typeId) {
                case 1:
                    updateTable("UPDATE unix_user SET hostid = ? WHERE hostid = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 7 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    updateTable("UPDATE apache_vhost SET host_id = ? WHERE host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 9 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 2:
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
                    System.out.println("Type of these logical servers doesn't support by this utility");
                    con.close();
                    return false;
                case 3:
                    updateTable("UPDATE mail_services SET mail_server = ? WHERE mail_server = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 1000 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 4:
                    updateTable("UPDATE mysqlres SET mysql_host_id = ? WHERE mysql_host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 6000 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 5:
                    updateTable("UPDATE unix_user SET hostid = ? WHERE hostid = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 7 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    updateTable("UPDATE iis_vhost SET host_id = ? WHERE host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 9 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    updateTable("UPDATE iis_ftp_vhost SET host_id = ? WHERE host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 2002 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 6:
                case 7:
                    updateTable("UPDATE real_server SET host_id = ? WHERE host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 6101 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 15:
                    updateTable("UPDATE mssqlres SET mssql_host_id = ? WHERE mssql_host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 6800 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
                case 18:
                    updateTable("UPDATE pgsqlres SET pgsql_host_id = ? WHERE pgsql_host_id = ? AND id IN (SELECT child_id from parent_child WHERE child_type = 6900 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                    break;
            }
            try {
                long urchinServerId = Long.parseLong(Session.getProperty("URCHIN_SERVER_ID"));
                if (this.from != urchinServerId && this.f220to == urchinServerId && selectTable("SELECT id FROM urchin4 WHERE server_id = ? AND id IN (SELECT child_id FROM parent_child WHERE child_type = 65 AND account_id = ?)", new Object[]{new Long(this.from), new Long(this.accountId)}, con).next()) {
                    updateTable("UPDATE urchin4 SET server_id = ? WHERE server_id = ? AND id IN (SELECT child_id FROM parent_child WHERE child_type = 65 AND account_id = ?)", new Object[]{new Long(this.f220to), new Long(this.from), new Long(this.accountId)}, con);
                }
            } catch (Exception e) {
            }
            return true;
        } finally {
            con.close();
        }
    }

    protected int updateTable(String update, Object[] params, Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(update);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        System.out.println(ps.toString());
        int result = ps.executeUpdate();
        System.out.println("UPDATE " + result);
        return result;
    }

    protected ResultSet selectTable(String update, Object[] params, Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(update);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery();
    }

    public void printHelpMessage() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.ChangeLServerId\n\t\t- Changing logical server id in H-Sphere database");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.ChangeLServerId [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t-a|--account ACCOUNT_ID -f|--from LOGICAL_SERVER_ID_1 -t|--to LOGICAL_SERVER_ID_2\n\t\twhere\n\t\tACCOUNT_ID - id of the account you want to change;\n\t\tLOGICAL_SERVER_ID_1 - id of the logical server you want to change from;\n\t\tLOGICAL_SERVER_ID_2 - id of the logical server you want to change to");
        System.out.println("SAMPLE:");
        System.out.println("\tjava psoft.hsphere.tools.ChangeLServerId -a 1000 -f 1 -t 2");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Initializing...");
        ChangeLServerId test = new ChangeLServerId();
        if (test.readParameters(args)) {
            if (test.m18go()) {
                System.out.println("Done");
            } else {
                System.out.println("ERROR");
            }
        } else if (test.isHelp) {
            test.printHelpMessage();
        } else {
            System.out.println("Wrong parameters");
            test.printHelpMessage();
        }
        System.exit(0);
    }
}
