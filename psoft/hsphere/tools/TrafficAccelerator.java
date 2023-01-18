package psoft.hsphere.tools;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.C0004CP;
import psoft.p000db.Database;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/TrafficAccelerator.class */
public class TrafficAccelerator extends C0004CP {

    /* renamed from: db */
    Database f238db;
    protected ResourceBundle config;

    public TrafficAccelerator() throws Exception {
        super("psoft_config.hsphere");
        this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        this.f238db = Toolbox.getDB(this.config);
    }

    public static void main(String[] args) {
        try {
            StringBuffer keys = new StringBuffer("");
            for (String str : args) {
                keys.append(str);
            }
            if (keys.toString().indexOf("--help") != -1) {
                System.out.println("NAME:\n\t psoft.hsphere.tools.TrafficAccelerator - H-Sphere traffic accelerating utility");
                System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.TrafficAccelerator [options]");
                System.out.println("OPTIONS:");
                System.out.println("\t--help \t- shows this screen");
                System.out.println("\t--process \t- run tool to accelerate Traffic");
                System.exit(0);
            }
            System.out.println(keys);
            TrafficAccelerator test = new TrafficAccelerator();
            test.m2go(keys.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Traffic accelerating finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m2go(String keys) throws Exception {
        if (keys.indexOf("--process") == -1) {
            return;
        }
        dropIndexes();
        if (!hasFlagField()) {
            addField();
        }
        createIndexes();
        updateFlagField();
        updateProperties();
    }

    private void dropIndexes() throws Exception {
        Connection con = this.f238db.getConnection();
        PreparedStatement ps = null;
        try {
            System.out.println("Dropping index \"trans_log_ind1\"...");
            PreparedStatement ps2 = con.prepareStatement("DROP INDEX trans_log_ind1");
            ps2.executeUpdate();
            System.out.println("Index \"trans_log_ind1\" was dropped successfully.\n");
            System.out.println("Dropping index \"trans_log_ind2\"...");
            PreparedStatement ps3 = con.prepareStatement("DROP INDEX trans_log_ind2");
            ps3.executeUpdate();
            System.out.println("Index \"trans_log_ind2\" has been dropped successfully.\n");
            System.out.println("Dropping index \"trans_log_ind3\"...");
            PreparedStatement ps4 = con.prepareStatement("DROP INDEX trans_log_ind3");
            ps4.executeUpdate();
            System.out.println("Index \"trans_log_ind3\" has been dropped successfully.\n");
            System.out.println("Dropping index \"trans_log_ind4\"...");
            PreparedStatement ps5 = con.prepareStatement("DROP INDEX trans_log_ind4");
            ps5.executeUpdate();
            System.out.println("Index \"trans_log_ind4\" has been dropped successfully.\n");
            System.out.println("Dropping index \"trans_log_ps\"...");
            ps = con.prepareStatement("DROP INDEX trans_log_ps");
            ps.executeUpdate();
            System.out.println("Index \"trans_log_ps\" has been dropped successfully.\n");
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void createIndexes() throws Exception {
        Connection con = this.f238db.getConnection();
        PreparedStatement ps = null;
        try {
            System.out.println("Adding index \"trans_log_ps\"...");
            PreparedStatement ps2 = con.prepareStatement("CREATE INDEX trans_log_ps ON trans_log USING BTREE (p_server_id, flag)");
            ps2.executeUpdate();
            System.out.println("Index \"trans_log_ps\" has been added successfully\n");
            System.out.println("Adding index \"trans_log_ind1\"...");
            PreparedStatement ps3 = con.prepareStatement("CREATE INDEX trans_log_ind1 ON trans_log USING BTREE (account_id, cdate, flag)");
            ps3.executeUpdate();
            System.out.println("Index \"trans_log_ind1\" has been added successfully\n");
            System.out.println("Adding index \"trans_log_ind2\"...");
            PreparedStatement ps4 = con.prepareStatement("CREATE INDEX trans_log_ind2 ON trans_log USING BTREE (resource_id, cdate, flag)");
            ps4.executeUpdate();
            System.out.println("Index \"trans_log_ind2\" has been added successfully\n");
            System.out.println("Adding index \"trans_log_ind3\"...");
            PreparedStatement ps5 = con.prepareStatement("CREATE INDEX trans_log_ind3 ON trans_log USING BTREE (cdate, flag)");
            ps5.executeUpdate();
            System.out.println("Index \"trans_log_ind3\" has been added successfully\n");
            System.out.println("Adding index \"trans_log_ind4\"...");
            ps = con.prepareStatement("CREATE INDEX trans_log_ind4 ON trans_log USING BTREE (name, tt_type, p_server_id, flag)");
            ps.executeUpdate();
            System.out.println("Index \"trans_log_ind4\" has been added successfully\n");
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void addField() throws Exception {
        Connection con = this.f238db.getConnection();
        PreparedStatement ps = null;
        try {
            System.out.println("Adding field \"flag\" into table \"trans_log\"...");
            ps = con.prepareStatement("ALTER TABLE trans_log ADD flag integer");
            ps.executeUpdate();
            System.out.println("Field \"flag\" has been added successfully\n");
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void updateFlagField() throws Exception {
        Connection con = this.f238db.getConnection();
        PreparedStatement ps = null;
        try {
            System.out.println("Setting flag into \"trans_log\"...");
            ps = con.prepareStatement("update trans_log set flag=1 where cdate < ?");
            Calendar cal = TimeUtils.getCalendar();
            cal.set(2003, 1, 1);
            ps.setDate(1, new Date(cal.getTime().getTime()));
            ps.executeUpdate();
            System.out.println("Flag has been set successfully\n");
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void updateProperties() throws Exception {
        System.out.println("Checking hsphere.properties...");
        try {
            this.config.getString("FAST_TRAFFIC");
        } catch (MissingResourceException e) {
            System.out.println("Adding variable \"FAST_TRAFFIC\" into hsphere.properties");
            FileWriter fw = new FileWriter("/hsphere/local/home/cpanel/shiva/psoft_config/hsphere.properties", true);
            fw.write("\nFAST_TRAFFIC=TRUE\n");
            fw.close();
            System.out.println("Variable \"FAST_TRAFFIC\" has been added successfully");
        }
    }

    private boolean hasFlagField() throws Exception {
        Connection con = this.f238db.getConnection();
        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet columns = dbMetaData.getColumns(null, "", "trans_log", "flag");
        while (columns.next()) {
            String tableName = columns.getString(3);
            String columnName = columns.getString(4);
            String columnJavaType = columns.getString(5);
            if ("trans_log".equals(tableName) && "flag".equals(columnName) && Integer.parseInt(columnJavaType) == 4) {
                return true;
            }
        }
        return false;
    }
}
