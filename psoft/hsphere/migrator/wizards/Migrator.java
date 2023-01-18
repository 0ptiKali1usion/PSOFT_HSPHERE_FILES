package psoft.hsphere.migrator.wizards;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Map;
import javax.servlet.ServletRequest;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.cron.MigrationCron;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/wizards/Migrator.class */
public abstract class Migrator implements TemplateHashModel {
    public static final Category log = Category.getInstance(Migrator.class.getName());
    protected static final String[] FIELDS = {"first_name", "last_name", "company", "address1", "address2", "address3", "city", "state", "postal_code", "country", "phone", "fax", "email"};

    protected abstract void setupJob(String str) throws MalformedURLException, UnknownResellerException, Exception;

    public abstract TemplateModel FM_start() throws Exception;

    public TemplateModel FM_getJobs() {
        return new TemplateList(MigrationCron.getJobs().values());
    }

    public TemplateModel FM_getJob(String jobId) {
        return MigrationCron.getJob(jobId);
    }

    public TemplateModel FM_massStart(String text) throws IOException, UnknownResellerException, Exception {
        BufferedReader sr = new BufferedReader(new StringReader(text));
        int count = 0;
        while (true) {
            String line = sr.readLine();
            if (line != null) {
                setupJob(line);
                count++;
            } else {
                return new TemplateString(count);
            }
        }
    }

    public void setValue(Map map, ServletRequest request, String field) {
        map.put(field, request.getParameter(field));
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
