package psoft.util;

import java.security.SecureRandom;

/* loaded from: hsphere.zip:psoft/util/PasswordGenerator.class */
public class PasswordGenerator {
    public static final char[] ALPHABET = {'!', ',', '.', '=', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public static final char[] ALPHA_NUM_ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static SecureRandom RANDOM = new SecureRandom();

    public static String getPassword(int length) {
        return getPassword(length, ALPHABET);
    }

    public static String getPassword(int length, char[] alphabet) {
        StringBuffer buf = new StringBuffer();
        int mod = alphabet.length;
        for (int i = 0; i < length; i++) {
            buf.append(alphabet[RANDOM.nextInt(mod)]);
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        System.out.println(getPassword(10));
        System.out.println(getPassword(5));
        System.out.println(getPassword(20));
    }
}
