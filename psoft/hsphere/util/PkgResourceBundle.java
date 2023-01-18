package psoft.hsphere.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import sun.misc.SoftCache;

/* loaded from: hsphere.zip:psoft/hsphere/util/PkgResourceBundle.class */
public class PkgResourceBundle extends ResourceBundle {
    protected String bundleName;
    protected PropertyResourceBundle baseBundle;
    protected List bundles;
    protected static SoftCache cache = new SoftCache();
    protected Locale locale;

    @Override // java.util.ResourceBundle
    public Locale getLocale() {
        return this.locale;
    }

    public PkgResourceBundle(String baseName, Locale locale) {
        this(baseName, locale, null);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale, List pkgBundles) {
        synchronized (PkgResourceBundle.class) {
            ResourceBundle bundle = (ResourceBundle) cache.get(baseName + "_" + locale.toString());
            if (bundle != null) {
                return bundle;
            }
            ResourceBundle bundle2 = new PkgResourceBundle(baseName, locale, pkgBundles);
            cache.put(baseName + "_" + locale.toString(), bundle2);
            return bundle2;
        }
    }

    public PkgResourceBundle(String baseName, Locale locale, List pkgBundles) {
        ResourceBundle rbPakage;
        this.locale = locale;
        try {
            this.baseBundle = (PropertyResourceBundle) PropertyResourceBundle.getBundle(baseName, locale);
        } catch (NullPointerException e) {
        } catch (MissingResourceException e2) {
        }
        if (pkgBundles != null && pkgBundles.size() > 0) {
            List localBundles = new LinkedList();
            Iterator i = pkgBundles.iterator();
            while (i.hasNext()) {
                try {
                    String pkgBaseName = (String) i.next();
                    if (getLocale() != null) {
                        rbPakage = PropertyResourceBundle.getBundle(pkgBaseName, getLocale());
                    } else {
                        rbPakage = PropertyResourceBundle.getBundle(pkgBaseName);
                    }
                    if (rbPakage != null) {
                        localBundles.add(rbPakage);
                    }
                } catch (NullPointerException e3) {
                } catch (MissingResourceException e4) {
                }
            }
            if (localBundles.size() > 0) {
                this.bundles = localBundles;
            }
        }
    }

    @Override // java.util.ResourceBundle
    public Object handleGetObject(String key) {
        Object value;
        if (this.baseBundle != null && (value = this.baseBundle.handleGetObject(key)) != null) {
            return value;
        }
        if (this.bundles != null) {
            for (PropertyResourceBundle rb : this.bundles) {
                Object value2 = rb.handleGetObject(key);
                if (value2 != null) {
                    return value2;
                }
            }
            return null;
        }
        return null;
    }

    @Override // java.util.ResourceBundle
    public Enumeration getKeys() {
        TreeSet tmpSet = new TreeSet();
        if (this.baseBundle != null) {
            while (this.baseBundle.getKeys().hasMoreElements()) {
                tmpSet.add(this.baseBundle.getKeys().nextElement());
            }
        }
        if (this.bundles != null) {
            for (PropertyResourceBundle rb : this.bundles) {
                while (rb.getKeys().hasMoreElements()) {
                    tmpSet.add(rb.getKeys().nextElement());
                }
            }
        }
        return new PkgResourceBundleEnumeration(tmpSet, this.parent != null ? this.parent.getKeys() : null);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/util/PkgResourceBundle$PkgResourceBundleEnumeration.class */
    class PkgResourceBundleEnumeration implements Enumeration {
        Set set;
        Object next = null;
        Iterator iterator;
        Enumeration enumeration;

        public PkgResourceBundleEnumeration(Set set, Enumeration enumeration) {
            PkgResourceBundle.this = r4;
            this.set = set;
            this.iterator = set.iterator();
            this.enumeration = enumeration;
        }

        @Override // java.util.Enumeration
        public boolean hasMoreElements() {
            if (this.next == null) {
                if (this.iterator.hasNext()) {
                    this.next = this.iterator.next();
                } else if (this.enumeration != null) {
                    while (this.next == null && this.enumeration.hasMoreElements()) {
                        this.next = this.enumeration.nextElement();
                        if (this.set.contains(this.next)) {
                            this.next = null;
                        }
                    }
                }
            }
            return this.next != null;
        }

        @Override // java.util.Enumeration
        public Object nextElement() {
            if (hasMoreElements()) {
                Object result = this.next;
                this.next = null;
                return result;
            }
            throw new NoSuchElementException();
        }
    }
}
