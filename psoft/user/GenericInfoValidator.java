package psoft.user;

import java.util.Hashtable;
import psoft.persistance.PersistanceError;
import psoft.util.Config;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.DefaultValidator;
import psoft.validators.HashAccessor;
import psoft.validators.NameModifier;
import psoft.validators.ValidationException;

/* loaded from: hsphere.zip:psoft/user/GenericInfoValidator.class */
public class GenericInfoValidator extends ExtendedValidator {
    protected Class whichUser;

    public GenericInfoValidator() {
        try {
            System.err.println("User used: " + Config.getProperty("psoft.user", "USER_CLASS"));
            this.whichUser = Class.forName(Config.getProperty("psoft.user", "USER_CLASS"));
        } catch (ClassNotFoundException e) {
            System.err.println(Config.getProperty("psoft.user", "USER_CLASS") + " not found. Returning validator for GenericUser.");
            this.whichUser = GenericUser.class;
        }
        register("login", "login", DefaultValidator.user);
        register("password", "password", DefaultValidator.password, true);
        register("password", "password1", DefaultValidator.password, true);
        register("password2", "password2", DefaultValidator.copy, true);
        register("email", "email", DefaultValidator.email);
    }

    @Override // psoft.validators.ComplexValidator
    public Hashtable validate(Accessor data, NameModifier nm) throws ComplexValidatorException {
        Hashtable hash = super.validate(data, nm);
        String pw = (String) hash.get("password");
        String pw2 = (String) hash.get("password2");
        try {
            Config.getUPM("psoft.user").get((Accessor) new HashAccessor(hash), NameModifier.blank(), this.whichUser);
            ComplexValidatorException cve = new ComplexValidatorException();
            cve.add("login", new ValidationException("login Already in use"));
            throw cve;
        } catch (PersistanceError e) {
            if (pw == null) {
                ComplexValidatorException cve2 = new ComplexValidatorException();
                cve2.add("password", new ValidationException("Missing Password."));
                throw cve2;
            } else if (pw2 != null && !pw.equals(pw2)) {
                ComplexValidatorException cve3 = new ComplexValidatorException();
                cve3.add("password", new ValidationException("Passwords do not match"));
                throw cve3;
            } else {
                return hash;
            }
        }
    }

    @Override // psoft.user.ExtendedValidator
    public void checkPresence(Accessor data, NameModifier nm) throws ComplexValidatorException {
        ComplexValidatorException cve;
        boolean allThere = true;
        try {
            super.checkPresence(data, nm);
            cve = new ComplexValidatorException();
        } catch (ComplexValidatorException cv) {
            cve = cv;
            allThere = false;
        }
        String pw = data.get(nm.getName("password"));
        String pw1 = data.get(nm.getName("password1"));
        if ((pw == null || pw.equals("")) && (pw1 == null || pw1.equals(""))) {
            cve.add("password", new ValidationException("Required attribute password missing"));
            allThere = false;
        }
        if (!allThere) {
            throw cve;
        }
    }
}
