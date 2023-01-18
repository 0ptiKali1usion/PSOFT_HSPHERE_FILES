package psoft.web.utils;

/* loaded from: hsphere.zip:psoft/web/utils/CSVEncoder.class */
public class CSVEncoder {
    public static String encode(String value) {
        if (value == null) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        boolean needQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\"') {
                result.append('\"');
            }
            if (!needQuote && !Character.isLetterOrDigit(ch)) {
                needQuote = true;
            }
            result.append(ch);
        }
        if (needQuote) {
            result.insert(0, '\"');
            result.append('\"');
        }
        return result.toString();
    }
}
