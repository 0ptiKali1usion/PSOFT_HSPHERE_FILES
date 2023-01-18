package psoft.hsphere.migrator.wizards;

import freemarker.template.TemplateModel;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.cron.MigrationCron;
import psoft.web.utils.CSVTokenizer;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/wizards/Raq4Migrator.class */
public class Raq4Migrator extends Migrator {
    @Override // psoft.hsphere.migrator.wizards.Migrator
    protected void setupJob(String line) throws MalformedURLException, UnknownResellerException, Exception {
        if (line.length() == 0 || line.startsWith("#")) {
            return;
        }
        CSVTokenizer csv = new CSVTokenizer(line);
        URL url = new URL(csv.nextToken());
        String login = csv.nextToken();
        String password = csv.nextToken();
        String plan = Plan.getPlan(csv.nextToken()).getDescription();
        String bpid = csv.nextToken();
        String balance = csv.nextToken();
        String startDate = csv.nextToken();
        Map map = new HashMap();
        for (int i = 0; i < FIELDS.length; i++) {
            map.put(FIELDS[i], csv.nextToken());
        }
        Raq4MigrationJob job = new Raq4MigrationJob(url, map, login, password, plan, bpid, balance, startDate);
        MigrationCron.addJob(job);
    }

    @Override // psoft.hsphere.migrator.wizards.Migrator
    public TemplateModel FM_start() throws Exception {
        HttpServletRequest request = Session.getRequest();
        Map map = new HashMap();
        for (int i = 0; i < FIELDS.length; i++) {
            setValue(map, request, FIELDS[i]);
        }
        URL url = new URL("ftp://" + request.getParameter("ftplogin") + ":" + request.getParameter("ftppassword") + "@" + request.getParameter("ftpserver") + "/" + request.getParameter("ftpfile"));
        Raq4MigrationJob job = new Raq4MigrationJob(url, map, request.getParameter("login"), request.getParameter("password"), Plan.getPlan(request.getParameter("plan_id")).getDescription(), request.getParameter("bpselect"), request.getParameter("balance"), request.getParameter("start_date"));
        MigrationCron.addJob(job);
        return job;
    }
}
