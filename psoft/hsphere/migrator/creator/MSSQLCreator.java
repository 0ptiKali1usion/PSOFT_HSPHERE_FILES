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
import psoft.hsphere.resource.mssql.MSSQLDatabase;
import psoft.hsphere.resource.mssql.MSSQLLogin;
import psoft.hsphere.resource.mssql.MSSQLResource;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/MSSQLCreator.class */
public class MSSQLCreator {
    private static Category log = Category.getInstance(MSSQLCreator.class.getName());
    protected Account account;
    protected CommonUserCreator creator;

    public MSSQLCreator(Account account, CommonUserCreator creator) {
        this.account = null;
        this.creator = null;
        this.account = account;
        this.creator = creator;
    }

    public void addMSSQL(Element mssql) throws Exception {
        ResourceId mssqlId;
        if (this.account.FM_getChild("MSSQL") == null) {
            mssqlId = this.account.addChild("MSSQL", "", null);
        } else {
            mssqlId = this.account.FM_getChild("MSSQL");
        }
        MSSQLResource res = (MSSQLResource) mssqlId.get();
        addMSSQLLogins(res, mssql);
        addMSSQLDatabases(res, mssql);
    }

    private void addMSSQLDatabases(MSSQLResource res, Element mssql) throws Exception {
        NodeList databases = mssql.getElementsByTagName("mssqldatabase");
        for (int i = 0; i < databases.getLength(); i++) {
            Element database = (Element) databases.item(i);
            addMSSQLDatabase(res, database);
        }
    }

    private void addMSSQLDatabase(MSSQLResource res, Element database) throws Exception {
        List values = new ArrayList();
        String name = null;
        try {
            name = database.getAttributes().getNamedItem("name").getNodeValue();
            String owner = database.getAttributes().getNamedItem("owner").getNodeValue();
            String quota = database.getAttributes().getNamedItem("quota").getNodeValue();
            values.add(name);
            values.add(getLoginID(res, owner));
            values.add(quota);
            this.creator.outMessage("Adding MSSQL database " + name);
            MSSQLDatabase db = (MSSQLDatabase) res.addChild("MSSQLDatabase", "", values).get();
            this.creator.outOK();
            addDatabaseUsers(res, db, database);
        } catch (Exception ex) {
            this.creator.outFail("Failed to create MSSQL database " + name, ex);
            Session.getLog().error("Failed to create MSSQL database " + name, ex);
            throw ex;
        }
    }

    private void addDatabaseUsers(MSSQLResource mssql, MSSQLDatabase db, Element database) throws Exception {
        NodeList users = database.getElementsByTagName("mssqluser");
        for (int i = 0; i < users.getLength(); i++) {
            Element user = (Element) users.item(i);
            addDatabaseUser(mssql, db, user);
        }
    }

    private void addDatabaseUser(MSSQLResource mssql, MSSQLDatabase db, Element user) throws Exception {
        List values = new ArrayList();
        String name = null;
        try {
            name = user.getAttributes().getNamedItem("name").getNodeValue();
            String login = user.getAttributes().getNamedItem("login").getNodeValue();
            values.add(name);
            values.add(getLoginID(mssql, login));
            values.add(db.getId().getAsString());
            db.addChild("MSSQLUser", "", values);
        } catch (Exception ex) {
            this.creator.outFail("Failed to create MSSQL user " + name, ex);
            Session.getLog().error("Failed to create MSSQL user " + name, ex);
            throw ex;
        }
    }

    private void addMSSQLLogins(MSSQLResource res, Element mssql) throws Exception {
        NodeList logins = mssql.getElementsByTagName("mssqllogin");
        for (int i = 0; i < logins.getLength(); i++) {
            Element login = (Element) logins.item(i);
            addMSSQLLogin(res, login);
        }
    }

    private void addMSSQLLogin(MSSQLResource res, Element mslogin) throws Exception {
        List values = new ArrayList();
        String login = null;
        try {
            login = mslogin.getAttributes().getNamedItem("login").getNodeValue();
            String password = mslogin.getAttributes().getNamedItem("password").getNodeValue();
            values.add(login);
            values.add(password);
            this.creator.outMessage("Adding MSSQL login " + login);
            res.addChild("MSSQLLogin", "", values);
            this.creator.outOK();
        } catch (Exception ex) {
            this.creator.outFail("Failed to create MSSQL login " + login, ex);
            Session.getLog().error("Failed to create MSSQL login " + login, ex);
            throw ex;
        }
    }

    private String getLoginID(MSSQLResource res, String login) throws Exception {
        ResourceId mssqlId = this.account.FM_getChild("MSSQL");
        TemplateList logins = mssqlId.FM_getChildren("MSSQLLogin");
        while (logins.hasNext()) {
            ResourceId rid = logins.next();
            MSSQLLogin log2 = new MSSQLLogin(rid);
            if (log2.getName().equals(login)) {
                return rid.getAsString();
            }
        }
        throw new Exception("MSSQL login - " + login + " not found");
    }
}
