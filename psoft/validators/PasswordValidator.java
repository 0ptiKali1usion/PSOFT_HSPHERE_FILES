package psoft.validators;

import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/PasswordValidator.class */
public class PasswordValidator extends ReValidator {
    public PasswordValidator() {
        try {
            init("^[a-zA-Z0-9]([a-zA-Z0-9_!$&])+");
            setMin(4, "Password must be at least 4 chars long.");
            setMax(10, "Password must be under 11 chars long.");
            setErrorMessage("Invalid charaters in Password");
        } catch (REException e) {
            e.printStackTrace();
        }
    }
}
