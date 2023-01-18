package psoft.user;

import java.util.Hashtable;
import java.util.Vector;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/user/GenericInfo.class */
public class GenericInfo implements Updatable {
    protected String login;
    protected String password;
    protected String email;

    public GenericInfo(String login, String password, String email) {
        this.login = login;
        this.password = password;
        this.email = email;
    }

    public Accessor getAccessor() {
        return new AttributeAccessor(this);
    }

    public static ExtendedValidator getValidator() {
        return new GenericInfoValidator();
    }

    @Override // psoft.user.Updatable
    public void update(Accessor data, NameModifier nm) throws ComplexValidatorException {
        Hashtable hash = getValidator().validate(data, nm);
        this.login = (String) hash.get("login");
        this.password = (String) hash.get("password");
        this.email = (String) hash.get("email");
    }

    @Override // psoft.user.Attributable
    public Vector listAttribs() {
        Vector v = new Vector();
        v.add("login");
        v.add("password");
        v.add("email");
        return v;
    }

    @Override // psoft.user.Attributable
    public Object getAttrib(String key) {
        if (key.equals("login")) {
            return this.login;
        }
        if (key.equals("password")) {
            return this.password;
        }
        if (key.equals("email")) {
            return this.email;
        }
        return null;
    }
}
