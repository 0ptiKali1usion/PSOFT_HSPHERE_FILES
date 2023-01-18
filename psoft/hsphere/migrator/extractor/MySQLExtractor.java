package psoft.hsphere.migrator.extractor;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.hsphere.resource.mysql.UserPrivileges;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/MySQLExtractor.class */
public class MySQLExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(MySQLExtractor.class.getName());
    private boolean force;

    public MySQLExtractor(Document doc, boolean force) {
        super(doc);
        this.force = force;
    }

    public Element getAllMySQLInfoAsXML(Account account) throws Exception {
        return getMySQL(account);
    }

    private Element getMySQL(Account account) throws Exception {
        Element node = createNode("mysql");
        MySQLResource mysql = null;
        try {
            ResourceId rid = account.getId().findChild("MySQL");
            if (rid != null) {
                mysql = (MySQLResource) rid.get();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (mysql != null) {
            getMySQLDatabases(node, mysql);
            getMySQLUsers(node, mysql);
        }
        return checkChildren(node);
    }

    private void getMySQLUsers(Element parent, MySQLResource mysql) throws Exception {
        for (ResourceId resourceId : mysql.getId().findChildren("MySQLUser")) {
            MySQLUser user = (MySQLUser) resourceId.get();
            appendChildNode(parent, getMySQLUser(mysql, user));
        }
    }

    private Element getMySQLUser(MySQLResource mysql, MySQLUser user) throws Exception {
        Element node = createNode("mysqluser");
        try {
            node.setAttributeNode(createAttribute("login", user.get("name").toString()));
            node.setAttributeNode(createAttribute("password", user.get("password").toString()));
            getUserGrant(node, mysql, user);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private void getUserGrant(Element parent, MySQLResource mysql, MySQLUser user) throws Exception {
        StringBuffer grant = new StringBuffer();
        try {
            for (ResourceId resourceId : mysql.getId().findChildren("MySQLDatabase")) {
                MySQLDatabase db = (MySQLDatabase) resourceId.get();
                if (db != null) {
                    user.FM_loadUserPrivilegesOn(db.getName());
                    UserPrivileges userPriv = user.getUserPrivileges();
                    Hashtable ht = userPriv.getDatabasePrivileges();
                    int count = 0;
                    Enumeration e = ht.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = (String) ht.get(key);
                        if (value.equals("Y")) {
                            if (count > 0) {
                                grant.append("," + key);
                            } else {
                                grant.append(key);
                            }
                            count++;
                        }
                    }
                    if (count > 0) {
                        Element node = createNode("grant");
                        String privileges = grant.toString();
                        node.setAttributeNode(createAttribute("privileges", privileges));
                        node.setAttributeNode(createAttribute("on", db.getName()));
                        appendChildNode(parent, node);
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
    }

    private void getMySQLDatabases(Element parent, MySQLResource mysql) throws Exception {
        for (ResourceId resourceId : mysql.getId().findChildren("MySQLDatabase")) {
            MySQLDatabase db = (MySQLDatabase) resourceId.get();
            appendChildNode(parent, getMySQLDatabase(db));
        }
    }

    private Element getMySQLDatabase(MySQLDatabase db) throws Exception {
        Element node = createNode("mysqldatabase");
        try {
            node.setAttributeNode(createAttribute("name", db.getName()));
            node.setAttributeNode(createAttribute("description", db.getDescription()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }
}
