package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/MnogoSearch.class */
public class MnogoSearch extends Resource implements HostDependentResource {

    /* renamed from: db */
    protected ResourceId f180db;
    protected ResourceId user;

    public MnogoSearch(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.f180db = null;
        this.user = null;
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.f180db = new ResourceId((String) i.next());
        }
        if (i.hasNext()) {
            this.user = new ResourceId((String) i.next());
        }
    }

    public MnogoSearch(ResourceId rId) throws Exception {
        super(rId);
        this.f180db = null;
        this.user = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_id, user_id FROM mnogosearch WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.f180db = new ResourceId(rs.getString(1));
                this.user = new ResourceId(rs.getString(2));
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
        super.initDone();
        if (((MySQLDatabase) this.f180db.get()).lockedBy() != null) {
            throw new HSUserException("mnogosearch.db_alreadylocked", new Object[]{String.valueOf(this.f180db.get("db_name"))});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO mnogosearch(id, db_id, user_id) VALUES (? ,? ,?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, String.valueOf(this.f180db));
            ps.setString(3, String.valueOf(this.user));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            physicalCreate(getHostId());
            ((MySQLDatabase) this.f180db.get()).lock(getId());
            ((MySQLUser) this.user.get()).lock(getId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        configMnoGoSearch("set");
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            return key.equals("db") ? this.f180db : key.equals(FMACLManager.USER) ? this.user : super.get(key);
        } catch (Exception e) {
            getLog().warn("geting config", e);
            return null;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (this.f180db.get() != null) {
                ((MySQLDatabase) this.f180db.get()).unlock();
            }
            if (this.user.get() != null) {
                ((MySQLUser) this.user.get()).unlock();
            }
        }
        Connection con = Session.getDb();
        configMnoGoSearch("drop");
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mnogosearch WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM mnogosearch_url WHERE mnogo_id = ?");
            ps1.setLong(1, getId().getId());
            ps1.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void configMnoGoSearch(String action) throws Exception {
        ResourceId mysql_host = this.f180db.get().getParent();
        HostEntry he = HostManager.getHost(recursiveGet("host_id"));
        String domain = String.valueOf(recursiveGet("name"));
        List l = new ArrayList();
        l.add("-f " + String.valueOf(recursiveGet("dir")));
        l.add("-u " + String.valueOf(recursiveGet("login")));
        l.add("-g " + String.valueOf(recursiveGet("group")));
        l.add("-d " + domain);
        l.add("-l " + String.valueOf(this.user.get("name")));
        l.add("-p " + String.valueOf(this.user.get("password")));
        if ("set".equals(action)) {
            l.add("-t " + mysql_host.get("host").getName());
        } else {
            String mysqlbox = "";
            List l1 = new ArrayList();
            l1.add(domain);
            Iterator output = he.exec("mnogosearch-get-mysqlbox", l1).iterator();
            if (output.hasNext()) {
                mysqlbox = output.next().toString();
            }
            if (!"".equals(mysqlbox)) {
                l.add("-t " + mysqlbox);
            } else {
                Session.getLog().debug("Failed to get MySQL box for mnogosearch for domain " + domain);
                return;
            }
        }
        l.add("-n " + String.valueOf(this.f180db.get("db_name")));
        l.add("-a " + action);
        he.exec("mnogosearch-init", l);
    }

    public TemplateModel FM_restoreConfig() throws Exception {
        try {
            configMnoGoSearch("set");
            return new TemplateOKResult();
        } catch (Exception e) {
            Session.getLog().error("Error setting Mnogosearch props", e);
            return new TemplateErrorResult(e.getMessage());
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void handleURL(String action, String url) throws Exception {
        HostEntry he = HostManager.getHost(recursiveGet("host_id"));
        List l = new ArrayList();
        l.add("-d " + String.valueOf(recursiveGet("name")));
        l.add("-u " + url);
        l.add(action);
        he.exec("mnogosearch-set", l);
    }

    public String removeURL(String url) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("DELETE FROM mnogosearch_url WHERE mnogo_id = ? AND url = ?");
                ps.setLong(1, getId().getId());
                ps.setString(2, url);
                ps.executeUpdate();
                handleURL("-r", url);
                Session.closeStatement(ps);
                con.close();
                return "OK";
            } catch (Exception e) {
                Session.getLog().error("Error setting Mnogosearch props", e);
                String message = e.getMessage();
                Session.closeStatement(ps);
                con.close();
                return message;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_removeURL(String url) throws Exception {
        String result = removeURL(url);
        if ("OK".equals(result)) {
            return new TemplateOKResult();
        }
        return new TemplateErrorResult(result);
    }

    public String setURL(String url) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO mnogosearch_url(mnogo_id, url) VALUES (? ,?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, url);
                ps.executeUpdate();
                handleURL("-a", url);
                Session.closeStatement(ps);
                con.close();
                return "OK";
            } catch (Exception e) {
                Session.getLog().error("Error setting Mnogosearch props", e);
                String message = e.getMessage();
                Session.closeStatement(ps);
                con.close();
                return message;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setURL(String url) throws Exception {
        String result = setURL(url);
        if ("OK".equals(result)) {
            return new TemplateOKResult();
        }
        return new TemplateErrorResult(result);
    }

    public TemplateModel FM_getSearchURLs() throws Exception {
        List resultList = getSearchURLs();
        if (resultList != null) {
            return new TemplateList(resultList);
        }
        return null;
    }

    public List getSearchURLs() throws Exception {
        List urlsList = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT url FROM mnogosearch_url WHERE mnogo_id = ?");
                ps.setLong(1, getId().getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    urlsList.add(rs.getString(1));
                }
                Session.closeStatement(ps);
                con.close();
                return urlsList;
            } catch (Exception e) {
                Session.getLog().error("Error setting Mnogosearch props", e);
                Session.closeStatement(ps);
                con.close();
                return null;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mnogosearch.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mnogosearch.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mnogosearch.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mnogosearch.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
