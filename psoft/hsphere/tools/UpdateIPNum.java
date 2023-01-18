package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.hsphere.resource.C0015IP;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/UpdateIPNum.class */
public class UpdateIPNum {
    protected boolean process;
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    protected Database f239db = Toolbox.getDB(this.config);

    public UpdateIPNum(boolean process) throws Exception {
        this.process = false;
        this.process = process;
    }

    public void execute() throws Exception {
        Connection con = this.f239db.getConnection();
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                ps = con.prepareStatement("SELECT l_server_id, ip, ip_num FROM l_server_ips");
                ps2 = con.prepareStatement("UPDATE l_server_ips SET ip_num  = ? WHERE l_server_id = ? and ip = ?");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (this.process) {
                        ps2.setLong(1, C0015IP.toLong(rs.getString(2)));
                        ps2.setLong(2, rs.getLong(1));
                        ps2.setString(3, rs.getString(2));
                        if (rs.getLong(3) != C0015IP.toLong(rs.getString(2))) {
                            ps2.executeUpdate();
                        }
                    } else if (rs.getLong(3) != C0015IP.toLong(rs.getString(2))) {
                        System.out.println("L_SERVER_ID:" + rs.getLong(1) + "IP:" + rs.getString(2) + " IP_NUM:" + C0015IP.toLong(rs.getString(2)));
                    }
                }
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                con.close();
            } catch (SQLException e) {
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0 && ("show".equals(args[0]) || "process".equals(args[0]))) {
                UpdateIPNum packDB = new UpdateIPNum("process".equals(args[0]));
                packDB.execute();
                System.out.println("FINISHED");
                System.exit(0);
            }
            System.out.println("You need to specify action: 'show' or 'process'");
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Unable to set ipNum");
        }
    }
}
