package psoft.hsphere.migrator.creator;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.pgsql.PGSQLResource;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/PostgreSQLCreator.class */
public class PostgreSQLCreator {
    private static Category log = Category.getInstance(PostgreSQLCreator.class.getName());
    protected Account account;
    protected CommonUserCreator creator;

    public PostgreSQLCreator(Account account, CommonUserCreator creator) {
        this.account = null;
        this.creator = null;
        this.account = account;
        this.creator = creator;
    }

    public void addPostgreSQL(Element pgsql) throws Exception {
        ResourceId pgsqlId;
        if (this.account.FM_getChild("pgsql") == null) {
            pgsqlId = this.account.addChild("pgsql", "", null);
        } else {
            pgsqlId = this.account.FM_getChild("pgsql");
        }
        PGSQLResource res = (PGSQLResource) pgsqlId.get();
        addPGSQLUsers(res, pgsql);
        addPGSQLDatabases(res, pgsql);
    }

    private void addPGSQLUsers(PGSQLResource res, Element pgsql) throws Exception {
        NodeList users = pgsql.getElementsByTagName("pgsqluser");
        for (int i = 0; i < users.getLength(); i++) {
            Element user = (Element) users.item(i);
            addPGSQLUser(res, user);
        }
    }

    private void addPGSQLUser(PGSQLResource res, Element pgsql) throws Exception {
        List values = new ArrayList();
        try {
            if (res.FM_isUserExist(null) != null) {
                throw new Exception("User exists: " + ((String) null));
            }
            String name = pgsql.getAttributes().getNamedItem("name").getNodeValue();
            String password = pgsql.getAttributes().getNamedItem("password").getNodeValue();
            values.add(name);
            values.add(password);
            this.creator.outMessage("Adding PGSQL user - " + name);
            res.addChild("pgsqluser", "", values);
            this.creator.outOK();
        } catch (Exception ex) {
            this.creator.outFail("Failed to create PGSQL user - " + ((String) null), ex);
            Session.getLog().error("Failed to create PGSQL user - " + ((String) null), ex);
            throw ex;
        }
    }

    private void addPGSQLDatabases(PGSQLResource res, Element pgsql) throws Exception {
        NodeList databases = pgsql.getElementsByTagName("pgsqldatabase");
        for (int i = 0; i < databases.getLength(); i++) {
            Element database = (Element) databases.item(i);
            addPGSQLDatabase(res, database);
        }
    }

    private void addPGSQLDatabase(PGSQLResource res, Element database) throws Exception {
        List values = new ArrayList();
        String name = null;
        try {
            name = database.getAttributes().getNamedItem("name").getNodeValue();
            String description = database.getAttributes().getNamedItem("description").getNodeValue();
            String owner = database.getAttributes().getNamedItem("owner").getNodeValue();
            values.add(name);
            values.add(description);
            values.add(owner);
            this.creator.outMessage("Adding PGSQL database " + name);
            res.addChild("pgsqldatabase", "", values).get();
            this.creator.outOK();
        } catch (Exception ex) {
            this.creator.outFail("Failed to create PGSQL database - " + name, ex);
            Session.getLog().error("Failed to create PGSQL database - " + name, ex);
            throw ex;
        }
    }
}
