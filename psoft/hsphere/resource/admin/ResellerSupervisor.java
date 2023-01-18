package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import psoft.hsp.HSPkg;
import psoft.hsp.Package;
import psoft.hsp.PackageNotFoundException;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.hsphere.resource.Urchin4Resource;
import psoft.hsphere.resource.dns.ADNSRecord;
import psoft.hsphere.resource.dns.CNAMERecord;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.dns.MXRecord;
import psoft.hsphere.resource.email.MailRelay;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ResellerSupervisor.class */
public class ResellerSupervisor extends Resource {
    private static final List allowedPlans = Arrays.asList(FMACLManager.ADMIN, "mysql", "unix", "unixreal", "windows", "windowsreal", "mailonly");

    public ResellerSupervisor(int type, Collection init) throws Exception {
        super(type, init);
    }

    public ResellerSupervisor(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("system_info".equals(key)) {
            return new SystemInfo();
        }
        if ("priced_types".equals(key)) {
            return new TemplateList(TypeRegistry.getPricedTypes());
        }
        try {
            if ("resellers".equals(key)) {
                return buildResellersList();
            }
            if ("pkgs".equals(key)) {
                try {
                    return new TemplateList(getInstalledPkgs());
                } catch (Exception ex) {
                    Session.getLog().error("Unable to get H-sphere packages list", ex);
                }
            }
            return super.get(key);
        } catch (Exception e) {
            Session.getLog().error("Problem with resellers list", e);
            return null;
        }
    }

    protected TemplateModel buildResellersList() throws Exception {
        TemplateList list = new TemplateList();
        for (Reseller res : Reseller.getResellerList()) {
            TemplateMap map = new TemplateMap();
            map.put("id", new TemplateString(res.getId()));
            map.put("name", new TemplateString(res.getUser()));
            map.put("url", new TemplateString(res.getURL()));
            list.add((TemplateModel) map);
        }
        return list;
    }

    public TemplateModel FM_getUsersCpURL(String login) throws Exception {
        User u = User.getUser(login);
        return new TemplateString(u.getCpURL());
    }

    public TemplateModel FM_reloadResellerPrices(int planId) throws Exception {
        reloadResellerPrices(planId);
        return new TemplateOKResult();
    }

    public void reloadResellerPrices(int planId) throws Exception {
        for (Reseller reseller : Reseller.getResellerList()) {
            if (reseller.getResellerPlanId() == planId) {
                reseller.setPrices();
            }
        }
    }

    public TemplateModel FM_moveUserToReseller(long userId, long tResellerId) throws Exception {
        moveUserToReseller(userId, tResellerId);
        return this;
    }

    protected boolean hasUserNonMovableDomain(Account a, String domainType) throws Exception {
        Collection sDomain = a.getId().findAllChildren(domainType);
        return (sDomain == null || sDomain.isEmpty()) ? false : true;
    }

