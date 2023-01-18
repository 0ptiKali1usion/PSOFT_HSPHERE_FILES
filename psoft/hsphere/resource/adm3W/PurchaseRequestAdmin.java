package psoft.hsphere.resource.adm3W;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PurchaseRequestAdmin.class */
public class PurchaseRequestAdmin extends Resource {
    protected long getEmployeeId() throws Exception {
        EmployeeContactInfo eci = (EmployeeContactInfo) getParent().get();
        return eci.getEmployeeId();
    }

    public TemplateModel FM_moderate(long prid, long manager, int status, String note) throws Exception {
        PurchaseRequest pr = PurchaseRequest.get(prid);
        pr.moderate(getEmployeeId(), manager, status, note);
        return pr;
    }

    public TemplateModel FM_getPR() throws Exception {
        TemplateList list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT pr_id FROM pr_approval WHERE (status = 0 AND manager = ?) OR (owner = ? AND status = 2)");
        ps.setLong(1, getEmployeeId());
        ps.setLong(2, getEmployeeId());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            PurchaseRequest pr = PurchaseRequest.get(rs.getLong(1));
            list.add((TemplateModel) pr);
        }
        con.close();
        return list;
    }

    public PurchaseRequestAdmin(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PurchaseRequestAdmin(ResourceId rid) throws Exception {
        super(rid);
    }
}
