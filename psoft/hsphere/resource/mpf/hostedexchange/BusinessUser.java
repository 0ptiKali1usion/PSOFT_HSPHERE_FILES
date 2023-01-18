package psoft.hsphere.resource.mpf.hostedexchange;

import com.microsoft.provisioning.webservices.ModifyUserPropertyXml;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.StringHolder;
import org.apache.axis.message.MessageElement;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.hsphere.resource.mpf.hostedexchange.BusinessOrganization;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/BusinessUser.class */
public class BusinessUser extends Resource {
    private static final Category log = Category.getInstance(BusinessUser.class.getName());
    String principalName;
    String descr;
    String password;
    String sAMAccountName;
    boolean isAdmin;
    int outlimit;
    int inlimit;
    String displayName;
    String givenName;
    String middleName;

    /* renamed from: sn */
    String f204sn;
    String initials;
    String physicalDeliveryOfficeName;
    String telephoneNumber;
    String street;
    String postOfficeBox;
    String city;
    String state;
    String country;
    String mailBoxPlanType;
    boolean storeAndForward;
    List proxy;
    ResourceId altRecipient;
    protected Map usageReport;
    private ResourceId smtpDomainId;
    public static final long TIME_TO_LIVE = 300000;

    private boolean getNextBoolean(Iterator i) {
        if (i.hasNext()) {
            return "true".equals(i.next());
        }
        return false;
    }

    private String getNextStr(Iterator i) {
        if (i.hasNext()) {
            return (String) i.next();
        }
        return null;
    }

    private int getNextInt(Iterator i) {
        return Integer.parseInt((String) i.next());
    }

