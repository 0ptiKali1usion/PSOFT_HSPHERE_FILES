package psoft.hsphere.resource.adm3W;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PurchaseRequestResource.class */
public class PurchaseRequestResource extends Resource {
    protected List requests;

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("requests") ? new TemplateList(this.requests) : super.get(key);
    }

    public PurchaseRequestResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.requests = new ArrayList();
    }

    public PurchaseRequestResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id FROM prequest WHERE rid = ?");
        ps.setLong(1, getEmployeeId());
        ResultSet rs = ps.executeQuery();
        this.requests = new ArrayList();
        while (rs.next()) {
            getLog().info("Loading pr: " + rs.getLong(1));
            this.requests.add(PurchaseRequest.get(rs.getLong(1)));
        }
    }

    protected long getEmployeeId() throws Exception {
        EmployeeContactInfo eci = (EmployeeContactInfo) getParent().get();
        return eci.getEmployeeId();
    }

    public TemplateModel FM_createPR() throws Exception {
        PurchaseRequest pr = PurchaseRequest.create(getEmployeeId());
        getLog().info("created pr:" + pr);
        this.requests.add(pr);
        return pr;
    }

    protected PurchaseRequest getPR(long prid) throws Exception {
        return PurchaseRequest.get(prid);
    }

    public TemplateModel FM_getPR(long prid) throws Exception {
        return PurchaseRequest.get(prid);
    }

    public TemplateModel FM_getItem(long prid, int itemId) throws Exception {
        return getPR(prid).getItem(itemId);
    }

    public TemplateModel FM_delItem(long prid, int itemId) throws Exception {
        PurchaseRequest pr = getPR(prid);
        getLog().info("deleting item " + itemId + " From " + pr);
        pr.deleteItem(itemId);
        return pr;
    }

    public TemplateModel FM_addItem(long prid, String quantity, String measurement, String descripton, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
        PurchaseRequest pr = getPR(prid);
        return pr.addItem(quantity, measurement, descripton, vnumber, required, eprice, eamount, anumber, dept, notes);
    }

    public TemplateModel FM_deleteItem(long prid, int itemid) throws Exception {
        PurchaseRequest pr = getPR(prid);
        pr.deleteItem(itemid);
        return pr;
    }

    public TemplateModel FM_changeItem(long prid, int itemid, String quantity, String measurement, String descripton, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
        PurchaseRequest pr = getPR(prid);
        pr.changeItem(itemid, quantity, measurement, descripton, vnumber, required, eprice, eamount, anumber, dept, notes);
        return pr;
    }

    public TemplateModel FM_submit(long prid, long manager) throws Exception {
        PurchaseRequest pr = getPR(prid);
        pr.submit(manager);
        return pr;
    }

    public TemplateModel FM_changePR(long prid, int dept, int potype, String location, int taxable, int expend, String address, String eusage, String notes, String vname, String taxid, String vaddress, String vcity, String vstate, String vzip, String vfob, String vcontact, String vphone, String vfax, String terms, String shipvia) throws Exception {
        PurchaseRequest pr = getPR(prid);
        pr.change(dept, potype, location, taxable, expend, address, eusage, notes, vname, taxid, vaddress, vcity, vstate, vzip, vfob, vcontact, vphone, vfax, terms, shipvia);
        return pr;
    }
}
