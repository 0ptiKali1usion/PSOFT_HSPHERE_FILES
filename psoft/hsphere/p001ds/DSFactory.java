package psoft.hsphere.p001ds;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import psoft.hsphere.Session;
import psoft.hsphere.composite.Holder;
import psoft.hsphere.composite.Item;
import psoft.hsphere.exception.DSTemplateNotFoundException;
import psoft.hsphere.exception.DServerNotFoundException;
import psoft.util.TimeUtils;

/* renamed from: psoft.hsphere.ds.DSFactory */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DSFactory.class */
public class DSFactory {
    public static DedicatedServer createDedicatedServer() {
        return null;
    }

    public static DedicatedServerTemplate createDSTemplate(String name, String os, String cpu, String ram, String storage) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ds_templates(id, name, os, cpu, ram, storage, reseller_id) VALUES (?,?,?,?,?,?,?)");
            long newId = Session.getNewIdAsLong("ds_seq");
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setString(3, os);
            ps.setString(4, cpu);
            ps.setString(5, ram);
            ps.setString(6, storage);
            ps.setLong(7, Session.getResellerId());
            ps.executeUpdate();
            DedicatedServerTemplate dedicatedServerTemplate = new DedicatedServerTemplate(newId, name, os, cpu, ram, storage, Session.getResellerId());
            Session.closeStatement(ps);
            con.close();
            return dedicatedServerTemplate;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static DedicatedServer createDedicatedServer(String name, String rebootURL, String internalId, String superUser, String suPasswd, double setup, double recurrent, String os, String cpu, String ram, String storage, String mainIP, long templateId) throws Exception {
        URL reboot = new URL(rebootURL);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Timestamp created = new Timestamp(TimeUtils.currentTimeMillis());
            long newId = Session.getNewIdAsLong("ds_seq");
            ps = con.prepareStatement("INSERT INTO dedicated_servers (id,name, reboot_url,internal_id,su,su_passwd,setup, recurrent,state,os,cpu,ram,storage,created, reseller_id, ip, template_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setString(3, reboot.toString());
            ps.setString(4, internalId);
            ps.setString(5, superUser);
            ps.setString(6, suPasswd);
            ps.setDouble(7, setup);
            ps.setDouble(8, recurrent);
            ps.setDouble(9, DedicatedServerState.DISABLED.toInt());
            ps.setString(10, os);
            ps.setString(11, cpu);
            ps.setString(12, ram);
            ps.setString(13, storage);
            ps.setTimestamp(14, created);
            ps.setLong(15, Session.getResellerId());
            ps.setString(16, mainIP);
            ps.setLong(17, templateId);
            ps.executeUpdate();
            DedicatedServer dedicatedServer = new DedicatedServer(newId, name, mainIP, reboot, internalId, superUser, suPasswd, setup, recurrent, DedicatedServerState.DISABLED.toInt(), 0L, os, cpu, ram, storage, created, null, null, Session.getResellerId());
            Session.closeStatement(ps);
            con.close();
            return dedicatedServer;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static Holder buildResellerHolder(long resellerId) throws Exception {
        Item h;
        Session.getLog().debug("Building holder for resller " + resellerId);
        Holder top = new Holder(0L);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT id FROM ds_templates WHERE reseller_id = ? ORDER BY id");
            ps2.setLong(1, resellerId);
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                Item h2 = Holder.findChild(rs.getLong("id"));
                if (h2 == null) {
                    h2 = loadHolder(rs.getLong("id"));
                }
                Session.getLog().debug("Adding " + h2.toString() + " id=" + h2.getId() + " to holder " + top.toString() + " id=" + top.getId());
                top.addChild((Holder) h2);
            }
            ps = con.prepareStatement("SELECT id,template_id FROM dedicated_servers WHERE reseller_id = ? ORDER BY id");
            ps.setLong(1, resellerId);
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                long templateId = rs2.getLong("template_id");
                long id = rs2.getLong("id");
                Item i = (DedicatedServer) Holder.findChild(id);
                Item i2 = i;
                if (i == null) {
                    i2 = loadItem(id);
                }
                if (templateId > 0) {
                    h = Holder.findChild(templateId);
                    if (h == null) {
                        Session.getLog().debug("A problem occured while loading DS with id " + id + " the Dedicated server template with ID " + templateId + " was not found");
                    }
                } else {
                    h = top;
                }
                Session.getLog().debug("Adding item " + i2.toString() + " id=" + i2.getId() + " to holder " + h.toString() + " id=" + h.getId());
                h.addChild(i2);
            }
            Session.closeStatement(ps);
            con.close();
            return top;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static Item loadHolder(long id) throws Exception {
        Connection con = Session.getDb();
        Session.getLog().debug("Loading holder id=" + id);
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name, os, cpu, ram, storage, reseller_id FROM ds_templates WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DedicatedServerTemplate dedicatedServerTemplate = new DedicatedServerTemplate(id, rs.getString("name"), rs.getString("os"), rs.getString("cpu"), rs.getString("ram"), rs.getString("storage"), rs.getLong("reseller_id"));
                Session.closeStatement(ps);
                con.close();
                return dedicatedServerTemplate;
            }
            throw new DSTemplateNotFoundException("Dedicated server template with id=" + id + " not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    private static Item loadItem(long id) throws Exception {
        Connection con = Session.getDb();
        Session.getLog().debug("Loading item id=" + id);
        PreparedStatement ps = con.prepareStatement("SELECT name, template_id, ip, reboot_url, internal_id, su, su_passwd, setup, recurrent, state, os, cpu, ram, storage, rid, created, taken, cancellation, reseller_id FROM dedicated_servers WHERE id = ?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new DedicatedServer(id, rs.getString("name"), rs.getString("ip"), new URL(rs.getString("reboot_url")), rs.getString("internal_id"), rs.getString("su"), rs.getString("su_passwd"), rs.getDouble("setup"), rs.getDouble("recurrent"), rs.getInt("state"), rs.getLong("rid"), rs.getString("os"), rs.getString("cpu"), rs.getString("ram"), rs.getString("storage"), rs.getTimestamp("created"), rs.getTimestamp("taken"), rs.getTimestamp("cancellation"), rs.getLong("reseller_id"));
        }
        throw new DServerNotFoundException("Dedicated server with id=" + id + " not found");
    }

    public static DedicatedServer saveDedicatedServer(DedicatedServer changed, DedicatedServer original) throws Exception {
        DedicatedServer result = original.copy();
        result.setParent(original.getParent());
        PreparedStatement ps = null;
        StringBuffer sb = new StringBuffer("UPDATE dedicated_servers SET ip = ?, reboot_url = ?, su = ?, su_passwd = ?, state = ? ");
        Connection con = Session.getDb();
        try {
            if (changed.getIntState() == 1) {
                sb.append(", name= ?");
            }
            if (!changed.isTemplatedServer()) {
                sb.append(", setup = ?");
                sb.append(", recurrent = ?");
            }
            if (changed.getIntState() == 1 && !changed.isTemplatedServer()) {
                sb.append(", os = ?");
                sb.append(", cpu = ?");
                sb.append(", ram = ?");
                sb.append(", storage = ?");
            }
            sb.append(" WHERE id = ?");
            ps = con.prepareStatement(sb.toString());
            int i = 1 + 1;
            ps.setString(1, changed.getIp());
            int i2 = i + 1;
            ps.setString(i, changed.getRebootURL().toString());
            int i3 = i2 + 1;
            ps.setString(i2, changed.getSuperUser());
            int i4 = i3 + 1;
            ps.setString(i3, changed.getSuPasswd());
            result.setIP(changed.getIp());
            result.setRebootURL(changed.getRebootURL());
            result.setSuperUser(changed.getSuperUser());
            result.setSuPasswd(changed.getSuPasswd());
            int i5 = i4 + 1;
            ps.setInt(i4, changed.getIntState());
            if (changed.getIntState() == 1) {
                i5++;
                ps.setString(i5, changed.getName());
                result.setName(changed.getName());
            }
            if (!changed.isTemplatedServer()) {
                int i6 = i5;
                int i7 = i5 + 1;
                ps.setDouble(i6, changed.getSetup());
                i5 = i7 + 1;
                ps.setDouble(i7, changed.getRecurrent());
                result.setSetup(changed.getSetup());
                result.setRecurrent(changed.getRecurrent());
            }
            if (changed.getIntState() == 1 && !changed.isTemplatedServer()) {
                int i8 = i5;
                int i9 = i5 + 1;
                ps.setString(i8, changed.getOs());
                int i10 = i9 + 1;
                ps.setString(i9, changed.getCpu());
                int i11 = i10 + 1;
                ps.setString(i10, changed.getRam());
                i5 = i11 + 1;
                ps.setString(i11, changed.getStorage());
                result.setOs(changed.getOs());
                result.setCpu(changed.getCpu());
                result.setRam(changed.getRam());
                result.setStorage(changed.getStorage());
            }
            int i12 = i5;
            int i13 = i5 + 1;
            ps.setLong(i12, original.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void saveDedicatedServerTemplate(long templateId, String name, String os, String cpu, String ram, String storage) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE ds_templates SET name = ?, cpu = ?, os = ?, ram = ?, storage = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setString(2, cpu);
            ps.setString(3, os);
            ps.setString(4, ram);
            ps.setString(5, storage);
            ps.setLong(6, templateId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void lockDedicatedServer(long dsId, long resourceId, DedicatedServerState state, Timestamp taken) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET rid = ?, state = ?, taken = ? WHERE id = ?");
            ps.setLong(1, resourceId);
            ps.setInt(2, state.toInt());
            ps.setTimestamp(3, taken);
            ps.setLong(4, dsId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void unlockDedicatedServer(long dsId, DedicatedServerState state) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET rid = null, state = ?, taken = null, cancellation = null WHERE id = ?");
            ps.setInt(1, state.toInt());
            ps.setLong(2, dsId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void scheduleDedicatedServerCancel(long dsId, Timestamp date) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET cancellation = ? WHERE id = ?");
            ps.setTimestamp(1, date);
            ps.setLong(2, dsId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void discardDedicatedServerCancel(long dsId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET cancellation = null WHERE id = ?");
            ps.setLong(1, dsId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void updateDedicatedServerState(long dsId, DedicatedServerState state) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dedicated_servers SET state = ? WHERE id = ?");
            ps.setInt(1, state.toInt());
            ps.setLong(2, dsId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void deleteItem(Item i) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        if (i instanceof DedicatedServer) {
            ps = con.prepareStatement("DELETE FROM dedicated_servers WHERE id = ?");
        } else if (i instanceof DedicatedServerTemplate) {
            ps = con.prepareStatement("DELETE FROM ds_templates WHERE id = ?");
        }
        if (ps != null) {
            try {
                ps.setLong(1, i.getId());
                ps.executeUpdate();
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
    }
}
