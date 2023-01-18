package psoft.hsphere.resource.zeus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/FrontPageResource.class */
public class FrontPageResource extends VHResource implements IPDependentResource, IPDeletedResource {
    protected String login;
    protected String password;

    public FrontPageResource(int type, Collection init) throws Exception {
        super(type, init);
        Iterator i = init.iterator();
        this.login = (String) i.next();
        this.password = (String) i.next();
    }

    public FrontPageResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM frontpage WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.password = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO frontpage (id, login, password) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.login);
            ps.setString(3, this.password);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            VirtualHostingResource virtualHostingResource = (VirtualHostingResource) getParent().get();
            HostEntry he = recursiveGet("host");
            String name = recursiveGet("real_name").toString();
            List l = new ArrayList();
            l.add(recursiveGet("vserver").toString());
            l.add(this.login);
            l.add(this.password);
            l.add(name);
            if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
                l.add(getParent().get("ip").toString());
            } else {
                l.add("www." + name);
            }
            he.exec("zeus-fp-install", l);
            restart();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void update(String login, String password) throws Exception {
        VirtualHostingResource hosting = (VirtualHostingResource) getParent().get();
        hosting.FM_updateConfig();
        HostEntry he = recursiveGet("host");
        List l = new ArrayList();
        l.add(recursiveGet("vserver").toString());
        l.add(login);
        l.add(password);
        he.exec("zeus-fp-update", l);
    }

    public TemplateModel FM_update(String login, String password) throws Exception {
        update(login, password);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE frontpage SET login = ?, password = ? WHERE id = ?");
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.login = login;
            this.password = password;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void restart() throws Exception {
        VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
        vh.restart();
        Session.getLog().debug("ZEUS was restarted by Frontpage");
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("login") ? new TemplateString(this.login) : key.equals("password") ? new TemplateString(this.password) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            HostEntry he = recursiveGet("host");
            String name = recursiveGet("real_name").toString();
            List l = new ArrayList();
            l.add(recursiveGet("vserver").toString());
            l.add(name);
            if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
                l.add(getParent().get("ip").toString());
            } else {
                l.add("www." + name);
            }
            he.exec("zeus-fp-delete", l);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM frontpage WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            restart();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_fix() throws Exception {
        update(this.login, this.password);
        return this;
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        HostEntry he = recursiveGet("host");
        String name = recursiveGet("real_name").toString();
        List l = new ArrayList();
        l.add(recursiveGet("vserver").toString());
        l.add(this.login);
        l.add(this.password);
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + name);
        }
        he.exec("zeus-fp-install", l);
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        HostEntry he = recursiveGet("host");
        String name = recursiveGet("real_name").toString();
        List l = new ArrayList();
        l.add(recursiveGet("vserver").toString());
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + name);
        }
        he.exec("zeus-fp-delete", l);
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return "modules!frontpage!enabled       yes\nmodules!frontpage!fphome        /usr/local/frontpage\n";
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.frontpage.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.frontpage.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("frontpage.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
