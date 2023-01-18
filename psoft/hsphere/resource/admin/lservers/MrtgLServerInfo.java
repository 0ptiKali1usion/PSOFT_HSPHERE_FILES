package psoft.hsphere.resource.admin.lservers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.p001ds.NetSwitch;
import psoft.hsphere.p001ds.NetSwitchManager;
import psoft.hsphere.resource.admin.LogicalServerInfo;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/lservers/MrtgLServerInfo.class */
public class MrtgLServerInfo extends LogicalServerInfo {
    static final List availableIpTypes = Arrays.asList(String.valueOf(4));

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getInfo(long lserverId) throws Exception {
        List list = new ArrayList();
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        List<NetSwitch> netswitches = nsm.getNetSwitchesByMrtgHost(lserverId);
        for (NetSwitch ns : netswitches) {
            Hashtable infos = new Hashtable();
            infos.put("name", "net_switch");
            infos.put("value", Localizer.translateLabel("label.eeman.net_switch_dev_mes", new String[]{ns.getDevice(), String.valueOf(ns.getUsedPortNumber())}));
            list.add(infos);
        }
        return list;
    }

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public String getUsed(long lserverId) throws Exception {
        NetSwitchManager nsm = (NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class);
        try {
            List netswitches = nsm.getNetSwitchesByMrtgHost(lserverId);
            return netswitches.isEmpty() ? "0" : "1";
        } catch (Exception ex) {
            Session.getLog().debug("Error in method 'MrtgLServerInfo::getUsed(" + lserverId + ")' ", ex);
            return "-1";
        }
    }

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getIPTypes() throws Exception {
        return availableIpTypes;
    }
}
