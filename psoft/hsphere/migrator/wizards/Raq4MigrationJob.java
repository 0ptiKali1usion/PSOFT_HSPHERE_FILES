package psoft.hsphere.migrator.wizards;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.psoft.hsphere.migration.CoboltRaqMigration;
import com.psoft.hsphere.migration.DomainContext;
import com.psoft.hsphere.migration.UserContext;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.zip.GZIPInputStream;
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
import psoft.util.IOUtils;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/wizards/Raq4MigrationJob.class */
public class Raq4MigrationJob extends MigrationJob {
    private static final Category log = Category.getInstance(MigrationJob.class.getName());
    UserContext _uc;

    protected static synchronized String getNewJobId() {
        counter++;
        return "RAQ-" + counter;
    }

    @Override // psoft.hsphere.migrator.wizards.MigrationJob
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    public Raq4MigrationJob(URL url, Map ci, String login, String password, String planDescription, String bpid, String balance, String startDate) throws UnknownResellerException, Exception {
        super(url, ci, login, password, planDescription, bpid, balance, startDate);
        this.jobId = getNewJobId();
        this.mType = "raq";
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
                SimpleHash root = CoboltRaqMigration.getMigrationInfo(output.getAbsolutePath());
                setState(this.step + 1, "Set customer info");
                UserContext uc2 = root.get(FMACLManager.USER);
                this._uc = uc2;
                uc2.setBillingInfo(this.f101ci);
                uc2.setContactInfo(this.f101ci);
                uc2.setLogin(this.login);
                uc2.setPassword(this.password);
                uc2.setPlan(this.planDescription);
                uc2.setBillingPeriodId(this.bpid);
                uc2.setBalance(this.balance);
                uc2.setStartDate(this.startDate);
                this.domain = uc2.getDomain().getName();
                setState(this.step + 1, "preparing config data");
                Template t = Session.getTemplate("migrator/raq4.xml.template");
                String xml = Template2String.process(t, (TemplateModelRoot) root);
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
                if (u != null) {
                    setState(this.step, "Error, deleting user");
                    try {
                        CommonUserCreator.clearUpUser(u);
                    } catch (Exception ee) {
                        log.warn("Error deleting user", ee);
                    }
                }
                setState(this.step, "ERROR");
                throw e;
            }
        } catch (Throwable th) {
            PrefixHolder.enablePrefixes();
            throw th;
        }
    }

    @Override // psoft.hsphere.migrator.wizards.MigrationJob
    protected void migrateContent(User u, UserContext uc, File output) throws Exception {
        String targetDir;
        String subUserName;
        try {
            Account a = u.getAccount((ResourceId) u.getAccountIds().iterator().next());
            Session.save();
            UserContext uc2 = this._uc;
            Session.setResellerId(this.resellerId);
            Session.setUser(u);
            Session.setAccount(a);
            setState(this.step + 1, "Uncompressing data");
            InputStream tarInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(output.getAbsolutePath())));
            UnixUser unixUser = (UnixUser) a.getId().findChild("unixuser").get();
            HostEntry webServer = HostManager.getHost(unixUser.getHostId());
            HostEntry mailServer = HostManager.getHost(((MailService) a.getId().findChild("mail_service").get()).getHostId());
            File baseDir = new File(unixUser.getDir(), this.domain);
            boolean first = true;
            int prefixSize = 0;
            for (TarEntry entry = tarInputStream.getNextEntry(); entry != null; entry = tarInputStream.getNextEntry()) {
                String tarEntryName = entry.getName();
                if (first) {
                    first = false;
                    if (tarEntryName.endsWith("/")) {
                        prefixSize = tarEntryName.length();
                    }
                }
                if (prefixSize > 0) {
                    tarEntryName = tarEntryName.substring(prefixSize);
                }
                if (tarEntryName.endsWith("-public.tar.gz")) {
                    if (tarEntryName.startsWith("users-")) {
                        String subUserName2 = singleUserNameFix(tarEntryName.substring(6, tarEntryName.length() - 14));
                        this.mLog.write("Working with:" + subUserName2 + "\n");
                        targetDir = new File(baseDir, "users/" + subUserName2).getAbsolutePath();
                        subUserName = unixUser.getLogin();
                    } else if (tarEntryName.startsWith("groups-")) {
                        targetDir = baseDir.getAbsolutePath();
                        subUserName = unixUser.getLogin();
                    } else {
                        this.mLog.write("UNKONWN TGZ: " + tarEntryName + "\n");
                    }
                    setState(this.step, "Uncompressing " + tarEntryName);
                    webServer.execSpecial("uncompress", new String[]{"-", targetDir, subUserName, subUserName}, tarInputStream);
                } else if (tarEntryName.endsWith("-private.tar.gz")) {
                    if (tarEntryName.startsWith("users-")) {
                        String subUserName3 = singleUserNameFix(tarEntryName.substring(6, tarEntryName.length() - 15));
                        this.mLog.write("Converting mail for: " + subUserName3);
                        try {
                            log.info("UC: " + uc2);
                        } catch (Throwable t) {
                            log.error("NPE: " + t);
                        }
                        String mboxfile = uc2.getPlatform() == 3 ? "mbox" : "cmu-mailspool";
                        mailServer.execSpecial("migration-raq4-userprivate", new String[]{this.domain, subUserName3, mboxfile}, tarInputStream);
                    } else {
                        setState(this.step, "Uncompressing " + tarEntryName);
                        webServer.execSpecial("uncompress-raq4private", new String[]{"-", unixUser.getDir(), unixUser.getLogin(), unixUser.getLogin()}, tarInputStream);
                    }
                }
            }
            setState(this.step + 1, "Setting old mail passwords");
            DomainContext dc = uc2.getDomain();
            mailServer.exec("mbox-set_md5_passwords", new String[]{this.domain}, dc.subusers2str());
            setState(this.step + 1, "Done");
            Session.restore();
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(this.mLog));
            setState(this.step, "ERROR");
            throw e;
        }
    }
}
