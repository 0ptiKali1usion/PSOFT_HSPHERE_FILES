package psoft.hsphere.resource.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateHash;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ResellerCpAlias.class */
public class ResellerCpAlias extends AdmInstantAlias {
    protected static final int sharedIPTag = 4;

    public ResellerCpAlias(long id, long parentId, String prefix) {
        super(id, parentId, prefix, 4);
    }

    @Override // psoft.hsphere.resource.admin.AdmInstantAlias
    public void getLservers() throws Exception {
        this.lServers.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT a.l_server_id, a.ip FROM l_server_ips a, l_server b WHERE a.l_server_id = b.id  AND a.flag = ? AND b.group_id = ? AND a.ip NOT IN (SELECT d.data FROM e_dns_records e, dns_records d WHERE d.id = e.id AND e.alias_id = ?)");
            ps.setInt(1, 4);
            ps.setInt(2, 10);
            ps.setLong(3, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHash lServer = new TemplateHash();
                lServer.put("lserver", LogicalServer.get(rs.getLong(1)));
                lServer.put("ip", rs.getString(2));
                this.lServers.add(lServer);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.admin.AdmInstantAlias
    public List getFreeLserver() throws Exception {
        List lservers = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.id FROM l_server_ips i, l_server l WHERE i.l_server_id = l.id AND i.flag = ? AND l.group_id = ? AND l.id NOT IN (SELECT l_server_id FROM l_server_ips WHERE flag = ?) GROUP BY l.id");
            ps.setInt(1, 4);
            ps.setInt(2, 10);
            ps.setInt(3, 4);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lservers.add(LogicalServer.get(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return lservers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.admin.AdmInstantAlias
    public synchronized AdmDNSRecord addRecord(long lServerId, String ip) throws Exception {
        for (AdmDNSRecord rec : this.records) {
            if (ip.equals(rec.getIP())) {
                throw new HSUserException("resellercpalias.dns", new Object[]{ip});
            }
        }
        AdmDNSRecord rs = AdmDNSRecord.create(this.f51id, this.parentId, ip, this.prefix);
        this.records.add(rs);
        return rs;
    }

    @Override // psoft.hsphere.resource.admin.AdmInstantAlias
    public synchronized void delRecord(long lServerId, String ip) throws Exception {
        ArrayList delList = new ArrayList();
        for (AdmDNSRecord r : this.records) {
            if (r.getName().startsWith("*." + this.prefix + ".")) {
                try {
                    r.delete();
                    delList.add(r);
                } catch (Exception ex) {
                    Session.getLog().debug("An error occured while deleting ADNSRecord", ex);
                }
            }
        }
        this.records.removeAll(delList);
    }
}
