package psoft.hsphere.migrator;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/HSphere1xInfoCollector.class */
public class HSphere1xInfoCollector {
    private Connection src;
    private Document result;
    private Element root;
    private String outFileName;
    private Hashtable cBpId = null;
    private Hashtable bpsInfo = null;

    public HSphere1xInfoCollector(String URL, String dbName, String userName, String passwd, String outFileName) throws Exception {
        this.src = null;
        this.outFileName = outFileName;
        try {
            Class.forName("postgresql.Driver");
            System.out.println("Postgree SQL driver class is OK");
            String dbURL = "jdbc:postgresql://" + URL + "/" + dbName;
            try {
                this.src = DriverManager.getConnection(dbURL, userName, passwd);
                System.out.println("Postgree SQL: Connection estabilished");
                this.result = new DocumentImpl();
                this.root = this.result.createElement("users");
            } catch (SQLException e) {
                System.out.println("Postgree SQL : can't establish connection because " + e.toString());
                throw e;
            }
        } catch (ClassNotFoundException e2) {
            System.out.println("Postgree SQL driver Unknown driver class");
            throw e2;
        }
    }

    public void loadUsers() throws Exception {
        PreparedStatement ps = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        try {
            try {
                ps = this.src.prepareStatement("SELECT a.user_id, a.username, a.firstname, a.lastname, a.email, a.plan, a.passwd, a.createdate, b.phone, b.address, b.city, b.state, b.zip, b.country, b.pay_type, b.credit_type, b.credit_num,b.credit_date, b.credit_name, c.discr_full, d.code FROM users a, billing_info b, plans c, billing_period d WHERE a.status <> ? AND a.user_id+0=b.user_id+0 AND b.del_date IS NULL AND a.plan+0=c.code+0 AND b.billing_code+0 = d.billing_code+0");
                ps.setInt(1, -2);
                ResultSet rs = ps.executeQuery();
                System.out.println("Query is done");
                while (rs.next()) {
                    Element user = this.result.createElement(FMACLManager.USER);
                    System.out.println("Processing user " + rs.getString("username"));
                    user.setAttribute("login", rs.getString("username").trim());
                    user.setAttribute("password", rs.getString("passwd").trim());
                    Element account = this.result.createElement("account");
                    account.setAttribute("plan", rs.getString("discr_full").trim());
                    account.setAttribute("startdate", dateFormat.format((Date) rs.getDate("createdate")));
                    Hashtable bp = (Hashtable) this.bpsInfo.get(rs.getString("code"));
                    account.setAttribute("bpid", (String) bp.get("bpid"));
                    Element bi = getInfoEl("_bi_", rs);
                    Element ci = getInfoEl("_ci_", rs);
                    account.appendChild(ci);
                    account.appendChild(bi);
                    appendDomain(rs.getInt("user_id"), account);
                    user.appendChild(account);
                    this.root.appendChild(user);
                }
                if (ps != null) {
                    ps.close();
                }
                this.src.close();
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.exit(-1);
                if (ps != null) {
                    ps.close();
                }
                this.src.close();
            }
            this.result.appendChild(this.root);
            OutputFormat format = new OutputFormat(this.result);
            format.setIndenting(true);
            XMLSerializer serializer = new XMLSerializer(new FileWriter(this.outFileName), format);
            serializer.serialize(this.result);
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            this.src.close();
            throw th;
        }
    }

