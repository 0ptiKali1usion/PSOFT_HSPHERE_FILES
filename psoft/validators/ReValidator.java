package psoft.validators;

import gnu.regexp.RE;
import gnu.regexp.REException;

/* loaded from: hsphere.zip:psoft/validators/ReValidator.class */
public class ReValidator extends GenericValidator {

    /* renamed from: re */
    protected RE f281re;

    public void init(String pattern) throws REException {
        this.f281re = new RE(pattern);
    }

    public ReValidator(String pattern) throws REException {
        init(pattern);
    }

    public ReValidator() {
    }

    public ReValidator(RE regexp) {
        this.f281re = regexp;
    }

    @Override // psoft.validators.GenericValidator, psoft.validators.Validator
    public void check(String data) throws ValidationException {
        super.check(data);
        try {
            if (!this.f281re.isMatch(data)) {
                throw new ValidationException(getErrorMessage());
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }
}
