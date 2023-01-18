package psoft.hsphere;

import java.util.StringTokenizer;
import psoft.encryption.MD5;
import psoft.user.UserException;
import psoft.user.UserLoginException;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/MACToken.class */
public class MACToken {
    protected String login;
    protected long time;
    protected String value;
    protected String digest;

    public MACToken(String login, String password) {
        this.login = login;
        StringBuffer token = new StringBuffer(login);
        StringBuffer append = token.append('|');
        long currentTimeMillis = TimeUtils.currentTimeMillis();
        this.time = currentTimeMillis;
        append.append(currentTimeMillis).append('|');
        MD5 md5 = new MD5(token.toString() + password);
        String asHex = md5.asHex();
        this.digest = asHex;
        token.append(asHex);
        this.value = token.toString();
    }

    public String getValue() {
        return this.value;
    }

    public MACToken(String tmp) throws UserException {
        Session.getLog().debug("new MACToken( " + tmp + " )");
        try {
            StringTokenizer st = new StringTokenizer(tmp, "|");
            int count = 0;
            while (st.hasMoreTokens()) {
                switch (count) {
                    case 0:
                        this.login = st.nextToken();
                        break;
                    case 1:
                        this.time = Long.parseLong(st.nextToken());
                        break;
                    case 2:
                        this.digest = st.nextToken();
                        break;
                    default:
                        throw new UserException("Invalid token");
                }
                count++;
            }
        } catch (Throwable e) {
            Session.getLog().info("token", e);
            throw new UserLoginException("Invalid token");
        }
    }

    public String getLogin() {
        return this.login;
    }

    public long timeCreated() {
        return this.time;
    }

    public String getDigest() {
        return this.digest;
    }

    public boolean isValid(String password) {
        MD5 md5 = new MD5(this.login + '|' + this.time + '|' + password);
        return md5.asHex().equals(this.digest);
    }
}
