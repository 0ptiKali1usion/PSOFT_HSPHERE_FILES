package psoft.hsphere.resource;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/RealServerUser.class */
public class RealServerUser extends Resource {
    protected int minConnection;
    protected int maxConnection;
    protected String user;

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.maxConnection;
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        return 1.0d;
    }

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        Integer.parseInt((String) i.next());
        int maxConnection = Integer.parseInt((String) i.next());
        return maxConnection;
    }

    public static double getSetupMultiplier(InitToken token) {
        return 1.0d;
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        Integer.parseInt((String) i.next());
        int maxConnection = Integer.parseInt((String) i.next());
        double defaultMaxConnection = token.getFreeUnits();
        if (defaultMaxConnection >= maxConnection) {
            return 0.0d;
        }
        return maxConnection - defaultMaxConnection;
    }

    public RealServerUser(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT min_con, max_con, user_login FROM real_server WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.minConnection = rs.getInt(1);
                this.maxConnection = rs.getInt(2);
                this.user = rs.getString(3);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public RealServerUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Session.getLog().debug("Test RealServerUser");
        Iterator i = initValues.iterator();
        this.minConnection = Integer.parseInt((String) i.next());
        this.maxConnection = Integer.parseInt((String) i.next());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            suspend();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM real_server WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        this.user = recursiveGet("login").toString();
        recursiveGet("host");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO real_server( id, min_con, max_con, user_login, user_info, host_id) VALUES ( ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.minConnection);
            ps.setInt(3, this.maxConnection);
            ps.setString(4, this.user);
            ps.setString(5, Session.getAccount().getId().toString());
            ps.setLong(6, Long.parseLong(recursiveGet("host_id").toString()));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            update();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getConfigEntry() throws Exception {
        SimpleHash root = new SimpleHash();
        TemplateList userlst = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id ,min_con, max_con, user_login, user_info FROM real_server WHERE host_id = ?");
            ps.setLong(1, Long.parseLong(recursiveGet("host_id").toString()));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash user = new TemplateHash();
                user.put("min_con", new TemplateString(rs.getInt(2)));
                user.put("max_con", new TemplateString(rs.getInt(3)));
                user.put(FMACLManager.USER, new TemplateString(rs.getString(4)));
                user.put("user_info", new TemplateString(rs.getString(5)));
                Session.getLog().debug("Real server list" + rs.getString(4) + " " + rs.getString(3));
                ResourceId ri = new ResourceId(rs.getLong(1), getId().getType());
                Account oldAccount = Session.getAccount();
                try {
                    Account a = ri.getAccount();
                    Session.setAccount(a);
                    Resource rUser = ri.get();
                    if (!rUser.isSuspended()) {
                        userlst.add((TemplateModel) user);
                    }
                    Session.setAccount(oldAccount);
                } catch (Exception e) {
                    Session.getLog().warn("Error getting Real Server User ", e);
                    Session.setAccount(oldAccount);
                }
            }
            Session.closeStatement(ps);
            con.close();
            root.put("userlist", userlst);
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            Session.getTemplate("/real/user.lst").process(root, out);
            out.close();
            Session.getLog().info("End getConfigEntry:" + sw.toString());
            return sw.toString();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "min_con".equals(key) ? new TemplateString(this.minConnection) : "max_con".equals(key) ? new TemplateString(this.maxConnection) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        try {
            this.suspended = true;
            update();
            this.suspended = false;
            super.suspend();
        } catch (Throwable th) {
            this.suspended = false;
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            try {
                this.suspended = false;
                update();
                this.suspended = true;
                super.resume();
            } catch (Throwable th) {
                this.suspended = true;
                throw th;
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    protected void update() throws Exception {
        HostEntry he = recursiveGet("host");
        if (he instanceof WinHostEntry) {
            ((WinHostEntry) he).exec("rs-update.asp", (String[][]) new String[]{new String[]{"userfile", getConfigEntry()}});
        } else {
            he.exec("rmserver-reconfig", new ArrayList(), getConfigEntry());
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.real_user.refund", new Object[]{new Integer(this.maxConnection), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.real_user.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Integer(this.maxConnection), new Double(this.maxConnection - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.real_user.refundall", new Object[]{new Integer(this.maxConnection), f42df.format(begin), f42df.format(end)});
    }
}
