package psoft.util;

import java.io.PrintStream;
import java.io.PrintWriter;

/* loaded from: hsphere.zip:psoft/util/ChainedException.class */
public class ChainedException extends Exception {
    protected Throwable cause;

    public ChainedException() {
        this.cause = null;
    }

    public ChainedException(String message) {
        super(message);
        this.cause = null;
    }

    public ChainedException(String message, Throwable cause) {
        super(message);
        this.cause = null;
        this.cause = cause;
    }

    public ChainedException(Throwable cause) {
        this.cause = null;
        this.cause = cause;
    }

    @Override // java.lang.Throwable
    public Throwable getCause() {
        return this.cause;
    }

    @Override // java.lang.Throwable
    public void printStackTrace() {
        super.printStackTrace();
        if (this.cause != null) {
            System.err.println("Caused by:");
            this.cause.printStackTrace();
        }
    }

    @Override // java.lang.Throwable
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (this.cause != null) {
            ps.println("Caused by:");
            this.cause.printStackTrace(ps);
        }
    }

    @Override // java.lang.Throwable
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (this.cause != null) {
            pw.println("Caused by:");
            this.cause.printStackTrace(pw);
        }
    }
}
