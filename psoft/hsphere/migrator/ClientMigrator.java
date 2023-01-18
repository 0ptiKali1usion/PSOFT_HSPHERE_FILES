package psoft.hsphere.migrator;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.Crontab;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.LinuxQuota;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.MailingList;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.FakeRequest;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/ClientMigrator.class */
public class ClientMigrator extends C0004CP {
    private static final String DEFAULT_PLAN = "Unix";
    private static String dataFile;
    private static String logFile;
    private static String defaultPlanName;
    private static boolean detailedLog;
    private static boolean force;
    private static boolean clearUp;
    private static boolean clearAll;
    private static String resumeUser;
    private static ArrayList dedicatedIPs = new ArrayList();
    private static String prfx = "";
    private static long quotaValue = 0;
    private static long totalClients = 0;
    private static FileWriter outLog = null;

    public ClientMigrator(String dataFile2, String logFile2, String defPlan, boolean detailedLog2, boolean sendmail, String resumeUser2, boolean force2, boolean clearUp2, boolean clearAll2) throws Exception {
        super("psoft_config.hsphere");
        if (sendmail) {
            try {
                outMessage("Initializing mail");
                Session.initMailer();
                outOK();
            } catch (Exception ex) {
                outFail("Failed to initialize mail", ex);
                throw new Exception("Unable to initialize mail");
            }
        }
        dataFile = dataFile2;
        if (logFile2.length() > 0) {
            outLog = new FileWriter(logFile2);
        }
        defaultPlanName = "".equals(defPlan) ? DEFAULT_PLAN : defPlan;
        detailedLog = detailedLog2;
        resumeUser = resumeUser2;
        force = force2;
        clearUp = clearUp2;
        clearAll = clearAll2;
        if (resumeUser2.length() > 0) {
            outMessage("Resuming from previous launch. Whill start from user " + resumeUser2 + ".\n");
        }
    }

