package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.exception.allocation.PServerNotMeetAllocateRequirementsException;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.hsphere.resource.admin.PhysicalServer;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.hsphere.resource.allocation.AllocatedPServerHolder;
import psoft.hsphere.resource.allocation.AllocatedServerManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/AllocatedServerReport.class */
public class AllocatedServerReport extends AdvReport {
    private static final Category log = Category.getInstance(AllocatedServerReport.class.getName());

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        List data = new ArrayList();
        PreparedStatement ps = null;
        Session.getLog().debug("Inside AllocatedPServerReport::init");
        boolean showUnassigned = ((Integer) args.get(0)).intValue() > 0;
        boolean showAvailable = ((Integer) args.get(1)).intValue() > 0;
        boolean showTaken = ((Integer) args.get(2)).intValue() > 0;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT p.id, a.id from p_server as p LEFT OUTER JOIN allocated_pserver as a ON p.id=a.id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AllocatedPServer aps = null;
                HashMap map = null;
                PhysicalServer pserver = PhysicalServer.get(rs.getLong(1));
                if (rs.getLong(2) > 0) {
                    aps = AllocatedPServerHolder.getInstance().get(rs.getLong(2));
                }
                if (aps != null) {
                    if (aps.getState() == 1 && showAvailable) {
                        map = new HashMap();
                        Plan p = Plan.getPlan(aps.getPlanId());
                        map.put("ps_id", new Long(pserver.getId()));
                        map.put("ps_name", pserver.getName());
                        map.put("plan_id", new Integer(p.getId()));
                        map.put("plan_name", p.getDescription());
                        map.put("state_description", Localizer.translateMessage("msg.allocated.state.available"));
                        map.put("state", "1");
                    } else if (aps.getState() == 2 && showTaken) {
                        map = new HashMap();
                        Plan p2 = Plan.getPlan(aps.getPlanId());
                        map.put("ps_id", new Long(pserver.getId()));
                        map.put("ps_name", pserver.getName());
                        map.put("plan_id", new Integer(p2.getId()));
                        map.put("plan_name", p2.getDescription());
                        map.put("account_id", new Long(aps.getUsedBy().getAccountId()));
                        map.put("state_description", Localizer.translateMessage("msg.allocated.state.taken"));
                        map.put("state", "2");
                        map.put("username", aps.getUsedBy().getAccount().getUser().getLogin());
                    }
                } else if (showUnassigned) {
                    boolean meetsRequirements = false;
                    try {
                        meetsRequirements = AllocatedServerManager.getInstance().meetRequirements(pserver);
                    } catch (PServerNotMeetAllocateRequirementsException psReqEx) {
                        if (psReqEx.getTypeOfInconsistence() == 3) {
                            map = new HashMap();
                            map.put("in_use", "1");
                            map.put("ps_id", new Long(pserver.getId()));
                            map.put("ps_name", pserver.getName());
                            map.put("state_description", Localizer.translateMessage("msg.allocated.state.other"));
                            map.put("state", "-1");
                        }
                    }
                    if (meetsRequirements) {
                        map = new HashMap();
                        map.put("ps_id", new Long(pserver.getId()));
                        map.put("ps_name", pserver.getName());
                        map.put("state_description", Localizer.translateMessage("msg.allocated.state.unassigned"));
                        map.put("state", "0");
                    }
                }
                if (map != null) {
                    data.add(map);
                }
            }
            init(new DataContainer(data));
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
