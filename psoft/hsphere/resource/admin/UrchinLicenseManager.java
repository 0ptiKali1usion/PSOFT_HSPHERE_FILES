package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/UrchinLicenseManager.class */
public class UrchinLicenseManager extends Resource {
    protected Hashtable licenses;
    protected List lic_list;
    protected String keycode;
    protected String keycode_ls;

    public UrchinLicenseManager(int type, Collection init) throws Exception {
        super(type, init);
        this.lic_list = new ArrayList();
    }

    public UrchinLicenseManager(ResourceId rid) throws Exception {
        super(rid);
        this.lic_list = new ArrayList();
    }

    protected void reloadLicenses() throws Exception {
        this.lic_list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT urchin_lic.id, urchin_lic.ser_num, urchin_lic.lic_code, urchin_lic.lic_size, urchin_lic.lic_used, l_server.name, urchin_lic.l_server_id FROM urchin_lic, l_server WHERE urchin_lic.l_server_id = l_server.id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.licenses = new Hashtable();
                this.licenses.put("id", rs.getString(1));
                this.licenses.put("ser_num", rs.getString(2));
                this.licenses.put("lic_code", rs.getString(3));
                this.licenses.put("lic_size", rs.getString(4));
                this.licenses.put("lic_used", rs.getString(5));
                this.licenses.put("l_server_name", rs.getString(6));
                this.licenses.put("l_server_id", rs.getString(7));
                this.lic_list.add(this.licenses);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void addLicense(long l_server_id, String ser_num, String lic_code, int lic_size) throws Exception {
        Connection con = Session.getTransConnection();
        PreparedStatement ps = null;
        try {
            try {
                long id = Session.getNewIdAsLong("urchin_lic_seq");
                ps = con.prepareStatement("INSERT INTO urchin_lic(id,l_server_id,ser_num, lic_code,lic_size,lic_used) VALUES(?, ?, ?, ?, ?, 0)");
                ps.setLong(1, id);
                ps.setLong(2, l_server_id);
                ps.setString(3, ser_num);
                ps.setString(4, lic_code);
                ps.setLong(5, lic_size);
                ps.executeUpdate();
                changeUrchinConfig(l_server_id);
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    protected void updateLicense(long l_server_id, long l_server_id_old, long lic_id, String ser_num, String lic_code, int lic_size, int lic_used) throws Exception {
        if (lic_used > lic_size || (l_server_id != l_server_id_old && lic_used > 0)) {
            throw new HSUserException("urchin.upd_lic_error", new Object[]{String.valueOf(lic_used)});
        }
        Connection con = Session.getTransConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE urchin_lic SET ser_num = ?, lic_code = ?, lic_size = ?, l_server_id = ? WHERE id = ?");
                ps.setString(1, ser_num);
                ps.setString(2, lic_code);
                ps.setInt(3, lic_size);
                ps.setLong(4, l_server_id);
                ps.setLong(5, lic_id);
                ps.executeUpdate();
                changeUrchinConfig(l_server_id);
                if (l_server_id != l_server_id_old) {
                    changeUrchinConfig(l_server_id_old);
                }
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    protected void deleteLicense(long l_server_id, long lic_id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT lic_used FROM urchin_lic WHERE id = ?");
            ps2.setLong(1, lic_id);
            ResultSet rs = ps2.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new HSUserException("urchin.upd_lic_error", new Object[]{rs.getString(1)});
            }
            ps2.close();
            Connection cont = Session.getTransConnection();
            try {
                ps = cont.prepareStatement("DELETE FROM urchin_lic WHERE id = ?");
                ps.setLong(1, lic_id);
                ps.executeUpdate();
                changeUrchinConfig(l_server_id);
                Session.commitTransConnection(cont);
            } catch (Exception e) {
                cont.rollback();
                throw e;
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static long getFreeLicense(long l_server_id) throws Exception {
        long id = -1;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT MIN(id) FROM urchin_lic WHERE l_server_id = ? AND lic_size > lic_used");
            ps.setLong(1, l_server_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getLong(1) > 0 ? rs.getLong(1) : -1L;
            }
            Session.closeStatement(ps);
            con.close();
            return id;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized boolean changeUsedLicense(long lic_id, int change_val) throws Exception {
        int lic_free = 0;
        int lic_used = 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT lic_size - lic_used, lic_used FROM urchin_lic WHERE id = ?");
            ps2.setLong(1, lic_id);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                lic_free = rs.getInt(1);
                lic_used = rs.getInt(2);
            }
            if (lic_free >= 0 && ((change_val <= 0 || lic_free >= change_val) && (change_val >= 0 || lic_used >= Math.abs(change_val)))) {
                ps2.close();
                ps = con.prepareStatement("UPDATE urchin_lic SET lic_used = lic_used + ? WHERE id = ?");
                ps.setInt(1, change_val);
                ps.setLong(2, lic_id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                return true;
            }
            Session.closeStatement(ps2);
            con.close();
            return false;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_reloadLicenses() throws Exception {
        reloadLicenses();
    }

    public TemplateModel FM_addLicense(long l_server_id, String ser_num, String lic_code, int lic_size) throws Exception {
        addLicense(l_server_id, ser_num, lic_code, lic_size);
        return this;
    }

    public TemplateModel FM_deleteLicense(long l_server_id, long lic_id) throws Exception {
        deleteLicense(l_server_id, lic_id);
        return this;
    }

    public TemplateModel FM_updateLicense(long l_server_id, long l_server_id_old, long lic_id, String ser_num, String lic_code, int lic_size, int lic_used) throws Exception {
        updateLicense(l_server_id, l_server_id_old, lic_id, ser_num, lic_code, lic_size, lic_used);
        return this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v4, types: [java.lang.String[], java.lang.String[][]] */
    protected void changeUrchinConfig(long l_server_id) throws Exception {
        String serial_numbers = "";
        String lic_codes = "";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ser_num, lic_code FROM urchin_lic WHERE l_server_id = ? ");
            ps.setLong(1, l_server_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (serial_numbers.length() == 0) {
                    serial_numbers = rs.getString(1);
                    lic_codes = rs.getString(2);
                } else {
                    serial_numbers = serial_numbers.concat("_").concat(rs.getString(1));
                    lic_codes = lic_codes.concat("_").concat(rs.getString(2));
                }
            }
            HostEntry he = HostManager.getHost(l_server_id);
            if (he.getGroupType() == 1) {
                List list = new ArrayList();
                list.add(serial_numbers);
                list.add(lic_codes);
                try {
                    he.exec("urchin-licenses.pl", list);
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception ex) {
                    Session.getLog().error("ERROR changing Urchin config " + ex);
                    throw new HSUserException("urchin.not_found");
                }
            }
            try {
                ((WinHostEntry) he).exec("urchin-setlicinfo.asp", (String[][]) new String[]{new String[]{"keys", serial_numbers}, new String[]{"lics", lic_codes}}).iterator();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex2) {
                Session.getLog().error("ERROR changing Urchin config ", ex2);
                throw new HSUserException("urchin.not_found");
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getKeyCode(long l_server_id) throws Exception {
        Iterator listRes;
        HostEntry he = HostManager.getHost(l_server_id);
        this.keycode = "";
        this.keycode_ls = "";
        try {
            if (he.getGroupType() == 1) {
                listRes = he.exec("urchin-key.pl", new ArrayList()).iterator();
            } else {
                listRes = ((WinHostEntry) he).exec("urchin-getkey.asp", new ArrayList()).iterator();
            }
            if (listRes.hasNext()) {
                this.keycode = (String) listRes.next();
                this.keycode_ls = LogicalServer.get(l_server_id).getName();
            }
            return this;
        } catch (Exception ex) {
            Session.getLog().error("Failed! Urchin not found." + ex);
            throw new HSUserException("urchin.not_found");
        }
    }

    public TemplateModel FM_l_servers() throws Exception {
        ArrayList l = new ArrayList();
        l.addAll(HostManager.getHostsByGroupType(1));
        l.addAll(HostManager.getHostsByGroupType(5));
        return new ListAdapter(l);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("licenses") ? new TemplateList(this.lic_list) : key.equals("keycode") ? new TemplateString(this.keycode) : key.equals("keycode_ls") ? new TemplateString(this.keycode_ls) : super.get(key);
    }
}
