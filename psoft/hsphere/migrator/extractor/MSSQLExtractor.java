package psoft.hsphere.migrator.extractor;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.mssql.MSSQLDatabase;
import psoft.hsphere.resource.mssql.MSSQLLogin;
import psoft.hsphere.resource.mssql.MSSQLResource;
import psoft.hsphere.resource.mssql.MSSQLUser;
import psoft.util.freemarker.ListAdapter;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/MSSQLExtractor.class */
public class MSSQLExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(MSSQLExtractor.class.getName());
    private boolean force;

    public MSSQLExtractor(Document doc, boolean force) {
        super(doc);
        this.force = force;
    }

    public Element getAllMSSQLInfoAsXML(Account account) throws Exception {
        return getMSSQL(account);
    }

    private Element getMSSQL(Account account) throws Exception {
        Element node = createNode("mssql");
        MSSQLResource mssql = null;
        try {
            ResourceId rid = account.getId().findChild("MSSQL");
            if (rid != null) {
                mssql = (MSSQLResource) rid.get();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (mssql != null) {
            getMSSQLDatabases(node, mssql);
            getMSSQLLogins(node, mssql);
        }
        return checkChildren(node);
    }

    private void getMSSQLDatabases(Element parent, MSSQLResource mssql) throws Exception {
        for (ResourceId resourceId : mssql.getId().findChildren("MSSQLDatabase")) {
            MSSQLDatabase db = (MSSQLDatabase) resourceId.get();
            appendChildNode(parent, getMSSQLDatabase(db));
        }
    }

    private Element getMSSQLDatabase(MSSQLDatabase db) throws Exception {
        Element node = createNode("mssqldatabase");
        try {
            node.setAttributeNode(createAttribute("name", db.getName()));
            node.setAttributeNode(createAttribute("owner", db.get("login_name").toString()));
            node.setAttributeNode(createAttribute("quota", db.get("quota_perc").toString()));
            ListAdapter adapter = db.FM_getChildren("MSSQLUser");
            while (adapter.hasNext()) {
                MSSQLUser user = (MSSQLUser) adapter.next().get();
                appendChildNode(node, getUser(user));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getUser(MSSQLUser user) throws Exception {
        Element usr = createNode("mssqluser");
        try {
            ResourceId loginId = new ResourceId(user.get("login").toString());
            MSSQLLogin login = new MSSQLLogin(loginId);
            usr.setAttributeNode(createAttribute("name", user.getName()));
            usr.setAttributeNode(createAttribute("login", login.getName()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(usr);
    }

    private void getMSSQLLogins(Element parent, MSSQLResource mssql) throws Exception {
        for (ResourceId resourceId : mssql.getId().findChildren("MSSQLLogin")) {
            MSSQLLogin login = (MSSQLLogin) resourceId.get();
            appendChildNode(parent, getMSSQLLogin(login));
        }
    }

    private Element getMSSQLLogin(MSSQLLogin login) throws Exception {
        Element node = createNode("mssqllogin");
        try {
            node.setAttributeNode(createAttribute("login", login.get("name").toString()));
            node.setAttributeNode(createAttribute("password", login.get("password").toString()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }
}