    private void appendDomain(int userId, Element account) throws Exception {
        String domainName;
        PreparedStatement ps = null;
        try {
            try {
                ps = this.src.prepareStatement("SELECT domain, wwwhost, ip_adrfull FROM wwwdomains WHERE user_id=?");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if ("www".equals(rs.getString("wwwhost").trim())) {
                        domainName = rs.getString("domain");
                    } else {
                        domainName = rs.getString("wwwhost").trim() + "." + rs.getString("domain").trim();
                    }
                    Element domain = this.result.createElement("domain");
                    domain.setAttribute("name", domainName);
                    if (rs.getString("ip_adrfull") != null) {
                        domain.setAttribute("ip", rs.getString("ip_adrfull").trim());
                    }
                    Element mail = this.result.createElement("mailservice");
                    addMailBoxes(mail, userId, domainName);
                    addMailForwards(mail, userId, domainName);
                    domain.appendChild(mail);
                    account.appendChild(domain);
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.exit(-1);
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            throw th;
        }
    }

    private void addMailForwards(Element mail, int userId, String domain) throws Exception {
        PreparedStatement ps = null;
        try {
            try {
                ps = this.src.prepareStatement("SELECT fromname, forwards FROM forwards WHERE user_id = ? AND domain = ?");
                ps.setInt(1, userId);
                ps.setString(2, domain);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (!"*".equals(rs.getString("fromname").trim())) {
                        Element forward = this.result.createElement("forward");
                        Element subscriber = this.result.createElement("subscriber");
                        forward.setAttribute("name", rs.getString("fromname").trim());
                        subscriber.setAttribute("email", rs.getString("forwards").trim());
                        forward.appendChild(subscriber);
                        mail.appendChild(forward);
                    }
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.exit(-1);
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            throw th;
        }
    }

    private void addMailBoxes(Element mail, int userId, String domain) throws Exception {
        PreparedStatement ps = null;
        try {
            try {
                ps = this.src.prepareStatement("SELECT pop3int, pop3acc, passwd FROM pop3box WHERE user_id = ? AND domain =?");
                ps.setInt(1, userId);
                ps.setString(2, domain);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (!"webmaster".equals(rs.getString("pop3acc").trim())) {
                        Element box = this.result.createElement("mailbox");
                        box.setAttribute("name", rs.getString("pop3acc").trim());
                        box.setAttribute("password", rs.getString("passwd").trim());
                        addResponder(box, userId, rs.getString("pop3int").trim());
                        mail.appendChild(box);
                    }
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.exit(-1);
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            throw th;
        }
    }

    private void addResponder(Element box, int userId, String pop3int) throws Exception {
        PreparedStatement ps = null;
        try {
            try {
                ps = this.src.prepareStatement("SELECT email, mesg FROM autoanswer WHERE user_id = ? AND pop3int = ?");
                ps.setInt(1, userId);
                ps.setString(2, pop3int);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Element responder = this.result.createElement("autoresponder");
                    responder.appendChild(this.result.createTextNode(rs.getString("mesg").trim()));
                    box.appendChild(responder);
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.exit(-1);
                if (ps != null) {
                    ps.close();
                }
            }
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            throw th;
        }
    }

    private Element getInfoEl(String type, ResultSet rs) throws Exception {
        String year;
        Element i = this.result.createElement("info");
        i.setAttribute("prefix", type);
        Element item = this.result.createElement("item");
        item.setAttribute("name", "first_name");
        item.appendChild(this.result.createTextNode(rs.getString("firstname").trim()));
        i.appendChild(item);
        Element item2 = this.result.createElement("item");
        item2.setAttribute("name", "last_name");
        item2.appendChild(this.result.createTextNode(rs.getString("lastname").trim()));
        i.appendChild(item2);
        Element item3 = this.result.createElement("item");
        item3.setAttribute("name", "email");
        item3.appendChild(this.result.createTextNode(rs.getString("email").trim()));
        i.appendChild(item3);
        Element item4 = this.result.createElement("item");
        item4.setAttribute("name", "company");
        item4.appendChild(this.result.createTextNode(""));
        i.appendChild(item4);
        Element item5 = this.result.createElement("item");
        item5.setAttribute("name", "address1");
        item5.appendChild(this.result.createTextNode(rs.getString("address").trim()));
        i.appendChild(item5);
        Element item6 = this.result.createElement("item");
        item6.setAttribute("name", "city");
        item6.appendChild(this.result.createTextNode(rs.getString("city").trim()));
        i.appendChild(item6);
        Element item7 = this.result.createElement("item");
        item7.setAttribute("name", "state");
        item7.appendChild(this.result.createTextNode(rs.getString("state").trim()));
        i.appendChild(item7);
        Element item8 = this.result.createElement("item");
        item8.setAttribute("name", "postal_code");
        item8.appendChild(this.result.createTextNode(rs.getString("zip").trim()));
        i.appendChild(item8);
        Element item9 = this.result.createElement("item");
        item9.setAttribute("name", "country");
        item9.appendChild(this.result.createTextNode(rs.getString("country").trim()));
        i.appendChild(item9);
        Element item10 = this.result.createElement("item");
        item10.setAttribute("name", "phone");
        item10.appendChild(this.result.createTextNode(rs.getString("phone").trim()));
        i.appendChild(item10);
        if ("_bi_".equals(type)) {
            Element item11 = this.result.createElement("item");
            item11.setAttribute("name", "type");
            if (rs.getInt("pay_type") == 1) {
                item11.appendChild(this.result.createTextNode("CC"));
                i.appendChild(item11);
                Element item12 = this.result.createElement("item");
                item12.setAttribute("name", "cc_type");
                item12.appendChild(this.result.createTextNode(rs.getString("credit_type").trim()));
                i.appendChild(item12);
                Element item13 = this.result.createElement("item");
                item13.setAttribute("name", "cc_number");
                item13.appendChild(this.result.createTextNode(rs.getString("credit_num").trim()));
                i.appendChild(item13);
                Element item14 = this.result.createElement("item");
                item14.setAttribute("name", "cc_name");
                item14.appendChild(this.result.createTextNode(rs.getString("credit_name").trim()));
                i.appendChild(item14);
                StringTokenizer st = new StringTokenizer(rs.getString("credit_date").trim(), "/");
                String month = null;
                String str = null;
                while (true) {
                    year = str;
                    if (!st.hasMoreTokens()) {
                        break;
                    }
                    month = st.nextToken();
                    str = st.nextToken();
                }
                Element item15 = this.result.createElement("item");
                item15.setAttribute("name", "cc_exp_month");
                item15.appendChild(this.result.createTextNode(month));
                i.appendChild(item15);
                Element item16 = this.result.createElement("item");
                item16.setAttribute("name", "cc_exp_year");
                item16.appendChild(this.result.createTextNode(year));
                i.appendChild(item16);
            } else {
                item11.appendChild(this.result.createTextNode("Check"));
                i.appendChild(item11);
            }
        }
        return i;
    }

    private static void printHelpInfo() {
        System.out.println("Usage\njava.psoft.hsphere.migrator.HSphere1xInfoCollector -h FQDN/IP -d dbname -u dbusername -p passwd -f outfilename");
        System.out.println("-h, --host");
        System.out.println("\t FQDN or IP addres of the host which contains H-Sphere 1x database");
        System.out.println("-d, --database");
        System.out.println("\t Name of the H-Sphere 1x database");
        System.out.println("-u, --user");
        System.out.println("\t User name which will be used to connect to the H-Sphere 1x database");
        System.out.println("-p, --passwd");
        System.out.println("\t User password");
        System.out.println("-f, --file");
        System.out.println("\t filename in which information will be stored");
    }

    public static void main(String[] argv) throws Exception {
        String host = null;
        String dbName = null;
        String dbUName = null;
        String dbPasswd = null;
        String fileName = null;
        int i = 0;
        while (i < argv.length) {
            if ("-h".equals(argv[i]) || "--host".equals(argv[i])) {
                host = argv[i + 1];
                i++;
            }
            if ("-d".equals(argv[i]) || "--database".equals(argv[i])) {
                dbName = argv[i + 1];
                i++;
            }
            if ("-u".equals(argv[i]) || "--user".equals(argv[i])) {
                dbUName = argv[i + 1];
                i++;
            }
            if ("-p".equals(argv[i]) || "--passwd".equals(argv[i])) {
                dbPasswd = argv[i + 1];
                i++;
            }
            if ("-f".equals(argv[i]) || "--file".equals(argv[i])) {
                fileName = argv[i + 1];
                i++;
            }
            i++;
        }
        if (host == null || dbName == null || dbUName == null || dbPasswd == null || fileName == null) {
            printHelpInfo();
            return;
        }
        HSphere1xInfoCollector collector = new HSphere1xInfoCollector(host, dbName, dbUName, dbPasswd, fileName);
        collector.gatherPlanBillingPeriods();
        collector.loadUsers();
        collector.printBPInfo();
    }

    private void gatherPlanBillingPeriods() throws Exception {
        Hashtable cBpIds = new Hashtable();
        this.bpsInfo = new Hashtable();
        PreparedStatement ps1 = this.src.prepareStatement("SELECT code,discr_full FROM plans");
        ResultSet plans = ps1.executeQuery();
        while (plans.next()) {
            PreparedStatement ps = this.src.prepareStatement("SELECT code, real_bill, billing FROM billing_period WHERE plan = ?");
            ps.setInt(1, plans.getInt("code"));
            ResultSet bps = ps.executeQuery();
            while (bps.next()) {
                int bpid = 0;
                if (!cBpIds.containsKey(plans.getString("code"))) {
                    cBpIds.put(plans.getString("code"), new Integer(0));
                } else {
                    bpid = ((Integer) cBpIds.get(plans.getString("code"))).intValue() + 1;
                    cBpIds.put(plans.getString("code"), new Integer(bpid));
                }
                Hashtable bp = new Hashtable();
                bp.put("plan_id", plans.getString("code"));
                bp.put("length", bps.getString("real_bill"));
                bp.put("bpid", Integer.toString(bpid));
                bp.put("plan_name", plans.getString("discr_full"));
                this.bpsInfo.put(bps.getString("code"), bp);
            }
        }
    }

    private void printBPInfo() {
        System.out.println("plan_name\t\t\tbpId\tlength\tcode\tplan_id");
        Enumeration e = this.bpsInfo.keys();
        while (e.hasMoreElements()) {
            String code = (String) e.nextElement();
            Hashtable bp = (Hashtable) this.bpsInfo.get(code);
            System.out.println(((String) bp.get("plan_name")) + "\t\t\t" + ((String) bp.get("bpid")) + "\t" + ((String) bp.get("length")) + "\t" + code + "\t" + ((String) bp.get("plan_id")));
        }
    }
}
