package psoft.hsphere;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.util.PackageConfigurator;

/* loaded from: hsphere.zip:psoft/hsphere/LangBundlesCompiler.class */
public class LangBundlesCompiler {
    public static final String INT_DIRECTORY = "languages";
    public static final String INT_TEMPLATE_BUNDLE = "hsphere_lang";
    public static final String INT_MENU_BUNDLE = "menu";
    public static final String INT_USER_BUNDLE = "messages";

    public LangBundlesCompiler() throws Exception {
        ExternalCP.initCP();
    }

    public static List getLocaleList() throws Exception {
        String encoding;
        List<String> tmpLangList = PackageConfigurator.getPackageProperties("LANG_LIST");
        StringBuffer locales = new StringBuffer(Session.getPropertyString("LANG_LIST"));
        if (tmpLangList != null) {
            for (String o : tmpLangList) {
                locales.append(' ').append(o);
            }
        }
        StringTokenizer tkz = new StringTokenizer(locales.toString());
        List localeList = new LinkedList();
        while (tkz.hasMoreTokens()) {
            StringTokenizer localeTkz = new StringTokenizer(tkz.nextToken(), "_");
            String language = localeTkz.nextToken();
            String country = localeTkz.nextToken();
            String nextToken = localeTkz.nextToken();
            while (true) {
                encoding = nextToken;
                if (!localeTkz.hasMoreTokens()) {
                    break;
                }
                nextToken = encoding + "_" + localeTkz.nextToken();
            }
            int cutIndex = encoding.indexOf("|");
            String charset = encoding.substring(0, cutIndex < 0 ? 0 : cutIndex);
            try {
                new String(" ".getBytes(), charset);
            } catch (UnsupportedEncodingException e) {
                System.out.print("The combined LANG_LIST variable contains an unsupported charset '" + charset + "'. ");
                charset = encoding.substring(encoding.indexOf("|") + 1, encoding.indexOf(":")).trim();
                System.out.println("Trying to use '" + charset + "' instead.");
            }
            localeList.add(new Locale(language, country, charset));
        }
        if (localeList.size() > 0) {
            return localeList;
        }
        return null;
    }

    protected static String encode(String val) {
        if (val == null) {
            return null;
        }
        char[] reChars = val.toCharArray();
        StringBuffer result = new StringBuffer();
        for (char reChar : reChars) {
            if (reChar >= ' ' && reChar <= '~') {
                result.append(reChar);
            } else {
                result.append("\\u");
                String uChar = Integer.toHexString(reChar);
                switch (uChar.length()) {
                    case 1:
                        result.append("000");
                        break;
                    case 2:
                        result.append("00");
                        break;
                    case 3:
                        result.append("0");
                        break;
                }
                result.append(uChar);
            }
        }
        return result.toString();
    }

