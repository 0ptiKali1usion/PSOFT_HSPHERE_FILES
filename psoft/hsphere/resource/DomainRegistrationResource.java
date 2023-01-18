package psoft.hsphere.resource;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.async.AsyncDeclinedException;
import psoft.hsphere.async.AsyncManager;
import psoft.hsphere.async.AsyncResourceException;
import psoft.hsphere.resource.registrar.AsyncRegistrar;
import psoft.hsphere.resource.registrar.Registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/DomainRegistrationResource.class */
public class DomainRegistrationResource extends OpenSRS {
    public DomainRegistrationResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public DomainRegistrationResource(ResourceId id) throws Exception {
        super(id);
    }

    @Override // psoft.hsphere.resource.OpenSRS
    protected void finishThirdPartyAction(Registrar r, String name, String tld, String login, String password, Map cInfo, Map rInfo, ContactInfo ci, List dns) throws Exception {
        r.register(name, tld, login, password, this.period, cInfo, rInfo, cInfo, rInfo, dns, ci.getExtraInfo());
        if ((r instanceof AsyncRegistrar) && ((AsyncRegistrar) r).isAsyncTLD(tld)) {
            AsyncManager.getManager().add(this);
        }
    }

    @Override // psoft.hsphere.resource.OpenSRS
    public boolean isTransfer() {
        return false;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public boolean isAsyncComplete() throws AsyncDeclinedException, AsyncResourceException {
        String name = DomainRegistrar.getName(this.domainName);
        String tld = DomainRegistrar.getTLD(this.domainName);
        try {
            AsyncRegistrar a = getAsyncRegistrar();
            return a.isRegComplete(name, tld);
        } catch (Exception e) {
            throw new AsyncResourceException(100, e);
        }
    }

    @Override // psoft.hsphere.async.AsyncResource
    public String getAsyncDescription() {
        return Localizer.translateMessage("opensrs.desc", new Object[]{this.domainName});
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncTimeout() {
        AsyncRegistrar a = getAsyncRegistrar();
        if (a == null) {
            return 0;
        }
        return a.getRegTimeout(DomainRegistrar.getTLD(this.domainName));
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncInterval() {
        AsyncRegistrar a = getAsyncRegistrar();
        if (a == null) {
            return 0;
        }
        return a.getRegCheckInterval(DomainRegistrar.getTLD(this.domainName));
    }

    private AsyncRegistrar getAsyncRegistrar() {
        Registrar r = null;
        try {
            r = getRegistrar();
        } catch (Exception e) {
            Session.getLog().warn(e);
        }
        if (r instanceof AsyncRegistrar) {
            return r;
        }
        return null;
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return getSetupChargeDescription(now, "bill.opensrs.setup");
    }
}
