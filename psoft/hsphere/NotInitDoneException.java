package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/NotInitDoneException.class */
public class NotInitDoneException extends Exception {
    public NotInitDoneException() {
        Session.getLog().debug("NotInitDoneException", this);
    }
}