    /* JADX WARN: Finally extract failed */
    public static Hashtable loadBundle(List bundlesList) throws Exception {
        InputStream profISt;
        Hashtable properties = new Hashtable();
        Hashtable prefixes = getBundlePrefixesList(getLocaleList());
        for (int i = 0; i < bundlesList.size(); i++) {
            String bundleName = (String) bundlesList.get(i);
            for (String prefix : prefixes.keySet()) {
                String bundleFullName = PackageConfigurator.getDefaultValue("HSPHERE_HOME") + "/" + bundleName.replace('.', '/') + prefix + ".properties";
                File propFile = new File(bundleFullName);
                if (propFile.exists()) {
                    FileInputStream fis = null;
                    InputStream profISt2 = null;
                    System.out.println("Loading " + bundleFullName);
                    try {
                        try {
                            fis = new FileInputStream(propFile);
                            Locale locale = (Locale) prefixes.get(prefix);
                            if ("".equals(prefix)) {
                                profISt = fis;
                            } else {
                                BufferedReader source = null;
                                StringBuffer convertedLine = new StringBuffer();
                                try {
                                    try {
                                        source = new BufferedReader(new InputStreamReader(fis, locale.getVariant()));
                                        while (true) {
                                            String line = source.readLine();
                                            if (line == null) {
                                                break;
                                            }
                                            convertedLine.append(encode(line)).append('\n');
                                        }
                                        if (source != null) {
                                            source.close();
                                        }
                                    } catch (UnsupportedEncodingException ex) {
                                        if (locale == null) {
                                            throw ex;
                                        }
                                        System.err.println("Unsupported encoding '" + locale.getVariant() + "' is detected. Skipping...");
                                        if (source != null) {
                                            source.close();
                                        }
                                    } catch (Exception ex2) {
                                        ex2.printStackTrace();
                                        if (source != null) {
                                            source.close();
                                        }
                                    }
                                    profISt = new StringBufferInputStream(convertedLine.toString());
                                } catch (Throwable th) {
                                    if (source != null) {
                                        source.close();
                                    }
                                    throw th;
                                }
                            }
                            Properties propBundle = (Properties) properties.get(prefix);
                            if (propBundle == null) {
                                propBundle = new Properties();
                            } else {
                                propBundle.get("");
                            }
                            Properties tmpProp = new Properties();
                            try {
                                tmpProp.load(profISt);
                                propBundle.putAll(tmpProp);
                            } catch (IOException e) {
                            }
                            properties.put(prefix, propBundle);
                            if ("".equals(prefix)) {
                                properties.put("_en", propBundle);
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            if (profISt != null) {
                                profISt.close();
                            }
                        } catch (Throwable th2) {
                            if (fis != null) {
                                fis.close();
                            }
                            if (0 != 0) {
                                profISt2.close();
                            }
                            throw th2;
                        }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        if (0 != 0) {
                            fis.close();
                        }
                        if (0 != 0) {
                            profISt2.close();
                        }
                    }
                }
            }
        }
        if (properties.size() > 0) {
            return properties;
        }
        return null;
    }

    public static Hashtable getBundlePrefixesList(List locales) {
        Hashtable prefixes = new Hashtable();
        Iterator iterator = locales.iterator();
        while (iterator.hasNext()) {
            Locale locale = (Locale) iterator.next();
            if (!locale.equals(Locale.getDefault())) {
                prefixes.put("_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant(), locale);
                prefixes.put("_" + locale.getLanguage() + "_" + locale.getCountry(), locale);
                prefixes.put("_" + locale.getLanguage(), locale);
            }
        }
        prefixes.put("", Locale.getDefault());
        return prefixes;
    }

    public static List getBundlesList(String bundleName) {
        List bundleList = new LinkedList();
        bundleList.add(Session.getPropertyString(bundleName));
        List packageProperties = PackageConfigurator.getPackageProperties(bundleName);
        if (packageProperties != null) {
            bundleList.addAll(packageProperties);
        }
        String custBundle = Session.getPropertyString("CUSTOM_" + bundleName);
        if (custBundle != null) {
            bundleList.add(custBundle);
        }
        if (bundleList.size() > 0) {
            return bundleList;
        }
        return null;
    }

    public static void saveBundles(String bundleName, Hashtable properties) {
        if (properties == null) {
            return;
        }
        Enumeration en = properties.keys();
        while (en.hasMoreElements()) {
            String prefix = (String) en.nextElement();
            Properties property = (Properties) properties.get(prefix);
            String bundleFullName = bundleName.replace('.', '/') + prefix + ".properties";
            File file = new File(bundleFullName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    property.store(fos, "Created by LangBundlesCompiler tool");
                    System.out.println(bundleFullName + " has been processed and saved");
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                } catch (IOException e2) {
                    System.out.println(e2.getMessage());
                    e2.printStackTrace();
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e3) {
                        }
                    }
                }
            } catch (FileNotFoundException e4) {
            }
        }
    }

    public static void compile() throws Exception {
        String intDirectory = Session.getPropertyString("INT_LANGBUNDLE_DIRECTORY");
        if ("".equals(intDirectory)) {
            intDirectory = PackageConfigurator.getDefaultValue("HSPHERE_HOME") + "/" + INT_DIRECTORY;
        }
        File intDirectoryFile = new File(intDirectory);
        if (!intDirectoryFile.exists()) {
            intDirectoryFile.mkdirs();
            System.out.println("The internal lang directoy " + intDirectoryFile.getAbsolutePath() + "has been created");
        } else if (!intDirectoryFile.isDirectory()) {
            throw new IOException("The directory " + intDirectoryFile.getAbsolutePath() + " is file");
        }
        String intTemplate = Session.getPropertyString("INT_TEMPLATE_BUNDLE");
        if ("".equals(intTemplate)) {
            intTemplate = intDirectory + "." + INT_TEMPLATE_BUNDLE;
        }
        saveBundles(intTemplate, loadBundle(getBundlesList("TEMPLATE_BUNDLE")));
        String intMenu = Session.getPropertyString("INT_MENU_BUNDLE");
        if ("".equals(intMenu)) {
            intMenu = intDirectory + "." + INT_MENU_BUNDLE;
        }
        saveBundles(intMenu, loadBundle(getBundlesList("MENU_BUNDLE")));
        String intMessages = Session.getPropertyString("INT_USER_BUNDLE");
        if ("".equals(intMessages)) {
            intMessages = intDirectory + "." + INT_USER_BUNDLE;
        }
        saveBundles(intMessages, loadBundle(getBundlesList("USER_BUNDLE")));
    }

    public static void main(String[] argvs) throws Exception {
        new LangBundlesCompiler();
        compile();
        System.exit(0);
    }
}
