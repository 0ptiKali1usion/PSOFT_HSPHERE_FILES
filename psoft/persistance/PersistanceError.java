package psoft.persistance;

/* loaded from: hsphere.zip:psoft/persistance/PersistanceError.class */
public class PersistanceError extends Error {

    /* renamed from: t */
    protected Throwable f245t;

    public PersistanceError() {
    }

    public PersistanceError(String s) {
        super(s);
    }

    public PersistanceError(Throwable t) {
        this.f245t = t;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.f245t == null ? super.getMessage() : this.f245t.getMessage();
    }

    @Override // java.lang.Throwable
    public void printStackTrace() {
        if (this.f245t != null) {
            this.f245t.printStackTrace();
        } else {
            super.printStackTrace();
        }
    }
}