    protected void moveDNS(String zoneName) throws Exception {
        PreparedStatement ps1 = null;
        Connection con1 = Session.getDb();
        try {
            try {
                PreparedStatement ps12 = con1.prepareStatement("SELECT a.id, a.type FROM dns_records a WHERE a.name LIKE ? OR a.name = ? AND a.id NOT IN (SELECT mxrecord_id FROM mx_list)");
                ps12.setString(1, "%." + zoneName);
                ps12.setString(2, zoneName);
                ResultSet rs1 = ps12.executeQuery();
                while (rs1.next()) {
                    long recordId = rs1.getLong(1);
                    String recordType = rs1.getString(2);
                    if (recordType.equalsIgnoreCase("A")) {
                        try {
                            int typeA = new Integer(Integer.parseInt(TypeRegistry.getTypeId("a_record"))).intValue();
                            ADNSRecord resA = (ADNSRecord) ADNSRecord.get(new ResourceId(recordId, typeA));
                            resA.reconfigure();
                        } catch (Exception ex) {
                            Session.getLog().error("Cannot recreate A Record ", ex);
                        }
                    }
                    if (recordType.equalsIgnoreCase("CNAME")) {
                        try {
                            int typeCNAME = new Integer(Integer.parseInt(TypeRegistry.getTypeId("cname_record"))).intValue();
                            CNAMERecord resCName = (CNAMERecord) CNAMERecord.get(new ResourceId(recordId, typeCNAME));
                            resCName.reconfigure();
                        } catch (Exception ex2) {
                            Session.getLog().error("Cannot recreate CNAME Record ", ex2);
                        }
                    }
                    if (recordType.equalsIgnoreCase("MX")) {
                        try {
                            int typeMX = new Integer(Integer.parseInt(TypeRegistry.getTypeId("mx"))).intValue();
                            MXRecord resMX = (MXRecord) MXRecord.get(new ResourceId(recordId, typeMX));
                            resMX.reconfigure();
                        } catch (Exception ex3) {
                            Session.getLog().error("Cannot recreate MX Record ", ex3);
                        }
                    }
                }
                ps12.close();
                ps1 = con1.prepareStatement("SELECT p2.child_id FROM dns_records d, mx_list m, parent_child p1, parent_child p2 WHERE (d.name LIKE ? OR d.name = ?) AND m.mxrecord_id = d.id AND m.id=p1.child_id AND p1.parent_id=p2.parent_id AND p2.child_type=1010");
                ps1.setString(1, "%." + zoneName);
                ps1.setString(2, zoneName);
                ResultSet rs12 = ps1.executeQuery();
                while (rs12.next()) {
                    long recordId2 = rs12.getLong(1);
                    int typeMailRelay = new Integer(Integer.parseInt(TypeRegistry.getTypeId("mail_relay"))).intValue();
                    MailRelay mr = (MailRelay) MailRelay.get(new ResourceId(recordId2, typeMailRelay));
                    mr.reconfigure();
                }
                Session.closeStatement(ps1);
                con1.close();
            } catch (Throwable th) {
                Session.closeStatement(ps1);
                con1.close();
                throw th;
            }
        } catch (Exception e) {
            Session.getLog().error("Cannot move DNS under another reseller", e);
            Session.closeStatement(ps1);
            con1.close();
        }
    }

