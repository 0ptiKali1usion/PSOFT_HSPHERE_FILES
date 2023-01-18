package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/converter/DiskUsageFix.class */
public class DiskUsageFix extends C0004CP {
    public DiskUsageFix() {
        super("psoft_config.hsphere");
        System.out.println("Initialized");
    }

    /* renamed from: go */
    public void m33go() throws Exception {
        PreparedStatement ps = null;
        System.out.println("Getting trans connection");
        Connection con = Session.getTransConnection();
        System.out.println("Got");
        try {
            try {
                ps = con.prepareStatement("UPDATE usage_log SET used = used / 1024 WHERE usage_type = 3 AND used > 1024");
                ps.executeUpdate();
                con.commit();
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        if (argv.length > 0) {
            if ("--process".equals(argv[0])) {
                try {
                    DiskUsageFix t = new DiskUsageFix();
                    t.m33go();
                    System.out.println("Finished");
                } catch (Exception ex) {
                    System.out.println("An error has occured. Database rolled back");
                    ex.printStackTrace();
                    System.exit(-1);
                }
            } else {
                printHelpMessage();
            }
        } else {
            printHelpMessage();
        }
        System.exit(0);
    }

    public static void printHelpMessage() {
        System.out.println("NAME:\n\t psoft.hsphere.converter.DiskUsageFix - H-Sphere mail disk usage fixer");
        System.out.println("WARNING:this utility should be ran only for H-Sphere 2.3.306 users");
        System.out.println("\tfor fixing wrong amount of mail disk usage");
        System.out.println("\tIT IS STRONGLY ADVISEABLE TO BACKUP H-SPHERE DATABASE BEFORE PROCESSING");
        System.out.println("\tThe utility divides main disk usage bu 1024.");
        System.out.println("\tPLEASE DO NOT RUN THIS AGAIN AFTER SUCCESSFUL ATTEMPT");
        System.out.println("\tTHE --process KEY SHOULD BE SUPPLIED FOR RUN, OTHERWAYS NO ACTIONS WILL BE PERFORMED");
        System.exit(0);
    }
}
