package psoft.hsphere.tools.install;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/install/InstallTool.class */
public class InstallTool extends C0004CP {
    public InstallTool() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    protected static void printUsage() {
        System.err.println("Usage: java psoft.hsphere.tools.install.InstallTool <optoion> [params]");
        System.err.println("--get-mail-server DOMAIN\t Returns IP address of the mail server");
        System.err.println("--get-mail-server-name DOMAIN\t Returns FQDN of the mail server");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        InstallTool tool = new InstallTool();
        String option = args[0];
        if ("--get-mail-server".equals(option)) {
            tool.getMailServer(args[1]);
            System.exit(0);
        }
        if ("--get-mail-server-name".equals(option)) {
            tool.getMailServerName(args[1]);
            System.exit(0);
            return;
        }
        printUsage();
        System.exit(1);
    }

    public void getMailServer(String domain) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT p_server.ip1 FROM p_server, l_server, domains, parent_child, mail_services WHERE UPPER(domains.name) = UPPER(?) AND domains.id = parent_child.parent_id AND parent_child.child_id = mail_services.id AND mail_services.mail_server = l_server.id AND l_server.p_server_id = p_server.id");
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getString(1));
            } else {
                System.err.println("Cannot find mail server");
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void getMailServerName(String domain) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server.name FROM l_server, domains, parent_child, mail_services WHERE UPPER(domains.name) = UPPER(?) AND domains.id = parent_child.parent_id AND parent_child.child_id = mail_services.id AND mail_services.mail_server = l_server.id");
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getString(1));
            } else {
                System.err.println("Cannot find mail server");
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
