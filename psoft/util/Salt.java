package psoft.util;

import java.security.SecureRandom;
import psoft.hsphere.SignupGuard;

/* loaded from: hsphere.zip:psoft/util/Salt.class */
public class Salt {
    private final char[] itoa64 = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM.,".toCharArray();
    private SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

    public Salt() throws Exception {
        this.random.setSeed(TimeUtils.currentTimeMillis());
    }

    public String getNext() {
        return getNext(10);
    }

    public String getNext(int size) {
        StringBuffer sb = new StringBuffer(size);
        byte[] rnd = new byte[size];
        this.random.nextBytes(rnd);
        for (int i = 0; i < size; i++) {
            sb.append(this.itoa64[(rnd[i] + SignupGuard.MODERATE_EVERYTHING_FLAG) % 64]);
        }
        return sb.toString();
    }
}
