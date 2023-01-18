package psoft.hsphere.resource;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import org.apache.regexp.RE;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.Base64;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Urchin4Resource.class */
public class Urchin4Resource extends Resource implements HostDependentResource, IPDependentResource, Reconfigurable {
    protected long urchinServerId;
    protected String app;
    protected String pass;
    protected String user;

    public Urchin4Resource(int type, Collection init) throws Exception {
        super(type, init);
        try {
            this.urchinServerId = Long.parseLong(Session.getProperty("URCHIN_SERVER_ID"));
        } catch (Exception e) {
            throw new HSUserException("urchin.server_not_set");
        }
    }

    public Urchin4Resource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT * FROM urchin4 WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.urchinServerId = rs.getLong("server_id");
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        if (getParent().findChildren("urchin4").size() > 1) {
            throw new HSUserException("urchin.already.exists");
        }
        super.initDone();
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO urchin4 (id, server_id) VALUES(?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, this.urchinServerId);
            ps.executeUpdate();
            physicalCreate(this.urchinServerId);
        } catch (Exception e) {
            throw new HSUserException("urchin.user_error");
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        Exception causedException = null;
        super.delete();
        if (this.initialized) {
            try {
                physicalDelete(this.urchinServerId);
            } catch (Exception ex) {
                Session.getLog().error("Error deleting urchin :", ex);
                causedException = ex;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("DELETE FROM urchin4 WHERE id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                if (causedException != null) {
                    throw causedException;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                if (causedException != null) {
                    throw causedException;
                }
                throw th;
            }
        } catch (Exception e) {
            throw new Exception("URCHIN4: Unable to remove urchin " + e.toString());
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.urchin.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.urchin.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.urchin.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.urchin.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (!key.equals("urchin_url")) {
            return key.equals("app") ? new TemplateString(this.app) : key.equals(FMACLManager.USER) ? new TemplateString(this.user) : key.equals("pass") ? new TemplateString(this.pass) : super.get(key);
        }
        try {
            return new TemplateString(getURL());
        } catch (Exception e) {
            return new TemplateString("");
        }
    }

    protected String getConfig() throws Exception {
        int start;
        int finish;
        Template config = Session.getTemplate("domain/urchin4.config");
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        SimpleHash root = new SimpleHash();
        root.put("login", Session.getUser().getLogin());
        root.put("password", Session.getUser().getPassword());
        root.put(FMACLManager.USER, recursiveGet("login"));
        root.put("domain", _getName());
        root.put("real_name", recursiveGet("real_name"));
        HostEntry he = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
        if (he instanceof WinHostEntry) {
            root.put("platform", "windows");
        } else {
            root.put("platform", "unix");
        }
        root.put("type", new TemplateString("remote"));
        String server = Session.int2ext(he.getIP().toString());
        root.put("server", server);
        root.put("loglocation", new TemplateString("/hsphere/local/urchin/var/logs/"));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = (HostManager.getHost(this.urchinServerId).getPFirstIP() + recursiveGet("real_name").toString()).getBytes();
        String key = Base64.encode(md.digest(bytes));
        root.put(MerchantGatewayManager.MG_KEY_PREFIX, key.toLowerCase().trim());
        root.put("rlist", getReportList());
        long hour = 4;
        long minute = 0;
        try {
            start = Integer.parseInt(Session.getProperty("URCHIN_SCHEDULER_START"));
            finish = Integer.parseInt(Session.getProperty("URCHIN_SCHEDULER_FINISH"));
        } catch (Exception e) {
        }
        if (start < 0 || start > 24 || finish < 0 || finish > 24) {
            throw new Exception();
        }
        if (finish < start) {
            finish = start;
            start = finish;
        }
        double random = Math.random() * (finish - start) * 60.0d;
        long hour2 = Math.round((random - 30.0d) / 60.0d);
        minute = Math.round((random - (hour2 * 60)) / 5.0d) * 5;
        hour = hour2 + start;
        root.put("hour", new TemplateString(hour));
        root.put("minute", new TemplateString(minute));
        config.process(root, writer);
        writer.flush();
        writer.close();
        return out.toString();
    }

    protected String getReportList() throws Exception {
        String domain = _getName();
        Connection con = Session.getDb();
        String result = domain;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name FROM domains WHERE name<>? AND id IN (SELECT child_id FROM parent_child WHERE account_id IN (SELECT account_id FROM user_account WHERE user_id = ?))");
            ps.setString(1, domain);
            ps.setLong(2, Session.getUser().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = result + ',' + rs.getString(1);
            }
            return result;
        } catch (Exception e) {
            throw new HSUserException("urchin.setup_error");
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v9, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String answer;
        String os;
        String answer2;
        int start;
        int finish;
        HostEntry urchin = HostManager.getHost(targetHostId);
        HostEntry web = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
        ArrayList params = new ArrayList();
        if (urchin instanceof WinHostEntry) {
            String domain = _getName();
            String login = Session.getUser().getLogin();
            String password = Session.getUser().getPassword();
            long hour = 4;
            long minute = 0;
            try {
                start = Integer.parseInt(Session.getProperty("URCHIN_SCHEDULER_START"));
                finish = Integer.parseInt(Session.getProperty("URCHIN_SCHEDULER_FINISH"));
            } catch (Exception e) {
            }
            if (start < 0 || start > 24 || finish < 0 || finish > 24) {
                throw new Exception();
            }
            if (finish < start) {
                finish = start;
                start = finish;
            }
            double random = Math.random() * (finish - start) * 60.0d;
            long hour2 = Math.round((random - 30.0d) / 60.0d);
            minute = Math.round((random - (hour2 * 60)) / 5.0d) * 5;
            hour = hour2 + start;
            String IP = Session.int2ext(web.getIP().toString());
            if (web instanceof WinHostEntry) {
                os = "WIN";
            } else {
                os = "UNIX";
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = (urchin.getPFirstIP() + recursiveGet("real_name").toString()).getBytes();
            String key = Base64.encode(md.digest(bytes));
            String log_source = recursiveGet("login").toString() + "&" + recursiveGet("real_name").toString() + "&" + key.toLowerCase().trim();
            Iterator i = ((WinHostEntry) urchin).exec("urchin4-install.asp", (String[][]) new String[]{new String[]{"hostname", domain}, new String[]{"user-name", login}, new String[]{"password", password}, new String[]{"IP", IP}, new String[]{"ostype", os}, new String[]{"hour", Long.toString(hour)}, new String[]{"minute", Long.toString(minute)}, new String[]{"log_source", log_source}}).iterator();
            String str = "";
            while (true) {
                answer2 = str;
                if (!i.hasNext()) {
                    break;
                }
                str = answer2 + ((String) i.next());
            }
            RE re = new RE("License limit has been reached");
            if (re.match(answer2)) {
                throw new HSUserException("urchin.out_of_licenses");
            }
        } else {
            String out = getConfig().toString();
            Iterator i2 = urchin.exec("uconf-import", params, out).iterator();
            String str2 = "";
            while (true) {
                answer = str2;
                if (!i2.hasNext()) {
                    break;
                }
                str2 = answer + ((String) i2.next());
            }
            RE re2 = new RE("License limit reached");
            if (re2.match(answer)) {
                throw new HSUserException("urchin.out_of_licenses");
            }
        }
        if (!(web instanceof WinHostEntry)) {
            params.add(recursiveGet("login").toString());
            params.add(_getName());
            web.exec("urchin-chmod", params);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v11, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry urchin = HostManager.getHost(targetHostId);
        String login = Session.getUser().getLogin();
        String password = Session.getUser().getPassword();
        String domain = _getName();
        if (urchin instanceof WinHostEntry) {
            ((WinHostEntry) urchin).exec("urchin4-uninstall.asp", (String[][]) new String[]{new String[]{"hostname", domain}, new String[]{"user-name", login}, new String[]{"password", password}});
            return;
        }
        ArrayList params = new ArrayList();
        params.add("--login=" + login);
        params.add("--password=" + password);
        params.add("--domain=" + domain);
        urchin.exec("urchin4-remove", params);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v7, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_changePassword(String password) throws Exception {
        HostEntry urchin = HostManager.getHost(this.urchinServerId);
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE urchin4 SET password = ? WHERE id = ?");
            ps.setString(1, password);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            String login = getAccount().get("login").toString();
            if (urchin instanceof WinHostEntry) {
                ((WinHostEntry) urchin).exec("urchin4-password.asp", (String[][]) new String[]{new String[]{"login", login}, new String[]{"password", password}});
            } else {
                ArrayList params = new ArrayList();
                params.add(login);
                params.add(password);
                urchin.exec("urchin-passwd", params);
            }
            return this;
        } catch (Exception e) {
            throw new HSUserException("urchin.user_error");
        }
    }

    protected String getURL() throws Exception {
        String serverName;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Account account = getAccount();
        try {
            ps = con.prepareStatement("SELECT password FROM urchin4 WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.pass = rs.getString(1);
                if (this.pass == null) {
                    this.pass = account.get("password").toString();
                }
            } else {
                notFound();
            }
            this.user = account.get("login").toString();
            String urchinPort = "9999";
            try {
                urchinPort = Session.getProperty("URCHIN_PORT");
            } catch (Exception e) {
            }
            String urchinProtocol = "http";
            try {
                urchinProtocol = Session.getProperty("URCHIN_PROTOCOL");
            } catch (Exception e2) {
            }
            HostEntry he = HostManager.getHost(this.urchinServerId);
            if (he instanceof WinHostEntry) {
                this.app = "admin.exe";
            } else {
                this.app = FMACLManager.ADMIN;
            }
            try {
                serverName = Session.getProperty("URCHIN_SERVERNAME");
            } catch (Exception e3) {
                serverName = he.getPFirstIP().toString();
            }
            return urchinProtocol + "://" + serverName + ":" + urchinPort;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return this.urchinServerId;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v32, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        try {
            this.urchinServerId = Long.parseLong(Session.getProperty("URCHIN_SERVER_ID"));
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE urchin4 SET server_id = ? WHERE id = ?");
            ps.setLong(1, this.urchinServerId);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        HostEntry urchin = HostManager.getHost(this.urchinServerId);
        String domain = _getName();
        if (urchin instanceof WinHostEntry) {
            ((WinHostEntry) urchin).exec("changeIP-urchin.asp", (String[][]) new String[]{new String[]{"hostname", _getName()}, new String[]{"newIP", HostManager.getHost(this.urchinServerId).getPFirstIP()}});
            return;
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = (HostManager.getHost(this.urchinServerId).getPFirstIP() + _getName()).getBytes();
        String key = Base64.encode(md.digest(bytes));
        ArrayList params = new ArrayList();
        params.add("action=edit");
        params.add("table=logfile");
        params.add("name=" + domain);
        params.add("cr_type=remote");
        params.add("ct_loglocation=/hsphere/local/urchin/var/logs/");
        params.add("cr_protocol=http");
        HostEntry he = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
        String server = Session.int2ext(he.getIP().toString());
        params.add("ct_server=" + server);
        params.add("ct_port=80");
        HostEntry web = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
        if (web instanceof WinHostEntry) {
            params.add("ct_remotelocation='\"urchin/urchin.exe?" + recursiveGet("login") + "&" + domain + "&" + key.toLowerCase().trim() + "&.gz\"'");
            params.add("cs_logformat=w3c");
        } else {
            params.add("ct_remotelocation='\"cgi-bin/print-log.pl?" + recursiveGet("login") + "&" + domain + "&" + key.toLowerCase().trim() + "&.gz\"'");
            params.add("cs_logformat=ncsa");
        }
        urchin.exec("uconf-driver", params);
    }

    @Override // psoft.hsphere.resource.Reconfigurable
    public void reconfigure() throws Exception {
        physicalCreate(getHostId());
    }
}
