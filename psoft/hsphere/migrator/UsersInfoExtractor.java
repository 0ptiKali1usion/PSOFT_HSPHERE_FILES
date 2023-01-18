package psoft.hsphere.migrator;

import freemarker.template.TemplateHashModel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import psoft.hsphere.Account;
import psoft.hsphere.ActiveBill;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.axis.Utils;
import psoft.hsphere.cron.CrontabItem;
import psoft.hsphere.migrator.extractor.FTPExtractor;
import psoft.hsphere.migrator.extractor.InfoExtractorUtils;
import psoft.hsphere.migrator.extractor.MSSQLExtractor;
import psoft.hsphere.migrator.extractor.MailServiceExtractor;
import psoft.hsphere.migrator.extractor.MySQLExtractor;
import psoft.hsphere.migrator.extractor.PostgreSQLExtractor;
import psoft.hsphere.migrator.extractor.WebServiceExtractor;
import psoft.hsphere.resource.Crontab;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.LinuxQuota;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.ODBC.DSNRecord;
import psoft.hsphere.resource.ODBC.ODBCService;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.UnixSubUserResource;
import psoft.hsphere.resource.UnixUserResource;
import psoft.hsphere.resource.WinQuota;
import psoft.hsphere.resource.admin.AdmDNSManager;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmInstantAlias;
import psoft.hsphere.resource.dns.CustomDNSRecord;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.dns.ServiceDNSZone;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.vps.VPSQuotaResource;
import psoft.hsphere.tools.ExternalCP;
import psoft.p000db.Database;
import psoft.util.Toolbox;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/UsersInfoExtractor.class */
public class UsersInfoExtractor extends ExternalCP {
    private User user = Session.getUser();
    private Account account = Session.getAccount();
    private Document document;
    private String file;
    private int action;
    private InfoExtractorUtils utils;
    private PrintStream oldStream;
    private boolean force;
    private static String usersDtd = "users.dtd";
    private static String resellersDtd = "resellers.dtd";
    private static String usersXMLFile = "migrate_users.xml";
    private static String resellersXMLFile = "migrate_resellers.xml";
    private static String errorsLog = "migration_errors.log";
    private static int USERS_INFO = 0;
    private static int RESELLERS_INFO = 1;

    public UsersInfoExtractor(int action, boolean force) throws Exception {
        String root;
        String dtd;
        String file;
        if (action == USERS_INFO) {
            root = "users";
            dtd = usersDtd;
            file = usersXMLFile;
            this.action = action;
        } else if (action == RESELLERS_INFO) {
            root = "resellers";
            dtd = resellersDtd;
            file = resellersXMLFile;
            this.action = RESELLERS_INFO;
        } else {
            throw new Exception("Unsupported action - " + String.valueOf(action));
        }
        this.document = createDocument(root, dtd);
        this.utils = new InfoExtractorUtils(this.document);
        this.force = force;
        this.file = file;
    }

    public Document getDocument() {
        return this.document;
    }

    public Node getRootElement(ArrayList resellers, ArrayList users) throws Exception {
        if (this.action == USERS_INFO) {
            if (users.size() < 1 && resellers.size() > 0) {
                ArrayList resellIds = getResellersList(resellers);
                Iterator iterator = resellIds.iterator();
                while (iterator.hasNext()) {
                    int resellerId = Integer.parseInt((String) iterator.next());
                    users.addAll(getUsersListForResellerBesidesAdmin(resellerId));
                }
            }
            return getUsers(users);
        } else if (this.action == RESELLERS_INFO) {
            return getResellers(resellers, users);
        } else {
            throw new Exception("Cannot get root document element");
        }
    }

    private static Document createDocument(String root, String dtd) throws Exception {
        DocumentType theDocType = new DocumentImpl().createDocumentType(root, (String) null, dtd);
        DocumentImpl impl = new DocumentImpl(theDocType);
        return impl;
    }

    private void serializeDocument() throws Exception {
        OutputFormat format = new OutputFormat(this.document, "utf-8", true);
        System.out.print("File " + this.file + " created.\n");
        FileOutputStream output = new FileOutputStream(this.file);
        XMLSerializer serial = new XMLSerializer(output, format);
        serial.serialize(this.document);
    }

