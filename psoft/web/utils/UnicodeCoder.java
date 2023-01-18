package psoft.web.utils;

/* loaded from: hsphere.zip:psoft/web/utils/UnicodeCoder.class */
public class UnicodeCoder {
    public static String encode(String val) {
        if (val == null) {
            return null;
        }
        StringBuffer result = new StringBuffer("");
        for (int i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            if (ch > 127) {
                result.append("\\u");
                if (ch < 4096) {
                    result.append("0");
                }
                if (ch < 256) {
                    result.append("0");
                }
                result.append(Integer.toHexString(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String decode(String val) {
        if (val == null) {
            return null;
        }
        StringBuffer result = new StringBuffer("");
        int i = 0;
        while (i < val.length()) {
            char ch = val.charAt(i);
            if (i + 5 < val.length() && ch == '\\' && val.charAt(i + 1) == 'u') {
                char sym = '?';
                try {
                    sym = (char) Integer.parseInt(val.substring(i + 2, i + 6), 16);
                } catch (Exception e) {
                }
                result.append(sym);
                i += 5;
            } else {
                result.append(ch);
            }
            i++;
        }
        return result.toString();
    }

    public static void main(String[] argv) throws Exception {
        String example = "Euro Sign = € .";
        if (argv.length > 0) {
            example = argv[0];
        }
        System.out.println("Initial string: " + example);
        String encoded = encode(example);
        System.out.println("Encoded string: " + encoded);
        String decoded = decode(encoded);
        System.out.println("Decoded string: " + decoded);
        System.out.println("Encoded string: " + encode(decoded));
        System.out.println("Result matches:" + example.equals(decoded));
    }
}
