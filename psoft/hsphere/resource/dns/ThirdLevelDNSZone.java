package psoft.hsphere.resource.dns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.AdmDNSZone;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/ThirdLevelDNSZone.class */
public class ThirdLevelDNSZone extends DNSZone {
    public ThirdLevelDNSZone(ResourceId id) throws Exception {
        super(id);
    }

    public ThirdLevelDNSZone(int type, Collection values) throws Exception {
        super(type, values);
    }

    @Override // psoft.hsphere.resource.dns.DNSZone
    public void assignNameServers() throws Exception {
        super.assignNameServers();
        String zoneName = this.name.substring(this.name.indexOf(".") + 1);
        AdmDNSZone admZone = AdmDNSZone.getByName(zoneName);
        if (admZone == null) {
            throw new HSUserException("thirdleveldnszone.no", new Object[]{zoneName});
        }
        if (!admZone.allowHosting()) {
            throw new HSUserException("thirdleveldnszone.zone", new Object[]{zoneName});
        }
        this.master = admZone.getMaster().getId();
        this.slave1 = admZone.getSlave1Id();
        this.slave2 = admZone.getSlave2Id();
    }

    @Override // psoft.hsphere.resource.dns.DNSZone
    public int testZoneName() throws Exception {
        boolean zoneTaken = false;
        String message = "";
        String realZoneName = this.name;
        Session.getLog().debug("ThirdLevelDNSZone testZoneName: " + realZoneName);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("SELECT id, name FROM l_server WHERE name=?");
                ps2.setString(1, realZoneName);
                ps2.executeQuery();
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    message = " by logical server";
                    zoneTaken = true;
                }
                ps = con.prepareStatement("SELECT l_server_id, prefix FROM l_server_aliases WHERE prefix=?");
                ps.setString(1, realZoneName);
                ps.executeQuery();
                ResultSet rs1 = ps.executeQuery();
                if (rs1.next()) {
                    message = " by logical server alias";
                    zoneTaken = true;
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().debug("Unable to check if zone " + realZoneName + " is already taken", ex);
                Session.closeStatement(ps);
                con.close();
            }
            Session.getLog().debug("method testZoneName() Zone name: " + realZoneName);
            if (zoneTaken) {
                String errmess = "Zone " + realZoneName + " is taken" + message;
                Session.getLog().debug(errmess);
                throw new HSUserException(errmess);
            }
            return testZoneName(this.name, false);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