    public static void main(String[] argv) throws Exception {
        String dFile = "";
        String lFile = "";
        String dPlanName = "";
        String resumeUser2 = "";
        boolean detailLog = false;
        boolean sendmail = false;
        boolean force2 = false;
        boolean clearUp2 = false;
        boolean clearAll2 = false;
        int i = 0;
        while (i < argv.length) {
            try {
                if ("-d".equals(argv[i]) || "--datafile".equals(argv[i])) {
                    dFile = argv[i + 1];
                    i++;
                }
                if ("-l".equals(argv[i]) || "--log".equals(argv[i])) {
                    lFile = argv[i + 1];
                    i++;
                }
                if ("-dp".equals(argv[i]) || "--defplan".equals(argv[i])) {
                    dPlanName = argv[i + 1];
                    i++;
                }
                if ("-dl".equals(argv[i]) || "--detailedlog".equals(argv[i])) {
                    detailLog = true;
                }
                if ("-sn".equals(argv[i]) || "--sendnotifications".equals(argv[i])) {
                    sendmail = true;
                }
                if ("-r".equals(argv[i]) || "--resume".equals(argv[i])) {
                    resumeUser2 = argv[i + 1];
                    i++;
                    if ("-c".equals(argv[i + 1]) || "--clearup".equals(argv[i])) {
                        clearUp2 = true;
                        i++;
                    }
                }
                if ("-f".equals(argv[i]) || "--force".equals(argv[i])) {
                    force2 = true;
                }
                if ("-ac".equals(argv[i]) || "--clearup-all".equals(argv[i])) {
                    clearAll2 = true;
                }
                i++;
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        if ("".equals(dFile)) {
            printHelp();
            System.exit(1);
        }
        try {
            new ClientMigrator(dFile, lFile, dPlanName, detailLog, sendmail, resumeUser2, force2, clearUp2, clearAll2);
            letsRock();
        } catch (Exception e2) {
            e2.printStackTrace();
            System.exit(1);
        }
        outMessage("Migration finished \n");
        System.exit(0);
    }

    public static void letsRock() throws Exception {
        outMessage("Initializing ");
        User admin = User.getUser(FMACLManager.ADMIN);
        Account adm = admin.getAccount(new ResourceId(1L, 0));
        Session.setUser(admin);
        Session.setAccount(adm);
        outOK();
        try {
            DOMParser parser = new DOMParser();
            try {
                parser.parse(new InputSource(new FileInputStream(dataFile)));
                Document doc = parser.getDocument();
                Element root = doc.getDocumentElement();
                NodeList list = root.getElementsByTagName(FMACLManager.USER);
                totalClients = list.getLength();
                outMessage("Total uses " + totalClients + "\n");
                for (int i = 0; i < list.getLength(); i++) {
                    createUser((Element) list.item(i));
                }
                Session.setUser(admin);
                Session.setAccount(adm);
                if (outLog != null) {
                    outLog.close();
                }
            } catch (IOException ioex) {
                outMessage("Unable to open file " + dataFile + ": " + ioex.toString());
                Session.setUser(admin);
                Session.setAccount(adm);
                if (outLog != null) {
                    outLog.close();
                }
            }
        } catch (Throwable th) {
            Session.setUser(admin);
            Session.setAccount(adm);
            if (outLog != null) {
                outLog.close();
            }
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    private static User createUser(Element node) throws Exception {
        String resellerName = "";
        String login = node.getAttributes().getNamedItem("login").getNodeValue();
        String password = node.getAttributes().getNamedItem("password").getNodeValue();
        if (node.getAttributeNode(FMACLManager.RESELLER) != null) {
            resellerName = node.getAttributes().getNamedItem(FMACLManager.RESELLER).getNodeValue();
        }
        prfx = "";
        if (!"".equals(resellerName)) {
            try {
                outMessage("User belongs to reseller " + resellerName + ". Trying to set reseller ");
                setReseller(resellerName);
                outOK();
            } catch (Exception ex) {
                outFail("Failed to set reseller Id", ex);
                if (force) {
                    return null;
                }
                System.exit(-1);
            }
        }
        if (resumeUser.length() > 0 && !resumeUser.equals(login)) {
            outMessage("Skipping  user " + login + "\n");
            return null;
        }
        if (clearUp || clearAll) {
            try {
                try {
                    outMessage("Clearing after user " + login);
                    User old = User.getUser(login);
                    clearUpUser(old);
                    old.delete();
                    outOK();
                    clearUp = false;
                } catch (Throwable th) {
                    clearUp = false;
                    throw th;
                }
            } catch (Exception ex2) {
                outFail("Error during user deletion. Try to delete this user manualy", ex2);
                clearUp = false;
            }
        }
        resumeUser = "";
        User u = null;
        User oldUser = Session.getUser();
        try {
            outMessage("Creating user " + login);
            User.createUser(login, password, Session.getResellerId());
            u = User.getUser(login);
            Session.setUser(u);
            outOK();
        } catch (Exception ex3) {
            outFail(ex3.getMessage(), ex3);
            if (force) {
                Session.setUser(oldUser);
                return null;
            }
            System.exit(-1);
        }
        try {
            try {
                NodeList accounts = node.getElementsByTagName("account");
                for (int i = 0; i < accounts.getLength(); i++) {
                    quotaValue = 0L;
                    addAccount(u, (Element) accounts.item(i));
                }
                if (u.getAccountIds().isEmpty()) {
                    outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                    u.delete();
                    if (!force) {
                        System.exit(-1);
                    }
                }
                Session.setUser(oldUser);
            } catch (Throwable th2) {
                if (u.getAccountIds().isEmpty()) {
                    outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                    u.delete();
                    if (!force) {
                        System.exit(-1);
                    }
                }
                Session.setUser(oldUser);
                throw th2;
            }
        } catch (Exception e) {
            outMessage("Failed to create accounts, will delete user " + login + "\n");
            if (u.getAccountIds().isEmpty()) {
                outMessage("No accounts were created for user " + login + ". User will be deleted\n");
                u.delete();
                if (!force) {
                    System.exit(-1);
                }
            }
            Session.setUser(oldUser);
        }
        return u;
    }

    private static Account addAccount(User u, Element account) throws Exception {
        String planName;
        Account a;
        String startDate = "";
        String bpId = "";
        String balance = "";
        Hashtable initval = new Hashtable();
        prfx = "\t";
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
        BillingInfoObject bi = new BillingInfoObject(new NameModifier("_bi_"));
        ContactInfoObject ci = new ContactInfoObject(new NameModifier("_ci_"));
        if (account.getAttributeNode("plan") != null) {
            planName = account.getAttributes().getNamedItem("plan").getNodeValue();
        } else {
            planName = defaultPlanName;
        }
        outMessage("Resolving plan ID for plan " + planName);
        int planId = Plan.getPlanIdByName(planName);
        if (planId < 0) {
            outFail("Accont will not be created");
            return null;
        }
        outOK();
        if (account.getAttributeNode("startdate") != null) {
            startDate = account.getAttributes().getNamedItem("startdate").getNodeValue();
        }
        if (account.getAttributeNode("bpid") != null) {
            bpId = account.getAttributes().getNamedItem("bpid").getNodeValue();
        }
        if (account.getAttributeNode("balance") != null) {
            balance = account.getAttributes().getNamedItem("balance").getNodeValue();
        }
        outMessage("Adding account for " + u.getLogin());
        if (startDate.length() > 0 && bpId.length() > 0) {
            a = u.addAccount(planId, bi, ci, "Account", "nodomain", Integer.parseInt(bpId));
            outOK();
        } else {
            a = u.addAccount(planId, bi, ci, "Account", "nodomain", 0);
            outOK();
        }
        try {
            outMessage("Preparing balance ");
            a.getBill().setCredit(1000.0d);
            outOK();
        } catch (Exception ex) {
            outFail("Failed to prepare balance", ex);
            if (!force) {
                System.exit(-1);
            }
        }
        Resource unixUser = a.FM_getChild("unixuser").get();
        NodeList domains = account.getElementsByTagName("domain");
        if (domains != null) {
            for (int i2 = 0; i2 < domains.getLength(); i2++) {
                Element domain = (Element) domains.item(i2);
                addDomain(unixUser, domain);
            }
        } else {
            outMessage("No domains found\n");
        }
        NodeList tmp = account.getElementsByTagName("mysql");
        Element mysql = tmp.getLength() > 0 ? (Element) tmp.item(0) : null;
        if (mysql != null) {
            addMySQLResources(a, mysql);
        }
        if (startDate.length() > 0 && bpId.length() > 0) {
            try {
                outMessage("Setting start billing date");
                setBillingPeriod(a, startDate);
                outOK();
            } catch (Exception ex2) {
                Session.getLog().error("Failed to set start billing date", ex2);
                outFail("Failed to set start billing date", ex2);
                if (!force) {
                    System.exit(-1);
                }
            }
        }
        NodeList tmp2 = account.getElementsByTagName("crontab");
        Element crontab = tmp2.getLength() > 0 ? (Element) tmp2.item(0) : null;
        if (crontab != null) {
            Resource unixUser2 = a.FM_getChild("unixuser").get();
            addCrons(unixUser2, crontab);
        }
        if (balance.length() > 0) {
            try {
                outMessage("Setting starting balance for account to " + balance);
                addCredit(a, balance);
                outOK();
            } catch (Exception ex3) {
                outFail("Error during setiing balance", ex3);
                Session.getLog().error("Error during setiing balance", ex3);
                if (!force) {
                    System.exit(-1);
                }
            }
        } else {
            try {
                outMessage("Setting starting balance for account to 0");
                addCredit(a, "0");
                outOK();
            } catch (Exception ex4) {
                outFail("Error during setiing balance", ex4);
                Session.getLog().error("Error during setiing balance", ex4);
                if (!force) {
                    System.exit(-1);
                }
            }
        }
        return a;
    }

    private static ResourceId addDomain(Resource u, Element domain) throws Exception {
        Hashtable args = new Hashtable();
        ResourceId rId = null;
        prfx = "\t\t";
        String domainName = domain.getAttributes().getNamedItem("name").getNodeValue();
        String ip = domain.getAttributes().getNamedItem("ip") != null ? domain.getAttributes().getNamedItem("ip").getNodeValue() : "";
        String quota = domain.getAttributes().getNamedItem("quota") != null ? domain.getAttributes().getNamedItem("quota").getNodeValue() : "";
        if (quota.length() > 0) {
            quotaValue += Long.parseLong(quota);
        }
        try {
            if ("".equals(ip)) {
                args.put("domain_name", new String[]{domainName});
                setRequest(args);
                outMessage("Adding domain " + domainName + " with shared IP");
                rId = u.addChild("domain", "signup", null);
                outOK();
            } else if (proceedIP(Long.parseLong(u.get("host_id").toString()), ip)) {
                args.put("domain_name", new String[]{domainName});
                args.put("ip", new String[]{ip});
                setRequest(args);
                try {
                    outMessage("Adding domain " + domainName + " whith ip " + ip);
                    rId = u.addChild("domain", "dedicated", null);
                    outOK();
                } catch (Exception ex) {
                    outFail("Failed to create domain " + domainName, ex);
                    Session.getLog().error("Failed to create domain " + domainName, ex);
                    if (force) {
                        return null;
                    }
                    System.exit(-1);
                    clearRequest();
                } finally {
                }
            } else {
                outFail("The import datata contains IP that can not be used");
                if (!force) {
                    System.exit(-1);
                }
            }
        } catch (Exception ex2) {
            outFail("Failed to create domain " + domainName, ex2);
            Session.getLog().error("Failed to create domain " + domainName, ex2);
            if (force) {
                return null;
            }
            System.exit(-1);
            clearRequest();
        } finally {
        }
        if (rId != null) {
            addMailService(rId.get(), (Element) domain.getElementsByTagName("mailservice").item(0));
        }
        if (rId != null) {
            NodeList subdomains = domain.getElementsByTagName("subdomain");
            for (int i = 0; i < subdomains.getLength(); i++) {
                Element subdomain = (Element) subdomains.item(i);
                addSubDomain(rId.get(), subdomain, domainName);
            }
        }
        return rId;
    }

    private static void addMailService(Resource u, Element mailService) throws Exception {
        prfx = "\t\t\t";
        if (mailService == null) {
            outMessage("No mailservice configuration found\n");
            return;
        }
        ResourceId mDomainId = u.FM_findChild("mail_domain");
        if (mDomainId == null) {
            return;
        }
        NodeList boxes = mailService.getElementsByTagName("mailbox");
        for (int i = 0; i < boxes.getLength(); i++) {
            addMailBox(mDomainId.get(), (Element) boxes.item(i));
        }
        NodeList lists = mailService.getElementsByTagName("maillist");
        for (int i2 = 0; i2 < lists.getLength(); i2++) {
            addMailList(mDomainId.get(), (Element) lists.item(i2));
        }
        NodeList forwards = mailService.getElementsByTagName("forward");
        for (int i3 = 0; i3 < forwards.getLength(); i3++) {
            addMailForward(mDomainId.get(), (Element) forwards.item(i3));
        }
    }

    private static void addMailBox(Resource mailDomain, Element box) throws Exception {
        Hashtable args = new Hashtable();
        String mboxName = box.getAttributes().getNamedItem("name").getNodeValue();
        String password = box.getAttributes().getNamedItem("password").getNodeValue();
        args.put("email", new String[]{mboxName});
        args.put("password", new String[]{password});
        args.put("description", new String[]{"The mailbox"});
        setRequest(args);
        try {
            outMessage("Creating mailbox " + mboxName);
            mailDomain.addChild("mailbox", "import", null);
            outOK();
            clearRequest();
        } catch (Exception ex) {
            outFail("Failed to create mailbox " + mboxName, ex);
            Session.getLog().error("Failed to create mailbox " + mboxName, ex);
            if (force) {
                return;
            }
            System.exit(-1);
            clearRequest();
        } finally {
        }
        NodeList responders = box.getElementsByTagName("autoresponder");
        if (responders.getLength() > 1) {
            outMessage("More than one autoresponder for " + mboxName + " specified. Skiping it all\n");
        } else if (responders.getLength() == 1) {
            Element responder = (Element) responders.item(0);
            String message = responder.getFirstChild().getNodeValue();
            args.clear();
            args.put("local", new String[]{mboxName});
            args.put("foreign", new String[]{""});
            args.put("subject", new String[]{"Autorespond"});
            args.put("message", new String[]{message});
            args.put("description", new String[]{"Autoresponder"});
            setRequest(args);
            try {
                outMessage("Adding responder ");
                mailDomain.addChild("responder", "import", null);
                outOK();
            } catch (Exception ex2) {
                outFail(ex2.getMessage(), ex2);
                if (!force) {
                    System.exit(-1);
                }
            } finally {
            }
        }
        NodeList aliases = box.getElementsByTagName("alias");
        for (int i = 0; i < aliases.getLength(); i++) {
            String foreign = aliases.item(i).getAttributes().getNamedItem("name").getNodeValue();
            args.clear();
            args.put("local", new String[]{foreign});
            args.put("foreign", new String[]{mboxName});
            args.put("description", new String[]{"The mailbox alias"});
            setRequest(args);
            try {
                outMessage("Adding mailbox alias " + mboxName);
                mailDomain.addChild("mailbox_alias", "import", null);
                outOK();
            } catch (Exception ex3) {
                Session.getLog().error("Failed to add alias " + mboxName, ex3);
                outFail("Failed to add alias " + mboxName, ex3);
                if (!force) {
                    System.exit(-1);
                    clearRequest();
                }
            } finally {
            }
        }
    }

    private static void addMailList(Resource mailDomain, Element mailList) throws Exception {
        Hashtable args = new Hashtable();
        ResourceId listId = null;
        String listName = mailList.getAttributes().getNamedItem("name").getNodeValue();
        args.put("email", new String[]{listName});
        args.put("description", new String[]{"The mailing list"});
        setRequest(args);
        try {
            try {
                outMessage("Adding mailing list " + listName);
                listId = mailDomain.addChild("mailing_list", "", null);
                outOK();
                clearRequest();
            } catch (Exception ex) {
                Session.getLog().error("Failed to add mailing list " + listName, ex);
                outFail("Failed to add mailing list " + listName, ex);
                if (force) {
                    clearRequest();
                    return;
                } else {
                    System.exit(-1);
                    clearRequest();
                }
            }
            NodeList subscribers = mailList.getElementsByTagName("subscriber");
            outMessage("Creating subscribers to mailing list " + listName);
            for (int i = 0; i < subscribers.getLength(); i++) {
                try {
                    outMessage("Adding subscriber ...");
                    ((MailingList) listId.get()).FM_subscribe(subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue());
                    outOK();
                } catch (Exception ex2) {
                    Session.getLog().error("Failed to add subscriber", ex2);
                    outFail("Failed to add subscriber", ex2);
                    if (!force) {
                        System.exit(-1);
                    }
                }
            }
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    private static void addMailForward(Resource mailDomain, Element forward) throws Exception {
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
                    } catch (Exception ex) {
                        outFail("Failed to add forward ", ex);
                        Session.getLog().error("Failed to add forward ", ex);
                        if (force) {
                            clearRequest();
                            return;
                        } else {
                            System.exit(-1);
                            clearRequest();
                        }
                    }
                } catch (Throwable th) {
                    clearRequest();
                    throw th;
                }
            } else {
                try {
                    outMessage("Adding forward subscriber");
                    ((MailForward) fId.get()).addSubscriber(subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue());
                    outOK();
                } catch (Exception ex2) {
                    outFail("Failed to add forward subscriber", ex2);
                    Session.getLog().error("Failed to add forward subscriber", ex2);
                    if (!force) {
                        System.exit(-1);
                    }
                }
            }
        }
    }

    private static void setRequest(Hashtable args) {
        Session.setRequest(new FakeRequest(args));
    }

    private static void clearRequest() {
        Session.setRequest(new FakeRequest(new Hashtable()));
    }

    private static void outOK() {
        System.out.println(" [  OK  ]");
        if (outLog != null) {
            try {
                outLog.write(" [  OK  ]\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void outFail(String errMessage) {
        System.out.println(" [  FAILED  ]");
        System.out.println(errMessage);
        if (outLog != null) {
            try {
                outLog.write(" [  FAILED  ]\n");
                outLog.write(errMessage + "\n");
            } catch (IOException e) {
            }
        }
    }

    private static void outMessage(String message) {
        System.out.print(prfx + message);
        if (outLog != null) {
            try {
                outLog.write(prfx + message);
                outLog.flush();
            } catch (IOException e) {
            }
        }
    }

    private static void outFail() {
        System.out.println(" [  FAILED  ]");
        if (outLog != null) {
            try {
                outLog.write(" [  FAILED  ]\n");
            } catch (IOException e) {
            }
        }
    }

    private static void outFail(String message, Exception ex) {
        System.out.println(" [  FAILED  ]");
        System.out.println(message);
        if (outLog != null) {
            try {
                outLog.write(" [  FAILED  ]\n");
                outLog.write(prfx + message + "\n");
                if (detailedLog) {
                    ex.printStackTrace(new PrintWriter((Writer) outLog, true));
                }
                outLog.flush();
            } catch (IOException e) {
            }
        }
    }

    private static boolean proceedIP(long hostId, String ip) throws Exception {
        System.out.println("");
        HostEntry he = HostManager.getHost(hostId);
        int res = he.checkIP(ip);
        switch (res) {
            case -1:
                HostEntry.checkAllIP(ip);
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

    private static void setQuota(Account a, long quotaValue2) throws Exception {
        if (quotaValue2 == 0) {
            return;
        }
        try {
            outMessage("Setting new disk quota value " + quotaValue2);
            Resource unixuser = a.FM_getChild("unixuser").get();
            LinuxQuota quota = (LinuxQuota) unixuser.FM_getChild("quota").get();
            quota.delete(true);
            ArrayList args = new ArrayList();
            args.add(new Long(quotaValue2).toString());
            unixuser.addChild("quota", "", args);
            outOK();
        } catch (Exception ex) {
            Session.getLog().error("Failed to set quota ", ex);
            outFail("Failed to set quota ", ex);
            if (!force) {
                System.exit(-1);
            }
        }
    }

    private static void setBillingPeriod(Account a, String pBegin) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        Date d = dateFormat.parse(pBegin, new ParsePosition(0));
        java.sql.Date dd = new java.sql.Date(d.getTime());
        a.setPeriodBegin(dd);
        java.sql.Date pEnd = new java.sql.Date(a.nextPaymentDate().getTime());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("UPDATE parent_child SET p_begin = ? where account_id=?");
                ps2.setDate(1, dd);
                ps2.setLong(2, a.getId().getId());
                ps2.executeUpdate();
                ps2.close();
                ps = con.prepareStatement("UPDATE accounts SET created = ?, p_end = ? WHERE id = ?");
                ps.setDate(1, dd);
                ps.setDate(2, pEnd);
                ps.setLong(3, a.getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                if (!force) {
                    System.exit(-1);
                }
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static ResourceId addSubDomain(Resource domain, Element subDomain, String parentName) throws Exception {
        Hashtable args = new Hashtable();
        ResourceId rId = null;
        prfx = "\t\t\t";
        String subDomainName = subDomain.getAttributes().getNamedItem("name").getNodeValue();
        try {
            try {
                args.put("domain_name", new String[]{subDomainName + "." + parentName});
                setRequest(args);
                outMessage("Adding subdomain " + subDomainName);
                rId = domain.addChild("subdomain", "import", null);
                outOK();
                clearRequest();
            } catch (Exception ex) {
                outFail("Failed to create subdomain " + subDomainName, ex);
                Session.getLog().error("Failed to create subdomain " + subDomainName, ex);
                if (force) {
                    clearRequest();
                    return null;
                }
                System.exit(-1);
                clearRequest();
            }
            return rId;
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    private static void addMySQLResources(Account a, Element mysqlEl) throws Exception {
        ResourceId mysqlUserId = null;
        Hashtable args = new Hashtable();
        prfx = "\t\t";
        ResourceId mysqlId = a.FM_getChild("MySQL") == null ? a.addChild("MySQL", "", null) : a.FM_getChild("MySQL");
        Resource mysql = mysqlId.get();
        NodeList databases = mysqlEl.getElementsByTagName("database");
        for (int i = 0; i < databases.getLength(); i++) {
            args.clear();
            Element database = (Element) databases.item(i);
            String name = database.getAttributes().getNamedItem("name").getNodeValue();
            String description = database.getAttributes().getNamedItem("description").getNodeValue();
            try {
                args.put("name", new String[]{name});
                args.put("description", new String[]{description});
                outMessage("Adding MySQL database " + name);
                setRequest(args);
                mysql.addChild("MySQLDatabase", "import", null);
                outOK();
            } catch (Exception ex) {
                outFail("Failed to create MySQL database " + name, ex);
                Session.getLog().error("Failed to create MySQL database " + name, ex);
                if (!force) {
                    System.exit(-1);
                    clearRequest();
                }
            } finally {
            }
        }
        NodeList users = mysqlEl.getElementsByTagName("mysqluser");
        for (int i2 = 0; i2 < users.getLength(); i2++) {
            args.clear();
            Element user = (Element) users.item(i2);
            String name2 = user.getAttributes().getNamedItem("login").getNodeValue();
            String password = user.getAttributes().getNamedItem("password").getNodeValue();
            try {
                args.put("name", new String[]{name2});
                args.put("password", new String[]{password});
                outMessage("Adding MySQL user " + name2);
                setRequest(args);
                mysqlUserId = mysql.addChild("MySQLUser", "import", null);
                outOK();
                clearRequest();
            } catch (Exception ex2) {
                outFail("Failed to create MySQL user " + name2, ex2);
                Session.getLog().error("Failed to create MySQL user " + name2);
                if (!force) {
                    System.exit(-1);
                    clearRequest();
                }
            } finally {
            }
            MySQLUser mysqlUser = (MySQLUser) mysqlUserId.get();
            NodeList grants = user.getElementsByTagName("grant");
            for (int j = 0; j < grants.getLength(); j++) {
                Element grant = (Element) grants.item(j);
                String privileges = grant.getAttributes().getNamedItem("privileges").getNodeValue();
                String dbName = grant.getAttributes().getNamedItem("on").getNodeValue();
                try {
                    outMessage("Granting privileges on " + dbName + " to " + name2);
                    mysqlUser.FM_loadUserPrivilegesOn(dbName);
                    mysqlUser.FM_revokeAllDatabasePrivileges();
                    mysqlUser.FM_setDatabasePrivileges(privileges);
                    outOK();
                } catch (Exception ex3) {
                    outFail("Failed to create MySQL user " + name2, ex3);
                    Session.getLog().error("Failed to create MySQL user " + name2);
                    if (!force) {
                        System.exit(-1);
                    }
                }
            }
        }
    }

    private static void setReseller(String rName) throws Exception {
        User reseller = User.getUser(rName);
        if (reseller != null) {
            Session.setResellerId(reseller.getId());
        }
    }

    private static void addCredit(Account a, String balance) throws Exception {
        Date now = TimeUtils.getDate();
        Bill bill = a.getBill();
        double amount = -(Double.parseDouble(balance) - bill.getBalance());
        synchronized (bill) {
            bill.addEntry(6, now, -1L, -1, Localizer.translateMessage("bill.b_balance"), now, (Date) null, (String) null, amount);
            bill.setCredit(0.0d);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, a.getId().getId());
            ps.setDouble(2, amount);
            ps.setString(3, "");
            ps.setString(4, "Starting balance");
            ps.setString(5, "OTHER");
            ps.setDate(6, new java.sql.Date(now.getTime()));
            ps.executeUpdate();
            con.close();
        }
    }

    private static void clearUpUser(User u) throws Exception {
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Session.setUser(u);
        u.getAccountIds();
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
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Session.setAccount(oldAcc);
                }
            } catch (Throwable th2) {
                Session.setAccount(oldAcc);
                throw th2;
            }
        }
        Session.setUser(oldUser);
        u.delete();
    }

    private static void addCrons(Resource unixUser, Element crontab) throws Exception {
        new Hashtable();
        ResourceId ctabId = unixUser.getId().findChild("crontab");
        if (ctabId == null) {
            try {
                outMessage("Activating crontab resource");
                ctabId = unixUser.addChild("crontab", "", null);
                outOK();
            } catch (Exception ex) {
                outFail(ex.getMessage(), ex);
                if (!force) {
                    System.exit(-1);
                }
            }
        }
        if (ctabId != null) {
            Crontab crontab2 = (Crontab) ctabId.get();
            NodeList jobs = crontab.getElementsByTagName("job");
            for (int i = 0; i < jobs.getLength(); i++) {
                Element job = (Element) jobs.item(i);
                job.getAttributes().getNamedItem("hour").getNodeValue();
                job.getAttributes().getNamedItem("day_of_month").getNodeValue();
                job.getAttributes().getNamedItem("month").getNodeValue();
                job.getAttributes().getNamedItem("day_of_week").getNodeValue();
                job.getFirstChild().getNodeValue();
            }
        }
    }

    private static void printHelp() {
        System.out.println("Usage java psoft.hsphere.converter.ClientMigrator -d datafile.xml [-l logfile] [-dl] \n[-dp defplan] [sn] [-r username] [-f]\n");
        System.out.println("Where ");
        System.out.println("-d datafile.xml or --datafile datafile.xml\n file which contains data for the bulk\n users creation\n");
        System.out.println("-l logfile or --log logfile\n file in which class should log actions and results\n");
        System.out.println("-dl or --detailedlog defines detailed log level.\n In this mode log will contain not just 'failed'\n messages but stack trace as well.\n");
        System.out.println("-dp defplan or --defplan defplan\n plan which will be used as the default if no plan is defined in datafile.\n In case no plan is defined in the datafile and this\n parameter is not defined, plan Unix will be used.\n");
        System.out.println("-sn or --sendnotifications Send welcome letters,\n troubletickets and all other kind of e-mail notifications.\n Please make sure mail settings are configured in the control\n panel before using this option\n");
        System.out.println("-r username --resume username.\n This option allows to resume migration process from certain user.\n All users which preceed this one will be skipped. This\n option is usable only if the previous launch of the program\n had been performed without the -f option.\n");
        System.out.println("-c or --clearup. This option can be used only in\n combination with -r option and allows to delete the user which\n migration has been resumed at. In case of any error \n during the user delition the program will terminate.\n");
        System.out.println("-f or --force. Continue the user migration even if there are any errors.\n Do not use this option if you are going to user -r option later\n\n");
    }
}
