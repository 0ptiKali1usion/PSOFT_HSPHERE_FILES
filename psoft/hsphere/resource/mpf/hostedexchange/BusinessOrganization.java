package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.axis.message.MessageElement;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.report.DataComparator;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.hsphere.resource.mpf.Tag;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/BusinessOrganization.class */
public class BusinessOrganization extends Resource {
    protected String name;
    protected String description;
    protected ArrayList availablePlans;

    /* renamed from: dc */
    protected DataComparator f202dc;
    protected Map serverReport;
    protected Map usersReport;
    public static final long TIME_TO_LIVE = 1800000;

    /* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/BusinessOrganization$QuotaValueType.class */
    public enum QuotaValueType {
        MAILSTORE_QUOTA,
        MAILSTORE_FREE,
        PUBLIC_STORE,
        PUBLIC_STORE_FREE
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/BusinessOrganization$UsersInfoType.class */
    public enum UsersInfoType {
        MAILBOXES,
        PUBLICFOLDERS
    }

    public BusinessOrganization(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.f202dc = new DataComparator();
        this.serverReport = new HashMap();
        this.usersReport = new HashMap();
        Iterator i = initValues.iterator();
        this.name = (String) i.next();
        this.description = Session.getAccount().getContactInfo().get("company").getAsString();
        this.availablePlans = getAvailablePlanTypes();
    }