    private void validateDocument(boolean validate) {
        try {
            InputSource input = new InputSource(new FileInputStream(this.file));
            DOMParser parser = new DOMParser();
            if (validate) {
                parser.setFeature("http://xml.org/sax/features/validation", true);
                parser.setErrorHandler(new DefaultHandler() { // from class: psoft.hsphere.migrator.UsersInfoExtractor.1
                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void error(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void fatalError(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void warning(SAXParseException e) {
                    }
                });
            }
            parser.parse(input);
            System.out.print("Document is correct\n");
        } catch (Exception exc) {
            System.out.print("Validation error: " + exc.getMessage());
            exc.printStackTrace();
        }
    }

    private void setOutputStream(PrintStream old, String errorFile) throws Exception {
        this.oldStream = old;
        PrintStream newStream = new PrintStream(new FileOutputStream(errorFile));
        System.setErr(newStream);
    }

    private void unsetOutputStream() {
        System.setOut(this.oldStream);
    }

    public static void main(String[] args) throws Exception {
        ArrayList users = new ArrayList();
        ArrayList resellers = new ArrayList();
        int info = USERS_INFO;
        int currParamIndex = 0;
        boolean force = false;
        if (args.length < 3) {
            System.out.print("\nCommand syntax :\njava psoft.hsphere.migrator.UsersInfoExtractor [<-force>] <action><params for action> ..\nforce - if avaliable, then ignore all errors in process migration\naction = -usersbynames -> params = users names\naction = -usersbylserver -> params = logical server id\naction = -resellersbynames -> params = resellers names\naction = -usersfromfile  -> params = file with users names\naction = -resellersfromfile  -> params = file with resellers names\naction = -resellers  - migrate_resellers.xml will be created to migrate resellers\naction = -users (by default) - migrate_users.xml will be created to migrate users\nExamples : java psoft.hsphere.migrator.UsersInfoExtractor -force -usersbynames user1 user2\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -resellersbynames reseller_user1\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -usersfromfile /home/users-names.txt\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -usersbylserver lserver_ID\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -resellersbynames res1 -usersbynames usr1\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -resellersbynames reseller_user1 -resellers\n           java psoft.hsphere.migrator.UsersInfoExtractor -force -resellersbynames reseller_user1 -users\n\n");
            System.out.print("Required - <action>, <params for action>\n");
            return;
        }
        while (currParamIndex < args.length) {
            String param = args[currParamIndex];
            if (param.equals("-usersbynames")) {
                currParamIndex = getNamesList(users, currParamIndex + 1, args);
            } else if (param.equals("-resellersbynames")) {
                currParamIndex = getNamesList(resellers, currParamIndex + 1, args);
            } else if (param.equals("-usersbylserver")) {
                int currParamIndex2 = currParamIndex + 1;
                getUsersList(users, Long.parseLong(args[currParamIndex2]));
                currParamIndex = currParamIndex2 + 1;
            } else if (param.equals("-usersfromfile")) {
                int currParamIndex3 = currParamIndex + 1;
                getNamesFromFile(users, args[currParamIndex3]);
                currParamIndex = currParamIndex3 + 1;
            } else if (param.equals("-resellersfromfile")) {
                int currParamIndex4 = currParamIndex + 1;
                getNamesFromFile(resellers, args[currParamIndex4]);
                currParamIndex = currParamIndex4 + 1;
            } else if (param.equals("-resellers")) {
                info = RESELLERS_INFO;
                currParamIndex++;
            } else if (param.equals("-users")) {
                info = USERS_INFO;
                currParamIndex++;
            } else if (param.equals("-force")) {
                force = true;
                currParamIndex++;
            } else {
                throw new Exception("Not supported action : " + param);
            }
        }
        if (info == RESELLERS_INFO && resellers.size() < 1) {
            throw new Exception("Resellers are not determined.");
        }
        UsersInfoExtractor extractor = new UsersInfoExtractor(info, force);
        extractor.setOutputStream(System.out, errorsLog);
        try {
            try {
                Document doc = extractor.getDocument();
                doc.appendChild(extractor.getRootElement(resellers, users));
                extractor.serializeDocument();
                extractor.validateDocument(true);
                extractor.restoreUser();
                extractor.restoreAccount();
            } catch (Exception exc) {
                exc.printStackTrace();
                extractor.restoreUser();
                extractor.restoreAccount();
            }
            extractor.unsetOutputStream();
        } catch (Throwable th) {
            extractor.restoreUser();
            extractor.restoreAccount();
            throw th;
        }
    }

    private void restoreUser() throws Exception {
        if (this.user != null) {
            Session.setUser(this.user);
        }
    }

    private void restoreAccount() throws Exception {
        if (this.account != null) {
            Session.setAccount(this.account);
        }
    }

    private static int getNamesList(ArrayList names, int index, String[] args) {
        while (index <= args.length - 1 && args[index].indexOf("-") != 0) {
            names.add(args[index]);
            index++;
        }
        return index;
    }

    private static void getUsersList(ArrayList params, long lserverId) throws Exception {
        try {
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            Database db = Toolbox.getDB(config);
            Connection con = db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT username FROM users ,user_account, parent_child, unix_user WHERE users.id=user_account.user_id  and user_account.account_id = parent_child.account_id and parent_child.child_type = ? and unix_user.id = parent_child.child_id and unix_user.hostid = ?");
            ps.setLong(1, 7L);
            ps.setLong(2, lserverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                if (username != null) {
                    params.add(username);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private static boolean isInSelectedUsers(ArrayList users, String resellUser) throws Exception {
        if (resellUser == null || users == null) {
            return false;
        }
        Iterator iterator = users.iterator();
        while (iterator.hasNext()) {
            String user = (String) iterator.next();
            if (resellUser.equals(user)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList getResellersList(ArrayList usersNames) throws Exception {
        ArrayList params = new ArrayList();
        try {
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            Database db = Toolbox.getDB(config);
            Connection con = db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT username, r.id FROM users u INNER JOIN resellers r ON u.id = r.id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                int resell_id = rs.getInt("id");
                if (isInSelectedUsers(usersNames, username)) {
                    params.add(Integer.toString(resell_id));
                }
            }
            return params;
        } catch (Exception exc) {
            throw exc;
        }
    }

    private static void getNamesFromFile(ArrayList names, String file) throws Exception {
        LineNumberReader strReader = new LineNumberReader(new FileReader(file));
        while (true) {
            String curStr = strReader.readLine();
            if (curStr != null) {
                try {
                    StringTokenizer tkz = new StringTokenizer(curStr);
                    if (tkz.countTokens() != 0) {
                        while (tkz.hasMoreTokens()) {
                            names.add(tkz.nextToken());
                        }
                    }
                } catch (Exception exc) {
                    throw exc;
                }
            } else {
                return;
            }
        }
    }

    private Node getUsers(ArrayList usersNames) throws Exception {
        Element node = this.utils.createNode("users");
        Iterator iterator = usersNames.iterator();
        while (iterator.hasNext()) {
            String user = (String) iterator.next();
            this.utils.appendChildNode(node, getUser(user));
        }
        return node;
    }

    private Element getUser(String userName) throws Exception {
        Element node = this.utils.createNode(FMACLManager.USER);
        User usr = null;
        HashSet accounts = null;
        try {
            usr = User.getUser(userName);
            Session.setUser(usr);
            accounts = usr.getAccountIds();
            long rid = usr.getResellerId();
            Reseller resell = Reseller.getReseller(rid);
            node.setAttributeNode(this.utils.createAttribute("login", usr.getLogin()));
            node.setAttributeNode(this.utils.createAttribute("password", usr.getPassword()));
            if (resell != null) {
                node.setAttributeNode(this.utils.createAttribute(FMACLManager.RESELLER, resell.getUser().toString()));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (accounts == null) {
            return null;
        }
        Iterator iterator = accounts.iterator();
        while (iterator.hasNext()) {
            ResourceId account = (ResourceId) iterator.next();
            this.utils.appendChildNode(node, getAccount(account, usr));
        }
        return this.utils.checkChildren(node);
    }

    private Element getAccount(ResourceId rid, User user) throws Exception {
        Element node = this.utils.createNode("account");
        Account account = null;
        try {
            account = (Account) Account.get(rid);
            Session.setAccount(account);
            node.setAttributeNode(this.utils.createAttribute("plan", account.getPlan().getDescription()));
            double balance = ((ActiveBill) account.get("bill")).getBalance();
            DecimalFormat balanceFormat = new DecimalFormat("#.##");
            node.setAttributeNode(this.utils.createAttribute("balance", balanceFormat.format(balance)));
            Date startDate = new Date(account.getCreated().getTime());
            DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            node.setAttributeNode(this.utils.createAttribute("startdate", format.format(startDate)));
            node.setAttributeNode(this.utils.createAttribute("bpid", account.get("periodId").toString()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (account != null) {
            getAccountInfo(node, account);
            getODBC(node, account);
            this.utils.appendChildNode(node, getLimits(account));
            this.utils.appendChildNode(node, getFtpSubAccounts(account));
            this.utils.appendChildNode(node, getCrontab(account));
            getDomains(node, account);
            MySQLExtractor mysql = new MySQLExtractor(this.document, this.force);
            this.utils.appendChildNode(node, mysql.getAllMySQLInfoAsXML(account));
            MSSQLExtractor mssql = new MSSQLExtractor(this.document, this.force);
            this.utils.appendChildNode(node, mssql.getAllMSSQLInfoAsXML(account));
            PostgreSQLExtractor postgresql = new PostgreSQLExtractor(this.document, this.force);
            this.utils.appendChildNode(node, postgresql.getAllPostgreSQLInfoAsXML(account));
        }
        return this.utils.checkChildren(node);
    }

    private void getODBC(Node parent, Account account) throws Exception {
        ResourceId rid;
        Element node = this.utils.createNode("odbc");
        try {
            ResourceId rid2 = account.FM_getChild("unixuser");
            if (rid2 == null || (rid = rid2.FM_getChild("odbc")) == null) {
                return;
            }
            ODBCService odbc = (ODBCService) rid.get();
            Collection<ResourceId> dsns = odbc.getId().findChildren("dsn_record");
            for (ResourceId dsnId : dsns) {
                if (rid != null) {
                    DSNRecord dsn = (DSNRecord) dsnId.get();
                    Hashtable params = dsn.getAllParams();
                    Element dsnRecord = this.utils.createNode("dsn_record");
                    String prefix = "";
                    try {
                        if (this.user.get("prefix") != null) {
                            prefix = this.user.get("prefix").toString();
                        }
                    } catch (Exception exc) {
                        if (!this.force) {
                            throw exc;
                        }
                    }
                    String DSN = params.get("DSN").toString();
                    dsnRecord.setAttributeNode(this.utils.createAttribute("dsn", prefix + DSN));
                    dsnRecord.setAttributeNode(this.utils.createAttribute("driver-name", params.get("driver-name").toString()));
                    Enumeration keys = params.keys();
                    while (keys.hasMoreElements()) {
                        String key = (String) keys.nextElement();
                        if (!key.equals("DSN") && !key.equals("driver-name")) {
                            String value = (String) params.get(key);
                            if (!value.trim().equals("")) {
                                Element driverParam = this.utils.createNode("driver_param");
                                driverParam.setAttributeNode(this.utils.createAttribute("name", key));
                                driverParam.setAttributeNode(this.utils.createAttribute("value", value));
                                dsnRecord.appendChild(driverParam);
                            }
                        }
                    }
                    node.appendChild(this.utils.checkChildren(dsnRecord));
                }
                parent.appendChild(this.utils.checkChildren(node));
            }
        } catch (Exception exc2) {
            exc2.printStackTrace();
            if (!this.force) {
                throw exc2;
            }
        }
    }

    private Element getFtpSubAccounts(Account account) throws Exception {
        ResourceId rid;
        Element node = this.utils.createNode("ftpsubaccounts");
        try {
            rid = account.FM_getChild("unixuser");
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (rid == null) {
            return null;
        }
        List<ResourceId> subusers = (List) rid.getChildHolder().getChildrenByName("unixsubuser");
        for (ResourceId subuserId : subusers) {
            this.utils.appendChildNode(node, getFtpSubAccount(subuserId));
        }
        return this.utils.checkChildren(node);
    }

    private Element getFtpSubAccount(ResourceId subuserId) throws Exception {
        Element node = this.utils.createNode("subaccount");
        try {
            UnixSubUserResource subuser = (UnixSubUserResource) subuserId.get();
            node.setAttribute("login", subuser.get("login").toString());
            String dir = subuser.get("dir").toString();
            node.setAttribute("homesuffix", getUnixHomeDirSuffix(dir));
            node.setAttribute("password", subuser.get("password").toString());
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private String getUnixHomeDirSuffix(String tmpDir) {
        while (tmpDir.endsWith("/")) {
            tmpDir = tmpDir.substring(0, tmpDir.length() - 1);
        }
        int ind = tmpDir.lastIndexOf(47);
        if (ind >= 0) {
            return tmpDir.substring(ind);
        }
        return "/" + tmpDir;
    }

    private Element getLimits(Account account) throws Exception {
        Element node = this.utils.createNode("limits");
        this.utils.appendChildNode(node, getQuota(account));
        this.utils.appendChildNode(node, getTraffic(account));
        return this.utils.checkChildren(node);
    }

    private Element getQuota(Account account) throws Exception {
        int quota;
        String quotaStr;
        Element quotaNode = null;
        try {
            try {
                Resource quotaRes = account.getId().findChild("quota").get();
                if (quotaRes instanceof LinuxQuota) {
                    LinuxQuota linux = (LinuxQuota) quotaRes;
                    quotaStr = linux.get("limitMb").toString();
                } else if (quotaRes instanceof WinQuota) {
                    WinQuota win = (WinQuota) quotaRes;
                    quotaStr = win.get("limitMb").toString();
                } else if (quotaRes instanceof VPSQuotaResource) {
                    VPSQuotaResource vps = (VPSQuotaResource) quotaRes;
                    quotaStr = vps.get("limitMb").toString();
                } else {
                    throw new Exception("Unknown quota class - " + quotaRes.getClass().toString());
                }
                try {
                    quota = Integer.parseInt(quotaStr);
                } catch (Exception e) {
                    quota = -1;
                }
            } catch (Exception e2) {
                throw new Exception("Not found quota for account - " + account.getId().getId());
            }
        } catch (Exception exc) {
            quota = -1;
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (quota > 0) {
            quotaNode = this.utils.createNode("quota", String.valueOf(quota));
        }
        return this.utils.checkChildren(quotaNode);
    }

    private Element getTraffic(Account account) throws Exception {
        double traff;
        Element trafficNode = null;
        try {
            try {
                Resource trafficRes = account.getId().findChild("traffic").get();
                Traffic traffic = (Traffic) trafficRes;
                traff = Double.parseDouble(traffic.get("traffic").toString());
            } catch (Exception e) {
                throw new Exception("Not found traffic for account -" + account.getId().getId());
            }
        } catch (Exception exc) {
            traff = -1.0d;
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (traff > 0.0d) {
            trafficNode = this.utils.createNode("traffic", String.valueOf(traff));
        }
        return this.utils.checkChildren(trafficNode);
    }

    private void getAccountInfo(Element parent, Account account) throws Exception {
        parent.appendChild(getContactInfo(account));
        parent.appendChild(getBillingInfo(account));
    }

    private void getDomains(Element parent, Account account) throws Exception {
        for (Domain domain : Utils.getDomainsByAcc(account).values()) {
            try {
                if (domain.getResourceType().getType().equals("domain") || domain.getResourceType().getType().equals("3ldomain") || domain.getResourceType().getType().equals("service_domain") || domain.getResourceType().getType().equals("parked_domain")) {
                    this.utils.appendChildNode(parent, getDomain(domain));
                }
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
        }
    }

    private void getDomainAliases(Element parent, Domain domain) throws Exception {
        Element node = this.utils.createNode("aliases");
        Collection<ResourceId> col = domain.getId().findChildren("domain_alias");
        for (ResourceId rid : col) {
            try {
                Resource res = rid.get();
                DomainAlias alias = (DomainAlias) res;
                this.utils.appendChildNode(node, getDomainAlias(alias, "dns_zone", "domain_alias"));
                this.utils.appendChildNode(parent, node);
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
            }
        }
    }

    private Element getInfoRecord(TemplateHashModel contact, String name, String field) throws Exception {
        String tagName = field;
        TemplateString templateString = contact.get(field);
        String val = templateString != null ? templateString.toString() : "";
        if (name != null) {
            tagName = name;
        }
        Element node = this.utils.createNode("item", val);
        node.setAttributeNode(this.utils.createAttribute("name", tagName));
        return this.utils.checkChildren(node);
    }

    private void getInfo(Element parent, TemplateHashModel contact) throws Exception {
        Object[] names = {"first_name", "last_name", "company", "address1", "city", "state", "postal_code", "country", "phone", "email", "type"};
        for (Object obj : names) {
            this.utils.appendChildNode(parent, getInfoRecord(contact, null, obj.toString()));
        }
    }

    private Element getContactInfo(Account account) throws Exception {
        Element node = this.utils.createNode("info");
        try {
            ContactInfoObject contact = account.getContactInfo();
            node.setAttributeNode(this.utils.createAttribute("prefix", "_ci_"));
            getInfo(node, contact);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private Element getBillingInfo(Account account) throws Exception {
        Element node = this.utils.createNode("info");
        try {
            BillingInfoObject billing = account.getBillingInfo();
            node.setAttributeNode(this.utils.createAttribute("prefix", "_bi_"));
            getInfo(node, billing);
            if (billing.getBillingTypeString().equals("CC")) {
                getPaymentInfo(node, billing);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private void getPaymentInfo(Element parent, BillingInfoObject billing) throws Exception {
        Object[] names = {"name", "type", "number", "exp_month", "exp_year"};
        for (int i = 0; i < names.length; i++) {
            this.utils.appendChildNode(parent, getInfoRecord((TemplateHashModel) billing.get("pi"), "cc_" + names[i].toString(), names[i].toString()));
        }
    }

    private Element getDomain(Domain domain) throws Exception {
        Element node = this.utils.createNode("domain");
        String zone = null;
        String domainType = domain.getId().getNamedType();
        try {
            node.setAttributeNode(this.utils.createAttribute("name", domain.getName()));
            if (!domainType.equals("parked_domain")) {
                String ip = ((MixedIPResource) domain.getId().findChild("ip").get()).toString();
                node.setAttributeNode(this.utils.createAttribute("ip", ip));
            }
            if (domainType.equals("domain")) {
                domainType = "transfer";
                zone = "dns_zone";
            } else if (domainType.equals("service_domain")) {
                domainType = "service";
                zone = "service_dns_zone";
            } else if (domainType.equals("3ldomain")) {
                domainType = "3ldomain";
                zone = "3l_dns_zone";
            } else if (domainType.equals("parked_domain")) {
                domainType = "parked_domain";
                zone = "dns_zone";
            }
            node.setAttributeNode(this.utils.createAttribute("type", domainType));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        MailServiceExtractor mail = new MailServiceExtractor(this.document, this.force);
        this.utils.appendChildNode(node, mail.getAllMailServiceInfoAsXML(domain));
        if (!domainType.equals("parked_domain")) {
            WebServiceExtractor web = new WebServiceExtractor(this.document, this.force);
            this.utils.appendChildNode(node, web.getAllWebServiceInfoAsXML(domain));
            FTPExtractor ftp = new FTPExtractor(this.document, this.force);
            this.utils.appendChildNode(node, ftp.getAllFTPInfoAsXML(domain));
            getSubdomains(node, domain.getAccount(), domain);
            getDomainAliases(node, domain);
        }
        this.utils.appendChildNode(node, getDNS(domain, zone));
        return this.utils.checkChildren(node);
    }

    private Element getCrontab(Account account) throws Exception {
        ResourceId rid;
        Element node = this.utils.createNode("crontab");
        try {
            rid = account.FM_getChild("unixuser");
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (rid == null) {
            return null;
        }
        Resource res = rid.get();
        if (res instanceof UnixUserResource) {
            UnixUserResource unixuser = (UnixUserResource) res;
            String login = unixuser.get("login").toString();
            ArrayList commands = Crontab.getCrontabEntries(login);
            boolean firstElement = true;
            Iterator i = commands.iterator();
            while (i.hasNext()) {
                CrontabItem item = (CrontabItem) i.next();
                Element commandNode = this.utils.createNode("command", item.getCommand());
                if (firstElement) {
                    node.setAttributeNode(this.utils.createAttribute("mailto", item.getMailto()));
                    firstElement = false;
                }
                this.utils.appendChildNode(node, commandNode);
            }
        }
        return this.utils.checkChildren(node);
    }

    private Element getDomainAlias(DomainAlias alias, String zone, String type) throws Exception {
        if (zone == null) {
            zone = "dns_zone";
        }
        Element node = this.utils.createNode("alias");
        try {
            node.setAttributeNode(this.utils.createAttribute("name", alias.getName()));
            ResourceId dnsRes = alias.getId().findChild(zone);
            if (dnsRes != null) {
                node.setAttributeNode(this.utils.createAttribute("dns", "1"));
                this.utils.appendChildNode(node, getDNS(alias, zone));
                MailServiceExtractor mailservice = new MailServiceExtractor(this.document, this.force);
                this.utils.appendChildNode(node, mailservice.getDomainAliasMailService(alias));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private Element getSubDomain(Domain domain) throws Exception {
        Element node = this.utils.createNode("subdomain");
        try {
            node.setAttributeNode(this.utils.createAttribute("name", domain.getName()));
            MailServiceExtractor mail = new MailServiceExtractor(this.document, this.force);
            this.utils.appendChildNode(node, mail.getAllMailServiceInfoAsXML(domain));
            WebServiceExtractor web = new WebServiceExtractor(this.document, this.force);
            this.utils.appendChildNode(node, web.getAllWebServiceInfoAsXML(domain));
            FTPExtractor ftp = new FTPExtractor(this.document, this.force);
            this.utils.appendChildNode(node, ftp.getAllFTPInfoAsXML(domain));
            getSubdomains(node, null, domain);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        getDomainAliases(node, domain);
        return this.utils.checkChildren(node);
    }

    private void getSubdomains(Element parent, Account account, Domain domain) throws Exception {
        Collection<ResourceId> col;
        if (account == null) {
            col = domain.getId().findChildren("subdomain");
        } else {
            col = account.getId().findChildren("subdomain");
        }
        for (ResourceId rid : col) {
            Domain currDomain = new Domain(rid);
            String currName = currDomain.get("name").toString();
            if (currName.indexOf("." + domain.getName()) > -1 && currName.indexOf("." + domain.getName()) == currName.indexOf(".")) {
                this.utils.appendChildNode(parent, getSubDomain(currDomain));
            }
        }
    }

    private Element getZone(ResourceId rid, String zone, String objectName) throws Exception {
        Element node = null;
        if (zone == null) {
        }
        try {
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (rid == null) {
            throw new Exception("Not found dns zone for " + objectName);
        }
        String encodedID = String.valueOf(rid.getAsString());
        node = getDNSZone(encodedID);
        return node;
    }

    private Element getDNS(Domain domain, String zone) throws Exception {
        Element node = null;
        try {
            ResourceId rid = domain.FM_getChild(zone);
            node = getZone(rid, zone, " domain - " + domain.getName());
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private Element getDNS(DomainAlias alias, String zone) throws Exception {
        Element node = null;
        try {
            ResourceId rid = alias.FM_getChild(zone);
            node = getZone(rid, zone, " alias - " + alias.getName());
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private Node getResellers(ArrayList resellers, ArrayList users) throws Exception {
        Element node = this.utils.createNode("resellers");
        ArrayList resellIds = getResellersList(resellers);
        Iterator iterator = resellIds.iterator();
        while (iterator.hasNext()) {
            int resellerId = Integer.parseInt((String) iterator.next());
            this.utils.appendChildNode(node, getReseller(resellerId));
        }
        return node;
    }

    private Element getReseller(int resellerId) throws Exception {
        Element node = this.utils.createNode(FMACLManager.RESELLER);
        try {
            try {
                Reseller resell = Reseller.getReseller(resellerId);
                Account account = resell.getAccount();
                User usr = User.getUser(resell.getUser());
                Session.setResellerId(resellerId);
                Session.setUser(usr);
                Session.setAccount(account);
                node.setAttributeNode(this.utils.createAttribute("login", account.get("login").toString()));
                node.setAttributeNode(this.utils.createAttribute("password", account.get("password").toString()));
                node.setAttributeNode(this.utils.createAttribute("plan", account.getPlan().get("description").toString()));
                this.utils.appendChildNode(node, getContactInfo(account));
                this.utils.appendChildNode(node, getBillingInfo(account));
                User admin = User.getUser(resell.getAdmin());
                HashSet hash = admin.getAccountIds();
                Session.setUser(admin);
                Account admAccount = null;
                Iterator iterator = hash.iterator();
                while (iterator.hasNext() && admAccount == null) {
                    ResourceId rid = (ResourceId) iterator.next();
                    Account admAcc = (Account) Account.get(rid);
                    if (admAcc.getPlan().isResourceAvailable(FMACLManager.ADMIN) != null) {
                        admAccount = admAcc;
                    }
                }
                this.utils.appendChildNode(node, getAdministrator(admAccount));
                getDNSZones(node, admAccount);
                restoreUser();
            } catch (Exception exc) {
                exc.printStackTrace();
                if (!this.force) {
                    throw exc;
                }
                restoreUser();
            }
            return node;
        } catch (Throwable th) {
            restoreUser();
            throw th;
        }
    }

    private static ArrayList getUsersListForResellerBesidesAdmin(long resellerId) throws Exception {
        ArrayList params = new ArrayList();
        try {
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            Database db = Toolbox.getDB(config);
            Connection con = db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT u.username FROM users u INNER JOIN resellers r ON (u.reseller_id = r.id) WHERE u.reseller_id = ? AND r.admin_id <> u.id ");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                if (username != null) {
                    params.add(username);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return params;
    }

    private Element getAdministrator(Account admin) throws Exception {
        Element node = this.utils.createNode("administrator");
        try {
            ContactInfoObject contact = admin.getContactInfo();
            node.setAttributeNode(this.utils.createAttribute("email", contact.get("email").toString()));
            node.setAttributeNode(this.utils.createAttribute("login", admin.get("login").toString()));
            node.setAttributeNode(this.utils.createAttribute("password", admin.get("password").toString()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private void getDNSZones(Element parent, Account admin) throws Exception {
        try {
            Session.setAccount(admin);
            ResourceId admDNSManagerId = admin.getId().findChild("adnsmanager");
            if (admDNSManagerId == null) {
                throw new Exception("Not found adnsmanager");
            }
            AdmDNSManager admDNSManager = (AdmDNSManager) admDNSManagerId.get();
            List<AdmDNSZone> dnsZones = admDNSManager.getDNSZones();
            for (AdmDNSZone zone : dnsZones) {
                this.utils.appendChildNode(parent, getDNSZone(zone));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
    }

    private String delZoneFromName(String name, String zone) throws Exception {
        if (name == null || zone == null) {
            return name;
        }
        int zoneIndex = name.indexOf("." + zone);
        if (zoneIndex >= 0) {
            StringBuffer buff = new StringBuffer(name);
            return buff.replace(zoneIndex, name.length(), "").toString();
        }
        return name;
    }

    private void getAllCustomDNSRecords(Element parent, DNSZone zone) throws Exception {
        Collection<ResourceId> custRecords = zone.getId().findChildren("cust_dns_record");
        for (ResourceId resourceId : custRecords) {
            CustomDNSRecord record = (CustomDNSRecord) resourceId.get();
            Element node = this.utils.createNode("record");
            String zonename = zone.get("name").toString();
            node.setAttributeNode(this.utils.createAttribute("name", delZoneFromName(record.get("name").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("type", delZoneFromName(record.get("type").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("ttl", delZoneFromName(record.get("ttl").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("data", delZoneFromName(record.get("data").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("pref", delZoneFromName(record.get("pref").toString(), zonename)));
            this.utils.appendChildNode(parent, node);
        }
    }

    private void getAllCustomDNSRecords(Element parent, ServiceDNSZone zone) throws Exception {
        Collection<ResourceId> custRecords = zone.getId().findChildren("cust_dns_record");
        for (ResourceId resourceId : custRecords) {
            CustomDNSRecord record = (CustomDNSRecord) resourceId.get();
            Element node = this.utils.createNode("record");
            String zonename = zone.get("name").toString();
            node.setAttributeNode(this.utils.createAttribute("name", delZoneFromName(record.get("name").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("type", delZoneFromName(record.get("type").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("ttl", delZoneFromName(record.get("ttl").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("data", delZoneFromName(record.get("data").toString(), zonename)));
            node.setAttributeNode(this.utils.createAttribute("pref", delZoneFromName(record.get("pref").toString(), zonename)));
            this.utils.appendChildNode(parent, node);
        }
    }

    private Element getDNSZone(String encodedZoneID) throws Exception {
        Element node = this.utils.createNode("dns");
        try {
            ResourceId rid = new ResourceId(encodedZoneID);
            Resource res = rid.get();
            if (res instanceof DNSZone) {
                DNSZone zone = (DNSZone) res;
                getAllCustomDNSRecords(node, zone);
            } else if (res instanceof ServiceDNSZone) {
                ServiceDNSZone zone2 = (ServiceDNSZone) res;
                getAllCustomDNSRecords(node, zone2);
            } else {
                throw new Exception("Unknown dns zone class - " + res.getClass().toString() + "\n");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private Element getDNSZone(AdmDNSZone zone) throws Exception {
        Element node = this.utils.createNode("zone");
        try {
            node.setAttributeNode(this.utils.createAttribute("name", zone.getName()));
            node.setAttributeNode(this.utils.createAttribute("email", zone.getEmail()));
            getInstantAliases(node, zone);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }

    private void getInstantAliases(Element parent, AdmDNSZone zone) throws Exception {
        for (AdmInstantAlias admInstantAlias : zone.getAliases()) {
            this.utils.appendChildNode(parent, getInstantAlias(admInstantAlias));
        }
    }

    private Element getInstantAlias(AdmInstantAlias admInstantAlias) throws Exception {
        Element node = this.utils.createNode("instantalias");
        try {
            node.setAttributeNode(this.utils.createAttribute("prefix", admInstantAlias.getPrefix()));
            node.setAttributeNode(this.utils.createAttribute("tag", String.valueOf(admInstantAlias.getTag())));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return this.utils.checkChildren(node);
    }
}
