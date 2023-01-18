package psoft.hsphere.resource.adm3W;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PurchaseRequest.class */
public class PurchaseRequest extends SharedObject implements TemplateHashModel {
    protected long owner;
    protected PRAproval approval;
    protected SimpleHash hash;
    protected List items;
    protected int count;

    public void put(String key, Date date) {
        if (date != null) {
            this.hash.put(key, date.toString());
        } else {
            this.hash.put(key, "");
        }
    }

    public void submit(long manager) throws Exception {
        this.approval = PRAproval.create(getId(), this.owner, manager);
    }

    public void accept(long owner, String note, long po) throws Exception {
        this.approval.moderate(owner, -1L, 1, note);
        if (po == 0) {
            throw new Exception("Invalid PO Number");
        }
        setPO(po);
    }

    public void moderate(long owner, long manager, int status, String note) throws Exception {
        this.approval.moderate(owner, manager, status, note);
    }

    protected void setPO(long po) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("UPDATE prequest SET po = ? WHERE id = ?");
        ps.setLong(1, po);
        ps.setLong(2, getId());
        ps.executeUpdate();
        put("po", po);
    }

    public void put(String key, String value) {
        if (value != null) {
            this.hash.put(key, value);
        } else {
            this.hash.put(key, "");
        }
    }

    public void put(String key, TemplateModel value) {
        this.hash.put(key, value);
    }

    protected void put(String key, int value) {
        this.hash.put(key, Integer.toString(value));
    }

    protected void put(String key, long value) {
        this.hash.put(key, Long.toString(value));
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("items") ? new TemplateList(this.items) : key.equals("approval") ? this.approval : this.hash.get(key);
    }

    public static PurchaseRequest create(long rid) throws Exception {
        long id = Resource.getNewId();
        java.sql.Date date = TimeUtils.getSQLDate();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("INSERT INTO prequest (id, rid, created) VALUES (?, ?, ?)");
        ps.setLong(1, id);
        ps.setLong(2, rid);
        ps.setDate(3, date);
        ps.executeUpdate();
        con.close();
        PurchaseRequest pr = new PurchaseRequest(id, rid, date);
        return pr;
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("DELETE FROM pr_items WHERE pr_id = ? ");
        ps.setLong(1, getId());
        ps.executeUpdate();
        PreparedStatement ps2 = con.prepareStatement("DELETE FROM prequest WHERE id = ?");
        ps2.setLong(1, getId());
        ps2.executeUpdate();
    }

    protected PurchaseRequest(long id, long rid, Date date) throws Exception {
        super(id);
        this.hash = new SimpleHash();
        this.count = 1;
        this.owner = rid;
        put("id", id);
        put("rid", rid);
        put("created", date.toString());
        this.items = new ArrayList();
        try {
            this.approval = PRAproval.load(getId(), rid);
        } catch (Exception e) {
            Session.getLog().debug("Unable to load approval", e);
        }
    }

    public static PurchaseRequest get(long id) throws Exception {
        PurchaseRequest p = (PurchaseRequest) get(id, PurchaseRequest.class);
        Session.getLog().info("got pr ->" + p);
        if (p != null) {
            return p;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT rid, dept, potype, location, taxable, expend, address, eusage, notes, vname, taxid, vaddress, vcity, vstate, vzip, vfob, vcontact, vphone, vfax, terms, shipvia, updated, created, status, po FROM prequest WHERE id = ?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            PurchaseRequest p2 = new PurchaseRequest(id, rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17), rs.getString(18), rs.getString(19), rs.getString(20), rs.getString(21), rs.getTimestamp(22), rs.getDate(23), rs.getInt(24), rs.getLong(25));
            Session.getLog().info("got pr ->" + p2);
            return p2;
        }
        throw new Exception("Not found");
    }

    public void change(int dept, int potype, String location, int taxable, int expend, String address, String eusage, String notes, String vname, String taxid, String vaddress, String vcity, String vstate, String vzip, String vfob, String vcontact, String vphone, String vfax, String terms, String shipvia) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("UPDATE prequest SET dept = ?, potype = ?, location = ?, taxable = ?, expend = ?, address = ?, eusage = ?, notes = ?, vname = ?, taxid = ?, vaddress = ?, vcity = ?, vstate = ?, vzip = ?, vfob = ?, vcontact = ?, vphone = ?, vfax = ?, terms = ?, shipvia = ?, updated = ? WHERE id = ?");
        Timestamp updated = TimeUtils.getSQLTimestamp();
        ps.setInt(1, dept);
        ps.setInt(2, potype);
        ps.setString(3, location);
        ps.setInt(4, taxable);
        ps.setInt(5, expend);
        ps.setString(6, address);
        ps.setString(7, eusage);
        ps.setString(8, notes);
        ps.setString(9, vname);
        ps.setString(10, taxid);
        ps.setString(11, vaddress);
        ps.setString(12, vcity);
        ps.setString(13, vstate);
        ps.setString(14, vzip);
        ps.setString(15, vfob);
        ps.setString(16, vcontact);
        ps.setString(17, vphone);
        ps.setString(18, vfax);
        ps.setString(19, terms);
        ps.setString(20, shipvia);
        ps.setTimestamp(21, updated);
        ps.setLong(22, getId());
        ps.executeUpdate();
        put("dept", dept);
        put("potype", potype);
        put("location", location);
        put("taxable", taxable);
        put("expend", expend);
        put("address", address);
        put("eusage", eusage);
        put("notes", notes);
        put("vname", vname);
        put("taxid", taxid);
        put("vaddress", vaddress);
        put("vcity", vcity);
        put("vstate", vstate);
        put("vzip", vzip);
        put("vfob", vfob);
        put("vcontact", vcontact);
        put("vphone", vphone);
        put("vfax", vfax);
        put("terms", terms);
        put("shipvia", shipvia);
        put("updated", updated);
    }

    protected PurchaseRequest(long id, long rid, int dept, int potype, String location, int taxable, int expend, String address, String eusage, String notes, String vname, String taxid, String vaddress, String vcity, String vstate, String vzip, String vfob, String vcontact, String vphone, String vfax, String terms, String shipvia, Date updated, Date created, int status, long po) throws Exception {
        super(id);
        this.hash = new SimpleHash();
        this.count = 1;
        this.owner = rid;
        put("id", id);
        put("rid", rid);
        put("dept", dept);
        put("potype", potype);
        put("location", location);
        put("taxable", taxable);
        put("expend", expend);
        put("address", address);
        put("eusage", eusage);
        put("notes", notes);
        put("vname", vname);
        put("taxid", taxid);
        put("vaddress", vaddress);
        put("vcity", vcity);
        put("vstate", vstate);
        put("vzip", vzip);
        put("vfob", vfob);
        put("vcontact", vcontact);
        put("vphone", vphone);
        put("vfax", vfax);
        put("terms", terms);
        put("shipvia", shipvia);
        put("updated", updated);
        put("created", created);
        put("status", status);
        if (po != 0) {
            put("po", po);
        }
        loadItems();
        try {
            this.approval = PRAproval.load(getId(), rid);
        } catch (Exception e) {
            Session.getLog().debug("Unable to load approval", e);
        }
    }

    protected void loadItems() throws Exception {
        this.items = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id, quantity, measurement, description, vnumber, required, eprice, eamount, anumber, dept, notes FROM pr_item WHERE pr_id = ? ORDER BY id");
        ps.setLong(1, getId());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            if (this.count < rs.getInt(1)) {
                this.count = rs.getInt(1);
            }
            this.items.add(new Item(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)));
        }
        con.close();
    }

    public Item getItem(int id) throws Exception {
        for (Item item : this.items) {
            if (item.f166id == id) {
                return item;
            }
        }
        return null;
    }

    public void deleteItem(int id) throws Exception {
        Item i = getItem(id);
        i.delete();
        this.items.remove(i);
    }

    public void changeItem(int id, String quantity, String measurement, String description, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
        getItem(id).change(quantity, measurement, description, vnumber, required, eprice, eamount, anumber, dept, notes);
    }

    public Item addItem(String quantity, String measurement, String description, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
        int id = this.count + 1;
        this.count = id;
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("INSERT INTO pr_item (id, pr_id, quantity, measurement, description, vnumber, required, eprice, eamount, anumber, dept, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setLong(2, getId());
        ps.setString(3, quantity);
        ps.setString(4, measurement);
        ps.setString(5, description);
        ps.setString(6, vnumber);
        ps.setString(7, required);
        ps.setString(8, eprice);
        ps.setString(9, eamount);
        ps.setString(10, anumber);
        ps.setString(11, dept);
        ps.setString(12, notes);
        ps.executeUpdate();
        con.close();
        List list = this.items;
        Item i = new Item(id, quantity, measurement, description, vnumber, required, eprice, eamount, anumber, dept, notes);
        list.add(i);
        return i;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PurchaseRequest$Item.class */
    public class Item extends SimpleHash {

        /* renamed from: id */
        protected long f166id;

        public TemplateModel get(String key) throws TemplateModelException {
            if (key.equals("pr_id")) {
                return new TemplateString(PurchaseRequest.this.getId());
            }
            return super.get(key);
        }

        public void delete() throws Exception {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("DELETE FROM pr_item WHERE id = ? AND pr_id = ?");
            ps.setLong(1, this.f166id);
            ps.setLong(2, PurchaseRequest.this.getId());
            ps.executeUpdate();
            con.close();
        }

        public void change(String quantity, String measurement, String description, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE pr_item SET quantity = ?, measurement = ?, description = ?, vnumber = ?, required = ?, eprice = ?, eamount = ?, anumber = ?, dept = ?, notes = ? WHERE id = ? AND pr_id = ?");
            ps.setString(1, quantity);
            ps.setString(2, measurement);
            ps.setString(3, description);
            ps.setString(4, vnumber);
            ps.setString(5, required);
            ps.setString(6, eprice);
            ps.setString(7, eamount);
            ps.setString(8, anumber);
            ps.setString(9, dept);
            ps.setString(10, notes);
            ps.setLong(11, this.f166id);
            ps.setLong(12, PurchaseRequest.this.getId());
            ps.executeUpdate();
            con.close();
            put("id", Long.toString(this.f166id));
            put("quantity", quantity);
            put("measurement", measurement);
            put("description", description);
            put("vnumber", vnumber);
            put("required", required);
            put("eprice", eprice);
            put("eamount", eamount);
            put("anumber", anumber);
            put("dept", dept);
            put("notes", notes);
        }

        public Item(int id, String quantity, String measurement, String description, String vnumber, String required, String eprice, String eamount, String anumber, String dept, String notes) throws Exception {
            PurchaseRequest.this = r5;
            this.f166id = id;
            put("id", Integer.toString(id));
            put("quantity", quantity);
            put("measurement", measurement);
            put("description", description);
            put("vnumber", vnumber);
            put("required", required);
            put("eprice", eprice);
            put("eamount", eamount);
            put("anumber", anumber);
            put("dept", dept);
            put("notes", notes);
        }
    }
}
