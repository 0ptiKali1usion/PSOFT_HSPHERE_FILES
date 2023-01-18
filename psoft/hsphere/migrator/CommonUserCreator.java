package psoft.hsphere.migrator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.calc.Calc;
import psoft.hsphere.cron.CrontabItem;
import psoft.hsphere.migrator.creator.AbstractUserCreator;
import psoft.hsphere.migrator.creator.FTPCreator;
import psoft.hsphere.migrator.creator.MSSQLCreator;
import psoft.hsphere.migrator.creator.MigratedUsersContainer;
import psoft.hsphere.migrator.creator.PostgreSQLCreator;
import psoft.hsphere.migrator.creator.UserCreatorConfig;
import psoft.hsphere.migrator.creator.WebServiceCreator;
import psoft.hsphere.resource.Crontab;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.ODBC.ODBCService;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.UnixUserResource;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.apache.VirtualHostingResource;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.email.MailingList;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.hsphere.tools.ExternalCP;
import psoft.util.Base64;
import psoft.util.FakeRequest;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/CommonUserCreator.class */
public class CommonUserCreator extends AbstractUserCreator {
    private MigratedUsersContainer muc;
    private long quotaValue;
    private double trafficValue;
    private boolean isParkedDomain;
    private String parkedDomainName;
    public static final String LOG_DIR = "/var/log/hsphere";
    public static final String MIGRATION_DIR = "/tmp";

    public CommonUserCreator(UserCreatorConfig conf) throws Exception {
        super(conf);
        this.muc = null;
        this.quotaValue = 0L;
        this.trafficValue = 0.0d;
        this.isParkedDomain = false;
        this.parkedDomainName = "";
        if (C0004CP.getCP() == null) {
            new ExternalCP();
        }
    }

    public CommonUserCreator(UserCreatorConfig conf, MigratedUsersContainer muc) throws Exception {
        this(conf);
        this.muc = muc;
    }

    private void initMail() throws Exception {
        getCp().setConfig();
        getCp().initLog();
        if (this.conf.isMailActivated()) {
            try {
                outMessage("Initializing mail");
                Session.initMailer();
                outOK();
            } catch (Exception ex) {
                try {
                    outFail("Failed to initialize mail", ex);
                } catch (Exception ex1) {
                    Session.getLog().debug(ex1.getMessage(), ex1);
                }
            }
        }
    }

    private void setAdmin() throws Exception {
        User admin = User.getUser(FMACLManager.ADMIN);
        Account adm = admin.getAccount(new ResourceId(1L, 0));
        Session.setUser(admin);
        Session.setAccount(adm);
    }

    @Override // psoft.hsphere.migrator.creator.UserCreator
    public int execute() throws Exception {
        Element root = this.conf.getDocument().getDocumentElement();
        NodeList users = root.getElementsByTagName(FMACLManager.USER);
        if (this.conf.isPrintPlans()) {
            printNecessaryPlans(users, null);
            return 0;
        }
        initMail();
        setAdmin();
        int count = 0;
        for (int i = 0; i < users.getLength(); i++) {
            if (createUser((Element) users.item(i)) != null) {
                count++;
            }
        }
        return count;
    }

    public void execute(NodeList users) throws Exception {
        if (this.conf.isPrintPlans()) {
            printNecessaryPlans(users, null);
            return;
        }
        initMail();
        setAdmin();
        for (int i = 0; i < users.getLength(); i++) {
            createUser((Element) users.item(i));
        }
    }

