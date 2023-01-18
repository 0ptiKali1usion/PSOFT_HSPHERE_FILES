package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PrioritySupportConfigurator.class */
public class PrioritySupportConfigurator extends C0004CP {
    public PrioritySupportConfigurator() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public static void main(String[] args) throws Exception {
        new PrioritySupportConfigurator();
        if (args.length < 2) {
            System.err.println("Usage: java psoft/hsphere/tool/PrioritySupportConfigurator alias client_description");
            System.exit(1);
        }
        Connection con = Session.getDb();
        boolean first = true;
        StringBuffer buf = new StringBuffer();
        List<String> services = new ArrayList();
        PreparedStatement ps = con.prepareStatement("SELECT name, ip1, mask1 FROM p_server");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            System.out.println("host[" + rs.getString(1) + "]=" + rs.getString(1) + ";" + rs.getString(2) + ";;check-host-alive;10;120;24x7;1;1;1;");
            if (first) {
                first = false;
            } else {
                buf.append(",");
            }
            buf.append(rs.getString(1));
            services.add("service[" + rs.getString(1) + "]+service[psoft1]=PING;0;24x7;3;5;1;admins;120;24x7;1;1;0;;check_ping!100.0,20%!500.0,60%");
        }
        System.out.println("hostgroup[" + args[0] + "]=" + args[1] + ";admins;" + buf.toString());
        Session.closeStatement(ps);
        PreparedStatement ps2 = con.prepareStatement("select p_server.name, l_server_groups.type_id from p_server, l_server, l_server_groups where l_server.p_server_id = p_server.id and l_server.group_id = l_server_groups.id;");
        ResultSet rs2 = ps2.executeQuery();
        while (rs2.next()) {
            switch (rs2.getInt(2)) {
                case 1:
                    System.out.println("service[" + rs2.getString(1) + "]=HTTP;0;24x7;3;5;1;admins;120;24x7;1;1;1;;check_http");
                    break;
                case 3:
                    System.out.println("service[" + rs2.getString(1) + "]=POP3;0;24x7;3;5;1;admins;120;24x7;1;1;1;;check_pop");
                    System.out.println("service[" + rs2.getString(1) + "]=SMTP;0;24x7;3;5;1;admins;120;24x7;1;1;1;;check_smtp");
                    break;
                case 5:
                    System.out.println("service[" + rs2.getString(1) + "]=HTTP;0;24x7;3;5;1;admins;120;24x7;1;1;1;;check_http");
                    break;
                case 10:
                    System.out.println("service[" + rs2.getString(1) + "]=CP;0;24x7;3;5;1;admins;120;24x7;1;1;1;;check_cp");
                    break;
            }
        }
        Session.closeStatement(ps2);
        for (String str : services) {
            System.out.println(str);
        }
        con.close();
        System.exit(0);
    }
}
