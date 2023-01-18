package psoft.validators;

import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/PhoneNumberValidator.class */
public class PhoneNumberValidator extends ReValidator {
    public PhoneNumberValidator() {
        try {
            init("[0-9 -.()xX]*");
            setErrorMessage("Invalid Phone Number");
            setMin(6, "Phone Number should be longer than 6 chars");
            setMax(30, "Phone Number should be shorter than 31 chars");
        } catch (REException e) {
            e.printStackTrace();
        }
    }
}
