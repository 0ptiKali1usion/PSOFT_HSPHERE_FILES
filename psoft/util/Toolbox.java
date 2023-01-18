package psoft.util;

import freemarker.template.TemplateCache;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.p000db.GenericDatabase;
import psoft.user.UserInitializer;
import psoft.util.freemarker.AutoRefreshFileTemplateCache;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/util/Toolbox.class */
public class Toolbox {
    public static boolean getRedirectType(String name) {
        String tmp = Config.getProperty(name, "REDIRECT_TYPE");
        return tmp == null || tmp.length() == 0 || tmp.toUpperCase().equals("LOCATION");
    }

    public static Hashtable getUserConfig(ResourceBundle rb, NameModifier nm) throws Exception {
        UserInitializer ui = (UserInitializer) Class.forName(rb.getString("USER_CLASS")).newInstance();
        return ui.init(rb, nm);
    }

    public static Database getDB(ResourceBundle rb) throws SQLException, Exception {
        return getDB(rb, NameModifier.none);
    }

    public static Database getDB(ResourceBundle rb, NameModifier nm) throws SQLException, Exception {
        String dbv = "";
        try {
            dbv = rb.getString(nm.getName("DB_VENDOR"));
        } catch (Exception e) {
        }
        if ("ORACLE".equals(dbv)) {
            Class[] argT = {String.class, String.class, String.class, String.class};
            Object[] argV = {rb.getString(nm.getName("DB_URL")), rb.getString(nm.getName("DB_USER")), rb.getString(nm.getName("DB_PASSWORD")), rb.getString(nm.getName("DB_NEWID"))};
            return instDB("psoft.db.OracleDatabase", argT, argV);
        } else if ("MSSQL".equals(dbv)) {
            Class[] argT2 = {String.class, String.class, String.class};
            Object[] argV2 = {rb.getString(nm.getName("DB_URL")), rb.getString(nm.getName("DB_USER")), rb.getString(nm.getName("DB_PASSWORD"))};
            return instDB("psoft.db.MSSQLDatabase", argT2, argV2);
        } else {
            return new GenericDatabase(rb.getString(nm.getName("DB_DRIVER")), rb.getString(nm.getName("DB_URL")), rb.getString(nm.getName("DB_USER")), rb.getString(nm.getName("DB_PASSWORD")), rb.getString(nm.getName("DB_NEWID")));
        }
    }

    protected static Database instDB(String className, Class[] argT, Object[] argV) throws Exception {
        return (Database) Class.forName(className).getConstructor(argT).newInstance(argV);
    }

    public static TemplateCache getTemplateCache(ResourceBundle rb) {
        return getTemplateCache(rb, "TEMPLATE");
    }

    public static TemplateCache getTemplateCache(ResourceBundle rb, String prefix) {
        int tSize = 1000;
        try {
            tSize = Integer.parseInt(rb.getString(prefix + "_SIZE"));
        } catch (Exception e) {
        }
        boolean refresh = true;
        try {
            refresh = !rb.getString(new StringBuilder().append(prefix).append("_REFRESH").toString()).equals("0");
        } catch (Exception e2) {
        }
        int timeout = 900000;
        try {
            timeout = Integer.parseInt(rb.getString(prefix + "_TIMEOUT"));
        } catch (Exception e3) {
        }
        if (rb.getString(prefix + "_PATH") == null) {
            return null;
        }
        return new AutoRefreshFileTemplateCache(rb.getString(prefix + "_PATH"), tSize, timeout, refresh);
    }

    public static Properties RB2Properties(ResourceBundle rb) {
        Properties p = new Properties();
        Enumeration e = rb.getKeys();
        while (e.hasMoreElements()) {
            try {
                String key = e.nextElement().toString();
                p.setProperty(key, rb.getString(key));
            } catch (Exception e2) {
            }
        }
        return p;
    }

    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String result = "";
        int pathLength = path.length();
        boolean isPPrevDoubleDot = false;
        boolean isPrevSlash = false;
        int i = 0;
        while (i < pathLength) {
            char c = path.charAt(i);
            result = result + ((!isPrevSlash || isPPrevDoubleDot) ? String.valueOf(c) : c == '/' ? "" : String.valueOf(c));
            isPrevSlash = c == '/';
            isPPrevDoubleDot = i == 0 ? false : path.charAt(i - 1) == ':';
            i++;
        }
        return result;
    }

    public static String RemoveSpaces(String str) {
        if (str == null) {
            return null;
        }
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != ' ') {
                result = result + c;
            }
        }
        return result;
    }
}
