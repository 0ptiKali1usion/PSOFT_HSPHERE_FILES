package psoft.user;

/* loaded from: hsphere.zip:psoft/user/UserSignature.class */
public class UserSignature {
    protected String login;
    protected Class userType;

    public UserSignature(String login, Class userType) {
        this.login = login;
        this.userType = userType;
    }

    public int hashCode() {
        return this.login.hashCode();
    }

    public String toString() {
        return this.userType.toString() + " : " + this.login;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof UserSignature)) {
            return false;
        }
        UserSignature sig = (UserSignature) obj;
        return sig.login.equals(this.login) && sig.userType.equals(this.userType);
    }
}
