package psoft.hsphere.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.background.JobManager;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/admin/NetworkGatewayManager.class */
public class NetworkGatewayManager {
    private HashMap gateways = new HashMap();
    private ArrayList newGateways = new ArrayList();
    private ArrayList delGateways = new ArrayList();
    private static NetworkGatewayManager gatewayManager;

    static {
        try {
            gatewayManager = new NetworkGatewayManager();
        } catch (Exception e) {
        }
    }

    protected NetworkGatewayManager() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM net_gateways");
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int flag = rs.getInt("flag");
                String addr = rs.getString("addr");
                if (flag != 2) {
                    NetworkGateway netGateway = new NetworkGateway(rs.getString("gateway"), rs.getString("mask"));
                    this.gateways.put(addr, netGateway);
                }
                if (flag == 1) {
                    this.newGateways.add(addr);
                } else if (flag == 2) {
                    this.delGateways.add(addr);
                }
            }
            if (!this.newGateways.isEmpty() || !this.delGateways.isEmpty()) {
                JobManager jobManager = C0004CP.getJobManager();
                jobManager.FM_enableJob("vpsReconfig");
                jobManager.FM_startJob("vpsReconfig");
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Hashtable getNetworkGateway(String addr) {
        NetworkGateway gateway = (NetworkGateway) this.gateways.get(addr);
        Hashtable hash = new Hashtable();
        if (gateway != null) {
            hash.put("addr", addr);
            hash.put("gateway", gateway.gateway);
            hash.put("mask", gateway.mask);
        }
        return hash;
    }

    public String getSubnetMaskByIP(String ip) {
        long ipNum = C0015IP.toLong(ip);
        for (String addr : this.gateways.keySet()) {
            NetworkGateway gateway = (NetworkGateway) this.gateways.get(addr);
            long maskNum = C0015IP.toLong(gateway.mask);
            long addrNum = C0015IP.toLong(addr);
            if ((addrNum & maskNum) == (ipNum & maskNum)) {
                return gateway.mask;
            }
        }
        return "";
    }

    public TemplateList getNetworkGateways() {
        List result = new ArrayList();
        for (String addr : this.gateways.keySet()) {
            result.add(new TemplateHash(getNetworkGateway(addr)));
        }
        return new TemplateList(result);
    }

    public ArrayList getNewNetworkGateways() throws Exception {
        ArrayList result = null;
        synchronized (this.newGateways) {
            if (!this.newGateways.isEmpty()) {
                result = new ArrayList(this.newGateways);
                Connection con = Session.getDb();
                PreparedStatement ps = null;
                try {
                    StringBuffer addresses = new StringBuffer();
                    Iterator i = this.newGateways.iterator();
                    while (i.hasNext()) {
                        TemplateHash hash = (TemplateHash) i.next();
                        addresses.append("'" + hash.get("addr") + "'");
                        if (i.hasNext()) {
                            addresses.append(", ");
                        }
                    }
                    ps = con.prepareStatement("UPDATE net_gateways SET flag = 0 WHERE addr IN (" + addresses.toString() + ")");
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                    con.close();
                } catch (SQLException e) {
                    Session.getLog().debug("Error updating network gateway", e);
                    Session.closeStatement(ps);
                    con.close();
                }
                this.newGateways.clear();
            }
        }
        return result;
    }

    public ArrayList getDelNetworkGateways() throws Exception {
        ArrayList result = null;
        synchronized (this.delGateways) {
            if (!this.delGateways.isEmpty()) {
                result = new ArrayList(this.delGateways);
                Connection con = Session.getDb();
                PreparedStatement ps = null;
                try {
                    StringBuffer addresses = new StringBuffer();
                    Iterator i = this.delGateways.iterator();
                    while (i.hasNext()) {
                        addresses.append("'" + ((String) i.next()) + "'");
                        if (i.hasNext()) {
                            addresses.append(", ");
                        }
                    }
                    ps = con.prepareStatement("DELETE FROM net_gateways WHERE addr IN (" + addresses.toString() + ")");
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                    con.close();
                } catch (SQLException e) {
                    Session.getLog().debug("Error deleting network gateway", e);
                    Session.closeStatement(ps);
                    con.close();
                }
                this.delGateways.clear();
            }
        }
        return result;
    }

    public void addNetworkGateway(String addr, String gateway, String mask) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO net_gateways (addr, gateway, mask, flag) VALUES(?, ?, ?, 1)");
                ps.setString(1, addr);
                ps.setString(2, gateway);
                ps.setString(3, mask);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                TemplateHash record = new TemplateHash();
                record.put("addr", addr);
                record.put("gateway", gateway);
                record.put("mask", mask);
                synchronized (this.newGateways) {
                    this.newGateways.add(record);
                }
                NetworkGateway netGateway = new NetworkGateway(gateway, mask);
                this.gateways.put(addr, netGateway);
                JobManager jobManager = C0004CP.getJobManager();
                if (jobManager.isJobDisabled("vpsReconfig")) {
                    jobManager.FM_enableJob("vpsReconfig");
                }
                if (jobManager.getAliveJobCount("vpsReconfig") == 0) {
                    jobManager.FM_startJob("vpsReconfig");
                }
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting network gateway", e);
                throw new HSUserException("eeman.net_gateway.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteNetworkGateway(String addr) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE net_gateways SET flag = 2 WHERE addr = ?");
            ps.setString(1, addr);
            ps.executeUpdate();
            this.gateways.remove(addr);
            Session.closeStatement(ps);
            con.close();
            synchronized (this.delGateways) {
                this.delGateways.add(addr);
            }
            JobManager jobManager = C0004CP.getJobManager();
            if (jobManager.isJobDisabled("vpsReconfig")) {
                jobManager.FM_enableJob("vpsReconfig");
            }
            if (jobManager.getAliveJobCount("vpsReconfig") == 0) {
                jobManager.FM_startJob("vpsReconfig");
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static NetworkGatewayManager getManager() {
        return gatewayManager;
    }

    public void assignDevice(long serverId, String addr, String device) throws Exception {
        NetworkGateway gateway = (NetworkGateway) this.gateways.get(addr);
        if (gateway != null) {
            HostEntry he = HostManager.getHost(serverId);
            he.exec("vps-subnets-change.pl", new String[]{"--addr=" + addr, "--mask=" + gateway.mask, "--gateway=" + gateway.gateway, "--device=" + device});
            return;
        }
        throw new HSUserException("Can't assign device. Gateway doesn't exist");
    }

    /* loaded from: hsphere.zip:psoft/hsphere/admin/NetworkGatewayManager$NetworkGateway.class */
    public class NetworkGateway {
        protected String gateway;
        protected String mask;

        NetworkGateway(String gateway, String mask) {
            NetworkGatewayManager.this = r4;
            this.gateway = gateway;
            this.mask = mask;
        }
    }
}
