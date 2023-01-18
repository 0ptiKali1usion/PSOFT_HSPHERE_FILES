package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.miva.MivaOperator;
import psoft.hsphere.resource.miva.MivaOperatorFactory;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/MIVAEmpresaOnlyResource.class */
public class MIVAEmpresaOnlyResource extends MimeTypeResource {
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

    public MIVAEmpresaOnlyResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        Session.getLog().debug("Miva Empresa physicalCreate");
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        Session.getLog().debug("Miva Empressa physicalCreate");
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        MivaOperator mop = MivaOperatorFactory.getInstance().getMivaOperator(getVersion(), 2, this);
        mop.setHost(he);
        mop.installMivaEmpresa();
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        MivaOperator mop = MivaOperatorFactory.getInstance().getMivaOperator(getVersion(), 2, this);
        mop.setHost(he);
        mop.uninstallMivaEmpresa();
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("miva.demo_mode");
        }
        getVersion();
        getExt();
        getExecute();
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
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

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource
    public String getExt() {
        if (getVersion().equals("4x")) {
            this.ext = MIVA_4X_DEFAULT_EXT;
            this.mimeType = MIVA_4X_DEFAULT_TYPE;
        } else {
            this.ext = MIVA_3X_DEFAULT_EXT;
            this.mimeType = MIVA_3X_DEFAULT_TYPE;
        }
        return this.ext;
    }

    protected String getExecute() {
        if (this.execute != null) {
            return this.execute;
        }
        if (getVersion().equals("4x")) {
            this.execute = "mivavm";
        } else {
            this.execute = "miva.cgi";
        }
        return this.execute;
    }
}
