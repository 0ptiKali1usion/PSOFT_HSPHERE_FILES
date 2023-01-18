package psoft.util;

import javax.servlet.http.Cookie;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/util/ServletUtils.class */
public class ServletUtils {
    private static Category log = Category.getInstance(ServletUtils.class.getName());

    public static String getCookieValue(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                return c.getValue();
            }
        }
        return null;
    }
}
