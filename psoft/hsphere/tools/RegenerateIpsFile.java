package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.PhysicalServer;

/* loaded from: hsphere.zip:psoft/hsphere/tools/RegenerateIpsFile.class */
public class RegenerateIpsFile extends C0004CP {
    public RegenerateIpsFile() {
        super("psoft_config.hsphere");
    }

    public static void main(String[] args) throws Exception {
        RegenerateIpsFile rif = new RegenerateIpsFile();
        if (args.length > 0 && "-pid".equals(args[0])) {
            rif.m8go(args[1]);
        }
        if (args.length > 0 && "-all".equals(args[0])) {
            rif.m8go("");
        }
        System.out.println("NAME:\n\t psoft.hsphere.tools.RegenerateIpsFile\n\t\t- Regenerate file /hsphere/local/network/ips on Unix physical box");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.RegenerateIpsFile [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t-all regenerate on all physical boxes");
        System.out.println("\t-pid regenerate on physical server with specific id");
    }

    /* renamed from: go */
    private void m8go(String psId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String query = "";
        if (!"".equals(psId)) {
            query = " AND ps.id = " + psId;
        }
        try {
            try {
                ps = con.prepareStatement("SELECT distinct ps.id, lsi.ip, lsi.mask FROM l_server_ips lsi JOIN l_server ls ON (lsi.l_server_id = ls.id) JOIN p_server ps ON (ls.p_server_id = ps.id) WHERE ( flag IN (?, ?, ?, ?, ?, ?) OR (flag between 10 AND 100) ) AND ls.group_id <> 12 AND ps.os_type = 1 " + query + " ORDER BY ps.id");
                ps.setInt(1, 1);
                ps.setInt(2, 2);
                ps.setInt(3, 4);
                ps.setInt(4, 5);
                ps.setInt(5, 6);
                ps.setInt(6, 8);
                ResultSet rs = ps.executeQuery();
                long curPsId = 0;
                List ipsForPsId = new ArrayList();
                StringBuffer buf = new StringBuffer("");
                while (rs.next()) {
                    long rowPsId = rs.getLong("id");
                    if (curPsId == 0) {
                        curPsId = rowPsId;
                    } else if (curPsId != rowPsId) {
                        try {
                            regenerateIpsOnBox(curPsId, buf.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        buf = new StringBuffer("");
                        ipsForPsId.clear();
                        curPsId = rowPsId;
                    }
                    String rowIP = rs.getString("ip");
                    if (!ipsForPsId.contains(rowIP)) {
                        buf.append(rowIP + "\t" + rs.getString("mask") + "\n");
                        ipsForPsId.add(rowIP);
                    }
                }
                try {
                    regenerateIpsOnBox(curPsId, buf.toString());
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
            System.out.println("Done");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void regenerateIpsOnBox(long psId, String content) throws Exception {
        PhysicalServer pserver = PhysicalServer.getPServer(psId);
        System.out.println("Re-generating file ips on physical server id " + psId);
        pserver.exec("io2file", new String[]{"/hsphere/local/network/ips"}, content);
        pserver.exec("setup-ips.pl", new String[]{""}, null);
    }
}
