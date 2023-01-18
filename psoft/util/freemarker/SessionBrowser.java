package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import psoft.util.Browser;

/* loaded from: hsphere.zip:psoft/util/freemarker/SessionBrowser.class */
public class SessionBrowser implements TemplateHashModel {
    protected String userAgent;
    protected Browser browser;
    protected boolean empty;
    private boolean isDetected;
    private static final Category log = Category.getInstance(SessionBrowser.class.getName());
    protected String isWinMSIE55Up = null;
    protected String css1 = null;

    public boolean isEmpty() {
        return this.empty;
    }

    public SessionBrowser(String userAgent) {
        this.isDetected = false;
        if (userAgent != null && !"".equals(userAgent)) {
            this.isDetected = false;
            this.userAgent = userAgent;
            return;
        }
        this.isDetected = true;
        this.userAgent = "";
        this.browser = new Browser("", "", null, null);
        this.empty = true;
    }

    protected void checkDetect() {
        if (!this.isDetected) {
            try {
                this.browser = Browser.detect(this.userAgent);
                this.empty = false;
            } catch (Exception ex) {
                this.browser = new Browser("", "", null, null);
                this.empty = true;
                log.error("Cannot detect browser. ", ex);
            }
        }
    }

    public TemplateModel get(String key) {
        checkDetect();
        if (key.equals("app_name")) {
            return new TemplateString(this.browser.getAppName());
        }
        if (key.equals("app_version")) {
            return new TemplateString(this.browser.getAppVersion());
        }
        if (key.equals("platform")) {
            return new TemplateString(this.browser.getPlatform());
        }
        if (key.equals("isWinPlatform")) {
            if (this.browser.isWinPlatform()) {
                return new TemplateString("1");
            }
            return null;
        } else if (key.equals("isWinMSIE55Up")) {
            if (this.isWinMSIE55Up == null) {
                this.isWinMSIE55Up = (this.browser.isWinPlatform() && this.browser.belongs("IE5.5") && "".equals(this.browser.getOptEngine())) ? "1" : "";
            }
            return new TemplateString(this.isWinMSIE55Up);
        } else if (key.equals("css1")) {
            if (this.css1 == null) {
                try {
                    String appName = this.browser.getAppName();
                    float appVersion = new Double(this.browser.getAppVersion()).floatValue();
                    this.css1 = ((!"Netscape".equals(appName) || ((double) appVersion) >= 5.0d) && (!"MSIE".equals(appName) || ((double) appVersion) >= 5.5d)) ? "" : "1";
                } catch (Exception e) {
                    this.css1 = "";
                }
            }
            return new TemplateString(this.css1);
        } else if (key.equals("user_agent")) {
            return new TemplateString(this.userAgent);
        } else {
            try {
                return TemplateMethodWrapper.getMethod(this, key);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    public TemplateModel FM_belongs(String family) {
        checkDetect();
        if (this.browser.belongs(family)) {
            return new TemplateString("1");
        }
        return null;
    }

    public TemplateModel FM_complies(String complexPattern) {
        checkDetect();
        boolean res = false;
        StringTokenizer orGroups = new StringTokenizer(complexPattern, "|");
        while (orGroups.hasMoreTokens() && !res) {
            StringTokenizer andElements = new StringTokenizer(orGroups.nextToken(), "&");
            while (andElements.hasMoreTokens()) {
                res = this.browser.complies(andElements.nextToken().trim());
                if (!res) {
                    break;
                }
            }
        }
        if (res) {
            return new TemplateString("1");
        }
        return null;
    }
}
