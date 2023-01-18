package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import javax.servlet.http.Cookie;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/CookieModel.class */
public class CookieModel implements TemplateHashModel {
    Cookie cookie;
    Set m_set;

    public boolean isEmpty() {
        return false;
    }

    public Cookie getCookie() {
        return this.cookie;
    }

    public CookieModel(Cookie cookie) {
        this.m_set = new Set();
        this.cookie = cookie;
    }

    public CookieModel(String name) {
        this(new Cookie(name, ""));
    }

    public CookieModel(String name, String value) {
        this(new Cookie(name, value));
    }

    public TemplateModel get(String key) {
        if ("name".equals(key)) {
            return new SimpleScalar(this.cookie.getName());
        }
        if ("comment".equals(key)) {
            return new SimpleScalar(this.cookie.getComment());
        }
        if ("domain".equals(key)) {
            return new SimpleScalar(this.cookie.getDomain());
        }
        if ("max_age".equals(key)) {
            return new SimpleScalar(Integer.toString(this.cookie.getMaxAge()));
        }
        if ("path".equals(key)) {
            return new SimpleScalar(this.cookie.getPath());
        }
        if ("secure".equals(key)) {
            return new SimpleScalar(this.cookie.getSecure());
        }
        if ("value".equals(key)) {
            return new SimpleScalar(this.cookie.getValue());
        }
        if ("version".equals(key)) {
            return new SimpleScalar(Integer.toString(this.cookie.getVersion()));
        }
        if ("set".equals(key)) {
            return this.m_set;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/CookieModel$Set.class */
    public class Set implements TemplateMethodModel {
        Set() {
            CookieModel.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List args) throws TemplateModelException {
            List args2 = HTMLEncoder.decode(args);
            System.err.println("CookieModel.set() ");
            String key = Toolbox.getArg(args2, 0);
            String value = Toolbox.getArg(args2, 1);
            System.err.println("CookieModel.set( " + key + ", " + value + ")");
            if ("value".equals(key)) {
                CookieModel.this.cookie.setValue(value);
                return null;
            } else if ("comment".equals(key)) {
                CookieModel.this.cookie.setComment(value);
                return null;
            } else if ("domain".equals(key)) {
                CookieModel.this.cookie.setDomain(value);
                return null;
            } else if ("max_age".equals(key)) {
                CookieModel.this.cookie.setMaxAge(Integer.parseInt(value));
                return null;
            } else if ("path".equals(key)) {
                CookieModel.this.cookie.setPath(value);
                return null;
            } else if ("secure".equals(key)) {
                CookieModel.this.cookie.setSecure(Boolean.getBoolean(value.toLowerCase()));
                return null;
            } else if ("version".equals(key)) {
                CookieModel.this.cookie.setVersion(Integer.parseInt(value));
                return null;
            } else {
                throw new TemplateModelException("Operation is not supported by CookieModel: '" + key + "'");
            }
        }
    }
}
