package psoft.web.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/web/utils/HTMLEncoder.class */
public class HTMLEncoder {
    static String special = "<>&\"";
    static String[] specialEncoded = {"&lt;", "&gt;", "&amp;", "&quot;"};
    static Map specialDecodeMap = new HashMap();

    static {
        specialDecodeMap.put("lt", "<");
        specialDecodeMap.put("gt", ">");
        specialDecodeMap.put("amp", "&");
        specialDecodeMap.put("quot", "\"");
    }

    public static String encode(String str) {
        if (isEncoded(str) == 0) {
            return encodeForcibly(str);
        }
        return str;
    }

    public static String encodeForcibly(String str) {
        StringBuffer res = new StringBuffer("");
        int lastIndex = str.length();
        for (int i = 0; i < lastIndex; i++) {
            char ch = str.charAt(i);
            if (ch > 255) {
                res.append("&#").append((int) ch).append(';');
            } else {
                int index = special.indexOf(ch);
                if (index == -1) {
                    res.append(ch);
                } else {
                    res.append(specialEncoded[index]);
                }
            }
        }
        return res.toString();
    }

    public static int isEncoded(String str) {
        if (str == null) {
            return -1;
        }
        int encoded = -1;
        int lastIndex = str.length();
        int i = 0;
        while (i < lastIndex) {
            char ch = str.charAt(i);
            if (ch == '&') {
                int lastPos = str.indexOf(59, i + 1);
                if (lastPos > 0) {
                    String lexeme = str.substring(i + 1, lastPos);
                    if (lexeme.startsWith("#")) {
                        int j = lexeme.length() - 1;
                        while (j > 0) {
                            int i2 = j;
                            j = i2 - 1;
                            if (!Character.isDigit(lexeme.charAt(i2))) {
                                break;
                            }
                        }
                        if (j == 0) {
                            encoded = 1;
                            i = lastPos;
                        } else {
                            return 0;
                        }
                    } else if (specialDecodeMap.get(lexeme) != null) {
                        encoded = 1;
                        i = lastPos;
                    } else {
                        return 0;
                    }
                } else {
                    continue;
                }
            } else if (ch > 255 || special.indexOf(ch) >= 0) {
                return 0;
            }
            i++;
        }
        return encoded;
    }

    public static String decode(String str) {
        if (isEncoded(str) == 1) {
            return decodeForcibly(str);
        }
        return str;
    }

    public static String decodeForcibly(String str) {
        int lastPos;
        StringBuffer buf = new StringBuffer();
        int lastIndex = str.length();
        int i = 0;
        while (i < lastIndex) {
            char ch = str.charAt(i);
            if (ch == '&' && (lastPos = str.indexOf(59, i + 1)) > 0) {
                String lexeme = str.substring(i + 1, lastPos);
                if (lexeme.startsWith("#")) {
                    int j = lexeme.length() - 1;
                    while (j > 0) {
                        int i2 = j;
                        j = i2 - 1;
                        if (!Character.isDigit(lexeme.charAt(i2))) {
                            break;
                        }
                    }
                    if (j == 0) {
                        buf.append((char) Integer.parseInt(lexeme.substring(1)));
                        i = lastPos;
                    }
                } else {
                    String decoded = (String) specialDecodeMap.get(lexeme);
                    if (decoded != null) {
                        buf.append(decoded);
                        i = lastPos;
                    }
                }
                i++;
            }
            buf.append(ch);
            i++;
        }
        return buf.toString();
    }

    public static List decode(List args) {
        if (args == null) {
            return null;
        }
        List result = new ArrayList();
        Iterator it = args.iterator();
        while (it.hasNext()) {
            String arg = (String) it.next();
            result.add(decode(arg));
        }
        return result;
    }
}
