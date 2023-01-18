package psoft.validators;

import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/UserValidator.class */
public class UserValidator extends ReValidator {
    public UserValidator() {
        try {
            init("^[a-zA-Z0-9]([a-zA-Z0-9_!$&])+");
            setMin(4, "Username must be at least 4 chars long.");
            setMax(10, "Username must be under 11 chars long.");
            setErrorMessage("Invalid charaters in Username");
        } catch (REException e) {
            e.printStackTrace();
        }
    }
}
