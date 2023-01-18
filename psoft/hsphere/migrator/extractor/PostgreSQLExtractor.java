package psoft.hsphere.migrator.extractor;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.pgsql.PGSQLDatabase;
import psoft.hsphere.resource.pgsql.PGSQLResource;
import psoft.hsphere.resource.pgsql.PGSQLUser;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/PostgreSQLExtractor.class */
public class PostgreSQLExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(PostgreSQLExtractor.class.getName());
    private boolean force;

    public PostgreSQLExtractor(Document doc, boolean force) {
        super(doc);
        this.force = force;
    }

    public Element getAllPostgreSQLInfoAsXML(Account account) throws Exception {
        return getPostgreSQL(account);
    }

    private Element getPostgreSQL(Account account) throws Exception {
        Element node = createNode("pgsql");
        PGSQLResource pgsql = null;
        try {
            ResourceId rid = account.getId().findChild("pgsql");
            if (rid != null) {
                pgsql = (PGSQLResource) rid.get();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        if (pgsql != null) {
            getPGSQLDatabases(node, pgsql);
            getPGSQLUsers(node, pgsql);
        }
        return checkChildren(node);
    }

    private void getPGSQLUsers(Element parent, PGSQLResource pgsql) throws Exception {
        for (ResourceId resourceId : pgsql.getId().findChildren("pgsqluser")) {
            PGSQLUser user = (PGSQLUser) resourceId.get();
            appendChildNode(parent, getPGSQLUser(user));
        }
    }

    private Element getPGSQLUser(PGSQLUser user) throws Exception {
        Element node = createNode("pgsqluser");
        try {
            node.setAttributeNode(createAttribute("name", user.get("name").toString()));
            node.setAttributeNode(createAttribute("password", user.get("password").toString()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private void getPGSQLDatabases(Element parent, PGSQLResource mysql) throws Exception {
        for (ResourceId resourceId : mysql.getId().findChildren("pgsqldatabase")) {
            PGSQLDatabase db = (PGSQLDatabase) resourceId.get();
            appendChildNode(parent, getPGSQLDatabase(db));
        }
    }

    private Element getPGSQLDatabase(PGSQLDatabase db) throws Exception {
        Element node = createNode("pgsqldatabase");
        try {
            node.setAttributeNode(createAttribute("name", db.getName()));
            node.setAttributeNode(createAttribute("description", db.getDescription()));
            node.setAttributeNode(createAttribute("owner", db.getOwner()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }
}
