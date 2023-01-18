package psoft.hsphere.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Category;
import psoft.hsphere.cache.storedobjects.StorableCache;
import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/hsphere/cache/CacheFactory.class */
public class CacheFactory {
    private Hashtable caches = new Hashtable();
    private NFUCache nfu;
    private String dirToStore;
    private static CacheFactory ourInstance = new CacheFactory();
    private static Category log = null;

    public void setNfu(NFUCache nfu) {
        this.nfu = nfu;
    }

    public static CacheFactory getInstance() {
        return ourInstance;
    }

    public void registerCache(Class cache) throws Exception {
        if (this.nfu == null) {
            throw new Exception("NFU Cache is not initialized");
        }
        if (Cache.class.isAssignableFrom(cache)) {
            Cache cacheInstance = (Cache) cache.newInstance();
            Class cacheableInterface = cacheInstance.getCacheInterface();
            if (!this.caches.keySet().contains(cacheableInterface)) {
                cacheInstance.setCache(this.nfu);
                this.caches.put(cacheableInterface, cacheInstance);
            }
        }
    }

    public Cache getCache(Class cacheableInterface) {
        return (Cache) this.caches.get(cacheableInterface);
    }

    public LockableCache getLockableCache(Class cacheableInterface) {
        return (LockableCache) this.caches.get(cacheableInterface);
    }

    public synchronized void store() {
        Enumeration e = this.caches.keys();
        while (e.hasMoreElements()) {
            Class cacheName = (Class) e.nextElement();
            Cache cache = (Cache) this.caches.get(cacheName);
            if (cache instanceof StorableCache) {
                ObjectOutputStream outputStreamByName = null;
                try {
                    try {
                        outputStreamByName = getOutputStreamByName(cacheName.getName());
                        ((StorableCache) cache).store(outputStreamByName);
                        if (outputStreamByName != null) {
                            try {
                                outputStreamByName.close();
                            } catch (IOException e1) {
                                getLog().error("Unable to store Cache:" + cacheName.getName(), e1);
                            }
                        }
                    } catch (Throwable th) {
                        if (outputStreamByName != null) {
                            try {
                                outputStreamByName.close();
                            } catch (IOException e12) {
                                getLog().error("Unable to store Cache:" + cacheName.getName(), e12);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e13) {
                    getLog().error("Unable to store Cache:" + cacheName.getName(), e13);
                    if (outputStreamByName != null) {
                        try {
                            outputStreamByName.close();
                        } catch (IOException e14) {
                            getLog().error("Unable to store Cache:" + cacheName.getName(), e14);
                        }
                    }
                }
            }
        }
    }

    public void setDirToStore(String dirToStore) {
        this.dirToStore = dirToStore;
    }

    public String getDirToStore() {
        return this.dirToStore;
    }

    public String getFileToStoreByName(String name) {
        return this.dirToStore + "/" + name + ".stored";
    }

    protected ObjectInputStream getInputStreamByName(String name) throws IOException {
        File storedDir = new File(getDirToStore());
        if (!storedDir.exists()) {
            throw new IOException("Directory " + getDirToStore() + " to store cache doesn't exists. Skipping restoring ...");
        }
        File storedFile = new File(getFileToStoreByName(name));
        if (!storedFile.exists()) {
            throw new IOException("File " + storedFile.getAbsolutePath() + " to store cache doesn't exists. Skipping restoring ...");
        }
        FileInputStream fis = new FileInputStream(getFileToStoreByName(name));
        return new ObjectInputStream(fis);
    }

    public synchronized void restore() {
        Enumeration e = this.caches.keys();
        while (e.hasMoreElements()) {
            Class cacheName = (Class) e.nextElement();
            Cache cache = (Cache) this.caches.get(cacheName);
            if (cache instanceof StorableCache) {
                ObjectInputStream inputStreamByName = null;
                try {
                    try {
                        inputStreamByName = getInputStreamByName(cacheName.getName());
                        ((StorableCache) cache).restore(inputStreamByName);
                        if (inputStreamByName != null) {
                            try {
                                inputStreamByName.close();
                            } catch (IOException e1) {
                                getLog().error("Unable to restore Cache:" + cacheName.getName(), e1);
                            }
                        }
                    } catch (IOException e12) {
                        getLog().error("Unable to restore Cache:" + cacheName.getName(), e12);
                        if (inputStreamByName != null) {
                            try {
                                inputStreamByName.close();
                            } catch (IOException e13) {
                                getLog().error("Unable to restore Cache:" + cacheName.getName(), e13);
                            }
                        }
                    } catch (ClassNotFoundException e14) {
                        getLog().error("Unable to restore Cache:" + cacheName.getName(), e14);
                        if (inputStreamByName != null) {
                            try {
                                inputStreamByName.close();
                            } catch (IOException e15) {
                                getLog().error("Unable to restore Cache:" + cacheName.getName(), e15);
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (inputStreamByName != null) {
                        try {
                            inputStreamByName.close();
                        } catch (IOException e16) {
                            getLog().error("Unable to restore Cache:" + cacheName.getName(), e16);
                        }
                    }
                    throw th;
                }
            }
        }
    }

    protected ObjectOutputStream getOutputStreamByName(String name) throws IOException {
        File outputDir = new File(getDirToStore());
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        File storedFile = new File(getFileToStoreByName(name));
        if (storedFile.exists()) {
            storedFile.delete();
        }
        FileOutputStream fis = new FileOutputStream(getFileToStoreByName(name));
        return new ObjectOutputStream(fis);
    }

    protected synchronized Category getLog() {
        if (log == null) {
            log = Category.getInstance(getClass().getName());
        }
        return log;
    }
}
