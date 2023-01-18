package psoft.hsphere.converter.alabanza;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.HTTPAuth;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailingList;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/MigrateMailingLists.class */
public class MigrateMailingLists extends C0004CP {
    public MigrateMailingLists() throws Exception {
        initLog();
        setConfig();
    }

    public static void main(String[] argv) throws Exception {
        try {
            MigrateMailingLists test = new MigrateMailingLists();
            test.m24go();
            System.out.println("Convertion finished.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* renamed from: go */
    private void m24go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users ");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String usrId = rs.getString(1);
                    getUser(usrId);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Convertion failed.");
                System.exit(0);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void getUser(String user) {
        try {
            User u = User.getUser(user);
            Session.setUser(u);
            Iterator aci = u.getAccountIds().iterator();
            while (aci.hasNext()) {
                ResourceId accountId = (ResourceId) aci.next();
                getAccount(accountId, u);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("User getting failed.");
        }
    }

    private void getAccount(ResourceId accountId, User u) {
        try {
            Account newAccount = u.getAccount(accountId);
            Session.setAccount(newAccount);
            System.out.println("Account :" + newAccount.get("description").toString());
            String userLogin = u.getLogin();
            String userPassword = u.getPassword();
            System.out.println("  User login: " + userLogin);
            Collection<ResourceId> domains = newAccount.getId().findAllChildren("mail_domain");
            for (ResourceId domainId : domains) {
                getDomain(domainId, userLogin, userPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Account getting failed.");
        }
    }

    private void getDomain(ResourceId domainId, String userLogin, String userPassword) {
        try {
            MailDomain domain = (MailDomain) domainId.get();
            String domainName = domain.getDomainName();
            System.out.println("    Domain name: " + domainName);
            Collection<ResourceId> mlists = domainId.getChildHolder().getChildrenByName("mailing_list");
            for (ResourceId mlistId : mlists) {
                getMList(mlistId, domainName, userLogin, userPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Domain getting failed.");
        }
    }

    private void getMList(ResourceId mlistId, String domainName, String userLogin, String userPassword) {
        try {
            MailingList mlist = (MailingList) mlistId.get();
            String mlistName = mlist.get("email").toString();
            System.out.println("      Mailing list name: " + mlistName);
            int dotIndex = domainName.indexOf(".");
            String domainCutName = domainName.substring(0, dotIndex);
            Authenticator.setDefault(new HTTPAuth(userLogin, userPassword));
            URL url = new URL("ftp://" + userLogin + ":" + userPassword + "@" + domainName + "/" + domainCutName + "-mail/" + mlistName + "/dist");
            System.out.println("Mailing list manager link: ftp://" + userLogin + ":" + userPassword + "@" + domainName + "/" + domainCutName + "-mail/" + mlistName + "/dist");
            parseOneMailingList(url, mlistName, mlist);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Mail list getting failed.");
        }
    }

    private void parseOneMailingList(URL url, String listName, MailingList mlist) {
        try {
            System.out.println("Waiting for reply...");
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            List addresses = new LinkedList();
            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                inputLine.trim();
                if (inputLine.indexOf(listName) == -1 && inputLine.indexOf(" ") == -1 && !inputLine.startsWith("(Only addresses below this line can be automatically removed)")) {
                    addresses.add(inputLine);
                }
            }
            int size = addresses.size();
            for (int i = 0; i < size; i++) {
                String address = (String) addresses.get(i);
                if (!"".equals(address)) {
                    System.out.println("Mailing list subscriber's email: " + address);
                    mlist.FM_subscribe(address);
                }
            }
            in.close();
            System.out.println("Finished reading one mailing list info.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
