package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.resource.HTTPAuth;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/GeneralInfo.class */
public class GeneralInfo {
    public static boolean parseGeneralInfo(URL url, Document doc, Element root, Document billDoc, Element billRoot, List traffic) {
        boolean result;
        BufferedInputStream in;
        Hashtable generalInfo;
        String userInfoLink;
        List domainsList;
        String strUrl = url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            generalInfo = MigrationProvider.getClientDomains(in);
            userInfoLink = (String) generalInfo.get("clientURL");
            domainsList = (List) generalInfo.get("domainList");
        } catch (Exception e) {
            System.out.println("Some errors happened during migration process. Account creation failed!\n");
            AlabanzaConfig.getLog().error("Some errors occured in GeneralInfo.parseGeneralInfo()", e);
            result = false;
        }
        if (domainsList.isEmpty() && AlabanzaConfig.systemGetNoDomain.booleanValue()) {
            in.close();
            System.out.println("Reading empty account finished.\n\n");
            result = true;
        } else {
            if (!AlabanzaConfig.systemUseSeparateAccs.booleanValue()) {
                try {
                    getOneAccountPerUser(doc, root, billDoc, billRoot, traffic, generalInfo, domainsList, userInfoLink, strUrl);
                    result = true;
                } catch (Exception e2) {
                    System.out.println("Some errors happened during migration process. Account creation failed!\n");
                    AlabanzaConfig.getLog().error("Some errors occured in GeneralInfo.parseGeneralInfo()", e2);
                    result = false;
                }
                in.close();
                System.out.println("Finished reading general client info.\n\n");
            } else {
                try {
                    getOneAccountPerDomain(doc, root, billDoc, billRoot, traffic, generalInfo, domainsList, userInfoLink, strUrl);
                    result = true;
                } catch (Exception e3) {
                    System.out.println("Some errors happened during migration process. Account creation failed!\n");
                    AlabanzaConfig.getLog().error("Some errors occured in GeneralInfo.parseGeneralInfo()", e3);
                    result = false;
                }
                in.close();
                System.out.println("Finished reading general client info.\n\n");
            }
            System.out.println("Some errors happened during migration process. Account creation failed!\n");
            AlabanzaConfig.getLog().error("Some errors occured in GeneralInfo.parseGeneralInfo()", e);
            result = false;
            return result;
        }
        return result;
    }

    private static Element getOneAccountPerUser(Document doc, Element root, Document billDoc, Element billRoot, List traffic, Hashtable generalInfo, List domainsList, String userInfoLink, String strUrl) throws Exception {
        String userLogin = new String();
        String userPassword = new String();
        List resDomains = new LinkedList();
        boolean resCP = false;
        Element user = doc.createElement(FMACLManager.USER);
        Element account = doc.createElement("account");
        Element billUser = billDoc.createElement(FMACLManager.USER);
        if (generalInfo.containsKey("planName")) {
            account.setAttribute("plan", (String) generalInfo.get("planName"));
        }
        if (generalInfo.containsKey("billingPeriod")) {
            String tmpbpid = (String) generalInfo.get("billingPeriod");
            String bpid = AlabanzaConfig.getBillPeriodValue(tmpbpid);
            if (bpid != null) {
                account.setAttribute("bpid", bpid);
            }
        }
        if (generalInfo.containsKey("startDate")) {
            account.setAttribute("startdate", (String) generalInfo.get("startDate"));
        }
        if (generalInfo.containsKey("suspended")) {
            account.setAttribute("suspended", "1");
        }
        if (generalInfo.containsKey("balance")) {
            String tmpBalance = (String) generalInfo.get("balance");
            float tmpFloatBalance = Float.valueOf(tmpBalance).floatValue();
            if (tmpFloatBalance != 0.0f) {
                account.setAttribute("balance", tmpBalance);
            }
        }
        Element mySQL = doc.createElement("mysql");
        if (!domainsList.isEmpty()) {
            Object[] domainsArray = domainsList.toArray();
            for (Object obj : domainsArray) {
                System.out.println("Started reading domain info...");
                Hashtable oneDomain = (Hashtable) obj;
                if (!oneDomain.isEmpty()) {
                    userLogin = (String) oneDomain.get("login");
                    userPassword = (String) oneDomain.get("password");
                    Element domain = doc.createElement("domain");
                    String domainName = (String) oneDomain.get("domainName");
                    domain.setAttribute("name", domainName);
                    String domainIP = (String) oneDomain.get("IP");
                    if (AlabanzaConfig.systemGetIP.booleanValue()) {
                        domain.setAttribute("ip", domainIP);
                    }
                    String urlDomainData = (String) oneDomain.get("editPackage");
                    DomainInfo.parseDomainData(urlDomainData, doc, domain, userLogin, userPassword, traffic);
                    String cpLink = AlabanzaConfig.systemHttpPrefix + domainIP + AlabanzaConfig.systemCPSuffix;
                    Authenticator.setDefault(new HTTPAuth(userLogin, userPassword));
                    URL urlCP = new URL(cpLink);
                    System.out.println("Control panel link: " + cpLink);
                    if (!ControlPanel.parseControlPanel(urlCP, doc, mySQL, domain, domainName, domainIP, userLogin, userPassword, traffic)) {
                        System.out.println("Reading control panel failed. Domain name = " + domainName + ". DomainIP = " + domainIP);
                        AlabanzaConfig.getLog().warn("Reading control panel failed. Domain name = " + domainName + ". DomainIP = " + domainIP);
                    }
                    resCP = true;
                    account.appendChild(domain);
                }
            }
            user.setAttribute("login", userLogin);
            if (AlabanzaConfig.systemUseBill.booleanValue()) {
                billUser.setAttribute("login", userLogin);
            }
            System.out.println("User login: " + userLogin);
            user.setAttribute("password", userPassword);
        }
        if (mySQL.hasChildNodes()) {
            account.appendChild(mySQL);
        }
        Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
        URL urlDetail = new URL(AlabanzaConfig.systemPrefix + userInfoLink);
        System.out.println("User detail info link: " + AlabanzaConfig.systemPrefix + userInfoLink);
        boolean resDetailInfo = DetailInfo.parseDetailInfo(urlDetail, doc, billDoc, user, billUser, account, traffic);
        if (generalInfo.containsKey("billingHistoryURL") && AlabanzaConfig.systemUseBill.booleanValue()) {
            String billLink = (String) generalInfo.get("billingHistoryURL");
            Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
            int countBill = 0;
            while (1 != 0) {
                countBill++;
                URL urlBilling = new URL(AlabanzaConfig.systemPrefix + billLink + "&exPageNO=" + String.valueOf(countBill));
                System.out.println("User billing history link: " + AlabanzaConfig.systemPrefix + billLink + "&exPageNO=" + String.valueOf(countBill));
                if (!BillingHistory.parseBillingHistory(urlBilling, billDoc, billUser, traffic)) {
                    break;
                }
            }
            billRoot.appendChild(billUser);
            System.out.println("Finished reading billing history.\n");
        }
        if ((resDomains.isEmpty() && resCP && resDetailInfo && !userLogin.equals("")) || (userLogin.equals("") && resDetailInfo)) {
            user.appendChild(account);
            root.appendChild(user);
            if (AlabanzaConfig.systemUseBill.booleanValue()) {
                billRoot.appendChild(billUser);
            }
        } else {
            System.out.println("Some errors happened during migration process. Account creation failed!\n");
            AlabanzaConfig.getLog().warn("Account with link " + strUrl + " creation failed.");
        }
        return root;
    }

    private static Element getOneAccountPerDomain(Document doc, Element root, Document billDoc, Element billRoot, List traffic, Hashtable generalInfo, List domainsList, String userInfoLink, String strUrl) throws Exception {
        String userLogin = new String();
        String userPassword = new String();
        Element user = doc.createElement(FMACLManager.USER);
        Element billUser = billDoc.createElement(FMACLManager.USER);
        if (!domainsList.isEmpty()) {
            Object[] domainsArray = domainsList.toArray();
            for (Object obj : domainsArray) {
                System.out.println("Started reading domain info...");
                Hashtable oneDomain = (Hashtable) obj;
                if (!oneDomain.isEmpty()) {
                    Element account = doc.createElement("account");
                    Element mySQL = doc.createElement("mysql");
                    userLogin = (String) oneDomain.get("login");
                    userPassword = (String) oneDomain.get("password");
                    if (generalInfo.containsKey("planName")) {
                        account.setAttribute("plan", (String) oneDomain.get("planName"));
                    }
                    if (generalInfo.containsKey("billingPeriod")) {
                        String tmpbpid = (String) oneDomain.get("billingPeriod");
                        String bpid = AlabanzaConfig.getBillPeriodValue(tmpbpid);
                        if (bpid != null) {
                            account.setAttribute("bpid", bpid);
                        }
                    }
                    if (generalInfo.containsKey("startDate")) {
                        account.setAttribute("startdate", (String) oneDomain.get("startDate"));
                    }
                    if (generalInfo.containsKey("suspended")) {
                        account.setAttribute("suspended", "1");
                    }
                    if (generalInfo.containsKey("balance")) {
                        String tmpBalance = (String) generalInfo.get("balance");
                        float tmpFloatBalance = Float.valueOf(tmpBalance).floatValue();
                        if (tmpFloatBalance != 0.0f) {
                            account.setAttribute("balance", tmpBalance);
                        }
                    }
                    Element domain = doc.createElement("domain");
                    String domainName = (String) oneDomain.get("domainName");
                    domain.setAttribute("name", domainName);
                    String domainIP = (String) oneDomain.get("IP");
                    if (AlabanzaConfig.systemGetIP.booleanValue()) {
                        domain.setAttribute("ip", domainIP);
                    }
                    String urlDomainData = AlabanzaConfig.systemPrefix + ((String) oneDomain.get("editPackage"));
                    DomainInfo.parseDomainData(urlDomainData, doc, domain, userLogin, userPassword, traffic);
                    String cpLink = AlabanzaConfig.systemHttpPrefix + domainIP + AlabanzaConfig.systemCPSuffix;
                    Authenticator.setDefault(new HTTPAuth(userLogin, userPassword));
                    URL urlCP = new URL(cpLink);
                    System.out.println("Control panel link: " + cpLink);
                    boolean resCP = ControlPanel.parseControlPanel(urlCP, doc, mySQL, domain, domainName, domainIP, userLogin, userPassword, traffic);
                    if (!resCP) {
                        System.out.println("Reading control panel failed. Domain name = " + domainName + ". DomainIP = " + domainIP);
                        AlabanzaConfig.getLog().warn("Reading control panel failed. Domain name = " + domainName + ". DomainIP = " + domainIP);
                    }
                    account.appendChild(domain);
                    if (mySQL.hasChildNodes()) {
                        account.appendChild(mySQL);
                    }
                    Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
                    String infoLink = AlabanzaConfig.systemPrefix + userInfoLink;
                    URL urlDetail = new URL(infoLink);
                    System.out.println("User detail info link: " + infoLink);
                    DetailInfo.parseDetailInfo(urlDetail, doc, billDoc, user, billUser, account, traffic);
                    user.appendChild(account);
                }
            }
            user.setAttribute("login", userLogin);
            if (AlabanzaConfig.systemUseBill.booleanValue()) {
                billUser.setAttribute("login", userLogin);
            }
            System.out.println("User login: " + userLogin);
            user.setAttribute("password", userPassword);
        }
        if (generalInfo.containsKey("billingHistoryURL") && AlabanzaConfig.systemUseBill.booleanValue()) {
            String billLink = (String) generalInfo.get("billingHistoryURL");
            Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
            int countBill = 0;
            while (1 != 0) {
                countBill++;
                URL urlBilling = new URL(AlabanzaConfig.systemPrefix + billLink + "&exPageNO=" + String.valueOf(countBill));
                System.out.println("User billing history link: " + AlabanzaConfig.systemPrefix + billLink + "&exPageNO=" + String.valueOf(countBill));
                if (!BillingHistory.parseBillingHistory(urlBilling, billDoc, billUser, traffic)) {
                    break;
                }
            }
            billRoot.appendChild(billUser);
            System.out.println("Finished reading billing history.\n");
        }
        root.appendChild(user);
        if (AlabanzaConfig.systemUseBill.booleanValue()) {
            billRoot.appendChild(billUser);
        }
        return root;
    }
}
