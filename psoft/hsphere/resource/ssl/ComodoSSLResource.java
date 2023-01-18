package psoft.hsphere.resource.ssl;

import com.comodo.api.ComodoAPI;
import com.comodo.api.ComodoBeingProcessedException;
import com.comodo.api.ComodoException;
import com.comodo.api.ComodoOrder;
import com.comodo.api.ComodoSSL;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.SSL;
import psoft.hsphere.Session;
import psoft.hsphere.ThirdPartyResource;
import psoft.hsphere.async.AsyncDeclinedException;
import psoft.hsphere.async.AsyncManager;
import psoft.hsphere.async.AsyncResource;
import psoft.hsphere.async.AsyncResourceException;
import psoft.hsphere.fmacl.ComodoManager;
import psoft.hsphere.fmacl.ComodoManagerInfo;
import psoft.hsphere.resource.IIS.VirtualHostingResource;
import psoft.hsphere.resource.SSLProperties;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ssl/ComodoSSLResource.class */
public class ComodoSSLResource extends Resource implements ThirdPartyResource, AsyncResource {
    private static final Category log = Category.getInstance(ComodoSSLResource.class.getName());
    String orderNumber;
    int deliveryTime;
    String privateKey;
    ComodoSSL cert;
    int period;
    String product;
    String site;
    private transient Iterator _initValues;
    private boolean certificatInstalled;
    private Date issuedOn;

