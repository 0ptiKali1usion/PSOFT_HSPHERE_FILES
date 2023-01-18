package psoft.hsphere.resource;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/* loaded from: hsphere.zip:psoft/hsphere/resource/HTTPAuth.class */
public class HTTPAuth extends Authenticator {
    protected static String login;
    protected static String passwd;

    public HTTPAuth(String login2, String password) {
        login = login2;
        passwd = password;
    }

    @Override // java.net.Authenticator
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(login, passwd.toCharArray());
    }
}