    public BusinessUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.usageReport = new HashMap();
        Iterator i = initValues.iterator();
        this.principalName = (String) i.next();
        this.password = (String) i.next();
        this.descr = getNextStr(i);
        this.isAdmin = getNextBoolean(i);
        this.mailBoxPlanType = getNextStr(i);
        this.altRecipient = null;
        this.displayName = getNextStr(i);
        this.givenName = getNextStr(i);
        this.middleName = getNextStr(i);
        this.f204sn = getNextStr(i);
        this.initials = getNextStr(i);
        this.physicalDeliveryOfficeName = getNextStr(i);
        this.telephoneNumber = getNextStr(i);
        this.street = getNextStr(i);
        this.postOfficeBox = getNextStr(i);
        this.city = getNextStr(i);
        this.state = getNextStr(i);
        this.country = getNextStr(i);
    }

    public BusinessUser(ResourceId id) throws Exception {
        super(id);
        this.usageReport = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT principal_name, password, description, is_admin, plan_type, sAMAccountName, store_forward,  altRecipient, display_name, given_name, middle_name, sn, initials, smtp_domain, physicalDeliveryOfficeName, telephoneNumber, street,postOfficeBox, city, state, country FROM he_bizuser WHERE id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.principalName = rs.getString(1);
                this.password = rs.getString(2);
                this.descr = rs.getString(3);
                this.isAdmin = rs.getBoolean(4);
                this.mailBoxPlanType = rs.getString(5);
                this.sAMAccountName = rs.getString(6);
                this.storeAndForward = rs.getBoolean(7);
                long contactId = rs.getLong(8);
                if (contactId > 0) {
                    int type = TypeRegistry.getIntTypeId("exchange_contact");
                    this.altRecipient = new ResourceId(contactId, type);
                }
                this.displayName = rs.getString(9);
                this.givenName = rs.getString(10);
                this.middleName = rs.getString(11);
                this.f204sn = rs.getString(12);
                this.initials = rs.getString(13);
                long domainId = rs.getLong(14);
                this.physicalDeliveryOfficeName = rs.getString(15);
                this.telephoneNumber = rs.getString(16);
                this.street = rs.getString(17);
                this.postOfficeBox = rs.getString(18);
                this.city = rs.getString(19);
                this.state = rs.getString(20);
                this.country = rs.getString(21);
                int type2 = TypeRegistry.getIntTypeId("domain");
                this.smtpDomainId = new ResourceId(domainId, type2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        SMTPDomain smtpDomain = bo.getDomain();
        this.smtpDomainId = smtpDomain.getId();
        try {
            ps = con.prepareStatement("INSERT INTO he_bizuser (id, principal_name, password, description, is_admin, plan_type, sAMAccountName, display_name, given_name, middle_name, sn, initials, smtp_domain, physicalDeliveryOfficeName, telephoneNumber, street,postOfficeBox, city, state, country) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.principalName);
            ps.setString(3, this.password);
            ps.setString(4, this.descr);
            ps.setBoolean(5, this.isAdmin);
            ps.setString(6, this.mailBoxPlanType);
            this.sAMAccountName = getSAMAccountName();
            ps.setString(7, this.sAMAccountName);
            ps.setString(8, this.displayName);
            ps.setString(9, this.givenName);
            ps.setString(10, this.middleName);
            ps.setString(11, this.f204sn);
            ps.setString(12, this.initials);
            ps.setLong(13, this.smtpDomainId.getId());
            ps.setString(14, this.physicalDeliveryOfficeName);
            ps.setString(15, this.telephoneNumber);
            ps.setString(16, this.street);
            ps.setString(17, this.postOfficeBox);
            ps.setString(18, this.city);
            ps.setString(19, this.state);
            ps.setString(20, this.country);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            MPFManager manager = MPFManager.getManager();
            MessageElement properties = new MessageElement("", "properties");
            if (this.physicalDeliveryOfficeName != null && !"".equals(this.physicalDeliveryOfficeName)) {
                MessageElement property = new MessageElement("", "property");
                property.addAttribute("", "name", "physicalDeliveryOfficeName");
                property.addTextNode(this.physicalDeliveryOfficeName);
                properties.addChild(property);
            }
            if (this.telephoneNumber != null && !"".equals(this.telephoneNumber)) {
                MessageElement property2 = new MessageElement("", "property");
                property2.addAttribute("", "name", "telephoneNumber");
                property2.addTextNode(this.telephoneNumber);
                properties.addChild(property2);
            }
            if (this.street != null && !"".equals(this.street)) {
                MessageElement property3 = new MessageElement("", "property");
                property3.addAttribute("", "name", "street");
                property3.addTextNode(this.street);
                properties.addChild(property3);
            }
            if (this.postOfficeBox != null && !"".equals(this.postOfficeBox)) {
                MessageElement property4 = new MessageElement("", "property");
                property4.addAttribute("", "name", "postOfficeBox");
                property4.addTextNode(this.postOfficeBox);
                properties.addChild(property4);
            }
            if (this.city != null && !"".equals(this.city)) {
                MessageElement property5 = new MessageElement("", "property");
                property5.addAttribute("", "name", "l");
                property5.addTextNode(this.city);
                properties.addChild(property5);
            }
            if (this.state != null && !"".equals(this.state)) {
                MessageElement property6 = new MessageElement("", "property");
                property6.addAttribute("", "name", "st");
                property6.addTextNode(this.state);
                properties.addChild(property6);
            }
            if (this.country != null && !"".equals(this.country)) {
                MessageElement property7 = new MessageElement("", "property");
                property7.addAttribute("", "name", "c");
                property7.addTextNode(this.country);
                properties.addChild(property7);
            }
            HePlanSettings plan = (HePlanSettings) MPFManager.getManager().getAvailablePlans().get(this.mailBoxPlanType);
            Session.getLog().debug("type: " + this.mailBoxPlanType + " Plan: " + plan);
            String planName = plan.get("name").toString();
            String result = manager.getHES().createBusinessUser(manager.getLDAP(bo.getName()), this.principalName + "@" + smtpDomain.getName(), this.sAMAccountName, this.displayName, this.givenName, this.middleName, this.f204sn, this.initials, this.descr, planName, this.password, this.isAdmin, (String) null, properties.toString(), (String) null, manager.getPdc());
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                if ("0x800708c5".equals(res.getErrorCode())) {
                    String error = Localizer.translateMessage("msexchange.password_restrictions");
                    log.warn(error + " \n " + res.getError());
                    throw new HSUserException(error);
                }
                throw new Exception(res.getError());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private String getSAMAccountName() throws Exception {
        String samAccountName;
        String samAccountName2 = this.principalName;
        String id = Long.toString(getId().getId());
        int nameLen = 19 - id.length();
        int len = samAccountName2.length();
        if (len > nameLen) {
            samAccountName = samAccountName2.substring(0, nameLen) + "_" + id;
        } else {
            String samAccountName3 = samAccountName2 + "_" + id;
            String domainName = getDomain().replace('.', '_');
            int symbolsToBeAdded = 20 - samAccountName3.length();
            if (symbolsToBeAdded < domainName.length()) {
                samAccountName = samAccountName3 + domainName.substring(0, symbolsToBeAdded);
            } else {
                samAccountName = samAccountName3 + domainName;
                for (int i = 0; i < symbolsToBeAdded - domainName.length(); i++) {
                    samAccountName = samAccountName + "_";
                }
            }
        }
        return samAccountName;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        int type = TypeRegistry.getIntTypeId("distribution_list");
        long dlId = 0;
        try {
            ps = con.prepareStatement("SELECT id FROM he_distribution_list_subscr WHERE subscriber_id=?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    dlId = rs.getLong(1);
                    ((DistributionList) new ResourceId(dlId, type).get()).FM_deleteSubscriber(getId());
                } catch (Exception ex) {
                    Session.getLog().error("Unable to remove subscriber " + getId().toString() + " from the distribution list " + dlId + "_" + type, ex);
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (this.initialized) {
                MPFManager manager = MPFManager.getManager();
                String result = manager.getHES().deleteBusinessUser(getLDAP(), manager.getPdc());
                MPFManager.Result res = manager.processHeResult(result);
                if (!res.isSuccess()) {
                    throw new Exception(res.getError());
                }
            }
            try {
                ps = con.prepareStatement("DELETE FROM he_bizuser WHERE id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                DistributionList.deleteSubscriber(this.f41id);
                for (ResourceId rid : getParent().findChildren("distribution_list")) {
                    DistributionList dl = (DistributionList) Resource.softGet(rid);
                    if (dl != null) {
                        dl.resetSubscriberList();
                    }
                }
            } finally {
            }
        } finally {
        }
    }

    private String getLDAP() throws Exception {
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        return manager.getLDAP(bo.getName(), this.principalName + "@" + getDomain());
    }

    public HePlanSettings getPlanSettings() {
        MPFManager manager = MPFManager.getManager();
        HashMap allPlans = manager.getAvailablePlans();
        HePlanSettings plan = (HePlanSettings) allPlans.get(this.mailBoxPlanType);
        return plan;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("principalName".equals(key)) {
            return new TemplateString(this.principalName);
        }
        if ("descr".equals(key)) {
            return new TemplateString(this.descr);
        }
        if ("sAMAccountName".equals(key)) {
            return new TemplateString(this.sAMAccountName);
        }
        if ("plan".equals(key)) {
            return getPlanSettings();
        }
        if ("usage".equals(key)) {
            return new TemplateString(getUsage());
        }
        if ("storeAndForward".equals(key)) {
            return new TemplateString(this.storeAndForward);
        }
        if ("proxy".equals(key)) {
            try {
                return new TemplateList(getProxy());
            } catch (SQLException e) {
                log.warn("Unable to retrieve proxy", e);
            }
        }
        if (!"forward".equals(key)) {
            return "displayName".equals(key) ? new TemplateString(this.displayName) : "givenName".equals(key) ? new TemplateString(this.givenName) : "middleName".equals(key) ? new TemplateString(this.middleName) : "sn".equals(key) ? new TemplateString(this.f204sn) : "initials".equals(key) ? new TemplateString(this.initials) : "domain".equals(key) ? this.smtpDomainId : "physicalDeliveryOfficeName".equals(key) ? new TemplateString(this.physicalDeliveryOfficeName) : "telephoneNumber".equals(key) ? new TemplateString(this.telephoneNumber) : "street".equals(key) ? new TemplateString(this.street) : "postOfficeBox".equals(key) ? new TemplateString(this.postOfficeBox) : "city".equals(key) ? new TemplateString(this.city) : "state".equals(key) ? new TemplateString(this.state) : "country".equals(key) ? new TemplateString(this.country) : "planType".equals(key) ? new TemplateString(this.mailBoxPlanType) : "isAdmin".equals(key) ? new TemplateString(this.isAdmin) : super.get(key);
        }
        try {
            Contact c = (Contact) Resource.get(this.altRecipient);
            return new TemplateString(c.getTargetAddr());
        } catch (Exception e2) {
            log.warn("Unable to retrieve contact smtp address" + e2.getMessage());
            return null;
        }
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        super.suspend();
        MPFManager manager = MPFManager.getManager();
        String result = manager.getHES().disableUser(getLDAP(), manager.getPdc());
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            super.resume();
            MPFManager manager = MPFManager.getManager();
            String result = manager.getHES().enableUser(getLDAP(), manager.getPdc());
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
        }
    }

    /* JADX WARN: Finally extract failed */
    public void FM_setAdmin(String state) throws Exception {
        boolean isAdmin = false;
        if ("on".equals(state)) {
            isAdmin = true;
        }
        MPFManager manager = MPFManager.getManager();
        String result = manager.getHES().makeUserAdmin(getLDAP(), isAdmin, manager.getPdc());
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        this.isAdmin = isAdmin;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET is_admin = ? WHERE id = ?");
            ps.setBoolean(1, isAdmin);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_setParams(String descr, String displayName, String givenName, String middleName, String sn, String initials, String physicalDeliveryOfficeName, String telephoneNumber, String street, String postOfficeBox, String city, String state, String country) throws Exception {
        Session.getLog().debug("FM_setParams initials: " + initials + " description: " + descr);
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        BooleanHolder modifyUserResult = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        MessageElement properties = new MessageElement("", "properties");
        if (physicalDeliveryOfficeName != null && !"".equals(physicalDeliveryOfficeName)) {
            MessageElement property = new MessageElement("", "property");
            property.addAttribute("", "name", "physicalDeliveryOfficeName");
            property.addTextNode(physicalDeliveryOfficeName);
            properties.addChild(property);
        }
        if (telephoneNumber != null && !"".equals(telephoneNumber)) {
            MessageElement property2 = new MessageElement("", "property");
            property2.addAttribute("", "name", "telephoneNumber");
            property2.addTextNode(telephoneNumber);
            properties.addChild(property2);
        }
        if (street != null && !"".equals(street)) {
            MessageElement property3 = new MessageElement("", "property");
            property3.addAttribute("", "name", "street");
            property3.addTextNode(street);
            properties.addChild(property3);
        }
        if (postOfficeBox != null && !"".equals(postOfficeBox)) {
            MessageElement property4 = new MessageElement("", "property");
            property4.addAttribute("", "name", "postOfficeBox");
            property4.addTextNode(postOfficeBox);
            properties.addChild(property4);
        }
        if (city != null && !"".equals(city)) {
            MessageElement property5 = new MessageElement("", "property");
            property5.addAttribute("", "name", "l");
            property5.addTextNode(city);
            properties.addChild(property5);
        }
        if (state != null && !"".equals(state)) {
            MessageElement property6 = new MessageElement("", "property");
            property6.addAttribute("", "name", "st");
            property6.addTextNode(state);
            properties.addChild(property6);
        }
        if (country != null && !"".equals(country)) {
            MessageElement property7 = new MessageElement("", "property");
            property7.addAttribute("", "name", "c");
            property7.addTextNode(country);
            properties.addChild(property7);
        }
        ModifyUserPropertyXml propertyXML = new ModifyUserPropertyXml();
        propertyXML.set_any(new MessageElement[]{properties});
        manager.getMADS().modifyUser(getLDAP(), this.principalName + "@" + getDomain(), this.sAMAccountName, displayName, givenName, middleName, sn, initials, descr, propertyXML, manager.getPdc(), true, modifyUserResult, errorReturn);
        if (modifyUserResult.value) {
            this.displayName = displayName;
            this.givenName = givenName;
            this.middleName = middleName;
            this.f204sn = sn;
            this.initials = initials;
            this.descr = descr;
            this.physicalDeliveryOfficeName = physicalDeliveryOfficeName;
            this.telephoneNumber = telephoneNumber;
            this.street = street;
            this.postOfficeBox = postOfficeBox;
            this.city = city;
            this.state = state;
            this.country = country;
        } else {
            Session.getLog().error("Problem setting params : " + errorReturn.value);
        }
        Session.getLog().debug("FM_setParams initials: " + initials + " description: " + descr);
        Session.getLog().debug("FM_setParams initials: " + this.initials + " description: " + this.descr + " result " + modifyUserResult.value);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET description = ?, display_name = ?, given_name = ?, middle_name = ?, sn = ?, initials = ?, physicalDeliveryOfficeName = ?, telephoneNumber = ?, street = ?, postOfficeBox = ?, city = ?, state = ?, country = ? WHERE id = ?");
            ps.setString(1, this.descr);
            ps.setString(2, this.sAMAccountName);
            ps.setString(3, this.givenName);
            ps.setString(4, this.middleName);
            ps.setString(5, this.f204sn);
            ps.setString(6, this.initials);
            ps.setString(7, this.physicalDeliveryOfficeName);
            ps.setString(8, this.telephoneNumber);
            ps.setString(9, this.street);
            ps.setString(10, this.postOfficeBox);
            ps.setString(11, this.city);
            ps.setString(12, this.state);
            ps.setString(13, this.country);
            ps.setLong(14, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_changePlan(String planType) throws Exception {
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        ArrayList children = new ArrayList(this.f41id.getChildHolder().getChildren());
        Iterator i = children.iterator();
        while (i.hasNext()) {
            ResourceId resid = (ResourceId) i.next();
            Resource res = resid.get();
            if (res instanceof HostedExchangePlan) {
                Session.getLog().debug("Plan: " + resid.getAsString() + " deletion.");
                res.FM_cdelete(0);
            }
        }
        addChild(planType, "", null);
        HePlanSettings planSettings = (HePlanSettings) MPFManager.getManager().getAvailablePlans().get(planType);
        String result = manager.getHES().changeUserMailboxPlan(getLDAP(), planSettings.get("name").toString(), manager.getPdc(), "BusinessUser");
        MPFManager.Result res2 = manager.processHeResult(result);
        if (!res2.isSuccess()) {
            throw new Exception(res2.getError());
        }
        this.mailBoxPlanType = planType;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET plan_type = ? WHERE id = ?");
            ps.setString(1, this.mailBoxPlanType);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_changePassword(String newPassword) throws Exception {
        MPFManager manager = MPFManager.getManager();
        String result = manager.getHES().setUserPassword(getLDAP(), newPassword, manager.getPdc());
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            if ("0x800708c5".equals(res.getErrorCode())) {
                throw new HSUserException(res.getError());
            }
            throw new Exception(res.getError());
        }
        this.password = newPassword;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET password = ? WHERE id = ?");
            ps.setString(1, newPassword);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return new TemplateOKResult();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized List getProxy() throws SQLException {
        if (this.proxy != null) {
            return this.proxy;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM he_bizuser_proxy WHERE id = ?");
            ps.setLong(1, this.f41id.getId());
            ResultSet rs = ps.executeQuery();
            this.proxy = new ArrayList();
            while (rs.next()) {
                this.proxy.add(rs.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            return this.proxy;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_addProxy(String name) throws Exception {
        getProxy();
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        String proxyAddr = name + "@" + getDomain();
        String result = manager.getHES().createSMTPProxyAddress(getLDAP(), manager.getPdc(), proxyAddr, false);
        System.out.println("\nRESULT: " + result);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        this.proxy.add(name);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO he_bizuser_proxy (name, id) VALUES (?, ?)");
            ps.setString(1, name);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_deleteProxy(String name) throws Exception {
        getProxy();
        if (!this.proxy.contains(name)) {
            throw new HSUserException("he_proxy_notinuse", new String[]{name});
        }
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        String proxyAddr = name + "@" + getDomain();
        String result = manager.getHES().deleteSMTPProxyAddress(getLDAP(), manager.getPdc(), proxyAddr);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        this.proxy.remove(name);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_bizuser_proxy WHERE name = ? AND id = ?");
            ps.setString(1, name);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_setStoreAndForward(boolean storeAndForward) throws Exception {
        BooleanHolder result = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        MPFManager manager = MPFManager.getManager();
        MessageElement properties = new MessageElement("", "properties");
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        MessageElement property = new MessageElement("", "property");
        property.addAttribute("", "name", "deliverAndRedirect");
        property.addTextNode(storeAndForward ? "TRUE" : "FALSE");
        properties.addChild(property);
        ModifyUserPropertyXml propertyXML = new ModifyUserPropertyXml();
        propertyXML.set_any(new MessageElement[]{properties});
        manager.getMADS().modifyUser(getLDAP(), this.principalName + "@" + getDomain(), this.sAMAccountName, this.sAMAccountName, this.givenName, this.middleName, this.f204sn, this.initials, this.descr, propertyXML, manager.getPdc(), true, result, errorReturn);
        if (!result.value) {
            throw new Exception(errorReturn.value);
        }
        this.storeAndForward = storeAndForward;
        log.info("STORE_PARAM:" + storeAndForward);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET store_forward = ? WHERE id = ?");
            ps.setBoolean(1, storeAndForward);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    public void FM_addForward(String email) throws Exception {
        Contact contact;
        boolean isContactCreated = false;
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        ResourceId contactId = bo.getContactByEmail(email);
        if (contactId == null) {
            StringTokenizer st = new StringTokenizer(email, "@");
            String alias = st.nextToken();
            contactId = bo.createContact(alias);
            contact = (Contact) contactId.get();
            isContactCreated = true;
            try {
                contact.mailEnable(alias + "@" + getDomain(), email);
            } catch (Exception ex) {
                contact.delete();
                throw ex;
            }
        } else {
            contact = (Contact) contactId.get();
        }
        BooleanHolder result = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        MessageElement properties = new MessageElement("", "properties");
        MessageElement property = new MessageElement("", "property");
        property.addAttribute("", "name", "altRecipient");
        property.addTextNode("CN=" + contact.getAlias() + ", OU=" + bo.getName() + "," + manager.getLdapString());
        properties.addChild(property);
        ModifyUserPropertyXml propertyXML = new ModifyUserPropertyXml();
        propertyXML.set_any(new MessageElement[]{properties});
        try {
            manager.getMADS().modifyUser(getLDAP(), this.principalName + "@" + getDomain(), this.sAMAccountName, this.sAMAccountName, this.givenName, this.middleName, this.f204sn, this.initials, this.descr, propertyXML, manager.getPdc(), true, result, errorReturn);
            if (!result.value) {
                throw new Exception(errorReturn.value);
            }
            this.altRecipient = contactId;
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE he_bizuser SET altRecipient = ? WHERE id = ?");
                ps.setLong(1, contactId.getId());
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } catch (Exception ex2) {
            if (isContactCreated) {
                contact.delete();
            }
            throw ex2;
        }
    }

    public void FM_deleteForward(String name) throws Exception {
        BooleanHolder result = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        MPFManager manager = MPFManager.getManager();
        MessageElement properties = new MessageElement("", "properties");
        MessageElement property = new MessageElement("", "property");
        property.addAttribute("", "name", "altRecipient");
        property.addTextNode("");
        properties.addChild(property);
        ModifyUserPropertyXml propertyXML = new ModifyUserPropertyXml();
        propertyXML.set_any(new MessageElement[]{properties});
        manager.getMADS().modifyUser(getLDAP(), this.principalName + "@" + getDomain(), this.sAMAccountName, this.sAMAccountName, this.givenName, this.middleName, this.f204sn, this.initials, this.descr, propertyXML, manager.getPdc(), true, result, errorReturn);
        if (!result.value) {
            throw new Exception(errorReturn.value);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_bizuser SET altRecipient = null WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            bo.removeContact(this.altRecipient);
            this.altRecipient = null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getName() throws Exception {
        BusinessOrganization businessOrganization = (BusinessOrganization) getParent().get();
        try {
            return this.principalName + "@" + getDomain();
        } catch (Exception ex) {
            Session.getLog().error("Unable to get full pryncypal name for the mailbox " + this.principalName, ex);
            return this.principalName;
        }
    }

    private String getUsage() {
        MPFManager.Result result;
        String usageResult = "";
        Long timeToLive = (Long) this.usageReport.get("time");
        String u = (String) this.usageReport.get("usage");
        if (timeToLive != null && u != null && System.currentTimeMillis() - timeToLive.longValue() < 300000) {
            return u;
        }
        try {
            MPFManager manager = MPFManager.getManager();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            String mailboxName = this.principalName + "@" + getDomain();
            HashMap info = (HashMap) bo.getOrgUsersInfo(false, BusinessOrganization.UsersInfoType.MAILBOXES).get(mailboxName);
            if (info == null) {
                HashMap hashMap = (HashMap) bo.getOrgUsersInfo(true, BusinessOrganization.UsersInfoType.MAILBOXES).get(mailboxName);
            }
            String path = manager.getLDAP(bo.getName());
            MessageElement data = new MessageElement("", "data");
            MessageElement tmp = new MessageElement("", "organization");
            tmp.addTextNode(path);
            data.addChild(tmp);
            MessageElement tmp2 = new MessageElement("", "preferredDomainController");
            tmp2.addTextNode(manager.getPdc());
            data.addChild(tmp2);
            MessageElement tmp3 = new MessageElement("", "mailbox");
            tmp3.addTextNode(getLDAP());
            data.addChild(tmp3);
            result = manager.executeMPSRequest("WS Exchange Provider Adapter", "GetMailboxUsage", data);
        } catch (Exception ex) {
            Session.getLog().warn("Unable to get mail box usage. Eror: " + ex.getMessage());
        }
        if (!result.isSuccess()) {
            throw new Exception(result.getError());
        }
        MessageElement[] xmldata = result.getMessageElements();
        String usage = XPathAPI.selectSingleNode(xmldata[0].getAsDocument(), "/response/data/usage/text()").getNodeValue();
        usageResult = USFormat.format(Integer.parseInt(usage));
        this.usageReport.clear();
        this.usageReport.put("usage", usageResult);
        this.usageReport.put("time", new Long(System.currentTimeMillis()));
        return usageResult;
    }

    private String getMsExchangeOmaWirelessValue(boolean oma, boolean oma_uis, boolean oma_utdn) {
        return Integer.toString((oma ? 0 : 2) + (oma_uis ? 0 : 4) + (oma_utdn ? 0 : 1));
    }

    public String getDomain() throws Exception {
        return ((SMTPDomain) this.smtpDomainId.get()).getName();
    }
}
