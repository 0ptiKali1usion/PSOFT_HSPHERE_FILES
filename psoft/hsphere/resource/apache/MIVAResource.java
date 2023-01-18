package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Hashtable;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.miva.MivaLicenseProvider;
import psoft.hsphere.resource.miva.MivaOperator;
import psoft.hsphere.resource.miva.MivaOperatorFactory;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/MIVAResource.class */
public class MIVAResource extends Resource implements HostDependentResource {
    protected String lic;
    protected String currentVersion;
    protected String ext;
    protected String execute;
    protected MivaOperator mivaOperator;

    public MIVAResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.mivaOperator = null;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        String domain = recursiveGet("name").toString();
        this.lic = MivaLicenseProvider.getInstance().getLicense(domain);
        try {
            ps = con.prepareStatement("INSERT INTO miva_merch(id, lic, ver) VALUES(?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.lic);
            ps.setString(3, getVersion());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            physicalCreate(getHostId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public MIVAResource(ResourceId rId) throws Exception {
        super(rId);
        this.mivaOperator = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT lic, ver FROM miva_merch WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.lic = rs.getString("lic");
                String ver = rs.getString("ver");
                if (ver != null) {
                    this.currentVersion = ver;
                } else {
                    this.currentVersion = "4.14";
                }
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.lic == null) {
            return;
        }
        Exception causedException = null;
        if (this.initialized) {
            try {
                physicalDelete(getHostId());
            } catch (Exception ex) {
                Session.getLog().error("Error deleting miva :", ex);
                causedException = ex;
            }
        }
        try {
            MivaLicenseProvider.getInstance().releaseLic(this.lic);
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("DELETE FROM miva_merch WHERE lic = ?");
                ps.setString(1, this.lic);
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
            throw new Exception("MIVA: unable to free lic#" + this.lic + ", " + e.toString());
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("lic")) {
            return new TemplateString(this.lic);
        }
        if (key.equals("version")) {
            return new TemplateString(getVersion());
        }
        if (key.equals("ext")) {
            return new TemplateString(getExt());
        }
        if (key.equals("execute")) {
            return new TemplateString(getExecute());
        }
        if (key.equals("setup_url")) {
            try {
                return new TemplateString(getSetupURL());
            } catch (Exception e) {
                return new TemplateString("ERROR");
            }
        } else if (key.equals("admin_url")) {
            try {
                return new TemplateString(getAdminURL());
            } catch (Exception e2) {
                return new TemplateString("ERROR");
            }
        } else {
            if (key.equals("merchant_url")) {
                try {
                    return new TemplateString(getMivaOperator().getMivaMerchantURL());
                } catch (Exception ex) {
                    Session.getLog().error("Unable to get miva merchant setup URL ", ex);
                }
            }
            if (key.equals("uninstall_url")) {
                try {
                    return new TemplateString(getMivaOperator().getMivaMerchantUninstallURL());
                } catch (Exception ex2) {
                    Session.getLog().error("Unable to get miva merchant uninstall URL ", ex2);
                }
            }
            if (key.equals("nat_support")) {
                try {
                    return new TemplateString(isNatSupport() ? "YES" : "NO");
                } catch (Exception e3) {
                    return new TemplateString("NO");
                }
            }
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        Session.getLog().debug("Miva Merchant physicalCreate");
        HostEntry he = HostManager.getHost(targetHostId);
        getMivaOperator().setHost(he);
        getMivaOperator().installMerchantBundle();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        Session.getLog().debug("Miva Merchant physicalDelete");
        HostEntry he = HostManager.getHost(targetHostId);
        getMivaOperator().setHost(he);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    protected String getVersion() {
        if (this.currentVersion != null) {
            return this.currentVersion;
        }
        try {
            HostEntry he = HostManager.getHost(recursiveGet("host_id"));
            this.currentVersion = he.getOption("miva");
        } catch (Throwable th) {
            this.currentVersion = "4.14";
        }
        if (this.currentVersion == null) {
            this.currentVersion = "4.14";
        }
        return this.currentVersion;
    }

    protected String getExt() {
        if (this.ext != null) {
            return this.ext;
        }
        if (Double.parseDouble(getVersion()) >= 4.14d) {
            this.ext = ".mvc";
        } else {
            this.ext = ".mv";
        }
        return this.ext;
    }

    protected String getExecute() {
        if (this.execute != null) {
            return this.execute;
        }
        if (Double.parseDouble(getVersion()) >= 4.14d) {
            this.execute = "mivavm";
        } else {
            this.execute = "miva.cgi";
        }
        return this.execute;
    }

    public TemplateModel FM_setup() throws Exception {
        getMivaOperator(true).setHost(HostManager.getHost(getHostId()));
        Hashtable result = getMivaOperator().configureMerchant();
        MivaLicenseProvider.getInstance().setLicenseInstalled(recursiveGet("name").toString(), this.lic);
        return new TemplateHash(result);
    }

    protected String getSetupURL() throws Exception {
        return getMivaOperator().getMivaMerchantSetupURL();
    }

    protected String getAdminURL() throws Exception {
        return getMivaOperator().getMivaMerchantAdminURL();
    }

    protected boolean isNatSupport() throws Exception {
        try {
            Session.getProperty("IPS-XML-FILENAME");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected MivaOperator getMivaOperator() throws Exception {
        return getMivaOperator(false);
    }

    protected MivaOperator getMivaOperator(boolean reload) throws Exception {
        if (this.mivaOperator == null || reload) {
            this.mivaOperator = MivaOperatorFactory.getInstance().getMivaOperator(getVersion(), 1, this);
        }
        return this.mivaOperator;
    }
}
