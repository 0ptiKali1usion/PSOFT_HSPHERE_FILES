package psoft.hsphere.resource.allocation;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.exception.PhysicalServerNotExist;
import psoft.hsphere.exception.allocation.NoAvailableAllocatedServerForPlan;
import psoft.hsphere.exception.allocation.PServerNotMeetAllocateRequirementsException;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.resource.admin.LServerInfo;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.admin.PhysicalServer;

/* loaded from: hsphere.zip:psoft/hsphere/resource/allocation/AllocatedServerManager.class */
public class AllocatedServerManager {
    private static AllocatedServerManager ourInstance = new AllocatedServerManager();
    public static final List supportedServices = Arrays.asList(new Integer(1), new Integer(3), new Integer(4));

    public static AllocatedServerManager getInstance() {
        return ourInstance;
    }

    private AllocatedServerManager() {
    }

    public AllocatedPServer allocateServer(long pServerId, int planId) throws Exception {
        PhysicalServer ps = PhysicalServer.get(pServerId);
        if (ps == null) {
            throw new PhysicalServerNotExist(pServerId);
        }
        meetRequirements(ps);
        return AllocatedPServerHolder.getInstance().add(ps.getId(), planId);
    }

    public boolean meetRequirements(PhysicalServer ps) throws Exception {
        Collection pServerGroups = ps.getGroups();
        Collection<String> lservers = ps.getLServers();
        if (pServerGroups.size() != 3) {
            throw new PServerNotMeetAllocateRequirementsException(ps.getId());
        }
        if (!pServerGroups.contains("1")) {
            throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 1, 1);
        }
        if (!pServerGroups.contains("3")) {
            throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 3, 1);
        }
        if (!pServerGroups.contains("4")) {
            throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 4, 1);
        }
        boolean hasLWeb = false;
        boolean hasLMail = false;
        boolean hasLMySQL = false;
        for (String str : lservers) {
            long lServerId = Long.parseLong(str);
            LogicalServer ls = LogicalServer.get(lServerId);
            if (ls.getGroup() == 1) {
                hasLWeb = true;
            }
            if (ls.getGroup() == 3) {
                hasLMail = true;
            }
            if (ls.getGroup() == 4) {
                hasLMySQL = true;
            }
            if (hasLWeb && hasLMail && hasLMySQL) {
                break;
            }
        }
        if (hasLWeb) {
            if (hasLMail) {
                if (hasLMySQL) {
                    for (String str2 : lservers) {
                        long lServerId2 = Long.parseLong(str2);
                        LogicalServer ls2 = LogicalServer.get(lServerId2);
                        LServerInfo lsi = new LServerInfo(ls2.getId(), ls2.getGroup());
                        if (!"0".equals(lsi.get("used").toString())) {
                            throw new PServerNotMeetAllocateRequirementsException(ps.getId(), ls2.getId());
                        }
                    }
                    return true;
                }
                throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 4, 2);
            }
            throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 3, 2);
        }
        throw new PServerNotMeetAllocateRequirementsException(ps.getId(), 1, 2);
    }

    public AllocatedPServer takeRandomAvailableAllocatedServer(long resellerId, ResourceId rid) throws UnknownResellerException, NoAvailableAllocatedServerForPlan, SQLException {
        return AllocatedPServerHolder.getInstance().takeRandomAvailableAllocatedServer(resellerId, rid);
    }

    public boolean isAllocated(long psId) {
        return AllocatedPServerHolder.getInstance().isAllocated(psId);
    }

    public AdvReport scan(List args) throws Exception {
        AdvReport rep = AdvReport.newInstance("allocatedserver");
        rep.init(args);
        return rep;
    }

    public void assignToPlan(long pServerId, int planId) throws Exception {
        AllocatedPServer aps = AllocatedPServerHolder.getInstance().get(pServerId);
        if (aps != null) {
            aps.setPlanId(planId);
            AllocatedPServerHolder.getInstance().updateAllocatedPServer(aps);
            return;
        }
        allocateServer(pServerId, planId);
    }

    public void unassignPServer(long apsId) throws SQLException {
        synchronized (this) {
            AllocatedPServer aps = AllocatedPServerHolder.getInstance().get(apsId);
            if (aps != null && aps.getState() == 1) {
                AllocatedPServerHolder.getInstance().unassignPServer(apsId);
            }
        }
    }

    public void releaseTakenAllocatedPServer(long apsId) throws SQLException {
        AllocatedPServer aps = AllocatedPServerHolder.getInstance().get(apsId);
        if (aps != null && aps.getState() == 2) {
            aps.setResellerId(0L);
            aps.setUsedBy(null);
            AllocatedPServerHolder.getInstance().updateAllocatedPServer(aps);
        }
    }

    public AdvReport getscanReport(int arid) throws Exception {
        AdvReport rep = AdvReport.getReport(arid);
        return rep;
    }
}
