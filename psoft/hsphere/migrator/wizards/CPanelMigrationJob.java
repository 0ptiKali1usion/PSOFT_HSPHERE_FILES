package psoft.hsphere.migrator.wizards;

import com.ice.tar.TarEntry;
import com.ice.tar.TarHeader;
import com.ice.tar.TarInputStream;
import com.psoft.hsphere.migration.CPanelMigration;
import com.psoft.hsphere.migration.DomainContext;
import com.psoft.hsphere.migration.MailServiceContext;
import com.psoft.hsphere.migration.MigrationUtils;
import com.psoft.hsphere.migration.SubDomainContext;
import com.psoft.hsphere.migration.UserContext;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Category;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.PrefixHolder;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.UnixUser;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.hsphere.resource.mysql.UserPrivileges;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.IOUtils;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/wizards/CPanelMigrationJob.class */
public class CPanelMigrationJob extends MigrationJob {
    private static final Category log = Category.getInstance(MigrationJob.class.getName());
    protected List subdomains;

    /* renamed from: dc */
    protected DomainContext f100dc;

    protected static synchronized String getNewJobId() {
        counter++;
        return "CPanel-" + counter;
    }

    @Override // psoft.hsphere.migrator.wizards.MigrationJob
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    public CPanelMigrationJob(URL url, Map ci, String login, String password, String planDescription, String bpid, String balance, String startDate) throws UnknownResellerException, Exception {
        super(url, ci, login, password, planDescription, bpid, balance, startDate);
        this.subdomains = new ArrayList();
        this.jobId = getNewJobId();
        this.mType = "cpanel";
    }

    @Override // psoft.hsphere.migrator.wizards.MigrationJob
    protected User createUserAccount(UserContext uc, File output) throws Exception {
        User u = null;
        try {
            try {
                setState(this.step + 1, "Connecting");
                URLConnection con = this.url.openConnection();
                setState(this.step, "Downloading");
                FileOutputStream out = new FileOutputStream(output);
                IOUtils.copy(con.getInputStream(), out);
                out.close();
                setState(this.step + 1, "Parse Date");
                SimpleHash root = CPanelMigration.getMigrationInfo(output.getAbsolutePath());
                setState(this.step + 1, "Set customer info");
                UserContext uc2 = root.get(FMACLManager.USER);
                uc2.setBillingInfo(this.f101ci);
                uc2.setContactInfo(this.f101ci);
                uc2.setLogin(this.login);
                uc2.setPassword(this.password);
                uc2.setPlan(this.planDescription);
                uc2.setBillingPeriodId(this.bpid);
                uc2.setBalance(this.balance);
                uc2.setStartDate(this.startDate);
                this.f100dc = uc2.getDomain();
                this.domain = this.f100dc.getName();
                List subDCs = this.f100dc.getSubDomains();
                for (int i = 0; i < subDCs.size(); i++) {
                    SubDomainContext sdc = (SubDomainContext) subDCs.get(i);
                    this.subdomains.add(sdc.getName());
                }
                migrateSubscribers(output);
                setState(this.step + 1, "preparing config data");
                Template t = Session.getTemplate("migrator/migration.xml.template");
                String xml = Template2String.process(t, (TemplateModelRoot) root);
                PrintWriter out1 = null;
                try {
                    try {
                        out1 = new PrintWriter(new FileWriter(new File(MIGRATION_DIR, uc2.getLogin() + ".xml")));
                        out1.print(xml);
                        out1.close();
                    } catch (Throwable th) {
                        out1.close();
                        throw th;
                    }
                } catch (IOException ioex) {
                    log.warn("Error writing xml", ioex);
                    out1.close();
                }
                setState(this.step + 1, "creating user");
                if (CommonUserCreator.create(new InputSource(new StringReader(xml)), this.mLog) == 1) {
                    setState(this.step + 1, "preparing environment");
                    u = User.getUser(uc2.getLogin());
                } else {
                    setState(this.step, "Unable to create user");
                }
                PrefixHolder.enablePrefixes();
                return u;
            } catch (Exception e) {
                e.printStackTrace(new PrintWriter(this.mLog));
                if (0 != 0) {
                    setState(this.step, "Error, deleting user");
                    try {
                        CommonUserCreator.clearUpUser(null);
                    } catch (Exception ee) {
                        log.warn("Error deleting user", ee);
                    }
                }
                setState(this.step, "ERROR");
                throw e;
            }
        } catch (Throwable th2) {
            PrefixHolder.enablePrefixes();
            throw th2;
        }
    }

