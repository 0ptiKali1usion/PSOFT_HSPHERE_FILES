package psoft.util;

import psoft.yafv.yafvsym;

/* loaded from: hsphere.zip:psoft/util/XMLEncoder.class */
public class XMLEncoder {
    public static final String encode(String s) {
        if (s == null || "".equals(s)) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case yafvsym.NEQ /* 34 */:
                    buffer.append("&quot;");
                    break;
                case yafvsym.f287IF /* 38 */:
                    buffer.append("&amp;");
                    break;
                case '<':
                    buffer.append("&lt;");
                    break;
                case '>':
                    buffer.append("&gt;");
                    break;
                default:
                    buffer.append(s.charAt(i));
                    break;
            }
        }
        return buffer.toString();
    }
}
