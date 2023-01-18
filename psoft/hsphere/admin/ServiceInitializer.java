package psoft.hsphere.admin;

import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.mail.MailClient;

/* loaded from: hsphere.zip:psoft/hsphere/admin/ServiceInitializer.class */
public class ServiceInitializer {
    protected static Map map;

    public static void init() {
        map = new HashMap();
        map.put("ttmail", MailClient.getInstance());
    }

    public static void update(String key) {
        Object o = map.get(key);
        if (o != null && (o instanceof UpdatableService)) {
            ((UpdatableService) o).update();
        }
    }
}
