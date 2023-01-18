package psoft.hsphere.resource.epayment;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.epayment.PaymentInstrument;
import psoft.epayment.PaymentInstrumentException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;
import psoft.validators.ServletRequestAccessor;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/BillingInfoObject.class */
public class BillingInfoObject implements BillingInfo, TemplateHashModel {

    /* renamed from: id */
    protected long f191id;
    protected String name;
    protected String lastName;
    protected String company;
    protected String address1;
    protected String address2;
    protected String city;
    protected String state;
    protected String state2;
    protected String postalCode;
    protected String country;
    protected String phone;
    protected String email;
    protected String type;
    protected Date stopDate;
    protected String reason;
    protected long user_id;

    /* renamed from: pi */
    protected PaymentInstrument f192pi;
    protected String exemptionCode;
    protected Timestamp exemptionApproved;
    protected Timestamp exemptionRejected;

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public long getId() {
        return this.f191id;
    }

    public boolean equals(Object o) {
        return getId() == ((BillingInfoObject) o).getId();
    }

    public void clearStopDate() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE billing_info SET stop_date = ?, reason = ? WHERE id = ?");
            ps.setNull(1, 91);
            ps.setNull(2, 12);
            ps.setLong(3, this.f191id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setStopDate(String reason) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE billing_info SET stop_date = ?, reason = ? WHERE id = ?");
            Date sQLDate = TimeUtils.getSQLDate();
            this.stopDate = sQLDate;
            ps.setDate(1, sQLDate);
            this.reason = reason;
            ps.setString(2, reason);
            ps.setLong(3, this.f191id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean equals(String name, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) {
        return iseq(name, this.name) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(state2, this.state2) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email);
    }

    public boolean equals(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) {
        return iseq(name, this.name) && iseq(lastName, this.lastName) && iseq(company, this.company) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(state2, this.state2) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email);
    }

    public boolean equals(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String exemptionCode) {
        return iseq(name, this.name) && iseq(lastName, this.lastName) && iseq(company, this.company) && iseq(address1, this.address1) && iseq(address2, this.address2) && iseq(city, this.city) && iseq(state, this.state) && iseq(state2, this.state2) && iseq(postalCode, this.postalCode) && iseq(country, this.country) && iseq(phone, this.phone) && iseq(email, this.email) && iseq(exemptionCode, this.exemptionCode);
    }

    protected boolean iseq(String a, String b) {
        return (a != null && a.equals(b)) || b == a;
    }

    public HashMap checkCC(long acctid) throws Exception, PaymentInstrumentException {
        return Session.getMerchantGateway(this.f192pi.getType()).checkCVV(acctid, this.f192pi);
    }

