package psoft.validators;

/* loaded from: hsphere.zip:psoft/validators/Validator.class */
public abstract class Validator {
    private String error;

    public abstract void check(String str) throws ValidationException;

    public String getErrorMessage() {
        return this.error;
    }

    public void setErrorMessage(String error) {
        this.error = error;
    }

    public String validate(String data) throws ValidationException {
        check(data);
        return data;
    }
}
