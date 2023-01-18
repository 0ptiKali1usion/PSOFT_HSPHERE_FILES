package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.log4j.Category;
import psoft.p000db.Database;
import psoft.util.Config;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/ReferalProgram.class */
public class ReferalProgram {
    private static Category log = Category.getInstance(ReferalProgram.class.getName());
    protected ResourceBundle config;
    Hashtable other = new Hashtable();
    static Connection con;

    /* renamed from: ps */
    static PreparedStatement f38ps;

    /* renamed from: rs */
    static ResultSet f39rs;
    static long referal_id;
    static int referal_group;
    static double referal_fee;

    private void performReferalProgram() throws Exception {
        PreparedStatement ups = null;
        try {
            try {
                Session.getLog().debug("ReferalProgram: starting select users.");
                ups = con.prepareStatement("SELECT a.id, a.username, b.referal_id FROM users a,referrals b WHERE a.id=b.user_id AND b.referal_id <> ?");
                ups.setLong(1, 0L);
                Session.getLog().debug("ReferalProgram: user prepared statement is ready.Going to execute");
                ResultSet urs = ups.executeQuery();
                Session.getLog().debug("ReferalProgram: Statement executed");
                while (urs.next()) {
                    Session.getLog().debug("ReferalProgram: Going to getUser");
                    User u = User.getUser(urs.getString("username"));
                    Session.getLog().debug("ReferalProgram: The user is " + u.getLogin());
                    Session.setUser(u);
                    Iterator i = u.getAccountIds().iterator();
                    while (i.hasNext()) {
                        Session.setAccount(u.getAccount((ResourceId) i.next()));
                        Session.getLog().debug("ReferalProgram:Using Account " + Long.toString(Session.getAccount().getId().getId()));
                    }
                }
                Session.closeStatement(ups);
                con.close();
            } catch (SQLException sqle) {
                Session.getLog().debug("ReferalProgram: SQLException " + sqle.toString());
                Session.closeStatement(ups);
                con.close();
            } catch (Exception e) {
                Session.getLog().debug("ReferalProgram: was NOT PERFORMED because " + e.toString());
                Session.closeStatement(ups);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ups);
            con.close();
            throw th;
        }
    }

    void initialize() {
        try {
            this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            Session.initMailer();
            try {
                log.info("initialized");
                Hashtable conf = new Hashtable();
                Database db = Toolbox.getDB(this.config);
                Session.setDb(db);
                Session.setLog(log);
                conf.put(Database.class, db);
                conf.put("config", this.config);
                Config.set("CLIENT", conf);
                Config.set("psoft.user", conf);
            } catch (MissingResourceException e) {
            }
            con = Session.getDb();
        } catch (Exception e2) {
            e2.printStackTrace();
            throw new ExceptionInInitializerError(e2);
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting");
        ReferalProgram rp = new ReferalProgram();
        System.out.println("Init");
        rp.initialize();
        System.out.println("InitDone");
        log.info("Referal program begins:" + TimeUtils.getDate());
        try {
            rp.performReferalProgram();
        } catch (Exception ex) {
            log.error("Error during operations ", ex);
        }
        log.info("DONE");
        System.exit(0);
    }
}
