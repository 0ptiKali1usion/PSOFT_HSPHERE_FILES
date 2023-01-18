package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/ResourceMoveException.class */
public class ResourceMoveException extends Exception {
    public ResourceMoveException(String message) {
        super(message);
    }

    public ResourceMoveException(Throwable tr) {
        super(tr.getMessage());
    }
}
