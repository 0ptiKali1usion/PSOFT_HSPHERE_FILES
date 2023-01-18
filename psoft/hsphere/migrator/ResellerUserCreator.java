package psoft.hsphere.migrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.migrator.creator.AbstractUserCreator;
import psoft.hsphere.migrator.creator.MigratedUser;
import psoft.hsphere.migrator.creator.MigratedUsersContainer;
import psoft.hsphere.migrator.creator.UserCreatorConfig;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.AdmDNSManager;
import psoft.hsphere.resource.admin.ResellerResource;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.tools.SetupCP;
import psoft.util.FakeRequest;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/ResellerUserCreator.class */
public class ResellerUserCreator extends AbstractUserCreator {
    boolean finished;
    private MigratedUsersContainer muc;
    private String resumedUser;

    public ResellerUserCreator(UserCreatorConfig conf) throws Exception {
        super(conf);
        this.finished = true;
        this.muc = null;
        this.resumedUser = "";
        this.resumedUser = conf.getResumedUser();
    }

    public ResellerUserCreator(UserCreatorConfig conf, MigratedUsersContainer muc) throws Exception {
        super(conf);
        this.finished = true;
        this.muc = null;
        this.resumedUser = "";
        this.muc = muc;
        this.resumedUser = conf.getResumedUser();
    }

