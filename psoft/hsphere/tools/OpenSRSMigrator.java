package psoft.hsphere.tools;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.manager.Manager;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.opensrs.OpenSRS;

/* loaded from: hsphere.zip:psoft/hsphere/tools/OpenSRSMigrator.class */
public class OpenSRSMigrator extends C0004CP {
    static String openSRSconf = "/etc/OpenSRS.conf";
    static String[] tlds = {"org", "net", "com"};

    public OpenSRSMigrator() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] args) throws Exception {
        new OpenSRSMigrator();
        BufferedReader f1 = new BufferedReader(new InputStreamReader(new FileInputStream(openSRSconf)));
        String user = "";
        String privateKey = "";
        RE reUser = new RE("^my\\s+\\$USERNAME\\s*=\\s*\"(\\S+)\";\\s.*$");
        RE rePrivateKey = new RE("^my\\s+\\$PRIVATE_KEY\\s*=\\s*\"(\\S+)\";\\s.*$");
        while (true) {
            String line = f1.readLine();
            if (line == null) {
                break;
            } else if (reUser.isMatch(line)) {
                REMatch x = reUser.getMatch(line);
                user = x.substituteInto("$1");
                System.out.println("findUser=" + user);
            } else if (rePrivateKey.isMatch(line)) {
                REMatch x2 = rePrivateKey.getMatch(line);
                privateKey = x2.substituteInto("$1");
                System.out.println("findPkey=" + privateKey);
            }
        }
        f1.close();
        if (user != "" && privateKey != "") {
            int opensrs_exist = 0;
            Manager registrar = Manager.getManager("REGISTRAR");
            Collection entities = registrar.getEntities();
            Iterator i = entities.iterator();
            while (true) {
                if (i.hasNext()) {
                    if (i.next() instanceof OpenSRS) {
                        System.out.println("OpenSRS records already exist. Skip addEntity");
                        opensrs_exist = 1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (opensrs_exist == 0) {
                HashMap entity = new HashMap();
                entity.put("username", user);
                entity.put(MerchantGatewayManager.MG_KEY_PREFIX, privateKey);
                entity.put("host", "horizon.opensrs.net");
                entity.put("port", "55000");
                entity.put("TEMPLATE", "admin/registrar/OpenSRS.html");
                HostEntry he = HostManager.getRandomHost(10);
                entity.put("ip", he.getPFirstIP());
                Registrar registrar2 = (Registrar) registrar.addEntity("psoft.hsphere.resource.registrar.opensrs.OpenSRS", "OpenSRS Registrar", 0L, entity);
            }
        } else {
            System.out.println("Failed! Cannot find in " + openSRSconf + " $USERNAME or(and) $PRIVATE_KEY parameter");
            System.exit(-1);
        }
        Connection con = Session.getTransConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT plan_id, value FROM plan_value WHERE name='_SETUP_PRICE_opensrs_'");
                ResultSet rs = ps.executeQuery();
                int found = 0;
                while (rs.next()) {
                    found = 1;
                    System.out.println("Plan: " + rs.getString(1));
                    for (int n = 0; n < 3; n++) {
                        System.out.println("TLD: " + tlds[n]);
                        for (int i2 = 1; i2 < 11; i2++) {
                            PreparedStatement psi = con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (" + rs.getString(1) + ",'_SETUP_PRICE_TLD_" + tlds[n] + "_" + i2 + "_', 3010," + (i2 * Float.parseFloat(rs.getString(2))) + ")");
                            psi.executeUpdate();
                            psi.close();
                        }
                    }
                }
                if (found > 0) {
                    System.out.println("DELETE opensrs record in plan_value");
                    ps.close();
                    ps = con.prepareStatement("DELETE FROM plan_value WHERE name='_SETUP_PRICE_opensrs_'");
                    ps.executeUpdate();
                }
                con.commit();
                ps.close();
                Session.commitTransConnection(con);
            } catch (Exception e) {
                con.rollback();
                ps.close();
                Session.commitTransConnection(con);
            }
            System.out.println("Migration finished");
            System.exit(0);
        } catch (Throwable th) {
            ps.close();
            Session.commitTransConnection(con);
            throw th;
        }
    }
}
