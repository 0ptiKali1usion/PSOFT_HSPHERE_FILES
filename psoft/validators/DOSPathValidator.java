package psoft.validators;

import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/DOSPathValidator.class */
public class DOSPathValidator extends ReValidator {
    public DOSPathValidator() {
        try {
            init("^[a-zA-Z]:\\\\([a-zA-Z0-9_]+\\\\)*$");
            setErrorMessage("Path in not valid");
        } catch (REException e) {
            e.printStackTrace();
        }
    }
}
