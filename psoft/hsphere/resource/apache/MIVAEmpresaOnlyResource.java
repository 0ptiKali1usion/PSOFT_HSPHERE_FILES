package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.util.Collection;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.miva.MivaOperator;
import psoft.hsphere.resource.miva.MivaOperatorFactory;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/MIVAEmpresaOnlyResource.class */
public class MIVAEmpresaOnlyResource extends MimeTypeResource implements HostDependentResource {
    protected static final String MIVA_3X_DEFAULT_EXT = ".mv";
    protected static final String MIVA_3X_DEFAULT_TYPE = "application/x-miva";
    protected static final String MIVA_4X_DEFAULT_EXT = ".mvc";
    protected static final String MIVA_4X_DEFAULT_TYPE = "application/x-miva-compiled";
    protected String lic;
    protected String currentVersion;
    protected String execute;

    public MIVAEmpresaOnlyResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        Connection con = Session.getDb();
        try {
            physicalCreate(getHostId());
            Session.closeStatement(null);
            con.close();
            getVersion();
            getExt();
            getExecute();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public MIVAEmpresaOnlyResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        String properties;
        if (Double.parseDouble(getVersion()) >= 4.14d) {
            properties = "\nAddType application/x-miva-compiled .mvc\nAction application/x-miva-compiled /cgi-bin/mivavm\n";
        } else {
            properties = "\nAddType application/x-miva .mv\nAction application/x-miva /cgi-bin/miva.cgi\n";
        }
        return properties;
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            try {
                physicalDelete(getHostId());
            } catch (Exception ex) {
                Session.getLog().error("Error deleting miva :", ex);
                throw ex;
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        Session.getLog().debug("Miva Empressa physicalCreate");
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        HostEntry he = HostManager.getHost(targetHostId);
        MivaOperator mop = MivaOperatorFactory.getInstance().getMivaOperator(getVersion(), 1, this);
        mop.setHost(he);
        mop.installMivaEmpresa();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        Session.getLog().debug("Miva Empressa physicalDelete");
        HostEntry he = HostManager.getHost(targetHostId);
        MivaOperator mop = MivaOperatorFactory.getInstance().getMivaOperator(getVersion(), 1, this);
        mop.setHost(he);
        mop.uninstallMivaEmpresa();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("version")) {
            return new TemplateString(getVersion());
        }
        if (key.equals("ext")) {
            return new TemplateString(getExt());
        }
        if (key.equals("execute")) {
            return new TemplateString(getExecute());
        }
        return super.get(key);
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

    @Override // psoft.hsphere.resource.apache.MimeTypeResource
    public String getExt() {
        if (Double.parseDouble(getVersion()) >= 4.14d) {
            this.ext = MIVA_4X_DEFAULT_EXT;
            this.mimeType = MIVA_4X_DEFAULT_TYPE;
        } else {
            this.ext = MIVA_3X_DEFAULT_EXT;
            this.mimeType = MIVA_3X_DEFAULT_TYPE;
        }
        return this.ext;
    }

    protected String getExecute() {
        if (Double.parseDouble(getVersion()) >= 4.14d) {
            this.execute = "mivavm";
        } else {
            this.execute = "miva.cgi";
        }
        return this.execute;
    }
}
