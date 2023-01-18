package psoft.hsphere.resource.zeus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/MIVAResource.class */
public class MIVAResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".mv";
    protected static final String DEFAULT_TYPE = "application/x-miva";
    protected String lic;

    public MIVAResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (this.ext == null) {
            this.ext = DEFAULT_EXT;
        }
        if (this.mimeType == null) {
            this.mimeType = DEFAULT_TYPE;
        }
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT lic FROM miva_lic WHERE state = 0");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.lic = rs.getString(1);
                HostEntry he = recursiveGet("host");
                String login = recursiveGet("login").toString();
                String group = recursiveGet("group").toString();
                String dir = recursiveGet("path").toString();
                Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
                List list = new ArrayList();
                list.add(login);
                list.add(group);
                list.add(dir);
                he.exec("miva-empresa-install", list);
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("UPDATE miva_lic SET state = 1 WHERE lic = ?");
                ps2.setString(1, this.lic);
                ps2.executeUpdate();
                ps2.close();
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO miva_merch(lic, id) VALUES(?, ?)");
                ps3.setString(1, this.lic);
                ps3.setLong(2, getId().getId());
                ps3.executeUpdate();
                Session.closeStatement(ps3);
                con.close();
                return;
            }
            throw new HSUserException("mivaresource.license");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public MIVAResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT lic FROM miva_merch WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.lic = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.zeus.MimeTypeResource, psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return super.getServerConfig() + "modules!map!handlers!" + normalizedExt() + "   /cgi-bin/miva.cgi\n";
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        HostEntry he = recursiveGet("host");
        String login = recursiveGet("login").toString();
        String dir = recursiveGet("path").toString();
        Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
        List list = new ArrayList();
        list.add(login);
        list.add(dir);
        he.exec("miva-empresa-uninstall", list);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM miva_merch WHERE lic = ?");
                ps2.setString(1, this.lic);
                ps2.executeUpdate();
                ps2.close();
                ps = con.prepareStatement("UPDATE miva_lic SET state = 0 WHERE lic = ?");
                ps.setString(1, this.lic);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                throw new Exception("MIVA: unable to free lic#" + this.lic + ", " + e.toString());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("lic") ? new TemplateString(this.lic) : super.get(key);
    }
}
