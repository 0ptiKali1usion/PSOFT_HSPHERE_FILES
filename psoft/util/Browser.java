package psoft.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/util/Browser.class */
public class Browser {
    String appName;
    String appVersion;
    String platform;
    String optEngine;
    static final int VER_EQUALS = 0;
    static final int VER_LESS = -1;
    static final int VER_MORE = 1;
    static final int VER_INCORRECT = -2;
    public static final Map familyMap = new HashMap();

    static {
        familyMap.put("IE4", BrowserFamily.IE4);
        familyMap.put("IE5", BrowserFamily.IE5);
        familyMap.put("IE5.5", BrowserFamily.IE5_5);
        familyMap.put("IE6", BrowserFamily.IE6);
        familyMap.put("IE7", BrowserFamily.IE7);
        familyMap.put("NS4", BrowserFamily.NS4);
        familyMap.put("NS4.5", BrowserFamily.NS4_5);
        familyMap.put("NS4.7", BrowserFamily.NS4_7);
        familyMap.put("NS6", BrowserFamily.NS6);
        familyMap.put("MOZILA4", BrowserFamily.MOZILA4);
    }

    public Browser(String appName, String appVersion, String platform, String optEngine) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.platform = platform;
        this.optEngine = optEngine;
    }

    /* loaded from: hsphere.zip:psoft/util/Browser$Parser.class */
    public static class Parser {
        String sig;
        String type;
        String version;
        String app;
        String appVersion;
        int len;
        static final NumberFormat VER_FORMAT_OBJ = USFormat.getInstance();
        String platform = null;
        String optEngine = null;
        String[] knownBrowsers = {"msie", "netscape", "opera", "konqueror", "firefox", "safari"};
        String[] knownPlatforms = {"windows", "linux", "freebsd", "ppc mac", "mac_powerpc"};

        /* renamed from: i */
        int f256i = 0;

        Parser(String sig) throws ParseException {
            this.app = null;
            this.appVersion = null;
            this.sig = sig;
            this.len = sig.length();
            boolean parsedOk = parseType() && ignore('/') && parseVersion() && parseEncoding() && parseBrackets();
            if (!parsedOk) {
                throw new ParseException("Unable to parse the Browser Agent string: [" + sig + "]", this.f256i);
            }
            if (this.app == null && this.type != null) {
                this.app = this.type.equalsIgnoreCase("Mozilla") ? "Netscape" : this.type;
                this.appVersion = this.version;
            }
        }

        String getToken(char ch) {
            int old = this.f256i;
            while (this.f256i < this.len && this.sig.charAt(this.f256i) != ch) {
                this.f256i++;
            }
            return this.sig.substring(old, this.f256i);
        }

        String getToken(String delim) {
            int old = this.f256i;
            if (delim != null) {
                char[] dArray = delim.toCharArray();
                Arrays.sort(dArray);
                while (this.f256i < this.len && Arrays.binarySearch(dArray, this.sig.charAt(this.f256i)) < 0) {
                    this.f256i++;
                }
            }
            return this.sig.substring(old, this.f256i);
        }

        boolean parseComponents() {
            String parenInfo = getToken(')');
            StringTokenizer st = new StringTokenizer(parenInfo, ";");
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                String tkLowcase = token.toLowerCase();
                boolean skipOther = false;
                int i = 0;
                while (true) {
                    if (i >= this.knownBrowsers.length) {
                        break;
                    }
                    if (tkLowcase.startsWith(this.knownBrowsers[i])) {
                        StringTokenizer appTokenizer = new StringTokenizer(token, " /");
                        if (appTokenizer.countTokens() > 1) {
                            this.app = appTokenizer.nextToken();
                            this.appVersion = appTokenizer.nextToken();
                            skipOther = true;
                            break;
                        }
                    }
                    i++;
                }
                if (!skipOther) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= this.knownPlatforms.length) {
                            break;
                        } else if (!tkLowcase.startsWith(this.knownPlatforms[i2])) {
                            i2++;
                        } else {
                            this.platform = token;
                            break;
                        }
                    }
                }
            }
            ignore(')');
            this.optEngine = getToken(';');
            if (this.optEngine != null) {
                this.optEngine = this.optEngine.trim();
                return true;
            }
            this.optEngine = "";
            return true;
        }

        boolean parseBrackets() {
            return ignore('(') && parseComponents();
        }

        boolean parseEncoding() {
            while (this.f256i < this.len && this.sig.charAt(this.f256i) != '(') {
                this.f256i++;
            }
            return this.f256i < this.len;
        }

        boolean parseVersion() {
            int old = this.f256i;
            while (this.f256i < this.len && !Character.isWhitespace(this.sig.charAt(this.f256i))) {
                this.f256i++;
            }
            this.version = this.sig.substring(old, this.f256i);
            return this.f256i < this.len;
        }

        boolean parseType() {
            while (this.f256i < this.len && this.sig.charAt(this.f256i) != '/') {
                this.f256i++;
            }
            this.type = this.sig.substring(0, this.f256i);
            return this.f256i < this.len;
        }

        boolean ignore(char ch) {
            if (ch == this.sig.charAt(this.f256i)) {
                this.f256i++;
                return true;
            }
            return false;
        }

        public String toString() {
            return "type: " + this.type + "\nversion: " + this.version + "\napp: " + this.app + "\nappVersion: " + this.appVersion;
        }

        static float parseVersion(String versionStr) {
            try {
                return VER_FORMAT_OBJ.parse(versionStr).floatValue();
            } catch (ParseException e) {
                return -1.0f;
            }
        }
    }

    public static Browser detect(String sig) throws Exception {
        Parser parser = new Parser(sig);
        Browser b = new Browser(parser.app, parser.appVersion, parser.platform, parser.optEngine);
        return b;
    }

    public String getAppVersion() {
        return this.appVersion;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getPlatform() {
        return this.platform;
    }

    public String getOptEngine() {
        return this.optEngine;
    }

    public boolean isWinPlatform() {
        return this.platform != null && this.platform.toLowerCase().startsWith("win");
    }

    public int checkVersion(String ver) {
        if (this.appVersion == null || ver == null) {
            return -2;
        }
        float appVerNum = Parser.parseVersion(this.appVersion);
        if (appVerNum > -1.0f) {
            float curVerNum = Parser.parseVersion(ver);
            if (curVerNum > -1.0f) {
                if (appVerNum < curVerNum) {
                    return -1;
                }
                if (appVerNum > curVerNum) {
                    return 1;
                }
            }
        }
        switch (this.appVersion.toLowerCase().compareTo(ver.toLowerCase())) {
            case -1:
                return -1;
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return -2;
        }
    }

    public boolean isPlatform(String value) {
        return (this.platform == null || value == null || !this.platform.toLowerCase().startsWith(value.toLowerCase())) ? false : true;
    }

    public boolean belongs(String family) {
        BrowserFamily bf = (BrowserFamily) familyMap.get(family);
        return bf != null && bf.belongs(this);
    }

    public boolean complies(String pattern) {
        StringTokenizer st = new StringTokenizer(pattern, "_");
        if (st.hasMoreTokens()) {
            String curAppName = st.nextToken().trim();
            if (!curAppName.equals(this.appName)) {
                return false;
            }
        }
        if (st.hasMoreTokens()) {
            String curAppVer = st.nextToken().trim();
            int verDelimIndex = curAppVer.indexOf(45);
            if (verDelimIndex >= 0) {
                if (verDelimIndex == 0) {
                    int check = checkVersion(curAppVer.substring(1));
                    if (check != -1 && check != 0) {
                        return false;
                    }
                } else if (verDelimIndex == curAppVer.length() - 1) {
                    int check2 = checkVersion(curAppVer.substring(0, verDelimIndex));
                    if (check2 != 1 && check2 != 0) {
                        return false;
                    }
                } else {
                    int checkVerMin = checkVersion(curAppVer.substring(0, verDelimIndex));
                    int checkVerMax = checkVersion(curAppVer.substring(verDelimIndex + 1));
                    if (checkVerMin != 1 && checkVerMin != 0 && checkVerMax != -1 && checkVerMax != 0) {
                        return false;
                    }
                }
            } else if (checkVersion(curAppVer) != 0) {
                return false;
            }
        }
        if (st.hasMoreTokens()) {
            String curPlatform = st.nextToken().trim();
            if (!isPlatform(curPlatform)) {
                return false;
            }
            return true;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        Browser b = detect(args[0]);
        System.out.println("IE5: " + b.belongs("IE5"));
        System.out.println("IE5.5: " + b.belongs("IE5.5"));
        System.out.println("IE6: " + b.belongs("IE6"));
        System.out.println("NS6: " + b.belongs("NS6"));
        System.out.println("NS4_5: " + b.belongs("NS4.5"));
        System.out.println("Mozila4: " + b.belongs("MOZILA4"));
    }
}
