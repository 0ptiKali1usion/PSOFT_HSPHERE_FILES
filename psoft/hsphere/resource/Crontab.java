package psoft.hsphere.resource;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.cron.CrontabItem;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Crontab.class */
public class Crontab extends Resource implements HostDependentResource {
    protected List crontabList;
    protected String mailTo;

    public Crontab(ResourceId id) throws Exception {
        super(id);
        this.crontabList = new ArrayList();
        this.mailTo = "root@localhost";
        reload();
    }

    public void save() throws Exception {
        save(Long.parseLong(recursiveGet("host_id").toString()));
    }

    private ArrayList getCommandsArray() {
        ArrayList commands = new ArrayList();
        try {
            for (Hashtable strHash : this.crontabList) {
                String command = strHash.get("minute").toString() + " " + strHash.get("hour").toString() + " " + strHash.get("day_of_month").toString() + " " + strHash.get("month").toString() + " " + strHash.get("day_of_week").toString() + " " + strHash.get("job").toString();
                commands.add(command);
            }
        } catch (Exception e) {
        }
        return commands;
    }

    protected void save(long hostId) throws Exception {
        HostEntry host = HostManager.getHost(hostId);
        List args = new ArrayList();
        String login = recursiveGet("login").toString();
        args.add(login);
        String cronFile = "MAILTO=" + this.mailTo + '\n';
        String command = "";
        ArrayList commands = getCommandsArray();
        Iterator i = commands.iterator();
        while (i.hasNext()) {
            command = command + ((String) i.next()) + '\n';
        }
        host.exec("crontab-save", args, cronFile + command);
        int num = 0;
        deleteCrontab(login);
        Iterator i2 = commands.iterator();
        while (i2.hasNext()) {
            String command2 = (String) i2.next();
            if (CrontabItem.instance(this.mailTo, command2) != null) {
                saveCrontabEntry(login, num, this.mailTo, command2);
                num++;
            }
        }
    }