    public ComodoSSLResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.certificatInstalled = false;
        Iterator i = initValues.iterator();
        String productPeriod = (String) i.next();
        int pos = productPeriod.indexOf(95);
        this.period = Integer.parseInt(productPeriod.substring(pos + 1));
        this.product = productPeriod.substring(0, pos);
        log.info("PRODUCT: " + this.product + ", " + this.period);
        this.site = (String) i.next();
        this.privateKey = (String) i.next();
        this._initValues = i;
    }

    public ComodoSSLResource(ResourceId id) throws Exception {
        super(id);
        this.certificatInstalled = false;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT order_num, delivery_time, product, period, site, pri, cert, root, inter, cert_installed, issued_on FROM comodo_ssl WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.orderNumber = rs.getString("order_num");
                this.deliveryTime = rs.getInt("delivery_time");
                this.product = rs.getString("product");
                this.period = rs.getInt("period");
                this.site = rs.getString("site");
                this.privateKey = rs.getString("pri");
                String root = rs.getString("root");
                if (root != null) {
                    this.cert = new ComodoSSL(root, rs.getString("inter"), rs.getString(MerchantGatewayManager.MG_CERT_PREFIX));
                } else {
                    this.cert = null;
                }
                setCertificatInstalled(rs.getInt("cert_installed") == 1);
                setIssuedOn(rs.getTimestamp("issued_on") == null ? null : new Date(rs.getTimestamp("issued_on").getTime()));
                rs.close();
            } else {
                rs.close();
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected String getSite() {
        return this.site;
    }

    @Override // psoft.hsphere.ThirdPartyResource
    public void thirdPartyAction() throws Exception {
        String request = (String) this._initValues.next();
        String address1 = (String) this._initValues.next();
        String address2 = (String) this._initValues.next();
        String address3 = (String) this._initValues.next();
        String postalCode = (String) this._initValues.next();
        String dunsNumber = (String) this._initValues.next();
        String companyNumber = (String) this._initValues.next();
        String email = (String) this._initValues.next();
        this._initValues = null;
        ComodoManagerInfo cm = ComodoManager.getInfo();
        ComodoOrder order = SSLTools.orderLicense(cm.getLogin(), cm.getPassword(), this.product, this.period, 1, 2, address1, address2, address3, postalCode, dunsNumber, companyNumber, request, email);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO comodo_ssl (id, order_num, delivery_time, product, period, pri, site) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            String orderNumber = order.getOrderNumber();
            this.orderNumber = orderNumber;
            ps.setString(2, orderNumber);
            ps.setInt(3, order.getDeliveryTime());
            ps.setString(4, this.product);
            ps.setInt(5, this.period);
            ps.setString(6, this.privateKey);
            ps.setString(7, this.site);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            AsyncManager.getManager().add(this);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int getPeriod() {
        return this.period;
    }

    public String getProduct() {
        return this.product;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM comodo_ssl WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        Collection result = new ArrayList();
        result.add(getProduct() + "_" + getPeriod());
        return result;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public boolean isAsyncComplete() throws AsyncDeclinedException, AsyncResourceException {
        try {
            ComodoManagerInfo m = ComodoManager.getInfo();
            ComodoSSL _cert = ComodoAPI.collectSSL(m.getLogin(), m.getPassword(), this.orderNumber);
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            Date _issuedOn = TimeUtils.getDate();
            try {
                ps = con.prepareStatement("UPDATE comodo_ssl SET root = ?, inter = ?, cert = ?, issued_on=? WHERE id = ?");
                ps.setString(1, _cert.getRoot());
                ps.setString(2, _cert.getIntermediary());
                ps.setString(3, _cert.getCertificate());
                ps.setTimestamp(4, new Timestamp(_issuedOn.getTime()));
                ps.setLong(5, getId().getId());
                ps.executeUpdate();
                setIssuedOn(_issuedOn);
                Session.closeStatement(ps);
                con.close();
                this.cert = _cert;
                installCert();
                return true;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } catch (Exception e) {
            throw new AsyncResourceException(100, e);
        } catch (ComodoException e2) {
            int code = e2.getCode();
            switch (code) {
                case 20:
                case 21:
                    throw new AsyncDeclinedException(code, e2.getMessage());
                case 22:
                    return false;
                default:
                    throw new AsyncResourceException(100, (Throwable) e2);
            }
        } catch (ComodoBeingProcessedException e3) {
            return false;
        }
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.period;
    }

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        String periodS = (String) i.next();
        int period = 1;
        if (periodS != null && periodS.length() > 0) {
            try {
                period = (int) USFormat.parseDouble(periodS);
            } catch (Exception e) {
                log.warn("Problem with parsing double: " + periodS, e);
            }
        }
        return period;
    }

    public void installCert() throws Exception {
        SSL ssl;
        SSLProperties sslp;
        PreparedStatement ps = null;
        ResourceId rid = getParent().findChild("ssl");
        if (rid != null) {
            ssl = (SSL) rid.get();
            sslp = new SSLProperties(ssl.getProperties());
            sslp.setKey(this.privateKey);
            sslp.setCertificate(this.cert.getCertificate());
            ssl.installCertificatePair(sslp);
        } else {
            Collection _params = new ArrayList();
            Resource _parent = getParent().get();
            if (_parent instanceof VirtualHostingResource) {
                _params.add("0");
                _params.add("0");
            }
            _params.add(this.privateKey);
            _params.add(this.cert.getCertificate());
            ssl = (SSL) _parent.addChild("ssl", "", _params).get();
            sslp = new SSLProperties(ssl.getProperties());
        }
        sslp.setCertificateChain(this.cert.getIntermediary());
        ssl.setProperties(sslp);
        ssl.installCertificateChain(sslp);
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement("UPDATE comodo_ssl SET cert_installed = ? WHERE id = ?");
                ps.setInt(1, 1);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                setCertificatInstalled(true);
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException sqlex) {
                Session.getLog().error("Error while setting cert_installed for comodossl " + getId().getId(), sqlex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_installCertificate() throws Exception {
        installCert();
        return this;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public String getAsyncDescription() {
        return Localizer.translateMessage("comodossl.desc", new Object[]{this.orderNumber, getSite()});
    }

    @Override // psoft.hsphere.async.AsyncResource
    public boolean isAsyncAutoRemove() {
        return false;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncTimeout() {
        return 48;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public int getAsyncInterval() {
        return 4;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public void asyncDelete() throws Exception {
        setupRefund(TimeUtils.getDate());
        delete(false);
    }

    public boolean isCeritificateAvailable() {
        return getCert() != null;
    }

    public ComodoSSL getCert() {
        return this.cert;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("cert_available".equals(key)) {
            return new TemplateString(isCeritificateAvailable());
        }
        if ("cert_installed".equals(key)) {
            return new TemplateString(isCertificatInstalled());
        }
        if ("product_id".equals(key)) {
            return new TemplateString(getProduct());
        }
        if ("period".equals(key)) {
            return new TemplateString(getPeriod());
        }
        if ("expires".equals(key)) {
            return new TemplateString(getShortLocalizedDate(getExpiresOn()));
        }
        if ("issued".equals(key)) {
            return new TemplateString(getShortLocalizedDate(getIssuedOn()));
        }
        if ("descr".equals(key)) {
            return new TemplateString(getProductDescription(getProduct(), getPeriod()));
        }
        if ("ext_description".equals(key)) {
            return new TemplateString(getExtendedResourceDescription());
        }
        return super.get(key);
    }

    public String getProductDescription(String _product_type, int _period) {
        return Localizer.translateMessage("msg.comodo.ssl.type." + _product_type, new String[]{Integer.toString(_period), getShortLocalizedDate(getExpiresOn())});
    }

    public String getExtendedResourceDescription() {
        String status;
        if (!isCeritificateAvailable()) {
            status = "msg.comodo.ssl.status.not_arrived";
        } else if (isCertificatInstalled()) {
            status = "msg.comodo.ssl.status.installed";
        } else {
            status = "msg.comodo.ssl.status.arived_not_installed";
        }
        return Localizer.translateMessage("mgs.comodo.ssl.extended_description", new String[]{getProductDescription(getProduct(), getPeriod()), Localizer.translateMessage(status)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return getProductDescription(getProduct(), getPeriod());
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return getProductDescription(getProduct(), getPeriod());
    }

    public boolean isCertificatInstalled() {
        return this.certificatInstalled;
    }

    public void setCertificatInstalled(boolean certificatInstalled) {
        this.certificatInstalled = certificatInstalled;
    }

    public Date getIssuedOn() {
        return this.issuedOn;
    }

    public void setIssuedOn(Date issuedOn) {
        this.issuedOn = issuedOn;
    }

    public Date getExpiresOn() {
        if (getIssuedOn() != null) {
            Calendar c = TimeUtils.getCalendar();
            c.setTime(getIssuedOn());
            c.add(1, getPeriod());
            return c.getTime();
        }
        return null;
    }

    public String getShortLocalizedDate(Date _date) {
        if (_date != null) {
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance(3);
            return sdf.format(_date);
        }
        return "";
    }
}
