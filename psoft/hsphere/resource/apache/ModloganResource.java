package psoft.hsphere.resource.apache;

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
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ModloganResource.class */
public class ModloganResource extends Resource implements HostDependentResource {
    static final String default_path = "/modlogan";
    protected String dir;

    public ModloganResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String property = Session.getProperty("MODLOGAN_PATH");
        this.dir = property;
        if (property == null) {
            this.dir = default_path;
        }
    }

    public ModloganResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT dir FROM apache_modlogan WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.dir = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO apache_modlogan (id, dir) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.dir);
            ps.executeUpdate();
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        physicalDelete(getHostId());
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM apache_modlogan WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("mod_dir")) {
            return new TemplateString(this.dir);
        }
        try {
            if (key.equals("mconfig")) {
                return new TemplateString(getConfigEntry());
            }
            return super.get(key);
        } catch (Exception e) {
            getLog().warn("modlogan config " + key, e);
            return null;
        }
    }

    public String getConfigEntry() throws Exception {
        String versionNumber;
        long hostId = getHostId();
        HostEntry he = HostManager.getHost(hostId);
        SimpleHash root = new SimpleHash();
        root.put("modlogan", this);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        ArrayList version = new ArrayList(he.exec("modlogan-version", new LinkedList()));
        String scriptExecString = (String) version.get(0);
        int startOfVersion = scriptExecString.indexOf("modlogan");
        if (startOfVersion != -1) {
            new String("");
            int startOfVersion2 = startOfVersion + "modlogan".length() + 1;
            try {
                versionNumber = scriptExecString.substring(startOfVersion2, scriptExecString.indexOf(" ", startOfVersion2));
            } catch (StringIndexOutOfBoundsException e) {
                versionNumber = scriptExecString.substring(startOfVersion2);
            }
            StringTokenizer tokenizer = new StringTokenizer(versionNumber, ".");
            float denominator = 1.0f;
            float comparableVersionValue = 0.0f;
            while (tokenizer.hasMoreTokens()) {
                String number = tokenizer.nextToken();
                comparableVersionValue += Integer.parseInt(number) * (100000.0f / denominator);
                denominator *= 100.0f;
            }
            if (comparableVersionValue >= 8080.0f) {
                Session.getTemplate("/domain/modlogan_0.8.8.config").process(root, out);
            } else {
                Session.getTemplate("/domain/modlogan_old.config").process(root, out);
            }
        } else {
            Session.getLog().error("Modlogan version is unknown");
        }
        out.close();
        Session.getLog().info("End modlogan getConfigEntry");
        return sw.toString();
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
        return Localizer.translateMessage("bill.modlogan.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.modlogan.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.modlogan.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.modlogan.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("real_name").toString());
        l.add(this.dir);
        l.add(recursiveGet("login").toString());
        l.add(recursiveGet("group").toString());
        l.add(recursiveGet("trans_file").toString());
        HostEntry he = HostManager.getHost(targetHostId);
        he.exec("modlogan-init", l, getConfigEntry());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (this.initialized) {
            List l = new ArrayList();
            l.add(recursiveGet("real_name").toString());
            HostEntry he = HostManager.getHost(targetHostId);
            he.exec("modlogan-stop", l);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
