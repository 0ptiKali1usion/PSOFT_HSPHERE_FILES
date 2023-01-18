package psoft.util;

import java.util.Enumeration;
import javax.servlet.ServletRequest;

/* loaded from: hsphere.zip:psoft/util/Request2String.class */
public class Request2String {
    public static String toString(ServletRequest r) {
        if (r == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        Enumeration e = r.getParameterNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String[] s = r.getParameterValues(key);
            buf.append(key).append(" = ").append("[");
            if (s != null && s.length > 0) {
                buf.append(s[0]);
                for (int i = 1; i < s.length; i++) {
                    buf.append(", ").append(s[i]);
                }
            }
            buf.append("]\n");
        }
        return buf.toString();
    }
}
