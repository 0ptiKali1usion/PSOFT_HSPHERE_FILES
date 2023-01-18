package psoft.util;

/* loaded from: hsphere.zip:psoft/util/BrowserFamily.class */
public class BrowserFamily {
    public static final BrowserFamily IE4 = new BrowserFamily("MSIE", "4.0");
    public static final BrowserFamily IE5 = new BrowserFamily("MSIE", "5.0");
    public static final BrowserFamily IE5_5 = new BrowserFamily("MSIE", "5.5");
    public static final BrowserFamily IE6 = new BrowserFamily("MSIE", "6.0");
    public static final BrowserFamily IE7 = new BrowserFamily("MSIE", "7.0");
    public static final BrowserFamily NS4 = new BrowserFamily("Netscape", "4.0");
    public static final BrowserFamily NS4_5 = new BrowserFamily("Netscape", "4.5");
    public static final BrowserFamily NS4_7 = new BrowserFamily("Netscape", "4.7");
    public static final BrowserFamily NS6 = new BrowserFamily("Netscape", "6.0");
    public static final BrowserFamily MOZILA4 = new BrowserFamily("Netscape|Opera|MSIE", "4.0");
    String name;
    String version;

    public BrowserFamily(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public boolean belongs(Browser b) {
        try {
            if (this.name.indexOf(b.getAppName()) != -1) {
                if (this.version.compareTo(b.getAppVersion()) < 1) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }
}
