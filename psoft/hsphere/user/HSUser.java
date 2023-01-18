package psoft.hsphere.user;

import psoft.user.GenericUser;

/* loaded from: hsphere.zip:psoft/hsphere/user/HSUser.class */
public class HSUser extends GenericUser {
    protected String dir;
    protected String url;
    protected String realLogin;

    public String getDir() {
        return this.dir;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUrl(String dir) {
        return this.url + dir;
    }

    public String getRealLogin() {
        return this.realLogin;
    }

    public HSUser(int id, String login, String realLogin, String password, String email, String dir, String url) {
        super(id, login, password, email);
        this.realLogin = realLogin;
        this.dir = dir;
        this.url = url;
    }

    public HSUser(String login, String realLogin, String password, String email, String dir, String url) {
        this(-1, login, realLogin, password, email, dir, url);
    }
}
