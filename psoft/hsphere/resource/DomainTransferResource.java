package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.async.AsyncDeclinedException;
import psoft.hsphere.async.AsyncManager;
import psoft.hsphere.async.AsyncResourceException;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/DomainTransferResource.class */
public class DomainTransferResource extends OpenSRS {
    public static final int TRANSFER_IN_PROGRESS = 1;
    public static final int TRANSFER_DONE = 2;

    public DomainTransferResource(int type, Collection values) throws Exception {
        super(type, values);
        this.transferState = 1;
    }

    public DomainTransferResource(ResourceId id) throws Exception {
        super(id);
    }

    @Override // psoft.hsphere.resource.OpenSRS
    protected void finishThirdPartyAction(Registrar r, String name, String tld, String login, String password, Map cInfo, Map rInfo, ContactInfo ci, List dns) throws Exception {
        r.transfer(name, tld, login, password, cInfo, rInfo, cInfo, rInfo, dns);
        AsyncManager.getManager().add(this);
    }

    @Override // psoft.hsphere.resource.OpenSRS
    public boolean isTransfer() {
        return true;
    }

    public static boolean isTransfer(InitToken token) {
        return true;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public boolean isAsyncComplete() throws AsyncDeclinedException, AsyncResourceException {
        String name = DomainRegistrar.getName(this.domainName);
        String tld = DomainRegistrar.getTLD(this.domainName);
        try {
            Date expDate = getRegistrar().isTransfered(name, tld);
            if (expDate == null) {
                return false;
            }
            this.transferState = 2;
            setExpDate(expDate);
            return true;
        } catch (Exception e) {
            throw new AsyncResourceException(100, e);
        }
    }

    @Override // psoft.hsphere.async.AsyncResource
    public String getAsyncDescription() {
        return Localizer.translateMessage("opensrs.transfer", new Object[]{this.domainName});
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncTimeout() {
        return 168;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncInterval() {
        return 1;
    }

    @Override // psoft.hsphere.resource.OpenSRS, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("transfer_state") ? new TemplateString(this.transferState) : super.get(key);
    }

    private void setExpDate(Date d) throws SQLException {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(1, (-1) * this.period);
        this.lastPayed = c.getTime();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE opensrs SET last_payed = ?, transfer = ? WHERE id = ?");
            ps.setDate(1, new java.sql.Date(this.lastPayed.getTime()));
            ps.setInt(2, this.transferState);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return getSetupChargeDescription(now, "bill.opensrs.transfer");
    }
}
