package psoft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/util/StringUtils.class */
public class StringUtils {
    public static List stringToList(String str, String sep) {
        List list = new ArrayList();
        if (str == null) {
            return list;
        }
        StringTokenizer st = new StringTokenizer(str, sep);
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.length() != 0) {
                list.add(token);
            }
        }
        return list;
    }
}
