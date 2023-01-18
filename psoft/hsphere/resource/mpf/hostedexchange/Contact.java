package psoft.hsphere.resource.mpf.hostedexchange;

import com.microsoft.provisioning.webservices.CreateContactPropertyXml;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.StringHolder;
import org.apache.axis.message.MessageElement;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.mpf.MPFManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/Contact.class */
public class Contact extends Resource {
    private String alias;
    private String proxyAddr;
    private String targetAddr;

    public Contact(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Session.getLog().debug("Contact resource creation " + initValues);
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.alias = (String) i.next();
        } else {
            this.alias = getId().toString();
        }
    }

    public Contact(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT alias, smtpAddr, targetAddr FROM he_contacts WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.alias = rs.getString(1);
                this.proxyAddr = rs.getString(2);
                this.targetAddr = rs.getString(3);
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
            ps = con.prepareStatement("INSERT INTO he_contacts (id, alias) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.alias);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            MessageElement properties = new MessageElement("", "properties");
            MessageElement property = new MessageElement("", "property");
            property.addAttribute("", "name", "cn");
            property.addTextNode(this.alias);
            properties.addChild(property);
            CreateContactPropertyXml propertyXML = new CreateContactPropertyXml();
            propertyXML.set_any(new MessageElement[]{properties});
            MPFManager manager = MPFManager.getManager();
            StringHolder result = new StringHolder();
            StringHolder errorReturn = new StringHolder();
            manager.getMADS().createContact(manager.getLDAP(bo.getName()), this.alias, propertyXML, manager.getPdc(), true, result, errorReturn);
            if (result.value == null || "".equals(result.value)) {
                throw new Exception(errorReturn.value);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void mailEnable(String proxyAddr, String targetAddr) throws Exception {
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        MPFManager manager = MPFManager.getManager();
        MessageElement data = new MessageElement("", "data");
        MessageElement tmp = new MessageElement("", "path");
        tmp.addTextNode(manager.getLDAP(bo.getName()));
        data.addChild(tmp);
        MessageElement tmp2 = new MessageElement("", "cn");
        tmp2.addTextNode(this.alias);
        data.addChild(tmp2);
        MessageElement tmp3 = new MessageElement("", "targetAddress");
        tmp3.addTextNode(targetAddr);
        data.addChild(tmp3);
        MessageElement tmp4 = new MessageElement("", "proxyAddress");
        tmp4.addTextNode(proxyAddr);
        data.addChild(tmp4);
        MPFManager.Result res = manager.executeMPSRequest("WS Exchange Provider Adapter", "MailEnableContact", data);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_contacts SET smtpAddr = ?, targetAddr = ? WHERE id=?");
            ps.setString(1, proxyAddr);
            ps.setString(2, targetAddr);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.proxyAddr = proxyAddr;
            this.targetAddr = targetAddr;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            MessageElement properties = new MessageElement("", "properties");
            MessageElement property = new MessageElement("", "property");
            property.addAttribute("", "name", "cn");
            property.addTextNode(this.alias);
            properties.addChild(property);
            CreateContactPropertyXml propertyXML = new CreateContactPropertyXml();
            propertyXML.set_any(new MessageElement[]{properties});
            MPFManager manager = MPFManager.getManager();
            BooleanHolder result = new BooleanHolder();
            StringHolder errorReturn = new StringHolder();
            manager.getMADS().deleteContact(manager.getLDAP(bo.getName(), this.alias), manager.getPdc(), true, result, errorReturn);
            if (!result.value) {
                throw new Exception(errorReturn.value);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_contacts WHERE id = ?");
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

    public String getAlias() {
        return this.alias;
    }

    public String getProxyAddr() {
        return this.proxyAddr;
    }

    public String getTargetAddr() {
        return this.targetAddr;
    }
}
