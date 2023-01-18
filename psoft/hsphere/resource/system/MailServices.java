package psoft.hsphere.resource.system;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.yafv.yafvsym;

/* loaded from: hsphere.zip:psoft/hsphere/resource/system/MailServices.class */
public class MailServices {
    public static void createMailingList(HostEntry host, String email, String description) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {"-a add -d " + getDomain(email), " -n " + getUsername(email)};
            host.exec("mlist-mod", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, create mailing list: " + email);
        } else {
            String username = getUsername(email);
            String domain = getIrisDomain(email);
            String hostname = getIrisHost(email, domain);
            Connection con = getIrisConnection("Iris");
            Connection con_list = getIrisConnection("IrisList");
            PreparedStatement pstm = null;
            if (description == null) {
                description = " ";
            }
            try {
                long pat_id = getIrisPattern_id(username, hostname, domain, false);
                if (pat_id <= 0) {
                    PreparedStatement pstm2 = con.prepareStatement("INSERT INTO Patterns (username, host, domain) VALUES(?, ?, ?)");
                    pstm2.setString(1, username);
                    pstm2.setString(2, hostname);
                    pstm2.setString(3, domain);
                    pstm2.executeUpdate();
                    Session.closeStatement(pstm2);
                    long pat_id2 = getIrisPattern_id(username, hostname, domain, false);
                    if (pat_id2 <= 0) {
                        throw new Exception("Iris database error: can't insert " + email + " in the Patterns table");
                    }
                    PreparedStatement pstm3 = con.prepareStatement("INSERT INTO Rules (pattern_id, action, expiration, quota) VALUES(?, 'List', '0000-00-00 00:00:00', 0)");
                    pstm3.setLong(1, pat_id2);
                    pstm3.executeUpdate();
                    Session.closeStatement(pstm3);
                    PreparedStatement pstm4 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id=? AND action='List'");
                    pstm4.setLong(1, pat_id2);
                    ResultSet rs = pstm4.executeQuery();
                    Session.closeStatement(pstm4);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm5 = con.prepareStatement("INSERT INTO MailingLists(rule_id, admin, reply_to_list, advanced) VALUES(?, 0, 'Y', 'Y')");
                        pstm5.setLong(1, rule_id);
                        pstm5.executeUpdate();
                        Session.closeStatement(pstm5);
                        pstm = con_list.prepareStatement("INSERT INTO Options(rule_id,  list_name) VALUES(?,?)");
                        pstm.setLong(1, rule_id);
                        pstm.setString(2, description);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        if (con_list != null) {
                            return;
                        }
                        return;
                    }
                    throw new Exception("Iris database error: can't insert " + email + " in the Rules table");
                }
                PreparedStatement pstm6 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id=? AND action='Discard'");
                pstm6.setLong(1, pat_id);
                ResultSet rs2 = pstm6.executeQuery();
                Session.closeStatement(pstm6);
                if (rs2.next()) {
                    long rule_id2 = rs2.getLong(1);
                    PreparedStatement pstm7 = con.prepareStatement("SELECT COUNT(*) FROM Vacation WHERE rule_id = ?");
                    pstm7.setLong(1, rule_id2);
                    ResultSet rs3 = pstm7.executeQuery();
                    Session.closeStatement(pstm7);
                    rs3.next();
                    if (rs3.getInt(1) > 0) {
                        PreparedStatement pstm8 = con.prepareStatement("UPDATE Rules SET action='List' WHERE rule_id = ?");
                        pstm8.setLong(1, rule_id2);
                        pstm8.executeUpdate();
                        Session.closeStatement(pstm8);
                        PreparedStatement pstm9 = con.prepareStatement("INSERT INTO MailingLists(rule_id, admin, reply_to_list, advanced) VALUES(?, 0, 'Y', 'Y')");
                        pstm9.setLong(1, rule_id2);
                        pstm9.executeUpdate();
                        Session.closeStatement(pstm9);
                        PreparedStatement pstm10 = con_list.prepareStatement("INSERT INTO Options(rule_id, list_name) VALUES(?, ?)");
                        pstm10.setLong(1, rule_id2);
                        pstm10.setString(2, description);
                        pstm10.executeUpdate();
                        if (pstm10 != null) {
                            Session.closeStatement(pstm10);
                        }
                        if (con != null) {
                            con.close();
                        }
                        if (con_list != null) {
                            con_list.close();
                            return;
                        }
                        return;
                    }
                    throw new Exception("Iris database error: mail object  " + email + " already exist. Check the Patterns table");
                }
                throw new Exception("Iris database error: mail object  " + email + " already exist. Check the Patterns table");
            } finally {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
                if (con_list != null) {
                    con_list.close();
                }
            }
        }
    }

    public static void deleteMailingList(HostEntry host, String email) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {"-a rem -d " + getDomain(email), " -n " + getUsername(email)};
            host.exec("mlist-mod", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, delete mailing list: " + email);
        } else {
            Connection con = getIrisConnection("Iris");
            PreparedStatement pstm = null;
            try {
                long pat_id = getIrisPattern_id(email, false);
                if (pat_id > 0) {
                    pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm2 = con.prepareStatement("SELECT address_id FROM Subscribers WHERE rule_id = ?");
                        pstm2.setLong(1, rule_id);
                        ResultSet rs2 = pstm2.executeQuery();
                        Session.closeStatement(pstm2);
                        PreparedStatement pstm3 = con.prepareStatement("DELETE FROM Subscribers WHERE rule_id = ? AND address_id = ?");
                        while (rs2.next()) {
                            long adr_id = rs2.getLong(1);
                            pstm3.setLong(1, rule_id);
                            pstm3.setLong(2, adr_id);
                            pstm3.executeUpdate();
                            removeIrisAddress(rule_id, adr_id);
                        }
                        Session.closeStatement(pstm3);
                        Connection con_list = getIrisConnection("IrisList");
                        PreparedStatement pstm4 = con_list.prepareStatement("SELECT address_id FROM Members WHERE rule_id = ?");
                        pstm4.setLong(1, rule_id);
                        ResultSet rs3 = pstm4.executeQuery();
                        Session.closeStatement(pstm4);
                        while (rs3.next()) {
                            removeIrisAddress(rule_id, rs3.getLong(1));
                        }
                        if (con_list != null) {
                            con_list.close();
                        }
                        PreparedStatement pstm5 = con.prepareStatement("DELETE FROM MailingLists WHERE rule_id = ?");
                        pstm5.setLong(1, rule_id);
                        pstm5.executeUpdate();
                        Session.closeStatement(pstm5);
                        PreparedStatement pstm6 = con.prepareStatement("SELECT COUNT(*) FROM Vacation WHERE rule_id = ?");
                        pstm6.setLong(1, rule_id);
                        ResultSet rs4 = pstm6.executeQuery();
                        Session.closeStatement(pstm6);
                        rs4.next();
                        if (rs4.getInt(1) > 0) {
                            pstm = con.prepareStatement("UPDATE Rules SET action='Discard' WHERE rule_id = ?");
                            pstm.setLong(1, rule_id);
                            pstm.executeUpdate();
                            Session.closeStatement(pstm);
                        } else {
                            PreparedStatement pstm7 = con.prepareStatement("DELETE FROM Rules WHERE rule_id = ?");
                            pstm7.setLong(1, rule_id);
                            pstm7.executeUpdate();
                            Session.closeStatement(pstm7);
                            pstm = con.prepareStatement("DELETE FROM Patterns WHERE pattern_id = ?");
                            pstm.setLong(1, pat_id);
                            pstm.executeUpdate();
                            Session.closeStatement(pstm);
                            cleanIrisUsage(rule_id);
                        }
                        cleanIrisList(rule_id);
                        if (pstm != null) {
                            Session.closeStatement(pstm);
                        }
                        if (con != null) {
                            con.close();
                            return;
                        }
                        return;
                    }
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
            } finally {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
            }
        }
    }

    public static Collection listMailingList(HostEntry host, String email) throws Exception {
        return listMailingList(host, email, false);
    }

    public static Collection listMailingList(HostEntry host, String email, boolean isModerate) throws Exception {
        if (HostEntry.getEmulationMode()) {
            return new ArrayList();
        }
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = new String[3];
            cmd[0] = getDomain(email);
            cmd[1] = getUsername(email);
            cmd[2] = isModerate ? "/mod" : "";
            return host.exec("mlist-list", cmd);
        }
        Connection con = getIrisConnection("Iris");
        PreparedStatement pstm = null;
        ArrayList res = new ArrayList();
        try {
            long pat_id = getIrisPattern_id(email, false);
            if (pat_id > 0) {
                pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id=? AND action='List'");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    if (!isModerate) {
                        PreparedStatement pstm2 = con.prepareStatement("SELECT address_id FROM Subscribers WHERE rule_id=?");
                        pstm2.setLong(1, rule_id);
                        ResultSet rs2 = pstm2.executeQuery();
                        Session.closeStatement(pstm2);
                        while (rs2.next()) {
                            long adr_id = rs2.getLong(1);
                            pstm2 = con.prepareStatement("SELECT username, host, domain FROM Addresses WHERE address_id=?");
                            pstm2.setLong(1, adr_id);
                            ResultSet rs1 = pstm2.executeQuery();
                            Session.closeStatement(pstm2);
                            if (rs1.next()) {
                                if (rs1.getString(2).equals("*")) {
                                    res.add(rs1.getString(1) + "@" + rs1.getString(3));
                                } else {
                                    res.add(rs1.getString(1) + "@" + rs1.getString(2) + "." + rs1.getString(3));
                                }
                            }
                        }
                        if (pstm2 != null) {
                            Session.closeStatement(pstm2);
                        }
                        if (con != null) {
                            con.close();
                        }
                        return res;
                    }
                    PreparedStatement pstm3 = con.prepareStatement("SELECT admin FROM MailingLists WHERE rule_id=?");
                    pstm3.setLong(1, rule_id);
                    ResultSet rs3 = pstm3.executeQuery();
                    Session.closeStatement(pstm3);
                    if (rs3.next()) {
                        long admin = rs3.getLong(1);
                        Connection con1 = getIrisConnection("IrisList");
                        PreparedStatement pstm4 = con1.prepareStatement("SELECT address_id FROM Members WHERE rule_id=? AND address_id<>? AND admin='Y'");
                        pstm4.setLong(1, rule_id);
                        pstm4.setLong(2, admin);
                        ResultSet rs12 = pstm4.executeQuery();
                        Session.closeStatement(pstm4);
                        pstm3 = con.prepareStatement("SELECT username, host, domain FROM Addresses WHERE address_id=?");
                        while (rs12.next()) {
                            long adr_id2 = rs12.getLong(1);
                            pstm3.setLong(1, adr_id2);
                            ResultSet rs4 = pstm3.executeQuery();
                            if (rs4.next()) {
                                if (rs4.getString(2).equals("*")) {
                                    res.add(rs4.getString(1) + "@" + rs4.getString(3));
                                } else {
                                    res.add(rs4.getString(1) + "@" + rs4.getString(2) + "." + rs4.getString(3));
                                }
                            }
                        }
                        Session.closeStatement(pstm3);
                        if (con1 != null) {
                            con1.close();
                        }
                    }
                    if (pstm3 != null) {
                        Session.closeStatement(pstm3);
                    }
                    if (con != null) {
                        con.close();
                    }
                    return res;
                }
            }
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static Collection statMailingList(HostEntry host, String email) throws Exception {
        if (HostEntry.getEmulationMode()) {
            return Arrays.asList("", "");
        }
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {getDomain(email), getUsername(email)};
            return host.exec("mlist-stat", cmd);
        }
        PreparedStatement pstm = null;
        Connection con = getIrisConnection("Iris");
        String admin = "";
        char[] arr = new char[4];
        try {
            long pat_id = getIrisPattern_id(email, false);
            if (pat_id > 0) {
                pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id=? AND action='List'");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    PreparedStatement pstm2 = con.prepareStatement("SELECT admin FROM MailingLists WHERE rule_id=?");
                    pstm2.setLong(1, rule_id);
                    ResultSet rs2 = pstm2.executeQuery();
                    Session.closeStatement(pstm2);
                    if (rs2.next()) {
                        long admin_adr = rs2.getLong(1);
                        PreparedStatement pstm3 = con.prepareStatement("SELECT username, host, domain FROM Addresses WHERE address_id=?");
                        pstm3.setLong(1, admin_adr);
                        ResultSet rs3 = pstm3.executeQuery();
                        Session.closeStatement(pstm3);
                        if (rs3.next()) {
                            admin = rs3.getString(2).equals("*") ? rs3.getString(1) + "@" + rs3.getString(3) : rs3.getString(1) + "@" + rs3.getString(2) + "." + rs3.getString(3);
                        }
                    }
                    con.close();
                    con = getIrisConnection("IrisList");
                    pstm = con.prepareStatement("SELECT moderated, private, members_only, archive FROM Options WHERE rule_id=?");
                    pstm.setLong(1, rule_id);
                    ResultSet rs4 = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs4.next()) {
                        if ("N".equals(rs4.getString(1))) {
                            arr[1] = 'M';
                        } else {
                            arr[1] = 'm';
                        }
                        if ("N".equals(rs4.getString(2))) {
                            arr[2] = 'H';
                        } else {
                            arr[2] = 'h';
                        }
                        if ("N".equals(rs4.getString(3))) {
                            arr[0] = 'O';
                        } else {
                            arr[0] = 'o';
                        }
                        if ("N".equals(rs4.getString(4))) {
                            arr[3] = 'A';
                        } else {
                            arr[3] = 'a';
                        }
                    }
                }
            }
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            return Arrays.asList(String.valueOf(arr), admin);
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void updateMailingList(HostEntry host, String email, String flag, String owner, boolean archiveWebAccess, String descr) throws Exception {
        PreparedStatement pstm;
        if (!C0004CP.isIrisEnabled()) {
            if (flag.indexOf("a") > -1 && flag.indexOf("i") == -1) {
                flag = flag + "i";
            } else if (flag.indexOf("A") > -1 && flag.indexOf("I") == -1) {
                flag = flag + "I";
            }
            List l = new ArrayList();
            l.add("-a mod -d " + getDomain(email) + " -n " + getUsername(email) + " -f " + flag);
            if (!"".equals(owner)) {
                l.add(" -o " + owner);
            }
            l.add(" -e ");
            l.add(shellQuote(descr));
            l.add(" -w ");
            if (archiveWebAccess) {
                l.add("add");
            } else {
                l.add("rem");
            }
            host.exec("mlist-mod", l);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, update mailing list: " + email);
        } else {
            Connection con = getIrisConnection("Iris");
            PreparedStatement pstm2 = null;
            long adr_id = 0;
            long old_admin = 0;
            try {
                long pat_id = getIrisPattern_id(email, false);
                if (pat_id > 0) {
                    pstm2 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'List'");
                    pstm2.setLong(1, pat_id);
                    ResultSet rs = pstm2.executeQuery();
                    Session.closeStatement(pstm2);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        if (!owner.equals("")) {
                            String ownername = getUsername(owner);
                            String ownerdomain = getIrisDomain(owner);
                            String ownerhost = getIrisHost(owner, ownerdomain);
                            PreparedStatement pstm3 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                            pstm3.setString(1, ownername);
                            pstm3.setString(2, ownerhost);
                            pstm3.setString(3, ownerdomain);
                            ResultSet rs2 = pstm3.executeQuery();
                            Session.closeStatement(pstm3);
                            if (rs2.next()) {
                                adr_id = rs2.getLong(1);
                            } else {
                                PreparedStatement pstm4 = con.prepareStatement("INSERT INTO Addresses(username, host, domain) VALUES(?, ?, ?)");
                                pstm4.setString(1, ownername);
                                pstm4.setString(2, ownerhost);
                                pstm4.setString(3, ownerdomain);
                                pstm4.executeUpdate();
                                Session.closeStatement(pstm4);
                                PreparedStatement pstm5 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                                pstm5.setString(1, ownername);
                                pstm5.setString(2, ownerhost);
                                pstm5.setString(3, ownerdomain);
                                ResultSet rs3 = pstm5.executeQuery();
                                Session.closeStatement(pstm5);
                                if (rs3.next()) {
                                    adr_id = rs3.getLong(1);
                                } else {
                                    throw new Exception("Iris database error: cant' find " + owner + " in the Addresses table");
                                }
                            }
                        }
                        PreparedStatement pstm6 = con.prepareStatement("SELECT admin FROM MailingLists WHERE rule_id=?");
                        pstm6.setLong(1, rule_id);
                        ResultSet rs4 = pstm6.executeQuery();
                        Session.closeStatement(pstm6);
                        if (rs4.next()) {
                            old_admin = rs4.getLong(1);
                        }
                        if (old_admin > 0 && old_admin != adr_id) {
                            removeIrisAddress(rule_id, old_admin);
                        }
                        if (old_admin != adr_id) {
                            if (!owner.equals("")) {
                                pstm = con.prepareStatement("UPDATE MailingLists SET admin = ? WHERE rule_id = ?");
                                pstm.setLong(1, adr_id);
                                pstm.setLong(2, rule_id);
                                pstm.executeUpdate();
                            } else {
                                pstm = con.prepareStatement("UPDATE MailingLists SET admin=null WHERE rule_id=?");
                                pstm.setLong(1, rule_id);
                                pstm.executeUpdate();
                            }
                            Session.closeStatement(pstm);
                        }
                        con.close();
                        Connection con2 = getIrisConnection("IrisList");
                        char[] arr = flag.toCharArray();
                        String o_archive = arr[3] == 'a' ? "Y" : "N";
                        String o_private = arr[2] == 'h' ? "Y" : "N";
                        String o_moderated = arr[1] == 'm' ? "Y" : "N";
                        String o_members_only = arr[0] == 'O' ? "N" : "Y";
                        PreparedStatement pstm7 = con2.prepareStatement("UPDATE Options SET moderated = ?, private = ?, members_only = ?, archive = ? WHERE rule_id = ?");
                        pstm7.setString(1, o_moderated);
                        pstm7.setString(2, o_private);
                        pstm7.setString(3, o_members_only);
                        pstm7.setString(4, o_archive);
                        pstm7.setLong(5, rule_id);
                        pstm7.executeUpdate();
                        Session.closeStatement(pstm7);
                        if (old_admin > 0 && old_admin != adr_id) {
                            pstm7 = con2.prepareStatement("DELETE FROM Members WHERE rule_id = ? AND address_id = ?");
                            pstm7.setLong(1, rule_id);
                            pstm7.setLong(2, old_admin);
                            pstm7.executeUpdate();
                            Session.closeStatement(pstm7);
                        }
                        if (adr_id > 0 && old_admin != adr_id) {
                            pstm7 = con2.prepareStatement("INSERT INTO Members(rule_id, address_id, subscribed, admin) VALUES(?, ?, 'N', 'Y')");
                            pstm7.setLong(1, rule_id);
                            pstm7.setLong(2, adr_id);
                            pstm7.executeUpdate();
                            Session.closeStatement(pstm7);
                        }
                        if (pstm7 != null) {
                            Session.closeStatement(pstm7);
                        }
                        if (con2 != null) {
                            con2.close();
                            return;
                        }
                        return;
                    }
                }
                if (pstm2 != null) {
                    Session.closeStatement(pstm2);
                }
                if (con != null) {
                    con.close();
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void subscribeToMailingList(HostEntry host, String listEmail, String email) throws Exception {
        subscribeToMailingList(host, listEmail, email, false);
    }

    public static void subscribeToMailingList(HostEntry host, String listEmail, String email, boolean isModerate) throws Exception {
        long adr_id;
        String line;
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = new String[4];
            cmd[0] = getDomain(listEmail);
            cmd[1] = getUsername(listEmail);
            cmd[2] = email;
            cmd[3] = isModerate ? "/mod" : "";
            host.exec("mlist-sub", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, subscribe to mailing list: " + email);
        } else {
            String to_username = getUsername(email);
            String to_domain = getIrisDomain(email);
            String to_hostname = getIrisHost(email, to_domain);
            Connection con = getIrisConnection("Iris");
            PreparedStatement pstm = null;
            try {
                long pat_id = getIrisPattern_id(listEmail, false);
                if (pat_id > 0) {
                    pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm2 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                        pstm2.setString(1, to_username);
                        pstm2.setString(2, to_hostname);
                        pstm2.setString(3, to_domain);
                        Session.closeStatement(pstm2);
                        ResultSet rs2 = pstm2.executeQuery();
                        if (rs2.next()) {
                            adr_id = rs2.getLong(1);
                        } else {
                            PreparedStatement pstm3 = con.prepareStatement("INSERT INTO Addresses(username, host, domain) VALUES(?, ?, ?)");
                            pstm3.setString(1, to_username);
                            pstm3.setString(2, to_hostname);
                            pstm3.setString(3, to_domain);
                            pstm3.executeUpdate();
                            Session.closeStatement(pstm3);
                            PreparedStatement pstm4 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                            pstm4.setString(1, to_username);
                            pstm4.setString(2, to_hostname);
                            pstm4.setString(3, to_domain);
                            ResultSet rs3 = pstm4.executeQuery();
                            Session.closeStatement(pstm4);
                            if (rs3.next()) {
                                adr_id = rs3.getLong(1);
                            } else {
                                throw new Exception("Iris database error: cant' find " + email + " in the Addresses table");
                            }
                        }
                        if (!isModerate) {
                            PreparedStatement pstm5 = con.prepareStatement("SELECT COUNT(*) FROM Subscribers WHERE rule_id = ? AND address_id = ?");
                            pstm5.setLong(1, rule_id);
                            pstm5.setLong(2, adr_id);
                            ResultSet rs4 = pstm5.executeQuery();
                            Session.closeStatement(pstm5);
                            rs4.next();
                            if (rs4.getInt(1) == 0) {
                                PreparedStatement pstm6 = con.prepareStatement("INSERT INTO Subscribers(rule_id, address_id) VALUES(?, ?)");
                                pstm6.setLong(1, rule_id);
                                pstm6.setLong(2, adr_id);
                                pstm6.executeUpdate();
                                Session.closeStatement(pstm6);
                            } else {
                                throw new HSUserException("mailinglist.subscriberexists", new Object[]{email, listEmail});
                            }
                        }
                        con.close();
                        Connection con2 = getIrisConnection("IrisList");
                        if (isModerate) {
                            line = "INSERT INTO Members(rule_id, address_id, subscribed, admin) VALUES(?, ?, 'N', 'Y')";
                        } else {
                            line = "INSERT INTO Members(rule_id, address_id, subscribed, admin) VALUES(?, ?, 'Y', 'N')";
                        }
                        PreparedStatement pstm7 = con2.prepareStatement(line);
                        pstm7.setLong(1, rule_id);
                        pstm7.setLong(2, adr_id);
                        pstm7.executeUpdate();
                        Session.closeStatement(pstm7);
                        if (pstm7 != null) {
                            Session.closeStatement(pstm7);
                        }
                        if (con2 != null) {
                            con2.close();
                            return;
                        }
                        return;
                    }
                }
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void unsubscribeFromMailingList(HostEntry host, String listEmail, String email) throws Exception {
        unsubscribeFromMailingList(host, listEmail, email, false);
    }

    public static void unsubscribeFromMailingList(HostEntry host, String listEmail, String email, boolean isModerate) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = new String[4];
            cmd[0] = getDomain(listEmail);
            cmd[1] = getUsername(listEmail);
            cmd[2] = email;
            cmd[3] = isModerate ? "/mod" : "";
            host.exec("mlist-unsub", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, unsubscribe from mailing list: " + email);
        } else {
            String to_username = getUsername(email);
            String to_domain = getIrisDomain(email);
            String to_hostname = getIrisHost(email, to_domain);
            Connection con = getIrisConnection("Iris");
            PreparedStatement pstm = null;
            try {
                long pat_id = getIrisPattern_id(listEmail, false);
                if (pat_id > 0) {
                    pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        pstm = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                        pstm.setString(1, to_username);
                        pstm.setString(2, to_hostname);
                        pstm.setString(3, to_domain);
                        ResultSet rs2 = pstm.executeQuery();
                        Session.closeStatement(pstm);
                        if (rs2.next()) {
                            long adr_id = rs2.getLong(1);
                            if (!isModerate) {
                                PreparedStatement pstm2 = con.prepareStatement("DELETE FROM Subscribers WHERE rule_id = ? AND address_id = ?");
                                pstm2.setLong(1, rule_id);
                                pstm2.setLong(2, adr_id);
                                pstm2.executeUpdate();
                                Session.closeStatement(pstm2);
                            }
                            Connection con_list = getIrisConnection("IrisList");
                            pstm = con_list.prepareStatement("DELETE FROM Members WHERE rule_id = ? AND address_id = ?");
                            pstm.setLong(1, rule_id);
                            pstm.setLong(2, adr_id);
                            pstm.executeUpdate();
                            Session.closeStatement(pstm);
                            if (con_list != null) {
                                con_list.close();
                            }
                            removeIrisAddress(rule_id, adr_id);
                            if (pstm != null) {
                                Session.closeStatement(pstm);
                            }
                            if (con != null) {
                                con.close();
                                return;
                            }
                            return;
                        }
                    }
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
            } finally {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
            }
        }
    }

    public static void unsubscribeAllFromMailingList(HostEntry host, String listEmail) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {getDomain(listEmail), getUsername(listEmail)};
            host.exec("mlist-unsuball", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, unsibscribe all from mailing list: " + listEmail);
        } else {
            Connection con = getIrisConnection("Iris");
            Connection con_list = getIrisConnection("IrisList");
            PreparedStatement pstm = null;
            try {
                long pat_id = getIrisPattern_id(listEmail, false);
                if (pat_id > 0) {
                    pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        pstm = con.prepareStatement("SELECT address_id FROM Subscribers WHERE rule_id = ?");
                        pstm.setLong(1, rule_id);
                        ResultSet rs2 = pstm.executeQuery();
                        Session.closeStatement(pstm);
                        while (rs2.next()) {
                            long adr_id = rs2.getLong(1);
                            PreparedStatement pstm2 = con.prepareStatement("DELETE FROM Subscribers WHERE rule_id = ? AND address_id = ?");
                            pstm2.setLong(1, rule_id);
                            pstm2.setLong(2, adr_id);
                            pstm2.executeUpdate();
                            Session.closeStatement(pstm2);
                            pstm = con_list.prepareStatement("DELETE FROM Members WHERE rule_id = ? AND address_id = ?");
                            pstm.setLong(1, rule_id);
                            pstm.setLong(2, adr_id);
                            pstm.executeUpdate();
                            Session.closeStatement(pstm);
                            removeIrisAddress(rule_id, adr_id);
                        }
                        if (con_list != null) {
                            return;
                        }
                        return;
                    }
                }
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
                if (con_list != null) {
                    con_list.close();
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + listEmail + " mailing list");
            } finally {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
                if (con_list != null) {
                    con_list.close();
                }
            }
        }
    }

    public static void putMailListTrailer(HostEntry host, String mailDomain, String localEmail, String trailerMsg) throws Exception {
        String[] cmd = {mailDomain, localEmail};
        StringBuffer sb = new StringBuffer();
        int lastInd = trailerMsg.length();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= lastInd) {
                break;
            }
            int curInd = trailerMsg.indexOf(13, i2);
            if (curInd >= 0) {
                sb.append(trailerMsg.substring(i2, curInd)).append('\n');
                if (curInd < lastInd && trailerMsg.charAt(curInd + 1) == '\n') {
                    i = curInd + 2;
                } else {
                    i = curInd + 1;
                }
            } else {
                sb.append(trailerMsg.substring(i2));
                if (!trailerMsg.endsWith("\n")) {
                    sb.append('\n');
                }
            }
        }
        host.exec("mlist-trailer-put", cmd, sb.toString());
    }

    public static String getMailListTrailer(HostEntry host, String mailDomain, String localEmail) throws Exception {
        String[] cmd = {mailDomain, localEmail};
        Iterator i = host.exec("mlist-trailer-get", cmd).iterator();
        if (i.hasNext()) {
            StringBuffer trailerMsg = new StringBuffer(i.next().toString());
            while (i.hasNext()) {
                trailerMsg.append('\n').append(i.next().toString());
            }
            return trailerMsg.toString();
        }
        return "";
    }

    public static void delMailListTrailer(HostEntry host, String mailDomain, String localEmail) throws Exception {
        String[] cmd = {mailDomain, localEmail};
        host.exec("mlist-trailer-del", cmd);
    }

    public static void createMailbox(HostEntry host, String email, String password, int quota) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {shellQuote(email.toString()), shellQuote(password), Integer.toString(quota)};
            host.exec("mbox-add", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, create mailbox: " + email);
        } else {
            Connection con = getIrisConnection("Iris");
            String username = getUsername(email);
            String domain = getIrisDomain(email);
            String hostname = getIrisHost(email, domain);
            try {
                long pat_id = getIrisPattern_id(username, hostname, domain, false);
                if (pat_id > 0) {
                    PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Discard'");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm2 = con.prepareStatement("SELECT COUNT(*) FROM Vacation WHERE rule_id = ?");
                        pstm2.setLong(1, rule_id);
                        ResultSet rs2 = pstm2.executeQuery();
                        Session.closeStatement(pstm2);
                        if (rs2.next()) {
                            int vac_cnt = rs2.getInt(1);
                            if (vac_cnt > 0) {
                                PreparedStatement pstm3 = con.prepareStatement("UPDATE Rules SET action = 'Store' WHERE rule_id = ?");
                                pstm3.setLong(1, rule_id);
                                pstm3.executeUpdate();
                                Session.closeStatement(pstm3);
                                PreparedStatement pstm4 = con.prepareStatement("INSERT INTO Auth (rule_id, password, allow_relay) VALUES(?, ?, 'Y' )");
                                pstm4.setLong(1, rule_id);
                                pstm4.setString(2, password);
                                pstm4.executeUpdate();
                                Session.closeStatement(pstm4);
                                if (pstm4 != null) {
                                    Session.closeStatement(pstm4);
                                }
                                if (con != null) {
                                    con.close();
                                    return;
                                }
                                return;
                            }
                        }
                    }
                    throw new Exception("Iris database error: record already exists for the " + email + " in the Patterns table");
                }
                PreparedStatement pstm5 = con.prepareStatement("INSERT INTO Patterns (username, host, domain) VALUES(?, ?, ?)");
                pstm5.setString(1, username);
                pstm5.setString(2, hostname);
                pstm5.setString(3, domain);
                pstm5.executeUpdate();
                Session.closeStatement(pstm5);
                long pat_id2 = getIrisPattern_id(username, hostname, domain, false);
                if (pat_id2 <= 0) {
                    throw new Exception("Iris database error");
                }
                PreparedStatement pstm6 = con.prepareStatement("INSERT INTO Rules (pattern_id, action, expiration, quota) VALUES(?, 'Store', '0000-00-00 00:00:00', ?)");
                pstm6.setLong(1, pat_id2);
                pstm6.setInt(2, quota);
                pstm6.executeUpdate();
                Session.closeStatement(pstm6);
                PreparedStatement pstm7 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Store'");
                pstm7.setLong(1, pat_id2);
                ResultSet rs3 = pstm7.executeQuery();
                Session.closeStatement(pstm7);
                if (rs3.next()) {
                    long rule_id2 = rs3.getLong(1);
                    PreparedStatement pstm8 = con.prepareStatement("INSERT INTO Auth (rule_id, password, allow_relay) VALUES(?, ?, 'Y')");
                    pstm8.setLong(1, rule_id2);
                    pstm8.setString(2, password);
                    pstm8.executeUpdate();
                    Session.closeStatement(pstm8);
                    if (pstm8 != null) {
                        Session.closeStatement(pstm8);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void deleteMailbox(HostEntry host, String email) throws Exception {
        deleteMailbox(host, email, false);
    }

    public static void deleteMailbox(HostEntry host, String email, boolean is_catch_all) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {shellQuote(email)};
            host.exec("mbox-del", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, delete mailbox: " + email);
        } else {
            Connection con = getIrisConnection("Iris");
            Connection con1 = getIrisConnection("IrisMail");
            try {
                long pat_id = getIrisPattern_id(email, is_catch_all);
                if (pat_id <= 0) {
                    throw new Exception("Iris database error: can't find the record in the Patterns table according to the " + email + " mailbox");
                }
                PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action IN ('Store', 'Discard')");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    PreparedStatement pstm2 = con.prepareStatement("SELECT COUNT(*) FROM Vacation WHERE rule_id = ?");
                    pstm2.setLong(1, rule_id);
                    ResultSet rs2 = pstm2.executeQuery();
                    Session.closeStatement(pstm2);
                    rs2.next();
                    if (rs2.getInt(1) == 0) {
                        PreparedStatement pstm3 = con.prepareStatement("DELETE FROM Auth WHERE rule_id = ?");
                        pstm3.setLong(1, rule_id);
                        pstm3.executeUpdate();
                        Session.closeStatement(pstm3);
                        PreparedStatement pstm4 = con.prepareStatement("DELETE FROM Rules WHERE rule_id = ?");
                        pstm4.setLong(1, rule_id);
                        pstm4.executeUpdate();
                        Session.closeStatement(pstm4);
                        PreparedStatement pstm5 = con.prepareStatement("DELETE FROM Patterns WHERE pattern_id = ?");
                        pstm5.setLong(1, pat_id);
                        pstm5.executeUpdate();
                        Session.closeStatement(pstm5);
                        cleanIrisUsage(rule_id);
                    } else {
                        PreparedStatement pstm6 = con.prepareStatement("UPDATE Rules SET action = 'Discard', quota = 0 WHERE rule_id = ?");
                        pstm6.setLong(1, rule_id);
                        pstm6.executeUpdate();
                        Session.closeStatement(pstm6);
                        PreparedStatement pstm7 = con.prepareStatement("DELETE FROM Auth WHERE rule_id = ?");
                        pstm7.setLong(1, rule_id);
                        pstm7.executeUpdate();
                        Session.closeStatement(pstm7);
                        if (is_catch_all) {
                            PreparedStatement pstm8 = con.prepareStatement("UPDATE Patterns SET username = ? WHERE pattern_id = ?");
                            pstm8.setString(1, getUsername(email));
                            pstm8.setLong(2, pat_id);
                            pstm8.executeUpdate();
                            Session.closeStatement(pstm8);
                        }
                    }
                    PreparedStatement pstm9 = con1.prepareStatement("SELECT message_id FROM Messages WHERE rule_id = ?");
                    pstm9.setLong(1, rule_id);
                    ResultSet rs3 = pstm9.executeQuery();
                    Session.closeStatement(pstm9);
                    while (rs3.next()) {
                        long msg_id = rs3.getLong(1);
                        PreparedStatement pstm10 = con1.prepareStatement("DELETE FROM Status WHERE message_id = ?");
                        pstm10.setLong(1, msg_id);
                        pstm10.executeUpdate();
                        Session.closeStatement(pstm10);
                        PreparedStatement pstm11 = con1.prepareStatement("DELETE FROM Errors WHERE  message_id = ?");
                        pstm11.setLong(1, msg_id);
                        pstm11.executeUpdate();
                        Session.closeStatement(pstm11);
                        pstm9 = con1.prepareStatement("DELETE FROM Messages WHERE  message_id = ?");
                        pstm9.setLong(1, msg_id);
                        pstm9.executeUpdate();
                        Session.closeStatement(pstm9);
                    }
                    if (pstm9 != null) {
                        Session.closeStatement(pstm9);
                    }
                    if (con != null) {
                        con.close();
                    }
                    if (con1 != null) {
                        con1.close();
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error: can't find the records in the Rules table according to the " + email + " mailbox");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                if (con1 != null) {
                    con1.close();
                }
                throw th;
            }
        }
    }

    public static void setDefaultMailbox(HostEntry host, String email) throws Exception {
        String username = getUsername(email);
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {getDomain(email), username};
            host.exec("mbox-setdef", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, set catch all for the mailbox: " + email);
        } else {
            String domain = getIrisDomain(email);
            String hostname = getIrisHost(email, domain);
            Connection con = getIrisConnection("Iris");
            try {
                PreparedStatement pstm = con.prepareStatement("UPDATE Patterns SET username = '*' WHERE username = ? AND host = ? AND domain = ?");
                pstm.setString(1, username);
                pstm.setString(2, hostname);
                pstm.setString(3, domain);
                pstm.executeUpdate();
                Session.closeStatement(pstm);
                if (con != null) {
                    con.close();
                }
            } catch (Throwable th) {
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void unsetDefaultMailbox(HostEntry host, String domain, String email) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domain};
            host.exec("mbox-unsetdef", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, unset catch all for the mailbox: " + email);
        } else {
            String iris_domain = getIrisDomain(email + "@" + domain);
            String hostname = getIrisHost(email + "@" + domain, iris_domain);
            PreparedStatement pstm = null;
            Connection con = getIrisConnection("Iris");
            try {
                pstm = con.prepareStatement("UPDATE Patterns SET username = ? WHERE username = '*' AND domain = ? AND host = ?");
                pstm.setString(1, email);
                pstm.setString(2, iris_domain);
                pstm.setString(3, hostname);
                pstm.executeUpdate();
                Session.closeStatement(pstm);
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
            } catch (Throwable th) {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void setMboxQuota(HostEntry host, String email, int quota) throws Exception {
        setMboxQuota(host, email, quota, false);
    }

    public static void setMboxQuota(HostEntry host, String email, int quota, boolean is_catch_all) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {shellQuote(email), Integer.toString(quota)};
            host.exec("mbox-quota", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, set quota for the mailbox: " + email);
        } else {
            int quota2 = quota * 1048576;
            Connection con = getIrisConnection("Iris");
            PreparedStatement pstm = null;
            try {
                long pat_id = getIrisPattern_id(email, is_catch_all);
                if (pat_id > 0) {
                    pstm = con.prepareStatement("UPDATE Rules SET quota = ? WHERE pattern_id = ?");
                    pstm.setInt(1, quota2);
                    pstm.setLong(2, pat_id);
                    pstm.executeUpdate();
                    if (con != null) {
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error");
            } finally {
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
                if (con != null) {
                    con.close();
                }
            }
        }
    }

    public static Collection getMboxQuota(HostEntry host, String email) throws Exception {
        return getMboxQuota(host, email, false);
    }

    public static Collection getMboxQuota(HostEntry host, String email, boolean is_catch_all) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {shellQuote(email)};
            return host.exec("mbox-quota-get", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, get quota for the mailbox: " + email);
            return Arrays.asList("0", "0");
        } else {
            Connection con = getIrisConnection("Iris");
            Connection con1 = getIrisConnection("IrisMail");
            try {
                long pat_id = getIrisPattern_id(email, is_catch_all);
                if (pat_id > 0) {
                    PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm2 = con1.prepareStatement("SELECT SUM(bytes) FROM Messages WHERE rule_id = ?");
                        pstm2.setLong(1, rule_id);
                        ResultSet rs2 = pstm2.executeQuery();
                        Session.closeStatement(pstm2);
                        if (rs2.next()) {
                            float usage = rs2.getFloat(1) / 1048576.0f;
                            NumberFormat form = NumberFormat.getInstance();
                            ((DecimalFormat) form).applyPattern("###,##0.00");
                            String s_usage = form.format(usage);
                            PreparedStatement pstm3 = con.prepareStatement("SELECT quota FROM Rules WHERE rule_id = ?");
                            pstm3.setLong(1, rule_id);
                            ResultSet rs3 = pstm3.executeQuery();
                            Session.closeStatement(pstm3);
                            if (rs3.next()) {
                                float quota = rs3.getFloat(1) / 1048576.0f;
                                ArrayList res = new ArrayList();
                                res.add(s_usage);
                                res.add(new Float(quota));
                                new String();
                                List asList = Arrays.asList(s_usage, String.valueOf(quota));
                                if (pstm3 != null) {
                                    Session.closeStatement(pstm3);
                                }
                                if (con != null) {
                                    con.close();
                                }
                                if (con1 != null) {
                                    con1.close();
                                }
                                return asList;
                            }
                        }
                    }
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailing list");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                if (con1 != null) {
                    con1.close();
                }
                throw th;
            }
        }
    }

    public static void setMboxPassword(HostEntry host, String email, String password) throws Exception {
        setMboxPassword(host, email, password, false);
    }

    public static void setMboxPassword(HostEntry host, String email, String password, boolean is_catch_all) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {shellQuote(email), shellQuote(password)};
            host.exec("mbox-passwd", cmd);
        } else if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, set password for the mailbox: " + email);
        } else {
            Connection con = getIrisConnection("Iris");
            try {
                long pat_id = getIrisPattern_id(email, is_catch_all);
                if (pat_id > 0) {
                    PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                    pstm.setLong(1, pat_id);
                    ResultSet rs = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    if (rs.next()) {
                        long rule_id = rs.getLong(1);
                        PreparedStatement pstm2 = con.prepareStatement("UPDATE Auth SET password = ? WHERE rule_id = ?");
                        pstm2.setString(1, password);
                        pstm2.setLong(2, rule_id);
                        pstm2.executeUpdate();
                        Session.closeStatement(pstm2);
                        if (pstm2 != null) {
                            Session.closeStatement(pstm2);
                        }
                        if (con != null) {
                            con.close();
                            return;
                        }
                        return;
                    }
                }
                throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + email + " mailbox");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
    }

    public static void discardIncommingMail(String email, String action, String mailobject) throws Exception {
        discardIncommingMail(email, action, mailobject, false);
    }

    public static void discardIncommingMail(String email, String action, String mailobject, boolean is_catch_all) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, discard incomming mail for the mailbox: " + email);
            return;
        }
        PreparedStatement pstm = null;
        Connection con = getIrisConnection("Iris");
        try {
            long pat_id = getIrisPattern_id(email, is_catch_all);
            if (pat_id > 0) {
                String action_str = "Forward";
                if (action.equals("1")) {
                    action_str = "Discard";
                } else if (mailobject.equals("mailbox")) {
                    action_str = "Store";
                }
                pstm = con.prepareStatement("UPDATE Rules SET action = ? WHERE pattern_id = ?");
                pstm.setString(1, action_str);
                pstm.setLong(2, pat_id);
                pstm.executeUpdate();
                if (con != null) {
                    return;
                }
                return;
            }
            throw new Exception("Iris database error: can't find the record in the Patterns table according to the " + email);
        } finally {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public static void createMailDomain(HostEntry host, String domainName, String password) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domainName, shellQuote(password)};
            host.exec("domain-add", cmd);
        }
    }

    public static void deleteMailDomain(HostEntry host, String domainName) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domainName};
            host.exec("domain-del", cmd);
        }
    }

    public static void suspendMailDomain(HostEntry host, String domainName) throws Exception {
        suspendMailDomain(host, domainName, false);
    }

    public static void suspendMailDomain(HostEntry host, String domainName, boolean is_catch_all) throws Exception {
        if (C0004CP.isIrisEnabled()) {
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, suspend mail : " + domainName);
                return;
            } else if (domainName.indexOf("@") > -1) {
                Connection con = getIrisConnection("Iris");
                PreparedStatement pstm = null;
                try {
                    long pat_id = getIrisPattern_id(domainName, is_catch_all);
                    if (pat_id > 0) {
                        pstm = con.prepareStatement("UPDATE Rules SET expiration = NOW() WHERE pattern_id = ?");
                        pstm.setLong(1, pat_id);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        if (con != null) {
                            return;
                        }
                        return;
                    }
                    throw new Exception("Iris database error: can't find the record in the Patterns table according to the " + domainName);
                } finally {
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                    }
                }
            } else {
                return;
            }
        }
        String[] cmd = {domainName};
        host.exec("domain-suspend", cmd);
    }

    public static void resumeMailDomain(HostEntry host, String domainName) throws Exception {
        resumeMailDomain(host, domainName, false);
    }

    public static void resumeMailDomain(HostEntry host, String domainName, boolean is_catch_all) throws Exception {
        if (C0004CP.isIrisEnabled()) {
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, resume mail : " + domainName);
                return;
            } else if (domainName.indexOf("@") > -1) {
                Connection con = getIrisConnection("Iris");
                PreparedStatement pstm = null;
                try {
                    long pat_id = getIrisPattern_id(domainName, is_catch_all);
                    if (pat_id > 0) {
                        pstm = con.prepareStatement("UPDATE Rules SET expiration='0000-00-00 00:00:00' WHERE pattern_id = ?");
                        pstm.setLong(1, pat_id);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        if (con != null) {
                            return;
                        }
                        return;
                    }
                    throw new Exception("Iris database error: can't find the record in the Patterns table according to the " + domainName);
                } finally {
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                    }
                }
            } else {
                return;
            }
        }
        String[] cmd = {domainName};
        host.exec("domain-resume", cmd);
    }

    public static void setDomainQuota(HostEntry host, String domainName, int quota) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domainName, Integer.toString(quota)};
            host.exec("mbox-quota", cmd);
        }
    }

    public static String shellQuote(String str) {
        StringBuffer sb = new StringBuffer("'");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case yafvsym.ELSE /* 39 */:
                case '\\':
                case '`':
                    sb.append('\\');
                    break;
            }
            sb.append(c);
        }
        return sb.append("'").toString();
    }

    static String getDomain(String email) {
        return email.substring(email.indexOf(64) + 1, email.length());
    }

    static String getIrisDomain(String email) throws Exception {
        String domain;
        String fulldomain = email.substring(email.indexOf(64) + 1, email.length());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT 1 FROM parent_child p, domains d WHERE d.name = ? AND d.id = p.child_id AND p.child_type = 35");
            ps.setString(1, fulldomain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                domain = fulldomain.substring(fulldomain.indexOf(46) + 1, fulldomain.length());
            } else {
                domain = fulldomain;
            }
            if (ps != null) {
                Session.closeStatement(ps);
            }
            if (con != null) {
                con.close();
            }
            return domain;
        } catch (Throwable th) {
            if (ps != null) {
                Session.closeStatement(ps);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    static String getIrisHost(String email, String domain) {
        String fulldomain = email.substring(email.indexOf(64) + 1, email.length());
        if (fulldomain.equals(domain)) {
            return "*";
        }
        return fulldomain.substring(0, fulldomain.indexOf(46));
    }

    static String getUsername(String email) {
        return email.substring(0, email.indexOf(64));
    }

    public static void createForward(String email, String forward) throws Exception {
        createForward(email, forward, false);
    }

    public static void createForward(String email, String forward, boolean is_catch_all) throws Exception {
        long rule_id;
        long adr_id;
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, create forward : " + email);
            return;
        }
        String from_username = getUsername(email);
        String from_domain = getIrisDomain(email);
        String from_hostname = getIrisHost(email, from_domain);
        String to_username = getUsername(forward);
        String to_domain = getIrisDomain(forward);
        String to_hostname = getIrisHost(forward, to_domain);
        Connection con = getIrisConnection("Iris");
        try {
            long pat_id = getIrisPattern_id(from_username, from_hostname, from_domain, is_catch_all);
            if (pat_id > 0) {
                PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Forward'");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    rule_id = rs.getLong(1);
                } else {
                    PreparedStatement pstm2 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Discard'");
                    pstm2.setLong(1, pat_id);
                    ResultSet rs2 = pstm2.executeQuery();
                    Session.closeStatement(pstm2);
                    if (rs2.next()) {
                        rule_id = rs2.getLong(1);
                        PreparedStatement pstm3 = con.prepareStatement("SELECT COUNT(*) FROM Auth WHERE rule_id = ?");
                        pstm3.setLong(1, rule_id);
                        ResultSet rs3 = pstm3.executeQuery();
                        Session.closeStatement(pstm3);
                        if (rs3.next()) {
                            if (rs3.getInt(1) == 0) {
                                PreparedStatement pstm4 = con.prepareStatement("UPDATE Rules SET action = 'Forward' WHERE rule_id = ?");
                                pstm4.setLong(1, rule_id);
                                pstm4.executeUpdate();
                                Session.closeStatement(pstm4);
                            }
                        } else {
                            throw new Exception("Iris database error: can't create mailforward " + email + ", because mailresource with such name already exists");
                        }
                    } else {
                        throw new Exception("Iris database error: can't create mailforward " + email + ", because mailresource with such name already exists");
                    }
                }
            } else if (is_catch_all) {
                throw new Exception("Iris database error: can't find the record in the Patterns table according to the " + email + " mailforward");
            } else {
                PreparedStatement pstm5 = con.prepareStatement("INSERT INTO Patterns (username, host, domain) VALUES(?, ?, ?)");
                pstm5.setString(1, from_username);
                pstm5.setString(2, from_hostname);
                pstm5.setString(3, from_domain);
                pstm5.executeUpdate();
                Session.closeStatement(pstm5);
                long pat_id2 = getIrisPattern_id(email, is_catch_all);
                if (pat_id2 < 0) {
                    throw new Exception("Iris database error: cant' find " + email + " in the Patterns table");
                }
                PreparedStatement pstm6 = con.prepareStatement("INSERT INTO Rules (pattern_id, action, expiration, quota) VALUES(?,'Forward', '0000-00-00 00:00:00', 0)");
                pstm6.setLong(1, pat_id2);
                pstm6.executeUpdate();
                Session.closeStatement(pstm6);
                PreparedStatement pstm7 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Forward'");
                pstm7.setLong(1, pat_id2);
                ResultSet rs4 = pstm7.executeQuery();
                Session.closeStatement(pstm7);
                if (rs4.next()) {
                    rule_id = rs4.getLong(1);
                } else {
                    throw new Exception("Iris database error: cant' find " + email + " in the Rules table");
                }
            }
            PreparedStatement pstm8 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
            pstm8.setString(1, to_username);
            pstm8.setString(2, to_hostname);
            pstm8.setString(3, to_domain);
            ResultSet rs5 = pstm8.executeQuery();
            Session.closeStatement(pstm8);
            if (rs5.next()) {
                adr_id = rs5.getLong(1);
            } else {
                PreparedStatement pstm9 = con.prepareStatement("INSERT INTO Addresses(username, host, domain) VALUES(?, ?, ?)");
                pstm9.setString(1, to_username);
                pstm9.setString(2, to_hostname);
                pstm9.setString(3, to_domain);
                pstm9.executeUpdate();
                Session.closeStatement(pstm9);
                PreparedStatement pstm10 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                pstm10.setString(1, to_username);
                pstm10.setString(2, to_hostname);
                pstm10.setString(3, to_domain);
                ResultSet rs6 = pstm10.executeQuery();
                Session.closeStatement(pstm10);
                if (rs6.next()) {
                    adr_id = rs6.getLong(1);
                } else {
                    throw new Exception("Iris database error: cant' find " + forward + " in the Addresses table");
                }
            }
            PreparedStatement pstm11 = con.prepareStatement("INSERT INTO Forward(rule_id, address_id) VALUES(?, ?)");
            pstm11.setLong(1, rule_id);
            pstm11.setLong(2, adr_id);
            pstm11.executeUpdate();
            if (pstm11 != null) {
                Session.closeStatement(pstm11);
            }
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void removeForward(String email, String forward) throws Exception {
        removeForward(email, forward, false);
    }

    public static void removeForward(String email, String forward, boolean is_catch_all) throws Exception {
        PreparedStatement pstm;
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, remove forward : " + email);
            return;
        }
        Connection con = getIrisConnection("Iris");
        try {
            long pat_id = getIrisPattern_id(email, is_catch_all);
            if (pat_id == 0) {
                throw new Exception("Iris database error: cant' find " + email + " in the Patterns table");
            }
            PreparedStatement pstm2 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
            pstm2.setLong(1, pat_id);
            ResultSet rs = pstm2.executeQuery();
            Session.closeStatement(pstm2);
            if (rs.next()) {
                long rule_id = rs.getLong(1);
                String to_username = getUsername(forward);
                String to_domain = getIrisDomain(forward);
                String to_hostname = getIrisHost(forward, to_domain);
                PreparedStatement pstm3 = con.prepareStatement("SELECT address_id FROM Addresses WHERE username = ? AND host = ? AND domain = ?");
                pstm3.setString(1, to_username);
                pstm3.setString(2, to_hostname);
                pstm3.setString(3, to_domain);
                ResultSet rs2 = pstm3.executeQuery();
                Session.closeStatement(pstm3);
                if (rs2.next()) {
                    long adr_id = rs2.getLong(1);
                    PreparedStatement pstm4 = con.prepareStatement("DELETE FROM Forward WHERE rule_id = ? AND address_id = ?");
                    pstm4.setLong(1, rule_id);
                    pstm4.setLong(2, adr_id);
                    pstm4.executeUpdate();
                    Session.closeStatement(pstm4);
                    removeIrisAddress(rule_id, adr_id);
                    PreparedStatement pstm5 = con.prepareStatement("SELECT COUNT(*) FROM Vacation WHERE rule_id = ?");
                    pstm5.setLong(1, rule_id);
                    ResultSet rs3 = pstm5.executeQuery();
                    Session.closeStatement(pstm5);
                    rs3.next();
                    if (rs3.getInt(1) > 0) {
                        pstm = con.prepareStatement("UPDATE Rules SET action = 'Discard' WHERE rule_id = ?");
                        pstm.setLong(1, rule_id);
                        pstm.executeUpdate();
                    } else {
                        PreparedStatement pstm6 = con.prepareStatement("DELETE FROM Rules WHERE rule_id = ?");
                        pstm6.setLong(1, rule_id);
                        pstm6.executeUpdate();
                        Session.closeStatement(pstm6);
                        pstm = con.prepareStatement("DELETE FROM Patterns WHERE pattern_id = ?");
                        pstm.setLong(1, pat_id);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        cleanIrisUsage(rule_id);
                    }
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error: cant' find " + forward + " in the Addresses table");
            }
            throw new Exception("Iris database error: cant' find " + email + " in the Rules table");
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void createResponder(String responderEmail, String subj, String message, String is_exist_mb) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, create responder : " + responderEmail);
            return;
        }
        String username = getUsername(responderEmail);
        String domain = getIrisDomain(responderEmail);
        String hostname = getIrisHost(responderEmail, domain);
        Connection con = getIrisConnection("Iris");
        long pat_id = 0;
        try {
            if (is_exist_mb.equals("1") || is_exist_mb.equals("2")) {
                if (is_exist_mb.equals("1")) {
                    pat_id = getIrisPattern_id(username, hostname, domain, false);
                } else {
                    pat_id = getIrisPattern_id(username, hostname, domain, true);
                }
                if (pat_id <= 0) {
                    throw new Exception("Iris database error: can't find " + responderEmail + " in the Patterns table");
                }
            }
            if (pat_id > 0) {
                PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    PreparedStatement pstm2 = con.prepareStatement("INSERT INTO Vacation (rule_id, message, subject) VALUES( ?, ?, ? )");
                    pstm2.setLong(1, rule_id);
                    pstm2.setString(2, message);
                    pstm2.setString(3, subj);
                    pstm2.executeUpdate();
                    Session.closeStatement(pstm2);
                    if (pstm2 != null) {
                        Session.closeStatement(pstm2);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error: can't find " + responderEmail + " in the Rules table");
            }
            PreparedStatement pstm3 = con.prepareStatement("INSERT INTO Patterns (username, host, domain) VALUES( ?, ?, ?)");
            pstm3.setString(1, username);
            pstm3.setString(2, hostname);
            pstm3.setString(3, domain);
            pstm3.executeUpdate();
            Session.closeStatement(pstm3);
            PreparedStatement pstm4 = con.prepareStatement("SELECT pattern_id FROM Patterns WHERE username = ? AND host = ? AND domain = ?");
            pstm4.setString(1, username);
            pstm4.setString(2, hostname);
            pstm4.setString(3, domain);
            ResultSet rs2 = pstm4.executeQuery();
            Session.closeStatement(pstm4);
            if (rs2.next()) {
                long pat_id2 = rs2.getLong(1);
                PreparedStatement pstm5 = con.prepareStatement("INSERT INTO Rules (pattern_id, action, expiration, quota) VALUES(?, 'Discard', '0000-00-00 00:00:00', 0)");
                pstm5.setLong(1, pat_id2);
                pstm5.executeUpdate();
                Session.closeStatement(pstm5);
                PreparedStatement pstm6 = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ? AND action = 'Discard'");
                pstm6.setLong(1, pat_id2);
                ResultSet rs3 = pstm6.executeQuery();
                Session.closeStatement(pstm6);
                if (rs3.next()) {
                    long rule_id2 = rs3.getLong(1);
                    PreparedStatement pstm7 = con.prepareStatement("INSERT INTO Vacation (rule_id, message, subject) VALUES(?, ?, ?)");
                    pstm7.setLong(1, rule_id2);
                    pstm7.setString(2, message);
                    pstm7.setString(3, subj);
                    pstm7.executeUpdate();
                    Session.closeStatement(pstm7);
                    if (pstm7 != null) {
                        Session.closeStatement(pstm7);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
                throw new Exception("Iris database error: can't find " + responderEmail + " in the Rules table");
            }
            throw new Exception("Iris database error: can't find " + responderEmail + " in the Patterns table");
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void modifyResponder(String responderEmail, String subj, String message, String is_exist_mb) throws Exception {
        long pat_id;
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, update responder : " + responderEmail);
            return;
        }
        Connection con = getIrisConnection("Iris");
        try {
            if (is_exist_mb.equals("2")) {
                pat_id = getIrisPattern_id(responderEmail, true);
            } else {
                pat_id = getIrisPattern_id(responderEmail, false);
            }
            if (pat_id <= 0) {
                throw new Exception("Iris database error: can't find " + responderEmail + " in the Patterns table");
            }
            PreparedStatement pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
            pstm.setLong(1, pat_id);
            ResultSet rs = pstm.executeQuery();
            Session.closeStatement(pstm);
            if (rs.next()) {
                long rule_id = rs.getLong(1);
                PreparedStatement pstm2 = con.prepareStatement("UPDATE Vacation SET subject = ?, message = ? WHERE rule_id = ?");
                pstm2.setString(1, subj);
                pstm2.setString(2, message);
                pstm2.setLong(3, rule_id);
                pstm2.executeUpdate();
                if (pstm2 != null) {
                    Session.closeStatement(pstm2);
                }
                if (con != null) {
                    con.close();
                    return;
                }
                return;
            }
            throw new Exception("Iris database error: can't find " + responderEmail + " in the Rules table");
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void removeResponder(String responderEmail, String is_exist_mb) throws Exception {
        long pat_id;
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, remove responder : " + responderEmail);
            return;
        }
        Connection con = getIrisConnection("Iris");
        try {
            if (is_exist_mb.equals("2")) {
                pat_id = getIrisPattern_id(responderEmail, true);
            } else {
                pat_id = getIrisPattern_id(responderEmail, false);
            }
            if (pat_id <= 0) {
                throw new Exception("Iris database error: can't find " + responderEmail + " in the Patterns table");
            }
            PreparedStatement pstm = con.prepareStatement("SELECT rule_id, action FROM Rules WHERE pattern_id = ?");
            pstm.setLong(1, pat_id);
            ResultSet rs = pstm.executeQuery();
            Session.closeStatement(pstm);
            if (rs.next()) {
                long rule_id = rs.getLong(1);
                String action = rs.getString(2);
                PreparedStatement pstm2 = con.prepareStatement("DELETE FROM Vacation WHERE rule_id = ?");
                pstm2.setLong(1, rule_id);
                pstm2.executeUpdate();
                Session.closeStatement(pstm2);
                if (action.equals("Discard") && is_exist_mb.equals("0")) {
                    PreparedStatement pstm3 = con.prepareStatement("DELETE FROM Rules WHERE rule_id = ?");
                    pstm3.setLong(1, rule_id);
                    pstm3.executeUpdate();
                    Session.closeStatement(pstm3);
                    pstm2 = con.prepareStatement("DELETE FROM Patterns WHERE pattern_id = ?");
                    pstm2.setLong(1, pat_id);
                    pstm2.executeUpdate();
                    Session.closeStatement(pstm2);
                    cleanIrisUsage(rule_id);
                }
                if (pstm2 != null) {
                    Session.closeStatement(pstm2);
                }
                if (con != null) {
                    con.close();
                    return;
                }
                return;
            }
            throw new Exception("Iris database error: can't find " + responderEmail + " in the Rules table");
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static Hashtable getIrisMailListMessages(String mailDomain, String localEmail) throws Exception {
        Hashtable ht = new Hashtable();
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, get messages of the mailling list : " + localEmail);
            return ht;
        }
        Connection con = getIrisConnection("Iris");
        PreparedStatement pstm = null;
        try {
            long pat_id = getIrisPattern_id(localEmail + "@" + mailDomain, false);
            if (pat_id > 0) {
                pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    con.close();
                    con = getIrisConnection("IrisList");
                    pstm = con.prepareStatement("SELECT name, value FROM Text WHERE rule_id = ?");
                    pstm.setLong(1, rule_id);
                    ResultSet rs2 = pstm.executeQuery();
                    Session.closeStatement(pstm);
                    while (rs2.next()) {
                        StringBuffer res = new StringBuffer();
                        InputStream is = rs2.getAsciiStream("value");
                        BufferedReader in = new BufferedReader(new InputStreamReader(is));
                        while (true) {
                            String tmp = in.readLine();
                            if (tmp != null) {
                                res.append(tmp).append('\n');
                            }
                        }
                        in.close();
                        ht.put(rs2.getString(1), res.toString());
                    }
                }
                return ht;
            }
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + localEmail + " mailing list");
        } finally {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public static void updateIrisMailListMessage(String mailDomain, String localEmail, String text_name, String text_value, String mode) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, update message of the mailling list : " + localEmail);
            return;
        }
        Connection con = getIrisConnection("Iris");
        PreparedStatement pstm = null;
        try {
            long pat_id = getIrisPattern_id(localEmail + "@" + mailDomain, false);
            if (pat_id > 0) {
                pstm = con.prepareStatement("SELECT rule_id FROM Rules WHERE pattern_id = ?");
                pstm.setLong(1, pat_id);
                ResultSet rs = pstm.executeQuery();
                Session.closeStatement(pstm);
                if (rs.next()) {
                    long rule_id = rs.getLong(1);
                    con.close();
                    con = getIrisConnection("IrisList");
                    if (mode.equals("add")) {
                        pstm = con.prepareStatement("INSERT INTO Text (rule_id, name, value) VALUES(?, ?, ?)");
                        pstm.setLong(1, rule_id);
                        pstm.setString(2, text_name);
                        pstm.setString(3, text_value);
                    } else if (mode.equals("edit")) {
                        pstm = con.prepareStatement("UPDATE Text SET value = ? WHERE rule_id = ? AND name = ?");
                        pstm.setString(1, text_value);
                        pstm.setLong(2, rule_id);
                        pstm.setString(3, text_name);
                    } else {
                        pstm = con.prepareStatement("DELETE FROM Text WHERE rule_id = ? AND name = ?");
                        pstm.setLong(1, rule_id);
                        pstm.setString(2, text_name);
                    }
                    pstm.executeUpdate();
                    Session.closeStatement(pstm);
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
            }
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw new Exception("Iris database error: can't find the records in the Patterns and Rules tables according to the " + localEmail + " mailing list");
        } catch (Throwable th) {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static void createMailDomainAlias(HostEntry host, String domainAlias, String domainName) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domainAlias, domainName};
            Collection col = host.exec("maildomainalias", cmd);
            if (!col.isEmpty()) {
                throw new HSUserException("maildomainalias.exists", new Object[]{domainAlias});
            }
            return;
        }
        createForward("*@" + domainAlias, "*@" + domainName);
    }

    public static void deleteMailDomainAlias(HostEntry host, String domainAlias, String domainName) throws Exception {
        if (!C0004CP.isIrisEnabled()) {
            String[] cmd = {domainAlias};
            host.exec("maildomainalias", cmd);
            return;
        }
        removeForward("*@" + domainAlias, "*@" + domainName);
    }

    public static Connection getIrisConnection(String dbname) throws Exception {
        Class.forName("org.gjt.mm.mysql.Driver");
        String db_url = "jdbc:mysql://" + Session.getPropertyString("IRIS_DB_HOST") + "/" + dbname;
        Connection con = DriverManager.getConnection(db_url, Session.getPropertyString("IRIS_USER"), Session.getPropertyString("IRIS_PASS"));
        return con;
    }

    private static void removeIrisAddress(long rule_id, long adr_id) throws Exception {
        Connection con = getIrisConnection("Iris");
        try {
            PreparedStatement pstm = con.prepareStatement("SELECT COUNT(*) FROM Subscribers WHERE address_id = ? AND rule_id <> ?");
            pstm.setLong(1, adr_id);
            pstm.setLong(2, rule_id);
            ResultSet rs = pstm.executeQuery();
            Session.closeStatement(pstm);
            rs.next();
            if (rs.getInt(1) == 0) {
                PreparedStatement pstm2 = con.prepareStatement("SELECT COUNT(*) FROM Forward WHERE address_id=? AND rule_id<>?");
                pstm2.setLong(1, adr_id);
                pstm2.setLong(2, rule_id);
                ResultSet rs2 = pstm2.executeQuery();
                Session.closeStatement(pstm2);
                rs2.next();
                if (rs2.getInt(1) == 0) {
                    PreparedStatement pstm3 = con.prepareStatement("SELECT COUNT(*) FROM MailingLists WHERE admin=? AND rule_id<>?");
                    pstm3.setLong(1, adr_id);
                    pstm3.setLong(2, rule_id);
                    ResultSet rs3 = pstm3.executeQuery();
                    Session.closeStatement(pstm3);
                    rs3.next();
                    if (rs3.getInt(1) == 0) {
                        Connection con_list = getIrisConnection("IrisList");
                        PreparedStatement pstm4 = con_list.prepareStatement("SELECT COUNT(*) FROM Members WHERE address_id=? AND rule_id<>?");
                        pstm4.setLong(1, adr_id);
                        pstm4.setLong(2, rule_id);
                        ResultSet rs4 = pstm4.executeQuery();
                        Session.closeStatement(pstm4);
                        rs4.next();
                        if (rs4.getInt(1) == 0) {
                            pstm4 = con_list.prepareStatement("SELECT COUNT(*) FROM SubConfirm WHERE address_id=? AND rule_id<>?");
                            pstm4.setLong(1, adr_id);
                            pstm4.setLong(2, rule_id);
                            ResultSet rs5 = pstm4.executeQuery();
                            Session.closeStatement(pstm4);
                            rs5.next();
                            if (rs5.getInt(1) == 0) {
                                pstm4 = con.prepareStatement("DELETE FROM Addresses WHERE address_id=?");
                                pstm4.setLong(1, adr_id);
                                pstm4.executeUpdate();
                                Session.closeStatement(pstm4);
                            }
                        }
                        if (pstm4 != null) {
                            Session.closeStatement(pstm4);
                        }
                        if (con_list != null) {
                            con_list.close();
                        }
                    }
                }
            }
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    private static void cleanIrisList(long rule_id) throws Exception {
        PreparedStatement pstm = null;
        Connection con = getIrisConnection("IrisList");
        try {
            PreparedStatement pstm2 = con.prepareStatement("DELETE FROM Members WHERE rule_id=?");
            pstm2.setLong(1, rule_id);
            pstm2.executeUpdate();
            Session.closeStatement(pstm2);
            PreparedStatement pstm3 = con.prepareStatement("DELETE FROM Options WHERE rule_id=?");
            pstm3.setLong(1, rule_id);
            pstm3.executeUpdate();
            Session.closeStatement(pstm3);
            PreparedStatement pstm4 = con.prepareStatement("DELETE FROM SubConfirm WHERE rule_id=?");
            pstm4.setLong(1, rule_id);
            pstm4.executeUpdate();
            Session.closeStatement(pstm4);
            pstm = con.prepareStatement("DELETE FROM Text WHERE rule_id=?");
            pstm.setLong(1, rule_id);
            pstm.executeUpdate();
            Session.closeStatement(pstm);
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    private static void cleanIrisUsage(long rule_id) throws Exception {
        PreparedStatement pstm = null;
        Connection con = getIrisConnection("IrisUsage");
        try {
            PreparedStatement pstm2 = con.prepareStatement("DELETE FROM Incoming WHERE rule_id=?");
            pstm2.setLong(1, rule_id);
            pstm2.executeUpdate();
            Session.closeStatement(pstm2);
            pstm = con.prepareStatement("DELETE FROM Outgoing WHERE rule_id=?");
            pstm.setLong(1, rule_id);
            pstm.executeUpdate();
            Session.closeStatement(pstm);
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static String getIrisDomain(long rule_id) throws Exception {
        Connection con = getIrisConnection("Iris");
        try {
            PreparedStatement pstm = con.prepareStatement("SELECT pattern_id FROM Rules WHERE rule_id=?");
            pstm.setLong(1, rule_id);
            ResultSet rs = pstm.executeQuery();
            Session.closeStatement(pstm);
            if (rs.next()) {
                long pat_id = rs.getLong(1);
                pstm = con.prepareStatement("SELECT host, domain FROM Patterns WHERE pattern_id=?");
                pstm.setLong(1, pat_id);
                ResultSet rs2 = pstm.executeQuery();
                if (rs2.next()) {
                    if (rs2.getString(1).equals("*")) {
                        String string = rs2.getString(2);
                        if (pstm != null) {
                            Session.closeStatement(pstm);
                        }
                        if (con != null) {
                            con.close();
                        }
                        return string;
                    }
                    String str = rs2.getString(1) + "." + rs2.getString(2);
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                    }
                    return str;
                }
            }
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            throw new HSUserException("iris_rule.error", new Object[]{String.valueOf(rule_id)});
        } catch (Throwable th) {
            if (0 != 0) {
                Session.closeStatement(null);
            }
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public static long getIrisPattern_id(String username, String hostname, String domain, boolean is_catch_all) throws Exception {
        Connection con = getIrisConnection("Iris");
        PreparedStatement pstm = null;
        try {
            if (!is_catch_all) {
                pstm = con.prepareStatement("SELECT pattern_id FROM Patterns WHERE username=? AND domain=? AND host=?");
                pstm.setString(1, username);
                pstm.setString(2, domain);
                pstm.setString(3, hostname);
            } else {
                pstm = con.prepareStatement("SELECT pattern_id FROM Patterns WHERE username='*' AND domain=? AND host=?");
                pstm.setString(1, domain);
                pstm.setString(2, hostname);
            }
            ResultSet rs = pstm.executeQuery();
            Session.closeStatement(pstm);
            if (!rs.next()) {
            }
            long j = rs.getLong(1);
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
            return j;
        } finally {
            if (pstm != null) {
                Session.closeStatement(pstm);
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public static long getIrisPattern_id(String email, boolean is_catch_all) throws Exception {
        String username = getUsername(email);
        String domain = getIrisDomain(email);
        String hostname = getIrisHost(email, domain);
        return getIrisPattern_id(username, hostname, domain, is_catch_all);
    }

    public static void setAntiSpamPreferences(HostEntry host, String email, String requiredHits, String whitelist, String blacklist, String processing, String maxScore) throws Exception {
        String[] cmd = {email};
        String stdin = "";
        if (!requiredHits.equalsIgnoreCase("noedit")) {
            stdin = " required_hits=" + requiredHits;
        }
        if (!whitelist.equalsIgnoreCase("noedit")) {
            stdin = stdin + " whitelist=" + whitelist;
        }
        if (!blacklist.equalsIgnoreCase("noedit")) {
            stdin = stdin + " blacklist=" + blacklist;
        }
        if (!processing.equalsIgnoreCase("noedit")) {
            stdin = stdin + " processing=" + processing;
        }
        if (!maxScore.equalsIgnoreCase("noedit")) {
            stdin = stdin + " max_score=" + maxScore;
        }
        host.exec("spamassassin-set-prefs.pl", cmd, stdin);
    }

    public static void setAntiVirusPreferences(HostEntry host, String email, String processing) throws Exception {
        String[] cmd = {email};
        String stdin = "processing=" + processing;
        host.exec("antivirus-set-prefs.pl", cmd, stdin);
    }

    public static Collection getBadMimesList(HostEntry host, String email, String mimeMode) throws Exception {
        String allowVal;
        if (!"allow".equals(mimeMode) && !"deny".equals(mimeMode)) {
            throw new Exception("Wrong 'mimeMode' parameter. It should be 'allow' or 'deny'");
        }
        if ("allow".equals(mimeMode)) {
            allowVal = "1";
        } else {
            allowVal = "0";
        }
        String[] cmd = {email, allowVal};
        return host.exec("get-bad-mimes", cmd);
    }
}