    public HashMap charge(String message, double amount) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).charge(message, amount, this.f192pi);
    }

    public HashMap charge(long id, String message, double amount) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).charge(id, message, amount, this.f192pi);
    }

    public HashMap auth(String message, double amount) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).authorize(message, amount, this.f192pi);
    }

    public HashMap auth(long id, String message, double amount) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).authorize(id, message, amount, this.f192pi);
    }

    public HashMap capture(String message, HashMap data) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).capture(message, data, this.f192pi);
    }

    public HashMap capture(long id, String message, HashMap data) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).capture(id, message, data, this.f192pi);
    }

    public HashMap void_auth(String message, HashMap data) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).voidAuthorize(message, data, this.f192pi);
    }

    public HashMap void_auth(long id, String message, HashMap data) throws Exception, PaymentInstrumentException {
        this.f192pi.checkValid();
        return Session.getMerchantGateway(this.f192pi.getType()).voidAuthorize(id, message, data, this.f192pi);
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getAddress() {
        return this.address1 + ((this.address2 == null || "".equals(this.address2)) ? "" : "\n" + this.address2);
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCity() {
        return this.city;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getState() {
        if (this.state != null) {
            return this.state;
        }
        return this.state2;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getState2() {
        return this.state2;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getZip() {
        return this.postalCode;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCountry() {
        return this.country;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getEmail() {
        return this.email;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getPhone() {
        return this.phone;
    }

    public String getExemptionCode() {
        return this.exemptionCode;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getType() {
        return this.type;
    }

    public int getBillingType() {
        if (this.f191id == 0) {
            return 0;
        }
        if (this.f191id == -1) {
            return -1;
        }
        return this.f192pi.getBillingType();
    }

    public String getBillingTypeString() {
        return this.f191id == 0 ? "NONE" : this.f191id == -1 ? "TRIAL" : this.type;
    }

    public PaymentInstrument getPaymentInstrument() {
        return this.f192pi;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f191id);
        }
        if (key.equals("bi_id")) {
            return new TemplateString(Long.toString(this.f191id));
        }
        if (!key.equals("name") && !key.equals("first_name")) {
            if (key.equals("last_name")) {
                return new TemplateString(this.lastName);
            }
            if (!key.equals("company") && !key.equals("org_name")) {
                if (key.equals("address1")) {
                    return new TemplateString(this.address1);
                }
                if (key.equals("address2")) {
                    return new TemplateString(this.address2);
                }
                if (key.equals("city")) {
                    return new TemplateString(this.city);
                }
                if (key.equals("state")) {
                    return new TemplateString(this.state);
                }
                if (key.equals("state2")) {
                    return new TemplateString(this.state2);
                }
                if (key.equals("postal_code")) {
                    return new TemplateString(this.postalCode);
                }
                if (key.equals("stop_date")) {
                    return new TemplateString(this.stopDate);
                }
                if (key.equals("reason")) {
                    return new TemplateString(this.reason);
                }
                if (key.equals("country")) {
                    return this.country == null ? new TemplateString("US") : new TemplateString(this.country);
                } else if (key.equals("phone")) {
                    return new TemplateString(this.phone);
                } else {
                    if (key.equals("type")) {
                        return new TemplateString(getBillingTypeString());
                    }
                    if (key.equals("email")) {
                        return new TemplateString(this.email);
                    }
                    if (key.equals("pi") && (this.f192pi instanceof TemplateModel)) {
                        return this.f192pi;
                    }
                    if (key.equals("exemption_code")) {
                        return new TemplateString(this.exemptionCode);
                    }
                    if (key.equals("negative_date")) {
                        try {
                            Date d = getNegativeDate();
                            return new TemplateString(DateFormat.getDateInstance(3).format((java.util.Date) d));
                        } catch (Exception e) {
                        }
                    }
                    return "status".equals(key) ? Resource.STATUS_OK : AccessTemplateMethodWrapper.getMethod(this, key);
                }
            }
            return new TemplateString(this.company);
        }
        return new TemplateString(this.name);
    }

    public BillingInfoObject(String name, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi) throws Exception {
        this(name, "", "", address1, address2, city, state, "", postalCode, country, phone, email, type, pi);
    }

    public BillingInfoObject(String name, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi) throws Exception {
        this(name, "", "", address1, address2, city, state, state2, postalCode, country, phone, email, type, pi);
    }

    public BillingInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi) throws Exception {
        this(name, lastName, company, address1, address2, city, state, "", postalCode, country, phone, email, type, pi);
    }

    public BillingInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi) throws Exception {
        this(name, lastName, company, address1, address2, city, state, state2, postalCode, country, phone, email, type, pi, null);
    }

    public BillingInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi, String exemptionCode) throws Exception {
        this(name, lastName, company, address1, address2, city, state, state2, postalCode, country, phone, email, type, pi, exemptionCode, 0L);
    }

    public BillingInfoObject(String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String type, PaymentInstrument pi, String exemptionCode, long userId) throws Exception {
        this.exemptionCode = null;
        this.exemptionApproved = null;
        this.exemptionRejected = null;
        this.f191id = Resource.getNewId();
        this.name = name;
        this.lastName = lastName;
        this.company = company;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.state2 = state2;
        this.postalCode = postalCode;
        this.country = country;
        this.phone = phone;
        this.email = email;
        this.type = type;
        this.exemptionCode = exemptionCode;
        if (pi != null) {
            this.f192pi = pi.copy(this);
        }
        if (userId == 0) {
            save();
        } else {
            save(userId);
        }
    }

    public BillingInfoObject(BillingInfoObject bio, Iterator i) throws Exception {
        this.exemptionCode = null;
        this.exemptionApproved = null;
        this.exemptionRejected = null;
        this.f191id = Resource.getNewId();
        this.name = bio.name;
        this.lastName = bio.lastName;
        this.company = bio.company;
        this.address1 = bio.address1;
        this.address2 = bio.address2;
        this.city = bio.city;
        this.state = bio.state;
        this.state2 = bio.state2;
        this.postalCode = bio.postalCode;
        this.country = bio.country;
        this.phone = bio.phone;
        this.email = bio.email;
        this.type = i == null ? bio.type : (String) i.next();
        this.exemptionCode = bio.exemptionCode;
        save();
        if (i == null) {
            this.f192pi = bio.f192pi;
        } else {
            this.f192pi = GenPaymentInstrument.getPaymentInstrument(this.type, this, i);
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/BillingInfoObject$PIUpdater.class */
    class PIUpdater implements TemplateMethodModel {
        PIUpdater() {
            BillingInfoObject.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            if (BillingInfoObject.this.f192pi != null) {
                throw new TemplateModelException("PI already defined");
            }
            try {
                List l2 = HTMLEncoder.decode(l);
                BillingInfoObject.this.f192pi = GenPaymentInstrument.getPaymentInstrument(BillingInfoObject.this.type, BillingInfoObject.this, l2.iterator());
                return this;
            } catch (Exception e) {
                Session.getLog().warn("Updating pi", e);
                return null;
            }
        }
    }

    public void setPaymentInfo(Iterator i) throws Exception {
        this.f192pi = GenPaymentInstrument.getPaymentInstrument(this.type, this, i);
    }

    public BillingInfoObject(NameModifier nm) throws Exception {
        this.exemptionCode = null;
        this.exemptionApproved = null;
        this.exemptionRejected = null;
        this.f191id = Resource.getNewId();
        Accessor a = new ServletRequestAccessor(Session.getRequest());
        this.name = a.get(nm.getName("name"));
        if (this.name == null || "".equals(this.name)) {
            this.name = a.get(nm.getName("first_name"));
        }
        this.lastName = a.get(nm.getName("last_name"));
        this.company = a.get(nm.getName("company"));
        this.address1 = a.get(nm.getName("address1"));
        this.address2 = a.get(nm.getName("address2"));
        this.city = a.get(nm.getName("city"));
        this.state = a.get(nm.getName("state"));
        this.state2 = a.get(nm.getName("state2"));
        this.postalCode = a.get(nm.getName("postal_code"));
        this.country = a.get(nm.getName("country"));
        this.phone = a.get(nm.getName("phone"));
        this.email = a.get(nm.getName("email"));
        this.type = a.get(nm.getName("type"));
        this.exemptionCode = a.get(nm.getName("exemption_code"));
        save();
        this.f192pi = GenPaymentInstrument.getPaymentInstrument(this.type, this, a, nm);
    }

    public BillingInfoObject(Collection values) throws Exception {
        this.exemptionCode = null;
        this.exemptionApproved = null;
        this.exemptionRejected = null;
        Iterator i = values.iterator();
        this.f191id = Resource.getNewId();
        this.name = (String) i.next();
        this.lastName = (String) i.next();
        this.company = (String) i.next();
        this.address1 = (String) i.next();
        this.address2 = (String) i.next();
        this.city = (String) i.next();
        this.state = (String) i.next();
        this.state2 = (String) i.next();
        this.postalCode = (String) i.next();
        this.country = (String) i.next();
        this.phone = (String) i.next();
        this.email = (String) i.next();
        this.type = (String) i.next();
        if (i.hasNext()) {
            this.exemptionCode = (String) i.next();
        }
        save();
        this.f192pi = GenPaymentInstrument.getPaymentInstrument(this.type, this, i);
    }

    protected void save() throws SQLException {
        long userId = Session.getUser().getId();
        save(userId);
    }

    protected void save(long userId) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO billing_info (id, address1, address2, city, state, state2, postal_code, country, phone, name, last_name, company, email, type, exemption_code, ec_approved, ec_rejected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps2.setLong(1, this.f191id);
            ps2.setString(2, this.address1);
            ps2.setString(3, this.address2);
            ps2.setString(4, this.city);
            ps2.setString(5, this.state);
            ps2.setString(6, this.state2);
            ps2.setString(7, this.postalCode);
            ps2.setString(8, this.country);
            ps2.setString(9, this.phone);
            ps2.setString(10, this.name);
            ps2.setString(11, this.lastName);
            ps2.setString(12, this.company);
            ps2.setString(13, this.email);
            ps2.setString(14, this.type);
            ps2.setString(15, this.exemptionCode);
            ps2.setTimestamp(16, this.exemptionApproved);
            ps2.setTimestamp(17, this.exemptionRejected);
            ps2.executeUpdate();
            ps2.close();
            this.user_id = userId;
            ps = con.prepareStatement("INSERT INTO user_billing_infos (user_id, billing_info_id) VALUES(?, ?)");
            ps.setLong(1, this.user_id);
            ps.setLong(2, this.f191id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public BillingInfoObject(long id) throws Exception {
        this.exemptionCode = null;
        this.exemptionApproved = null;
        this.exemptionRejected = null;
        this.f191id = id;
        if (id <= 0) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT name, last_name, company, address1, address2, city, state, state2, postal_code, country, phone, email, type, stop_date, reason, exemption_code, ec_approved, ec_rejected FROM billing_info WHERE id = ?");
            ps2.setLong(1, id);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.lastName = rs.getString(2);
                this.company = rs.getString(3);
                this.address1 = rs.getString(4);
                this.address2 = rs.getString(5);
                this.city = rs.getString(6);
                this.state = rs.getString(7);
                this.state2 = rs.getString(8);
                this.postalCode = rs.getString(9);
                this.country = rs.getString(10);
                this.phone = rs.getString(11);
                this.email = rs.getString(12);
                this.type = rs.getString(13);
                this.stopDate = rs.getDate(14);
                this.reason = rs.getString(15);
                this.f192pi = GenPaymentInstrument.getPaymentInstrument(this.type, this);
                this.exemptionCode = rs.getString(16);
                this.exemptionApproved = rs.getTimestamp(17);
                this.exemptionRejected = rs.getTimestamp(18);
                ps2.close();
                ps = con.prepareStatement("SELECT user_id FROM user_billing_infos WHERE billing_info_id = ?");
                ps.setLong(1, id);
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    this.user_id = rs2.getLong(1);
                } else {
                    Session.getLog().debug("Can`t find relation user to billing info #" + id);
                }
                return;
            }
            throw new Exception("Not Found Billing Info #" + id);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void updateBI(String name1, String lastName1, String company1, String address11, String address21, String city1, String state1, String postalCode1, String country1, String phone1, String email1) throws Exception {
        updateBI(name1, lastName1, company1, address11, address21, city1, state1, "", postalCode1, country1, phone1, email1);
    }

    public void updateBI(String name1, String lastName1, String company1, String address11, String address21, String city1, String state1, String state21, String postalCode1, String country1, String phone1, String email1) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE billing_info SET address1=?, address2=?, city=?, state=?, state2=?, postal_code=?, country=?, phone=?, name=?, last_name=?, company=?, email=? WHERE id = ?");
            ps.setString(1, address11);
            ps.setString(2, address21);
            ps.setString(3, city1);
            ps.setString(4, state1);
            ps.setString(5, state21);
            ps.setString(6, postalCode1);
            ps.setString(7, country1);
            ps.setString(8, phone1);
            ps.setString(9, name1);
            ps.setString(10, lastName1);
            ps.setString(11, company1);
            ps.setString(12, email1);
            ps.setLong(13, this.f191id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateExemptionCode(String exemptionCode) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if ("".equals(exemptionCode)) {
            exemptionCode = null;
        }
        try {
            ps = con.prepareStatement("UPDATE billing_info SET exemption_code=?, ec_approved=null, ec_rejected=null WHERE id = ?");
            ps.setString(1, exemptionCode);
            ps.setLong(2, this.f191id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.exemptionCode = exemptionCode;
            this.exemptionApproved = null;
            this.exemptionRejected = null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void approveTaxExemption(String code) throws Exception {
        if (taxExemptionApproved()) {
            throw new HSUserException("bi.tax_exemption_already_approved");
        }
        if (this.exemptionCode != null && this.exemptionCode.equals(code)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            Timestamp approvalDate = TimeUtils.getSQLTimestamp();
            try {
                ps = con.prepareStatement("UPDATE billing_info SET ec_approved=?, ec_rejected=null WHERE id = ?");
                ps.setTimestamp(1, approvalDate);
                ps.setLong(2, this.f191id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                this.exemptionApproved = approvalDate;
                this.exemptionRejected = null;
                return;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        throw new HSUserException("bi.tax_exemption_incorrect_code");
    }

    public void rejectTaxExemption(String code) throws Exception {
        if (taxExemptionRejected()) {
            throw new HSUserException("bi.tax_exemption_already_rejected");
        }
        if (this.exemptionCode != null && this.exemptionCode.equals(code)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            Timestamp rejectionDate = TimeUtils.getSQLTimestamp();
            try {
                ps = con.prepareStatement("UPDATE billing_info SET ec_rejected=? WHERE id = ?");
                ps.setTimestamp(1, rejectionDate);
                ps.setLong(2, this.f191id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                this.exemptionRejected = rejectionDate;
                return;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        throw new HSUserException("bi.tax_exemption_incorrect_code");
    }

    public TemplateModel FM_setNegativeDate(String negative_date) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE billing_info SET negative_date = ? WHERE id = ?");
            Date d = new Date(HsphereToolbox.parseShortDate(negative_date).getTime());
            ps.setDate(1, d);
            ps.setLong(2, this.f191id);
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

    public Date getNegativeDate() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Date result = null;
        try {
            ps = con.prepareStatement("SELECT negative_date FROM billing_info WHERE id = ?");
            ps.setLong(1, this.f191id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getDate(1);
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean taxExemptionApproved() {
        return (this.exemptionCode == null || this.exemptionApproved == null || this.exemptionRejected != null) ? false : true;
    }

    public boolean taxExemptionRejected() {
        return (this.exemptionCode == null || this.exemptionRejected == null) ? false : true;
    }

    public TemplateModel FM_taxExemptionApproved() {
        if (taxExemptionApproved()) {
            return new TemplateString(this.exemptionApproved);
        }
        return null;
    }

    public TemplateModel FM_taxExemptionRejected() {
        if (taxExemptionApproved()) {
            return null;
        }
        return new TemplateString(this.exemptionRejected);
    }

    public String getName() {
        return this.name;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getAddress1() {
        return this.address1;
    }

    public String getAddress2() {
        return this.address2;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCompany() {
        return this.company;
    }

    public String getPostalCode() {
        return this.postalCode;
    }
}