    public static void main(String[] argv) throws Exception {
        try {
            ResellerUserCreator migrator = new ResellerUserCreator(new UserCreatorConfig(argv));
            migrator.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }

    public boolean isFinished() {
        return this.finished;
    }

    @Override // psoft.hsphere.migrator.creator.UserCreator
    public int execute() throws Exception {
        if (this.conf.isPrintPlans()) {
            ArrayList notPrintableUsers = printNecessaryPlans(this.conf.getDocument().getDocumentElement().getElementsByTagName(FMACLManager.RESELLER));
            CommonUserCreator migrator = new CommonUserCreator(this.conf);
            migrator.printNecessaryPlans(this.conf.getDocument().getDocumentElement().getElementsByTagName(FMACLManager.USER), notPrintableUsers);
            return 0;
        }
        outMessage("Initializing\n");
        this.finished = false;
        getCp().setConfig();
        getCp().initLog();
        if (this.conf.isMailActivated()) {
            try {
                outMessage("Initializing mail");
                Session.initMailer();
                outOK();
            } catch (Exception ex) {
                try {
                    outFail("Failed to initialize mail", ex);
                } catch (Exception ex1) {
                    Session.getLog().debug(ex1.getMessage(), ex1);
                }
            }
        }
        try {
            if (this.conf.isCreateOnlyUsers()) {
                createOnlyUsers();
            } else {
                createResellers();
            }
            this.finished = true;
            return 0;
        } catch (Exception e) {
            this.finished = true;
            return 0;
        } catch (Throwable th) {
            this.finished = true;
            throw th;
        }
    }

    private void createResellers() throws Exception {
        User admin = User.getUser(FMACLManager.ADMIN);
        Account adm = admin.getAccount(new ResourceId(1L, 0));
        Session.setUser(admin);
        Session.setAccount(adm);
        Element root = this.conf.getDocument().getDocumentElement();
        NodeList list = root.getElementsByTagName(FMACLManager.RESELLER);
        for (int i = 0; i < list.getLength(); i++) {
            createUser((Element) list.item(i));
        }
    }

    private void createOnlyUsers() throws Exception {
        Element doc = this.conf.getDocument().getDocumentElement();
        NodeList resellers = doc.getElementsByTagName(FMACLManager.RESELLER);
        boolean foundResumed = false;
        String login = null;
        for (int i = 0; i < resellers.getLength(); i++) {
            try {
                login = resellers.item(i).getAttributes().getNamedItem("login").getNodeValue();
                if (!this.resumedUser.equals("")) {
                    NodeList resellUsers = XPathAPI.selectNodeList(resellers.item(i), "self::reseller/users/user");
                    NodeList resumedUsers = XPathAPI.selectNodeList(resellers.item(i), "self::reseller/users/user[@login=\"" + this.resumedUser + "\"]");
                    if (resellUsers.getLength() > 0) {
                        if (resumedUsers.getLength() > 0) {
                            this.conf.setResumedUser(this.resumedUser);
                            foundResumed = true;
                        } else {
                            this.conf.clearResumedUser();
                        }
                    }
                } else {
                    foundResumed = true;
                    this.conf.clearResumedUser();
                }
                if (foundResumed) {
                    addUsers(login, (Element) resellers.item(i));
                }
            } catch (Exception exc) {
                outFail("Not found reseller - " + login, exc);
                return;
            }
        }
        addAdminUsers(doc);
    }

    private void addUsers(String login, Element res) throws Exception {
        User resusr = User.getUser(login);
        Reseller resell = Reseller.getReseller(resusr.getId());
        Account resAccount = resell.getAccount();
        Session.setUser(resusr);
        Session.setAccount(resAccount);
        addResellerUsers(resAccount, res);
    }

    private ArrayList printNecessaryPlans(NodeList resellers) {
        ArrayList notPrintableUsers = new ArrayList();
        for (int i = 0; i < resellers.getLength(); i++) {
            try {
                String adminLogin = XPathAPI.selectNodeList(resellers.item(i), "self::reseller/administrator/@login").item(0).getNodeValue();
                notPrintableUsers.add(adminLogin);
                String resellerLogin = XPathAPI.selectNodeList(resellers.item(i), "self::reseller/@login").item(0).getNodeValue();
                NodeList resellerPlans = XPathAPI.selectNodeList(resellers.item(i), "self::reseller/@plan");
                outMessage("\nPlans necessary for reseller - " + resellerLogin + " :\n");
                for (int j = 0; j < resellerPlans.getLength(); j++) {
                    outMessage("\t" + resellerPlans.item(j).getNodeValue() + "\n");
                }
            } catch (Exception e) {
            }
        }
        return notPrintableUsers;
    }

    @Override // psoft.hsphere.migrator.creator.UserCreator
    public User createUser(Element reseller) throws Exception {
        Hashtable initval = new Hashtable();
        String login = reseller.getAttributes().getNamedItem("login").getNodeValue();
        String password = reseller.getAttributes().getNamedItem("password").getNodeValue();
        String planName = reseller.getAttributes().getNamedItem("plan").getNodeValue();
        if (this.muc != null) {
            this.currentUser = this.muc.getUser(login);
            login = this.currentUser.getUserName();
            planName = this.currentUser.getPlanName();
        }
        if (this.conf.isResumed() && !this.conf.getResumedUser().equals(login)) {
            outMessage("Skipping " + login + ".....\n");
            return null;
        }
        this.conf.setResumed(false);
        try {
            outMessage("Resolving plan ID for plan " + planName);
            int planId = Plan.getPlanIdByName(planName);
            if (planId != -1) {
                outOK();
                NodeList tmp = reseller.getElementsByTagName("zone");
                Element szone = tmp.getLength() > 0 ? (Element) tmp.item(0) : null;
                if (szone == null) {
                    outMessage("No service zone defenition found. Accont can not be created\n");
                    return null;
                }
                NodeList tmp2 = reseller.getElementsByTagName("administrator");
                Element administrator = tmp2.getLength() > 0 ? (Element) tmp2.item(0) : null;
                if (administrator == null) {
                    outMessage("No administrator account defenition found. Accont can not be created\n");
                    return null;
                }
                User oldUser = Session.getUser();
                Account oldAcc = Session.getAccount();
                try {
                    if (this.conf.isClearUp()) {
                        try {
                            outMessage("Clearing up user " + login);
                            clearUpUser(reseller);
                            outOK();
                            this.conf.setClearUp(false);
                            Session.setUser(oldUser);
                            Session.setAccount(oldAcc);
                        } catch (Exception ex) {
                            outFail(ex.getMessage(), ex);
                            this.conf.setClearUp(false);
                            Session.setUser(oldUser);
                            Session.setAccount(oldAcc);
                            return null;
                        }
                    }
                    try {
                        outMessage("Cheking reseller " + login + "... ");
                        if (!canBeCreated(reseller)) {
                            return null;
                        }
                        outOK();
                        try {
                            outMessage("Creating user " + login);
                            User.createUser(login, password, Session.getResellerId());
                            User u = User.getUser(login);
                            Session.setUser(u);
                            outOK();
                            initval.put("login", new String[]{u.getLogin()});
                            initval.put("password", new String[]{u.getPassword()});
                            initval.put("type_domain", new String[]{"without_domain"});
                            initval.put("_mod", new String[]{"nodomain"});
                            NodeList infos = XPathAPI.selectNodeList(reseller, "self::reseller/info");
                            for (int i = 0; i < infos.getLength(); i++) {
                                try {
                                    Element info = (Element) infos.item(i);
                                    String prefix = info.getAttributes().getNamedItem("prefix").getNodeValue();
                                    NodeList items = info.getElementsByTagName("item");
                                    for (int j = 0; j < items.getLength(); j++) {
                                        Node item = items.item(j);
                                        String name = prefix + item.getAttributes().getNamedItem("name").getNodeValue();
                                        String value = "";
                                        if (item.getFirstChild() != null) {
                                            value = item.getFirstChild().getNodeValue();
                                        }
                                        initval.put(name, new String[]{value});
                                    }
                                } catch (Throwable th) {
                                    clearRequest();
                                    throw th;
                                }
                            }
                            try {
                                outMessage("Adding account for " + u.getLogin());
                                Session.setRequest(new FakeRequest(initval));
                                SignupManager.saveRequest(Session.getRequest());
                                BillingInfoObject bi = new BillingInfoObject(new NameModifier("_bi_"));
                                ContactInfoObject ci = new ContactInfoObject(new NameModifier("_ci_"));
                                Account a = u.addAccount(planId, bi, ci, "Account", "", 0);
                                outOK();
                                clearRequest();
                                englareCredit(a, 10000.0d);
                                try {
                                    outMessage("Adding administrative account");
                                    addAdmAccount(a, administrator);
                                    outOK();
                                    try {
                                        outMessage("Adding service DNS zone");
                                        addServiceZone(a, szone);
                                    } catch (Exception ex2) {
                                        outFail(ex2.getMessage(), ex2);
                                    }
                                    try {
                                        createSystemPlan(a, szone);
                                    } catch (Exception ex3) {
                                        outFail(ex3.getMessage(), ex3);
                                    }
                                    addCredit(a, "0");
                                    if (this.muc != null) {
                                        this.muc.setAccountMark(login);
                                    }
                                    return u;
                                } catch (Exception ex4) {
                                    outFail(ex4.getMessage(), ex4);
                                    return u;
                                }
                            } catch (Exception ex5) {
                                outFail(ex5.getMessage(), ex5);
                                clearRequest();
                                return null;
                            }
                        } catch (Exception ex6) {
                            outFail(ex6.getMessage(), ex6);
                            return null;
                        }
                    } catch (Exception ex7) {
                        outFail(ex7.toString());
                        return null;
                    }
                } catch (Throwable th2) {
                    this.conf.setClearUp(false);
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                    throw th2;
                }
            }
            outFail("Reseller " + login + " can not be created.");
            return null;
        } catch (Exception ex8) {
            outFail("Accont can not be created", ex8);
            return null;
        }
    }

    private boolean canBeCreated(Element reseller) throws Exception {
        boolean result = true;
        String login = reseller.getAttributes().getNamedItem("login").getNodeValue();
        User u = null;
        try {
            u = User.getUser(login);
        } catch (Exception e) {
        }
        if (u != null) {
            outFail("Reseller with the same login is already registered.");
            result = false;
        }
        return result;
    }

    private String getAdminAccount(Account racc) throws Exception {
        ResourceId resellerId = racc.getId().FM_getChild(FMACLManager.RESELLER);
        ResellerResource resellerRes = (ResellerResource) resellerId.get();
        String adminLogin = null;
        if (resellerRes.getAdminAccount() != null) {
            adminLogin = resellerRes.getAdminAccount().get("login").toString();
        }
        return adminLogin;
    }

    private void addResellerUsers(Account racc, Element reseller) throws Exception {
        User resellerUser = Session.getUser();
        Account resellerAccount = Session.getAccount();
        NodeList users = XPathAPI.selectNodeList(reseller, "self::reseller/users/user[@login!=\"" + getAdminAccount(racc) + "\"]");
        NodeList resumedUsers = XPathAPI.selectNodeList(reseller, "self::reseller/users/user[@login=\"" + this.resumedUser + "\"]");
        createNotResumedUsers(users, resumedUsers);
        Session.setUser(resellerUser);
        Session.setAccount(resellerAccount);
    }

    private void createNotResumedUsers(NodeList users, NodeList resumedUsers) throws Exception {
        if (users.getLength() > 0) {
            if (resumedUsers.getLength() > 0) {
                this.conf.setResumedUser(this.resumedUser);
            } else {
                this.conf.clearResumedUser();
            }
            CommonUserCreator migrator = new CommonUserCreator(this.conf);
            migrator.execute(users);
            return;
        }
        outMessage("\nNot found users for reseller - " + Session.getReseller().getUser() + "\n");
    }

    private void addAdminUsers(Element root) throws Exception {
        User admin = User.getUser(FMACLManager.ADMIN);
        Account adm = admin.getAccount(new ResourceId(1L, 0));
        Session.setUser(admin);
        Session.setAccount(adm);
        NodeList users = XPathAPI.selectNodeList(root, "self::resellers/users/user");
        NodeList resumedUsers = XPathAPI.selectNodeList(root, "self::resellers/users/user[@login=\"" + this.resumedUser + "\"]");
        createNotResumedUsers(users, resumedUsers);
        Session.setUser(admin);
        Session.setAccount(adm);
    }

    private void addAdmAccount(Account racc, Element administrator) throws Exception {
        Hashtable args = new Hashtable();
        String adminLogin = administrator.getAttributes().getNamedItem("login").getNodeValue();
        String adminPasswd = administrator.getAttributes().getNamedItem("password").getNodeValue();
        args.put("login", new String[]{adminLogin});
        args.put("password", new String[]{adminPasswd});
        ResourceId resellerId = racc.getId().FM_getChild(FMACLManager.RESELLER);
        ResellerResource reseller = (ResellerResource) resellerId.get();
        try {
            setRequest(args);
            reseller.FM_createAdmin();
            clearRequest();
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    private void addServiceZone(Account a, Element szone) throws Exception {
        String serviceZone = szone.getAttributes().getNamedItem("name").getNodeValue();
        String zoneAdmin = szone.getAttributes().getNamedItem("email").getNodeValue();
        NodeList tmp = szone.getElementsByTagName("instantalias");
        Element alias = tmp.getLength() > 0 ? (Element) tmp.item(0) : null;
        if (alias == null) {
            outMessage("No instant alias defenition found.\n");
            return;
        }
        String aliasPrefix = alias.getAttributes().getNamedItem("prefix").getNodeValue();
        int aliasTag = Integer.parseInt(alias.getAttributes().getNamedItem("tag").getNodeValue());
        ResourceId resellerId = a.getId().FM_getChild(FMACLManager.RESELLER);
        ResellerResource reseller = (ResellerResource) resellerId.get();
        User admResUser = User.getUser(reseller.getAdmin());
        Session.getUser();
        Account admAccount = reseller.getAdminAccount();
        if (admAccount != null) {
            User oldUser = Session.getUser();
            Account oldAcc = Session.getAccount();
            try {
                Session.setUser(admResUser);
                Session.setAccount(admAccount);
                Iterator i = HostManager.getRandomHostsList(2).iterator();
                long master = ((HostEntry) i.next()).getId();
                long slave1 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
                long slave2 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
                AdmDNSManager dnsMan = (AdmDNSManager) admAccount.getId().findChild("adnsmanager").get();
                try {
                    long zoneId = dnsMan.addDNSZone(serviceZone, zoneAdmin, false, master, slave1, slave2);
                    outOK();
                    try {
                        dnsMan.FM_addAlias(zoneId, aliasPrefix, aliasTag);
                        Session.setUser(oldUser);
                        Session.setAccount(oldAcc);
                        return;
                    } catch (Exception ex) {
                        outFail(ex.getMessage(), ex);
                        throw ex;
                    }
                } catch (Exception ex2) {
                    outFail(ex2.getMessage());
                    throw ex2;
                }
            } catch (Exception e) {
                Session.setUser(oldUser);
                Session.setAccount(oldAcc);
                return;
            } catch (Throwable th) {
                Session.setUser(oldUser);
                Session.setAccount(oldAcc);
                throw th;
            }
        }
        outMessage("Unable to get Administrative account\n");
    }

    public void clearUpUser(Element reseller) throws Exception {
        String login = reseller.getAttributes().getNamedItem("login").getNodeValue();
        if (this.muc != null) {
            MigratedUser u = this.muc.getUser(login);
            u.clearErrors();
        }
        User resell = User.getUser(login);
        Session.setUser(resell);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT username FROM users WHERE reseller_id = ?");
            ps.setLong(1, resell.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    User tu = User.getUser(rs.getString("username"));
                    Session.setUser(tu);
                    Iterator i = tu.getAccountIds().iterator();
                    while (i.hasNext()) {
                        ResourceId aId = (ResourceId) i.next();
                        Account ta = tu.getAccount(aId);
                        Session.setAccount(tu.getAccount(aId));
                        PreparedStatement ps2 = null;
                        try {
                            ps2 = con.prepareStatement("DELETE FROM user_account WHERE user_id = ? AND account_id = ?");
                            ps2.setLong(1, tu.getId());
                            ps2.setLong(2, ta.getId().getId());
                            ta.delete(true);
                            ps2.executeUpdate();
                            Session.closeStatement(ps2);
                        } catch (Throwable th) {
                            Session.closeStatement(ps2);
                            throw th;
                            break;
                        }
                    }
                    tu.delete();
                } catch (Exception ex) {
                    outMessage("Troubles during clearup " + ex.getMessage() + "\n");
                }
            }
            Session.closeStatement(ps);
            con.close();
            Connection con2 = Session.getDb();
            Session.setUser(resell);
            Iterator i2 = resell.getAccountIds().iterator();
            while (i2.hasNext()) {
                try {
                    ResourceId aId2 = (ResourceId) i2.next();
                    Account ta2 = resell.getAccount(aId2);
                    Session.setAccount(resell.getAccount(aId2));
                    PreparedStatement ps22 = con2.prepareStatement("DELETE FROM user_account WHERE user_id = ? AND account_id = ?");
                    ps22.setLong(1, resell.getId());
                    ps22.setLong(2, ta2.getId().getId());
                    ta2.delete(true);
                    ps22.executeUpdate();
                    Session.closeStatement(ps22);
                } finally {
                    con2.close();
                }
            }
            resell.delete();
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    public Plan createSystemPlan(Account a, Element szone) throws Exception {
        String serviceZone = szone.getAttributes().getNamedItem("name").getNodeValue();
        NodeList tmp = szone.getElementsByTagName("instantalias");
        Element alias = tmp.getLength() > 0 ? (Element) tmp.item(0) : null;
        if (alias == null) {
            outMessage("No instant alias defenition found.\n");
            return null;
        }
        String aliasPrefix = alias.getAttributes().getNamedItem("prefix").getNodeValue();
        String aliasTag = alias.getAttributes().getNamedItem("tag").getNodeValue();
        return SetupCP.createSystemPlan("System plan", aliasTag, aliasPrefix, serviceZone);
    }
}
