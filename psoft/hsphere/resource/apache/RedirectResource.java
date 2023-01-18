package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/RedirectResource.class */
public class RedirectResource extends psoft.hsphere.resource.RedirectResource {
    public RedirectResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.stat = (String) i.next();
        this.url_path = (String) i.next();
        this.url = (String) i.next();
        this.protocol = (String) i.next();
    }

    public RedirectResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT status, url_path, url, protocol FROM vhost_redirect WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.stat = rs.getString(1);
                this.url_path = rs.getString(2);
                this.url = rs.getString(3);
                this.protocol = rs.getString(4);
            } else {
                notFound();
            }
        } finally {
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        String conf;
        String conf2 = "Redirect " + this.stat + " /";
        if (this.url_path != null) {
            conf2 = conf2 + this.url_path;
        }
        String conf3 = conf2 + " ";
        if (!this.stat.equals("gone")) {
            if ("http".equals(this.protocol) || "ftp".equals(this.protocol) || "https".equals(this.protocol)) {
                conf = conf3 + " " + this.protocol + "://";
            } else {
                conf = conf3 + " http://";
            }
            conf3 = conf + this.url;
        }
        return conf3 + "\n";
    }

    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.RedirectResource
    public String getHostName() throws Exception {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    public TemplateModel FM_update(String type, String url_path, String url, String protocol) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE vhost_redirect SET status = ?, url_path = ?, url = ?, protocol = ? WHERE id = ?");
            ps.setString(1, this.stat);
            ps.setString(2, url_path);
            ps.setString(3, url);
            ps.setString(4, protocol);
            ps.setLong(5, getId().getId());
            ps.executeUpdate();
            con.close();
            this.stat = type;
            this.url_path = url_path;
            this.url = url;
            this.protocol = protocol;
            return this;
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("stat")) {
            return new TemplateString(this.stat);
        }
        if (key.equals("protocol")) {
            if ("http".equals(this.protocol) || "ftp".equals(this.protocol) || "https".equals(this.protocol)) {
                return new TemplateString(this.protocol);
            }
            return new TemplateString("http");
        }
        return super.get(key);
    }
}
