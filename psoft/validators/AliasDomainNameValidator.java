package psoft.validators;

/* loaded from: hsphere.zip:psoft/validators/AliasDomainNameValidator.class */
public class AliasDomainNameValidator extends DomainNameValidator {
    protected String[] forbidden = {"www.", "webshell."};

    @Override // psoft.validators.ReValidator, psoft.validators.GenericValidator, psoft.validators.Validator
    public void check(String data) throws ValidationException {
        super.check(data);
        String tmp = data.toLowerCase();
        for (int i = 0; i < this.forbidden.length; i++) {
            if (tmp.startsWith(this.forbidden[i])) {
                throw new ValidationException("Domain name cannot start with " + this.forbidden[i]);
            }
        }
    }
}
