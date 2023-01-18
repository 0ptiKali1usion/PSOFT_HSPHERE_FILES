package psoft.validators;

import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/DomainNameValidator.class */
public class DomainNameValidator extends ReValidator {
    public DomainNameValidator() {
        try {
            init("^([-a-zA-Z0-9]+\\.)+\\w+$");
            setErrorMessage("Invalid domain name");
        } catch (REException e) {
            e.printStackTrace();
        }
    }

    @Override // psoft.validators.Validator
    public String validate(String data) throws ValidationException {
        return super.validate(data).toLowerCase();
    }
}
