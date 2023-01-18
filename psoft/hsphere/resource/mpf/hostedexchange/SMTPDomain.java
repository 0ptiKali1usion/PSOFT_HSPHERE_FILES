package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/SMTPDomain.class */
public class SMTPDomain extends Resource {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int VANITY = 3;
    String name;
    int type;

    public SMTPDomain(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.name = (String) i.next();
        if (i.hasNext()) {
            this.type = Integer.parseInt((String) i.next());
        } else {
            this.type = 2;
        }
    }

    public SMTPDomain(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, domain_type FROM he_smtpdomain WHERE id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.type = rs.getInt(2);
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
            ps = con.prepareStatement("INSERT INTO he_smtpdomain (id, name, domain_type) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.name);
            ps.setInt(3, this.type);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (this.type != 1) {
                MPFManager manager = MPFManager.getManager();
                BusinessOrganization bo = (BusinessOrganization) getParent().get();
                Session.getLog().debug("LDAP: " + manager.getLDAP(bo.getName()));
                Session.getLog().debug("PDC: " + manager.getPdc());
                Session.getLog().debug("name: " + this.name);
                Session.getLog().debug("type: " + getTypeAsStirng());
                String result = manager.getHES().createSMTPDomain(manager.getLDAP(bo.getName()), manager.getPdc(), this.name, getTypeAsStirng());
                MPFManager.Result res = manager.processHeResult(result);
                if (!res.isSuccess()) {
                    throw new Exception(res.getError());
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private String getTypeAsStirng(int type) {
        switch (type) {
            case 1:
                return "PrimaryDomain";
            case 2:
                return "SecondaryDomain";
            case 3:
                return "VanityDomain";
            default:
                return null;
        }
    }

    private String getTypeAsStirng() {
        return getTypeAsStirng(this.type);
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() throws Exception {
        if (containsUser()) {
            throw new HSUserException("Cannot remove SMTP domain, because it contains user");
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized && this.type != 1) {
            MPFManager manager = MPFManager.getManager();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            String result = manager.getHES().deleteSMTPDomain(manager.getLDAP(bo.getName()), manager.getPdc(), this.name);
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_smtpdomain WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("role".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("containsUser".equals(key)) {
            try {
                return new TemplateString(containsUser());
            } catch (Exception e) {
                Session.getLog().error("Cannot find out if SMTP domain already contains user ", e);
                return new TemplateString("1");
            }
        } else if ("dns_zone".equals(key)) {
            ResourceId zoneId = null;
            try {
                zoneId = getId().findChild("dns_zone");
            } catch (Exception ex) {
                Session.getLog().error("Unable to find dns_zone", ex);
            }
            return zoneId;
        } else {
            return super.get(key);
        }
    }

    public TemplateModel FM_switchDNSRecords(String state) throws Exception {
        ResourceId zoneId = null;
        try {
            zoneId = getId().findChild("dns_zone");
            ((DNSZone) zoneId.get()).FM_cdelete(0);
        } catch (Exception ex) {
            if (zoneId != null) {
                Session.getLog().warn("Unable to get DNS Zone with Id: " + zoneId, ex);
            }
        }
        if ("ON".equals(state)) {
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            String mailServer = bo.get("dnsServerName").toString();
            ArrayList args = new ArrayList();
            args.add(this.name);
            ResourceId zone = addChild("dns_zone", "", args);
            args.clear();
            args.add(this.name);
            args.add("MX");
            args.add("86400");
            args.add(mailServer);
            args.add("10");
            zone.get().addChild("cust_dns_record", "", args);
        }
        return this;
    }

    public String getName() {
        return this.name;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) throws Exception {
        if (type != 1 && type != 2 && type != 3) {
            throw new Exception("Unknown smtp domain type");
        }
        if (type == 1) {
            MPFManager manager = MPFManager.getManager();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            String result = manager.getHES().createSMTPDomain(manager.getLDAP(bo.getName()), manager.getPdc(), this.name, getTypeAsStirng(1));
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
        }
        this.type = type;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_smtpdomain SET domain_type = ? WHERE id = ?");
            ps.setInt(1, type);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean containsUser() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) FROM he_bizuser WHERE smtp_domain = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                Session.closeStatement(ps);
                con.close();
                return true;
            }
            Session.closeStatement(ps);
            con.close();
            return false;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
