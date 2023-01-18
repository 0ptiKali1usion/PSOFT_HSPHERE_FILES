package psoft.hsphere;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.util.PackageConfigurator;

/* loaded from: hsphere.zip:psoft/hsphere/Localizer.class */
public class Localizer {
    public static String translateMessage(String id) {
        return translateMessage(id, null);
    }

    public static String translateLabel(String id) {
        return translateLabel(id, null);
    }

    public static String translateMenu(String id) {
        return translateMenu(id, null);
    }

    public static String translateMessage(String id, Object[] args) {
        try {
            ResourceBundle bundle = C0004CP.getMessageBundle(Session.getCurrentLocale());
            return translate(bundle, id, args);
        } catch (MissingResourceException e) {
            Session.getLog().debug("Message bundle exception", e);
            return id;
        }
    }

    public static String translateLabel(String id, Object[] args) {
        try {
            ResourceBundle bundle = C0004CP.getLabelBundle(Session.getCurrentLocale());
            return translate(bundle, id, args);
        } catch (MissingResourceException e) {
            Session.getLog().debug("Label bundle exception", e);
            return id;
        }
    }

    public static String translateMenu(String id, Object[] args) {
        try {
            ResourceBundle bundle = C0004CP.getMenuBundle(Session.getCurrentLocale());
            return translate(bundle, id, args);
        } catch (MissingResourceException e) {
            Session.getLog().debug("Menu bundle exception", e);
            return id;
        }
    }

    protected static String translate(ResourceBundle bundle, String id, Object[] args) {
        if (id == null || "".equals(id)) {
            return "";
        }
        if (bundle == null) {
            return id;
        }
        try {
            String msg = bundle.getString(id);
            if (args == null || msg == null) {
                return msg == null ? id : msg;
            }
            return MessageFormat.format(msg, args);
        } catch (MissingResourceException e) {
            Session.getLog().debug("Lang Error: Missing Label:" + id);
            return id;
        }
    }

    public static Locale getLocaleFromString(String str) {
        Locale result;
        String[] arr = getArrayLocale(str);
        try {
            result = new Locale(arr[0], arr[1], arr[2]);
        } catch (Exception e) {
            result = Locale.getDefault();
        }
        return result;
    }

    public static String getLanguageFromString(String str) {
        String result;
        String[] arr = getArrayLocale(str);
        try {
            result = arr[0];
        } catch (Exception e) {
            result = Locale.getDefault().getLanguage();
        }
        return result;
    }

    public static String getCountryFromString(String str) {
        String result;
        String[] arr = getArrayLocale(str);
        try {
            result = arr[1];
        } catch (Exception e) {
            result = Locale.getDefault().getCountry();
        }
        return result;
    }

    public static String getCharsetFromString(String str) {
        String result;
        String[] arr = getArrayLocale(str);
        try {
            result = arr[3];
        } catch (Exception e) {
            result = Session.getProperty("ENCODING");
        }
        return result;
    }

    public static String getMailCharsetFromString(String str) {
        String result;
        String[] arr = getArrayLocale(str);
        try {
            result = arr[4];
        } catch (Exception e) {
            result = Session.getProperty("ENCODING");
        }
        return result;
    }

    public static String[] getArrayLocale(String str) {
        String mailCharset;
        try {
            StringTokenizer tokenizer = new StringTokenizer(str, "|");
            if (tokenizer.countTokens() < 2) {
                throw new Exception("Invalid language token");
            }
            String locale = tokenizer.nextToken();
            String charset = tokenizer.nextToken();
            new String();
            if (tokenizer.hasMoreTokens()) {
                mailCharset = tokenizer.nextToken();
            } else {
                mailCharset = charset;
            }
            StringTokenizer tokenizer2 = new StringTokenizer(locale, "_");
            String language = tokenizer2.nextToken();
            String country = tokenizer2.nextToken();
            String encoding = tokenizer2.nextToken();
            while (tokenizer2.hasMoreTokens()) {
                encoding = encoding + "_" + tokenizer2.nextToken();
            }
            String[] result = {language, country, encoding, charset, mailCharset};
            return result;
        } catch (Exception e) {
            String[] result2 = {Locale.getDefault().getLanguage(), Locale.getDefault().getCountry(), Locale.getDefault().getVariant(), Session.getProperty("ENCODING"), Session.getProperty("ENCODING")};
            return result2;
        }
    }

    public static String getAssumedUserLocaleString(Account a) throws Exception {
        String lang = a.getPreferences().getValueSafe(AccountPreferences.LANGUAGE);
        if (lang != null) {
            return lang;
        }
        String result = new String();
        ContactInfoObject ci = a.getContactInfo();
        String userCountry = ci.get("country").toString();
        if (userCountry != null) {
            List<String> tmpLangList = PackageConfigurator.getPackageProperties("LANG_LIST");
            StringBuffer availLocales = new StringBuffer(Session.getProperty("LANG_LIST"));
            if (tmpLangList != null) {
                for (String o : tmpLangList) {
                    Session.getLog().debug("Locale from package is -> " + o);
                    availLocales.append(" ");
                    availLocales.append(o);
                }
            }
            StringTokenizer tokenizer = new StringTokenizer(availLocales.toString(), " ");
            List locales = new ArrayList();
            while (tokenizer.hasMoreTokens()) {
                String tmpLocaleStr = tokenizer.nextToken();
                String countryFromList = getCountryFromString(tmpLocaleStr);
                if (userCountry.equals(countryFromList)) {
                    locales.add(tmpLocaleStr);
                }
            }
            try {
                StringTokenizer tkz = new StringTokenizer((String) locales.get(0), ":");
                result = tkz.nextToken();
            } catch (Exception e) {
                result = "en_US_ISO8859_1|ISO-8859-1";
            }
        }
        return result;
    }
}
