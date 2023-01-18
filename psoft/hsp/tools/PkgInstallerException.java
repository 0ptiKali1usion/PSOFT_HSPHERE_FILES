package psoft.hsp.tools;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgInstallerException.class */
public class PkgInstallerException extends Exception {

    /* renamed from: t */
    Throwable f19t;

    public PkgInstallerException(String s) {
        super(s);
    }

    public PkgInstallerException(String s, Throwable t) {
        super(s);
        this.f19t = t;
    }

    @Override // java.lang.Throwable
    public Throwable getCause() {
        return this.f19t;
    }
}
