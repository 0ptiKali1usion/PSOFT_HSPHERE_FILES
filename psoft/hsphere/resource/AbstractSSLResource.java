package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.SSL;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/AbstractSSLResource.class */
public abstract class AbstractSSLResource extends Resource implements SSL {
    private SSLProperties properties;

    @Override // psoft.hsphere.SSL
    public abstract void installCertificatePair(SSLProperties sSLProperties) throws Exception;

    @Override // psoft.hsphere.SSL
    public abstract void updateCertificate(SSLProperties sSLProperties) throws Exception;

    @Override // psoft.hsphere.SSL
    public abstract void installCACert(SSLProperties sSLProperties) throws Exception;

    @Override // psoft.hsphere.SSL
    public abstract void installCertificateChain(SSLProperties sSLProperties) throws Exception;

    @Override // psoft.hsphere.SSL
    public abstract void installRevocationCert(SSLProperties sSLProperties) throws Exception;

    @Override // psoft.hsphere.SSL
    public abstract void updateCertificateProperties(SSLProperties sSLProperties) throws Exception;

    public AbstractSSLResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.properties = null;
    }

    public AbstractSSLResource(ResourceId id) throws Exception {
        super(id);
        this.properties = null;
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
        return Localizer.translateMessage("bill.ssl.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssl.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssl.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ssl.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        Session.getLog().debug("SSL was deleted by IP");
        delete(false);
    }

    @Override // psoft.hsphere.SSL
    public SSLProperties getProperties() {
        if (this.properties == null) {
            this.properties = new SSLProperties();
        }
        return this.properties;
    }

    @Override // psoft.hsphere.SSL
    public void setProperties(SSLProperties properties) {
        this.properties = properties;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("chain") ? new TemplateString(getProperties().getChain()) : key.equals("rev") ? new TemplateString(getProperties().getRev()) : key.equals(MerchantGatewayManager.MG_CERT_PREFIX) ? new TemplateString(getProperties().getCert()) : key.equals("vdepth") ? new TemplateString(getProperties().getVdepth()) : key.equals("vclient") ? new TemplateString(getProperties().getVclient()) : key.equals("site_name") ? new TemplateString(getProperties().getSiteName()) : key.equals("forced") ? new TemplateString(getProperties().getForced()) : key.equals("need128") ? new TemplateString(getProperties().getNeed128()) : super.get(key);
    }
}
