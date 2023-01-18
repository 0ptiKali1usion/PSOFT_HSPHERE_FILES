package psoft.util;

import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;
import psoft.hsphere.admin.LanguageManager;

/* loaded from: hsphere.zip:psoft/util/Base64.class */
public class Base64 {
    public static String encode(String data) {
        byte[] bytes;
        try {
            bytes = data.getBytes(LanguageManager.STANDARD_CHARSET);
        } catch (UnsupportedEncodingException e) {
            bytes = data.getBytes();
        }
        StringTokenizer st = new StringTokenizer(org.apache.xerces.impl.dv.util.Base64.encode(bytes));
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
        }
        return sb.toString();
    }

    public static String encode(byte[] data) {
        StringTokenizer st = new StringTokenizer(org.apache.xerces.impl.dv.util.Base64.encode(data));
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
        }
        return sb.toString();
    }

    public static byte[] decode(String data) {
        return org.apache.xerces.impl.dv.util.Base64.decode(data);
    }

    public static String decodeToString(String data) {
        try {
            return new String(decode(data), LanguageManager.STANDARD_CHARSET);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
