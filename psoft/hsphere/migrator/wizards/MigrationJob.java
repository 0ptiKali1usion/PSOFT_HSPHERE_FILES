package psoft.hsphere.migrator.wizards;

import com.psoft.hsphere.migration.UserContext;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.MissingResourceException;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/wizards/MigrationJob.class */
public abstract class MigrationJob implements TemplateHashModel {
    private static final Category log = Category.getInstance(MigrationJob.class.getName());
    public static int counter = 1;
    public static File MIGRATION_DIR = new File(CommonUserCreator.MIGRATION_DIR);
    protected URL url;

    /* renamed from: ci */
    protected Map f101ci;
    protected String login;
    protected String password;
    protected String planDescription;
    protected String bpid;
    protected String balance;
    protected String startDate;
    protected String jobId;
    protected String state;
    protected int step;
    protected long resellerId;
    protected StringWriter logStr;
    protected BufferedWriter mLog;
    protected String domain;
    protected String mType;

    protected abstract User createUserAccount(UserContext userContext, File file) throws Exception;

    protected abstract void migrateContent(User user, UserContext userContext, File file) throws Exception;

    public void setState(int step, String s) {
        this.step = step;
        this.state = s;
        try {
            this.mLog.write("#" + step + ":" + this.state + "\n");
        } catch (IOException e) {
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("job_id".equals(key)) {
            return new TemplateString(this.jobId);
        }
        if ("state".equals(key)) {
            return new TemplateString(this.state);
        }
        if ("step".equals(key)) {
            return new TemplateString(this.step);
        }
        if ("domain".equals(key)) {
            return new TemplateString(this.domain);
        }
        if ("mtype".equals(key)) {
            return new TemplateString(this.mType);
        }
        if ("log".equals(key)) {
            try {
                this.mLog.flush();
            } catch (IOException e) {
            }
            return new TemplateString(this.logStr.getBuffer().toString());
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public MigrationJob() throws Exception {
    }

    public MigrationJob(URL url, Map ci, String login, String password, String planDescription, String bpid, String balance, String startDate) throws UnknownResellerException {
        try {
            MIGRATION_DIR = new File(Session.getProperty("MIGRATION_DIR"));
        } catch (MissingResourceException e) {
        }
        this.url = url;
        this.f101ci = ci;
        this.login = login;
        this.password = password;
        this.planDescription = planDescription;
        this.bpid = bpid;
        this.balance = balance;
        this.startDate = startDate;
        this.resellerId = Session.getResellerId();
        this.logStr = new StringWriter();
        this.mLog = new BufferedWriter(this.logStr);
        setState(0, "WAITING");
    }

    public String getJobId() {
        return this.jobId;
    }

    public void execute() throws Exception {
        File output = null;
        try {
            output = new File(MIGRATION_DIR, this.jobId + ".tar.gz");
            User u = createUserAccount(null, output);
            if (u != null) {
                migrateContent(u, null, output);
            }
            if (output != null) {
                try {
                    output.delete();
                } catch (Exception e) {
                }
            }
        } catch (Throwable th) {
            if (output != null) {
                try {
                    output.delete();
                } catch (Exception e2) {
                }
            }
            throw th;
        }
    }

    public String singleUserNameFix(String name) {
        return name;
    }

    public int getStep() {
        return this.step;
    }

    public String getState() {
        return this.state;
    }

    public String getJobType() {
        return this.mType;
    }
}