    @Override // psoft.hsphere.migrator.wizards.MigrationJob
    protected void migrateContent(User u, UserContext uc, File output) throws Exception {
        try {
            Account a = u.getAccount((ResourceId) u.getAccountIds().iterator().next());
            Session.save();
            Session.setResellerId(this.resellerId);
            Session.setUser(u);
            Session.setAccount(a);
            setState(this.step + 1, "Uncompressing data");
            TarInputStream tarIs = MigrationUtils.openTGZ(output.getAbsolutePath());
            UnixUser unixUser = (UnixUser) a.getId().findChild("unixuser").get();
            HostEntry webServer = HostManager.getHost(unixUser.getHostId());
            HostEntry mailServer = HostManager.getHost(((MailService) a.getId().findChild("mail_service").get()).getHostId());
            Collection mysqlDbs = a.getId().findAllChildren("MySQLDatabase");
            Collection mailboxes = a.getId().findAllChildren("mailbox");
            File baseDir = new File(unixUser.getDir(), this.domain);
            for (TarEntry entry = tarIs.getNextEntry(); entry != null; entry = tarIs.getNextEntry()) {
                String tarEntryName = entry.getName();
                if (tarEntryName.indexOf("mysql/") != -1 && !tarEntryName.endsWith("mysql/") && !tarEntryName.endsWith("horde.sql")) {
                    try {
                        migrateMySQLContent(tarEntryName, mysqlDbs, tarIs);
                        setState(this.step, "Migrated MYSQL " + tarEntryName);
                    } catch (Exception ex1) {
                        ex1.printStackTrace(new PrintWriter(this.mLog));
                    }
                }
                if (tarEntryName.indexOf("homedir/public_html/") != -1 && !tarEntryName.endsWith("homedir/public_html/") && tarEntryName.indexOf("_vti_") == -1 && tarEntryName.indexOf("_private") == -1 && tarEntryName.indexOf("postinfo.html") == -1 && tarEntryName.indexOf("phpbb/") == -1) {
                    try {
                        migrateWebContent(webServer, baseDir, unixUser, tarEntryName, entry, tarIs);
                        setState(this.step, "Uncompressed " + tarEntryName);
                    } catch (Exception ex12) {
                        ex12.printStackTrace(new PrintWriter(this.mLog));
                    }
                }
                if (tarEntryName.indexOf("homedir/mail/") != -1 && tarEntryName.indexOf("inbox") != -1) {
                    try {
                        migrateMailContent(mailServer, mailboxes, tarEntryName, entry, tarIs);
                        setState(this.step, "Migrated mailbox " + tarEntryName);
                    } catch (Exception ex13) {
                        ex13.printStackTrace(new PrintWriter(this.mLog));
                    }
                }
            }
            setState(this.step + 1, "Done");
            Session.restore();
            MigrationUtils.closeTGZ(tarIs);
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(this.mLog));
            setState(this.step, "ERROR");
            throw e;
        }
    }

    protected void migrateSubscribers(File output) throws Exception {
        File mailman = new File("/usr/local/cpanel/3rdparty/mailman/");
        if (!mailman.exists()) {
            this.mLog.write("Mailman does not exist. Subscribers for mailing lists cannot be migrated. \n");
            return;
        }
        MailServiceContext mc = this.f100dc.getMailService();
        TarInputStream tarIs = null;
        try {
            tarIs = MigrationUtils.openTGZ(output.getAbsolutePath());
            List maillists = mc.getMaillists();
            HostEntry cpServer = HostManager.getCPHost();
            for (TarEntry entry = tarIs.getNextEntry(); entry != null; entry = tarIs.getNextEntry()) {
                String tarEntryName = entry.getName();
                for (int i = 0; i < maillists.size(); i++) {
                    HashMap maillist = (HashMap) maillists.get(i);
                    String listName = (String) maillist.get("name");
                    TarHeader header = entry.getHeader();
                    if (tarEntryName.indexOf("/mm/" + listName) != -1) {
                        int mmPos = tarEntryName.indexOf("/mm/");
                        String suffix = tarEntryName.substring(mmPos + 3);
                        String newFilePath = "/usr/local/cpanel/3rdparty/mailman/lists" + suffix;
                        if (!entry.isDirectory()) {
                            cpServer.execSpecial("migrate-cpanel-content", new String[]{"--file=" + MailServices.shellQuote(newFilePath), "--user=mailman", "--group=mailman", "--mode=" + header.mode}, tarIs);
                        } else {
                            cpServer.exec("migrate-cpanel-content", new String[]{"--dir=" + MailServices.shellQuote(newFilePath), "--user=mailman", "--group=mailman", "--mode=" + header.mode});
                        }
                    }
                }
            }
            for (int i2 = 0; i2 < maillists.size(); i2++) {
                HashMap maillist2 = (HashMap) maillists.get(i2);
                String listName2 = (String) maillist2.get("name");
                List subscribers = new ArrayList();
                for (String email : cpServer.exec("migrate-cpanel-mlist-subscribers", new String[]{listName2 + "_" + this.domain})) {
                    HashMap subscriber = new HashMap();
                    subscriber.put("email", email);
                    subscribers.add(subscriber);
                }
                mc.addSubscribersToMaillist(listName2, subscribers);
            }
            MigrationUtils.closeTGZ(tarIs);
        } catch (Throwable th) {
            MigrationUtils.closeTGZ(tarIs);
            throw th;
        }
    }

    protected void migrateMySQLContent(String tarEntryName, Collection mysqlDbs, TarInputStream tarIs) throws Exception {
        String dbFileName = tarEntryName.substring(tarEntryName.lastIndexOf("/") + 1, tarEntryName.length() - 4);
        Iterator i = mysqlDbs.iterator();
        while (i.hasNext()) {
            ResourceId mysqlDbResId = (ResourceId) i.next();
            MySQLDatabase mysqlDb = (MySQLDatabase) MySQLDatabase.get(mysqlDbResId);
            String dbName = mysqlDb.getName();
            Collection<ResourceId> dbUsers = mysqlDb.getUsers();
            if (dbFileName.equals(dbName)) {
                this.mLog.write("Migrate MySQL database with name " + dbName + "...\n");
                for (ResourceId dbUserId : dbUsers) {
                    MySQLUser mUser = (MySQLUser) MySQLUser.get(dbUserId);
                    UserPrivileges privs = mUser.getUserPrivileges();
                    if (privs.getDatabasePriv("create").equals("Y")) {
                        mysqlDb.batchSQL(dbUserId, new LineNumberReader(new InputStreamReader(tarIs)));
                    }
                }
            }
        }
    }

    protected void migrateWebContent(HostEntry webServer, File baseDir, UnixUser unixUser, String tarEntryName, TarEntry entry, TarInputStream tarIs) throws Exception {
        String str;
        int tt1 = tarEntryName.lastIndexOf("public_html/");
        int tt2 = tarEntryName.length();
        String suffix = tarEntryName.substring(tt1 + "public_html/".length(), tt2);
        String newFilePath = "";
        if (!this.subdomains.isEmpty()) {
            for (int i = 0; i < this.subdomains.size(); i++) {
                String subdomain = (String) this.subdomains.get(i);
                String subdomainName = subdomain.substring(0, subdomain.indexOf(this.domain) - 1);
                if (tarEntryName.indexOf("public_html/" + subdomainName + "/") != -1) {
                    suffix = suffix.substring(subdomainName.length() + 1);
                    str = unixUser.getDir() + "/" + subdomain + "/" + suffix;
                } else {
                    str = baseDir.getAbsolutePath() + "/" + suffix;
                }
                newFilePath = str;
            }
        } else {
            newFilePath = baseDir.getAbsolutePath() + "/" + suffix;
        }
        TarHeader header = entry.getHeader();
        this.mLog.write("new File path = " + newFilePath + "\n");
        if (!entry.isDirectory()) {
            webServer.execSpecial("migrate-cpanel-content", new String[]{"--file=" + MailServices.shellQuote(newFilePath), "--user=" + unixUser.getLogin(), "--group=" + unixUser.getGroup(), "--mode=" + header.mode}, tarIs);
        } else {
            webServer.exec("migrate-cpanel-content", new String[]{"--dir=" + MailServices.shellQuote(newFilePath), "--user=" + unixUser.getLogin(), "--group=" + unixUser.getGroup(), "--mode=" + header.mode});
        }
    }

    protected void migrateMailContent(HostEntry mailServer, Collection mailboxes, String tarEntryName, TarEntry entry, TarInputStream tarIs) throws Exception {
        Iterator i = mailboxes.iterator();
        while (i.hasNext()) {
            ResourceId mailboxResId = (ResourceId) i.next();
            Mailbox mailbox = (Mailbox) Mailbox.get(mailboxResId);
            String mboxName = mailbox.getEmail();
            String inboxName = mboxName + "/inbox";
            String dirName = "/hsphere/local/var/vpopmail/domains/" + this.domain + "/" + mboxName + "/Maildir/new/";
            if (tarEntryName.endsWith(inboxName)) {
                mailServer.execSpecial("migrate-cpanel-mailbox-content", new String[]{"--dir=" + MailServices.shellQuote(dirName), "--user=vpopmail", "--group=vchkpw"}, tarIs);
            }
        }
    }
}
