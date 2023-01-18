package psoft.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import psoft.hsphere.cron.CronManager;

/* loaded from: hsphere.zip:psoft/util/NFUCache.class */
public class NFUCache implements GenericCache {
    protected Hashtable cache;
    protected long maxSize;
    protected long initialSize;
    protected long systemMax;
    protected long readCounter;
    protected long hitCounter;
    protected double hitRate;
    protected AgingThread aThread;
    protected SoftHashMap softMap;
    public static final int REMOVE_PERCENT = 30;

    public String toString() {
        return this.cache.toString();
    }

    public NFUCache(int maxSize) {
        this(maxSize, CronManager.MULTIPLIER, 0L);
    }

    public NFUCache(int maxSize, int timeOut) {
        this(maxSize, timeOut, 0L);
    }

    public NFUCache(long maxSize, int timeOut, long systemMax) {
        this.maxSize = maxSize;
        this.initialSize = maxSize;
        this.systemMax = systemMax;
        this.readCounter = 0L;
        this.hitCounter = 0L;
        this.hitRate = 0.0d;
        this.cache = new Hashtable();
        this.softMap = new SoftHashMap();
        this.aThread = new AgingThread(timeOut);
        this.aThread.start();
    }

    @Override // psoft.util.GenericCache
    public synchronized Object get(Object key) {
        Object obj;
        if (key == null) {
            return null;
        }
        NFUEntry entry = (NFUEntry) this.cache.get(key);
        this.readCounter++;
        if (entry != null) {
            obj = entry.get();
        } else {
            obj = this.softMap.get(key);
            if (obj != null) {
                put(key, obj);
            }
        }
        if (obj != null) {
            this.hitCounter++;
        }
        return obj;
    }

    public synchronized void remove(Object key) {
        NFUEntry entry = (NFUEntry) this.cache.get(key);
        if (entry != null) {
            entry.invalidate();
            this.cache.remove(key);
            this.softMap.remove(key);
        }
    }

    public synchronized void removeAll(Map keys) {
        for (Object key : keys.keySet()) {
            NFUEntry entry = (NFUEntry) keys.get(key);
            if (entry != null) {
                entry.invalidate();
                this.cache.remove(key);
                this.softMap.remove(key);
            }
        }
    }

    public void reload() {
        synchronized (this) {
            this.softMap.clear();
        }
        for (NFUEntry nFUEntry : this.cache.values()) {
            nFUEntry.reload();
        }
    }

    public void invalidate() {
        synchronized (this) {
            this.softMap.clear();
        }
        for (NFUEntry e : this.cache.values()) {
            e.invalidate();
        }
    }

    protected synchronized Map findOldest() {
        SortedSet<NFUEntry> oldest = Collections.synchronizedSortedSet(new TreeSet());
        long oldestSize = (this.cache.size() * 30) / 100;
        if (oldestSize == 0) {
            return null;
        }
        Enumeration e = this.cache.keys();
        while (e.hasMoreElements()) {
            NFUEntry entry = (NFUEntry) this.cache.get(e.nextElement());
            if (!entry.locked() && (oldest.size() < oldestSize || entry.getAge() < ((NFUEntry) oldest.last()).getAge())) {
                if (oldest.size() >= oldestSize) {
                    Object del = oldest.last();
                    oldest.remove(del);
                }
                try {
                    oldest.add(entry);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (oldest.size() <= 0) {
            return null;
        }
        Map keys = new HashMap();
        for (NFUEntry entry2 : oldest) {
            keys.put(entry2.getKey(), entry2);
        }
        return keys;
    }

    public synchronized void clear() {
        this.cache.clear();
        this.softMap.clear();
    }

    @Override // psoft.util.GenericCache
    public synchronized void put(Object key, Object value) {
        Map removeKey;
        while (this.cache.size() > this.maxSize && (removeKey = findOldest()) != null) {
            removeAll(removeKey);
        }
        NFUEntry entry = (NFUEntry) this.cache.get(key);
        if (entry == null) {
            if (this.cache.size() >= this.maxSize) {
                Map removeKey2 = findOldest();
                if (removeKey2 != null) {
                    removeAll(removeKey2);
                    entry = new NFUEntry(key);
                } else if (this.cache.size() >= 10 * this.maxSize) {
                    throw new Error("Insufficient cache size");
                } else {
                    entry = new NFUEntry(key);
                }
            } else {
                entry = new NFUEntry(key);
            }
            this.cache.put(key, entry);
        } else if (value.equals(entry.get())) {
            return;
        }
        setEntry(entry, value);
        this.softMap.put(key, value);
    }

    protected void setEntry(NFUEntry entry, Object value) {
        entry.set(value);
    }

    public Collection getKeys() {
        return this.cache.keySet();
    }

    protected void finalize() throws Throwable {
        this.aThread.die();
        super.finalize();
    }

    public double getHitRate() {
        return this.hitRate;
    }

    public long getMaxSize() {
        return this.maxSize;
    }

    public long getSize() {
        return this.cache.size();
    }

    public String getElements() {
        return this.cache.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/NFUCache$AgingThread.class */
    public class AgingThread extends Thread {
        protected int timeOut;
        boolean cont;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AgingThread(int timeOut) {
            super("NFUCache");
            NFUCache.this = r4;
            this.timeOut = timeOut;
            this.cont = true;
            setDaemon(true);
        }

        public void die() {
            this.cont = false;
            interrupt();
        }

        /*  JADX ERROR: JadxOverflowException in pass: RegionMakerVisitor
            jadx.core.utils.exceptions.JadxOverflowException: Regions count limit reached
            	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:56)
            	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:30)
            	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:18)
            */
        /* JADX WARN: Removed duplicated region for block: B:55:0x0026  */
        /* JADX WARN: Removed duplicated region for block: B:59:0x008b  */
        /* JADX WARN: Removed duplicated region for block: B:74:0x0152  */
        @Override // java.lang.Thread, java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public void run() {
            /*
                Method dump skipped, instructions count: 374
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: psoft.util.NFUCache.AgingThread.run():void");
        }
    }

    public String getInfo() {
        return "initial size:" + this.initialSize + " size:" + getSize() + " max size:" + getMaxSize() + " rate:" + getHitRate() + " ";
    }
}
