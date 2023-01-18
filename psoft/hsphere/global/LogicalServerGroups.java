package psoft.hsphere.global;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/global/LogicalServerGroups.class */
public class LogicalServerGroups extends AnyGlobalSet implements GlobalKeySet {
    static final String prefix = "SERVER_GROUP_";
    public static final String UNIX_PLATFORM = "unix_platform";
    public static final String WINDOWS_PLATFORM = "windows_platform";

    public LogicalServerGroups(String name, String label, String section, Boolean isDisabledByDefault, Boolean isConfigured, Boolean managedGlobally) throws Exception {
        super(name, label, section, isDisabledByDefault, isConfigured, managedGlobally);
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public boolean isKeyAvailable(String lsgId) throws Exception {
        String platform = getPlatformByKey(lsgId);
        return HostManager.areThereSignupHosts(Integer.parseInt(lsgId)) && (platform == null || Globals.isObjectDisabled(platform) == 0);
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public List getAvailableKeys() throws Exception {
        TemplateList allGroups = EnterpriseManager.getLServerGroups();
        List result = new ArrayList();
        while (allGroups.hasNext()) {
            TemplateMap group = allGroups.next();
            String id = group.get("id").toString();
            if (isKeyAvailable(id)) {
                result.add(id);
            }
        }
        return result;
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getPrefix() {
        return prefix;
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getKeyDescription(String key) throws Exception {
        if (isKeyDisabled(key) != 0) {
            return Localizer.translateMessage("lsg.unavailable");
        }
        return EnterpriseManager.getGroupNameById(Integer.parseInt(key));
    }

    public static String getPlatformByKey(String lsgId) throws Exception {
        int groupType = HostManager.getTypeByGroup(Integer.parseInt(lsgId));
        switch (groupType) {
            case 1:
                return UNIX_PLATFORM;
            case 5:
                return WINDOWS_PLATFORM;
            default:
                return null;
        }
    }

    public TemplateModel FM_getPlatform(String key) throws Exception {
        String platform = getPlatformByKey(key);
        if (platform != null) {
            return new TemplateString(platform);
        }
        return null;
    }
}
