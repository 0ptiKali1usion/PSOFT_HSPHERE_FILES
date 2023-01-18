package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import psoft.hsphere.Bill;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.plan.ResourceType;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/BillViewer.class */
public class BillViewer extends Resource {
    public BillViewer(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public BillViewer(ResourceId id) throws Exception {
        super(id);
    }

    public TemplateModel FM_view(long id) throws Exception {
        return Session.getAccount().getBill(id);
    }

    public TemplateModel FM_list() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, opened, closed, description, amount FROM bill WHERE account_id = ? ORDER BY id DESC");
            long accountId = Session.getAccount().getId().getId();
            ps.setLong(1, accountId);
            TemplateList list = new TemplateList();
            try {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    TemplateMap map = new TemplateMap();
                    long id = rs.getLong(1);
                    String description = rs.getString(4);
                    map.put("id", String.valueOf(id));
                    map.put("from", rs.getDate(2));
                    map.put("to", rs.getDate(3));
                    map.put("description", Bill.getDescription(description, id));
                    map.put("desc_plan", Bill.getPlanDescription(accountId, description));
                    map.put("desc_short", Bill.getShortDescription(accountId, id));
                    map.put("amount", rs.getString(5));
                    list.add((TemplateModel) map);
                }
            } catch (SQLException se) {
                getLog().warn("Unable to get old bills ", se);
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

    public TemplateModel FM_getDomainTransferPrice(String tld) throws Exception {
        ResourceType rt = Session.getAccount().getPlan().getResourceType(TypeRegistry.getTypeId("domain_transfer"));
        return new TemplateString(DomainRegistrar.getTransferPrice(tld, rt));
    }

    public TemplateModel FM_getTLDPrices(String tld) throws Exception {
        Plan plan = Session.getAccount().getPlan();
        ResourceType rt = plan.getResourceType(TypeRegistry.getTypeId("opensrs"));
        return new TemplateMap(DomainRegistrar.getTLDPrices(tld, rt));
    }
}
