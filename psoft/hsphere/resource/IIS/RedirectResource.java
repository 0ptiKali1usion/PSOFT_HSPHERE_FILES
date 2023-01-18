package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/RedirectResource.class */
public class RedirectResource extends psoft.hsphere.resource.RedirectResource {
    protected static final int EXACT_VAL = 1;
    protected static final int BELOW_VAL = 2;
    protected static final int PERM_VAL = 4;

    public RedirectResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        String exact = (String) i.next();
        String below = (String) i.next();
        String perm = (String) i.next();
        int redirectType = getType(exact, below, perm);
        this.stat = Integer.toString(redirectType);
        this.url_path = (String) i.next();
        this.url = (String) i.next();
        if (i.hasNext()) {
            this.protocol = validateProtocol((String) i.next(), redirectType);
        } else {
            this.protocol = "http";
        }
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

    private void update(String url_path, String url, String stat) throws Exception {
        update(url_path, url, stat, getHostId());
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    private void update(String url_path, String url, String stat, long targetHostId) throws Exception {
        String hostnum = recursiveGet("hostnum").toString();
        String name = recursiveGet("real_name").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("redirect-create.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"url-path", url_path}, new String[]{"url", url}, new String[]{"status", stat}});
    }

    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        int redirectType = Integer.parseInt(this.stat);
        update(this.url_path, getUrl(this.protocol, this.url, redirectType), this.stat, targetHostId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum = recursiveGet("hostnum").toString();
        String name = recursiveGet("real_name").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        he.exec("redirect-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"url-path", this.url_path}});
    }

    @Override // psoft.hsphere.resource.RedirectResource
    public String getHostName() throws Exception {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    private int getType(String exact, String below, String perm) throws Exception {
        int rtype = 0;
        if (exact.equals("exact_on")) {
            rtype = 0 + 1;
        }
        if (below.equals("below_on")) {
            rtype += 2;
        }
        if (perm.equals("perm_on")) {
            rtype += 4;
        }
        return rtype;
    }

    private String getUrl(String protocol, String newUrl, int redirectType) {
        return (redirectType & 2) == 2 ? newUrl : protocol + "://" + newUrl;
    }

    private String validateProtocol(String protocol, int redirectType) {
        String result;
        try {
            if ((redirectType & 2) == 2) {
                result = "";
            } else {
                if (!"http".equals(protocol) && !"ftp".equals(protocol)) {
                    if (!"https".equals(protocol)) {
                        result = "http";
                    }
                }
                result = protocol;
            }
        } catch (Exception ex) {
            Session.getLog().debug("IIS Protocol validation problem.Protocol has been set to default (http):", ex);
            result = "http";
        }
        return result;
    }

    public TemplateModel FM_update(String exact, String below, String perm, String url_path, String url) throws Exception {
        return FM_update(exact, below, perm, url_path, url, "http");
    }

    public TemplateModel FM_update(String exact, String below, String perm, String url_path, String url, String protocol) throws Exception {
        int rtype = getType(exact, below, perm);
        String stat = Integer.toString(rtype);
        String vprotocol = validateProtocol(protocol, rtype);
        update(url_path, getUrl(vprotocol, url, rtype), stat);
        this.stat = stat;
        this.url_path = url_path;
        this.url = url;
        this.protocol = vprotocol;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE vhost_redirect SET  status=?, url_path=?, url = ?, protocol = ?WHERE id = ?");
            ps.setString(1, this.stat);
            ps.setString(2, this.url_path);
            ps.setString(3, this.url);
            ps.setString(4, this.protocol);
            ps.setLong(5, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private String getRDesc() {
        int rtype = Integer.parseInt(this.stat);
        String result = getUrl(validateProtocol(this.protocol, rtype), this.url, rtype);
        if (rtype > 0) {
            String result2 = result + " ( ";
            if ((rtype & 1) == 1) {
                result2 = result2 + "exact ";
            }
            if ((rtype & 2) == 2) {
                result2 = result2 + "below ";
            }
            if ((rtype & 4) == 4) {
                result2 = result2 + "perm ";
            }
            result = result2 + ")";
        }
        return this.url_path + "->" + result;
    }

    @Override // psoft.hsphere.resource.RedirectResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("stat")) {
            return new TemplateString(this.stat);
        }
        int redirectType = Integer.parseInt(this.stat);
        if (key.equals("isExact")) {
            if ((redirectType & 1) == 1) {
                return new TemplateString("exact_on");
            }
            return new TemplateString("exact_off");
        } else if (key.equals("isBelow")) {
            if ((redirectType & 2) == 2) {
                return new TemplateString("below_on");
            }
            return new TemplateString("below_off");
        } else if (key.equals("isPerm")) {
            if ((redirectType & 4) == 4) {
                return new TemplateString("perm_on");
            }
            return new TemplateString("perm_off");
        } else if (key.equals("protocol")) {
            return new TemplateString(validateProtocol(this.protocol, redirectType));
        } else {
            if (key.equals("rdescription")) {
                return new TemplateString(getRDesc());
            }
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return null;
    }
}
