package psoft.hsphere.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsp.Package;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/util/PackageConfigurator.class */
public class PackageConfigurator {
    private static final String HOME = "/hsphere/local/home/cpanel/shiva";
    private static final String JAVA_RT_HOME = "/hsphere/local/home/cpanel/java_rt";
    protected static final String CUSTOM_PREFIX = "CUSTOM_";
    private static List packages = null;
    private static HashMap defaultValues = new HashMap();
    private static boolean arePackagesInstalled = false;
    private static boolean checkedPackagesInstalled = false;
    private static String hsphereHome = null;
    private static String javaRtHome = null;
    protected static final List availableCustomXMLs = Arrays.asList("MENU_CONFIG", "DESIGN_SCHEME_CONFIG", "HELP_CONFIG", "ONLINE_HELP_CONFIG", "CRON_CONFIG", "PROMO_CONFIG", "USER_EMAILS", "LSERVER_INFO");

    static {
        defaultValues.put("PLAN_WIZARDS_DIR", "/psoft/hsphere/plan/wizard/xml");
        defaultValues.put("PLAN_WIZARDS", "/psoft/hsphere/plan/wizard/xml/plan_wizards.xml");
        defaultValues.put("MENU_CONFIG", "/psoft/hsphere/menu.xml");
        defaultValues.put("DESIGN_SCHEME_CONFIG", "/psoft/hsphere/design_config.xml");
        defaultValues.put("HELP_CONFIG", "/psoft/hsphere/help.xml");
        defaultValues.put("ONLINE_HELP_CONFIG", "/psoft/hsphere/help_url.xml");
        defaultValues.put("PATH_TO_MERGE_XSLT", "/psoft/util/merge.xslt");
        defaultValues.put("CRON_CONFIG", "/psoft_config/cron_config.xml");
        defaultValues.put("MERCHANT_GATEWAYS_CONF", "/psoft/hsphere/merchants.xml");
        defaultValues.put("ACL_OBJECTS", "/psoft/hsphere/acl_objects.xml");
        defaultValues.put("REGISTRAR_CONF", "/psoft/hsphere/registrar.xml");
        defaultValues.put("HSPHERE_HOME", "");
        defaultValues.put("PACKAGE_STORE", "/packages");
        defaultValues.put("PHYSICAL_SERVERS_PARAMS_FILE", "/psoft/hsphere/physical-server-params.xml");
        defaultValues.put("MAIL_SERVERS_PARAMS_FILE", "/psoft/hsphere/mail-server-params.xml");
        defaultValues.put("UTILS", "/psoft/hsphere/functions/functions.xml");
        defaultValues.put("ADV_REPORTS", "/psoft/hsphere/report/adv/adv_reports.xml");
        defaultValues.put("PROMO_CONFIG", "/psoft/hsphere/promotion/xml/promotions.xml");
        defaultValues.put("USER_EMAILS", "/psoft/hsphere/user_emails.xml");
        defaultValues.put("USER_TEMPLATE_PATH", "/custom/templates");
        defaultValues.put("USER_IMAGE_PATH", "/custom/images");
        defaultValues.put("LSERVER_INFO", "/psoft/hsphere/resource/admin/lserver_info.xml");
        defaultValues.put("GLOBALS_CONFIG", "/psoft/hsphere/globals.xml");
        defaultValues.put("RESOURCE_DEPENDENCES_CONFIG", "/psoft/hsphere/resource_dependences.xml");
        defaultValues.put("HOSTED_EXCHANGE_PLANS", "/psoft/hsphere/msExchangePlans.xml");
        defaultValues.put("LS_OPTIONS", "/psoft/hsphere/logical_server_options.xml");
    }

    public static synchronized String getDefaultValue(String key) {
        if (hsphereHome == null) {
            hsphereHome = Session.getPropertyString("HSPHERE_HOME");
            if (isEmpty(hsphereHome)) {
                hsphereHome = HOME;
            }
            defaultValues.put("HSPHERE_HOME", hsphereHome);
        }
        if (isEmpty(key) || "HSPHERE_HOME".equals(key)) {
            return hsphereHome;
        }
        String value = Session.getPropertyString(key);
        if (isEmpty(value) && defaultValues.get(key) != null) {
            value = hsphereHome + ((String) defaultValues.get(key));
        }
        return value;
    }

    public static synchronized String getJavaRtHome() {
        if (javaRtHome == null) {
            javaRtHome = Session.getPropertyString("JAVA_RT_HOME");
            if (isEmpty(javaRtHome)) {
                javaRtHome = JAVA_RT_HOME;
            }
        }
        return javaRtHome;
    }