    public void moveUserToReseller(long userId, long tResellerId) throws Exception {
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Connection con = Session.getTransConnection();
        try {
            try {
                User u = User.getUser(userId);
                if (u.getAccountIds(true).size() == 0) {
                    return;
                }
                long sResellerId = u.getResellerId();
                User reseller = User.getUser(tResellerId);
                if (reseller.getAccountIds(true).size() == 0) {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                    Session.commitTransConnection(con);
                    return;
                }
                new ArrayList();
                Session.setResellerId(reseller.getId());
                Collection rAvailablePlanList = Plan.getPlanList();
                Iterator i = u.getAccountIds().iterator();
                while (i.hasNext()) {
                    Account acc = u.getAccount((ResourceId) i.next());
                    Session.setUser(u);
                    Session.setAccount(acc);
                    if (hasUserNonMovableDomain(acc, "service_domain")) {
                        throw new HSUserException("su.serv_domain_not_mov");
                    }
                    if (hasUserNonMovableDomain(acc, "3ldomain")) {
                        throw new HSUserException("su.thirdlevel_domain_not_mov");
                    }
                    Plan srcPlan = acc.getPlan();
                    if (!allowedPlans.contains(srcPlan.getValue("_CREATED_BY_"))) {
                        throw new HSUserException("su.plan_not_mov", new Object[]{srcPlan.getDescription()});
                    }
                    Plan compatiblePlan = null;
                    Iterator pl = rAvailablePlanList.iterator();
                    while (true) {
                        if (!pl.hasNext()) {
                            break;
                        }
                        Plan tPlan = (Plan) pl.next();
                        if (srcPlan.isTotalyEqual(tPlan)) {
                            compatiblePlan = tPlan;
                            break;
                        }
                    }
                    if (compatiblePlan == null) {
                        compatiblePlan = new Plan(srcPlan, "", tResellerId);
                    }
                    Session.setUser(u);
                    Session.setAccount(acc);
                    acc.closeBillingPeriod(TimeUtils.getDate(), true, false);
                    PreparedStatement ps = con.prepareStatement("UPDATE accounts SET plan_id = ?, reseller_id = ? WHERE id =?");
                    ps.setInt(1, compatiblePlan.getId());
                    ps.setLong(2, tResellerId);
                    ps.setLong(3, acc.getId().getId());
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                }
                PreparedStatement ps2 = con.prepareStatement("UPDATE users SET reseller_id = ? WHERE id = ?");
                ps2.setLong(1, tResellerId);
                ps2.setLong(2, userId);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                if (sResellerId != 1) {
                    Reseller.getReseller(sResellerId).loadTypeCounter();
                }
                if (tResellerId != 1) {
                    Reseller.getReseller(tResellerId).loadTypeCounter();
                }
                User.getUser(u.getLogin(), true);
                Session.setUser(u);
                Session.setResellerId(tResellerId);
                Session.getLog().debug("Reseller id: " + tResellerId);
                Iterator i2 = u.getAccountIds(true).iterator();
                while (i2.hasNext()) {
                    ResourceId accId = (ResourceId) i2.next();
                    ((Account) Account.get(accId)).removeFromCache();
                    Account acc2 = (Account) Account.get(accId);
                    Session.setAccount(acc2);
                    Session.setResellerId(tResellerId);
                    acc2.openBillingPeriod(TimeUtils.getDate(), false);
                    Collection<ResourceId> sDNSZone = acc2.getId().findAllChildren("dns_zone");
                    if (sDNSZone != null && !sDNSZone.isEmpty()) {
                        for (ResourceId dnsZoneId : sDNSZone) {
                            Resource dnsZone = Resource.get(dnsZoneId);
                            if (dnsZone instanceof DNSZone) {
                                String zoneName = ((DNSZone) dnsZone).getName();
                                moveDNS(zoneName);
                            }
                        }
                    }
                }
                Session.setUser(oldUser);
                Session.setAccount(oldAcc);
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                Session.getLog().error("Error during user move ", ex);
                throw ex;
            }
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAcc);
            Session.commitTransConnection(con);
        }
    }

    public TemplateModel FM_resetCredit() throws Exception {
        AccountManager.resetCredit(true);
        return this;
    }

    public TemplateModel FM_getTicket(long resellerId, long ticketId) throws Exception {
        Session.save();
        Reseller res = Reseller.getReseller(resellerId);
        Session.setResellerId(res.getId());
        Session.getLog().debug("New reseller : " + res.getId());
        User reseller = User.getUser(res.getAdmin());
        Session.getLog().debug("New user : " + reseller.getLogin());
        Session.setUser(reseller);
        HashSet accountIds = reseller.getAccountIds();
        Iterator iterator = accountIds.iterator();
        while (iterator.hasNext()) {
            ResourceId accountId = (ResourceId) iterator.next();
            Account res_account = reseller.getAccount(accountId);
            Session.getLog().debug("New account: " + res_account.get("description").toString());
            Session.setAccount(res_account);
        }
        Ticket result = new Ticket(ticketId);
        Session.restore();
        return result;
    }

    public TemplateModel FM_getContentMoveItem(long id) throws Exception {
        return getContentMoveItem(id);
    }

    public ContentMoveItem getContentMoveItem(long id) throws Exception {
        return ContentMoveItem.get(id);
    }

    public List getInstalledPkgs() throws Exception {
        return HSPkg.getPackages();
    }

    public TemplateModel FM_getPackage(String pkgName) throws Exception {
        try {
            return new HSPkg(Package.getPackage(pkgName));
        } catch (PackageNotFoundException e) {
            Session.getLog().error("H-Sphere package with name " + pkgName + " not found", e);
            throw new HSUserException("admin.package.not_found", new Object[]{pkgName});
        }
    }

    public TemplateModel FM_canAccountBeMoved(long accountId) throws Exception {
        if (canAccountBeMoved(accountId)) {
            return new TemplateString("1");
        }
        return null;
    }

    protected boolean canAccountBeMoved(long accountId) throws Exception {
        boolean result = true;
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        Account a = Account.getAccount(accountId);
        User u = a.getUser();
        HashSet accounts = u.getAccountIds();
        try {
            try {
                Session.setUser(u);
                Iterator iterator = accounts.iterator();
                while (true) {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    ResourceId r = (ResourceId) iterator.next();
                    Account tmp = (Account) Account.get(r);
                    Session.setAccount(tmp);
                    Plan p = tmp.getPlan();
                    if (p.isResellerPlan()) {
                        result = false;
                        break;
                    }
                }
            } catch (Exception e) {
                Session.getLog().error("Exception ", e);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
            }
            return result;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    public TemplateModel FM_canAccountBeSeparated(long accountId) throws Exception {
        if (canAccountBeSeparated(accountId)) {
            return new TemplateString("1");
        }
        return null;
    }

    protected boolean canAccountBeSeparated(long accountId) throws Exception {
        boolean isReseller = false;
        boolean hasManyAccounts = false;
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        Account a = Account.getAccount(accountId);
        User u = a.getUser();
        try {
            try {
                Session.setUser(u);
                Session.setAccount(a);
                Plan p = a.getPlan();
                if (p.isResellerPlan()) {
                    isReseller = true;
                }
            } catch (Exception e) {
                Session.getLog().error("Exception ", e);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
            }
            HashSet accounts = u.getAccountIds();
            if (accounts.size() > 1) {
                hasManyAccounts = true;
            }
            if (!isReseller && hasManyAccounts) {
                return true;
            }
            return false;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    public TemplateModel FM_breakUser(long accountId, String newUserName, String newPassword) throws Exception {
        breakUser(accountId, newUserName, newPassword);
        return this;
    }

    protected void reconfigUrchin(User u, User newUser) throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        HashSet accs = u.getAccountIds();
        accs.addAll(newUser.getAccountIds());
        Iterator iterator = accs.iterator();
        while (iterator.hasNext()) {
            ResourceId accId = (ResourceId) iterator.next();
            try {
                try {
                    Account acc = accId.getAccount();
                    Session.setAccount(acc);
                    Session.setUser(acc.getUser());
                    ResourceId rid = accId.findChild("urchin4");
                    Urchin4Resource urchin = (Urchin4Resource) rid.get();
                    urchin.reconfigure();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                } catch (Exception e) {
                    Session.getLog().error("Exception Urchin reconfig ", e);
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                }
            } catch (Throwable th) {
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                throw th;
            }
        }
    }

    protected void breakUser(long accountId, String newUserName, String newUserPassword) throws Exception {
        Account a = Account.getAccount(accountId);
        User u = a.getUser();
        long resellerId = a.getResellerId();
        if (isUserExists(newUserName)) {
            throw new HSUserException("User with login " + newUserName + " already exists!");
        }
        User.createUser(newUserName, newUserPassword, resellerId);
        User newUser = User.getUser(newUserName);
        newUser.setActiveAccount(a);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE user_account SET user_id = ? WHERE account_id = ?");
            ps.setLong(1, newUser.getId());
            ps.setLong(2, accountId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            BillingInfoObject bi = a.getBillingInfo();
            BillingInfoObject newBI = new BillingInfoObject(bi.getName(), bi.getLastName(), bi.getCompany(), bi.getAddress1(), bi.getAddress2(), bi.getCity(), bi.getState(), bi.getState2(), bi.getPostalCode(), bi.getCountry(), bi.getPhone(), bi.getEmail(), bi.getType(), bi.getPaymentInstrument(), bi.getExemptionCode(), newUser.getId());
            a.updateBillingInfoId(newBI);
            u.getAccountIds().remove(a.getId());
            newUser.getAccountIds().add(a.getId());
            reconfigUrchin(u, newUser);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean isUserExists(String login) throws Exception {
        boolean result = false;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM users WHERE username = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = true;
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
}