    public void printNecessaryPlans(NodeList users, ArrayList notPrintableUsers) {
        for (int i = 0; i < users.getLength(); i++) {
            try {
                String userLogin = XPathAPI.selectNodeList(users.item(i), "self::user/@login").item(0).getNodeValue();
                NodeList userPlans = XPathAPI.selectNodeList(users.item(i), "self::user/account/@plan");
                if (notPrintableUsers == null || Arrays.binarySearch(notPrintableUsers.toArray(), userLogin) < 0) {
                    outMessage("\nPlans necessary for user - " + userLogin + " :\n");
                    for (int j = 0; j < userPlans.getLength(); j++) {
                        outMessage("\t" + userPlans.item(j).getNodeValue() + "\n");
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    @Override // psoft.hsphere.migrator.creator.UserCreator
    public User createUser(Element node) throws Exception {
        String login = node.getAttributes().getNamedItem("login").getNodeValue();
        String password = node.getAttributes().getNamedItem("password").getNodeValue();
        String resellerName = node.getAttributeNode(FMACLManager.RESELLER) != null ? node.getAttributes().getNamedItem(FMACLManager.RESELLER).getNodeValue() : "";
        if (this.muc != null) {
            this.currentUser = this.muc.getUser(login);
            login = this.currentUser.getUserName();
        }
        if (this.conf.isResumed() && !this.conf.getResumedUser().equals(login)) {
            outMessage("Skipping  user " + login + "\n");
            return null;
        }
        this.conf.setResumed(false);
        if (!"".equals(resellerName)) {
            try {
                outMessage("User belongs to reseller " + resellerName + ". Trying to set reseller ");
                setReseller(resellerName);
                outOK();
            } catch (Exception ex) {
                outFail("Failed to set reseller Id", ex);
                return null;
            }
        }
        try {
            if (this.conf.isClearUp()) {
                try {
                    outMessage("Clearing after user " + login);
                    User old = User.getUser(login);
                    clearUpUser(old);
                    old.delete();
                    outOK();
                    if (!"".equals(resellerName)) {
                        try {
                            outMessage("User belongs to reseller " + resellerName + ". Trying to set reseller ");
                            setReseller(resellerName);
                            outOK();
                        } catch (Exception ex2) {
                            outFail("Failed to set reseller Id", ex2);
                            this.conf.setClearUp(false);
                            return null;
                        }
                    }
                    this.conf.setClearUp(false);
                } catch (Exception ex3) {
                    outFail("Error during user deletion. Try to delete this user manualy", ex3);
                    this.conf.setClearUp(false);
                }
            }
            try {
                outMessage("Cheking user " + login + "... ");
                if (canBeCreated(node)) {
                    outOK();
                    User u = null;
                    User oldUser = Session.getUser();
                    try {
                        try {
                            outMessage("Creating user " + login);
                            User.createUser(login, password, Session.getResellerId());
                            u = User.getUser(login);
                            Session.setUser(u);
                            outOK();
                            try {
                                NodeList accounts = node.getElementsByTagName("account");
                                for (int i = 0; i < accounts.getLength(); i++) {
                                    this.quotaValue = 0L;
                                    this.trafficValue = 0.0d;
                                    addAccount(u, (Element) accounts.item(i));
                                }
                                if (u.getAccountIds().isEmpty()) {
                                    outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                                    u.delete();
                                }
                                Session.setUser(oldUser);
                            } catch (Exception ex4) {
                                outFail("Failed to create accounts, will delete user " + login + "\n", ex4);
                                clearUpUser(u);
                                if (u.getAccountIds().isEmpty()) {
                                    outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                                    u.delete();
                                }
                                Session.setUser(oldUser);
                            }
                            return u;
                        } catch (Throwable th) {
                            if (u.getAccountIds().isEmpty()) {
                                outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                                u.delete();
                            }
                            Session.setUser(oldUser);
                            throw th;
                        }
                    } catch (Exception ex5) {
                        outFail(ex5.getMessage(), ex5);
                        Session.setUser(oldUser);
                        if (this.conf.isForced()) {
                            return null;
                        }
                        throw new Exception("Unable to add user " + login);
                    }
                }
                return null;
            } catch (Exception ex6) {
                outFail(ex6.toString(), ex6);
                return null;
            }
        } catch (Throwable th2) {
            this.conf.setClearUp(false);
            throw th2;
        }
    }

    protected Account addAccount(User u, Element account) throws Exception {
        String planName;
        BillingInfoObject bi;
        Account a;
        Resource unixUser;
        NodeList limits;
        String startDate = "";
        String bpId = "";
        String balance = "";
        boolean suspended = false;
        Hashtable initval = new Hashtable();
        this.quotaValue = 0L;
        this.trafficValue = 0.0d;
        NodeList infos = account.getElementsByTagName("info");
        for (int i = 0; i < infos.getLength(); i++) {
            Element info = (Element) infos.item(i);
            String prefix = info.getAttributes().getNamedItem("prefix").getNodeValue();
            NodeList items = info.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) {
                Node item = items.item(j);
                String name = prefix + item.getAttributes().getNamedItem("name").getNodeValue();
                String value = "";
                if (item.getFirstChild() != null) {
                    value = item.getFirstChild().getNodeValue();
                }
                initval.put(name, new String[]{value});
            }
        }
        initval.put("login", new String[]{u.getLogin()});
        initval.put("password", new String[]{u.getPassword()});
        initval.put("type_domain", new String[]{"without_domain"});
        initval.put("_mod", new String[]{"nodomain"});
        Session.setRequest(new FakeRequest(initval));
        ((FakeRequest) Session.getRequest()).addParameter("signup_id", new String[]{Long.toString(SignupManager.saveRequest(Session.getRequest()))});
        if (account.getAttributeNode("plan") != null) {
            planName = account.getAttributes().getNamedItem("plan").getNodeValue();
        } else {
            planName = this.conf.getDefaultPlan();
        }
        if (this.muc != null) {
            this.currentUser = this.muc.getUser(u.getLogin());
            planName = this.currentUser.getPlanName();
        }
        outMessage("Resolving plan ID for plan " + planName);
        int planId = Plan.getPlanIdByName(planName);
        if (planId < 0) {
            outFail("Accont will not be created");
            return null;
        }
        Plan p = Plan.getPlan(planId);
        outOK();
        if (p.getBilling() == 0) {
            bi = new BillingInfoObject(0L);
        } else {
            bi = new BillingInfoObject(new NameModifier("_bi_"));
        }
        ContactInfoObject ci = new ContactInfoObject(new NameModifier("_ci_"));
        if (account.getAttributeNode("startdate") != null) {
            startDate = account.getAttributes().getNamedItem("startdate").getNodeValue();
        }
        if (account.getAttributeNode("bpid") != null) {
            bpId = account.getAttributes().getNamedItem("bpid").getNodeValue();
        }
        if (account.getAttributeNode("balance") != null) {
            balance = account.getAttributes().getNamedItem("balance").getNodeValue();
        }
        if (account.getAttributeNode("suspended") != null) {
            String suspend = account.getAttributes().getNamedItem("suspended").getNodeValue();
            try {
                if (Integer.valueOf(suspend).intValue() == 1) {
                    suspended = true;
                }
            } catch (NumberFormatException e) {
            }
        }
        outMessage("Adding account for " + u.getLogin());
        if (bpId.length() > 0) {
            if (Integer.parseInt(p.getValue("_PERIOD_TYPES")) - 1 < Integer.parseInt(bpId)) {
                outMessage("Wrong billing period id " + bpId + " Using default billing period\n");
                bpId = "0";
            }
            a = u.addAccount(planId, bi, ci, "Account", "empty", Integer.parseInt(bpId));
            outOK();
        } else {
            a = u.addAccount(planId, bi, ci, "Account", "empty", 0);
            outOK();
        }
        try {
            outMessage("Preparing balance ");
            a.getBill().setCredit(100000.0d);
            outOK();
        } catch (Exception ex) {
            outFail("Failed to prepare balance", ex);
        }
        if (p.isResourceAvailable("unixuser") != null) {
            unixUser = a.FM_getChild("unixuser").get();
        } else {
            unixUser = a;
        }
        NodeList limits2 = account.getElementsByTagName("limits");
        if (limits2 != null && limits2.getLength() > 0 && (limits = limits2.item(0).getChildNodes()) != null) {
            for (int i2 = 0; i2 < limits.getLength(); i2++) {
                Node limit = limits.item(i2);
                if (limit.getNodeName().equals("quota")) {
                    this.quotaValue = Long.parseLong(limit.getFirstChild().getNodeValue());
                } else if (limit.getNodeName().equals("traffic")) {
                    this.trafficValue = Double.parseDouble(limit.getFirstChild().getNodeValue());
                }
            }
        }
        setQuota(a, this.quotaValue);
        setTraffic(a, this.trafficValue);
        NodeList subaccounts = account.getElementsByTagName("ftpsubaccounts");
        Resource res = a.FM_getChild("unixuser").get();
        if (res instanceof UnixUserResource) {
            UnixUserResource unixuser = (UnixUserResource) res;
            addFTPSubAccounts(unixuser, (Element) subaccounts.item(0));
        }
        NodeList crontab = account.getElementsByTagName("crontab");
        ResourceId crontabId = a.FM_getChild("unixuser");
        if (crontabId != null) {
            Resource res2 = crontabId.get();
            if (res2 instanceof UnixUserResource) {
                UnixUserResource unixuser2 = (UnixUserResource) res2;
                addAccountCrontab(unixuser2, (Element) crontab.item(0));
            }
        }
        NodeList domains = account.getElementsByTagName("domain");
        if (domains != null) {
            for (int i3 = 0; i3 < domains.getLength(); i3++) {
                Element domain = (Element) domains.item(i3);
                try {
                    try {
                        addDomain(unixUser, domain);
                        this.isParkedDomain = false;
                        this.parkedDomainName = "";
                    } catch (Throwable th) {
                        this.isParkedDomain = false;
                        this.parkedDomainName = "";
                        throw th;
                    }
                } catch (Exception ex2) {
                    outMessage("Errors during domain creation\n");
                    if (!this.conf.isForced()) {
                        throw ex2;
                    }
                    this.isParkedDomain = false;
                    this.parkedDomainName = "";
                }
            }
        } else {
            outMessage("No domains found\n");
        }
        try {
            ResourceId rid = a.getId().findChild("unixuser");
            Resource unixuser3 = rid.get();
            Element odbc = (Element) account.getElementsByTagName("odbc").item(0);
            if (odbc != null) {
                ResourceId odbcId = unixuser3.addChild("odbc", "", null);
                outMessage("Adding dsn records for ODBC");
                NodeList records = odbc.getElementsByTagName("dsn_record");
                for (int i4 = 0; i4 < records.getLength(); i4++) {
                    Element dsnRecord = (Element) records.item(i4);
                    NamedNodeMap attrs = dsnRecord.getAttributes();
                    String dsn = attrs.getNamedItem("dsn").getNodeValue().toString();
                    String driverName = attrs.getNamedItem("driver-name").getNodeValue().toString();
                    ODBCService odbcService = (ODBCService) odbcId.get();
                    NodeList paramsNodes = dsnRecord.getElementsByTagName("driver_param");
                    Collection params = new ArrayList();
                    params.add("driver-name");
                    params.add(driverName);
                    params.add("DSN");
                    params.add(dsn);
                    for (int j2 = 0; j2 < paramsNodes.getLength(); j2++) {
                        NamedNodeMap paramAttrs = paramsNodes.item(j2).getAttributes();
                        params.add(paramAttrs.getNamedItem("name").getNodeValue());
                        params.add(paramAttrs.getNamedItem("value").getNodeValue());
                    }
                    odbcService.addChild("dsn_record", "", params);
                }
                outOK();
            }
        } catch (Exception e2) {
            outFail("Failed to add dsn records for ODBC", e2);
            if (!this.conf.isForced()) {
                throw e2;
            }
        }
        NodeList tmp = account.getElementsByTagName("mysql");
        Element mysql = tmp.getLength() > 0 ? (Element) tmp.item(0) : null;
        if (mysql != null) {
            addMySQLResources(a, mysql);
        }
        NodeList mstmp = account.getElementsByTagName("mssql");
        Element mssqlnode = mstmp.getLength() > 0 ? (Element) mstmp.item(0) : null;
        if (mssqlnode != null) {
            MSSQLCreator mssql = new MSSQLCreator(a, this);
            mssql.addMSSQL(mssqlnode);
        }
        NodeList pgtmp = account.getElementsByTagName("pgsql");
        Element pgsqlnode = pgtmp.getLength() > 0 ? (Element) pgtmp.item(0) : null;
        if (pgsqlnode != null) {
            PostgreSQLCreator postsql = new PostgreSQLCreator(a, this);
            postsql.addPostgreSQL(pgsqlnode);
        }
        if (startDate.length() > 0) {
            try {
                outMessage("Setting start billing date");
                setBillingPeriod(a, startDate, bpId.length() > 0 ? Integer.parseInt(bpId) : 0);
                outOK();
            } catch (Exception ex3) {
                Session.getLog().error("Failed to set start billing date", ex3);
                outFail("Failed to set start billing date", ex3);
            }
        }
        if (balance.length() > 0) {
            try {
                outMessage("Setting starting balance for account to " + balance);
                addCredit(a, balance);
                outOK();
            } catch (Exception ex4) {
                outFail("Error during setting balance", ex4);
                Session.getLog().error("Error during setting balance", ex4);
            }
        } else {
            try {
                outMessage("Setting starting balance for account to 0");
                addCredit(a, "0");
                outOK();
            } catch (Exception ex5) {
                outFail("Error during setting balance", ex5);
                Session.getLog().error("Error during setting balance", ex5);
            }
        }
        if (this.muc != null) {
            this.muc.setAccountMark(u.getLogin());
        }
        Session.getLog().debug("suspended = " + suspended);
        if (suspended) {
            Session.getLog().debug("Suspending account");
            a.suspend("Migrated as a suspended account");
        }
        return a;
    }

    protected void addAccountCrontab(UnixUserResource unixuser, Element crontabNode) throws Exception {
        if (crontabNode == null) {
            return;
        }
        try {
            outMessage("Adding crontab ");
            ResourceId crontabRid = unixuser.addChild("crontab", "", null);
            Crontab crontab = (Crontab) crontabRid.get();
            Crontab.Adder adder = crontab.getAdderForEmptyList();
            NodeList commands = crontabNode.getElementsByTagName("command");
            String mailto = crontabNode.getAttributes().getNamedItem("mailto").getNodeValue();
            for (int i = 0; i < commands.getLength(); i++) {
                Element command = (Element) commands.item(i);
                addCrontabCommand(adder, command, mailto);
            }
            crontab.FM_setMailTo(mailto);
            crontab.FM_saveLines();
            outOK();
        } catch (Exception exc) {
            outFail("Failed to add crontab", exc);
            Session.getLog().error("Failed to add crontab - ", exc);
        }
    }

    protected void addCrontabCommand(Crontab.Adder adder, Element command, String mailto) throws Exception {
        ArrayList list = new ArrayList();
        CrontabItem item = CrontabItem.instance(mailto, command.getFirstChild().getNodeValue());
        if (item != null) {
            list.add(item.getMinute());
            list.add(item.getHour());
            list.add(item.getDOM());
            list.add(item.getMonth());
            list.add(item.getDOW());
            list.add(item.getJob());
            adder.exec(list);
        }
    }

    protected void addFTPSubAccounts(UnixUserResource unixuser, Element ftpsubaccounts) throws Exception {
        if (ftpsubaccounts == null) {
            return;
        }
        NodeList subaccounts = ftpsubaccounts.getElementsByTagName("subaccount");
        for (int i = 0; i < subaccounts.getLength(); i++) {
            Element subaccount = (Element) subaccounts.item(i);
            addFTPSubAccount(unixuser, subaccount);
        }
    }

    protected void addFTPSubAccount(UnixUserResource unixuser, Element subaccount) throws Exception {
        List values = new ArrayList();
        try {
            NamedNodeMap attrs = subaccount.getAttributes();
            String login = attrs.getNamedItem("login").getNodeValue();
            String homeSuffix = attrs.getNamedItem("homesuffix").getNodeValue();
            if (homeSuffix != null && !homeSuffix.startsWith("/") && !homeSuffix.startsWith("\\")) {
                homeSuffix = "/" + homeSuffix;
            }
            values.add(login);
            values.add(homeSuffix);
            values.add(attrs.getNamedItem("password").getNodeValue());
            outMessage("Adding FTP subaccount - " + login);
            unixuser.addChild("unixsubuser", "", values);
            outOK();
        } catch (Exception ex) {
            outFail("Failed to add FTP subaccount - ", ex);
            Session.getLog().error("Failed to add FTP subaccount - ", ex);
        }
    }

    protected ResourceId addDomain(Resource u, Element domain) throws Exception {
        ResourceId rId;
        ResourceId virtualHosting;
        ResourceId dnsZone;
        Hashtable args = new Hashtable();
        String ip = "";
        String domainResourceType = "";
        String domainResourceMod = "";
        String domainName = domain.getAttributes().getNamedItem("name").getNodeValue();
        if (domain.getAttributes().getNamedItem("ip") != null) {
            ip = domain.getAttributes().getNamedItem("ip").getNodeValue();
        }
        String domainType = domain.getAttributes().getNamedItem("type").getNodeValue();
        args.put("domain_name", new String[]{domainName});
        args.put("password", new String[]{domainName});
        if ("transfer".equals(domainType)) {
            domainResourceType = "domain";
            if ("".equals(ip)) {
                domainResourceMod = "signup";
                outMessage("Adding domain " + domainName + " with shared IP");
            } else {
                domainResourceMod = "dedicated";
                if (!"dedicated".equals(ip)) {
                    args.put("ip", new String[]{ip});
                    if (!proceedIP(Long.parseLong(u.get("host_id").toString()), ip)) {
                        outFail("The import datata contains IP that can not be used " + ip);
                        return null;
                    }
                    outMessage("Adding domain " + domainName + " with dedicated IP " + ip);
                }
            }
        } else if ("service".equals(domainType)) {
            domainResourceType = "service_domain";
            domainResourceMod = "signup";
            outMessage("Adding service domain " + domainName + " with shared IP");
        } else if ("3ldomain".equals(domainType)) {
            domainResourceType = "3ldomain";
            domainResourceMod = "signup";
            outMessage("Adding third level domain " + domainName + " with shared IP");
        } else if ("parked_domain".equals(domainType)) {
            domainResourceType = "parked_domain";
            domainResourceMod = "parked";
            Element dnsNode = (Element) domain.getElementsByTagName("dns").item(0);
            NodeList custDNSRecords = dnsNode.getElementsByTagName("record");
            int i = 0;
            while (true) {
                if (i >= custDNSRecords.getLength()) {
                    break;
                }
                Element custDNSRecord = (Element) custDNSRecords.item(i);
                if (!custDNSRecord.getAttributes().getNamedItem("type").getNodeValue().equals("A") || !custDNSRecord.getAttributes().getNamedItem("name").getNodeValue().equals(domainName)) {
                    i++;
                } else {
                    ip = custDNSRecord.getAttributes().getNamedItem("data").getNodeValue();
                    break;
                }
            }
            if (!ip.equals("")) {
                args.put("domain_ip", new String[]{ip});
                this.isParkedDomain = true;
                this.parkedDomainName = domainName;
                outMessage("Adding parked domain " + domainName);
            } else {
                throw new Exception("Parked domain " + domainName + " doesn't have custom DNS A record");
            }
        }
        try {
            try {
                if (u instanceof WinUserResource) {
                    Collection col = new ArrayList();
                    col.add(domainName);
                    rId = u.addChild(domainResourceType, domainResourceMod, col);
                } else {
                    setRequest(args);
                    rId = u.addChild(domainResourceType, domainResourceMod, null);
                }
                outOK();
                clearRequest();
                if (rId != null) {
                    addMailService(rId.get(), (Element) domain.getElementsByTagName("mailservice").item(0));
                }
                Element webservice = (Element) domain.getElementsByTagName("webservice").item(0);
                WebServiceCreator webCreator = new WebServiceCreator(Session.getAccount(), domainName, this);
                webCreator.addWebService(webservice);
                Element ftp = (Element) domain.getElementsByTagName("ftp").item(0);
                FTPCreator ftpCreator = new FTPCreator(Session.getAccount(), domainName, this);
                ftpCreator.addFTP(ftp);
                if (rId != null) {
                    NodeList subdomains = domain.getElementsByTagName("subdomain");
                    for (int i2 = 0; i2 < subdomains.getLength(); i2++) {
                        Element subdomain = (Element) subdomains.item(i2);
                        if (domain.equals(subdomain.getParentNode())) {
                            addSubDomain(rId.get(), subdomain, domainName, domainName);
                        }
                    }
                }
                if (rId != null) {
                    Element aliases = null;
                    List aList = getElementsByName(domain, "aliases");
                    if (aList.size() > 0) {
                        aliases = (Element) aList.get(0);
                    }
                    if (aliases != null) {
                        outMessage("Got and processing domain aliases for domain " + domainName + "\n");
                        addDomainAliases(rId.get(), aliases);
                    }
                }
                if (rId != null) {
                    Element dns = null;
                    List l = getElementsByName(domain, "dns");
                    if (l.size() > 0) {
                        dns = (Element) l.get(0);
                    }
                    if (dns != null && (dnsZone = rId.get().get("dns_zone")) != null) {
                        outMessage("Got and processing custom DNS configuration for " + domainName + "\n");
                        addCustomDNS(dnsZone.get(), dns);
                    }
                }
                if (rId != null && (virtualHosting = rId.get().FM_getChild("hosting")) != null) {
                    Resource res = virtualHosting.get();
                    if (res instanceof VirtualHostingResource) {
                        System.out.println("Update Config");
                        ((VirtualHostingResource) res).FM_updateConfig();
                    }
                }
                return rId;
            } catch (Exception ex) {
                outFail("Failed to create domain " + domainName, ex);
                Session.getLog().error("Failed to create " + domainResourceType + " mod=" + domainResourceMod, ex);
                if (!this.conf.isForced()) {
                    throw ex;
                }
                clearRequest();
                return null;
            }
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    private boolean isAttributeOn(Node n, String attribute) {
        Node attr = n.getAttributes().getNamedItem(attribute);
        if (attr != null) {
            return "1".equals(attr.getNodeValue());
        }
        return false;
    }

    protected void addDomainAliases(Resource domain, Element aliasesEl) throws Exception {
        ResourceId dnsZone;
        if (aliasesEl == null) {
            outMessage("No domain aliases configuration found\n");
            return;
        }
        NodeList aliases = aliasesEl.getElementsByTagName("alias");
        for (int i = 0; i < aliases.getLength(); i++) {
            Element alias = (Element) aliases.item(i);
            String aliasName = alias.getAttributes().getNamedItem("name").getNodeValue();
            boolean dns = isAttributeOn(alias, "dns");
            boolean mail = isAttributeOn(alias, "mail");
            List args = new ArrayList();
            args.add(aliasName);
            try {
                outMessage("Creating domain alias " + aliasName + (dns ? " with dns zone" : ""));
                String mod = "";
                if (dns && mail) {
                    mod = "dns_n_mda";
                } else if (dns) {
                    mod = "with_dns";
                } else if (mail) {
                    mod = "with_mda";
                }
                ResourceId rId = domain.addChild("domain_alias", mod, args);
                outOK();
                if (rId != null) {
                    Element custDNS = null;
                    List l = getElementsByName(alias, "dns");
                    if (l.size() > 0) {
                        custDNS = (Element) l.get(0);
                    }
                    if (custDNS != null && (dnsZone = (ResourceId) rId.get().get("dns_zone")) != null) {
                        outMessage("Got and processing custom DNS configuration for " + aliasName + "\n");
                        addCustomDNS(dnsZone.get(), custDNS);
                    }
                }
                if (rId != null && alias.getElementsByTagName("mailservice").getLength() > 0) {
                    Element mailservice = (Element) alias.getElementsByTagName("mailservice").item(0);
                    addMailService(rId.get(), mailservice);
                }
            } catch (Exception ex) {
                outFail();
                outFail("Failed to create domain alias " + aliasName, ex);
                Session.getLog().error("Failed to create domain alias " + aliasName, ex);
                if (!this.conf.isForced()) {
                    throw ex;
                }
            }
        }
    }

    protected void addMailService(Resource u, Element mailService) throws Exception {
        if (mailService == null) {
            outMessage("No mailservice configuration found\n");
            return;
        }
        String catchAll = "";
        if (mailService.getAttributes().getNamedItem("catchall") != null) {
            catchAll = mailService.getAttributes().getNamedItem("catchall").getNodeValue();
        }
        ResourceId rid = u.getId().findChild("mail_service");
        if (rid == null) {
            List values = new ArrayList();
            rid = u.addChild("mail_service", "", values);
        }
        ResourceId mDomainId = rid.findChild("mail_domain");
        if (mDomainId == null) {
            return;
        }
        NodeList boxes = mailService.getElementsByTagName("mailbox");
        for (int i = 0; i < boxes.getLength(); i++) {
            addMailBox(mDomainId.get(), (Element) boxes.item(i));
        }
        NodeList responders = mailService.getElementsByTagName("autoresponder");
        for (int i2 = 0; i2 < responders.getLength(); i2++) {
            addAutoresponder(mDomainId.get(), (Element) responders.item(i2));
        }
        NodeList lists = mailService.getElementsByTagName("maillist");
        for (int i3 = 0; i3 < lists.getLength(); i3++) {
            addMailList(mDomainId.get(), (Element) lists.item(i3));
        }
        NodeList forwards = mailService.getElementsByTagName("forward");
        for (int i4 = 0; i4 < forwards.getLength(); i4++) {
            addMailForward(mDomainId.get(), (Element) forwards.item(i4));
        }
        if (catchAll.length() > 0) {
            setCatchAll(mDomainId, catchAll);
        }
    }

    protected void addMailBox(Resource mailDomain, Element box) throws Exception {
        Hashtable args = new Hashtable();
        if ("webmaster".equals(box.getAttribute("name"))) {
            MailDomain d = (MailDomain) mailDomain;
            Collection<ResourceId> col = d.getId().findChildren("mailbox");
            for (ResourceId resourceId : col) {
                Mailbox mb = (Mailbox) resourceId.get();
                if (mb.getEmail().equals("webmaster")) {
                    mb.FM_cdelete(0);
                }
            }
        }
        String mboxName = box.getAttributes().getNamedItem("name").getNodeValue();
        String password = box.getAttributes().getNamedItem("password").getNodeValue();
        String desc = "";
        if (box.getAttributes().getNamedItem("description") != null) {
            desc = box.getAttributes().getNamedItem("description").getNodeValue();
        }
        if (!desc.equals("FORWARD_ONLY")) {
            args.put("email", new String[]{mboxName});
            args.put("password", new String[]{password});
            args.put("description", new String[]{desc});
            setRequest(args);
            try {
                try {
                    outMessage("Creating mailbox " + mboxName);
                    mailDomain.addChild("mailbox", "import", null);
                    outOK();
                    clearRequest();
                } catch (Exception ex) {
                    outFail("Failed to create mailbox " + mboxName, ex);
                    Session.getLog().error("Failed to create mailbox " + mboxName, ex);
                    if (!this.conf.isForced()) {
                        throw ex;
                    }
                    clearRequest();
                    return;
                }
            } catch (Throwable th) {
                clearRequest();
                throw th;
            }
        }
        NodeList aliases = box.getElementsByTagName("mailalias");
        for (int i = 0; i < aliases.getLength(); i++) {
            Node alias = aliases.item(i);
            Node mbox = alias.getParentNode();
            String foreign = mbox.getAttributes().getNamedItem("name").getNodeValue();
            String local = alias.getAttributes().getNamedItem("name").getNodeValue();
            try {
                try {
                    outMessage("Adding mailbox alias " + local + " for mailbox - " + foreign);
                    Collection params = new ArrayList();
                    params.add(local);
                    params.add(foreign);
                    params.add("The mailbox alias");
                    mailDomain.addChild("mailbox_alias", "", params);
                    outOK();
                    clearRequest();
                } catch (Throwable th2) {
                    clearRequest();
                    throw th2;
                }
            } catch (Exception ex2) {
                Session.getLog().error("Failed to add alias " + mboxName, ex2);
                outFail("Failed to add alias " + mboxName, ex2);
                if (!this.conf.isForced()) {
                    throw ex2;
                }
                clearRequest();
            }
        }
    }

    private String getAttributeValue(NamedNodeMap attrs, String attrName) {
        if (attrs.getNamedItem(attrName) != null) {
            return attrs.getNamedItem(attrName).getNodeValue();
        }
        return null;
    }

    protected void addAutoresponder(Resource mailDomain, Element autoresponder) throws Exception {
        Hashtable args = new Hashtable();
        NamedNodeMap attrs = autoresponder.getAttributes();
        String message = autoresponder.getFirstChild().getNodeValue();
        String local = getAttributeValue(attrs, "local");
        String subject = getAttributeValue(attrs, "subject");
        args.clear();
        args.put("local", new String[]{local});
        args.put("foreign", new String[]{""});
        args.put("subject", new String[]{Base64.decodeToString(subject)});
        args.put("message", new String[]{Base64.decodeToString(message)});
        args.put("description", new String[]{"Autoresponder"});
        setRequest(args);
        try {
            try {
                outMessage("Adding responder ");
                mailDomain.addChild("responder", "import", null);
                outOK();
                clearRequest();
            } catch (Exception ex) {
                outFail(ex.getMessage(), ex);
                if (!this.conf.isForced()) {
                    throw ex;
                }
                clearRequest();
            }
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    protected void addModerators(Element mailList, ResourceId listId) throws Exception {
        String listName = mailList.getAttributes().getNamedItem("name").getNodeValue();
        NodeList moderators = mailList.getElementsByTagName("moderator");
        outMessage("Creating moderators to mailing list " + listName + "...\n");
        String moderator = null;
        for (int i = 0; i < moderators.getLength(); i++) {
            try {
                moderator = moderators.item(i).getAttributes().getNamedItem("email").getNodeValue();
                outMessage("Adding moderator - " + moderator);
                ((MailingList) listId.get()).FM_subscribeMod(moderator);
                outOK();
            } catch (Exception exc) {
                Session.getLog().error("Failed to add moderator - " + moderator, exc);
                outFail("Failed to add moderator - " + moderator, exc);
                if (!this.conf.isForced()) {
                    throw exc;
                }
            }
        }
    }

    protected void addSubscribers(Element mailList, ResourceId listId) throws Exception {
        String listName = mailList.getAttributes().getNamedItem("name").getNodeValue();
        NodeList subscribers = mailList.getElementsByTagName("subscriber");
        outMessage("Creating subscribers to mailing list " + listName + "...\n");
        String subscriber = null;
        for (int i = 0; i < subscribers.getLength(); i++) {
            try {
                subscriber = subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue();
                outMessage("Adding subscriber - " + subscriber);
                ((MailingList) listId.get()).FM_subscribe(subscriber);
                outOK();
            } catch (Exception exc) {
                Session.getLog().error("Failed to add subscriber - " + subscriber, exc);
                outFail("Failed to add subscriber - " + subscriber, exc);
                if (!this.conf.isForced()) {
                    throw exc;
                }
            }
        }
    }

    protected void addMessageTrailer(Element mailList, ResourceId listId) throws Exception {
        String listName = mailList.getAttributes().getNamedItem("name").getNodeValue();
        String trailer = null;
        try {
            NodeList nodes = mailList.getElementsByTagName("messagetrailer");
            if (nodes.getLength() != 0) {
                trailer = nodes.item(0).getFirstChild().getNodeValue();
                outMessage("Creating trailer to mailing list " + listName);
                ((MailingList) listId.get()).FM_put_trailer(trailer);
                outOK();
            }
        } catch (Exception exc) {
            Session.getLog().error("Failed to add mailing list trailer - " + trailer, exc);
            outFail("Failed to add mailing list trailer - " + trailer, exc);
            if (this.conf.isForced()) {
                return;
            }
            throw exc;
        }
    }

    protected void addMailList(Resource mailDomain, Element mailList) throws Exception {
        Hashtable args = new Hashtable();
        String listName = mailList.getAttributes().getNamedItem("name").getNodeValue();
        args.put("email", new String[]{listName});
        args.put("description", new String[]{"The mailing list"});
        setRequest(args);
        try {
            try {
                outMessage("Adding mailing list " + listName);
                ResourceId listId = mailDomain.addChild("mailing_list", "", null);
                outOK();
                clearRequest();
                addSubscribers(mailList, listId);
                addModerators(mailList, listId);
                addMessageTrailer(mailList, listId);
            } catch (Exception ex) {
                Session.getLog().error("Failed to add mailing list " + listName, ex);
                outFail("Failed to add mailing list " + listName, ex);
                if (!this.conf.isForced()) {
                    throw ex;
                }
                clearRequest();
            }
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    protected void addMailForward(Resource mailDomain, Element forward) throws Exception {
        Hashtable args = new Hashtable();
        ResourceId fId = null;
        String name = forward.getAttributes().getNamedItem("name").getNodeValue();
        NodeList subscribers = forward.getElementsByTagName("subscriber");
        for (int i = 0; i < subscribers.getLength(); i++) {
            if (i == 0) {
                String foreign = subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue();
                args.put("local", new String[]{name});
                args.put("subscriber", new String[]{foreign});
                args.put("description", new String[]{"The mail forward"});
                setRequest(args);
                try {
                    try {
                        outMessage("Creating mail forward " + name + "-->" + foreign);
                        fId = mailDomain.addChild("mail_forward", "import", null);
                        outOK();
                        clearRequest();
                    } catch (Throwable th) {
                        clearRequest();
                        throw th;
                    }
                } catch (Exception ex) {
                    outFail("Failed to add forward ", ex);
                    Session.getLog().error("Failed to add forward ", ex);
                    if (!this.conf.isForced()) {
                        throw ex;
                    }
                    clearRequest();
                    return;
                }
            } else {
                try {
                    outMessage("Adding forward subscriber");
                    ((MailForward) fId.get()).addSubscriber(subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue());
                    outOK();
                } catch (Exception ex2) {
                    outFail("Failed to add forward subscriber", ex2);
                    Session.getLog().error("Failed to add forward subscriber", ex2);
                    if (!this.conf.isForced()) {
                        throw ex2;
                    }
                }
            }
        }
    }

    private static boolean proceedIP(long hostId, String ip) throws Exception {
        System.out.println("");
        HostEntry he = HostManager.getHost(hostId);
        int res = he.checkIP(ip);
        switch (res) {
            case -1:
                if (HostEntry.checkAllIP(ip) == -1) {
                    LogicalServer lsrv = LogicalServer.get(hostId);
                    lsrv.FM_addIPRange(ip, ip, "255.255.255.0", 0);
                    System.out.println("\tCreating IP " + ip);
                    return true;
                }
                System.out.println("\tIP " + ip + " can not be created because it is busy 1");
                return false;
            case 0:
                System.out.println("\tIP " + ip + " already exist and will be used for domain creation");
                return true;
            default:
                System.out.println("\tIP " + ip + " can not be created because it is busy 2");
                return false;
        }
    }

    private void setQuota(Account a, long quotaValue) throws Exception {
        if (quotaValue == 0) {
            return;
        }
        try {
            outMessage("Setting new disk quota value " + quotaValue);
            Resource unixuser = a.FM_getChild("unixuser").get();
            Quota quota = (Quota) unixuser.FM_getChild("quota").get();
            quota.delete(true);
            ArrayList args = new ArrayList();
            args.add(new Long(quotaValue).toString());
            unixuser.addChild("quota", "", args);
            outOK();
        } catch (Exception ex) {
            Session.getLog().error("Failed to set quota ", ex);
            outFail("Failed to set quota ", ex);
            if (!this.conf.isForced()) {
                throw ex;
            }
        }
    }

    private void setTraffic(Account a, double trafficValue) throws Exception {
        if (trafficValue == 0.0d) {
            return;
        }
        try {
            outMessage("Setting new traffic value " + trafficValue);
            Traffic traffic = (Traffic) a.FM_getChild("traffic").get();
            traffic.delete(true);
            ArrayList args = new ArrayList();
            args.add(new Double(trafficValue).toString());
            a.addChild("traffic", "", args);
            outOK();
        } catch (Exception ex) {
            Session.getLog().error("Failed to set traffic ", ex);
            outFail("Failed to set traffic ", ex);
            if (!this.conf.isForced()) {
                throw ex;
            }
        }
    }

    private void setBillingPeriod(Account a, String pBegin, int bpId) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        Date d = dateFormat.parse(pBegin, new ParsePosition(0));
        java.sql.Date startDate = new java.sql.Date(d.getTime());
        Date today = TimeUtils.getDate();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE accounts SET created = ? WHERE id = ?");
                ps.setDate(1, startDate);
                ps.setLong(2, a.getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                Plan p = a.getPlan();
                double bpLength = Calc.getMultiplier(p, bpId);
                long mult = (long) ((today.getTime() - d.getTime()) / (((bpLength * 30.0d) * 86400.0d) * 1000.0d));
                Date bpStart = new Date(d.getTime() + ((long) (mult * bpLength * 30.0d * 86400.0d * 1000.0d)));
                Account oldAccount = Session.getAccount();
                User oldUser = Session.getUser();
                try {
                    Session.setUser(a.getUser());
                    Session.setAccount(a.getAccount());
                    a.setNewPeriodBegin(bpStart);
                    Connection con2 = Session.getDb();
                    PreparedStatement ps2 = con2.prepareStatement("UPDATE parent_child SET p_begin = ?  WHERE account_id=?  AND child_type IN (121,112,113,114,6005,6900,6101)");
                    ps2.setDate(1, TimeUtils.getSQLDate());
                    ps2.setLong(2, a.getId().getId());
                    ps2.executeUpdate();
                    Session.closeStatement(ps2);
                    con2.close();
                } finally {
                    Session.setAccount(oldAccount);
                    Session.setUser(oldUser);
                }
            } catch (Exception ex) {
                outFail("Failed to set billing period");
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected ResourceId addSubDomain(Resource domain, Element subDomain, String parentName, String parentBase) throws Exception {
        Hashtable args = new Hashtable();
        String subDomainName = subDomain.getAttributes().getNamedItem("name").getNodeValue();
        try {
            try {
                args.put("domain_name", new String[]{subDomainName});
                setRequest(args);
                outMessage("Adding subdomain " + subDomainName);
                ResourceId rId = domain.addChild("subdomain", "import", null);
                outOK();
                clearRequest();
                NodeList msChildrens = subDomain.getElementsByTagName("mailservice");
                for (int i = 0; i < msChildrens.getLength(); i++) {
                    Node node = msChildrens.item(i);
                    if (node.getParentNode().equals(subDomain)) {
                        try {
                            addMailService(rId.get(), (Element) node);
                        } catch (Exception e) {
                        }
                    }
                }
                NodeList msChildrens2 = subDomain.getElementsByTagName("webservice");
                for (int i2 = 0; i2 < msChildrens2.getLength(); i2++) {
                    Node node2 = msChildrens2.item(i2);
                    if (node2.getParentNode().equals(subDomain)) {
                        try {
                            WebServiceCreator webCreator = new WebServiceCreator(Session.getAccount(), subDomainName, this);
                            webCreator.addWebService((Element) node2);
                        } catch (Exception e2) {
                        }
                    }
                }
                NodeList msChildrens3 = subDomain.getElementsByTagName("ftp");
                for (int i3 = 0; i3 < msChildrens3.getLength(); i3++) {
                    Node node3 = msChildrens3.item(i3);
                    if (node3.getParentNode().equals(subDomain)) {
                        try {
                            FTPCreator ftpCreator = new FTPCreator(Session.getAccount(), subDomainName, this);
                            ftpCreator.addFTP((Element) node3);
                        } catch (Exception e3) {
                        }
                    }
                }
                NodeList childrens = subDomain.getChildNodes();
                for (int i4 = 0; i4 < childrens.getLength(); i4++) {
                    Node node4 = childrens.item(i4);
                    if (node4.getNodeName().equals("subdomain")) {
                        addSubDomain(rId.get(), (Element) node4, subDomainName, parentBase);
                    }
                }
                if (rId != null) {
                    Element aliases = null;
                    List aList = getElementsByName(subDomain, "aliases");
                    if (aList.size() > 0) {
                        aliases = (Element) aList.get(0);
                    }
                    if (aliases != null) {
                        addDomainAliases(rId.get(), aliases);
                    }
                }
                if (rId != null) {
                    Element dns = null;
                    List l = getElementsByName(subDomain, "dns");
                    if (l.size() > 0) {
                        dns = (Element) l.get(0);
                    }
                    if (dns != null) {
                        ResourceId dnsZone = rId.get().get("dns_zone");
                        outMessage("Got and processing custom DNS settings for subdomain " + subDomainName + "." + parentName + "\n");
                        addCustomDNS(dnsZone.get(), dns);
                    }
                }
                return rId;
            } catch (Throwable th) {
                clearRequest();
                throw th;
            }
        } catch (Exception ex) {
            outFail("Failed to create subdomain " + subDomainName, ex);
            Session.getLog().error("Failed to create subdomain " + subDomainName, ex);
            if (!this.conf.isForced()) {
                throw ex;
            }
            clearRequest();
            return null;
        }
    }

    protected void addMySQLResources(Account a, Element mysqlEl) throws Exception {
        ResourceId mysqlId;
        Hashtable args = new Hashtable();
        if (a.FM_getChild("MySQL") == null) {
            mysqlId = a.addChild("MySQL", "", null);
        } else {
            mysqlId = a.FM_getChild("MySQL");
        }
        Resource mysql = mysqlId.get();
        NodeList databases = mysqlEl.getElementsByTagName("mysqldatabase");
        for (int i = 0; i < databases.getLength(); i++) {
            args.clear();
            Element database = (Element) databases.item(i);
            String name = database.getAttributes().getNamedItem("name").getNodeValue();
            String description = database.getAttributes().getNamedItem("description").getNodeValue();
            try {
                try {
                    args.put("name", new String[]{name});
                    args.put("description", new String[]{description});
                    outMessage("Adding MySQL database " + name);
                    setRequest(args);
                    mysql.addChild("MySQLDatabase", "import", null);
                    outOK();
                    clearRequest();
                } catch (Exception ex) {
                    outFail("Failed to create MySQL database " + name, ex);
                    Session.getLog().error("Failed to create MySQL database " + name, ex);
                    if (!this.conf.isForced()) {
                        throw ex;
                    }
                    clearRequest();
                }
            } catch (Throwable th) {
                clearRequest();
                throw th;
            }
        }
        NodeList users = mysqlEl.getElementsByTagName("mysqluser");
        for (int i2 = 0; i2 < users.getLength(); i2++) {
            args.clear();
            Element user = (Element) users.item(i2);
            String name2 = user.getAttributes().getNamedItem("login").getNodeValue();
            String password = user.getAttributes().getNamedItem("password").getNodeValue();
            try {
                try {
                    args.put("name", new String[]{name2});
                    args.put("password", new String[]{password});
                    outMessage("Adding MySQL user " + name2);
                    setRequest(args);
                    ResourceId mysqlUserId = mysql.addChild("MySQLUser", "import", null);
                    outOK();
                    clearRequest();
                    MySQLUser mysqlUser = (MySQLUser) mysqlUserId.get();
                    NodeList grants = user.getElementsByTagName("grant");
                    for (int j = 0; j < grants.getLength(); j++) {
                        Element grant = (Element) grants.item(j);
                        String privileges = grant.getAttributes().getNamedItem("privileges").getNodeValue();
                        String dbName = grant.getAttributes().getNamedItem("on").getNodeValue();
                        try {
                            outMessage("Granting privileges on " + dbName + " to " + name2);
                            mysqlUser.FM_loadUserPrivilegesOn(dbName);
                            mysqlUser.FM_setDatabasePrivileges(privileges);
                            outOK();
                        } catch (Exception ex2) {
                            outFail("Failed to create MySQL user " + name2, ex2);
                            Session.getLog().error("Failed to create MySQL user " + name2);
                            if (!this.conf.isForced()) {
                                throw ex2;
                            }
                        }
                    }
                    continue;
                } catch (Exception ex3) {
                    outFail("Failed to create MySQL user " + name2, ex3);
                    Session.getLog().error("Failed to create MySQL user " + name2);
                    if (!this.conf.isForced()) {
                        throw ex3;
                    }
                    clearRequest();
                }
            } catch (Throwable th2) {
                clearRequest();
                throw th2;
            }
        }
    }

    private void setReseller(String rName) throws Exception {
        User reseller = User.getUser(rName);
        if (reseller != null) {
            Session.setResellerId(reseller.getId());
        }
    }

    public static void clearUpUser(User u) throws Exception {
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Session.setUser(u);
        Iterator i = u.getAccountIds().iterator();
        while (i.hasNext()) {
            Account a = u.getAccount((ResourceId) i.next());
            Session.setAccount(a);
            try {
                try {
                    System.out.println("\nAccount deletion " + a.getId() + "\n");
                    long aid = a.getId().getId();
                    a.delete(true);
                    Connection con = Session.getDb();
                    PreparedStatement ps = null;
                    try {
                        ps = con.prepareStatement("DELETE FROM user_account WHERE account_id = ? AND user_id = ?");
                        ps.setLong(1, aid);
                        ps.setLong(2, u.getId());
                        ps.executeUpdate();
                        Session.closeStatement(ps);
                        con.close();
                        Session.setAccount(oldAcc);
                    } catch (Throwable th) {
                        Session.closeStatement(ps);
                        con.close();
                        throw th;
                        break;
                    }
                } catch (Throwable th2) {
                    Session.setAccount(oldAcc);
                    throw th2;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Session.setAccount(oldAcc);
            }
        }
        Session.setUser(oldUser);
        u.delete();
    }

    public static void main(String[] argv) throws Exception {
        try {
            CommonUserCreator migrator = new CommonUserCreator(new UserCreatorConfig(argv));
            migrator.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }

    public static int create(InputSource is, BufferedWriter migrationLog) throws Exception {
        CommonUserCreator migrator = new CommonUserCreator(new UserCreatorConfig(is, migrationLog, false));
        return migrator.execute();
    }

    public static void create(String migrationId) throws Exception {
        File log = new File(LOG_DIR, "migration_" + migrationId + ".log");
        File xmlFile = new File(MIGRATION_DIR, "migration_" + migrationId + ".xml");
        create(new InputSource(new FileInputStream(xmlFile)), new BufferedWriter(new FileWriter(log)));
    }

    private boolean canBeCreated(Element user) throws Exception {
        Hashtable dInfo;
        boolean result = true;
        ArrayList reasons = new ArrayList();
        String login = user.getAttributes().getNamedItem("login").getNodeValue();
        User u = null;
        try {
            u = User.getUser(login);
        } catch (Exception e) {
        }
        if (u != null) {
            reasons.add("User with the same login is already registered.");
            result = false;
        }
        NodeList accounts = user.getElementsByTagName("account");
        for (int i = 0; i < accounts.getLength(); i++) {
            Element acc = (Element) accounts.item(i);
            NodeList domains = acc.getElementsByTagName("domain");
            for (int j = 0; j < domains.getLength(); j++) {
                Element domain = (Element) domains.item(j);
                String dName = domain.getAttributes().getNamedItem("name").getNodeValue();
                String dType = domain.getAttributes().getNamedItem("type").getNodeValue();
                if (DNSZone.testZoneName(dName, ("3ldomain".equals(dType) || "service".equals(dType)) ? false : true) > -1 && (dInfo = Domain.getDomainInfoByName(dName)) != null) {
                    reasons.add("Domain " + dName + " is already registered by account #" + dInfo.get("account_id") + "\n");
                    result = false;
                }
            }
        }
        if (!result) {
            outFail();
            outMessage("User " + login + " can not be registered because of the following reasons:\n");
            for (int i2 = 0; i2 < reasons.size(); i2++) {
                outMessage(((String) reasons.get(i2)) + "\n");
            }
        }
        return result;
    }

    protected void setCatchAll(ResourceId mailDomainId, String catchAll) throws Exception {
        outMessage("Setting catchAll to mailbox " + catchAll);
        MailDomain mailDomain = (MailDomain) mailDomainId.get();
        if ("postmaster".equals(catchAll)) {
            mailDomain.FM_setCatchAll(catchAll);
            outOK();
            return;
        }
        Collection<ResourceId> boxes = mailDomain.getChildHolder().getChildrenByName("mailbox");
        Collection<ResourceId> forwarders = mailDomain.getChildHolder().getChildrenByName("mail_forward");
        List<String> addresses = new ArrayList();
        for (ResourceId mboxId : boxes) {
            Mailbox mbox = (Mailbox) mboxId.get();
            addresses.add(mbox.get("email").toString());
        }
        for (ResourceId forwardId : forwarders) {
            MailForward forward = (MailForward) forwardId.get();
            addresses.add(forward.get("local").toString());
        }
        for (String address : addresses) {
            if (catchAll.equals(address)) {
                mailDomain.FM_setCatchAll(catchAll);
                outOK();
                return;
            }
        }
        outFail("There is no " + catchAll + " mailbox defined");
    }

    protected void addCustomDNS(Resource u, Element custDNS) throws Exception {
        if (custDNS == null) {
            return;
        }
        String pref = null;
        NodeList records = custDNS.getElementsByTagName("record");
        for (int i = 0; i < records.getLength(); i++) {
            List args = new ArrayList();
            Element record = (Element) records.item(i);
            if ((!this.isParkedDomain || !record.getAttributes().getNamedItem("name").getNodeValue().equals(this.parkedDomainName)) && record.getAttributes().getNamedItem("type").getNodeValue().equals("A")) {
                String name = record.getAttributes().getNamedItem("name").getNodeValue();
                args.add(name + "." + u.get("name").toString());
                String type = record.getAttributes().getNamedItem("type").getNodeValue();
                args.add(type);
                String ttl = record.getAttributes().getNamedItem("ttl").getNodeValue();
                args.add(ttl);
                String data = record.getAttributes().getNamedItem("data").getNodeValue();
                args.add(data);
                if ("MX".equals(type)) {
                    pref = record.getAttributes().getNamedItem("pref").getNodeValue();
                    args.add(pref);
                }
                try {
                    outMessage("Adding custom DNS record " + name + " " + ttl + " IN  " + type + " " + (pref == null ? "" : pref) + " " + data);
                    u.addChild("cust_dns_record", "", args);
                    outOK();
                } catch (Exception ex) {
                    outFail();
                    outFail("Failed to create custom DNS record ", ex);
                    Session.getLog().error("Failed to create custom DNS record ", ex);
                    if (!this.conf.isForced()) {
                        throw ex;
                    }
                }
            }
        }
    }

    private static List getElementsByName(Node src, String nodeName) throws Exception {
        List result = new ArrayList();
        NodeList nl = src.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == 1) {
                Element el = (Element) nl.item(i);
                if (nodeName.equals(el.getTagName())) {
                    result.add(el);
                }
            }
        }
        return result;
    }
}
