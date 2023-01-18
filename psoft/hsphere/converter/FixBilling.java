package psoft.hsphere.converter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.Domain;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/FixBilling.class */
public class FixBilling extends C0004CP {
    protected List pairs = new LinkedList();
    protected List xmlUsers = new LinkedList();
    protected static String source_path;
    protected static String log_path;
    protected static FileWriter test_log;
    protected String billName;

    public static void main(String[] argv) throws Exception {
        source_path = argv[0];
        log_path = argv[1];
        FixBilling test = new FixBilling();
        if (test.m32go()) {
            try {
                test_log.write("\nFixing finished.\n");
                test_log.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("\nFixing finished.");
            System.exit(0);
            return;
        }
        System.out.println("\nFixing failed.");
        System.exit(0);
    }

    /* renamed from: go */
    protected boolean m32go() throws Exception {
        boolean result;
        if (setLog()) {
            if (getDataFromXML()) {
                if (setBilling()) {
                    result = true;
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    protected boolean setLog() {
        boolean result;
        try {
            test_log = new FileWriter(log_path);
            result = true;
        } catch (IOException e) {
            System.out.println("Failed log settings.");
            result = false;
        }
        return result;
    }

    protected Hashtable getOneAccountFromDB(String domainName) throws Exception {
        Hashtable result = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("select id from domains where name=?");
                ps2.setString(1, domainName);
                ResultSet rs = ps2.executeQuery();
                long domain_id = 0;
                while (rs.next()) {
                    domain_id = rs.getLong("id");
                }
                rs.close();
                PreparedStatement ps3 = con.prepareStatement("select parent_id from parent_child where child_id=?");
                ps3.setLong(1, domain_id);
                ResultSet rs2 = ps3.executeQuery();
                long unixuser_id = 0;
                while (rs2.next()) {
                    unixuser_id = rs2.getLong("parent_id");
                }
                rs2.close();
                PreparedStatement ps4 = con.prepareStatement("select parent_id from parent_child where child_id=?");
                ps4.setLong(1, unixuser_id);
                ResultSet rs3 = ps4.executeQuery();
                long account_id = 0;
                while (rs3.next()) {
                    account_id = rs3.getLong("parent_id");
                }
                rs3.close();
                ps = con.prepareStatement("select user_id from user_account where account_id = ?");
                ps.setLong(1, account_id);
                ResultSet rs4 = ps.executeQuery();
                if (rs4.next()) {
                    long user_id = rs4.getLong("user_id");
                    User tmpUser = User.getUser(user_id);
                    String tmpLogin = tmpUser.getLogin();
                    result.put("accountId", new Long(account_id));
                    result.put("user_login", tmpLogin);
                    result.put("user_obj", tmpUser);
                    Account a = (Account) Account.get(new ResourceId(account_id, 0));
                    result.put("account_obj", a);
                    ResourceId unixuser = a.FM_findChild("unixuser");
                    if (unixuser != null) {
                        TemplateList domains = unixuser.FM_getChildren("domain");
                        while (domains.hasNext()) {
                            ResourceId oneDomain = domains.next();
                            Domain d = new Domain(oneDomain);
                            result.put("domain_obj", d);
                            result.put("domain_name", d.get("name").toString());
                        }
                    }
                }
                rs4.close();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.closeStatement(ps);
                con.close();
                e.printStackTrace();
                Session.closeStatement(ps);
                con.close();
            }
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean getDataFromXML() throws Exception {
        boolean result;
        logNormal("\n Get data from xml...");
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(source_path));
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName(FMACLManager.USER);
            for (int i = 0; i < list.getLength(); i++) {
                Hashtable oneUser = new Hashtable();
                Element oneUserElement = (Element) list.item(i);
                oneUser.put("login", oneUserElement.getAttribute("login"));
                NodeList accounts = oneUserElement.getElementsByTagName("account");
                for (int j = 0; j < accounts.getLength(); j++) {
                    Element oneAccount = (Element) accounts.item(j);
                    oneUser.put("startdate", oneAccount.getAttribute("startdate"));
                    oneUser.put("bpid", oneAccount.getAttribute("bpid"));
                    NodeList domains = oneAccount.getElementsByTagName("domain");
                    for (int k = 0; k < domains.getLength(); k++) {
                        Element oneDomain = (Element) domains.item(k);
                        oneUser.put("domain", oneDomain.getAttribute("name"));
                    }
                }
                this.xmlUsers.add(oneUser);
            }
            result = true;
        } catch (Exception e) {
            logFailed("Error parsing xml", e);
            result = false;
        }
        return result;
    }

    protected boolean setBilling() throws Exception {
        String periodType;
        int periodSize;
        int periodTypeId;
        logNormal("\nFix billing...");
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        for (int j = 0; j < this.xmlUsers.size(); j++) {
            Hashtable oneUserFromXML = (Hashtable) this.xmlUsers.get(j);
            String xmlDomain = (String) oneUserFromXML.get("domain");
            Hashtable oneUserFromDB = getOneAccountFromDB(xmlDomain);
            String dbDomain = (String) oneUserFromDB.get("domain_name");
            if (dbDomain != null && dbDomain.equals(xmlDomain)) {
                logNormal("    Domain = " + xmlDomain);
                User tmpUser = (User) oneUserFromDB.get("user_obj");
                Account tmpAccount = (Account) oneUserFromDB.get("account_obj");
                String new_startdate = (String) oneUserFromXML.get("startdate");
                int bpid = Integer.parseInt((String) oneUserFromXML.get("bpid"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
                Date d = dateFormat.parse(new_startdate, new ParsePosition(0));
                java.sql.Date startDate = new java.sql.Date(d.getTime());
                Session.setUser(tmpUser);
                Session.setAccount(tmpAccount);
                Connection con = Session.getDb();
                PreparedStatement ps = null;
                try {
                    try {
                        logNormal("    Set start date to " + startDate.toString() + "...");
                        ps = con.prepareStatement("UPDATE accounts SET created = ? WHERE id = ?");
                        ps.setDate(1, startDate);
                        ps.setLong(2, tmpAccount.getId().getId());
                        ps.executeUpdate();
                        Session.closeStatement(ps);
                        con.close();
                        try {
                            try {
                                logNormal("    Set new bpid to " + String.valueOf(bpid));
                                Plan tmpPlan = tmpAccount.getPlan();
                                setCredit(tmpAccount, 10000.0d);
                                tmpAccount.FM_changePaymentPeriod(bpid);
                                try {
                                    periodType = tmpPlan.getValue("_PERIOD_TYPE_" + bpid).toUpperCase();
                                    System.out.println("DEBUG: period type = " + periodType);
                                } catch (NullPointerException e) {
                                    periodType = "MONTH";
                                }
                                try {
                                    periodSize = Integer.parseInt(tmpPlan.getValue("_PERIOD_SIZE_" + bpid));
                                    System.out.println("DEBUG: period size = " + String.valueOf(periodSize));
                                } catch (NullPointerException e2) {
                                    periodSize = 1;
                                } catch (NumberFormatException e3) {
                                    periodSize = 1;
                                }
                                if (periodType.equals("DAY")) {
                                    periodTypeId = 5;
                                } else if (periodType.equals("WEEK")) {
                                    periodTypeId = 3;
                                } else if (periodType.equals("MONTH")) {
                                    periodTypeId = 2;
                                } else if (periodType.equals("YEAR")) {
                                    periodTypeId = 1;
                                } else {
                                    periodTypeId = 2;
                                }
                                System.out.println("DEBUG: Proshli calendar!");
                                Calendar next = TimeUtils.getCalendar(startDate);
                                next.add(periodTypeId, periodSize);
                                Date nextDate = next.getTime();
                                System.out.println("DEBUG: nextDate before = " + nextDate.toString());
                                Date today = TimeUtils.getDate();
                                while (nextDate.getTime() < today.getTime()) {
                                    next.setTime(nextDate);
                                    next.add(periodTypeId, periodSize);
                                    Date tmpDate = next.getTime();
                                    if (tmpDate.getTime() >= today.getTime()) {
                                        break;
                                    }
                                    nextDate = next.getTime();
                                    System.out.println("Motaem... " + nextDate.toString());
                                }
                                System.out.println("DEBUG: nextDate after = " + nextDate.toString());
                                if (nextDate.after(today)) {
                                    nextDate = startDate;
                                }
                                if (nextDate.before(today) && !nextDate.after(today)) {
                                    logNormal("    Set new billing period to " + nextDate.toString());
                                    tmpAccount.setNewPeriodBegin(nextDate);
                                    try {
                                        logNormal("    Set balance to 0...");
                                        addCredit(tmpAccount, "0");
                                    } catch (Exception e22) {
                                        logFailed("Failed to set balance to 0", e22);
                                    }
                                }
                                logNormal("End of billing fixing.\n");
                            } catch (Exception e1) {
                                logFailed("Failed to fix billing", e1);
                                return false;
                            }
                        } catch (Exception q) {
                            logFailed("Failed to set new period begin", q);
                            return false;
                        }
                    } catch (Throwable th) {
                        Session.closeStatement(ps);
                        con.close();
                        throw th;
                    }
                } catch (Exception ex) {
                    logFailed("Failed to set start date", ex);
                    Session.closeStatement(ps);
                    con.close();
                    return false;
                }
            }
        }
        Session.setAccount(oldAccount);
        Session.setUser(oldUser);
        return true;
    }

    protected void setCredit(Account a, double credit) throws Exception {
        a.getBill().setCredit(credit);
    }

    protected void addCredit(Account a, String balance) throws Exception {
        Date now = TimeUtils.getDate();
        Bill bill = a.getBill();
        double amount = -(Double.parseDouble(balance) - bill.getBalance());
        bill.addEntry(6, now, -1L, -1, "Initial balance adjustment", now, (Date) null, (String) null, amount);
        bill.setCredit(0.0d);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setLong(1, a.getId().getId());
                ps.setDouble(2, amount);
                ps.setString(3, "");
                ps.setString(4, "Starting balance");
                ps.setString(5, "OTHER");
                ps.setDate(6, new java.sql.Date(now.getTime()));
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                logFailed("Failed to set balance to 0", e);
                Session.closeStatement(ps);
                con.close();
            }
            a.initBill(now, true);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void logNormal(String s) throws IOException {
        System.out.println(s);
        test_log.write(s + "\n");
    }

    protected void logFailed(String s, Exception e) throws IOException {
        System.out.println(s);
        test_log.write(s + "\n");
        e.printStackTrace();
        e.printStackTrace(new PrintWriter((Writer) test_log, true));
    }
}