    public static String getCustomValue(String key) {
        return Session.getPropertyString(CUSTOM_PREFIX + key);
    }

    protected static void checkFile(List l, File f) {
        Session.getLog().debug("File:" + f.getAbsolutePath());
        if (f.exists()) {
            l.add(f.getPath());
        }
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static Object getPackageProperty(String key) {
        Object tmpObj;
        for (String str : packages) {
            try {
                ResourceBundle rb = PropertyResourceBundle.getBundle(str);
                tmpObj = rb.getObject(key);
            } catch (NullPointerException e) {
            } catch (MissingResourceException e2) {
            }
            if (tmpObj != null) {
                return tmpObj;
            }
        }
        return null;
    }

    public static List getPackageProperties(String key) {
        List list = new LinkedList();
        try {
            for (Package pkg : getPackages()) {
                try {
                    ResourceBundle rb = PropertyResourceBundle.getBundle(pkg.getConfFile());
                    String tmpStr = rb.getString(key);
                    if (tmpStr != null) {
                        list.add(tmpStr.trim());
                    }
                } catch (NullPointerException e) {
                } catch (MissingResourceException e2) {
                }
            }
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get Packages", ex);
        }
        if (list.size() > 0) {
            return list;
        }
        return null;
    }

    public static boolean isXMLKey(String key) {
        String value = (String) defaultValues.get(key);
        if (value != null) {
            return value.endsWith(".xml");
        }
        return false;
    }

    public static List getXMLFilesList(String key) {
        String value;
        List list = new ArrayList();
        checkFile(list, new File(getDefaultValue(key)));
        if (isXMLKey(key) && (value = getCustomValue(key)) != null) {
            checkFile(list, new File(value));
        }
        for (Package pkg : getPackages()) {
            try {
                ResourceBundle rb = PropertyResourceBundle.getBundle(pkg.getConfFile());
                Session.getLog().debug("File:" + key + " Dir:" + pkg.getConfDirectory());
                checkFile(list, new File(pkg.getConfDirectory() + rb.getString(key)));
            } catch (NullPointerException e) {
            } catch (MissingResourceException e2) {
            }
        }
        return list;
    }

    public static List getXMLFileList(String key, String fileName) {
        List list = new ArrayList();
        String value = getDefaultValue(key);
        if (!isEmpty(value)) {
            checkFile(list, new File(value, fileName));
        }
        for (Package pkg : getPackages()) {
            try {
                ResourceBundle rb = PropertyResourceBundle.getBundle(pkg.getConfFile());
                checkFile(list, new File(pkg.getConfDirectory() + rb.getString(key), fileName));
            } catch (NullPointerException e) {
            } catch (MissingResourceException e2) {
            }
        }
        return list;
    }

    public static long getSummaryTime(String key) {
        long result = 0;
        String value = getDefaultValue(key);
        File f = new File(value);
        if (f.exists()) {
            result = 0 + f.lastModified();
        }
        for (Package pkg : getPackages()) {
            try {
                ResourceBundle rb = PropertyResourceBundle.getBundle(pkg.getConfFile());
                File f2 = new File(pkg.getConfDirectory() + rb.getString(key));
                if (f2.exists()) {
                    result += f2.lastModified();
                }
            } catch (NullPointerException e) {
            } catch (MissingResourceException e2) {
            }
        }
        return result;
    }

    public static List getPackages() {
        try {
            return Package.getPackages();
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get packages:", ex);
            return new LinkedList();
        }
    }

    public static long getSummaryTime(String key, String fileName) {
        long result = 0;
        String value = getDefaultValue(key);
        if (!isEmpty(value)) {
            File f = new File(value, fileName);
            if (f.exists()) {
                result = 0 + f.lastModified();
            }
        }
        for (Package pkg : getPackages()) {
            try {
                ResourceBundle rb = PropertyResourceBundle.getBundle(pkg.getConfFile());
                File f2 = new File(pkg.getConfDirectory() + rb.getString(key), fileName);
                if (f2.exists()) {
                    result += f2.lastModified();
                }
            } catch (NullPointerException e) {
            } catch (MissingResourceException e2) {
            }
        }
        return result;
    }

    public static boolean arePackagesInstalled() {
        if (!checkedPackagesInstalled) {
            try {
                arePackagesInstalled = getPackages().size() > 0;
                checkedPackagesInstalled = true;
            } catch (Exception ex) {
                Session.getLog().debug("Unable to get packages: ", ex);
            }
        }
        return arePackagesInstalled;
    }
}
