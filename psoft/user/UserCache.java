package psoft.user;

import psoft.hsphere.cron.CronManager;
import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/user/UserCache.class */
public class UserCache {
    protected static NFUCache cache = new NFUCache(100, CronManager.MULTIPLIER);

    public static void put(Object key, User usr) {
        cache.put(key, usr);
    }

    public static User get(Object key) {
        if (key == null) {
            return null;
        }
        Object obj = cache.get(key);
        try {
            return (User) obj;
        } catch (ClassCastException e) {
            return null;
        }
    }
}
