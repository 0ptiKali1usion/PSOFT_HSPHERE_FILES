package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ResellerSSLZoneUpdate.class */
public class ResellerSSLZoneUpdate extends C0004CP {
    public ResellerSSLZoneUpdate() {
        super("psoft_config.hsphere");
    }

    public static void main(String[] args) throws Exception {
        try {
            ResellerSSLZoneUpdate ru = new ResellerSSLZoneUpdate();
            ru.processUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    private void processUpdate() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("update e_dns_records set type_rec=4 where id in (select r_id from l_server_ips where flag = 8)");
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
