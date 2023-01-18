package psoft.hsphere.resource;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.HSUserException;
import psoft.hsphere.InitToken;
import psoft.hsphere.Invoice;
import psoft.hsphere.InvoiceEntry;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.ThirdPartyResource;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.async.AsyncResource;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.util.PhoneNumberConverter;
import psoft.util.PhoneNumberConvertionException;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/OpenSRS.class */
public abstract class OpenSRS extends Resource implements ThirdPartyResource, AsyncResource {
    protected static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy");
    protected int period;
    protected String login;
    protected String password;
    protected String registrar;
    protected Date lastPayed;
    protected String domainName;
    protected int renew;
    protected int totalPeriod;
    protected int isExpired;
    protected int transferState;

    public abstract boolean isTransfer();

    protected abstract void finishThirdPartyAction(Registrar registrar, String str, String str2, String str3, String str4, Map map, Map map2, ContactInfo contactInfo, List list) throws Exception;

    public static boolean isTransfer(InitToken token) {
        return false;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public boolean isAsyncAutoRemove() {
        return false;
    }

    @Override // psoft.hsphere.async.AsyncResource
    public void asyncDelete() throws Exception {
        setupRefund(TimeUtils.getDate());
        try {
            getParent().findChild("contact_info").get().delete(false);
        } catch (Exception e) {
            Session.getLog().warn(e);
        }
        try {
            getParent().findChild("billing_info").get().delete(false);
        } catch (Exception e2) {
            Session.getLog().warn(e2);
        }
        delete(false);
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() {
        return new Object[]{this.domainName, new Integer(this.totalPeriod), new Integer(this.renew)};
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.totalPeriod;
    }

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        String periodS = (String) i.next();
        int period = 0;
        if (periodS == null || periodS.length() == 0) {
            period = 1;
        } else {
            try {
                period = (int) USFormat.parseDouble(periodS);
            } catch (Exception e) {
                Session.getLog().warn("Problem with parsing double", e);
            }
        }
        return period;
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        return getFreeMultiplier();
    }

    public static double getSetupMultiplier(InitToken token) throws Exception {
        double amount = 0.0d;
        if (token.getCurrentAmount() > token.getFreeUnits()) {
            amount = token.getCurrentAmount() - token.getFreeUnits();
        }
        return Math.min(amount, token.getAmount());
    }

    public OpenSRS(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        String periodS = (String) i.next();
        this.login = (String) i.next();
        this.password = (String) i.next();
        this.domainName = (String) i.next();
        this.registrar = getRegistrar().getSignature();
        if (isTransfer()) {
            this.period = 1;
        } else if (periodS == null || periodS.length() == 0) {
            this.period = 1;
        } else {
            this.period = Integer.parseInt(periodS);
        }
        this.totalPeriod = this.period;
        String renew_mode = getRegistrar().get("renew_mode");
        if (renew_mode != null && renew_mode.equals("manual")) {
            this.renew = 0;
        } else {
            this.renew = this.period;
        }
        if (this.renew == -1) {
            this.isExpired = 1;
        } else {
            this.isExpired = 0;
        }
    }

    public OpenSRS(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT period, login, password, last_payed, registrar,renew,total_period, transfer FROM opensrs WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.period = rs.getInt(1);
                this.login = rs.getString(2);
                this.password = rs.getString(3);
                this.lastPayed = rs.getDate(4);
                this.registrar = rs.getString(5);
                this.renew = rs.getInt("renew");
                this.totalPeriod = rs.getInt("total_period");
                this.domainName = getParent().get("real_name").toString();
                this.transferState = rs.getInt("transfer");
                if (this.renew == -1) {
                    this.isExpired = 1;
                } else {
                    this.isExpired = 0;
                }
            } else {
                notFound();
            }
            this.domainName = _getName();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (Session.getAccount().getPlan().isDemoPlan()) {
            throw new HSUserException("resource.opensrs.domain_unable_register_osrs_demo");
        }
        if (this.login == null || "".equals(this.login)) {
            this.login = recursiveGet("login").toString();
            this.password = recursiveGet("password").toString();
        }
        this.login = normalizeLogin(this.login);
        this.lastPayed = TimeUtils.getDate();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO opensrs (id, period, login, password, last_payed, registrar, renew, total_period, transfer) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.period);
            ps.setString(3, this.login);
            ps.setString(4, this.password);
            ps.setDate(5, new java.sql.Date(this.lastPayed.getTime()));
            ps.setString(6, this.registrar);
            ps.setInt(7, this.renew);
            ps.setInt(8, this.totalPeriod);
            ps.setInt(9, this.transferState);
            ps.executeUpdate();
            if (this.renew == -1) {
                this.isExpired = 1;
            } else {
                this.isExpired = 0;
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected String normalizeLogin(String str) {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                res.append(c);
            }
        }
        return res.toString();
    }

    @Override // psoft.hsphere.ThirdPartyResource
    public void thirdPartyAction() throws Exception {
        try {
            ContactInfo ci = getContactInfo();
            ResourceId zone = recursiveGet("dns_zone");
            List dns = new ArrayList();
            dns.add(zone.get("master").getName());
            HostEntry host = zone.get("slave1");
            if (host != null) {
                dns.add(host.getName());
            }
            HostEntry host2 = zone.get("slave2");
            if (host2 != null) {
                dns.add(host2.getName());
            }
            Map cInfo = correctInfoMap(ci.getMap());
            Map rInfo = correctInfoMap(getRegistrarBillingInfo());
            Registrar r = getRegistrar();
            String tld = DomainRegistrar.getTLD(this.domainName);
            String name = DomainRegistrar.getName(this.domainName);
            finishThirdPartyAction(r, name, tld, this.login, this.password, cInfo, rInfo, ci, dns);
        } catch (RegistrarException re) {
            Session.getLog().error("Domain registration problem", re);
            throw new HSUserException(re);
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("period")) {
            return new TemplateString(this.period);
        }
        if (key.equals("total_period")) {
            return new TemplateString(this.totalPeriod);
        }
        if (key.equals("login")) {
            return new TemplateString(this.login);
        }
        if (key.equals("password")) {
            return new TemplateString(this.password);
        }
        if (key.equals("registrar")) {
            return new TemplateString(this.registrar);
        }
        if (key.equals("domain_name")) {
            return new TemplateString(_getName());
        }
        if (key.equals("renew")) {
            return new TemplateString(getRenew());
        }
        if (key.equals("isAutoRenew")) {
            return new TemplateString((getRenew() == 0 || getRenew() == -1) ? false : true);
        } else if (key.equals("isExpired")) {
            return new TemplateString(getExpired() != 0);
        } else if (key.equals("AutoRenewDate")) {
            return new TemplateString(sdf.format(getRenewalDate()));
        } else {
            if (key.equals("last_paid")) {
                return new TemplateString(sdf.format(this.lastPayed));
            }
            if (key.equals("expire_date")) {
                return new TemplateString(sdf.format(getDomainExpirationDate()));
            }
            if (key.equals("adv_renew_date")) {
                return new TemplateString(sdf.format(getAdvDomainRenewDate()));
            }
            if (key.equals("tld")) {
                return new TemplateString(DomainRegistrar.getTLD(this.domainName));
            }
            return super.get(key);
        }
    }

    public ContactInfo getContactInfo() throws Exception {
        return (ContactInfo) getParent().FM_getChild("contact_info").get();
    }

    public int getExpired() {
        return this.isExpired;
    }

    public TemplateModel FM_update(String type, String affect) throws Exception {
        String domainName = _getName();
        checkRegistrar();
        ContactInfo ci = getContactInfo();
        Map cInfo = correctInfoMap(ci.getMap());
        Map rInfo = correctInfoMap(getRegistrarBillingInfo());
        try {
            getRegistrar().changeContacts(DomainRegistrar.getName(domainName), DomainRegistrar.getTLD(domainName), this.login, this.password, cInfo, rInfo, cInfo, rInfo);
        } catch (Exception ex) {
            if (ex instanceof RegistrarException) {
                throw new HSUserException((RegistrarException) ex);
            }
        }
        return this;
    }

    public void checkRegistrar() throws Exception {
        String current = getRegistrar().getSignature();
        if (!this.registrar.equals(current)) {
            throw new HSUserException("resource.opensrs.oldregistrar", new Object[]{this.registrar, current});
        }
    }

    public TemplateModel FM_changePassword(String newPassword) throws Exception {
        checkRegistrar();
        String domainName = _getName();
        getRegistrar().setPassword(DomainRegistrar.getName(domainName), DomainRegistrar.getTLD(domainName), this.login, this.password, newPassword);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE opensrs SET password = ? WHERE id = ?");
            ps.setString(1, newPassword);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.password = newPassword;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Date getLastPayed() {
        return this.lastPayed;
    }

    public int getPeriod() {
        return this.period;
    }

    public String getDomainName() {
        return this.domainName;
    }

    public Date getNextPaymentDate() {
        Calendar next = TimeUtils.getCalendar(this.lastPayed);
        next.add(1, this.period);
        return next.getTime();
    }

    public void charge(boolean force, int period) throws Exception {
        if (this.isExpired == 1) {
            return;
        }
        Date now = TimeUtils.getDate();
        Date nextPayment = getNextPaymentDate();
        try {
            int renewDaysInAdvanceBeforeExpire = Integer.parseInt(getRegistrar().get("renew_days"));
            Calendar cal = TimeUtils.getCalendar(nextPayment);
            cal.setTime(nextPayment);
            cal.add(5, -renewDaysInAdvanceBeforeExpire);
            nextPayment = cal.getTime();
        } catch (Exception ex) {
            Session.getLog().warn("Failed to get renew_days ", ex);
        }
        if (!now.before(nextPayment) || force) {
            if (period == 0 && !force) {
                expire();
                return;
            }
            boolean skipCharge = Session.getAccount().getBillingInfo().getBillingType() == 2 || Session.getAccount().getBillingInfo().getBillingType() == 3;
            BillEntry be = setupCharge(now);
            Bill bill = Session.getAccount().getBill();
            if (!skipCharge) {
                BillingInfoObject bi = Session.getAccount().getBillingInfo();
                Session.getLog().info("Authorizing");
                HashMap authRes = bill.auth(bi);
                try {
                    renew(period);
                    Session.getLog().info("Post auth charging");
                    try {
                        bill.capture(bi, authRes);
                    } catch (Exception e) {
                        Ticket.create(e, this, "PostAuth(CC Capture) error after successful third party resource(like OpenSRS) registration");
                        Session.getLog().error("PostAuth error " + e);
                        throw e;
                    }
                } catch (Exception ex2) {
                    Session.getLog().error("Error during domain renew ", ex2);
                    if (authRes != null) {
                        Session.getLog().info("Voiding auth");
                        try {
                            bill.void_auth(bi, authRes);
                        } catch (Exception ex1) {
                            Session.getLog().error("Error during void ", ex1);
                        }
                    }
                    bill.cancel(be.getId());
                    throw ex2;
                }
            } else {
                try {
                    renew(period);
                } catch (Exception ex3) {
                    bill.cancel(be.getId());
                    throw ex3;
                }
            }
            setLastPayed(nextPayment);
        }
    }

    public void expire() throws Exception {
        Date now = TimeUtils.getDate();
        Date lifeEnd = getDomainExpirationDate();
        if (now.after(lifeEnd)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE opensrs SET renew = ? WHERE id = ?");
                ps.setInt(1, -1);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                this.isExpired = 1;
                this.renew = -1;
                sendWarning("REGISTRAR_EXPIRED_WARN");
                return;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        sendWarning("REGISTRAR_RENEW_WARN");
    }

    protected void sendWarning(String msgId) throws Exception {
        try {
            Account a = Session.getAccount();
            String email = a.getContactInfo().getEmail();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
            root.put("osrs", this);
            try {
                CustomEmailMessage.send(msgId, email, (TemplateModelRoot) root);
            } catch (Throwable se) {
                Session.getLog().warn("Error sending message", se);
            }
        } catch (NullPointerException npe) {
            Session.getLog().warn("NPE", npe);
        } catch (Throwable t) {
            Session.getLog().warn("Error", t);
        }
    }

    public void charge() throws Exception {
        charge(false, getRenew());
    }

    public void setLastPayed(Date now) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE opensrs SET last_payed = ?, period = ?, total_period = ? WHERE id = ?");
            ps.setDate(1, new java.sql.Date(now.getTime()));
            ps.setInt(2, this.period);
            ps.setInt(3, this.totalPeriod);
            ps.setLong(4, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.lastPayed = now;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String _getName() {
        try {
            return getParent().get("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String getSetupChargeDescription(Date now, String label) throws Exception {
        Date renewalDate = getRenewalDate();
        if (renewalDate != null) {
            return Localizer.translateMessage("bill.opensrs.setup_withrenew", new Object[]{_getName(), Integer.toString(this.period), this.registrar, renewalDate.toString()});
        }
        return Localizer.translateMessage(label, new Object[]{_getName(), Integer.toString(this.period), this.registrar});
    }

    public Registrar getRegistrar() throws Exception {
        String tld = DomainRegistrar.getTLD(this.domainName);
        Registrar reg = (Registrar) DomainRegistrar.getManager().getEntity(tld);
        if (reg == null) {
            throw new HSUserException("resource.opensrs.noregistrar", new Object[]{tld});
        }
        return reg;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM opensrs WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int getRenew() {
        return this.renew;
    }

    public Date getRenewalDate() {
        Session.getLog().debug("DomainName = " + this.domainName);
        if (getRenew() != 0 && getLastPayed() != null) {
            try {
                Calendar cal = TimeUtils.getCalendar(getLastPayed());
                cal.add(1, this.period);
                cal.add(5, -Integer.parseInt(getRegistrar().get("renew_days")));
                cal.set(14, 0);
                return cal.getTime();
            } catch (Exception ex) {
                Session.getLog().warn("Error getting autorenewal date ", ex);
                return null;
            }
        }
        return null;
    }

    public Date getExpaireNoticeDate() {
        Session.getLog().debug("DomainName = " + this.domainName);
        if (getRenew() != 0) {
            try {
                Calendar cal = TimeUtils.getCalendar(getLastPayed());
                cal.add(1, this.period);
                cal.add(5, -Integer.parseInt(getRegistrar().get("email_days")));
                cal.set(14, 0);
                return cal.getTime();
            } catch (Exception ex) {
                Session.getLog().warn("Error getting autorenewal date ", ex);
                return null;
            }
        }
        return null;
    }

    public Date getDomainExpirationDate() {
        return getNextPaymentDate();
    }

    public Date getAdvDomainRenewDate() {
        try {
            int renewDaysInAdvanceBeforeExpire = Integer.parseInt(getRegistrar().get("renew_days"));
            try {
                Calendar cal = TimeUtils.getCalendar(getDomainExpirationDate());
                cal.add(5, -renewDaysInAdvanceBeforeExpire);
                cal.set(14, 0);
                return cal.getTime();
            } catch (Exception ex) {
                Session.getLog().warn("Error getting AdvDomainRenew date ", ex);
                return null;
            }
        } catch (Exception ex2) {
            Session.getLog().warn("Failed to get renew_days ", ex2);
            return null;
        }
    }

    public void setAutoRenew(int years) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE opensrs SET renew = ? WHERE id = ?");
            ps.setInt(1, years);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.renew = years;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setAutoRenew(int years) throws Exception {
        DomainRegistrar.checkRegistarPeriod(years, this.domainName, getAccount().getPlan());
        setAutoRenew(years);
        return this;
    }

    public void renew(int renewPeriod) throws Exception {
        try {
            DomainRegistrar.checkRegistarPeriod(renewPeriod, this.domainName, getAccount().getPlan());
            ContactInfo ci = getContactInfo();
            Calendar cal = TimeUtils.getCalendar(getDomainExpirationDate());
            cal.setTime(getDomainExpirationDate());
            String currentExpYear = Integer.toString(cal.get(1));
            getRegistrar().renew(DomainRegistrar.getName(this.domainName), DomainRegistrar.getTLD(this.domainName), currentExpYear, renewPeriod, correctInfoMap(ci.getMap()));
            int oldPeriod = this.totalPeriod;
            this.period = renewPeriod;
            this.totalPeriod += renewPeriod;
            boolean wasTrans = Session.isTransConnection();
            Connection con = wasTrans ? Session.getDb() : Session.getTransConnection();
            try {
                setNewAmountResource(oldPeriod);
                if (!wasTrans) {
                    Session.commitTransConnection(con);
                } else {
                    con.close();
                }
            } catch (Exception ex) {
                if (!wasTrans) {
                    con.rollback();
                }
                throw ex;
            }
        } catch (Exception ex2) {
            expire();
            if (ex2 instanceof RegistrarException) {
                throw new HSUserException((RegistrarException) ex2);
            }
            throw ex2;
        }
    }

    public TemplateModel FM_manualDomainRenew(int renewPeriod) throws Exception {
        charge(true, renewPeriod);
        return this;
    }

    private Map getRegistrarBillingInfo() throws UnknownResellerException {
        HashMap info = new HashMap();
        info.put("address1", Settings.get().getMap().get("address"));
        info.put("address2", Settings.get().getMap().get("address2"));
        info.put("city", Settings.get().getMap().get("city"));
        info.put("country", Settings.get().getMap().get("country"));
        info.put("state", Settings.get().getMap().get("state"));
        info.put("email", Settings.get().getMap().get("email"));
        info.put("first_name", Settings.get().getMap().get("ofname"));
        info.put("last_name", Settings.get().getMap().get("olname"));
        info.put("org_name", Settings.get().getMap().get("name"));
        info.put("phone", Settings.get().getMap().get("phone"));
        info.put("fax", Settings.get().getMap().get("fax"));
        info.put("postal_code", Settings.get().getMap().get("zip"));
        return info;
    }

    protected Map correctInfoMap(Map map) {
        String tld = DomainRegistrar.getTLD(this.domainName).toUpperCase();
        String phone = (String) map.get("phone");
        String fax = (String) map.get("fax");
        if ("CA".equals(tld)) {
            String country = (String) map.get("country");
            if (phone != null && !"".equals(phone)) {
                try {
                    map.put("phone", PhoneNumberConverter.getCountryFormated(phone, country));
                } catch (PhoneNumberConvertionException e) {
                    Session.getLog().debug("Phone number convertion", e);
                }
            }
            if (fax != null && !"".equals(fax)) {
                try {
                    map.put("fax", PhoneNumberConverter.getCountryFormated(fax, country));
                } catch (PhoneNumberConvertionException e2) {
                    Session.getLog().debug("Fax number convertion", e2);
                }
            }
        } else {
            if (phone != null && !"".equals(phone)) {
                try {
                    map.put("phone", PhoneNumberConverter.getITUFormated(phone));
                } catch (PhoneNumberConvertionException e3) {
                    Session.getLog().debug("Converting a phone number into ITU format", e3);
                }
            }
            if (fax != null && !"".equals(fax)) {
                try {
                    map.put("fax", PhoneNumberConverter.getITUFormated(fax));
                } catch (PhoneNumberConvertionException e4) {
                    Session.getLog().debug("Converting a fax number into ITU format", e4);
                }
            }
        }
        return map;
    }

    public TemplateModel FM_estimateRenew(int newPeriod) throws Exception {
        Date now = TimeUtils.getDate();
        getAccount().charge();
        getAccount().sendInvoice(now);
        List entries = new ArrayList();
        double total = 0.0d;
        InitToken token = new InitToken(Session.getAccount());
        token.getTypeCounter().inc(getId().getType(), newPeriod);
        Collection values = new ArrayList();
        values.add(String.valueOf(newPeriod));
        values.add(this.login);
        values.add(this.password);
        values.add(this.domainName);
        token.set(getId().getType(), this, values);
        double setup = calc(token, 1);
        String setupDescription = token.getSetupChargeDescription(now);
        if (setup != 0.0d) {
            entries.add(new InvoiceEntry(1, setupDescription, setup));
            total = setup;
        }
        Invoice invoice = new Invoice(entries, total);
        return invoice;
    }

    public boolean checkRenewPeriod() throws Exception {
        Plan p = getAccount().getPlan();
        ResourceType rt = p.getResourceType(TypeRegistry.getTypeId("opensrs"));
        DomainRegistrar.getActivePricedTLDs(rt);
        return false;
    }
}
