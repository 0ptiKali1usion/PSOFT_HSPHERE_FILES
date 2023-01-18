package psoft.web.utils;

/* loaded from: hsphere.zip:psoft/web/utils/URLEncoder.class */
public class URLEncoder {
    private static final String hexChars = "0123456789ABCDEF";

    public static String encode(String value) {
        return encode(value, false);
    }

    public static String encode(String value, boolean plusAsSpace) {
        if (value == null) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (plusAsSpace && ch == ' ') {
                result.append('+');
            } else if ((ch >= '0' && ch <= '9') || ((ch >= 'A' && ch <= 'Z') || ((ch >= 'a' && ch <= 'z') || ch == '.' || ch == '-' || ch == '_' || ch == ':' || ch == '/'))) {
                result.append(ch);
            } else {
                result.append('%');
                result.append(hexChars.charAt((ch >> 4) & 15));
                result.append(hexChars.charAt(ch & 15));
            }
        }
        return result.toString();
    }
}
