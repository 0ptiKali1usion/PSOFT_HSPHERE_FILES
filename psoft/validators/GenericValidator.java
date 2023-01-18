package psoft.validators;

/* loaded from: hsphere.zip:psoft/validators/GenericValidator.class */
public class GenericValidator extends Validator {
    protected int maxSize = -1;
    protected int minSize = -1;
    protected String maxErrorMessage;
    protected String minErrorMessage;

    @Override // psoft.validators.Validator
    public void check(String data) throws ValidationException {
        if (data != null) {
            if (this.minSize != -1 && this.minSize > data.length()) {
                throw new ValidationException(this.minErrorMessage);
            }
            if (this.maxSize != -1 && this.maxSize < data.length()) {
                throw new ValidationException(this.maxErrorMessage);
            }
            System.err.println("Out of GenericValidator check");
            return;
        }
        throw new ValidationException(getErrorMessage());
    }

    public void setMax(int size, String message) {
        this.maxSize = size;
        this.maxErrorMessage = message;
    }

    public void setMin(int size, String message) {
        this.minSize = size;
        this.minErrorMessage = message;
    }
}
