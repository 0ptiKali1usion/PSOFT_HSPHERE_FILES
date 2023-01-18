package psoft.util.freemarker;

import freemarker.template.SimpleList;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/CookieMaster.class */
public class CookieMaster implements TemplateHashModel {
    Get m_get = new Get();
    New m_new = new New();
    HttpServletRequest req;
    HttpServletResponse res;
    Cookie[] cookies;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        System.err.println("CookieMaster.get(" + key + ")");
        if ("get".equals(key)) {
            return this.m_get;
        }
        if ("new".equals(key)) {
            return this.m_new;
        }
        if ("list".equals(key)) {
            return listCookies();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/CookieMaster$Get.class */
    public class Get implements TemplateMethodModel {
        Get() {
            CookieMaster.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) {
            Cookie c = CookieMaster.this.getCookie(Toolbox.getArg(HTMLEncoder.decode(list), 0));
            if (null != c) {
                return new CookieModel(c);
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/CookieMaster$New.class */
    public class New implements TemplateMethodModel {
        New() {
            CookieMaster.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) throws TemplateModelException {
            CookieModel cm;
            List list2 = HTMLEncoder.decode(list);
            if (list2.size() == 1) {
                cm = new CookieModel(Toolbox.getArg(list2, 0));
            } else if (list2.size() == 2) {
                cm = new CookieModel(Toolbox.getArg(list2, 0), Toolbox.getArg(list2, 1));
            } else {
                throw new TemplateModelException("Wrong number of aguments to create a cookie:\nneed: NAME and optional VALUE");
            }
            CookieMaster.this.res.addCookie(cm.getCookie());
            return cm;
        }
    }

    public CookieMaster(HttpServletRequest req, HttpServletResponse res) {
        this.cookies = req.getCookies();
        this.res = res;
        this.req = req;
    }

    public Cookie getCookie(String name) {
        if (this.cookies == null) {
            return null;
        }
        for (int i = 0; i < this.cookies.length; i++) {
            if (this.cookies[i].getName().equals(name)) {
                return this.cookies[i];
            }
        }
        return null;
    }

    public TemplateModel listCookies() {
        SimpleList sl = new SimpleList();
        for (int i = 0; i < this.cookies.length; i++) {
            sl.add(new CookieModel(this.cookies[i]));
        }
        return sl;
    }
}
