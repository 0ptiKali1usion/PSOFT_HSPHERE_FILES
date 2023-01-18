package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/AuthToken.class */
public class AuthToken implements Serializable {
    String login;
    String password;
    int accountId;
    Role role;

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AuthToken() {
    }

    public AuthToken(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return this.login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAccountId() {
        return this.accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }
}
