package psoft.validators;

import java.util.Enumeration;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/validators/ComplexValidatorException.class */
public class ComplexValidatorException extends Exception {
    protected Hashtable exceptionHash = new Hashtable();
    protected Hashtable complexException = new Hashtable();
    protected Enumeration exceptions = null;

    public Hashtable getExceptions() {
        return this.exceptionHash;
    }

    public ComplexValidatorException next() {
        if (this.exceptions == null) {
            this.exceptions = this.complexException.keys();
        }
        if (this.exceptions.hasMoreElements()) {
        }
        return (ComplexValidatorException) this.complexException.get(this.exceptions.nextElement());
    }

    public ComplexValidatorException get(NameModifier nm) {
        return (ComplexValidatorException) this.complexException.get(nm);
    }

    public boolean hasMore() {
        if (this.exceptions == null) {
            this.exceptions = this.complexException.keys();
        }
        return this.exceptions.hasMoreElements();
    }

    public void add(String fieldName, ValidationException ve) {
        this.exceptionHash.put(fieldName, ve);
    }

    public void add(NameModifier fieldName, ComplexValidatorException cve) {
        this.complexException.put(fieldName, cve);
    }
}
