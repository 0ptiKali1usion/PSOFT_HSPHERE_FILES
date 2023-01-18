package psoft.hsphere.servmon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.User;
import psoft.hsphere.cron.CronManager;
import psoft.hsphere.resource.PhysicalServer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/servmon/MonitoringThread.class */
public class MonitoringThread extends Thread {

    /* renamed from: cp */
    protected C0004CP f218cp;
    int delay = CronManager.MULTIPLIER;
    static HashMap servInfo = new HashMap();

    public static ServerInfo getServerInfo(Long id) {
        return (ServerInfo) servInfo.get(id);
    }

    public static ServerInfo getServerInfo(long id) {
        return getServerInfo(new Long(id));
    }

    public MonitoringThread(C0004CP cp) {
        this.f218cp = cp;
        setDaemon(true);
    }

    /* JADX WARN: Finally extract failed */
    protected void monitorServers() throws Exception {
        List<Long> list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM p_server where get_server_info = 1");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Long(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            for (Long serverId : list) {
                ServerInfo serverInfo = (ServerInfo) servInfo.get(serverId);
                if (serverInfo == null) {
                    serverInfo = new ServerInfo();
                    servInfo.put(serverId, serverInfo);
                }
                try {
                    PhysicalServer.getPServer(serverId.longValue()).monitor(serverInfo);
                } catch (Exception e) {
                    Session.getLog().warn("Error doing monitor for server: " + serverId, e);
                }
            }
            PhysicalServer.checkServer();
            PhysicalServer.reformLServerToPServer();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void die() {
        interrupt();
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        this.f218cp.setConfig();
        this.f218cp.initLog();
        Session.getLog().info("Monitor daemon started");
        while (true) {
            try {
                monitorServers();
                TimeUtils.sleep(this.delay);
                Session.getLog().debug("Resource NFU cache:" + Resource.getCache().getInfo());
                Session.getLog().debug("SharedObject NFU cache:" + SharedObject.getCache().getInfo());
                Session.getLog().debug("User NFU cache:" + User.getCache().getInfo());
            } catch (InterruptedException e) {
                return;
            } catch (Exception e2) {
                Session.getLog().warn("Error while server monitoring", e2);
            }
        }
    }
}