    public BusinessOrganization(ResourceId id) throws Exception {
        super(id);
        this.f202dc = new DataComparator();
        this.serverReport = new HashMap();
        this.usersReport = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            this.availablePlans = new ArrayList();
            ps = con.prepareStatement("SELECT name, description, available_plans FROM he_bizorg WHERE id = ? ORDER BY description");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.description = rs.getString(2);
                String plans = rs.getString(3);
                StringTokenizer st = new StringTokenizer(plans, ",");
                while (st.hasMoreTokens()) {
                    this.availablePlans.add(st.nextToken());
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
    public void initDone() throws Exception {
        super.initDone();
        MPFManager manager = MPFManager.getManager();
        Tag mailStore = new Tag("mailStore");
        mailStore.addChild(new Tag("megabytes", "1"));
        mailStore.addChild(new Tag("shared", "1"));
        Tag publicStore = new Tag("publicStore", new com.psoft.mpf.Tag("megabytes", "1"));
        new Tag("properties", new com.psoft.mpf.Tag("property", "postalAddress", new com.psoft.mpf.Tag("value", "601 Brightwater CT")));
        Tag plans = new Tag("availablePlans");
        String availablePlanTypes = "";
        int i = 0;
        while (i < this.availablePlans.size()) {
            String planType = (String) this.availablePlans.get(i);
            availablePlanTypes = availablePlanTypes + (i > 0 ? "," : "") + planType;
            HePlanSettings plan = (HePlanSettings) manager.getAvailablePlans().get(planType);
            plans.addChild(new com.psoft.mpf.Tag("planName", plan.get("name").toString()));
            i++;
        }
        String result = manager.getHES().createBusinessOrganization(manager.getContainer(), this.name, mailStore.toXML(), publicStore.toXML(), getDomain().getName(), this.description, (String) null, plans.toXML(), manager.getPdc(), true);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO he_bizorg (id, name, description, available_plans) VALUES (?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.name);
            ps.setString(3, this.description);
            ps.setString(4, availablePlanTypes);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public StoreQuota getMailStoreQuota() throws Exception {
        StoreQuota mailstore = (StoreQuota) this.f41id.findChild("mailstore").get();
        return mailstore;
    }

    public StoreQuota getPublicStoreQuota() throws Exception {
        StoreQuota publicstore = (StoreQuota) this.f41id.findChild("pubstore").get();
        return publicstore;
    }

    public void fixPermissions() throws Exception {
        MPFManager manager = MPFManager.getManager();
        String domainName = getDomain().getName();
        MPFManager.Result res = manager.managedExchangeMailEnableGroup(manager.getLDAP(getName()), "Admins@" + getName(), "Admins@" + domainName);
        if (!res.isSuccess()) {
            Session.getLog().error(" Mail Enable \"Admins\" Group problem. " + res.getError());
        }
        MPFManager.Result res2 = manager.managedExchangeMailEnableGroup(manager.getLDAP(getName(), "_Private"), "AllUsers@" + getName(), "AllUsers@" + domainName);
        if (!res2.isSuccess()) {
            Session.getLog().error(" Mail Enable \"AllUsers\" Group problem. " + res2.getError());
        }
    }

    @Override // psoft.hsphere.Resource
    public void postInitDone() throws Exception {
        MPFManager manager = MPFManager.getManager();
        String result = manager.getHES().createOrganizationAddressLists(manager.getLDAP(this.name), getDomain().getName(), manager.getPdc(), true);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            MPFManager manager = MPFManager.getManager();
            String result = manager.getHES().deleteBusinessOrganization(manager.getLDAP(this.name), (String) null, manager.getPdc());
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_bizorg WHERE id = ?");
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

    public TemplateModel FM_checkDomain(String domain) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM he_smtpdomain WHERE UPPER(name) = UPPER(?)");
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                TemplateString templateString = TemplateString.TRUE;
                Session.closeStatement(ps);
                con.close();
                return templateString;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setPrimaryDomain(ResourceId rid) throws Exception {
        ((SMTPDomain) rid.get()).setType(1);
        Collection<ResourceId> col = getId().findAllChildren("domain");
        for (ResourceId currentId : col) {
            if (rid.getId() != currentId.getId()) {
                ((SMTPDomain) currentId.get()).setType(2);
            }
        }
        return this;
    }

    public HePlanSettings getPlanByType(String type) {
        MPFManager manager = MPFManager.getManager();
        HePlanSettings plan = (HePlanSettings) manager.getAvailablePlans().get(type);
        return plan;
    }

    public TemplateModel FM_getPlanByType(String type) {
        return getPlanByType(type);
    }

    public TemplateModel FM_addPlan(String type) throws Exception {
        boolean isAvailable = Session.getAccount().getPlan().areResourcesAvailable(type);
        Tag plans = new Tag("availablePlans");
        plans.addChild(new Tag("planName", getPlanByType(type).get("name").toString()));
        if (!this.availablePlans.contains(type) && isAvailable) {
            MPFManager manager = MPFManager.getManager();
            String result = manager.getHES().addAvailablePlans(manager.getLDAP(this.name), plans.toXML(), manager.getPdc(), true);
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
            this.availablePlans.add(type);
            String availablePlanTypes = "";
            int i = 0;
            while (i < this.availablePlans.size()) {
                String planType = (String) this.availablePlans.get(i);
                availablePlanTypes = availablePlanTypes + (i > 0 ? "," : "") + planType;
                i++;
            }
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE he_bizorg SET available_plans=? WHERE id=?");
                ps.setString(1, availablePlanTypes);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return this;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("description".equals(key)) {
            return new TemplateString(this.description);
        }
        if ("dnsServerName".equals(key)) {
            return new TemplateString(getServerValue("server_name"));
        }
        if ("pubStoreUsage".equals(key)) {
            int pubStoreQuota = ((Integer) getServerValue("pubstore_quota")).intValue();
            int pubStoreFree = ((Integer) getServerValue("pubstore_free")).intValue();
            return new TemplateString(pubStoreQuota - pubStoreFree);
        } else if ("primaryDomain".equals(key)) {
            ResourceId domain = null;
            try {
                domain = getDomain().getId();
            } catch (Exception ex) {
                Session.getLog().error("Problem getting primary domain.", ex);
            }
            return domain;
        } else if ("mailStoreUsage".equals(key)) {
            double usage = 0.0d;
            try {
                usage = getMailstoreUsage();
            } catch (Exception ex2) {
                Session.getLog().error("Impossible to calculate summary mailstore usage", ex2);
            }
            return new TemplateString(usage);
        } else if ("availablePlans".equals(key)) {
            ArrayList list = new ArrayList();
            MPFManager manager = MPFManager.getManager();
            HashMap allPlans = manager.getAvailablePlans();
            for (int i = 0; i < this.availablePlans.size(); i++) {
                HashMap map = new HashMap();
                String planType = (String) this.availablePlans.get(i);
                HePlanSettings plan = (HePlanSettings) allPlans.get(planType);
                map.put("type", planType);
                map.put("description", plan.get("description").toString());
                map.put("settings", plan);
                list.add(map);
                this.f202dc.setConstrain("description", true);
                Collections.sort(list, this.f202dc);
            }
            return new TemplateList(list);
        } else if ("newPlans".equals(key)) {
            ArrayList diff = new ArrayList();
            MPFManager manager2 = MPFManager.getManager();
            HashMap allPlans2 = manager2.getAvailablePlans();
            for (String planType2 : allPlans2.keySet()) {
                boolean isAvailable = false;
                try {
                    isAvailable = Session.getAccount().getPlan().areResourcesAvailable(planType2);
                } catch (Exception ex3) {
                    Session.getLog().warn("Unable to check if the " + planType2 + " resource available." + ex3.getMessage());
                }
                if (!this.availablePlans.contains(planType2) && isAvailable) {
                    HashMap map2 = new HashMap();
                    map2.put("type", planType2);
                    HePlanSettings plan2 = (HePlanSettings) allPlans2.get(planType2);
                    map2.put("description", plan2.get("description").toString());
                    map2.put("settings", plan2);
                    diff.add(map2);
                    this.f202dc.setConstrain("description", true);
                    Collections.sort(diff, this.f202dc);
                }
            }
            return new TemplateList(diff);
        } else {
            return super.get(key);
        }
    }

    public String getName() {
        return this.name;
    }

    public SMTPDomain getDomain() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        ResourceId domainRID = null;
        try {
            ps = con.prepareStatement("SELECT d.id FROM he_smtpdomain AS d JOIN parent_child AS p ON (p.child_id=d.id)  WHERE d.domain_type=? AND p.parent_id=?");
            ps.setInt(1, 1);
            ps.setLong(2, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long domainId = rs.getLong(1);
                int type = TypeRegistry.getIntTypeId("domain");
                domainRID = new ResourceId(domainId, type);
            }
            Session.closeStatement(ps);
            con.close();
            if (domainRID != null) {
                return (SMTPDomain) domainRID.get();
            }
            return (SMTPDomain) this.f41id.findChild("domain").get();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public double getMailstoreUsage() throws Exception {
        Session.getDb();
        int mailstoreUsage = 0;
        Collection<ResourceId> users = this.f41id.getChildHolder().getChildrenByName("bizuser");
        for (ResourceId resid : users) {
            try {
                HePlanSettings plan = ((BusinessUser) resid.get()).getPlanSettings();
                String size = plan.get("MailboxSize").toString();
                mailstoreUsage += Integer.parseInt(size);
            } catch (Exception ex) {
                Session.getLog().error("Unable to get plan properties for the " + resid.getAsString() + " business user." + ex.getMessage());
            }
        }
        return mailstoreUsage / HostEntry.VPS_IP;
    }

    public ResourceId getContactByEmail(String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT c.id FROM he_contacts AS c JOIN parent_child AS p ON (p.child_id=c.id)  WHERE c.targetaddr=? AND p.parent_id=?");
            ps.setString(1, email);
            ps.setLong(2, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long contactId = rs.getLong(1);
                int type = TypeRegistry.getIntTypeId("exchange_contact");
                ResourceId resourceId = new ResourceId(contactId, type);
                Session.closeStatement(ps);
                con.close();
                return resourceId;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ResourceId createContact(String name) throws Exception {
        ArrayList args = new ArrayList();
        args.add(name);
        return addChild("exchange_contact", "", args);
    }

    public void removeContact(ResourceId contact) throws Exception {
        int count = 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT b.id FROM he_bizuser AS b JOIN parent_child AS p ON (p.child_id=b.id)  WHERE b.altRecipient=? AND p.parent_id=?");
            ps.setLong(1, contact.getId());
            ps.setLong(2, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rs.getLong(1);
                count++;
            }
            Session.closeStatement(ps);
            con.close();
            if (count < 2) {
                contact.get().delete();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Document getOrgResourcesInfo() {
        Document doc = null;
        MPFManager manager = MPFManager.getManager();
        try {
            String result = manager.getERMS().queryResourcesByOrganization(manager.getLDAP(this.name), this.name, manager.getPdc(), true);
            MPFManager.Result res = manager.processHeResult(result);
            if (res.isSuccess()) {
                doc = res.getDocument();
            }
        } catch (Exception ex) {
            Session.getLog().error("Unable to get organization resources ", ex);
        }
        return doc;
    }

    public HashMap getOrgUsersInfo(boolean force, UsersInfoType type) {
        Long timeToLive = (Long) this.usersReport.get("time");
        HashMap info = (HashMap) this.usersReport.get("info");
        HashMap foldersInfo = (HashMap) this.usersReport.get("folders_info");
        if (!force && timeToLive != null && info != null && foldersInfo != null && System.currentTimeMillis() - timeToLive.longValue() < TIME_TO_LIVE) {
            return type == UsersInfoType.MAILBOXES ? info : foldersInfo;
        }
        HashMap info2 = new HashMap();
        HashMap foldersInfo2 = new HashMap();
        this.usersReport.clear();
        this.usersReport.put("time", new Long(System.currentTimeMillis()));
        Document doc = null;
        MPFManager manager = MPFManager.getManager();
        try {
            String result = manager.getERMS().queryConsumersByOrganization(manager.getLDAP(this.name), this.name, manager.getPdc(), true);
            MPFManager.Result res = manager.processHeResult(result);
            if (res.isSuccess()) {
                doc = res.getDocument();
            }
            NodeList mailboxes = XPathAPI.selectNodeList(doc, "/response/data/mailboxes/mailbox");
            NodeList publicfolders = XPathAPI.selectNodeList(doc, "/response/data/publicFolders/publicFolder");
            for (int i = 0; i < mailboxes.getLength(); i++) {
                HashMap map = new HashMap();
                Node properties = mailboxes.item(i);
                map.put("GUID", XPathAPI.selectSingleNode(properties, "GUID/text()").getNodeValue());
                map.put("kilobytes", XPathAPI.selectSingleNode(properties, "kilobytes/text()").getNodeValue());
                String path = XPathAPI.selectSingleNode(properties, "path/text()").getNodeValue();
                int start = path.indexOf("CN=");
                int end = path.indexOf(",");
                String name = path.substring(start + 3, end);
                info2.put(name, map);
            }
            this.usersReport.put("info", info2);
            for (int i2 = 0; i2 < publicfolders.getLength(); i2++) {
                HashMap map2 = new HashMap();
                Node properties2 = publicfolders.item(i2);
                map2.put("GUID", XPathAPI.selectSingleNode(properties2, "GUID/text()").getNodeValue());
                String folderPath = XPathAPI.selectSingleNode(properties2, "folderPath/text()").getNodeValue();
                map2.put("folderPath", folderPath);
                map2.put("megabytes", XPathAPI.selectSingleNode(properties2, "megabytes/text()").getNodeValue());
                foldersInfo2.put(folderPath, map2);
            }
            this.usersReport.put("folders_info", foldersInfo2);
        } catch (Exception ex) {
            Session.getLog().error("Unable to get organization consumers ", ex);
        }
        return type == UsersInfoType.MAILBOXES ? info2 : foldersInfo2;
    }

    public Object getServerValue(String key) {
        return getServerValue(key, false);
    }

    public Object getServerValue(String key, boolean force) {
        Long timeToLive = (Long) this.serverReport.get("time");
        Object value = this.serverReport.get(key);
        if (!force && timeToLive != null && value != null && System.currentTimeMillis() - timeToLive.longValue() < TIME_TO_LIVE) {
            return value;
        }
        updateServerInfo();
        return this.serverReport.get(key);
    }

    private Integer getQuotaVal(Document doc, QuotaValueType type) {
        Integer result = null;
        String value = "";
        try {
            switch (type) {
                case MAILSTORE_QUOTA:
                    value = XPathAPI.selectSingleNode(doc, "/response/data/orgResources/mailStore/megabytes/text()").getNodeValue();
                    break;
                case MAILSTORE_FREE:
                    value = XPathAPI.selectSingleNode(doc, "/response/data/orgResources/mailStore/megabytesFree/text()").getNodeValue();
                    break;
                case PUBLIC_STORE:
                case PUBLIC_STORE_FREE:
                    value = XPathAPI.selectSingleNode(doc, "/response/data/orgResources/publicStore/megabytes/text()").getNodeValue();
                    break;
            }
            result = new Integer(Integer.parseInt(value));
        } catch (Exception ex) {
            Session.getLog().error("Unable to get quota value: " + value, ex);
        }
        return result;
    }

    public void updateServerInfo() {
        MPFManager manager = MPFManager.getManager();
        try {
            Document doc = getOrgResourcesInfo();
            if (doc != null) {
                String serverName = XPathAPI.selectSingleNode(doc, "/response/data/orgResources/mailStore/serverName/text()").getNodeValue();
                Integer mailStoreQuota = getQuotaVal(doc, QuotaValueType.MAILSTORE_QUOTA);
                Integer mailStoreFree = getQuotaVal(doc, QuotaValueType.MAILSTORE_FREE);
                Integer pubStoreQuota = getQuotaVal(doc, QuotaValueType.PUBLIC_STORE);
                Integer pubStoreFree = getQuotaVal(doc, QuotaValueType.PUBLIC_STORE_FREE);
                String ldap = manager.getLdapString();
                String path = "LDAP://CN=" + serverName + ", CN=Computers," + ldap.substring(ldap.indexOf("DC="));
                Document doc2 = null;
                String dnsServerName = null;
                MessageElement properties = new MessageElement("", "propertyList");
                MessageElement property = new MessageElement("", "property");
                property.addAttribute("", "name", "dNSHostName");
                properties.addChild(property);
                String result = manager.getHES().getProperties(path, manager.getPdc(), properties.toString());
                MPFManager.Result res = manager.processHeResult(result);
                if (res.isSuccess()) {
                    doc2 = res.getDocument();
                }
                NodeList prop = XPathAPI.selectNodeList(doc2, "/response/data/properties/property");
                int i = 0;
                while (true) {
                    if (i >= prop.getLength()) {
                        break;
                    } else if (!"dNSHostName".equals(prop.item(i).getAttributes().getNamedItem("name").getNodeValue())) {
                        i++;
                    } else {
                        dnsServerName = prop.item(i).getFirstChild().getFirstChild().getNodeValue();
                        break;
                    }
                }
                this.serverReport.clear();
                this.serverReport.put("server_id", serverName);
                this.serverReport.put("server_name", dnsServerName);
                if (mailStoreQuota != null) {
                    this.serverReport.put("mailstore_quota", mailStoreQuota);
                }
                if (mailStoreFree != null) {
                    this.serverReport.put("mailstore_free", mailStoreFree);
                }
                if (pubStoreQuota != null) {
                    this.serverReport.put("pubstore_quota", pubStoreQuota);
                }
                if (pubStoreFree != null) {
                    this.serverReport.put("pubstore_free", pubStoreFree);
                }
                this.serverReport.put("time", new Long(System.currentTimeMillis()));
            }
        } catch (Exception ex) {
            Session.getLog().error("Unable to get server name ", ex);
        }
    }

    public static ArrayList getAvailablePlanTypes() {
        ArrayList plans = new ArrayList();
        MPFManager manager = MPFManager.getManager();
        HashMap allPlans = manager.getAvailablePlans();
        for (String planType : allPlans.keySet()) {
            planType = "";
            try {
                if (Session.getAccount().getPlan().areResourcesAvailable(planType)) {
                    plans.add(planType);
                }
            } catch (Exception ex) {
                Session.getLog().error("Unable to get " + planType + " from plan", ex);
            }
        }
        return plans;
    }
}
