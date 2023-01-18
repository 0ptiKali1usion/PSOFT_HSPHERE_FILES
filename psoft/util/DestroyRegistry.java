package psoft.util;

import java.util.Hashtable;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/util/DestroyRegistry.class */
public class DestroyRegistry {
    protected Hashtable hash = new Hashtable();

    public void register(Destroyable d) {
        if (null == this.hash.get(d)) {
            this.hash.put(d, d);
        }
    }

    protected static Category getLog() {
        return Config.getLog("OTHER");
    }

    public void destroy() {
        for (Destroyable destroyable : this.hash.values()) {
            destroyable.destroy();
        }
    }
}
