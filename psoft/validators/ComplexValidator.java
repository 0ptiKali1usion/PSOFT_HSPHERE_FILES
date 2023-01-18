package psoft.validators;

import java.util.Hashtable;
import java.util.Vector;

/* loaded from: hsphere.zip:psoft/validators/ComplexValidator.class */
public class ComplexValidator {
    protected Vector validators = new Vector();

    /* loaded from: hsphere.zip:psoft/validators/ComplexValidator$Entry.class */
    public abstract class Entry {
        String name;

        public abstract void validate(NameModifier nameModifier, Hashtable hashtable, Accessor accessor) throws ValidationException, ComplexValidatorException;

        Entry() {
            ComplexValidator.this = r4;
        }

        public String getName() {
            return this.name;
        }
    }

    /* loaded from: hsphere.zip:psoft/validators/ComplexValidator$ValidatorEntry.class */
    public class ValidatorEntry extends Entry {
        String access;

        /* renamed from: v */
        Validator f279v;
        boolean optional;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ValidatorEntry(String name, String access, Validator v, boolean optional) {
            super();
            ComplexValidator.this = r4;
            this.name = name;
            this.access = access;
            this.f279v = v;
            this.optional = optional;
        }

        @Override // psoft.validators.ComplexValidator.Entry
        public void validate(NameModifier nm, Hashtable hash, Accessor data) throws ValidationException {
            String value = data.get(nm.getName(this.access));
            if (this.optional && (value == null || value.length() == 0)) {
                hash.put(this.name, "");
            } else {
                hash.put(this.name, this.f279v.validate(data.get(nm.getName(this.access))));
            }
        }

        @Override // psoft.validators.ComplexValidator.Entry
        public String getName() {
            return this.name;
        }

        public String getAccess() {
            return this.access;
        }

        public boolean isOptional() {
            return this.optional;
        }
    }

    /* loaded from: hsphere.zip:psoft/validators/ComplexValidator$ComplexValidatorEntry.class */
    protected class ComplexValidatorEntry extends Entry {

        /* renamed from: nm */
        NameModifier f277nm;

        /* renamed from: v */
        ComplexValidator f278v;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ComplexValidatorEntry(String name, NameModifier nm, ComplexValidator v) {
            super();
            ComplexValidator.this = r4;
            this.name = name;
            this.f277nm = nm;
            this.f278v = v;
        }

        @Override // psoft.validators.ComplexValidator.Entry
        public void validate(NameModifier nm, Hashtable hash, Accessor data) throws ComplexValidatorException {
            hash.put(this.name, this.f278v.validate(data, this.f277nm.setNameModifier(nm)));
        }

        public ComplexValidator getValidator() {
            return this.f278v;
        }

        public NameModifier getNM() {
            return this.f277nm;
        }
    }

    public void register(String name, String access, Validator v) {
        register(name, access, v, false);
    }

    public void register(String name, String access, Validator v, boolean optional) {
        this.validators.add(new ValidatorEntry(name, access, v, optional));
    }

    public void register(String name, NameModifier nm, ComplexValidator v) {
        this.validators.add(new ComplexValidatorEntry(name, nm, v));
    }

    public Hashtable validate(Accessor data, NameModifier nameModifier) throws ComplexValidatorException {
        ComplexValidatorException tmpCVE = new ComplexValidatorException();
        if (data == null) {
            throw tmpCVE;
        }
        if (nameModifier == null) {
            nameModifier = new NameModifier("", "", null);
        }
        Hashtable hash = new Hashtable();
        boolean isError = false;
        for (int i = 0; i < this.validators.size(); i++) {
            Entry v = (Entry) this.validators.elementAt(i);
            try {
                v.validate(nameModifier, hash, data);
            } catch (ComplexValidatorException cve) {
                isError = true;
                tmpCVE.add(nameModifier, cve);
            } catch (ValidationException ve) {
                isError = true;
                tmpCVE.add(v.getName(), ve);
            }
        }
        if (isError) {
            throw tmpCVE;
        }
        return hash;
    }
}
