package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.admin.AdmResellerSSL;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/RepostResellerSSLConfigs.class */
public class RepostResellerSSLConfigs extends C0004CP {

    /* renamed from: db */
    protected Database f233db;
    protected ResourceBundle config;
    protected List data;

    public RepostResellerSSLConfigs() throws Exception {
        super("psoft_config.hsphere");
        this.data = new ArrayList();
        this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        this.f233db = Toolbox.getDB(this.config);
    }

    public static void main(String[] args) {
        try {
            StringBuffer keys = new StringBuffer("");
            for (String str : args) {
                keys.append(str);
            }
            RepostResellerSSLConfigs test = new RepostResellerSSLConfigs();
            if (keys.toString().indexOf("--help") != -1) {
                test.printDescr();
            }
            System.out.println(keys);
            test.m7go(keys.toString(), args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Reposting Reseller CP SSL config files finished.\nRestart your Control Panel to apply changes.");
        System.exit(0);
    }

    /* renamed from: go */
    public void m7go(String keys, String[] args) throws Exception {
        if (keys.indexOf("--process") != -1) {
            getDataFromDB();
        } else if (keys.indexOf("--reseller") != -1) {
            getDataFromArgs(args);
        }
        process();
    }

    /* JADX WARN: Code restructure failed: missing block: B:39:0x002b, code lost:
        r9 = r8 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0035, code lost:
        if (r9 >= r5.length) goto L19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x0038, code lost:
        r0 = r5[r9];
        r7.setString(1, r0);
        r0 = r7.executeQuery();
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0056, code lost:
        if (r0.next() == false) goto L17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x0059, code lost:
        r0 = r0.getLong(1);
        addResToData(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x006c, code lost:
        r9 = r9 + 1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void getDataFromArgs(java.lang.String[] r5) throws java.lang.Exception {
        /*
            r4 = this;
            r0 = r4
            psoft.db.Database r0 = r0.f233db
            java.sql.Connection r0 = r0.getConnection()
            r6 = r0
            r0 = 0
            r7 = r0
            r0 = r6
            java.lang.String r1 = "SELECT id FROM users WHERE username=?"
            java.sql.PreparedStatement r0 = r0.prepareStatement(r1)     // Catch: java.lang.Throwable -> L8a
            r7 = r0
            r0 = 0
            r8 = r0
        L18:
            r0 = r8
            r1 = r5
            int r1 = r1.length     // Catch: java.lang.Throwable -> L8a
            if (r0 >= r1) goto L7b
            java.lang.String r0 = "--reseller"
            r1 = r5
            r2 = r8
            r1 = r1[r2]     // Catch: java.lang.Throwable -> L8a
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Throwable -> L8a
            if (r0 == 0) goto L75
            r0 = r8
            r1 = 1
            int r0 = r0 + r1
            r9 = r0
        L31:
            r0 = r9
            r1 = r5
            int r1 = r1.length     // Catch: java.lang.Throwable -> L8a
            if (r0 >= r1) goto L72
            r0 = r5
            r1 = r9
            r0 = r0[r1]     // Catch: java.lang.Throwable -> L8a
            r10 = r0
            r0 = r7
            r1 = 1
            r2 = r10
            r0.setString(r1, r2)     // Catch: java.lang.Throwable -> L8a
            r0 = r7
            java.sql.ResultSet r0 = r0.executeQuery()     // Catch: java.lang.Throwable -> L8a
            r11 = r0
        L4f:
            r0 = r11
            boolean r0 = r0.next()     // Catch: java.lang.Throwable -> L8a
            if (r0 == 0) goto L6c
            r0 = r11
            r1 = 1
            long r0 = r0.getLong(r1)     // Catch: java.lang.Throwable -> L8a
            r12 = r0
            r0 = r4
            r1 = r12
            r0.addResToData(r1)     // Catch: java.lang.Throwable -> L8a
            goto L4f
        L6c:
            int r9 = r9 + 1
            goto L31
        L72:
            goto L7b
        L75:
            int r8 = r8 + 1
            goto L18
        L7b:
            r0 = r7
            r0.close()
            r0 = r6
            r0.close()
            goto L9b
        L8a:
            r14 = move-exception
            r0 = r7
            r0.close()
            r0 = r6
            r0.close()
            r0 = r14
            throw r0
        L9b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.tools.RepostResellerSSLConfigs.getDataFromArgs(java.lang.String[]):void");
    }

    private void addResToData(long res_id) throws Exception {
        Connection con = this.f233db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, ip FROM reseller_ssl WHERE reseller_id = ?");
            ps.setLong(1, res_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long resId = rs.getLong(1);
                String ip = rs.getString(2);
                Hashtable oneRes = new Hashtable();
                oneRes.put("ssl_id", new Long(resId));
                oneRes.put("ip", ip);
                oneRes.put("id", new Long(res_id));
                String login = getResLoginById(res_id);
                oneRes.put("login", login);
                this.data.add(oneRes);
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void getDataFromDB() throws Exception {
        Connection con = this.f233db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, ip, reseller_id FROM reseller_ssl");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long resId = rs.getLong(1);
                String ip = rs.getString(2);
                long reseller_id = rs.getLong(3);
                Hashtable oneRes = new Hashtable();
                oneRes.put("ssl_id", new Long(resId));
                oneRes.put("ip", ip);
                oneRes.put("id", new Long(reseller_id));
                String login = getResLoginById(reseller_id);
                oneRes.put("login", login);
                this.data.add(oneRes);
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private String getResLoginById(long resId) throws Exception {
        String result = new String();
        Connection con = this.f233db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT username FROM users WHERE id=?");
            ps.setLong(1, resId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getString(1);
            }
            ps.close();
            con.close();
            return result;
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void process() throws Exception {
        int sslType;
        for (int i = 0; i < this.data.size(); i++) {
            Hashtable oneRes = (Hashtable) this.data.get(i);
            long resId = ((Long) oneRes.get("ssl_id")).longValue();
            String ip = (String) oneRes.get("ip");
            long id = ((Long) oneRes.get("id")).longValue();
            String login = (String) oneRes.get("login");
            System.out.print("Process reseller \"" + login + "\" with IP = " + String.valueOf(ip) + "... ");
            try {
                Session.setResellerId(id);
                AdmResellerSSL resSSL = AdmResellerSSL.get(resId);
                int ipType = HostEntry.checkAllIP(ip);
                if (ipType == 4) {
                    sslType = AdmResellerSSL.sslBaseTypeId("port_based");
                } else {
                    sslType = AdmResellerSSL.sslBaseTypeId("ip_based");
                    resSSL.setInsecPort(Session.getProperty("RESELLER_SSL_INSEC_PORT"));
                }
                resSSL.setSSLType(sslType);
                resSSL.changeDBIP(ip);
                resSSL.repostConfig(ip);
                System.out.print("[OK]\n");
            } catch (UnknownResellerException e) {
                System.out.print("[Failed] Unknown reseller\n");
                delUnknownResSSLFromDB(resId);
            } catch (Exception e2) {
                System.out.print("[Failed] " + e2.toString() + "\n");
                e2.printStackTrace();
            }
        }
    }

    private void delUnknownResSSLFromDB(long resId) throws Exception {
        System.out.print("Delete unknown reseller_ssl from DB... ");
        Connection con = this.f233db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM reseller_ssl WHERE id = ?");
            ps.setLong(1, resId);
            ps.executeUpdate();
            ps.close();
            con.close();
            System.out.print("[OK]\n");
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void printDescr() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.RepostResellerSSLConfigs -\n\t\tthis H-Sphere tool recreates virtual host\n\t\tconfig files for resellers.");
        System.out.println("SYNOPSIS:\n\t java psoft.hsphere.tools.RepostResellerSSLConfigs [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help - shows this screen");
        System.out.println("\t--process - run the tool for all config files");
        System.out.println("\t--reseller <res_name_1> <res_name_2>...\n\t\t<res_name_n> - run the tool for determined\n\t\treseller user names.");
        System.exit(0);
    }
}
