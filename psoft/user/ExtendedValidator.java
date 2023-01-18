package psoft.user;

import java.util.Enumeration;
import java.util.Vector;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidator;
import psoft.validators.ComplexValidatorException;
import psoft.validators.NameModifier;
import psoft.validators.ValidationException;

/* loaded from: hsphere.zip:psoft/user/ExtendedValidator.class */
public abstract class ExtendedValidator extends ComplexValidator {
    public void checkPresence(Accessor data, NameModifier nm) throws ComplexValidatorException {
        String value;
        boolean allThere = true;
        ComplexValidatorException cve = new ComplexValidatorException();
        Enumeration en = this.validators.elements();
        while (en.hasMoreElements()) {
            Object obj = en.nextElement();
            if (obj instanceof ComplexValidator.ValidatorEntry) {
                ComplexValidator.ValidatorEntry ve = (ComplexValidator.ValidatorEntry) obj;
                if (!ve.isOptional() && ((value = data.get(nm.getName(ve.getAccess()))) == null || value.equals(""))) {
                    cve.add(ve.getName(), new ValidationException("Required attribute " + ve.getName() + " is missing"));
                    allThere = false;
                }
            } else if (obj instanceof ComplexValidator.ComplexValidatorEntry) {
                ComplexValidator val = ((ComplexValidator.ComplexValidatorEntry) obj).getValidator();
                if (val instanceof ExtendedValidator) {
                    ((ExtendedValidator) val).checkPresence(data, nm.setNameModifier(((ComplexValidator.ComplexValidatorEntry) obj).getNM()));
                }
            } else {
                System.err.println("unexpected class:" + obj.getClass().toString());
            }
        }
        if (!allThere) {
            throw cve;
        }
    }

    public Vector listFields(NameModifier nm) {
        Vector v = new Vector();
        Enumeration en = this.validators.elements();
        while (en.hasMoreElements()) {
            Object obj = en.nextElement();
            if (obj instanceof ComplexValidator.ValidatorEntry) {
                ComplexValidator.ValidatorEntry ve = (ComplexValidator.ValidatorEntry) obj;
                v.add(nm.getName(ve.getAccess()));
            } else if (obj instanceof ComplexValidator.ComplexValidatorEntry) {
                ComplexValidator val = ((ComplexValidator.ComplexValidatorEntry) obj).getValidator();
                if (val instanceof ExtendedValidator) {
                    v.addAll(((ExtendedValidator) val).listFields(((ComplexValidator.ComplexValidatorEntry) obj).getNM()));
                }
            } else {
                System.err.println("unexpected class:" + obj.getClass().toString());
            }
        }
        return v;
    }
}
