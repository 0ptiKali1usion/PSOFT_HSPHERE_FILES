package psoft.hsphere.global;

import java.util.Iterator;
import javax.servlet.ServletRequest;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.plan_wizard.PlanValueSetter;

/* loaded from: hsphere.zip:psoft/hsphere/global/GlobalValueProvider.class */
public class GlobalValueProvider {
    public static int keyStatus(String key, Reseller r, boolean isDisabledDefault, boolean managedGlobally) throws Exception {
        String glbValue = managedGlobally ? Settings.get().getAdminGlobalValue(key) : Settings.get().getGlobalValue(key);
        if ("0".equals(glbValue)) {
            return 2;
        }
        if (r.getId() != 1 && managedGlobally) {
            String value = r.getGlobalValueHolder().getValue(Globals.GLB_VALUE_PREFIX + key);
            if ("0".equals(value)) {
                return 1;
            }
            if ("1".equals(value)) {
                return 0;
            }
        } else if ("1".equals(glbValue)) {
            return 0;
        }
        return isDisabledDefault ? 1 : 0;
    }

    public static int keyStatus(String key, Reseller r, GlobalObject go) throws Exception {
        if (go != null) {
            if (!go.isConfigured()) {
                return 3;
            }
            return keyStatus(key, r, go.isDisabledByDefault(), go.isManagedGlobally());
        }
        return keyStatus(key, r, false, true);
    }

    public static int keyStatus(String key, int reselPlanId, boolean isDisabledDefault) throws Exception {
        if (Session.getResellerId() != 1) {
            return 1;
        }
        if ("0".equals(Settings.get().getAdminGlobalValue(key))) {
            return 2;
        }
        String value = Reseller.getGlobalValueHolder(reselPlanId).getValue(Globals.GLB_VALUE_PREFIX + key);
        if ("0".equals(value)) {
            return 1;
        }
        return (!"1".equals(value) && isDisabledDefault) ? 1 : 0;
    }

    public static int keyStatus(String key, int reselPlanId, GlobalObject go) throws Exception {
        if (go != null) {
            if (!go.isConfigured()) {
                return 3;
            }
            return keyStatus(key, reselPlanId, go.isDisabledByDefault());
        }
        return keyStatus(key, reselPlanId, false);
    }

    public static int keyStatus(String key, ServletRequest rq, boolean managedGlobally) throws Exception {
        String glbValue = managedGlobally ? Settings.get().getAdminGlobalValue(key) : Settings.get().getGlobalValue(key);
        if ("0".equals(glbValue)) {
            return 2;
        }
        String value = rq.getParameter(key);
        if (value == null || "".equals(value)) {
            return 1;
        }
        return 0;
    }

    public static int keyStatus(String key, ServletRequest rq, GlobalObject go) throws Exception {
        if (go != null) {
            if (!go.isConfigured) {
                return 3;
            }
            return keyStatus(key, rq, go.isManagedGlobally());
        }
        return keyStatus(key, rq, true);
    }

    public static void updateGlobalSettings(ServletRequest rq) throws Exception {
        for (VisSection vs : Globals.getAccessor().getAllSections()) {
            String store = vs.getStore();
            if ("*".equals(store) || "settings".equals(store)) {
                for (GlobalObject go : vs.getObjects()) {
                    String key = go.getName();
                    String param = rq.getParameter(key);
                    updateSettingsValue(key, param);
                }
                for (GlobalKeySet gs : vs.getSets()) {
                    String prefix = gs.getPrefix();
                    Iterator kIter = gs.getAvailableKeys().iterator();
                    while (kIter.hasNext()) {
                        String key2 = prefix + ((String) kIter.next());
                        String param2 = rq.getParameter(key2);
                        updateSettingsValue(key2, param2);
                    }
                }
            }
        }
    }

    public static void updateGlobalSettings(ServletRequest rq, String section) throws Exception {
        VisSection vs = Globals.getAccessor().getSection(section);
        if (vs != null) {
            for (GlobalObject go : vs.getObjects()) {
                String key = go.getName();
                String param = rq.getParameter(key);
                updateSettingsValue(key, param);
            }
            for (GlobalKeySet gs : vs.getSets()) {
                String prefix = gs.getPrefix();
                Iterator kIter = gs.getAvailableKeys().iterator();
                while (kIter.hasNext()) {
                    String key2 = prefix + ((String) kIter.next());
                    String param2 = rq.getParameter(key2);
                    updateSettingsValue(key2, param2);
                }
            }
        }
    }

    protected static void updateSettingsValue(String key, String reqParameter) throws Exception {
        String setKey = Globals.GLB_VALUE_PREFIX + key;
        String lastValue = Settings.get().getValue(setKey);
        if (reqParameter == null || "".equals(reqParameter)) {
            Settings.get().setValue(setKey, "0");
        } else if (!"1".equals(lastValue)) {
            Settings.get().setValue(setKey, "1");
        }
    }

    public static void updatePlanValues(PlanValueSetter pvs, ServletRequest rq, boolean changeExtraParams) throws Exception {
        for (VisSection vs : Globals.getAccessor().getAllSections()) {
            String store = vs.getStore();
            String show = vs.getShow();
            if ("*".equals(store) || ("plans".equals(store) && (changeExtraParams || !"custom".equals(show)))) {
                for (GlobalObject go : vs.getObjects()) {
                    String key = go.getName();
                    String param = rq.getParameter(key);
                    updatePlanValue(pvs, key, param);
                }
                for (GlobalKeySet gs : vs.getSets()) {
                    String prefix = gs.getPrefix();
                    Iterator kIter = gs.getAvailableKeys().iterator();
                    while (kIter.hasNext()) {
                        String key2 = prefix + ((String) kIter.next());
                        String param2 = rq.getParameter(key2);
                        updatePlanValue(pvs, key2, param2);
                    }
                }
            }
        }
    }

    protected static void updatePlanValue(PlanValueSetter pvs, String key, String reqParameter) throws Exception {
        String toSet = (reqParameter == null || "".equals(reqParameter)) ? "0" : "1";
        pvs.setValue(Globals.GLB_VALUE_PREFIX + key, toSet);
    }
}
