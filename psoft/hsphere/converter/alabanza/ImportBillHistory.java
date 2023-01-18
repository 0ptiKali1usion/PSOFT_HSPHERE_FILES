package psoft.hsphere.converter.alabanza;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/ImportBillHistory.class */
public class ImportBillHistory extends C0004CP {
    protected List pairs = new LinkedList();
    protected List source_path = new LinkedList();
    protected String billName;

    public static void main(String[] argv) throws Exception {
        ImportBillHistory test = new ImportBillHistory();
        test.m26go();
        System.exit(0);
    }

    /* renamed from: go */
    protected void m26go() throws Exception {
        getSettings();
        getPairs();
        setBillingHistory();
    }

    protected void getSettings() {
        System.out.println("Get settings from xml...");
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource("bill_config.xml"));
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node oneNode = list.item(i);
                short nodeType = oneNode.getNodeType();
                if (nodeType != 3) {
                    String nodeName = oneNode.getNodeName();
                    String nodeValue = oneNode.getAttributes().getNamedItem("name").getNodeValue();
                    if (nodeName.equals("source")) {
                        this.source_path.add(nodeValue);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Session.getLog().error("Class variables settings failed!" + e.toString());
        }
        this.billName = this.config.getString("BILL_NAME");
        System.out.println("Settings reading finished.");
    }

    protected void getPairs() throws Exception {
        System.out.println("Get account ids and logins from H-Sphere database...");
        List accountsIds = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ResultSet rs = con.prepareStatement("select id from accounts").executeQuery();
                while (rs.next()) {
                    long account_id = rs.getLong("id");
                    accountsIds.add(new Long(account_id));
                }
                rs.close();
                ps = con.prepareStatement("select user_id from user_account where account_id = ?");
                int size = accountsIds.size();
                for (int i = 0; i < size; i++) {
                    long tmpId = ((Long) accountsIds.get(i)).longValue();
                    ps.setLong(1, tmpId);
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next()) {
                        long user_id = rs2.getLong("user_id");
                        User tmpUser = User.getUser(user_id);
                        String tmpLogin = tmpUser.getLogin();
                        Hashtable acc_login = new Hashtable();
                        acc_login.put("accountId", new Long(tmpId));
                        acc_login.put("user_login", tmpLogin);
                        acc_login.put("user_obj", tmpUser);
                        this.pairs.add(acc_login);
                    }
                    rs2.close();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().error("Error getting accounts", e);
                e.printStackTrace();
                Session.closeStatement(ps);
                con.close();
            }
            System.out.println("Account ids and logins reading finished.");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void setBillingHistory() throws Exception {
        System.out.println("Set billing history...");
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        for (int p = 0; p < this.source_path.size(); p++) {
            String path_source = (String) this.source_path.get(p);
            try {
                DOMParser parser = new DOMParser();
                parser.parse(new InputSource(path_source));
                Document doc = parser.getDocument();
                Element root = doc.getDocumentElement();
                NodeList list = root.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    Node oneNode = list.item(i);
                    short nodeType = oneNode.getNodeType();
                    if (nodeType != 3) {
                        String nodeName = oneNode.getNodeName();
                        String nodeValue = oneNode.getAttributes().getNamedItem("login").getNodeValue();
                        boolean isInHash = false;
                        long account_id = 0;
                        User tmp_user = Session.getUser();
                        int j = 0;
                        int size = this.pairs.size();
                        while (true) {
                            if (j >= size) {
                                break;
                            }
                            Hashtable tmp = (Hashtable) this.pairs.get(j);
                            if (!tmp.containsValue(nodeValue)) {
                                j++;
                            } else {
                                account_id = ((Long) tmp.get("accountId")).longValue();
                                tmp_user = (User) tmp.get("user_obj");
                                isInHash = true;
                                break;
                            }
                        }
                        if (nodeName.equals(FMACLManager.USER) && isInHash) {
                            System.out.println("  for user with login " + nodeValue);
                            Session.setUser(tmp_user);
                            Account tmpAcount = (Account) Account.get(new ResourceId(account_id, 0));
                            Session.setAccount(tmpAcount);
                            Timestamp now = Session.getAccount().getCreated();
                            Bill bill = new Bill(this.billName, account_id, now);
                            ResourceId res_id = Session.getAccount().getId();
                            NodeList entries = oneNode.getChildNodes();
                            int length = entries.getLength();
                            for (int j2 = 0; j2 < length; j2++) {
                                Node oneEntry = entries.item(j2);
                                short oneNodeType = oneEntry.getNodeType();
                                if (oneNodeType != 3) {
                                    String descr_domain = oneEntry.getAttributes().getNamedItem("domain").getNodeValue();
                                    if ("".equals(descr_domain)) {
                                        descr_domain = "-";
                                    }
                                    String descr_package = oneEntry.getAttributes().getNamedItem("package").getNodeValue();
                                    if ("".equals(descr_package)) {
                                        descr_package = "-";
                                    }
                                    String descr_type = oneEntry.getAttributes().getNamedItem("type").getNodeValue();
                                    if ("".equals(descr_type)) {
                                        descr_type = "-";
                                    }
                                    String descr_date = oneEntry.getAttributes().getNamedItem("date").getNodeValue();
                                    if ("".equals(descr_date)) {
                                        descr_date = "-";
                                    }
                                    String descr_notes = oneEntry.getAttributes().getNamedItem("notes").getNodeValue();
                                    if ("".equals(descr_notes)) {
                                        descr_notes = "-";
                                    }
                                    String descr_startdate = oneEntry.getAttributes().getNamedItem("startdate").getNodeValue();
                                    if ("".equals(descr_startdate)) {
                                        descr_startdate = "-";
                                    }
                                    String descr_unitprice = oneEntry.getAttributes().getNamedItem("unitprice").getNodeValue();
                                    if ("".equals(descr_unitprice)) {
                                        descr_unitprice = "-";
                                    }
                                    String descr_length = oneEntry.getAttributes().getNamedItem("length").getNodeValue();
                                    if ("".equals(descr_length)) {
                                        descr_length = "-";
                                    }
                                    String descr_discount = oneEntry.getAttributes().getNamedItem("discount").getNodeValue();
                                    if ("".equals(descr_discount)) {
                                        descr_discount = "-";
                                    }
                                    String descr_credit = oneEntry.getAttributes().getNamedItem("credit").getNodeValue();
                                    if ("".equals(descr_credit)) {
                                        descr_credit = "-";
                                    }
                                    String descr_debit = oneEntry.getAttributes().getNamedItem("debit").getNodeValue();
                                    if ("".equals(descr_debit)) {
                                        descr_debit = "-";
                                    }
                                    String descr_balance = oneEntry.getAttributes().getNamedItem("balance").getNodeValue();
                                    if ("".equals(descr_balance)) {
                                        descr_balance = "-";
                                    }
                                    String description = descr_domain + "|" + descr_package + "|" + descr_type + "|" + descr_date + "|" + descr_notes + "|" + descr_startdate + "|" + descr_unitprice + "|" + descr_length + "|" + descr_discount + "|" + descr_credit + "|" + descr_debit + "|" + descr_balance;
                                    System.out.println("    entry description = " + description);
                                    bill.addEntry(1, now, res_id, description, now, now, null, 0.0d);
                                }
                            }
                            bill.close();
                        }
                    }
                }
            } catch (Exception e) {
                Session.getLog().debug("Error parsing xml", e);
                e.printStackTrace();
            }
        }
        Session.setAccount(oldAccount);
        Session.setUser(oldUser);
    }
}
