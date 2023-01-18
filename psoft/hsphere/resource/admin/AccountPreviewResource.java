package psoft.hsphere.resource.admin;

import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.design.SessionDesign;
import psoft.util.freemarker.HtmlEncodedHashListScalar;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplatePair;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AccountPreviewResource.class */
public class AccountPreviewResource extends Resource {
    public AccountPreviewResource(int type, Collection initValue) throws Exception {
        super(type, initValue);
    }

    public AccountPreviewResource(ResourceId rid) throws Exception {
        super(rid);
    }

    protected TemplateList getUsers(long id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT users.username, users.id FROM users, user_account WHERE users.id = user_account.user_id AND user_account.account_id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                list.add((TemplateModel) new TemplatePair(rs.getString(1), rs.getString(2)));
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_display(long id, String template) throws Exception {
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        long oldResellerId = Session.getResellerId();
        try {
            SessionDesign sd = Session.getDesign();
            String oldDesignId = sd.getDesignId();
            Account newAccount = (Account) Account.get(new ResourceId(id, 0));
            Session.setAccount(newAccount);
            if (oldResellerId != 1 && oldResellerId != newAccount.getResellerId()) {
                throw new HSUserException("accountpreview.invalid_account");
            }
            User newUser = newAccount.getUser();
            SessionDesign sd2 = Session.getDesign();
            sd2.enforceDesign(oldDesignId);
            if (newUser != null) {
                Session.setUser(newUser);
            }
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            TemplateModelRoot root = Session.getModelRoot();
            root.put("account", new HtmlEncodedHashListScalar(newAccount));
            root.put("users", new HtmlEncodedHashListScalar((TemplateListModel) getUsers(id)));
            root.put("toolbox", HsphereToolbox.toolbox);
            Session.getTemplate("/account_preview/" + template).process(root, out);
            out.close();
            TemplateString templateString = new TemplateString(sw.toString());
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            return templateString;
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_displayDeleted(long id, String template) throws Exception {
        Account oldAccount = Session.getAccount();
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        TemplateModelRoot root = Session.getModelRoot();
        root.put("account", oldAccount);
        root.put("toolbox", HsphereToolbox.toolbox);
        root.put("accountId", new TemplateString(id));
        Session.getTemplate("/account_preview/" + template).process(root, out);
        out.close();
        return new TemplateString(sw.toString());
    }
}
