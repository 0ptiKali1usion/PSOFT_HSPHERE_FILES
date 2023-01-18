package psoft.validators;

import gnu.regexp.REException;
import psoft.hsphere.SignupGuard;

/* loaded from: hsphere.zip:psoft/validators/EmailValidator.class */
public class EmailValidator extends ReValidator {
    public EmailValidator() {
        try {
            init("^[-_a-zA-Z0-9.]*@([-a-zA-Z0-9]+\\.)+\\w+$");
            setErrorMessage("Invalid Email Address");
            setMin(8, "Email should be 8 chars or longer");
            setMax(SignupGuard.CVV_VALIDATION, "Email should be 256 chars or shorter");
        } catch (REException e) {
            e.printStackTrace();
        }
    }

    @Override // psoft.validators.Validator
    public String validate(String data) throws ValidationException {
        return super.validate(data).toLowerCase();
    }
}
