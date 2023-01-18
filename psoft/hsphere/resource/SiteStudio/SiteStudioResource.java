package psoft.hsphere.resource.SiteStudio;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SiteStudio/SiteStudioResource.class */
public class SiteStudioResource extends Resource {
    protected String login;
    protected String server;
    protected String port;
    protected String url;
    protected String dir;
    protected String email;

    public SiteStudioResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.login = (String) i.next();
        this.server = (String) i.next();
        this.port = (String) i.next();
        this.url = (String) i.next();
        this.dir = (String) i.next();
        this.email = (String) i.next();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO site_studio (id, login, server, port, url, dir, email, group_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.login);
            ps.setString(3, this.server);
            ps.setInt(4, Integer.parseInt(this.port));
            ps.setString(5, this.url);
            ps.setString(6, this.dir);
            ps.setString(7, this.email);
            ps.setLong(8, getParent().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public SiteStudioResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, server, port, url, dir, email FROM site_studio WHERE id = ?");
            ps.setLong(1, rid.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.server = rs.getString(2);
                this.port = rs.getString(3);
                this.url = rs.getString(4);
                this.dir = rs.getString(5);
                this.email = rs.getString(6);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "login".equals(key) ? new TemplateString(this.login) : "server".equals(key) ? new TemplateString(this.server) : "port".equals(key) ? new TemplateString(this.port) : "url".equals(key) ? new TemplateString(this.url) : "dir".equals(key) ? new TemplateString(this.dir) : "email".equals(key) ? new TemplateString(this.email) : super.get(key);
    }

    public TemplateModel FM_update(String login, String server, String port, String url, String dir, String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE site_studio SET login = ?, server = ?, port = ?, url = ?, dir = ?, email = ? WHERE id = ?");
            ps.setString(1, login);
            ps.setString(2, server);
            ps.setInt(3, Integer.parseInt(port));
            ps.setString(4, url);
            ps.setString(5, dir);
            ps.setString(6, email);
            ps.setLong(7, getId().getId());
            ps.executeUpdate();
            this.login = login;
            this.server = server;
            this.port = port;
            this.url = url;
            this.dir = dir;
            this.email = email;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
