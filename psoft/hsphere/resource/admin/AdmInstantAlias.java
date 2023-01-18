package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmInstantAlias.class */
public class AdmInstantAlias extends SharedObject implements TemplateHashModel {
    protected String prefix;
    protected int sharedIPTag;
    protected long parentId;
    protected List records;
    protected List lServers;

    public String getPrefix() {
        return this.prefix;
    }

    public int getTag() {
        return this.sharedIPTag;
    }

    public boolean equals(Object obj) {
        return getId() == ((AdmInstantAlias) obj).getId();
    }

    public AdmInstantAlias(long id, long parentId, String prefix, int sharedIPTag) {
        super(id);
        this.records = Collections.synchronizedList(new ArrayList());
        this.lServers = Collections.synchronizedList(new ArrayList());
        this.parentId = parentId;
        this.prefix = prefix;
        this.sharedIPTag = sharedIPTag;
        try {
            loadRecords();
            getLservers();
        } catch (Exception ex) {
            Session.getLog().error("Error loading DNS records", ex);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static AdmInstantAlias create(long parentId, String prefix, int ipTag) throws Exception {
        AdmInstantAlias als;
        long newId = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO e_ialiases(id,zone_id,prefix,tag) VALUES (?,?,?,?)");
            ps.setLong(1, newId);
            ps.setLong(2, parentId);
            ps.setString(3, prefix);
            ps.setInt(4, ipTag);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (ipTag != 4) {
                als = new AdmInstantAlias(newId, parentId, prefix, ipTag);
            } else {
                als = new ResellerCpAlias(newId, parentId, prefix);
            }
            als.createAllDNSRecords();
            return als;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static AdmInstantAlias get(long id) throws Exception {
        AdmInstantAlias tmp = (AdmInstantAlias) get(id, AdmInstantAlias.class);
        if (tmp != null) {
            return tmp;
        }
        AdmInstantAlias tmp2 = (AdmInstantAlias) get(id, ResellerCpAlias.class);
        if (tmp2 != null) {
            return tmp2;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT zone_id, prefix, tag FROM e_ialiases WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt(3) != 4) {
                    tmp2 = new AdmInstantAlias(id, rs.getLong(1), rs.getString(2), rs.getInt(3));
                } else {
                    tmp2 = new ResellerCpAlias(id, rs.getLong(1), rs.getString(2));
                }
            }
            return tmp2;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static List getByTag(int tag) throws Exception {
        List iAliases = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM e_ialiases WHERE tag = ?");
            ps.setInt(1, tag);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                iAliases.add(get(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return iAliases;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getTags() throws Exception {
        List tags = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT tag FROM e_ialiases GROUP BY tag");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(new Integer(rs.getInt(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return tags;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getFreeLserver() throws Exception {
        List lservers = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.id FROM l_server_ips i, l_server l WHERE i.l_server_id=l.id AND l.group_id IN (SELECT id FROM l_server_groups WHERE type_id IN (?, ?)) AND l.id NOT IN (SELECT l_server_id FROM l_server_ips WHERE flag = ?) GROUP BY l.id");
            ps.setInt(1, 1);
            ps.setInt(2, 5);
            ps.setInt(3, this.sharedIPTag);
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

    public void delete() throws Exception {
        for (AdmDNSRecord rec : this.records) {
            rec.delete();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM e_ialiases WHERE id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            remove(this.f51id, AdmInstantAlias.class);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void loadRecords() throws Exception {
        this.records.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM e_dns_records WHERE alias_id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AdmDNSRecord rec = AdmDNSRecord.get(rs.getLong(1));
                if (rec != null) {
                    this.records.add(rec);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized AdmDNSRecord addRecord(long lServerId, String ip) throws Exception {
        AdmDNSRecord rs = AdmDNSRecord.create(this.f51id, this.parentId, ip, this.prefix + lServerId);
        this.records.add(rs);
        return rs;
    }

    public synchronized void delRecord(long lServerId, String ip) throws Exception {
        ArrayList delList = new ArrayList();
        for (AdmDNSRecord r : this.records) {
            if (r.getName().startsWith("*." + this.prefix + lServerId)) {
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

    public synchronized void delRecord(long id) throws Exception {
        AdmDNSRecord rs = AdmDNSRecord.get(id);
        if (this.records.contains(rs)) {
            Session.getLog().error("Going to delete DNS record with id=" + id);
            rs.delete();
            this.records.remove(rs);
            return;
        }
        Session.getLog().error("This record doesn`t belong to this alias.");
        throw new HSUserException("adminstantalias.record");
    }

    public synchronized void delRecord(String ip) throws Exception {
        if (ip == null) {
            throw new HSUserException("adminstantalias.ip");
        }
        for (AdmDNSRecord rec : this.records) {
            if (ip.equals(rec.getIP())) {
                rec.delete();
            }
        }
        loadRecords();
    }

    public void getLservers() throws Exception {
        this.lServers.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server_id, ip FROM l_server_ips  WHERE  flag = ? AND ip NOT IN ( SELECT d.data FROM e_dns_records e, dns_records d WHERE d.id = e.id AND  e.alias_id = ?)");
            ps.setInt(1, this.sharedIPTag);
            ps.setLong(2, getId());
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

    public void createAllDNSRecords() throws Exception {
        Exception exCaused = null;
        for (TemplateHash hs : this.lServers) {
            try {
                LogicalServer ls = hs.get("lserver");
                addRecord(ls.getId(), hs.get("ip").toString());
            } catch (Exception ex) {
                exCaused = ex;
            }
        }
        getLservers();
        if (exCaused != null) {
            throw exCaused;
        }
    }

    public String getFullName(long serverId) {
        try {
            AdmDNSZone zone = AdmDNSZone.get(this.parentId);
            return this.prefix + Long.toString(serverId) + "." + zone.getDomainName();
        } catch (Exception ex) {
            Session.getLog().error("Error getting zone name", ex);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("prefix".equals(key)) {
            return new TemplateString(this.prefix);
        }
        if ("tag".equals(key)) {
            return new TemplateString(this.sharedIPTag);
        }
        if ("records".equals(key)) {
            return new ListAdapter(this.records);
        }
        if ("l_servers".equals(key)) {
            return new TemplateList(this.lServers);
        }
        if ("free_lservers".equals(key)) {
            try {
                return new ListAdapter(getFreeLserver());
            } catch (Exception ex) {
                throw new TemplateModelException(ex.toString());
            }
        }
        return super.get(key);
    }
}
