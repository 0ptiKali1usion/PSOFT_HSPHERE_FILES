package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.ODBC.DSNRecord;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DSNRecreator.class */
public class DSNRecreator {
    private static final Category log = Category.getInstance(DSNRecreator.class.getName());

    public DSNRecreator() throws Exception {
        ExternalCP.initCP();
    }

    public static void main(String[] args) throws Exception {
        StringBuffer keys = new StringBuffer("");
        for (String str : args) {
            keys.append(str);
        }
        if (keys.toString().indexOf("--help") != -1) {
            System.out.println("NAME:\n\t psoft.hsphere.tools.DSNRecreator - H-Sphere DSN Records Recreation tool");
            System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.DSNRecreator [options]");
            System.out.println("OPTIONS:");
            System.out.println("\t--help \t- shows this screen");
            System.out.println("\t--force \t- only physical recreation of DSN records");
            System.exit(0);
        }
        DSNRecreator dr = new DSNRecreator();
        System.out.println(keys);
        dr.process(keys.toString());
        System.exit(0);
    }

    public void process(String keys) throws Exception {
        System.out.println("Initializing...");
        System.out.println("Recreation of DSN Records...");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        List acc_ids = new ArrayList();
        try {
            try {
                ps = con.prepareStatement("SELECT DISTINCT account_id FROM parent_child pc, odbc_params op WHERE child_id = op.id AND op.name= ?");
                ps.setString(1, "DSN");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    acc_ids.add(new Long(rs.getLong(1)));
                }
                ps.close();
                con.close();
            } catch (Exception e) {
                Session.getLog().error("error recreating DSN record", e);
                e.printStackTrace();
                System.exit(1);
                ps.close();
                con.close();
            }
            for (int i = 0; i < acc_ids.size(); i++) {
                Session.save();
                try {
                    try {
                        long acc_id = ((Long) acc_ids.get(i)).longValue();
                        System.out.println("Process account #" + String.valueOf(acc_id));
                        Account a = Account.getAccount(acc_id);
                        TemplateList dsns = a.FM_findChildren("dsn_record");
                        while (dsns.hasNext()) {
                            ResourceId dsnId = dsns.next();
                            DSNRecord dsn = new DSNRecord(dsnId);
                            String dsn_value = dsn.get("DSN").toString();
                            User u = a.getUser();
                            String userPrefix = u.getUserPrefix();
                            if (!dsn_value.startsWith(userPrefix)) {
                                dbRecreate(u, a, dsn);
                                ResourceId rId = dsn.getId();
                                Resource.getCache().remove(rId);
                                DSNRecord dsn2 = (DSNRecord) rId.get();
                                if (keys.indexOf("--force") != -1) {
                                    physicalRecreate(u, a, dsn2);
                                }
                                System.out.println("\n");
                            }
                        }
                        Session.restore();
                    } catch (Exception e2) {
                        Session.getLog().error("error recreating DSN record", e2);
                        e2.printStackTrace();
                        Session.restore();
                    }
                } catch (Throwable th) {
                    Session.restore();
                    throw th;
                }
            }
            System.out.println("Recreation of DSN Records finished.");
        } catch (Throwable th2) {
            ps.close();
            con.close();
            throw th2;
        }
    }

    private void dbRecreate(User u, Account a, DSNRecord dsn) throws Exception {
        System.out.println("database recreation...");
        Session.save();
        try {
            Session.setUser(u);
            Session.setAccount(a);
            String dsn_value = dsn.get("DSN").toString();
            System.out.println("dsn_value = " + dsn_value);
            ResourceId unixuser = a.FM_getChild("unixuser");
            String oldUserPrefix = unixuser.get("login").toString() + "-";
            if (dsn_value.startsWith(oldUserPrefix)) {
                return;
            }
            String new_dsn_value = dsn_value.indexOf(oldUserPrefix) == -1 ? oldUserPrefix + dsn_value : "";
            System.out.println("new_dsn_value = " + new_dsn_value);
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                try {
                    ps = con.prepareStatement("UPDATE odbc_params SET value = ? WHERE name = ? AND id = ?");
                    ps.setString(1, new_dsn_value);
                    ps.setString(2, "DSN");
                    ps.setLong(3, dsn.getId().getId());
                    ps.executeUpdate();
                    ps.close();
                    con.close();
                } catch (Throwable th) {
                    ps.close();
                    con.close();
                    throw th;
                }
            } catch (Exception e) {
                Session.getLog().error("error recreating DSN record", e);
                e.printStackTrace();
                ps.close();
                con.close();
            }
        } catch (Exception ex) {
            Session.getLog().error("error recreating DSN record", ex);
        } finally {
            Session.restore();
        }
    }

    private void physicalRecreate(User u, Account a, DSNRecord dsn) throws Exception {
        System.out.println("physical recreation...");
        Session.save();
        try {
            try {
                Session.setUser(u);
                Session.setAccount(a);
                String dsn_value = dsn.get("DSN").toString();
                System.out.println("dsn_value = " + dsn_value);
                dsn.physicalDelete(dsn.getHostId());
                dsn.physicalCreate(dsn.getHostId());
                Session.restore();
            } catch (Exception ex) {
                Session.getLog().error("error recreating DSN record", ex);
                Session.restore();
            }
        } catch (Throwable th) {
            Session.restore();
            throw th;
        }
    }
}
