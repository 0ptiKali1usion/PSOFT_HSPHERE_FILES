package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/AccountCreationException.class */
public class AccountCreationException extends Exception {

    /* renamed from: ex */
    Exception f24ex;

    public AccountCreationException(Exception ex) {
        this.f24ex = ex;
        Session.getLog().debug("Error account creation tracked, request dumped", this);
    }

    public Exception getReason() {
        return this.f24ex;
    }
}