    public static ArrayList getCrontabEntries(String login) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ArrayList commands = new ArrayList();
        try {
            con = Session.getDb();
            ps = con.prepareStatement("SELECT command, mailto FROM crontab WHERE (login = ?)");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String command = rs.getString(1);
                String mailto = rs.getString(2);
                CrontabItem item = CrontabItem.instance(mailto, command);
                if (item != null) {
                    commands.add(item);
                }
            }
            Session.closeStatement(ps);
            con.close();
            return commands;
        } catch (Exception e) {
            Session.closeStatement(ps);
            con.close();
            return commands;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void saveCrontabEntry(String login, int num, String mailTo, String command) throws Exception {
        if (command == null) {
            return;
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            try {
                con = Session.getDb();
                ps = con.prepareStatement("INSERT INTO crontab(login,num,command,mailto) VALUES(?, ?, ?, ?)");
                ps.setString(1, login);
                ps.setInt(2, num);
                ps.setString(3, command);
                ps.setString(4, mailTo);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error save crontab entry ", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void deleteCrontab(String login) throws Exception {
        PreparedStatement ps = null;
        Connection con = null;
        try {
            try {
                con = Session.getDb();
                ps = con.prepareStatement("DELETE FROM crontab WHERE (login = ?) ");
                ps.setString(1, login);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error remove crontab items", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void reload() throws Exception {
        HostEntry host = HostManager.getHost(recursiveGet("host_id"));
        List args = new ArrayList();
        String login = recursiveGet("login").toString();
        args.add(login);
        Collection<String> listRes = host.exec("crontab-list", args);
        this.crontabList.clear();
        int num = 0;
        for (String tmpStr : listRes) {
            Session.getLog().debug("Get String:" + tmpStr);
            if (tmpStr == null || !"".equals(tmpStr)) {
                if (tmpStr == null || (!tmpStr.startsWith("LOGNAME") && !tmpStr.startsWith("HOME") && !tmpStr.startsWith("SHELL"))) {
                    if (tmpStr != null && tmpStr.startsWith("MAILTO")) {
                        this.mailTo = tmpStr.substring(tmpStr.indexOf("MAILTO=") + 7);
                    } else if (tmpStr != null && !tmpStr.startsWith("#")) {
                        Hashtable job = new Hashtable();
                        int i = num;
                        num++;
                        job.put("num", Integer.toString(i));
                        StringTokenizer tkz = new StringTokenizer(tmpStr);
                        if (tkz.hasMoreTokens()) {
                            job.put("minute", tkz.nextToken());
                        }
                        if (tkz.hasMoreTokens()) {
                            job.put("hour", tkz.nextToken());
                        }
                        if (tkz.hasMoreTokens()) {
                            job.put("day_of_month", tkz.nextToken());
                        }
                        if (tkz.hasMoreTokens()) {
                            job.put("month", tkz.nextToken());
                        }
                        if (tkz.hasMoreTokens()) {
                            job.put("day_of_week", tkz.nextToken());
                        }
                        String str = "";
                        if (tkz.hasMoreTokens()) {
                            str = tkz.nextToken();
                        }
                        while (tkz.hasMoreTokens()) {
                            str = str + " " + tkz.nextToken();
                        }
                        job.put("job", str);
                        this.crontabList.add(job);
                    }
                }
            }
        }
    }

    public TemplateModel FM_reload() throws Exception {
        reload();
        return new TemplateOKResult();
    }

    public TemplateModel FM_setMailTo(String mailTo) throws Exception {
        this.mailTo = mailTo;
        return new TemplateOKResult();
    }

    public TemplateModel FM_clearLines() throws Exception {
        this.crontabList.clear();
        Session.getLog().debug("Crontab lines cleared");
        return new TemplateOKResult();
    }

    public TemplateModel FM_saveLines() throws Exception {
        save();
        Session.getLog().debug("Crontab lines saved");
        return new TemplateOKResult();
    }

    public Crontab(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.crontabList = new ArrayList();
        this.mailTo = "root@localhost";
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            HostEntry host = HostManager.getHost(recursiveGet("host_id"));
            List args = new ArrayList();
            String login = recursiveGet("login").toString();
            args.add(login);
            host.exec("crontab-remove", args);
            deleteCrontab(login);
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        save();
    }

    public Adder getAdderForEmptyList() {
        this.crontabList.clear();
        return new Adder(this.crontabList);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "cron_list".equals(key) ? new TemplateList(this.crontabList) : "mail_to".equals(key) ? new TemplateString(this.mailTo) : "addLine".equals(key) ? new Adder(this.crontabList) : super.get(key);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/Crontab$Adder.class */
    public class Adder implements TemplateMethodModel {
        List cronstrList;

        public Adder(List cronstrList) {
            Crontab.this = r5;
            this.cronstrList = new ArrayList();
            this.cronstrList = cronstrList;
            Session.getLog().debug("Adder created");
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                List l2 = HTMLEncoder.decode(l);
                if (!l2.isEmpty()) {
                    try {
                        Hashtable jobStr = new Hashtable();
                        jobStr.put("minute", l2.get(0));
                        jobStr.put("hour", l2.get(1));
                        jobStr.put("day_of_month", l2.get(2));
                        jobStr.put("month", l2.get(3));
                        jobStr.put("day_of_week", l2.get(4));
                        jobStr.put("job", l2.get(5));
                        this.cronstrList.add(jobStr);
                        Session.getLog().debug("CRONTAB Adder" + l2);
                    } catch (Exception e) {
                        Crontab.getLog().warn("Error adding crontab command: " + l2, e);
                        return new TemplateErrorResult(e);
                    }
                }
                return new TemplateOKResult();
            } catch (Exception e2) {
                Session.getLog().error("Error create crontab record", e2);
                return new TemplateErrorResult(e2);
            }
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("login").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.crontab.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.crontab.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.crontab.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.crontab.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        reload();
        save(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry host = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        String login = recursiveGet("login").toString();
        args.add(login);
        host.exec("crontab-remove", args);
        deleteCrontab(login);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
